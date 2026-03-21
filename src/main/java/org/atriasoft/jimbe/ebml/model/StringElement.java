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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * EBML element containing a string value. Supports any character encoding;
 * defaults to US-ASCII. Use {@link UTF8StringElement} for UTF-8 strings.
 */
public class StringElement extends BinaryElement {

	private Charset charset = StandardCharsets.US_ASCII;

	/**
	 * Creates a string element with the specified type ID and ASCII encoding.
	 *
	 * @param typeID the EBML element ID bytes
	 */
	public StringElement(final byte[] typeID) {
		super(typeID);
	}

	/** Creates an empty string element with ASCII encoding. */
	public StringElement() {}

	/**
	 * Creates a string element with the specified type ID and encoding.
	 *
	 * @param typeID the EBML element ID bytes
	 * @param encoding the character encoding to use
	 */
	public StringElement(final byte[] typeID, final Charset encoding) {
		super(typeID);
		this.charset = encoding;
	}

	/**
	 * Creates an empty string element with the specified encoding.
	 *
	 * @param encoding the character encoding to use
	 */
	public StringElement(final Charset encoding) {
		this.charset = encoding;
	}

	/**
	 * Returns the string value decoded from the element's data.
	 *
	 * @return the string value
	 */
	public String getValue() {
		return new String(this.data.array(), this.charset);
	}

	/**
	 * Sets the string value, encoding it into the element's data buffer.
	 *
	 * @param value the string value to set
	 */
	public void setValue(final String value) {
		setData(ByteBuffer.wrap(value.getBytes(this.charset)));
	}

	/**
	 * Returns the character encoding used by this string element.
	 *
	 * @return the character encoding
	 */
	public Charset getEncoding() {
		return this.charset;
	}
}
