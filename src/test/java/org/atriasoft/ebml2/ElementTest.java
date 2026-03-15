package org.atriasoft.ebml2;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElementTest {

	@Test
	public void testCodedSizeLength1Byte() {
		Assertions.assertEquals(1, Element.codedSizeLength(0, 0));
		Assertions.assertEquals(1, Element.codedSizeLength(1, 0));
		Assertions.assertEquals(1, Element.codedSizeLength(126, 0));
	}

	@Test
	public void testCodedSizeLength2Bytes() {
		Assertions.assertEquals(2, Element.codedSizeLength(127, 0));
		Assertions.assertEquals(2, Element.codedSizeLength(16382, 0));
	}

	@Test
	public void testCodedSizeLength3Bytes() {
		Assertions.assertEquals(3, Element.codedSizeLength(16383, 0));
		Assertions.assertEquals(3, Element.codedSizeLength(2097150, 0));
	}

	@Test
	public void testCodedSizeLength4Bytes() {
		Assertions.assertEquals(4, Element.codedSizeLength(2097151, 0));
		Assertions.assertEquals(4, Element.codedSizeLength(268435454, 0));
	}

	@Test
	public void testCodedSizeLengthMinSizeEnforced() {
		Assertions.assertEquals(3, Element.codedSizeLength(1, 3));
		Assertions.assertEquals(4, Element.codedSizeLength(1, 4));
	}

	@Test
	public void testMakeEbmlCodedSizeRoundTrip() {
		final long[] values = {0, 1, 42, 126, 127, 16382, 16383, 2097150, 2097151};
		for (final long v : values) {
			final byte[] encoded = Element.makeEbmlCodedSize(v);
			final ByteBuffer buf = ByteBuffer.wrap(encoded);
			final MockSource src = new MockSource(buf);
			final long decoded = EBMLReader.readEBMLCode(src);
			Assertions.assertEquals(v, decoded, "Round-trip failed for value " + v);
		}
	}

	@Test
	public void testGetMinByteSizeUnsigned() {
		Assertions.assertEquals(8, Element.getMinByteSizeUnsigned(0));
		Assertions.assertEquals(1, Element.getMinByteSizeUnsigned(1));
		Assertions.assertEquals(1, Element.getMinByteSizeUnsigned(0xFF));
		Assertions.assertEquals(2, Element.getMinByteSizeUnsigned(0x100));
		Assertions.assertEquals(8, Element.getMinByteSizeUnsigned(Long.MAX_VALUE));
	}

	@Test
	public void testGetMinByteSize() {
		Assertions.assertEquals(8, Element.getMinByteSize(0));
		Assertions.assertEquals(1, Element.getMinByteSize(1));
		Assertions.assertEquals(1, Element.getMinByteSize(-1));
		Assertions.assertEquals(1, Element.getMinByteSize(42));
		Assertions.assertEquals(1, Element.getMinByteSize(-42));
	}

	@Test
	public void testPackIntUnsignedRoundTrip() {
		final long[] values = {1, 42, 255, 256, 65535, 0xFFFFFFL, 0xFFFFFFFFL};
		for (final long v : values) {
			final byte[] packed = Element.packIntUnsigned(v);
			long reconstructed = 0;
			for (final byte b : packed) {
				reconstructed = (reconstructed << 8) | (b & 0xFF);
			}
			Assertions.assertEquals(v, reconstructed, "Pack unsigned round-trip failed for " + v);
		}
	}

	@Test
	public void testPackIntSpecificSize() {
		final byte[] result = Element.packInt(42, 4);
		Assertions.assertEquals(4, result.length);
		Assertions.assertEquals(0, result[0]);
		Assertions.assertEquals(0, result[1]);
		Assertions.assertEquals(0, result[2]);
		Assertions.assertEquals(42, result[3]);
	}

	@Test
	public void testSetDataUpdatesSize() {
		final Element elem = new BinaryElement();
		elem.setType(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFE, (byte) 0x42});
		final ByteBuffer data = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
		elem.setData(data);
		Assertions.assertEquals(5, elem.getSize());
	}

	@Test
	public void testGetDataReturnsDuplicate() {
		final Element elem = new BinaryElement();
		elem.setType(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFE, (byte) 0x42});
		final ByteBuffer data = ByteBuffer.wrap(new byte[]{1, 2, 3});
		elem.setData(data);
		final ByteBuffer dup = elem.getData();
		dup.get();
		Assertions.assertEquals(0, elem.getData().position());
	}

	@Test
	public void testClearData() {
		final Element elem = new BinaryElement();
		elem.setType(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFE, (byte) 0x42});
		elem.setData(ByteBuffer.wrap(new byte[]{1}));
		elem.clearData();
		Assertions.assertThrows(NullPointerException.class, () -> elem.getData());
	}

	@Test
	public void testIsTypeByteArray() {
		final byte[] typeId = {(byte) 0x1F, (byte) 0xFE, (byte) 0xFD, (byte) 0xA1};
		final Element elem = new BinaryElement(typeId);
		Assertions.assertTrue(elem.isType(typeId));
		Assertions.assertFalse(elem.isType(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFD, (byte) 0xA2}));
	}

	@Test
	public void testIsTypeByteBuffer() {
		final byte[] typeId = {(byte) 0x1F, (byte) 0xFE, (byte) 0xFD, (byte) 0xA1};
		final Element elem = new BinaryElement(typeId);
		Assertions.assertTrue(elem.isType(ByteBuffer.wrap(typeId)));
		Assertions.assertFalse(elem.isType(ByteBuffer.wrap(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFD, (byte) 0xA2})));
	}

	@Test
	public void testGetTotalSizeWithHeaders() {
		final Element elem = new BinaryElement(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFE, (byte) 0x42});
		elem.setData(ByteBuffer.wrap(new byte[]{1, 2, 3}));
		final long totalSize = elem.getTotalSize();
		Assertions.assertTrue(totalSize > 3);
		// 4 bytes type + 1 byte size + 3 bytes data
		Assertions.assertEquals(4 + 1 + 3, totalSize);
	}

	@Test
	public void testGetTotalSizeWithHeadersSizeOverride() {
		final Element elem = new BinaryElement(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFE, (byte) 0x42});
		elem.setData(ByteBuffer.wrap(new byte[]{1, 2, 3}));
		elem.setHeadersSize(10);
		Assertions.assertEquals(10 + 3, elem.getTotalSize());
	}

	@Test
	public void testSetSizeClearsHeadersSize() {
		final Element elem = new BinaryElement(new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFE, (byte) 0x42});
		elem.setData(ByteBuffer.wrap(new byte[]{1, 2, 3}));
		elem.setHeadersSize(10);
		elem.setSize(5);
		final long totalSize = elem.getTotalSize();
		// 4 bytes type + 1 byte size + 5 bytes data
		Assertions.assertEquals(4 + 1 + 5, totalSize);
	}

	@Test
	public void testWriteAndReadBack() {
		final ProtoType<BinaryElement> typeInfo = new ProtoType<>(BinaryElement.class, "testBin_ET",
				new byte[]{(byte) 0x1F, (byte) 0xFE, (byte) 0xFD, (byte) 0xFC}, 1);
		final BinaryElement elem = typeInfo.getInstance();
		elem.setData(ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03}));

		final MockWriter writer = new MockWriter();
		elem.writeElement(writer);

		final ByteBuffer written = writer.getBuff();
		written.flip();

		// 4-byte type ID
		Assertions.assertEquals((byte) 0x1F, written.get());
		Assertions.assertEquals((byte) 0xFE, written.get());
		Assertions.assertEquals((byte) 0xFD, written.get());
		Assertions.assertEquals((byte) 0xFC, written.get());
		Assertions.assertEquals(3, EBMLReader.readEBMLCode(written));
		Assertions.assertEquals(0x01, written.get());
		Assertions.assertEquals(0x02, written.get());
		Assertions.assertEquals(0x03, written.get());
	}

	@Test
	public void testParentAccessors() {
		final Element parent = new BinaryElement(new byte[]{(byte) 0x01});
		final Element child = new BinaryElement(new byte[]{(byte) 0x02});
		child.setParent(parent);
		Assertions.assertSame(parent, child.getParent());
	}
}
