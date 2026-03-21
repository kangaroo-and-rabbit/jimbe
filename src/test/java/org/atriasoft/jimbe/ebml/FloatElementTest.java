package org.atriasoft.jimbe.ebml;

import org.atriasoft.jimbe.ebml.model.FloatElement;
import org.atriasoft.jimbe.ebml.model.ProtoType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FloatElementTest {

	// Use 4-byte IDs in unused range to avoid collision with DocTypes
	private static final byte[] FLOAT_ID_1 = { (byte) 0x1F, (byte) 0xFE, (byte) 0xF0, (byte) 0x01 };
	private static final byte[] FLOAT_ID_2 = { (byte) 0x1F, (byte) 0xFE, (byte) 0xF0, (byte) 0x02 };
	private static final byte[] FLOAT_ID_3 = { (byte) 0x1F, (byte) 0xFE, (byte) 0xF0, (byte) 0x03 };

	@Test
	public void testFloatRangeValue() {
		final FloatElement elem = new FloatElement();
		elem.setType(FLOAT_ID_1);
		elem.setValue(3.14);
		Assertions.assertEquals(4, elem.getSize());
		Assertions.assertEquals(3.14, elem.getValue(), 0.01);
	}

	@Test
	public void testZeroValue() {
		final FloatElement elem = new FloatElement();
		elem.setType(FLOAT_ID_1);
		elem.setValue(0.0);
		Assertions.assertEquals(4, elem.getSize());
		Assertions.assertEquals(0.0, elem.getValue(), 0.0001);
	}

	@Test
	public void testNegativeValue() {
		final FloatElement elem = new FloatElement();
		elem.setType(FLOAT_ID_1);
		elem.setValue(-42.5);
		Assertions.assertEquals(4, elem.getSize());
		Assertions.assertEquals(-42.5, elem.getValue(), 0.01);
	}

	@Test
	public void testSmallFloatValue() {
		final FloatElement elem = new FloatElement();
		elem.setType(FLOAT_ID_1);
		elem.setValue(0.001f);
		Assertions.assertEquals(4, elem.getSize());
		Assertions.assertEquals(0.001, elem.getValue(), 0.0001);
	}

	@Test
	public void testWriteAndReadFloat() {
		final ProtoType<FloatElement> typeInfo = new ProtoType<>(FloatElement.class, "testFloat_FET", FLOAT_ID_2, 1);
		final FloatElement elem = typeInfo.getInstance();
		elem.setValue(44100.0);

		final MockWriter writer = new MockWriter();
		elem.writeData(writer);

		Assertions.assertEquals(4, writer.getBuff().position());
	}

	@Test
	public void testConstructorWithType() {
		final FloatElement elem = new FloatElement(FLOAT_ID_3);
		elem.setValue(48000.0);
		Assertions.assertEquals(48000.0, elem.getValue(), 0.1);
	}
}
