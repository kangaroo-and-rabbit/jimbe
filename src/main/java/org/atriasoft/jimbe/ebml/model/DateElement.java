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
import java.util.Date;

/**
 * EBML element containing a date value. Dates are stored as nanoseconds since
 * 2001-01-01T00:00:00 UTC (the EBML epoch), encoded as an 8-byte signed integer.
 */
public class DateElement extends SignedIntegerElement {
	// const uint64 EbmlDate::UnixEpochDelay = 978307200; // 2001/01/01 00:00:00 UTC
	/** Seconds between the Unix epoch and the EBML epoch. */
	public static final long UnixEpochDelay = 978307200; // 2001/01/01 00:00:00 UTC
	private static final long EBML_EPOCH_MS = UnixEpochDelay * 1000;
	private static final long NANOS_PER_MS = 1000000;
	private static final int MIN_SIZE_LENGTH = 8;

	/**
	 * Creates a date element with the specified type ID.
	 *
	 * @param type the EBML element ID bytes
	 */
	public DateElement(final byte[] type) {
		super(type);
	}

	/** Creates an empty date element. */
	public DateElement() {}

	/**
	 * Set the Date of this element
	 *
	 * @param value Date to set
	 * @throws ArithmeticException if the date is too far from 2001 for 64 bits of nanoseconds, about 292 years
	 */
	public void setDate(final Date value) {
		final long val = Math.multiplyExact(value.getTime() - EBML_EPOCH_MS, NANOS_PER_MS);
		setData(ByteBuffer.wrap(packInt(val, MIN_SIZE_LENGTH)));
	}

	/**
	 * Get the Date value of this element
	 *
	 * @return Date of this element
	 */
	public Date getDate() {
		// An empty date is valid: it stands for the epoch.
		if (getSize() == 0) {
			return new Date(EBML_EPOCH_MS);
		}
		return new Date(Math.floorDiv(getValue(), NANOS_PER_MS) + EBML_EPOCH_MS);
	}

	/**
	 * It's not recommended to use this method. Use the setDate(Date) method instead.
	 */
	@Override
	public void setValue(final long value) {
		setData(ByteBuffer.wrap(packInt(value, MIN_SIZE_LENGTH)));
	}
}
