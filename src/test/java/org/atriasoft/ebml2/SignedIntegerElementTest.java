package org.atriasoft.ebml2;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedIntegerElementTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(UnsignedIntegerElementTest.class);
	
	private final MockWriter writer = new MockWriter();
	private final ProtoType<SignedIntegerElement> typeInfo = new ProtoType<>(SignedIntegerElement.class, "test",
			new byte[] { (byte) 0xC2 }, 1);
	private final SignedIntegerElement elem = this.typeInfo.getInstance();
	
	@Test
	public void testPositiveData() {
		for (int i = 0; i < 8; ++i) {
			final long value = (long) (42 * Math.pow(256, i));
			LOG.debug("Testing {} for {}", value, i);
			this.elem.setValue(value);
			Assertions.assertEquals(value, this.elem.getValue());
			this.elem.writeData(this.writer);
			final ByteBuffer buf = this.writer.getBuff();
			buf.flip();
			Assertions.assertEquals(i + 1, buf.remaining());
			Assertions.assertEquals((byte) 42, buf.get());
			for (int k = i; k > 0; --k) {
				Assertions.assertEquals(0, buf.get());
			}
			buf.clear();
		}
	}
	
	@Test
	public void testNegativeData() {
		for (int i = 0; i < 7; ++i) {
			final long value = (long) (-42 * Math.pow(256, i));
			LOG.debug("Testing {} for {}", value, i);
			this.elem.setValue(value);
			Assertions.assertEquals(value, this.elem.getValue());
			this.elem.writeData(this.writer);
			final ByteBuffer buf = this.writer.getBuff();
			buf.flip();
			Assertions.assertEquals(i + 1, buf.remaining());
			Assertions.assertEquals((byte) -42, buf.get());
			for (int k = i; k > 0; --k) {
				Assertions.assertEquals(0, buf.get());
			}
			buf.clear();
		}
	}
	
	@Test
	public void testPositiveElement() {
		for (int i = 0; i < 8; ++i) {
			final long value = (long) (42 * Math.pow(256, i));
			final long size = Element.getMinByteSize(value);
			final long sizeSize = Element.getMinByteSizeUnsigned(size);
			LOG.debug("Testing element {} val {} ({})", i, value, size);
			
			this.elem.setValue(value);
			Assertions.assertEquals(value, this.elem.getValue());
			this.elem.writeElement(this.writer);
			final ByteBuffer buf = this.writer.getBuff();
			buf.flip();
			Assertions.assertEquals(i + 2 + sizeSize, buf.remaining());
			Assertions.assertEquals((byte) 0xC2, buf.get());
			Assertions.assertEquals(size, EBMLReader.readEBMLCode(buf));
			Assertions.assertEquals((byte) 42, buf.get());
			for (int k = i; k > 0; --k) {
				Assertions.assertEquals(0, buf.get());
			}
			buf.clear();
		}
	}
	
	@Test
	public void testNegativeElement() {
		for (int i = 0; i < 8; ++i) {
			final long value = (long) (-42 * Math.pow(256, i));
			final long size = Element.getMinByteSize(value);
			final long sizeSize = Element.getMinByteSizeUnsigned(size);
			LOG.debug("Testing element {} val {} ({})", i, value, size);
			
			this.elem.setValue(value);
			Assertions.assertEquals(value, this.elem.getValue());
			this.elem.writeElement(this.writer);
			final ByteBuffer buf = this.writer.getBuff();
			buf.flip();
			Assertions.assertEquals(i + 2 + sizeSize, buf.remaining());
			Assertions.assertEquals((byte) 0xC2, buf.get());
			Assertions.assertEquals(size, EBMLReader.readEBMLCode(buf));
			Assertions.assertEquals((byte) -42, buf.get());
			for (int k = i; k > 0; --k) {
				Assertions.assertEquals(0, buf.get());
			}
			buf.clear();
		}
	}
}
