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

package org.atriasoft.jimbe.matroska.model;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.atriasoft.jimbe.ebml.Reader;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.io.DataWriter;

/**
 * Void element. Doesn't do anything but take up space for later use.
 */
public class VoidElement extends Element {
	private static final long MAX_SIZE = (long) (Math.pow(2, 56) - 2);
	private long realSize;

	/**
	 * Creates a new VoidElement with the specified total size in bytes.
	 *
	 * @param size the total size of the void element (must be greater than 1 and less than 2^56 - 2)
	 * @throws IllegalArgumentException if the size is out of the valid range
	 */
	public VoidElement(final long size) {
		setElementType(DocTypes.Void);
		setType(DocTypes.Void.getType());
		setSize(size);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Writes the void element header, forcing the size field to use up to 8 bytes
	 * to allow creating voids of any desired total size.
	 * </p>
	 */
	@Override
	public long writeHeaderData(final DataWriter writer) {

		int len = 0;

		len += getType().remaining();

		final byte[] encodedSize = Element.makeEbmlCodedSize(getSize(), (int) Math.min(this.realSize - len, 8));
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
	 * {@inheritDoc}
	 * <p>
	 * Writes the void body as a sequence of bytes filled with the value 1.
	 * </p>
	 */
	@Override
	public long writeData(final DataWriter ioDW) {
		final byte[] voids = new byte[(int) getSize()];
		Arrays.fill(voids, (byte) 1);
		return ioDW.write(ByteBuffer.wrap(voids));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overrides the default size calculation to account for the forced 8-byte size encoding,
	 * ensuring void elements can be created at any desired total size.
	 * </p>
	 *
	 * @throws IllegalArgumentException if the size is less than 2 or greater than 2^56 - 2
	 */
	@Override
	public void setSize(final long size) {
		if (size < 2 || size > MAX_SIZE) {
			throw new IllegalArgumentException("Size must be greater than one and less than (2^52 - 2)");
		}
		// In order to be able to create voids of every possible size, we must force the size field to be larger than required
		// The simplest thing to do is to first force the size field to the maximum, 8 bytes. So a 9 byte void will have 0 "body" bytes
		// but 1 byte type header and 8 bytes size header.
		this.realSize = size;
		final long partialSize = size - this.type.remaining();
		super.setSize(partialSize - Math.min(partialSize, 8));
	}

	/**
	 * Reduces the body size of this void element by the specified amount.
	 *
	 * @param size the number of bytes to subtract from the current body size
	 */
	public void reduceSize(final long size) {
		super.setSize(getSize() - size);
	}
}
