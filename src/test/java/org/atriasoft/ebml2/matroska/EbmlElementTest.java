package org.atriasoft.ebml2.matroska;

import org.atriasoft.ebml2.matroska.model.*;

import java.nio.ByteBuffer;
import java.util.Random;

import org.atriasoft.ebml2.EBMLReader;
import org.atriasoft.ebml2.Element;
import org.atriasoft.ebml2.MockSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EbmlElementTest {
	@Test
	public void testEbmlCodedSizes() {
		// TODO: The upper bound here is all that the write-side supports. Should it be better?
		final Random rnd = new Random();
		for (int i = 0; i < 268435455; i += rnd.nextInt(42)) {
			final byte[] encoded = Element.makeEbmlCodedSize(i);
			final long decoded = EBMLReader.readEBMLCodeSize(encoded[0]);
			Assertions.assertEquals(encoded.length, decoded);
		}
	}
	
	@Test
	public void testMakeReadEbmlCode() {
		final ByteBuffer buffer = ByteBuffer.allocate(1024);
		final Random rnd = new Random();
		for (int i = 0; i < 268435455; i += rnd.nextInt(42)) {
			buffer.put(Element.makeEbmlCodedSize(i));
			buffer.flip();
			
			final MockSource src = new MockSource(buffer);
			Assertions.assertEquals(i, EBMLReader.readEBMLCode(src));
			buffer.clear();
		}
	}
	
	@Test
	public void testMakeParseEbmlCode() {
		final ByteBuffer buffer = ByteBuffer.allocate(32);
		final Random rnd = new Random();
		for (int i = 0; i < 268435455; i += rnd.nextInt(42)) {
			final byte[] arr = Element.makeEbmlCodedSize(i);
			arr[0] &= (0xff >>> arr.length);
			buffer.put(arr);
			buffer.flip();
			Assertions.assertEquals(i, EBMLReader.parseEBMLCode(buffer));
			buffer.clear();
		}
	}
}
