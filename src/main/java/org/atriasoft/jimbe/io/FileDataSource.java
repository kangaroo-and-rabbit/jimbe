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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * File-based implementation of {@link DataSource}. Uses {@link RandomAccessFile} and {@link FileChannel}
 * for efficient reading and seeking within files. Implements {@link AutoCloseable} for resource management.
 */
public class FileDataSource implements DataSource, AutoCloseable {
	private RandomAccessFile file = null;
	private FileChannel fc = null;

	/**
	 * Opens a file for reading.
	 *
	 * @param filename the path to the file to read
	 * @throws FileNotFoundException if the file does not exist
	 * @throws IOException if an I/O error occurs
	 */
	public FileDataSource(final String filename) throws FileNotFoundException, IOException {
		this.file = new RandomAccessFile(filename, "r");
		this.fc = this.file.getChannel();
	}

	/**
	 * Opens a file with a specified access mode.
	 *
	 * @param filename the path to the file
	 * @param mode the access mode (e.g. "r" for read-only, "rw" for read-write)
	 * @throws FileNotFoundException if the file does not exist
	 * @throws IOException if an I/O error occurs
	 */
	public FileDataSource(final String filename, final String mode) throws FileNotFoundException, IOException {
		this.file = new RandomAccessFile(filename, mode);
		this.fc = this.file.getChannel();
	}

	@Override
	public byte readByte() {
		try {
			return this.file.readByte();
		} catch (final IOException ex) {
			return 0;
		}
	}

	@Override
	public int read(final ByteBuffer buff) {
		try {
			return this.fc.read(buff);
		} catch (final IOException ex) {
			return 0;
		}
	}

	@Override
	public long skip(final long offset) {
		try {
			return this.file.skipBytes((int) offset);
		} catch (final IOException ex) {
			return 0;
		}
	}

	@Override
	public long length() {
		try {
			return this.file.length();
		} catch (final IOException ex) {
			return -1;
		}
	}

	@Override
	public long getFilePointer() {
		try {
			return this.file.getFilePointer();
		} catch (final IOException ex) {
			return -1;
		}
	}

	@Override
	public boolean isSeekable() {
		return true;
	}

	@Override
	public long seek(final long pos) {
		try {
			this.file.seek(pos);
			return this.file.getFilePointer();
		} catch (final IOException ex) {
			return -1;
		}
	}

	@Override
	public void close() throws IOException {
		this.fc.close();
		this.file.close();
	}
}
