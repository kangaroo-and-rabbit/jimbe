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

	public SimpleBlock() {

	}

	public Element toElement() {
		if (isSimpleBlock()) {
			return toSimpleBlock();
		}
		return toBlockGroup();
	}

	public Element toSimpleBlock() {
		final BinaryElement blockElem = DocTypes.SimpleBlock.getInstance();
		blockElem.setData(createInnerData());
		return blockElem;
	}

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

	public long getTimecode() {
		return this.timecode;
	}

	public void setTimecode(final long timecode) {
		assert timecode < Short.MAX_VALUE;
		this.timecode = (short) timecode;
	}

	public LaceMode getLaceMode() {
		return this.laceMode;
	}

	public void setLaceMode(final LaceMode laceMode) {
		this.laceMode = laceMode;
	}

	public boolean isInvisible() {
		return this.invisible;
	}

	public void setInvisible(final boolean invisible) {
		this.invisible = invisible;
	}

	public boolean isDiscardable() {
		return this.discardable;
	}

	public void setDiscardable(final boolean discardable) {
		this.discardable = discardable;
	}

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

	public int getTrackNumber() {
		return this.trackNumber;
	}

	public void setTrackNumber(final int trackNumber) {
		this.trackNumber = trackNumber;
	}

	public boolean isKeyFrame() {
		return this.keyFrame;
	}

	public void setKeyFrame(final boolean keyFrame) {
		this.keyFrame = keyFrame;
	}

	public long getDuration() {
		return this.duration;
	}

	public void setDuration(final long duration) {
		this.duration = duration;
	}

	public boolean isSimpleBlock() {
		return this.duration == Long.MIN_VALUE;
	}
}
