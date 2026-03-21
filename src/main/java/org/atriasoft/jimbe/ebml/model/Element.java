/**
 * JEBML - Java library to read/write EBML/Matroska elements.
 * Copyright (C) 2004 Jory Stone <jebml@jory.info>
 * Based on Javatroska (C) 2002 John Cannon <spyder@matroska.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.atriasoft.jimbe.ebml.model;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.atriasoft.jimbe.ebml.Reader;
import org.atriasoft.jimbe.io.DataSource;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all EBML elements. An element consists of an ID (type), a size, and optional data.
 * Subclasses specialize behavior for different EBML data types (binary, string, integer, float, etc.).
 *
 * <p>Also provides static utility methods for EBML size encoding/decoding and integer packing.</p>
 */
public class Element {
	protected static final Logger LOG = LoggerFactory.getLogger(Element.class);
	private static int minSizeLength = 0;

	protected Element parent;
	protected ProtoType<?> typeInfo;
	protected ByteBuffer type;
	protected long size = 0;
	protected ByteBuffer data = null;
	protected boolean dataRead = false;
	private Long headersSize = null;

	/**
	 * Creates an element with the specified type ID.
	 *
	 * @param type the EBML element ID bytes
	 */
	public Element(final byte[] type) {
		this.type = ByteBuffer.wrap(type);
	}

	/** Creates an empty element. Type and size must be set before use. */
	public Element() {}

	/**
	 * Reads the element's data payload from the data source into an internal buffer.
	 *
	 * @param source the data source to read from
	 */
	public void readData(final DataSource source) {
		// Setup a buffer for it's data
		this.data = ByteBuffer.allocate((int) this.size);
		// Read the data
		source.read(this.data);
		this.data.flip();
		this.dataRead = true;

		LOG.trace("Read {} bytes from {}", this.size, this.typeInfo.getName());
	}

	/**
	 * Skips the element's data payload in the data source without reading it.
	 *
	 * @param source the data source to skip in
	 */
	public void skipData(final DataSource source) {
		if (!this.dataRead) {
			// Skip the data
			source.skip(this.size);
			this.dataRead = true;
		}
	}

	/**
	 * Writes the complete element (header + data) to the output.
	 *
	 * @param writer the data writer to write to
	 * @return the total number of bytes written
	 */
	public long writeElement(final DataWriter writer) {
		LOG.trace("Writing element {} with size {}", this.typeInfo.getName(), getTotalSize());
		return writeHeaderData(writer) + writeData(writer);
	}

	/**
	 * Writes the element header (type ID + encoded size) to the output.
	 *
	 * @param writer the data writer to write to
	 * @return the number of header bytes written
	 */
	public long writeHeaderData(final DataWriter writer) {

		int len = 0;

		len += getType().remaining();

		final byte[] encodedSize = Element.makeEbmlCodedSize(getSize());
		// System.out.printf("Writing header for element %s with size %d (%s)\n", typeInfo.name, getTotalSize(), Reader.bytesToHex(size));

		len += encodedSize.length;
		final ByteBuffer buf = ByteBuffer.allocate(len);
		buf.put(getType());
		buf.put(encodedSize);
		buf.flip();
		LOG.trace("Writing out header {}, {}", buf.remaining(), Reader.bytesToHex(buf.array()));
		writer.write(buf);
		return len;
	}

	/**
	 * Writes the element's data payload to the output.
	 *
	 * @param writer the data writer to write to
	 * @return the number of data bytes written
	 * @throws NullPointerException if no data has been set
	 */
	public long writeData(final DataWriter writer) {
		if (this.data == null) {
			throw new NullPointerException(String.format("No data to write: %s : %s", this.typeInfo.getName(),
					Arrays.toString(this.type.array())));
		}
		this.data.mark();
		try {
			LOG.trace("Writing data {} bytes of {}", this.data.remaining(), Reader.bytesToHex(this.data.array()));
			return writer.write(this.data);
		} finally {
			this.data.reset();
		}
	}

	/**
	 * Returns a duplicate of the element's data buffer.
	 *
	 * @return a duplicate of the data buffer
	 */
	public ByteBuffer getData() {
		return this.data.duplicate();
	}

	/**
	 * Sets the element's data buffer and updates the size accordingly.
	 *
	 * @param data the data buffer to set
	 */
	public void setData(final ByteBuffer data) {
		this.data = data;
		this.size = data.remaining();
	}

	/**
	 * Clears the data buffer. Useful for placeholder elements that only carry type/size info.
	 */
	public void clearData() {
		this.data = null;
	}

	/**
	 * Returns the data payload size in bytes (excluding the header).
	 *
	 * @return the data size
	 */
	public long getSize() {
		return this.size;
	}

	/**
	 * Sets the data payload size. Invalidates the cached header size.
	 *
	 * @param size the data size in bytes
	 */
	public void setSize(final long size) {
		// Clear headersSize because it is no longer valid.
		this.headersSize = null;
		this.size = size;
	}

	/**
	 * Returns the total size of this element (header + data).
	 *
	 * @return the total size in bytes
	 */
	public long getTotalSize() {
		long totalSize = 0;

		if (this.headersSize != null) {
			totalSize += this.headersSize;
		} else {
			totalSize += getType().array().length;
			totalSize += Element.codedSizeLength(getSize(), 0);
		}
		totalSize += getSize();
		return totalSize;
	}

	/**
	 * Returns a duplicate of the element's type ID buffer.
	 *
	 * @return a duplicate of the type ID bytes
	 */
	public ByteBuffer getType() {
		return this.type.duplicate();
	}

	/**
	 * Sets the element's type ID from a byte array.
	 *
	 * @param type the EBML element ID bytes
	 */
	public void setType(final byte[] type) {
		this.type = ByteBuffer.wrap(type);
	}

	/**
	 * Sets the element's type ID from a buffer.
	 *
	 * @param type the EBML element ID buffer
	 */
	public void setType(final ByteBuffer type) {
		this.type = type;
	}

	/**
	 * Sets the prototype descriptor for this element.
	 *
	 * @param typeInfo the prototype that describes this element's type
	 */
	public void setElementType(final ProtoType<?> typeInfo) {
		this.typeInfo = typeInfo;
	}

	/**
	 * Returns the prototype descriptor for this element.
	 *
	 * @return the prototype, or {@code null} for unknown elements
	 */
	public ProtoType<?> getElementType() {
		return this.typeInfo;
	}

	/**
	 * Sets whether this element's data has been read or skipped.
	 *
	 * @param dataRead {@code true} if the data has been consumed
	 */
	public void setDataRead(final boolean dataRead) {
		this.dataRead = dataRead;
	}

	/**
	 * Returns the parent element in the EBML hierarchy.
	 *
	 * @return the parent element, or {@code null} if this is a top-level element
	 */
	public Element getParent() {
		return this.parent;
	}

	/**
	 * Sets the parent element in the EBML hierarchy.
	 *
	 * @param parent the parent element
	 */
	public void setParent(final Element parent) {
		this.parent = parent;
	}

	/**
	 * Checks whether this element's type ID matches the given bytes.
	 *
	 * @param typeId the type ID bytes to compare against
	 * @return {@code true} if the types match
	 */
	public boolean isType(final byte[] typeId) {
		return Arrays.equals(this.type.array(), typeId);
	}

	/**
	 * Checks whether this element's type ID matches the given buffer.
	 *
	 * @param typeId the type ID buffer to compare against
	 * @return {@code true} if the types match
	 */
	public boolean isType(final ByteBuffer typeId) {
		return typeId.equals(this.type);
	}

	/**
	 * Sets the global minimum number of bytes used for EBML size encoding.
	 *
	 * @param minSize the minimum size length (0 for no minimum)
	 */
	public static void setMinSizeLength(final int minSize) {
		minSizeLength = minSize;
	}

	/**
	 * Returns the global minimum number of bytes used for EBML size encoding.
	 *
	 * @return the minimum size length
	 */
	public static int getMinSizeLength() {
		return minSizeLength;
	}

	/**
	 * Encodes a size value into EBML variable-length format.
	 *
	 * @param size the size to encode
	 * @return the encoded bytes with VINT marker bit
	 */
	public static byte[] makeEbmlCodedSize(final long size) {
		return makeEbmlCodedSize(size, 0);
	}

	/**
	 * Encodes a size value into EBML variable-length format with a minimum byte count.
	 *
	 * @param size the size to encode
	 * @param minSizeLen the minimum number of bytes to use (0 for no minimum)
	 * @return the encoded bytes with VINT marker bit
	 */
	public static byte[] makeEbmlCodedSize(final long size, final int minSizeLen) {
		final int len = codedSizeLength(size, minSizeLen);
		final byte[] ret = new byte[len];
		// byte[] packedSize = packIntUnsigned(size);
		long mask = 0x00000000000000FFL;
		for (int i = 0; i < len; i++) {
			ret[len - 1 - i] = (byte) ((size & mask) >>> (i * 8));
			mask <<= 8;
		}
		// The first size bits should be clear, otherwise we have an error in the size determination.
		ret[0] |= 0x80 >> (len - 1);
		LOG.trace("Ebml coded size {} for {}", Reader.bytesToHex(ret), size);
		return ret;
	}

	/**
	 * Returns the minimum number of bytes needed to represent a signed value.
	 *
	 * @param value the signed value
	 * @return the minimum byte count (1-8)
	 */
	public static int getMinByteSize(final long value) {
		final long absValue = Math.abs(value);
		return getMinByteSizeUnsigned(absValue << 1);
	}

	/**
	 * Returns the minimum number of bytes needed to represent an unsigned value.
	 *
	 * @param value the unsigned value
	 * @return the minimum byte count (1-8)
	 */
	public static int getMinByteSizeUnsigned(final long value) {
		int size = 8;
		long mask = 0xFF00000000000000L;
		for (int i = 0; i < 8; i++) {
			if ((value & mask) == 0) {
				mask = mask >>> 8;
				size--;
			} else {
				return size;
			}
		}
		return 8;
	}

	/**
	 * Computes the number of bytes needed to EBML-encode a size value.
	 *
	 * @param value the size value to encode
	 * @param minSizeLen the minimum byte count (0 for no minimum)
	 * @return the number of bytes needed (1-4)
	 */
	public static int codedSizeLength(final long value, final int minSizeLen) {
		int codedSize = 0;
		if (value < 127) {
			codedSize = 1;
		} else if (value < 16383) {
			codedSize = 2;
		} else if (value < 2097151) {
			codedSize = 3;
		} else if (value < 268435455) {
			codedSize = 4;
		}
		if ((minSizeLen > 0) && (codedSize <= minSizeLen)) {
			codedSize = minSizeLen;
		}

		return codedSize;
	}

	/**
	 * Packs an unsigned long into the minimum number of big-endian bytes.
	 *
	 * @param value the unsigned value to pack
	 * @return the packed bytes
	 */
	public static byte[] packIntUnsigned(final long value) {
		final int size = getMinByteSizeUnsigned(value);
		return packInt(value, size);
	}

	/**
	 * Packs a signed long into the minimum number of big-endian bytes.
	 *
	 * @param value the signed value to pack
	 * @return the packed bytes
	 */
	public static byte[] packInt(final long value) {
		final int size = getMinByteSize(value);
		return packInt(value, size);
	}

	/**
	 * Packs a long into a specified number of big-endian bytes.
	 *
	 * @param value the value to pack
	 * @param size the number of bytes to use
	 * @return the packed bytes
	 */
	public static byte[] packInt(final long value, final int size) {
		final byte[] ret = new byte[size];
		final long mask = 0x00000000000000FFL;
		int b = size - 1;
		for (int i = 0; i < size; i++) {
			ret[b] = (byte) (((value >>> (8 * i)) & mask));
			b--;
		}
		return ret;
	}

	/**
	 * Caches the actual header size as read from the stream. This avoids recomputing
	 * the size encoding length when calculating {@link #getTotalSize()}.
	 *
	 * @param headersSize the header size in bytes (type ID + encoded size)
	 */
	public void setHeadersSize(final long headersSize) {
		this.headersSize = headersSize;
	}
}
