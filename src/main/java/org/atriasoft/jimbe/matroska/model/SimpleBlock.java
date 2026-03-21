package org.atriasoft.jimbe.matroska.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
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
	private int trackNumber = 0;
	private short timecode = 0;
	private boolean keyFrame = true;
	private LaceMode laceMode = LaceMode.EBML;
	private boolean invisible = false;
	private boolean discardable = false;
	private final List<Frame> frames = new ArrayList<>();
	private int totalSize = 18;

	private long duration = Long.MIN_VALUE;

	/** Constructs an empty SimpleBlock with default settings. */
	public SimpleBlock() {

	}

	/**
	 * Converts this block to the appropriate EBML element.
	 * Returns a SimpleBlock element if no duration is set, otherwise a BlockGroup.
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
	 * Creates a BlockGroup EBML element containing the block data and its duration.
	 *
	 * @return a {@link MasterElement} representing the BlockGroup
	 */
	public Element toBlockGroup() {
		final MasterElement blockGroupElem = DocTypes.BlockGroup.getInstance();
		final BinaryElement blockElem = DocTypes.Block.getInstance();
		blockElem.setData(createInnerData());
		final UnsignedIntegerElement durationElem = DocTypes.BlockDuration.getInstance();
		durationElem.setValue(this.duration);
		blockGroupElem.addChildElement(blockElem);
		blockGroupElem.addChildElement(durationElem);
		return blockGroupElem;
	}

	private ByteBuffer createInnerData() {
		final ByteBuffer buf = ByteBuffer.allocate(this.totalSize);

		assert this.trackNumber < 0x4000;
		if (this.trackNumber < 0x80) {
			buf.put((byte) (this.trackNumber | 0x80));
		} else {
			buf.put((byte) (this.trackNumber >> 8 | 0x40));
			buf.put((byte) (this.trackNumber & 0xFF));
		}

		buf.putShort(this.timecode);
		final BitSet bs = new BitSet(8);
		bs.set(0, this.keyFrame);
		bs.set(4, this.invisible);
		ByteBuffer sizes = null;
		this.laceMode = pickBestLaceMode();
		// TODO: correctly calculate resultant buffer size for different lace modes
		switch (this.laceMode) {
			case EBML:
				bs.set(5);
				bs.set(6);
				sizes = ebmlEncodeLaceSizes();
				break;
			case XIPH:
				bs.set(6);
				sizes = xiphEncodeLaceSizes();
				break;
			case FIXED:
				sizes = fixedEncodeLaceSizes();
				bs.set(5);
				break;
			case NONE:
				LOG.trace("Lace mode none!");
				break;
			default:
				break;
		}
		bs.set(7, this.discardable);
		buf.put(bs.toByteArray());
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
		return ByteBuffer.allocate(1).put((byte) (this.frames.size() - 1));
	}

	private ByteBuffer xiphEncodeLaceSizes() {
		LOG.trace("Encoding xiph lace sizes");
		final ByteBuffer buf = ByteBuffer.allocate(30);
		buf.put((byte) (this.frames.size() - 1));
		for (int i = 0; i < this.frames.size() - 1; ++i) {
			int tmpSize = this.frames.get(i).getData().remaining();
			while (tmpSize >= 0xFF) {
				buf.put((byte) 0xFF);
				tmpSize -= 0xFF;
			}
			buf.put((byte) tmpSize);
		}
		return buf;
	}

	private ByteBuffer ebmlEncodeLaceSizes() {
		LOG.trace("Encoding ebml lace sizes");
		final ByteBuffer buf = ByteBuffer.allocate(30);
		buf.put((byte) (this.frames.size() - 1));
		for (int i = 0; i < this.frames.size() - 1; ++i) {
			final int tmpSize = this.frames.get(i).getData().remaining();
			buf.put(Element.makeEbmlCodedSize(tmpSize));
		}
		return buf;
	}

	/** Returns the timecode of this block relative to the containing cluster. */
	public long getTimecode() {
		return this.timecode;
	}

	/**
	 * Sets the timecode of this block relative to the containing cluster.
	 *
	 * @param timecode the timecode value (must fit in a {@code short})
	 */
	public void setTimecode(final long timecode) {
		assert timecode < Short.MAX_VALUE;
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
		this.totalSize += frame.getData().remaining();
		this.frames.add(frame);
		if (frame.getDuration() != Long.MIN_VALUE) {
			this.duration = frame.getDuration();
		}

		if (frame.getData().remaining() > MAX_LACE_SIZE) {
			this.laceMode = LaceMode.NONE;
			return false;
		}
		this.totalSize += 4;

		return !isSimpleBlock() || !(this.laceMode.equals(LaceMode.NONE) || this.frames.size() > 8);
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
	 * Returns {@code true} if this block should be written as a SimpleBlock (no duration set).
	 * When a duration is present, it must be written as a BlockGroup instead.
	 */
	public boolean isSimpleBlock() {
		return this.duration == Long.MIN_VALUE;
	}
}
