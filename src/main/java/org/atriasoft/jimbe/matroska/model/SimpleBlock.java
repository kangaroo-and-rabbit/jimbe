package org.atriasoft.jimbe.matroska.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.SignedIntegerElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and serializes a Matroska SimpleBlock or BlockGroup element for writing.
 * <p>
 * A SimpleBlock is the most common way to store frame data in a Matroska cluster.
 * When a duration is specified, the block is written as a BlockGroup instead.
 * This class handles frame accumulation, lacing mode selection, and binary
 * encoding of the block header and payload.
 * </p>
 *
 * @see Block
 * @see Frame
 * @see LaceMode
 */
public class SimpleBlock {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleBlock.class);

	// Note: this max size based on libmatroska src
	private static final int MAX_LACE_SIZE = 6 * 0xFF;

	/** A lace size is a frame size, an int: 5 bytes of EBML hold it, and the difference of two. */
	private static final int MAX_EBML_SIZE_BYTES = 5;
	private int trackNumber = 0;
	private short timecode = 0;
	private boolean keyFrame = true;
	private LaceMode laceMode = LaceMode.NONE;
	private boolean invisible = false;
	private boolean discardable = false;
	private final List<Frame> frames = new ArrayList<>();
	private int totalSize = 18;

	private long duration = Long.MIN_VALUE;

	/** What a decoder drops from the end of this block in nanoseconds, or 0 if nothing. */
	private long discardPadding = 0;

	/** Constructs an empty SimpleBlock with default settings. */
	public SimpleBlock() {

	}

	/**
	 * Converts this block to the appropriate EBML element.
	 * Returns a SimpleBlock element if it has nothing else to carry, otherwise a BlockGroup.
	 *
	 * @return the EBML {@link Element} representing this block
	 */
	public Element toElement() {
		if (isSimpleBlock()) {
			return toSimpleBlock();
		}
		return toBlockGroup();
	}

	/**
	 * Creates a SimpleBlock EBML element from the accumulated frames.
	 *
	 * @return a {@link BinaryElement} representing the SimpleBlock
	 */
	public Element toSimpleBlock() {
		final BinaryElement blockElem = DocTypes.SimpleBlock.getInstance();
		blockElem.setData(createInnerData());
		return blockElem;
	}

	/**
	 * Creates a BlockGroup EBML element containing the block data, its duration and its padding.
	 *
	 * @return a {@link MasterElement} representing the BlockGroup
	 */
	public Element toBlockGroup() {
		final MasterElement blockGroupElem = DocTypes.BlockGroup.getInstance();
		final BinaryElement blockElem = DocTypes.Block.getInstance();
		blockElem.setData(createInnerData());
		blockGroupElem.addChildElement(blockElem);
		if (this.duration != Long.MIN_VALUE) {
			final UnsignedIntegerElement durationElem = DocTypes.BlockDuration.getInstance();
			durationElem.setValue(this.duration);
			blockGroupElem.addChildElement(durationElem);
		}
		if (this.discardPadding != 0) {
			final SignedIntegerElement paddingElem = DocTypes.DiscardPadding.getInstance();
			paddingElem.setValue(this.discardPadding);
			blockGroupElem.addChildElement(paddingElem);
		}
		if (!this.keyFrame) {
			final SignedIntegerElement refElem = DocTypes.ReferenceBlock.getInstance();
			refElem.setValue(0);
			blockGroupElem.addChildElement(refElem);
		}
		return blockGroupElem;
	}

	private ByteBuffer createInnerData() {
		// Matroska SimpleBlock flags byte (big-endian bit numbering):
		// bit 7 (0x80): keyframe
		// bit 3 (0x08): invisible
		// bits 2-1 (0x06): lacing (00=none, 01=Xiph, 11=EBML, 10=fixed)
		// bit 0 (0x01): discardable
		byte flags = 0;
		if (this.keyFrame) {
			flags |= 0x80;
		}
		if (this.invisible) {
			flags |= 0x08;
		}
		if (this.discardable) {
			flags |= 0x01;
		}
		ByteBuffer sizes = null;
		this.laceMode = pickBestLaceMode();
		switch (this.laceMode) {
			case EBML:
				flags |= 0x06;
				sizes = ebmlEncodeLaceSizes();
				break;
			case XIPH:
				flags |= 0x02;
				sizes = xiphEncodeLaceSizes();
				break;
			case FIXED:
				flags |= 0x04;
				sizes = fixedEncodeLaceSizes();
				break;
			case NONE:
				LOG.trace("Lace mode none!");
				break;
			default:
				break;
		}
		final ByteBuffer buf = ByteBuffer.allocate(this.totalSize + (sizes == null ? 0 : sizes.remaining()));

		assert this.trackNumber < 0x4000;
		if (this.trackNumber < 0x80) {
			buf.put((byte) (this.trackNumber | 0x80));
		} else {
			buf.put((byte) (this.trackNumber >> 8 | 0x40));
			buf.put((byte) (this.trackNumber & 0xFF));
		}

		buf.putShort(this.timecode);
		buf.put(flags);
		if (sizes != null) {
			buf.put(sizes);
		}
		for (final Frame frame : this.frames) {
			LOG.trace("Writing frame {}", frame.getData().remaining());
			buf.put(frame.getData());
		}
		buf.flip();
		return buf;
	}

	private LaceMode pickBestLaceMode() {
		if (this.frames.size() == 1) {
			return LaceMode.NONE;
		}
		return this.laceMode;
	}

	private ByteBuffer fixedEncodeLaceSizes() {
		LOG.trace("Encoding fixed lace sizes");
		for (final Frame frame : this.frames) {
			if (frame.getData().remaining() != this.frames.get(0).getData().remaining()) {
				throw new IllegalStateException("Fixed lacing needs frames of one size");
			}
		}
		final ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put((byte) (this.frames.size() - 1));
		buf.flip();
		return buf;
	}

	private ByteBuffer xiphEncodeLaceSizes() {
		LOG.trace("Encoding xiph lace sizes");
		int length = 1;
		for (int i = 0; i < this.frames.size() - 1; ++i) {
			length += this.frames.get(i).getData().remaining() / 0xFF + 1;
		}
		final ByteBuffer buf = ByteBuffer.allocate(length);
		buf.put((byte) (this.frames.size() - 1));
		for (int i = 0; i < this.frames.size() - 1; ++i) {
			int tmpSize = this.frames.get(i).getData().remaining();
			while (tmpSize >= 0xFF) {
				buf.put((byte) 0xFF);
				tmpSize -= 0xFF;
			}
			buf.put((byte) tmpSize);
		}
		buf.flip();
		return buf;
	}

	private ByteBuffer ebmlEncodeLaceSizes() {
		LOG.trace("Encoding ebml lace sizes");
		final ByteBuffer buf = ByteBuffer.allocate(1 + this.frames.size() * MAX_EBML_SIZE_BYTES);
		buf.put((byte) (this.frames.size() - 1));
		int previousSize = 0;
		for (int i = 0; i < this.frames.size() - 1; ++i) {
			final int size = this.frames.get(i).getData().remaining();
			// The first size is absolute, each of the others is relative to the one before.
			buf.put(i == 0 ? Element.makeEbmlCodedSize(size) : makeSignedEbmlCodedSize(size - previousSize));
			previousSize = size;
		}
		buf.flip();
		return buf;
	}

	/** A signed value is stored shifted by half of what its length can hold: 0 is 63 on one byte. */
	private static byte[] makeSignedEbmlCodedSize(final long value) {
		for (int length = 1; length <= MAX_EBML_SIZE_BYTES; length++) {
			final long shift = (1L << (7 * length - 1)) - 1;
			if (Math.abs(value) <= shift) {
				return Element.makeEbmlCodedSize(value + shift, length);
			}
		}
		throw new IllegalArgumentException("Too large for a lace size: " + value);
	}

	/** Returns the timecode of this block relative to the containing cluster. */
	public long getTimecode() {
		return this.timecode;
	}

	/**
	 * Sets the timecode of this block relative to the containing cluster.
	 *
	 * @param timecode the timecode value
	 * @throws IllegalArgumentException if the value does not fit in a {@code short}: cast to fit, it would
	 *         silently move the frame
	 */
	public void setTimecode(final long timecode) {
		if (timecode < Short.MIN_VALUE || timecode > Short.MAX_VALUE) {
			throw new IllegalArgumentException("A block timecode must fit in 16 bits: " + timecode);
		}
		this.timecode = (short) timecode;
	}

	/** Returns the lacing mode used for encoding multiple frames in this block. */
	public LaceMode getLaceMode() {
		return this.laceMode;
	}

	/** Sets the lacing mode for encoding multiple frames in this block. */
	public void setLaceMode(final LaceMode laceMode) {
		this.laceMode = laceMode;
	}

	/** Returns {@code true} if this block is marked as invisible (should not be rendered). */
	public boolean isInvisible() {
		return this.invisible;
	}

	/** Sets whether this block is invisible (should not be rendered). */
	public void setInvisible(final boolean invisible) {
		this.invisible = invisible;
	}

	/** Returns {@code true} if this block can be discarded during playback if needed. */
	public boolean isDiscardable() {
		return this.discardable;
	}

	/** Sets whether this block can be discarded during playback. */
	public void setDiscardable(final boolean discardable) {
		this.discardable = discardable;
	}

	/**
	 * Adds a frame to this block. The block's timecode and track number are set
	 * from the first frame added.
	 *
	 * @param frame the frame to add
	 * @return {@code true} if more frames can be added to this block, {@code false} if
	 *         a new block should be started (e.g., frame too large for lacing or max frames reached)
	 */
	public boolean addFrame(final Frame frame) {
		LOG.trace("Adding frame {}", frame.getData().remaining());
		setTimecode(frame.getTimecode());
		setTrackNumber(frame.getTrackNo());
		setKeyFrame(frame.isKeyFrame());
		this.totalSize += frame.getData().remaining();
		this.frames.add(frame);
		if (frame.getDuration() != Long.MIN_VALUE) {
			this.duration = frame.getDuration();
		}
		if (frame.getDiscardPadding() != 0) {
			this.discardPadding = frame.getDiscardPadding();
		}

		if (frame.getData().remaining() > MAX_LACE_SIZE) {
			// A block already holding frames stays laced: without the sizes they could not be told apart.
			if (this.frames.size() == 1) {
				this.laceMode = LaceMode.NONE;
			}
			return false;
		}
		this.totalSize += 4;

		// Without lacing a block holds one frame: two would be read back as one, glued together.
		if (this.laceMode == LaceMode.NONE) {
			return false;
		}
		// The padding of a block belongs to its last frame: another one would take it.
		if (frame.getDiscardPadding() != 0) {
			return false;
		}
		return !isSimpleBlock() || this.frames.size() <= 8;
	}

	/** Returns the track number that this block belongs to. */
	public int getTrackNumber() {
		return this.trackNumber;
	}

	/** Sets the track number that this block belongs to. */
	public void setTrackNumber(final int trackNumber) {
		this.trackNumber = trackNumber;
	}

	/** Returns {@code true} if this block contains a key frame. */
	public boolean isKeyFrame() {
		return this.keyFrame;
	}

	/** Sets whether this block contains a key frame. */
	public void setKeyFrame(final boolean keyFrame) {
		this.keyFrame = keyFrame;
	}

	/** Returns the duration of this block, or {@link Long#MIN_VALUE} if unset. */
	public long getDuration() {
		return this.duration;
	}

	/** Sets the duration of this block. Setting a duration causes it to be written as a BlockGroup. */
	public void setDuration(final long duration) {
		this.duration = duration;
	}

	/**
	 * Returns {@code true} if this block should be written as a SimpleBlock (no duration or padding set).
	 * When either is present, it must be written as a BlockGroup instead.
	 */
	public boolean isSimpleBlock() {
		return this.duration == Long.MIN_VALUE && this.discardPadding == 0;
	}
}
