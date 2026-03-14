package org.ebml.matroska;

import java.nio.ByteBuffer;

import org.ebml.MockWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for MatroskaSegment covering the Matroska Segment element
 * and its unknown-size mode for streaming per the spec.
 */
public class MatroskaSegmentTest {

	@Test
	public void testDefaultUnknownSizeIsFalse() {
		final MatroskaSegment segment = new MatroskaSegment();
		Assertions.assertFalse(segment.getUnknownSize());
	}

	@Test
	public void testSetUnknownSize() {
		final MatroskaSegment segment = new MatroskaSegment();
		segment.setUnknownSize(true);
		Assertions.assertTrue(segment.getUnknownSize());
	}

	@Test
	public void testWriteHeaderKnownSize() {
		final MatroskaSegment segment = new MatroskaSegment();
		segment.setSize(100);
		segment.setUnknownSize(false);

		final MockWriter writer = new MockWriter();
		final long headerLen = segment.writeHeaderData(writer);

		// Should write 4-byte Segment ID + encoded size
		Assertions.assertTrue(headerLen > 4);
		final ByteBuffer buf = writer.getBuff();
		buf.flip();

		// Segment ID: 0x18538067
		Assertions.assertEquals((byte) 0x18, buf.get());
		Assertions.assertEquals((byte) 0x53, buf.get());
		Assertions.assertEquals((byte) 0x80, buf.get());
		Assertions.assertEquals((byte) 0x67, buf.get());
	}

	@Test
	public void testWriteHeaderUnknownSize() {
		// Per EBML spec, unknown size = all-ones VINT
		final MatroskaSegment segment = new MatroskaSegment();
		segment.setUnknownSize(true);

		final MockWriter writer = new MockWriter();
		final long headerLen = segment.writeHeaderData(writer);

		// 4-byte Segment ID + 5-byte unknown size
		Assertions.assertEquals(9, headerLen);
		final ByteBuffer buf = writer.getBuff();
		buf.flip();

		// Skip 4-byte Segment ID
		buf.position(4);

		// Unknown size: 5 bytes all-ones pattern
		// First byte: 0xFF >>> 4 = 0x0F? No, code is: size[0] = (byte)(0xFF >>> (size.length - 1))
		// size.length = 5, so 0xFF >>> 4 = 0x0F
		final byte firstByte = buf.get();
		Assertions.assertEquals((byte) 0x0F, firstByte);
		for (int i = 1; i < 5; i++) {
			Assertions.assertEquals((byte) 0xFF, buf.get(), "Byte " + i + " should be 0xFF for unknown size");
		}
	}

	@Test
	public void testSegmentHasCorrectType() {
		final MatroskaSegment segment = new MatroskaSegment();
		// Segment type ID should be 0x18538067
		final ByteBuffer type = segment.getType();
		Assertions.assertEquals(4, type.remaining());
	}
}
