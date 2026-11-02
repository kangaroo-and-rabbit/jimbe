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

/**
 * EBML element containing a signed integer value. The value is stored in big-endian
 * format using the minimum number of bytes needed.
 */
public class SignedIntegerElement extends BinaryElement {

	/**
	 * Creates a signed integer element with the specified type ID.
	 *
	 * @param typeID the EBML element ID bytes
	 */
	public SignedIntegerElement(final byte[] typeID) {
		super(typeID);
	}

	/** Creates an empty signed integer element. */
	public SignedIntegerElement() {}

	/**
	 * Sets the signed integer value.
	 *
	 * @param value the value to set
	 */
	public void setValue(final long value) {
		// System.out.println(Long.toHexString(value));
		setData(ByteBuffer.wrap(packInt(value)));
		/*
		 * for (int i = 0; i < data.length; i++) { System.out.print(Integer.toHexString(data[i]) + ", "); } System.out.print("\n");
		 */
	}

	/**
	 * Returns the signed integer value.
	 *
	 * @return the decoded signed value, 0 for an empty element
	 */
	public long getValue() {
		final byte[] dataArray = this.data.array();
		if (dataArray.length == 0) {
			return 0;
		}
		long l = 0;
		long tmp = 0;
		l |= ((long) dataArray[0] << (56 - ((8 - dataArray.length) * 8)));
		for (int i = 1; i < dataArray.length; i++) {
			tmp = ((long) dataArray[dataArray.length - i]) << 56;
			tmp >>>= 56 - (8 * (i - 1));
			l |= tmp;
		}
		// System.out.println(Long.toHexString(l));
		return l;
	}

}
