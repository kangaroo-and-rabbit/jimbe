package org.ebml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for BinaryElement covering the minSizeLength accessors.
 */
public class BinaryElementTest {

	@Test
	public void testDefaultMinSizeLength() {
		Assertions.assertEquals(4, BinaryElement.getMinSizeLength());
	}

	@Test
	public void testSetMinSizeLength() {
		final int original = BinaryElement.getMinSizeLength();
		try {
			BinaryElement.setMinSizeLength(8);
			Assertions.assertEquals(8, BinaryElement.getMinSizeLength());
		} finally {
			BinaryElement.setMinSizeLength(original);
		}
	}

	@Test
	public void testConstructorWithType() {
		final byte[] typeId = {(byte) 0x1F, (byte) 0xFE, (byte) 0xFA, (byte) 0x01};
		final BinaryElement elem = new BinaryElement(typeId);
		Assertions.assertTrue(elem.isType(typeId));
	}

	@Test
	public void testDefaultConstructor() {
		final BinaryElement elem = new BinaryElement();
		Assertions.assertNotNull(elem);
	}
}
