package org.atriasoft.ebml2;

import java.nio.ByteBuffer;

import org.atriasoft.ebml2.matroska.model.DocTypes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EBMLReaderTest {

	// Force DocTypes class loading so ProtoType.CLASS_MAP is populated
	@SuppressWarnings("unused")
	private static final Object INIT = DocTypes.EBMLVersion;

	@Test
	public void testReadEBMLCodeSize() {
		Assertions.assertEquals(1, EBMLReader.readEBMLCodeSize((byte) 0x80));
		Assertions.assertEquals(1, EBMLReader.readEBMLCodeSize((byte) 0xFF));
		Assertions.assertEquals(2, EBMLReader.readEBMLCodeSize((byte) 0x40));
		Assertions.assertEquals(2, EBMLReader.readEBMLCodeSize((byte) 0x7F));
		Assertions.assertEquals(3, EBMLReader.readEBMLCodeSize((byte) 0x20));
		Assertions.assertEquals(4, EBMLReader.readEBMLCodeSize((byte) 0x10));
		Assertions.assertEquals(5, EBMLReader.readEBMLCodeSize((byte) 0x08));
		Assertions.assertEquals(6, EBMLReader.readEBMLCodeSize((byte) 0x04));
		Assertions.assertEquals(7, EBMLReader.readEBMLCodeSize((byte) 0x02));
		Assertions.assertEquals(8, EBMLReader.readEBMLCodeSize((byte) 0x01));
	}

	@Test
	public void testReadEBMLCodeFromDataSource() {
		final byte[] data = {(byte) 0x81};
		final MockSource src = new MockSource(ByteBuffer.wrap(data));
		Assertions.assertEquals(1, EBMLReader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCode2Bytes() {
		final byte[] data = {(byte) 0x40, (byte) 0x80};
		final MockSource src = new MockSource(ByteBuffer.wrap(data));
		Assertions.assertEquals(128, EBMLReader.readEBMLCode(src));
	}

	@Test
	public void testReadEBMLCodeFromByteBuffer() {
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x83});
		Assertions.assertEquals(3, EBMLReader.readEBMLCode(buf));
	}

	@Test
	public void testBytesToHex() {
		final byte[] data = {(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3};
		final String hex = EBMLReader.bytesToHex(data);
		Assertions.assertEquals("1A45DFA3", hex.toUpperCase());
	}

	@Test
	public void testBytesToHexEmpty() {
		Assertions.assertEquals("", EBMLReader.bytesToHex(new byte[]{}));
	}

	@Test
	public void testReadNextElementReturnsNullOnEmptySource() {
		final MockSource src = new MockSource(ByteBuffer.allocate(0));
		final EBMLReader reader = new EBMLReader(src);
		// Empty source should not produce an element
		// (may throw BufferUnderflowException, which is expected)
		try {
			final Element elem = reader.readNextElement();
			// If it returns, it should be null
			Assertions.assertNull(elem);
		} catch (final java.nio.BufferUnderflowException e) {
			// Expected for empty source
		}
	}

	@Test
	public void testReadNextElementKnownType() {
		// Build a minimal EBML element: EBMLVersion (0x4286) with value 1
		final ByteBuffer buf = ByteBuffer.allocate(32);
		buf.put((byte) 0x42);
		buf.put((byte) 0x86);
		buf.put((byte) 0x81);
		buf.put((byte) 0x01);
		buf.flip();

		final MockSource src = new MockSource(buf);
		final EBMLReader reader = new EBMLReader(src);
		final Element elem = reader.readNextElement();

		Assertions.assertNotNull(elem);
		Assertions.assertNotNull(elem.getElementType());
		Assertions.assertEquals("EBMLVersion", elem.getElementType().getName());
		Assertions.assertEquals(1, elem.getSize());
	}

	@Test
	public void testReadNextElementUnknownTypeSkipped() {
		// Build an element with unknown 1-byte type ID 0xFE, size 2, data 0x00 0x00
		final ByteBuffer buf = ByteBuffer.allocate(32);
		buf.put((byte) 0xFE);
		buf.put((byte) 0x82);
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.flip();

		final MockSource src = new MockSource(buf);
		final EBMLReader reader = new EBMLReader(src);
		final Element elem = reader.readNextElement();

		// Unknown element returns a BinaryElement placeholder
		Assertions.assertNotNull(elem);
		Assertions.assertTrue(elem instanceof BinaryElement);
		Assertions.assertEquals(2, elem.getSize());
	}

	@Test
	public void testParseEBMLCodeValues() {
		// 1-byte: value 42 encoded as just the raw byte
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{42});
		Assertions.assertEquals(42, EBMLReader.parseEBMLCode(buf));
	}

	@Test
	public void testParseEBMLCode2Bytes() {
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{0x01, 0x00});
		Assertions.assertEquals(256, EBMLReader.parseEBMLCode(buf));
	}

	@Test
	public void testReadSignedEBMLCodePositive() {
		// Encode value 5: 1-byte VINT = 0x85, signed offset = 5 - 63 = ?
		// Actually readSignedEBMLCode reads a VINT then subtracts offset.
		// For 1-byte VINT, range is 0..126, offset = 63
		// So to get +5, we need raw=68 -> 0x80|68 = 0xC4
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0xC4});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(5, EBMLReader.readSignedEBMLCode(src));
	}

	@Test
	public void testReadSignedEBMLCodeNegative() {
		// For 1-byte, offset=63. Raw=0 -> signed = 0 - 63 = -63
		final ByteBuffer buf = ByteBuffer.wrap(new byte[]{(byte) 0x80});
		final MockSource src = new MockSource(buf);
		Assertions.assertEquals(-63, EBMLReader.readSignedEBMLCode(src));
	}
}
