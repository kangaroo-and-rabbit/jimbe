package org.atriasoft.jimbe.matroska;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.matroska.model.Block;
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
		block.setLaceMode(LaceMode.XIPH);
		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setData(ByteBuffer.wrap(new byte[100]));

		Assertions.assertTrue(block.addFrame(frame), "A laced block should accept more than one small frame");
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
		Assertions.assertEquals(LaceMode.NONE, block.getLaceMode());
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
		// Lace mode bits are bits 1-2 of flags byte.
		// NONE -> both bits clear -> (flags & 0x06) == 0
		final int laceBits = flags & 0x06;
		Assertions.assertEquals(0x00, laceBits, "Single frame should have no lacing");
	}

	@Test
	public void testEbmlLaceEncoding() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.EBML);
		block.addFrame(makeFrame(1, 0, new byte[] { 0x01, 0x02 }));
		block.addFrame(makeFrame(1, 0, new byte[] { 0x03, 0x04 }));

		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		final int flags = data.get(3) & 0xFF;
		Assertions.assertEquals(0x06, flags & 0x06, "EBML lacing should set bits 1-2");
		// byte 4 = lace count (1 = 2 frames - 1)
		Assertions.assertEquals(1, data.get(4) & 0xFF, "Lace count should be 1 for 2 frames");
	}

	@Test
	public void testXiphLaceEncoding() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.XIPH);
		block.addFrame(makeFrame(1, 0, new byte[] { 0x01, 0x02 }));
		block.addFrame(makeFrame(1, 0, new byte[] { 0x03, 0x04 }));

		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		final int flags = data.get(3) & 0xFF;
		Assertions.assertEquals(0x02, flags & 0x06, "XIPH lacing should set bit 1 only");
		// byte 4 = lace count (1 = 2 frames - 1)
		Assertions.assertEquals(1, data.get(4) & 0xFF, "Lace count should be 1 for 2 frames");
		// byte 5 = first frame size (2 bytes)
		Assertions.assertEquals(2, data.get(5) & 0xFF, "First frame size should be 2");
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
		Assertions.assertEquals(0x04, flags & 0x06, "FIXED lacing should set bit 2 only");
	}

	@Test
	public void testFixedLaceCountBytePresent() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.FIXED);
		block.addFrame(makeFrame(1, 0, new byte[] { 0x01, 0x02 }));
		block.addFrame(makeFrame(1, 0, new byte[] { 0x03, 0x04 }));

		final Element elem = block.toSimpleBlock();
		final ByteBuffer data = elem.getData();
		// byte 4 = lace count (1 = 2 frames - 1)
		Assertions.assertEquals(1, data.get(4) & 0xFF, "Lace count byte should be present");
		// byte 5 = first frame data
		Assertions.assertEquals(0x01, data.get(5) & 0xFF, "First frame data should follow lace count");
	}

	// ========== Helper ==========

	private Frame makeFrame(final int trackNo, final long timecode, final byte[] data) {
		final Frame frame = new Frame();
		frame.setTrackNo(trackNo);
		frame.setTimecode(timecode);
		frame.setData(ByteBuffer.wrap(data));
		return frame;
	}

	// ========== default lacing and block timecode ==========

	@Test
	public void testNoLacingKeepsOneFramePerBlock() {
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setData(ByteBuffer.wrap(new byte[] { 1 }));
		Assertions.assertFalse(block.addFrame(frame));
	}

	@Test
	public void testNoLacingKeepsOneFramePerBlockGroup() {
		// A frame with a duration, a subtitle for one: the block is written as a BlockGroup.
		final SimpleBlock block = new SimpleBlock();
		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setDuration(100);
		frame.setData(ByteBuffer.wrap(new byte[] { 1 }));
		Assertions.assertFalse(block.addFrame(frame));
		Assertions.assertEquals(100, block.getDuration());
	}

	@Test
	public void testTimecodeLimitsAreAccepted() {
		final SimpleBlock block = new SimpleBlock();
		block.setTimecode(Short.MAX_VALUE);
		Assertions.assertEquals(Short.MAX_VALUE, block.getTimecode());
		block.setTimecode(Short.MIN_VALUE);
		Assertions.assertEquals(Short.MIN_VALUE, block.getTimecode());
	}

	@Test
	public void testTimecodeOutOfRangeIsRefused() {
		final SimpleBlock block = new SimpleBlock();
		Assertions.assertThrows(IllegalArgumentException.class, () -> block.setTimecode(Short.MAX_VALUE + 1L));
		Assertions.assertThrows(IllegalArgumentException.class, () -> block.setTimecode(Short.MIN_VALUE - 1L));
	}

	// ========== laced blocks read back ==========

	private void assertLacedBlockReadsBack(final LaceMode mode, final int... sizes) {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(mode);
		for (int i = 0; i < sizes.length; i++) {
			final byte[] data = new byte[sizes[i]];
			java.util.Arrays.fill(data, (byte) (i + 1));
			block.addFrame(makeFrame(1, 0, data));
		}

		final Block read = new Block(block.toSimpleBlock().getData());
		read.parseBlock();

		Assertions.assertEquals(sizes.length, read.getFrameCount());
		for (int i = 0; i < sizes.length; i++) {
			final ByteBuffer frame = read.getFrame(i);
			Assertions.assertEquals(sizes[i], frame.remaining(), "Size of frame " + i);
			Assertions.assertEquals((byte) (i + 1), frame.get(), "Content of frame " + i);
		}
	}

	@Test
	public void testEbmlLacingReadsBack() {
		assertLacedBlockReadsBack(LaceMode.EBML, 100, 110, 120);
		assertLacedBlockReadsBack(LaceMode.EBML, 120, 110, 100, 90, 80);
		// Differences on one byte up to 63, on two bytes from 64.
		assertLacedBlockReadsBack(LaceMode.EBML, 200, 263, 199, 1000, 10, 1500, 7, 1530, 3);
	}

	@Test
	public void testXiphLacingReadsBack() {
		assertLacedBlockReadsBack(LaceMode.XIPH, 100, 110, 120);
		assertLacedBlockReadsBack(LaceMode.XIPH, 254, 255, 256, 510, 1530, 1529, 765, 1, 2);
	}

	@Test
	public void testFixedLacingReadsBack() {
		assertLacedBlockReadsBack(LaceMode.FIXED, 50, 50, 50, 50);
	}

	@Test
	public void testFixedLacingRefusesFramesOfDifferentSizes() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.FIXED);
		block.addFrame(makeFrame(1, 0, new byte[2]));
		block.addFrame(makeFrame(1, 0, new byte[3]));
		Assertions.assertThrows(IllegalStateException.class, block::toSimpleBlock);
	}

	@Test
	public void testFrameTooLargeForLacingKeepsTheBlockLaced() {
		final SimpleBlock block = new SimpleBlock();
		block.setLaceMode(LaceMode.XIPH);
		Assertions.assertTrue(block.addFrame(makeFrame(1, 0, new byte[100])));
		Assertions.assertFalse(block.addFrame(makeFrame(1, 0, new byte[2000])));
		Assertions.assertEquals(LaceMode.XIPH, block.getLaceMode());
		assertLacedBlockReadsBack(LaceMode.XIPH, 100, 2000);
	}
}
