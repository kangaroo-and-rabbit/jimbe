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

/**
 * EBML element containing raw binary data. Serves as the default element type for
 * unknown elements and as the base class for typed elements (string, integer, float, etc.).
 */
public class BinaryElement extends Element {
	/**
	 * Creates a binary element with the specified type ID.
	 *
	 * @param type the EBML element ID bytes
	 */
	public BinaryElement(final byte[] type) {
		super(type);
	}

	/** Creates an empty binary element. */
	public BinaryElement() {}
}
