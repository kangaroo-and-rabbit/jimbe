package org.atriasoft.jimbe.matroska;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.matroska.model.Block;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for Block parsing covering the Matroska Block/SimpleBlock structure
 * as defined in https://www.matroska.org/technical/basics.html
 *
 * Block structure: TrackNumber (VINT) | Timecode (int16) | Flags (byte) | [LaceCount] | [LaceSizes] | FrameData
 */
public class BlockTest {

	/**
	 * Build a minimal block with no lacing:
	 * - Track number as 1-byte VINT (value | 0x80)
	 * - Timecode as signed int16
	 * - Flags byte (keyframe=bit7, lacing=bits1-2)
	 * - Frame data
	 */
	private ByteBuffer buildSimpleBlock(
			final int trackNo,
			final short timecode,
			final boolean keyFrame,
			final byte[] frameData) {
		final ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 1 + frameData.length);
		buf.put((byte) (trackNo | 0x80));
		buf.putShort(timecode);
		final byte flags = (byte) (keyFrame ? 0x80 : 0x00);
		buf.put(flags);
		buf.put(frameData);
		buf.flip();
		return buf;
	}

	@Test
	public void testParseSimpleBlockNoLacing() {
		final byte[] frameData = { 0x01, 0x02, 0x03, 0x04, 0x05 };
		final ByteBuffer blockData = buildSimpleBlock(1, (short) 0, true, frameData);

		final Block block = new Block(blockData);
		block.parseBlock();

		Assertions.assertEquals(1, block.getTrackNo());
		Assertions.assertEquals(0, block.getBlockTimecode());
		Assertions.assertTrue(block.isKeyFrame());
		Assertions.assertEquals(1, block.getFrameCount());

		final ByteBuffer frame = block.getFrame(0);
		Assertions.assertEquals(5, frame.remaining());
	}

	@Test
	public void testParseBlockNonKeyFrame() {
		final byte[] frameData = { 0x0A, 0x0B };
		final ByteBuffer blockData = buildSimpleBlock(1, (short) 100, false, frameData);

		final Block block = new Block(blockData);
		block.parseBlock();

		Assertions.assertFalse(block.isKeyFrame());
		Assertions.assertEquals(100, block.getBlockTimecode());
	}

	@Test
	public void testParseBlockNegativeTimecode() {
		// Per Matroska spec, block timecode is a signed int16 relative to cluster timecode
		final byte[] frameData = { 0x01 };
		final ByteBuffer blockData = buildSimpleBlock(1, (short) -50, true, frameData);

		final Block block = new Block(blockData);
		block.parseBlock();

		Assertions.assertEquals(-50, block.getBlockTimecode());
	}

	@Test
	public void testParseBlockMultipleTracks() {
		// Track number 5
		final byte[] frameData = { 0x01, 0x02 };
		final ByteBuffer blockData = buildSimpleBlock(5, (short) 0, true, frameData);

		final Block block = new Block(blockData);
		block.parseBlock();

		Assertions.assertEquals(5, block.getTrackNo());
	}

	@Test
	public void testParseFixedSizeLacing() {
		// Fixed-size lacing: all frames same size
		// Flags: keyframe=1, lacing mode=fixed (0x04)
		final int trackNo = 1;
		final int numFrames = 3;
		final byte[] frameData = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };

		final ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 1 + 1 + frameData.length);
		buf.put((byte) (trackNo | 0x80));
		buf.putShort((short) 0);
		buf.put((byte) (0x80 | 0x04)); // keyframe + fixed lacing
		buf.put((byte) (numFrames - 1)); // lace count = numFrames - 1
		buf.put(frameData);
		buf.flip();

		final Block block = new Block(buf);
		block.parseBlock();

		Assertions.assertEquals(numFrames, block.getFrameCount());
		// Each frame should be 2 bytes (6 / 3)
		for (int i = 0; i < numFrames; i++) {
			final ByteBuffer frame = block.getFrame(i);
			Assertions.assertEquals(2, frame.remaining(), "Frame " + i + " size mismatch");
		}
	}

	@Test
	public void testParseXiphLacing() {
		// Xiph lacing: frame sizes encoded as sum of 255-valued bytes + terminator
		final int trackNo = 1;
		final byte[] frame0Data = { 0x01, 0x02, 0x03 }; // 3 bytes
		final byte[] frame1Data = { 0x04, 0x05 }; // 2 bytes
		final byte[] frame2Data = { 0x06, 0x07, 0x08, 0x09 }; // 4 bytes (last, deduced)

		final ByteBuffer buf = ByteBuffer
				.allocate(1 + 2 + 1 + 1 + 2 + frame0Data.length + frame1Data.length + frame2Data.length);
		buf.put((byte) (trackNo | 0x80));
		buf.putShort((short) 0);
		buf.put((byte) (0x80 | 0x02)); // keyframe + xiph lacing
		buf.put((byte) 2); // lace count = 2 means 3 frames
		// Frame 0 size = 3 (encoded as single byte < 255)
		buf.put((byte) 3);
		// Frame 1 size = 2
		buf.put((byte) 2);
		// Frame 2 size is deduced from remaining
		buf.put(frame0Data);
		buf.put(frame1Data);
		buf.put(frame2Data);
		buf.flip();

		final Block block = new Block(buf);
		block.parseBlock();

		Assertions.assertEquals(3, block.getFrameCount());
		// Verify non-last frames are correctly sized
		Assertions.assertEquals(3, block.getFrame(0).remaining());
		Assertions.assertEquals(2, block.getFrame(1).remaining());
		Assertions.assertEquals(4, block.getFrame(2).remaining());
		Assertions.assertEquals(0x06, block.getFrame(2).get());
	}

	@Test
	public void testParseXiphLacingLargeFrame() {
		// Xiph lacing with frame size >= 255 (encoded as 0xFF + remainder)
		final int trackNo = 1;
		final byte[] frame0Data = new byte[300]; // 300 bytes
		final byte[] frame1Data = new byte[10]; // 10 bytes (last, deduced)

		final ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 1 + 1 + 3 + frame0Data.length + frame1Data.length);
		buf.put((byte) (trackNo | 0x80));
		buf.putShort((short) 0);
		buf.put((byte) (0x80 | 0x02)); // keyframe + xiph lacing
		buf.put((byte) 1); // lace count = 1 means 2 frames
		// Frame 0 size = 300: 0xFF (255) + 0x2D (45)
		buf.put((byte) 0xFF);
		buf.put((byte) 45);
		buf.put(frame0Data);
		buf.put(frame1Data);
		buf.flip();

		final Block block = new Block(buf);
		block.parseBlock();

		Assertions.assertEquals(2, block.getFrameCount());
		// First frame correctly parsed at 300 bytes
		Assertions.assertEquals(300, block.getFrame(0).remaining());
		Assertions.assertEquals(10, block.getFrame(1).remaining());
	}

	@Test
	public void testParseEBMLLacing() {
		// EBML lacing: first frame size as unsigned VINT, subsequent as signed delta
		final int trackNo = 1;
		final byte[] frame0Data = { 0x01, 0x02, 0x03 }; // 3 bytes
		final byte[] frame1Data = { 0x04, 0x05, 0x06 }; // 3 bytes (delta = 0 from first)
		final byte[] frame2Data = { 0x07, 0x08 }; // 2 bytes (last, deduced)

		final ByteBuffer buf = ByteBuffer.allocate(64);
		buf.put((byte) (trackNo | 0x80));
		buf.putShort((short) 0);
		buf.put((byte) (0x80 | 0x06)); // keyframe + EBML lacing
		buf.put((byte) 2); // lace count = 2 means 3 frames
		// First frame size = 3 as EBML VINT: 0x83
		buf.put((byte) 0x83);
		// Second frame size delta = 0 (3 - 3 = 0): signed VINT with offset 63 -> raw = 63 -> 0x80|63 = 0xBF
		buf.put((byte) 0xBF);
		buf.put(frame0Data);
		buf.put(frame1Data);
		buf.put(frame2Data);
		buf.flip();

		final Block block = new Block(buf);
		block.parseBlock();

		Assertions.assertEquals(3, block.getFrameCount());
		// Non-last frames are correctly parsed
		Assertions.assertEquals(3, block.getFrame(0).remaining());
		Assertions.assertEquals(3, block.getFrame(1).remaining());
		Assertions.assertEquals(2, block.getFrame(2).remaining());
		Assertions.assertEquals(0x07, block.getFrame(2).get());
	}

	@Test
	public void testParseEBMLLacingNegativeDelta() {
		final ByteBuffer buf = ByteBuffer.allocate(16);
		buf.put((byte) 0x81);
		buf.putShort((short) 0);
		buf.put((byte) (0x80 | 0x06)); // keyframe + EBML lacing
		buf.put((byte) 2); // 3 frames
		buf.put((byte) 0x83); // first frame: 3 bytes
		buf.put((byte) (0x80 | (63 - 2))); // second frame: 3 - 2 = 1 byte
		buf.put(new byte[] { 1, 2, 3, 4, 5, 6 });
		buf.flip();

		final Block block = new Block(buf);
		block.parseBlock();

		Assertions.assertEquals(3, block.getFrame(0).remaining());
		Assertions.assertEquals(1, block.getFrame(1).remaining());
		Assertions.assertEquals(2, block.getFrame(2).remaining());
		Assertions.assertEquals(5, block.getFrame(2).get());
	}

	@Test
	public void testParseLacingWithOneFrame() {
		// A count of 0 means one frame: no size is stored, it takes the whole payload.
		for (final int laceFlag : new int[] { 0x02, 0x04, 0x06 }) {
			final ByteBuffer buf = ByteBuffer.allocate(8);
			buf.put((byte) 0x81);
			buf.putShort((short) 0);
			buf.put((byte) (0x80 | laceFlag));
			buf.put((byte) 0);
			buf.put(new byte[] { 1, 2, 3 });
			buf.flip();

			final Block block = new Block(buf);
			block.parseBlock();

			Assertions.assertEquals(1, block.getFrameCount());
			Assertions.assertEquals(3, block.getFrame(0).remaining());
		}
	}

	@Test
	public void testLaceCountIsUnsigned() {
		// 200 frames of one byte: the count, 199, does not fit a signed byte.
		final int frames = 200;
		final ByteBuffer buf = ByteBuffer.allocate(5 + frames);
		buf.put((byte) 0x81);
		buf.putShort((short) 0);
		buf.put((byte) (0x80 | 0x04)); // keyframe + fixed lacing
		buf.put((byte) (frames - 1));
		for (int i = 0; i < frames; i++) {
			buf.put((byte) i);
		}
		buf.flip();

		final Block block = new Block(buf);
		block.parseBlock();

		Assertions.assertEquals(frames, block.getFrameCount());
		Assertions.assertEquals((byte) 199, block.getFrame(199).get());
	}

	@Test
	public void testLacedBlockInsideALargerBuffer() {
		// The frames are found from the position of the block, not from the start of the buffer.
		final ByteBuffer buf = ByteBuffer.allocate(16);
		buf.position(4);
		buf.put((byte) 0x81);
		buf.putShort((short) 0);
		buf.put((byte) (0x80 | 0x02)); // keyframe + xiph lacing
		buf.put((byte) 1); // 2 frames
		buf.put((byte) 2); // first frame: 2 bytes
		buf.put(new byte[] { 1, 2, 3 });
		buf.flip();
		buf.position(4);

		final Block block = new Block(buf);
		block.parseBlock();

		Assertions.assertEquals(2, block.getFrame(0).remaining());
		Assertions.assertEquals(1, block.getFrame(1).remaining());
		Assertions.assertEquals(3, block.getFrame(1).get());
	}

	@Test
	public void testGetAdjustedBlockTimecode() {
		final byte[] frameData = { 0x01 };
		final ByteBuffer blockData = buildSimpleBlock(1, (short) 33, true, frameData);

		final Block block = new Block(blockData);
		block.parseBlock();

		// Per the source: clusterTimecode + blockTimecode (timecodeScale is not used)
		Assertions.assertEquals(1033, block.getAdjustedBlockTimecode(1000, 1000000));
	}

	@Test
	public void testGetFrameThrowsForNonLacedBlock() {
		final byte[] frameData = { 0x01, 0x02 };
		final ByteBuffer blockData = buildSimpleBlock(1, (short) 0, true, frameData);

		final Block block = new Block(blockData);
		block.parseBlock();

		// Frame 0 is valid
		Assertions.assertNotNull(block.getFrame(0));
		// Frame 1 should throw for non-laced block
		Assertions.assertThrows(IllegalArgumentException.class, () -> block.getFrame(1));
	}

	@Test
	public void testBlockTimecodeMinMax() {
		// Test with min and max signed int16 values
		final byte[] frameData = { 0x01 };

		final ByteBuffer minBlock = buildSimpleBlock(1, Short.MIN_VALUE, true, frameData);
		final Block blockMin = new Block(minBlock);
		blockMin.parseBlock();
		Assertions.assertEquals(Short.MIN_VALUE, blockMin.getBlockTimecode());

		final ByteBuffer maxBlock = buildSimpleBlock(1, Short.MAX_VALUE, true, frameData);
		final Block blockMax = new Block(maxBlock);
		blockMax.parseBlock();
		Assertions.assertEquals(Short.MAX_VALUE, blockMax.getBlockTimecode());
	}

	@Test
	public void testParseBlockLeavesTheCallerBufferUntouched() {
		final ByteBuffer blockData = buildSimpleBlock(1, (short) 0, true, new byte[] { 0x01, 0x02 });
		final int position = blockData.position();
		final Block block = new Block(blockData);
		block.parseBlock();
		Assertions.assertEquals(position, blockData.position());
	}
}
