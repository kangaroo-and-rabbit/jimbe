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
package org.atriasoft.jimbe.ebml;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.ProtoType;
import org.atriasoft.jimbe.io.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EBML element reader. Reads element headers (id + size) from a {@link DataSource}, resolves them
 * via {@link ProtoType}, and returns typed {@link Element} instances ready for data consumption.
 *
 * <p>Also provides static utility methods for EBML variable-length integer encoding/decoding
 * used throughout the library.</p>
 */
public class Reader {
	private static final Logger LOG = LoggerFactory.getLogger(Reader.class);
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	protected DataSource source;

	/**
	 * Creates a new EBML reader.
	 *
	 * @param source the data source to read from
	 */
	public Reader(final DataSource source) {
		this.source = source;
	}

	/**
	 * Converts a byte array to its hexadecimal string representation.
	 *
	 * @param bytes the bytes to convert
	 * @return uppercase hex string (two characters per byte)
	 */
	public static String bytesToHex(final byte[] bytes) {
		final char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			final int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Reads the next EBML element from the data source. Parses the element id and size,
	 * resolves the element type via {@link ProtoType}, and returns the corresponding typed element.
	 * Unknown elements are skipped and returned as {@link BinaryElement} placeholders.
	 *
	 * @return the next element, or {@code null} if the end of the stream is reached
	 */
	public Element readNextElement() {
		// Read the type.
		final long position = this.source.getFilePointer();
		final ByteBuffer elementType = readEBMLCodeAsBytes(this.source);

		if (elementType == null) {
			// Failed to read type id
			return null;
		}

		final Element elem = ProtoType.getInstance(elementType);

		// Read the size (needed for both known and unknown elements).
		final long elementSize = readEBMLCode(this.source);
		final long end = this.source.getFilePointer();

		if (elem == null) {
			// Unknown element: skip its data and create a BinaryElement placeholder
			// so that MasterElement.readNextChild() can track usedSize correctly.
			LOG.debug("Skipping unknown element (id bytes={}) with size {}", bytesToHex(elementType.array()),
					elementSize);
			if (elementSize > 0) {
				this.source.skip(elementSize);
			}
			// Create a placeholder element with correct size info
			final BinaryElement placeholder = new BinaryElement();
			placeholder.setType(elementType);
			placeholder.setSize(elementSize);
			placeholder.setHeadersSize(end - position);
			placeholder.setDataRead(true);
			return placeholder;
		}
		LOG.trace("Read element {}", elem.getElementType().getName());

		if (elementSize == 0) {
			// Zero sized element is valid
			LOG.error("Invalid element size for {}", elem.getElementType().getName());
		}

		// Set it's size
		elem.setSize(elementSize);
		elem.setHeadersSize(end - position);
		LOG.trace("Read element {} with size {}", elem.getElementType().getName(), elem.getTotalSize());

		// Setup a buffer for it's data
		// byte[] elementData = new byte[(int)elementSize];
		// Read the data
		// source.read(elementData, 0, elementData.length);
		// Set the data property on the element
		// elem.setData(elementData);

		// System.out.println("Reader.readNextElement() returning element " + elem.getElementType().name + " with size " +
		// Long.toString(elem.getTotalSize()-elementSize)+" "+Long.toString(elementSize));

		// Return the element
		return elem;
	}

	/**
	 * Reads an EBML variable-length code from the data source and returns it with the leading
	 * length bits stripped. The result contains only the payload bits of the VINT.
	 *
	 * @param source the data source to read from
	 * @return a buffer containing the code bytes (without VINT marker bit), or {@code null} on failure
	 */
	public static ByteBuffer getEBMLCodeAsBytes(final DataSource source) {
		// Begin loop with byte set to newly read byte.
		final byte firstByte = source.readByte();
		final int numBytes = readEBMLCodeSize(firstByte);

		if (numBytes == 0) {
			LOG.error("Failed to read ebml code size from {}", firstByte);
			// Invalid size
			return null;
		}

		// Setup space to store the bits
		final ByteBuffer buf = ByteBuffer.allocate(numBytes);

		// Clear the 1 at the front of this byte, all the way to the beginning of the size
		buf.put((byte) (firstByte & ((0xFF >>> (numBytes)))));

		if (numBytes > 1) {
			// Read the rest of the size.
			source.read(buf);
		}
		buf.flip();
		return buf;
	}

	/**
	 * Determines the number of bytes in an EBML variable-length integer from its first byte.
	 * The position of the first set bit (from the MSB) indicates the total byte count.
	 *
	 * @param firstByte the first byte of the EBML code
	 * @return the total number of bytes (1-8), or {@code 0} if invalid
	 */
	public static int readEBMLCodeSize(final byte firstByte) {
		int numBytes = 0;
		// Begin by counting the bits unset before the first '1'.
		long mask = 0x0080;
		for (int i = 0; i < 8; i++) {
			// Start at left, shift to right.
			if ((firstByte & mask) == mask) { // One found
												// Set number of bytes in size = i+1 ( we must count the 1 too)
				numBytes = i + 1;
				// exit loop by pushing i out of the limit
				i = 8;
			}
			mask >>>= 1;
		}
		// System.out.printf("Read code size %d\n", numBytes);
		return numBytes;
	}

	/**
	 * Reads an unsigned EBML variable-length integer from a {@link DataSource} and returns it as a long.
	 *
	 * @param source the data source to read from
	 * @return the decoded unsigned value, or {@code 0} if the code is invalid
	 */
	public static long readEBMLCode(final DataSource source) {
		// Begin loop with byte set to newly read byte.
		final byte firstByte = source.readByte();
		final int numBytes = readEBMLCodeSize(firstByte);
		LOG.trace("Reading ebml code of {} bytes", numBytes);
		if (numBytes == 0) {
			// Invalid size
			return 0;
		}

		// Setup space to store the bits
		final ByteBuffer data = ByteBuffer.allocate(numBytes);

		// Clear the 1 at the front of this byte, all the way to the beginning of the size
		data.put((byte) (firstByte & ((0xFF >>> (numBytes)))));

		if (numBytes > 1) {
			// Read the rest of the size.
			source.read(data);
		}
		data.flip();
		return parseEBMLCode(data);
	}

	/**
	 * Interprets the bytes in a buffer as an unsigned big-endian integer.
	 * The buffer position is restored after reading.
	 *
	 * @param data the buffer containing the EBML code bytes (without VINT marker bit)
	 * @return the decoded unsigned value, or {@code 0} if the buffer is {@code null}
	 */
	public static long parseEBMLCode(final ByteBuffer data) {
		if (data == null) {
			return 0;
		}
		data.mark();

		// Put this into a long
		long size = 0;
		for (int i = data.remaining() - 1; i >= 0; i--) {
			final long n = data.get() & 0xFF;
			size = size | (n << (8 * i));
		}
		data.reset();
		LOG.trace("Parsed ebml code {} as {}", bytesToHex(data.array()), size);
		return size;
	}

	/**
	 * Reads an unsigned EBML variable-length integer from a {@link ByteBuffer} and returns it as a long.
	 *
	 * @param source the buffer to read from
	 * @return the decoded unsigned value, or {@code 0} if the code is invalid
	 */
	public static long readEBMLCode(final ByteBuffer source) {
		// Begin loop with byte set to newly read byte.
		final byte firstByte = source.get();
		final int numBytes = readEBMLCodeSize(firstByte);
		if (numBytes == 0) {
			// Invalid size
			return 0;
		}

		// Setup space to store the bits
		final ByteBuffer data = ByteBuffer.allocate(numBytes);

		// Clear the 1 at the front of this byte, all the way to the beginning of the size
		data.put((byte) (firstByte & ((0xFF >>> (numBytes)))));

		// Read the rest of the size.
		for (int i = 1; i < numBytes; i++) {
			data.put(source.get());
		}

		data.flip();
		return parseEBMLCode(data);
	}

	/**
	 * Reads a signed EBML variable-length integer from a {@link ByteBuffer}.
	 * Applies the EBML sign offset based on the encoded byte count.
	 *
	 * @param source the buffer to read from
	 * @return the decoded signed value, or {@code 0} if the code is invalid
	 */
	public static long readSignedEBMLCode(final ByteBuffer source) {
		// Begin loop with byte set to newly read byte.
		final byte firstByte = source.get();
		final int numBytes = readEBMLCodeSize(firstByte);
		if (numBytes == 0) {
			// Invalid size
			return 0;
		}

		// Setup space to store the bits
		final ByteBuffer data = ByteBuffer.allocate(numBytes);

		// Clear the 1 at the front of this byte, all the way to the beginning of the size
		data.put((byte) (firstByte & ((0xFF >>> (numBytes)))));

		// Read the rest of the size.
		for (int i = 1; i < numBytes; i++) {
			data.put(source.get());
		}

		data.flip();
		// Put this into a long
		long size = parseEBMLCode(data);

		// Sign it ;)
		if (numBytes == 1) {
			size -= 63;

		} else if (numBytes == 2) {
			size -= 8191;

		} else if (numBytes == 3) {
			size -= 1048575;

		} else if (numBytes == 4) {
			size -= 134217727;
		}

		return size;
	}

	/**
	 * Reads a signed EBML variable-length integer from a {@link DataSource}.
	 * Applies the EBML sign offset based on the encoded byte count.
	 *
	 * @param source the data source to read from
	 * @return the decoded signed value, or {@code 0} if the code is invalid
	 */
	public static long readSignedEBMLCode(final DataSource source) {

		// Begin loop with byte set to newly read byte.
		final byte firstByte = source.readByte();
		final int numBytes = readEBMLCodeSize(firstByte);
		if (numBytes == 0) {
			// Invalid size
			return 0;
		}

		// Setup space to store the bits
		final ByteBuffer data = ByteBuffer.allocate(numBytes);

		// Clear the 1 at the front of this byte, all the way to the beginning of the size
		data.put((byte) (firstByte & ((0xFF >>> (numBytes)))));

		if (numBytes > 1) {
			// Read the rest of the size.
			source.read(data);
		}

		data.flip();
		// Put this into a long
		long size = parseEBMLCode(data);

		// Sign it ;)
		if (numBytes == 1) {
			size -= 63;

		} else if (numBytes == 2) {
			size -= 8191;

		} else if (numBytes == 3) {
			size -= 1048575;

		} else if (numBytes == 4) {
			size -= 134217727;
		}

		return size;
	}

	/**
	 * Reads an EBML variable-length code from the data source and returns the raw bytes
	 * including the VINT marker bit. Used for reading element IDs.
	 *
	 * @param source the data source to read from
	 * @return a buffer containing the raw code bytes, or {@code null} on failure
	 */
	public static ByteBuffer readEBMLCodeAsBytes(final DataSource source) {
		// Begin loop with byte set to newly read byte.
		final byte firstByte = source.readByte();
		final int numBytes = readEBMLCodeSize(firstByte);

		if (numBytes == 0) {
			LOG.error("Failed to read ebml code size from {}", firstByte);
			// Invalid size
			return null;
		}

		// Setup space to store the bits
		final ByteBuffer buf = ByteBuffer.allocate(numBytes);

		buf.put(firstByte);

		if (numBytes > 1) {
			// Read the rest of the size.
			source.read(buf);
		}
		buf.flip();
		return buf;
	}

}
