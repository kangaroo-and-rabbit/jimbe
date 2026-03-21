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

/**
 * Base interface for seekable data access. Provides position, length, and seeking capabilities.
 * Not designed to be used alone but as a super interface for {@link DataSource} and {@link DataWriter}.
 */
public interface DataSeekable {
	/**
	 * Returns the total length of the data.
	 *
	 * @return the length in bytes, or {@code -1} if unknown
	 */
	long length();

	/**
	 * Returns the current position in the data.
	 *
	 * @return the current byte offset from the beginning
	 */
	long getFilePointer();

	/**
	 * Checks whether this data source supports seeking.
	 *
	 * @return {@code true} if seeking is supported, {@code false} otherwise
	 */
	boolean isSeekable();

	/**
	 * Seeks to an absolute position.
	 *
	 * @param pos the absolute byte position to seek to
	 * @return the new position after seeking
	 */
	long seek(long pos);
}
