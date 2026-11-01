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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cluster of frames in a file. Used internally during muxing.
 */
public class Cluster {
	private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);
	private final Queue<Frame> frames = new ConcurrentLinkedQueue<>();
	private final Set<Integer> tracks = new HashSet<>();
	private final List<Long> sliencedTracks = new ArrayList<>();

	private long clusterTimecode = Long.MAX_VALUE;

	/** The latest timecode in this cluster: with the earliest, what the blocks have to reach. */
	private long latestTimecode = Long.MIN_VALUE;
	private int sizeLimit = Integer.MAX_VALUE;

	/** What a cluster must hold before a key frame may close it; 0 keeps the old behaviour. */
	private int minSize = 0;

	/** What a cluster must span before a key frame may close it; 0 keeps the old behaviour. */
	private long minDuration = 0;
	private int totalSize = 0;
	private long durationLimit = Long.MAX_VALUE;

	/** Constructs an empty cluster with no size or duration limits. */
	public Cluster() {}

	/**
	 * Sets the maximum duration and size limits for this cluster.
	 * When either limit is exceeded, {@link #addFrame(Frame)} returns {@code false}.
	 *
	 * @param duration the maximum duration span of frames in the cluster
	 * @param size the maximum total data size in bytes
	 */
	public void setLimitParameters(final long duration, final int size) {
		this.sizeLimit = size;
		this.durationLimit = duration;
	}

	/**
	 * Sets how much a cluster must hold before a key frame is allowed to end it.
	 *
	 * <p>
	 * A cluster has to start on a key frame, so that the cues point at positions one can seek to. Ending it on every key frame is another matter: on a stream where they are frequent, that writes one
	 * tiny cluster and one index entry per frame, and the index grows with the file until nothing is left to hold it. A cluster may hold several key frames, the cues simply point at the first.
	 *
	 * @param duration the span a cluster must reach before a key frame may close it
	 * @param size the size a cluster must reach before a key frame may close it
	 */
	public void setMinimumParameters(final long duration, final int size) {
		this.minDuration = duration;
		this.minSize = size;
	}

	/**
	 * Checks if the cluster should be flushed before adding this frame.
	 * Forces cluster boundaries on keyframes so that Cues always point to seekable positions.
	 * Also forced when a block could not reach the frame from the cluster timecode.
	 *
	 * @param frame the frame about to be added
	 * @return {@code true} if the cluster should be flushed before adding this frame
	 */
	public boolean shouldFlushBefore(final Frame frame) {
		if (this.frames.isEmpty()) {
			return false;
		}
		if (exceedsBlockRange(frame.getTimecode())) {
			return true;
		}
		if (!frame.isKeyFrame()) {
			return false;
		}
		// Big enough, or long enough: below that the key frame joins the cluster
		// being built instead of starting a new one of its own.
		return this.totalSize >= this.minSize || frame.getTimecode() - this.clusterTimecode >= this.minDuration;
	}

	/**
	 * A block stores its timecode on 16 signed bits, relative to the earliest frame of the cluster. A frame
	 * that arrives too late breaks that, and so does one that arrives too early, since it moves the cluster
	 * timecode away from the frames already held.
	 */
	private boolean exceedsBlockRange(final long timecode) {
		return Math.max(this.latestTimecode, timecode) - Math.min(this.clusterTimecode, timecode) > Short.MAX_VALUE;
	}

	/**
	 * Adds a frame to the cluster. Call {@link #shouldFlushBefore(Frame)} first: {@link #flush(DataWriter)} fails
	 * on frames that are too far apart for a single cluster.
	 *
	 * @param frame the frame to add
	 * @return {@code false} if the cluster limits have been exceeded and a new cluster should be started
	 */
	public boolean addFrame(final Frame frame) {
		// Is this the earliest timecode?
		if (frame.getTimecode() < this.clusterTimecode) {
			this.clusterTimecode = frame.getTimecode();
		}
		if (frame.getTimecode() > this.latestTimecode) {
			this.latestTimecode = frame.getTimecode();
		}
		this.frames.add(frame);
		this.totalSize += frame.getData().remaining();
		this.tracks.add(frame.getTrackNo());
		return ((frame.getTimecode() - this.clusterTimecode) < this.durationLimit) && (this.totalSize < this.sizeLimit);
	}

	/**
	 * Writes all accumulated frames as a Matroska Cluster element to the output,
	 * then clears the internal state for reuse.
	 *
	 * @param ioDW the data writer to write the cluster to
	 * @return the number of bytes written, or 0 if the cluster was empty
	 */
	public long flush(final DataWriter ioDW) {
		if (this.frames.size() == 0) {
			return 0;
		}
		try {
			final MasterElement clusterElem = DocTypes.Cluster.getInstance();
			final UnsignedIntegerElement timecodeElem = DocTypes.Timecode.getInstance();
			timecodeElem.setValue(this.clusterTimecode);
			clusterElem.addChildElement(timecodeElem);

			if (!this.sliencedTracks.isEmpty()) {
				final MasterElement silentElem = DocTypes.SilentTracks.getInstance();
				for (final Long silent : this.sliencedTracks) {
					final UnsignedIntegerElement silentTrackElem = DocTypes.SilentTrackNumber.getInstance();
					silentTrackElem.setValue(silent);
					silentElem.addChildElement(silentTrackElem);
				}
				clusterElem.addChildElement(silentElem);
			}

			LOG.trace("Timecode for cluster set to {}", this.clusterTimecode);
			for (final Frame frame : this.frames) {
				frame.setTimecode(frame.getTimecode() - this.clusterTimecode);
				LOG.trace("Timecode for frame set to {}", frame.getTimecode());
				final SimpleBlock block = new SimpleBlock();
				block.addFrame(frame);
				clusterElem.addChildElement(block.toElement());
			}
			return clusterElem.writeElement(ioDW);
		} finally {
			this.frames.clear();
			this.tracks.clear();
			this.totalSize = 0;
			this.clusterTimecode = Long.MAX_VALUE;
			this.latestTimecode = Long.MIN_VALUE;
		}
	}

	/** Returns the timecode of this cluster (the earliest frame timecode). */
	public long getClusterTimecode() {
		return this.clusterTimecode;
	}

	/** Returns a read-only view of the track numbers that have frames in this cluster. */
	public Collection<Integer> getTracks() {
		return Collections.unmodifiableSet(this.tracks);
	}

	/**
	 * Removes a track from the silent tracks list.
	 *
	 * @param trackNumber the track number to unsilence
	 */
	public void unsilenceTrack(final long trackNumber) {
		this.sliencedTracks.remove(trackNumber);
	}

	/**
	 * Adds a track to the silent tracks list. Silent tracks are written in the
	 * cluster header to indicate tracks that have no data in this cluster.
	 *
	 * @param trackNumber the track number to silence
	 */
	public void silenceTrack(final long trackNumber) {
		this.sliencedTracks.add(trackNumber);
	}
}
