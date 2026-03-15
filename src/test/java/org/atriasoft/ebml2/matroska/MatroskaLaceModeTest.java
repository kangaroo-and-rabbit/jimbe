package org.atriasoft.ebml2.matroska;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MatroskaLaceModeTest {

	@Test
	public void testRepresentationValues() {
		Assertions.assertEquals(0, MatroskaLaceMode.NONE.getRepresentation());
		Assertions.assertEquals(1, MatroskaLaceMode.XIPH.getRepresentation());
		Assertions.assertEquals(2, MatroskaLaceMode.FIXED.getRepresentation());
		Assertions.assertEquals(3, MatroskaLaceMode.EBML.getRepresentation());
	}

	@Test
	public void testFromRepresentationValid() {
		Assertions.assertEquals(MatroskaLaceMode.NONE, MatroskaLaceMode.fromRepresentation(0));
		Assertions.assertEquals(MatroskaLaceMode.XIPH, MatroskaLaceMode.fromRepresentation(1));
		Assertions.assertEquals(MatroskaLaceMode.FIXED, MatroskaLaceMode.fromRepresentation(2));
		Assertions.assertEquals(MatroskaLaceMode.EBML, MatroskaLaceMode.fromRepresentation(3));
	}

	@Test
	public void testFromRepresentationInvalid() {
		Assertions.assertNull(MatroskaLaceMode.fromRepresentation(4));
		Assertions.assertNull(MatroskaLaceMode.fromRepresentation(-1));
		Assertions.assertNull(MatroskaLaceMode.fromRepresentation(99));
	}
}
