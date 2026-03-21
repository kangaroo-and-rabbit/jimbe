package org.atriasoft.jimbe.matroska;

import java.nio.ByteBuffer;
import java.util.Random;

import org.atriasoft.jimbe.ebml.MockSource;
import org.atriasoft.jimbe.ebml.Reader;
import org.atriasoft.jimbe.ebml.model.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EbmlElementTest {
	@Test
	public void testEbmlCodedSizes() {
		// TODO: The upper bound here is all that the write-side supports. Should it be better?
		final Random rnd = new Random();
		for (int i = 0; i < 268435455; i += rnd.nextInt(42)) {
			final byte[] encoded = Element.makeEbmlCodedSize(i);
			final long decoded = Reader.readEBMLCodeSize(encoded[0]);
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
			Assertions.assertEquals(i, Reader.readEBMLCode(src));
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
			Assertions.assertEquals(i, Reader.parseEBMLCode(buffer));
			buffer.clear();
		}
	}
}
