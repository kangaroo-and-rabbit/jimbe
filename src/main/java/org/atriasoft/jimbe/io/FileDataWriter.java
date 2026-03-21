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

public class FileDataWriter implements DataWriter {
	RandomAccessFile file = null;
	FileChannel fc = null;

	public FileDataWriter(final String filename) throws FileNotFoundException, IOException {
		this.file = new RandomAccessFile(filename, "rw");
		this.fc = this.file.getChannel();
	}

	public FileDataWriter(final String filename, final String mode) throws FileNotFoundException, IOException {
		this.file = new RandomAccessFile(filename, mode);
		this.fc = this.file.getChannel();
	}

	@Override
	public int write(final byte b) {
		try {
			this.file.write(b);
			return 1;
		} catch (final IOException ex) {
			return 0;
		}
	}

	@Override
	public int write(final ByteBuffer buff) {
		try {
			return this.fc.write(buff);
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

	public void close() throws IOException {
		this.file.close();
	}
}
