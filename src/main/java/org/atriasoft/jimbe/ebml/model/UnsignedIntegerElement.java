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

import org.atriasoft.jimbe.ebml.Reader;

/**
 * EBML element containing an unsigned integer value. The value is stored in big-endian
 * format using the minimum number of bytes needed.
 */
public class UnsignedIntegerElement extends BinaryElement {

	/**
	 * Creates an unsigned integer element with the specified type ID.
	 *
	 * @param typeID the EBML element ID bytes
	 */
	public UnsignedIntegerElement(final byte[] typeID) {
		super(typeID);
	}

	/** Creates an empty unsigned integer element. */
	public UnsignedIntegerElement() {}

	/**
	 * Sets the unsigned integer value.
	 *
	 * @param value the value to set
	 */
	public void setValue(final long value) {
		final ByteBuffer buf = ByteBuffer.wrap(packIntUnsigned(value));
		LOG.trace("Setting value {} to {}", value, Reader.bytesToHex(buf.array()));
		setData(buf);
	}

	/**
	 * Returns the unsigned integer value.
	 *
	 * @return the decoded unsigned value
	 */
	public long getValue() {
		return Reader.parseEBMLCode(this.data.duplicate());
	}
}
