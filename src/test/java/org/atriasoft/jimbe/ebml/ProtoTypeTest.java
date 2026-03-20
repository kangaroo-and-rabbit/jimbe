package org.atriasoft.jimbe.ebml;

import org.atriasoft.jimbe.ebml.model.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProtoTypeTest {

	// Use 4-byte IDs in an unused range to avoid collision with DocTypes
	private static final byte[] TEST_ID_1 = {(byte) 0x1F, (byte) 0xFF, (byte) 0xFE, (byte) 0x01};
	private static final byte[] TEST_ID_2 = {(byte) 0x1F, (byte) 0xFF, (byte) 0xFE, (byte) 0x02};
	private static final byte[] TEST_ID_3 = {(byte) 0x1F, (byte) 0xFF, (byte) 0xFE, (byte) 0x03};
	private static final byte[] TEST_ID_4 = {(byte) 0x1F, (byte) 0xFF, (byte) 0xFE, (byte) 0x04};
	private static final byte[] TEST_ID_5 = {(byte) 0x1F, (byte) 0xFF, (byte) 0xFE, (byte) 0x05};

	@Test
	public void testGetInstanceReturnsCorrectType() {
		final ProtoType<UnsignedIntegerElement> proto = new ProtoType<>(UnsignedIntegerElement.class,
				"TestUint_PT1", TEST_ID_1, 3);
		final UnsignedIntegerElement elem = proto.getInstance();
		Assertions.assertNotNull(elem);
		Assertions.assertEquals("TestUint_PT1", elem.getElementType().getName());
		Assertions.assertEquals(3, elem.getElementType().getLevel());
	}

	@Test
	public void testStaticGetInstanceReturnsNullForUnknown() {
		final ByteBuffer unknownType = ByteBuffer.wrap(new byte[]{(byte) 0x1F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
		final Element elem = ProtoType.getInstance(unknownType);
		Assertions.assertNull(elem);
	}

	@Test
	public void testGetName() {
		final ProtoType<BinaryElement> proto = new ProtoType<>(BinaryElement.class,
				"TestBin_PT2", TEST_ID_2, 1);
		Assertions.assertEquals("TestBin_PT2", proto.getName());
	}

	@Test
	public void testGetLevel() {
		final ProtoType<BinaryElement> proto = new ProtoType<>(BinaryElement.class,
				"TestBin_PT3", TEST_ID_3, 5);
		Assertions.assertEquals(5, proto.getLevel());
	}

	@Test
	public void testGetType() {
		final ProtoType<BinaryElement> proto = new ProtoType<>(BinaryElement.class,
				"TestBin_PT4", TEST_ID_4, 0);
		final ByteBuffer result = proto.getType();
		Assertions.assertEquals(4, result.remaining());
	}

	@Test
	public void testInstanceHasCorrectTypeBytes() {
		final ProtoType<BinaryElement> proto = new ProtoType<>(BinaryElement.class,
				"TestBin_PT5", TEST_ID_5, 0);
		final BinaryElement elem = proto.getInstance();
		Assertions.assertTrue(elem.isType(TEST_ID_5));
	}
}
