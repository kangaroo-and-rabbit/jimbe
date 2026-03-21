package org.atriasoft.jimbe.matroska;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.matroska.model.DocTypes;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.LaceMode;
import org.atriasoft.jimbe.matroska.model.SimpleBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SimpleBlock.
 * This class is package-private so tests must be in the same package.
 */
public class SimpleBlockTest {

	// ========== isSimpleBlock / toElement routing ==========

	@Test
	public void testIsSimpleBlockByDefault() {
		final SimpleBlock block = new SimpleBlock();
		Assertions.assertTrue(block.isSimpleBlock(), "A new block with no duration set should be a SimpleBlock");
	}

	@Test
	public void testIsNotSimpleBlockWithDuration() {
		final SimpleBlock block = new SimpleBlock();
		block.setDuration(100);
		Assertions.assertFalse(block.isSimpleBlock(), "A block with duration set should not be a SimpleBlock");
	}

	@Test
	public void testToElementReturnsSimpleBlock() {
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = makeFrame(1, 0, new byte[] { 0x01, 0x02 });
		block.addFrame(frame);

		final Element elem = block.toElement();
		// SimpleBlock type ID
		Assertions.assertTrue(elem.isType(DocTypes.SimpleBlock.getType()));
	}

	@Test
	public void testToElementReturnsBlockGroup() {
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = makeFrame(1, 0, new byte[] { 0x01, 0x02 });
		frame.setDuration(500);
		block.addFrame(frame);

		final Element elem = block.toElement();
		// BlockGroup type ID
		Assertions.assertTrue(elem.isType(DocTypes.BlockGroup.getType()));
	}

	// ========== addFrame behaviour ==========

	@Test
	public void testAddFrameSetsTrackAndTimecode() {
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = makeFrame(5, 42, new byte[] { 0x01 });
		block.addFrame(frame);

		Assertions.assertEquals(5, block.getTrackNumber());
		Assertions.assertEquals(42, block.getTimecode());
	}

	@Test
	public void testAddFrameReturnsFalseForLargeFrame() {
		final SimpleBlock block = new SimpleBlock();
		// MAX_LACE_SIZE = 6 * 0xFF = 1530
		final byte[] largeData = new byte[1531];
		final Frame frame = makeFrame(1, 0, largeData);
		final boolean canAddMore = block.addFrame(frame);

		Assertions.assertFalse(canAddMore, "addFrame should return false for frames larger than MAX_LACE_SIZE");
	}

	@Test
	public void testAddFrameReturnsTrueForSmallFrame() {
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = makeFrame(1, 0, new byte[] { 0x01, 0x02 });
		final boolean canAddMore = block.addFrame(frame);

		Assertions.assertTrue(canAddMore, "addFrame should return true for a small single frame");
	}

	@Test
	public void testAddFrameWithDurationSetsModeNone() {
		// When duration is set, isSimpleBlock() is false, so addFrame always returns true
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = makeFrame(1, 0, new byte[] { 0x01 });
		frame.setDuration(100);
		final boolean canAddMore = block.addFrame(frame);

		Assertions.assertTrue(canAddMore);
		Assertions.assertEquals(100, block.getDuration());
	}

	// ========== Getters/setters ==========

	@Test
	public void testKeyFrameDefault() {
		final SimpleBlock block = new SimpleBlock();
		Assertions.assertTrue(block.isKeyFrame(), "Default should be keyFrame=true");
	}

	@Test
	public void testSetKeyFrame() {
		final SimpleBlock block = new SimpleBlock();
		block.setKeyFrame(false);
		Assertions.assertFalse(block.isKeyFrame());
		block.setKeyFrame(true);
		Assertions.assertTrue(block.isKeyFrame());
	}

	@Test
	public void testInvisibleDefault() {
		final SimpleBlock block = new SimpleBlock();
		Assertions.assertFalse(block.isInvisible());
	}

	@Test
	public void testSetInvisible() {
		final SimpleBlock block = new SimpleBlock();
		block.setInvisible(true);
		Assertions.assertTrue(block.isInvisible());
	}

	@Test
	public void testDiscardableDefault() {
		final SimpleBlock block = new SimpleBlock();
		Assertions.assertFalse(block.isDiscardable());
	}

	@Test
	public void testSetDiscardable() {
		final SimpleBlock block = new SimpleBlock();
		block.setDiscardable(true);
		Assertions.assertTrue(block.isDiscardable());
	}

	@Test
	public void testLaceModeDefault() {
		final SimpleBlock block = new SimpleBlock();
		Assertions.assertEquals(LaceMode.EBML, block.getLaceMode());
	}

	@Test
	public void testSetLaceMode() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.XIPH);
		Assertions.assertEquals(LaceMode.XIPH, block.getLaceMode());
	}

	@Test
	public void testDurationDefault() {
		final SimpleBlock block = new SimpleBlock();
		Assertions.assertEquals(Long.MIN_VALUE, block.getDuration());
	}

	// ========== createInnerData / toSimpleBlock output verification ==========

	@Test
	public void testSimpleBlockSingleFrameOutput() {
		final SimpleBlock block = new SimpleBlock();
		final byte[] payload = { 0x10, 0x20, 0x30 };
		final Frame frame = makeFrame(1, 0, payload);
		block.addFrame(frame);

		final Element elem = block.toSimpleBlock();
		Assertions.assertTrue(elem instanceof BinaryElement);
		final ByteBuffer data = elem.getData();
		Assertions.assertNotNull(data);
		// Single frame, no lacing: EBML-coded track (1 byte for track 1) + 2-byte timecode + 1-byte flags + payload
		// Track 1 encoded as 0x81 (1 | 0x80)
		Assertions.assertEquals(0x81, data.get(0) & 0xFF);
		// Timecode 0 as 2 bytes
		Assertions.assertEquals(0x00, data.get(1) & 0xFF);
		Assertions.assertEquals(0x00, data.get(2) & 0xFF);
		// After flags byte, payload starts
		final int payloadStart = 4; // 1 (track) + 2 (timecode) + 1 (flags)
		for (int i = 0; i < payload.length; i++) {
			Assertions.assertEquals(payload[i], data.get(payloadStart + i), "Payload byte " + i + " mismatch");
		}
	}

	@Test
	public void testSimpleBlockTwoByteTrackNumber() {
		final SimpleBlock block = new SimpleBlock();
		final byte[] payload = { 0x42 };
		// Track number 128 (>= 0x80) needs 2-byte encoding
		final Frame frame = makeFrame(128, 0, payload);
		block.addFrame(frame);

		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		// Track 128 encoded as 2 bytes: (128 >> 8 | 0x40), (128 & 0xFF) = (0x40), (0x80)
		Assertions.assertEquals(0x40, data.get(0) & 0xFF);
		Assertions.assertEquals(0x80, data.get(1) & 0xFF);
	}

	@Test
	public void testSimpleBlockTimecodeEncoded() {
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = makeFrame(1, 500, new byte[] { 0x01 });
		block.addFrame(frame);

		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		// Timecode 500 as big-endian short after track byte
		final short tc = data.getShort(1);
		Assertions.assertEquals(500, tc);
	}

	@Test
	public void testBlockGroupContainsBlockAndDuration() {
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = makeFrame(1, 0, new byte[] { 0x01 });
		frame.setDuration(250);
		block.addFrame(frame);

		final Element elem = block.toBlockGroup();
		Assertions.assertTrue(elem instanceof MasterElement);
		Assertions.assertTrue(elem.isType(DocTypes.BlockGroup.getType()));
	}

	// ========== pickBestLaceMode / lace encoding ==========

	@Test
	public void testSingleFrameForcesLaceModeNone() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.EBML);
		final Frame frame = makeFrame(1, 0, new byte[] { 0x01 });
		block.addFrame(frame);

		// After toSimpleBlock(), the inner data is created.
		// pickBestLaceMode returns NONE for single frame.
		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		// Flags byte at position 3: 1 (track) + 2 (timecode) = index 3
		final int flags = data.get(3) & 0xFF;
		// In BitSet layout: bits 5-6 are the lace mode bits.
		// NONE -> neither bit 5 nor bit 6 set -> (flags & 0x60) == 0
		final int laceBits = flags & 0x60;
		Assertions.assertEquals(0x00, laceBits, "Single frame should have no lacing");
	}

	/**
	 * EBML lacing with multiple frames causes BufferOverflowException.
	 * Bug: ebmlEncodeLaceSizes() doesn't flip its buffer, so buf.put(sizes)
	 * tries to write up to 30 uninitialized bytes, overflowing the output buffer.
	 */
	@Test
	public void testEbmlLaceOverflows() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.EBML);
		block.addFrame(makeFrame(1, 0, new byte[] { 0x01, 0x02 }));
		block.addFrame(makeFrame(1, 0, new byte[] { 0x03, 0x04 }));

		Assertions.assertThrows(java.nio.BufferOverflowException.class, () -> block.toSimpleBlock(),
				"EBML lacing has a known bug: missing flip() on sizes buffer");
	}

	/**
	 * XIPH lacing with multiple frames causes BufferOverflowException.
	 * Bug: xiphEncodeLaceSizes() doesn't flip its buffer, same as EBML.
	 */
	@Test
	public void testXiphLaceOverflows() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.XIPH);
		block.addFrame(makeFrame(1, 0, new byte[] { 0x01, 0x02 }));
		block.addFrame(makeFrame(1, 0, new byte[] { 0x03, 0x04 }));

		Assertions.assertThrows(java.nio.BufferOverflowException.class, () -> block.toSimpleBlock(),
				"XIPH lacing has a known bug: missing flip() on sizes buffer");
	}

	@Test
	public void testFixedLaceModeFlags() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.FIXED);
		block.addFrame(makeFrame(1, 0, new byte[] { 0x01, 0x02 }));
		block.addFrame(makeFrame(1, 0, new byte[] { 0x03, 0x04 }));

		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		final int flags = data.get(3) & 0xFF;
		// FIXED lacing: bs.set(5) only -> bit 5 -> 0x20
		Assertions.assertEquals(0x20, flags & 0x60, "FIXED lacing should set only bit 5");
	}

	/**
	 * Fixed lacing lace count byte is silently lost.
	 * Bug: fixedEncodeLaceSizes() returns a ByteBuffer with position=1 (after
	 * the put), so remaining()=0. The lace count is written into the buffer
	 * but never transferred to the output because buf.put(sizes) copies 0 bytes.
	 * As a result, the frame data directly follows the flags byte with no lace count.
	 */
	@Test
	public void testFixedLaceCountByteLost() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.FIXED);
		block.addFrame(makeFrame(1, 0, new byte[] { 0x01, 0x02 }));
		block.addFrame(makeFrame(1, 0, new byte[] { 0x03, 0x04 }));

		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		// Due to the bug, byte 4 is the first frame's first byte, not the lace count.
		// The lace count byte (1) was written to sizes buffer but never transferred.
		Assertions.assertEquals(0x01, data.get(4) & 0xFF,
				"Bug: lace count byte is lost, frame data starts immediately after flags");
	}

	// ========== Helper ==========

	private Frame makeFrame(final int trackNo, final long timecode, final byte[] data) {
		final Frame frame = new Frame();
		frame.setTrackNo(trackNo);
		frame.setTimecode(timecode);
		frame.setData(ByteBuffer.wrap(data));
		return frame;
	}
}
