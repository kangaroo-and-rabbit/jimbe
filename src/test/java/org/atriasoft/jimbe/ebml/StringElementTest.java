package org.atriasoft.jimbe.ebml;

import org.atriasoft.jimbe.ebml.model.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringElementTest {

	// Use 4-byte IDs in unused range to avoid collision with DocTypes
	private static final byte[] STR_ID_1 = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF1, (byte) 0x01};
	private static final byte[] STR_ID_2 = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF1, (byte) 0x02};
	private static final byte[] STR_ID_3 = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF1, (byte) 0x03};

	@Test
	public void testDefaultEncodingIsAscii() {
		final StringElement elem = new StringElement();
		Assertions.assertEquals(StandardCharsets.US_ASCII, elem.getEncoding());
	}

	@Test
	public void testAsciiRoundTrip() {
		final StringElement elem = new StringElement();
		elem.setType(STR_ID_1);
		elem.setValue("hello world");
		Assertions.assertEquals("hello world", elem.getValue());
	}

	@Test
	public void testUtf8RoundTrip() {
		final StringElement elem = new StringElement(StandardCharsets.UTF_8);
		elem.setType(STR_ID_1);
		elem.setValue("hello world");
		Assertions.assertEquals("hello world", elem.getValue());
	}

	@Test
	public void testEmptyString() {
		final StringElement elem = new StringElement();
		elem.setType(STR_ID_1);
		elem.setValue("");
		Assertions.assertEquals("", elem.getValue());
		Assertions.assertEquals(0, elem.getSize());
	}

	@Test
	public void testSizeMatchesStringBytes() {
		final StringElement elem = new StringElement();
		elem.setType(STR_ID_1);
		elem.setValue("test");
		Assertions.assertEquals(4, elem.getSize());
	}

	@Test
	public void testConstructorWithTypeAndCharset() {
		final StringElement elem = new StringElement(STR_ID_2, StandardCharsets.UTF_8);
		elem.setValue("codec-id");
		Assertions.assertEquals("codec-id", elem.getValue());
		Assertions.assertEquals(StandardCharsets.UTF_8, elem.getEncoding());
	}

	@Test
	public void testUtf8StringElement() {
		final UTF8StringElement elem = new UTF8StringElement();
		elem.setType(STR_ID_1);
		elem.setValue("unicode test");
		Assertions.assertEquals("unicode test", elem.getValue());
	}

	@Test
	public void testWriteAndReadBackAscii() {
		final ProtoType<StringElement> typeInfo = new ProtoType<>(StringElement.class, "testStr_SET",
				STR_ID_3, 1);
		final StringElement elem = typeInfo.getInstance();
		elem.setValue("abc");

		final MockWriter writer = new MockWriter();
		elem.writeElement(writer);

		final java.nio.ByteBuffer written = writer.getBuff();
		written.flip();

		// 4-byte type ID
		Assertions.assertEquals((byte) 0x1F, written.get());
		Assertions.assertEquals((byte) 0xFE, written.get());
		Assertions.assertEquals((byte) 0xF1, written.get());
		Assertions.assertEquals((byte) 0x03, written.get());
		Assertions.assertEquals(3, Reader.readEBMLCode(written));
		final byte[] strBytes = new byte[3];
		written.get(strBytes);
		Assertions.assertEquals("abc", new String(strBytes, StandardCharsets.US_ASCII));
	}
}
