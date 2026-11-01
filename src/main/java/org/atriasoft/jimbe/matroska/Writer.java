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
package org.atriasoft.jimbe.matroska;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.StringElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.atriasoft.jimbe.matroska.model.Cluster;
import org.atriasoft.jimbe.matroska.model.Cues;
import org.atriasoft.jimbe.matroska.model.DocTypes;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.MetaSeek;
import org.atriasoft.jimbe.matroska.model.Segment;
import org.atriasoft.jimbe.matroska.model.SegmentInfo;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.Tracks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary API entrypoint for writing Matroska files.
 */
public class Writer {
	/** How long a cluster may span before it is written out, in milliseconds. */
	private static final long CLUSTER_DURATION_LIMIT_MS = 5000;

	/** How much a cluster may hold before it is written out, in bytes. */
	private static final int CLUSTER_SIZE_LIMIT_BYTES = 4 * 1024 * 1024;

	/** How long a cluster must span before a key frame is allowed to end it. */
	private static final long CLUSTER_MIN_DURATION_MS = 1000;

	/** How much a cluster must hold before a key frame is allowed to end it. */
	private static final int CLUSTER_MIN_SIZE_BYTES = 512 * 1024;

	private static final Logger LOG = LoggerFactory.getLogger(Writer.class);

	protected DataWriter ioDW;

	private static final int SEGMENT_SIZE_BYTES = 5;

	private final MetaSeek metaSeek;
	private final Cues cueData;
	private final Cluster cluster;
	private final SegmentInfo segmentInfoElem;
	private final Tracks tracks;
	private final long segmentSizePosition;
	private final long segmentDataStart;
	private boolean tracksWritten = false;
	private String docType = "webm";

	/**
	 * Creates a new Matroska writer with "webm" DocType for maximum browser compatibility.
	 * Immediately writes the EBML header and segment header.
	 *
	 * @param outputDataWriter the data writer to write to
	 */
	public Writer(final DataWriter outputDataWriter) {
		this(outputDataWriter, "webm");
	}

	/**
	 * Creates a new Matroska writer with the specified DocType.
	 * Immediately writes the EBML header and segment header.
	 *
	 * @param outputDataWriter the data writer to write to
	 * @param docType the DocType string ("webm" for web use, "matroska" for generic files)
	 */
	public Writer(final DataWriter outputDataWriter, final String docType) {
		this.docType = docType;
		this.ioDW = outputDataWriter;
		writeEBMLHeader();
		writeSegmentHeader();
		final long endOfSegmentHeader = this.ioDW.getFilePointer();
		this.segmentSizePosition = endOfSegmentHeader - SEGMENT_SIZE_BYTES;
		this.segmentDataStart = endOfSegmentHeader;
		this.metaSeek = new MetaSeek(endOfSegmentHeader);
		this.cueData = new Cues(endOfSegmentHeader);
		this.metaSeek.write(this.ioDW);
		this.segmentInfoElem = new SegmentInfo(this.ioDW.getFilePointer());
		this.metaSeek.addIndexedElement(DocTypes.Info.getType(), this.ioDW.getFilePointer());
		this.segmentInfoElem.writeElement(this.ioDW);
		this.metaSeek.addIndexedElement(DocTypes.Tracks.getType(), this.ioDW.getFilePointer());
		this.tracks = new Tracks(this.ioDW.getFilePointer());
		this.cluster = new Cluster();
		this.cluster.setLimitParameters(CLUSTER_DURATION_LIMIT_MS, CLUSTER_SIZE_LIMIT_BYTES);
		this.cluster.setMinimumParameters(CLUSTER_MIN_DURATION_MS, CLUSTER_MIN_SIZE_BYTES);
	}

	/**
	 * Tracks wait for the first cluster or for the end of the file, whichever comes first: the room they take
	 * can not change afterwards, and only now are they all known. A codec private alone can exceed the usual
	 * block.
	 */
	private void writeTracksOnce() {
		if (this.tracksWritten) {
			return;
		}
		this.tracksWritten = true;
		this.tracks.writeTracks(this.ioDW);
		this.metaSeek.addIndexedElement(DocTypes.Cluster.getType(), this.ioDW.getFilePointer());
	}

	void writeEBMLHeader() {
		final MasterElement ebmlHeaderElem = DocTypes.EBML.getInstance();

		final UnsignedIntegerElement ebmlVersionElem = DocTypes.EBMLVersion.getInstance();
		ebmlVersionElem.setValue(1);

		final UnsignedIntegerElement ebmlReadVersionElem = DocTypes.EBMLReadVersion.getInstance();
		ebmlReadVersionElem.setValue(1);

		final UnsignedIntegerElement ebmlMaxIdLenElem = DocTypes.EBMLMaxIDLength.getInstance();
		ebmlMaxIdLenElem.setValue(4);

		final UnsignedIntegerElement ebmlMaxSizeLenElem = DocTypes.EBMLMaxSizeLength.getInstance();
		ebmlMaxSizeLenElem.setValue(8);

		final StringElement docTypeElem = DocTypes.DocType.getInstance();
		docTypeElem.setValue(this.docType);

		final UnsignedIntegerElement docTypeVersionElem = DocTypes.DocTypeVersion.getInstance();
		docTypeVersionElem.setValue("webm".equals(this.docType) ? 4 : 3);

		final UnsignedIntegerElement docTypeReadVersionElem = DocTypes.DocTypeReadVersion.getInstance();
		docTypeReadVersionElem.setValue(2);

		ebmlHeaderElem.addChildElement(ebmlVersionElem);
		ebmlHeaderElem.addChildElement(ebmlReadVersionElem);
		ebmlHeaderElem.addChildElement(ebmlMaxIdLenElem);
		ebmlHeaderElem.addChildElement(ebmlMaxSizeLenElem);
		ebmlHeaderElem.addChildElement(docTypeElem);
		ebmlHeaderElem.addChildElement(docTypeVersionElem);
		ebmlHeaderElem.addChildElement(docTypeReadVersionElem);
		ebmlHeaderElem.writeElement(this.ioDW);
	}

	void writeSegmentHeader() {
		final Segment segmentElem = new Segment();
		segmentElem.setUnknownSize(true);
		segmentElem.writeHeaderData(this.ioDW);
	}

	void writeSegmentInfo() {
		this.segmentInfoElem.update(this.ioDW);
	}

	void writeTracks() {
		this.tracks.update(this.ioDW);
	}

	/**
	 * Returns the timecode scale in nanoseconds per timecode unit.
	 *
	 * @return the timecode scale (default 1,000,000 = 1ms)
	 */
	public long getTimecodeScale() {
		return this.segmentInfoElem.getTimecodeScale();
	}

	/**
	 * Sets the timecode scale. This is the number of nanoseconds per timecode unit.
	 * Defaults to 1,000,000 (1ms per unit).
	 *
	 * @param timecodeScale the timecode scale in nanoseconds
	 */
	public void setTimecodeScale(final long timecodeScale) {
		this.segmentInfoElem.setTimecodeScale(timecodeScale);
	}

	/**
	 * Returns the file duration.
	 *
	 * @return the duration in timecode scale units
	 */
	public double getDuration() {
		return this.segmentInfoElem.getDuration();
	}

	/**
	 * Sets the duration of the file. Optional.
	 *
	 * @param duration the duration in timecode scale units
	 */
	public void setDuration(final double duration) {
		this.segmentInfoElem.setDuration(duration);
	}

	/**
	 * Adds a track to the file. Add every track before the first frame: the tracks are written with the first
	 * cluster, in as much room as they need then. After that a track is only accepted if it fits what is left.
	 *
	 * @param track the track to add
	 * @throws IllegalStateException if the first cluster is written and the track does not fit
	 */
	public void addTrack(final Track track) {
		this.tracks.addTrack(track);
	}

	/**
	 * Marks a track as silent in subsequent clusters.
	 *
	 * @param trackNumber the track number to silence
	 */
	public void silenceTrack(final long trackNumber) {
		this.cluster.silenceTrack(trackNumber);
	}

	/**
	 * Removes the silent track notation for a track.
	 *
	 * @param trackNumber the track number to unsilence
	 */
	public void unsilenceTrack(final long trackNumber) {
		this.cluster.unsilenceTrack(trackNumber);
	}

	/**
	 * Add a frame
	 *
	 * @param frame The frame to add
	 */
	public void addFrame(final Frame frame) {
		if (this.cluster.shouldFlushBefore(frame)) {
			flush();
		}
		if (!this.cluster.addFrame(frame)) {
			flush();
		}
	}

	/**
	 * Flushes pending content to disk and starts a new cluster. This is typically not necessary to call manually.
	 */
	public void flush() {
		if (this.cluster.getTracks().isEmpty()) {
			// Nothing was added since the last one: an empty cluster would only add an
			// entry to the index and a few bytes to the file.
			return;
		}
		writeTracksOnce();
		final long clusterPos = this.ioDW.getFilePointer();
		this.cueData.addCue(clusterPos, this.cluster.getClusterTimecode(), this.cluster.getTracks());
		LOG.debug("Cluster flushing, timecode {}", this.cluster.getClusterTimecode());
		this.cluster.flush(this.ioDW);
	}

	/**
	 * Finalizes the file by writing the final headers, index, and flushing data to the writer.
	 */
	public void close() {
		// The last cluster still has to go out, empty or not.
		if (!this.cluster.getTracks().isEmpty()) {
			flush();
		}
		// A file without a frame still says what tracks it has.
		writeTracksOnce();

		this.cueData.write(this.ioDW, this.metaSeek);
		this.metaSeek.update(this.ioDW);
		this.segmentInfoElem.update(this.ioDW);
		this.tracks.update(this.ioDW);

		// Patch the Segment size so browsers can seek (unknown size = live stream = no seek)
		if (this.ioDW.isSeekable()) {
			final long endPos = this.ioDW.getFilePointer();
			final long segmentSize = endPos - this.segmentDataStart;
			final byte[] encodedSize = Element.makeEbmlCodedSize(segmentSize, SEGMENT_SIZE_BYTES);
			this.ioDW.seek(this.segmentSizePosition);
			this.ioDW.write(ByteBuffer.wrap(encodedSize));
			this.ioDW.seek(endPos);
		}
	}
}
