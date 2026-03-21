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

import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.io.DataWriter;

/**
 * EBML element writer. Serializes {@link Element} instances (header + data) to a {@link DataWriter} sink.
 */
public class Writer {
	protected DataWriter writer;

	/**
	 * Creates a new EBML writer.
	 *
	 * @param writer the data sink to write to
	 */
	public Writer(final DataWriter writer) {
		this.writer = writer;
	}

	/**
	 * Writes a complete EBML element (header and data) to the output.
	 *
	 * @param elem the element to write
	 * @return the total number of bytes written (header + data)
	 */
	public long writeElement(final Element elem) {
		return elem.writeHeaderData(this.writer) + elem.writeData(this.writer);
	}
}
