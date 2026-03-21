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
package org.atriasoft.jimbe.io;

import java.nio.ByteBuffer;

/**
 * Interface for writing bytes to a data sink. Extends {@link DataSeekable} to combine
 * write and seek operations. Used by the EBML {@link org.atriasoft.jimbe.ebml.Writer} to serialize elements.
 */
public interface DataWriter extends DataSeekable {

	/**
	 * Writes a single byte.
	 *
	 * @param b the byte to write
	 * @return the number of bytes written (1 on success, 0 on failure)
	 */
	int write(byte b);

	/**
	 * Writes the contents of a buffer.
	 *
	 * @param buff the buffer to write
	 * @return the number of bytes written
	 */
	int write(ByteBuffer buff);
}
