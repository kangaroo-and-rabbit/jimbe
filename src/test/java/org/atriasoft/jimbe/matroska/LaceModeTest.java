package org.atriasoft.jimbe.matroska;

import org.atriasoft.jimbe.matroska.model.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LaceModeTest {

	@Test
	public void testRepresentationValues() {
		Assertions.assertEquals(0, LaceMode.NONE.getRepresentation());
		Assertions.assertEquals(1, LaceMode.XIPH.getRepresentation());
		Assertions.assertEquals(2, LaceMode.FIXED.getRepresentation());
		Assertions.assertEquals(3, LaceMode.EBML.getRepresentation());
	}

	@Test
	public void testFromRepresentationValid() {
		Assertions.assertEquals(LaceMode.NONE, LaceMode.fromRepresentation(0));
		Assertions.assertEquals(LaceMode.XIPH, LaceMode.fromRepresentation(1));
		Assertions.assertEquals(LaceMode.FIXED, LaceMode.fromRepresentation(2));
		Assertions.assertEquals(LaceMode.EBML, LaceMode.fromRepresentation(3));
	}

	@Test
	public void testFromRepresentationInvalid() {
		Assertions.assertNull(LaceMode.fromRepresentation(4));
		Assertions.assertNull(LaceMode.fromRepresentation(-1));
		Assertions.assertNull(LaceMode.fromRepresentation(99));
	}
}
