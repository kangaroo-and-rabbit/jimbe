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
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matroska Frame, holds a Matroska frame timecode, duration, and data. <br>
 * Note that the data, track number, and timecode fields are all mandatory.
 */
public class Frame {
	private static final Logger LOG = LoggerFactory.getLogger(Frame.class);

	private int trackNo;
	/** The presentation timecode in milliseconds. */
	private long timecode;
	/** The duration of this frame in milliseconds, or {@link Long#MIN_VALUE} if unset. */
	private long duration = Long.MIN_VALUE;

	/** List of timecode references to other frames (for non-keyframe dependencies). */
	private ArrayList<Long> references = new ArrayList<>();
	/** The raw frame data payload. */
	private ByteBuffer data;
	private boolean keyFrame;

	/** Constructs an empty frame with no data or timecode set. */
	public Frame() {
		this.references = new ArrayList<>();
	}

	/**
	 * Copy constructor that duplicates all properties from another frame.
	 *
	 * @param copy the frame to copy
	 */
	public Frame(final Frame copy) {
		this.trackNo = copy.trackNo;
		setTimecode(copy.getTimecode());
		setDuration(copy.getDuration());
		setKeyFrame(copy.isKeyFrame());
		if (copy.getReferences() != null) {
			this.references.addAll(copy.getReferences());
		}
		if (copy.getData() != null) {
			setData(copy.getData().duplicate());
		}
	}

	/** Returns {@code true} if this is a key frame. */
	public boolean isKeyFrame() {
		return this.keyFrame;
	}

	/**
	 * Sets whether this frame is a key frame.
	 *
	 * @param keyFrame {@code true} if this is a key frame
	 */
	public void setKeyFrame(final boolean keyFrame) {
		this.keyFrame = keyFrame;
	}

	/** Returns the track number this frame belongs to. */
	public int getTrackNo() {
		return this.trackNo;
	}

	/**
	 * Sets the track number this frame belongs to.
	 *
	 * @param trackNo the track number
	 */
	public void setTrackNo(final int trackNo) {
		this.trackNo = trackNo;
	}

	/** Returns the list of timecode references to other frames. */
	public ArrayList<Long> getReferences() {
		return this.references;
	}

	/**
	 * Adds one or more frame references (timecodes of frames this frame depends on).
	 *
	 * @param references the timecode references to add
	 */
	public void addReferences(final long... references) {
		for (final Long ref : references) {
			this.references.add(ref);
		}
	}

	/** Returns the presentation timecode in milliseconds. */
	public long getTimecode() {
		return this.timecode;
	}

	/**
	 * Sets the presentation timecode in milliseconds.
	 *
	 * @param timecode the timecode value
	 */
	public void setTimecode(final long timecode) {
		this.timecode = timecode;
	}

	/** Returns the duration of this frame in milliseconds, or {@link Long#MIN_VALUE} if unset. */
	public long getDuration() {
		return this.duration;
	}

	/**
	 * Sets the duration of this frame in milliseconds.
	 *
	 * @param duration the duration value
	 */
	public void setDuration(final long duration) {
		this.duration = duration;
	}

	/** Returns a duplicate of the data buffer contained in this frame. */
	public ByteBuffer getData() {
		return this.data.duplicate();
	}

	/**
	 * Sets the data payload for this frame (the buffer is duplicated internally).
	 *
	 * @param data the frame data buffer
	 */
	public void setData(final ByteBuffer data) {
		LOG.trace("Setting data with size {}", data.remaining());
		this.data = data.duplicate();
	}

}
