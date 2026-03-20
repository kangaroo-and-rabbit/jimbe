package org.atriasoft.jimbe.ebml;

import org.atriasoft.jimbe.ebml.model.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for EBML Variable-Size Integer (VINT) encoding and decoding
 * based on the EBML specification (RFC 8794) and Matroska docs.
 *
 * VINT format: Leading zeros count determines width. First byte's leading 1-bit
 * is the width marker, remaining bits + subsequent bytes form the value.
 */
public class VintTest {

	// ============ VINT Size Detection ============

	@Test
	public void testReadEBMLCodeSizeAllWidths() {
		// Width marker is determined by position of first 1-bit
		Assertions.assertEquals(1, Reader.readEBMLCodeSize((byte) 0x80)); // 1xxxxxxx
		Assertions.assertEquals(1, Reader.readEBMLCodeSize((byte) 0xFF)); // 11111111
		Assertions.assertEquals(2, Reader.readEBMLCodeSize((byte) 0x40)); // 01xxxxxx
		Assertions.assertEquals(2, Reader.readEBMLCodeSize((byte) 0x7F)); // 01111111
		Assertions.assertEquals(3, Reader.readEBMLCodeSize((byte) 0x20)); // 001xxxxx
		Assertions.assertEquals(3, Reader.readEBMLCodeSize((byte) 0x3F)); // 00111111
		Assertions.assertEquals(4, Reader.readEBMLCodeSize((byte) 0x10)); // 0001xxxx
		Assertions.assertEquals(4, Reader.readEBMLCodeSize((byte) 0x1F)); // 00011111
		Assertions.assertEquals(5, Reader.readEBMLCodeSize((byte) 0x08)); // 00001xxx
		Assertions.assertEquals(5, Reader.readEBMLCodeSize((byte) 0x0F)); // 00001111
		Assertions.assertEquals(6, Reader.readEBMLCodeSize((byte) 0x04)); // 000001xx
		Assertions.assertEquals(6, Reader.readEBMLCodeSize((byte) 0x07)); // 00000111
		Assertions.assertEquals(7, Reader.readEBMLCodeSize((byte) 0x02)); // 0000001x
		Assertions.assertEquals(7, Reader.readEBMLCodeSize((byte) 0x03)); // 00000011
		Assertions.assertEquals(8, Reader.readEBMLCodeSize((byte) 0x01)); // 00000001
	}

	// ============ 1-byte VINT values ============

	@Test
	public void testReadEBMLCode1ByteMin() {
		// Value 0: 0x80 = 10000000 -> value = 0
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x80});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(0, Reader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode1ByteMax() {
		// Max 1-byte value = 126: 0xFE = 11111110
		// 0xFF = all-ones is reserved ("unknown size")
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0xFE});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(126, Reader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode1ByteValue1() {
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x81});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(1, Reader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode1ByteValue42() {
		// 42 = 0x2A, with width marker: 0x80 | 0x2A = 0xAA
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0xAA});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(42, Reader.readEBMLCode(src));
	}

	// ============ 2-byte VINT values ============

	@Test
	public void testReadEBMLCode2ByteMin() {
		// Value 0 in 2 bytes: 0x4000
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x40, (byte) 0x00});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(0, Reader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode2ByteValue128() {
		// Value 128: 0x40 | (128 >> 8) = 0x40, 128 & 0xFF = 0x80
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x40, (byte) 0x80});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(128, Reader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode2ByteMax() {
		// Max 2-byte value = 2^14 - 2 = 16382: 0x7FFE
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x7F, (byte) 0xFE});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(16382, Reader.readEBMLCode(src));
	}

	// ============ 3-byte VINT values ============

	@Test
	public void testReadEBMLCode3ByteMin() {
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x20, (byte) 0x00, (byte) 0x00});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(0, Reader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode3ByteMax() {
		// Max 3-byte value = 2^21 - 2 = 2097150: 0x3F 0xFF 0xFE
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x3F, (byte) 0xFF, (byte) 0xFE});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(2097150, Reader.readEBMLCode(src));
	}

	// ============ 4-byte VINT values ============

	@Test
	public void testReadEBMLCode4ByteMin() {
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(0, Reader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode4ByteMax() {
		// Max 4-byte value = 2^28 - 2 = 268435454: 0x1F 0xFF 0xFF 0xFE
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x1F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(268435454, Reader.readEBMLCode(src));
	}

	// ============ makeEbmlCodedSize tests ============

	@Test
	public void testMakeEbmlCodedSize1Byte() {
		final byte[] encoded = Element.makeEbmlCodedSize(0);
		Assertions.assertEquals(1, encoded.length);
		Assertions.assertEquals((byte) 0x80, encoded[0]);
	}

	@Test
	public void testMakeEbmlCodedSize1ByteMax() {
		final byte[] encoded = Element.makeEbmlCodedSize(126);
		Assertions.assertEquals(1, encoded.length);
		Assertions.assertEquals((byte) 0xFE, encoded[0]);
	}

	@Test
	public void testMakeEbmlCodedSize2ByteBoundary() {
		// Value 127 requires 2 bytes
		final byte[] encoded = Element.makeEbmlCodedSize(127);
		Assertions.assertEquals(2, encoded.length);
	}

	@Test
	public void testMakeEbmlCodedSize3ByteBoundary() {
		// Value 16383 requires 3 bytes
		final byte[] encoded = Element.makeEbmlCodedSize(16383);
		Assertions.assertEquals(3, encoded.length);
	}

	@Test
	public void testMakeEbmlCodedSize4ByteBoundary() {
		// Value 2097151 requires 4 bytes
		final byte[] encoded = Element.makeEbmlCodedSize(2097151);
		Assertions.assertEquals(4, encoded.length);
	}

	// ============ Round-trip encoding/decoding ============

	@Test
	public void testVintRoundTripAllBoundaries() {
		final long[] boundaryValues = {
			0, 1, 126,           // 1-byte boundaries
			127, 16382,          // 2-byte boundaries
			16383, 2097150,      // 3-byte boundaries
			2097151, 268435454   // 4-byte boundaries
		};
		for (final long v : boundaryValues) {
			final byte[] encoded = Element.makeEbmlCodedSize(v);
			final ByteBuffer buf = ByteBuffer.wrap(encoded);
			final MockSource src = new MockSource(buf);
			final long decoded = Reader.readEBMLCode(src);
			Assertions.assertEquals(v, decoded, "Round-trip failed for value " + v);
		}
	}

	@Test
	public void testVintRoundTripFromByteBuffer() {
		final long[] values = {0, 1, 42, 127, 1000, 16383, 100000, 2097151};
		for (final long v : values) {
			final byte[] encoded = Element.makeEbmlCodedSize(v);
			final ByteBuffer buf = ByteBuffer.wrap(encoded);
			final long decoded = Reader.readEBMLCode(buf);
			Assertions.assertEquals(v, decoded, "ByteBuffer round-trip failed for value " + v);
		}
	}

	// ============ codedSizeLength tests ============

	@Test
	public void testCodedSizeLengthBoundaries() {
		// Verify codedSizeLength returns correct byte count
		Assertions.assertEquals(1, Element.codedSizeLength(0, 0));
		Assertions.assertEquals(1, Element.codedSizeLength(126, 0));
		Assertions.assertEquals(2, Element.codedSizeLength(127, 0));
		Assertions.assertEquals(2, Element.codedSizeLength(16382, 0));
		Assertions.assertEquals(3, Element.codedSizeLength(16383, 0));
		Assertions.assertEquals(3, Element.codedSizeLength(2097150, 0));
		Assertions.assertEquals(4, Element.codedSizeLength(2097151, 0));
		Assertions.assertEquals(4, Element.codedSizeLength(268435454, 0));
	}

	@Test
	public void testCodedSizeLengthMinSizeRespected() {
		// Even small values should use at least minSize bytes
		Assertions.assertEquals(2, Element.codedSizeLength(1, 2));
		Assertions.assertEquals(3, Element.codedSizeLength(1, 3));
		Assertions.assertEquals(4, Element.codedSizeLength(1, 4));
	}

	// ============ Signed VINT tests ============

	@Test
	public void testSignedEBMLCodeZero() {
		// For 1-byte signed VINT, offset = 63
		// To get value 0: raw = 63, encoded = 0x80|63 = 0xBF
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0xBF});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(0, Reader.readSignedEBMLCode(src));
	}

	@Test
	public void testSignedEBMLCodePositive() {
		// For 1-byte, offset=63. Value +10: raw=73 -> 0x80|73 = 0xC9
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0xC9});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(10, Reader.readSignedEBMLCode(src));
	}

	@Test
	public void testSignedEBMLCodeNegative() {
		// For 1-byte, offset=63. Value -63: raw=0 -> 0x80|0 = 0x80
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x80});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(-63, Reader.readSignedEBMLCode(src));
	}

	@Test
	public void testSignedEBMLCodeMax1Byte() {
		// For 1-byte, offset=63. Max value = 126-63 = 63: raw=126 -> 0x80|126 = 0xFE
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0xFE});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(63, Reader.readSignedEBMLCode(src));
	}

	// ============ parseEBMLCode tests ============

	@Test
	public void testParseEBMLCodeSingleByte() {
		// parseEBMLCode reads raw bytes without VINT markers
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{42});
		Assertions.assertEquals(42, Reader.parseEBMLCode(buf));
	}

	@Test
	public void testParseEBMLCode2Bytes() {
		// Value 256 = 0x01 0x00
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{0x01, 0x00});
		Assertions.assertEquals(256, Reader.parseEBMLCode(buf));
	}

	@Test
	public void testParseEBMLCode4Bytes() {
		// Known Matroska element ID: EBML header = 0x1A45DFA3
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3});
		final long result = Reader.parseEBMLCode(buf);
		Assertions.assertEquals(0x1A45DFA3L, result);
	}

	// ============ bytesToHex tests ============

	@Test
	public void testBytesToHexEBMLHeader() {
		final byte[] data = {(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3};
		final String hex = Reader.bytesToHex(data);
		Assertions.assertEquals("1A45DFA3", hex.toUpperCase());
	}

	@Test
	public void testBytesToHexSingleByte() {
		Assertions.assertEquals("FF", Reader.bytesToHex(new byte[]{(byte) 0xFF}).toUpperCase());
		Assertions.assertEquals("00", Reader.bytesToHex(new byte[]{(byte) 0x00}).toUpperCase());
	}

	@Test
	public void testBytesToHexEmpty() {
		Assertions.assertEquals("", Reader.bytesToHex(new byte[]{}));
	}
}
