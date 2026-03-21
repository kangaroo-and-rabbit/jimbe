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

import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.io.DataWriter;

/**
 * Represents the top-level Matroska Segment element.
 * <p>
 * The Segment is the root container for all Matroska data (tracks, clusters, cues, etc.).
 * It extends {@link MasterElement} and adds support for writing the element size as
 * "unknown" (all 1-bits), which is required for streaming or when the final size
 * is not known in advance.
 * </p>
 */
public class Segment extends MasterElement {
	protected boolean bUnknownSize = false;

	/** Constructs a Segment element with the standard Matroska Segment type ID. */
	public Segment() {
		super(DocTypes.Segment.getType().array());
	}

	/**
	 * Writes the element header (type ID and size) to the output.
	 * If the unknown size flag is set, the size field is filled with all 1-bits
	 * to indicate an indeterminate length.
	 *
	 * @param writer the data writer to write the header to
	 * @return the number of bytes written
	 */
	@Override
	public long writeHeaderData(final DataWriter writer) {
		long len = 0;

		final ByteBuffer type = getType();
		len += type.remaining();
		writer.write(type);

		byte[] size;
		if (this.bUnknownSize) {
			size = new byte[5];
			size[0] = (byte) (0xFF >>> (size.length - 1));
			for (int i = 1; i < size.length; i++) {
				size[i] = (byte) 0xFF;
			}
		} else {
			size = Element.makeEbmlCodedSize(getSize());
		}

		len += size.length;
		writer.write(ByteBuffer.wrap(size));

		return len;
	}

	/**
	 * Sets the unknown size flag. When enabled, the element size is written as
	 * indeterminate (all 1-bits), which is used for streaming scenarios.
	 *
	 * @param bUnknownSize {@code true} to write an unknown/indeterminate size
	 */
	public void setUnknownSize(final boolean bUnknownSize) {
		this.bUnknownSize = bUnknownSize;
	}

	/** Returns {@code true} if this segment will be written with an indeterminate size. */
	public boolean getUnknownSize() {
		return this.bUnknownSize;
	}
}
