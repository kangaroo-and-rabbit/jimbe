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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.atriasoft.jimbe.ebml.model.DateElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.FloatElement;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.SignedIntegerElement;
import org.atriasoft.jimbe.ebml.model.StringElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataSource;
import org.atriasoft.jimbe.matroska.model.Block;
import org.atriasoft.jimbe.matroska.model.DocTypes;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.Tag;
import org.atriasoft.jimbe.matroska.model.TagEntry;
import org.atriasoft.jimbe.matroska.model.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level Matroska/WebM file reader. Parses the EBML header, segment info, tracks, tags,
 * and clusters, providing frame-by-frame access with optional seeking.
 *
 * <p>Usage: construct with a {@link DataSource}, call {@link #readFile()}, then iterate
 * with {@link #getNextFrame()} or {@link #getNextFrame(int)}.</p>
 */
public class Reader {
	/**
	 * Number of Clusters to search before assuming that a track has ended.
	 */
	public static final int CLUSTER_TRACK_SEARCH_COUNT = 4;
	protected static final Logger LOG = LoggerFactory.getLogger(Reader.class);

	static {
		DocTypes.ensureRegistered();
	}

	private final DataSource ioDS;
	private final org.atriasoft.jimbe.ebml.Reader reader;
	private final Object fillLock = new Object();
	private Element level0 = null;
	private String segmentTitle;
	private Date segmentDate;
	private String muxingApp;
	private String writingApp;
	private volatile long timecodeScale = 1000000;
	private volatile double duration;
	private final ArrayList<Track> trackList = new ArrayList<>();
	private final ArrayList<TagEntry> tagList = new ArrayList<>();
	private final Queue<Frame> frameQueue = new ConcurrentLinkedQueue<>();
	private volatile boolean scanFirstCluster = true;
	private volatile long firstClusterFilePosition = -1;

	/** Where the data of the Segment starts: the positions stored in the file are relative to it. */
	private volatile long segmentDataPosition = -1;

	/** Where the SeekHead is, relative to the Segment, or -1 if there is none before the first cluster. */
	private long seekHeadPosition = -1;

	/** Where the Cues are, relative to the Segment, or -1 when the file does not say. */
	private long cuesPosition = -1;

	/** The clusters the Cues point at, in file order. Null until {@link #seek(long)} needs them. */
	private List<CuePosition> cueIndex = null;

	/** A place the Cues point at: a timecode, and the cluster to read from, relative to the Segment. */
	private record CuePosition(
			long timecode,
			long clusterPosition) {}

	/**
	 * Primary Constructor for Matroska File class.
	 *
	 * @param inputDataSource DataSource to read the Matroska file from
	 */
	public Reader(final DataSource inputDataSource) {
		this.ioDS = inputDataSource;
		this.reader = new org.atriasoft.jimbe.ebml.Reader(this.ioDS);
	}

	/**
	 * Read / Parse the Matroska file. Call this before any other method.
	 *
	 * @throws RuntimeException On various errors
	 */
	public void readFile() {
		Element level1 = null;
		final Element level2 = null;
		// Element level3 = null;
		// Element level4 = null;

		this.level0 = this.reader.readNextElement();
		if (this.level0 == null) {
			throw new java.lang.RuntimeException("Error: Unable to scan for EBML elements");
		}

		if (this.level0.isType(DocTypes.EBML.getType())) {
			level1 = ((MasterElement) this.level0).readNextChild(this.reader);

			while (level1 != null) {
				// System.out.printf("Found element %s\n", level1.getElementType().name);
				level1.readData(this.ioDS);
				if (level1.isType(DocTypes.DocType.getType())) {
					final String docType = ((StringElement) level1).getValue();
					if (docType.compareTo("matroska") != 0 && docType.compareTo("webm") != 0) {
						throw new java.lang.RuntimeException(
								"Error: DocType is not matroska, \"" + ((StringElement) level1).getValue() + "\"");
					}
				}
				level1 = ((MasterElement) this.level0).readNextChild(this.reader);
			}
		} else {
			throw new java.lang.RuntimeException("Error: EBML Header not the first element in the file");
		}

		this.level0 = this.reader.readNextElement();
		if (this.level0.isType(DocTypes.Segment.getType())) {
			this.segmentDataPosition = this.ioDS.getFilePointer();
			level1 = ((MasterElement) this.level0).readNextChild(this.reader);
			LOG.debug("Got segment element");
			while (level1 != null) {
				LOG.debug("Got {} element in segment", level1.getElementType().getName());
				if (level1.isType(DocTypes.Info.getType())) {
					parseSegmentInfo(level1, level2);

				} else if (level1.isType(DocTypes.Tracks.getType())) {
					parseTracks(level1, level2);

				} else if (level1.isType(DocTypes.SeekHead.getType())) {
					// Only a seek needs it: a damaged index must not keep the file from opening.
					this.seekHeadPosition = positionInSegment(level1);

				} else if (level1.isType(DocTypes.Cues.getType())) {
					// Most files have them after the clusters, where only the SeekHead tells.
					this.cuesPosition = positionInSegment(level1);

				} else if (level1.isType(DocTypes.Cluster.getType())) {
					// Remember where the first Cluster header starts for seek() support.
					// After readNextChild, the file pointer is right after the Cluster header.
					// Compute the header start by subtracting the header size.
					final long clusterHeaderSize = level1.getTotalSize() - level1.getSize();
					this.firstClusterFilePosition = this.ioDS.getFilePointer() - clusterHeaderSize;
					if (this.scanFirstCluster) {
						parseNextCluster(level1);
					}
					// Break out of this loop, we should only parse the first cluster
					break;

				} else if (level1.isType(DocTypes.Tags.getType())) {
					parseTags(level1, level2);

				}

				level1.skipData(this.ioDS);
				level1 = ((MasterElement) this.level0).readNextChild(this.reader);
			}
		} else {
			throw new java.lang.RuntimeException(
					String.format("Error: Segment not the second element in the file: was %s instead",
							this.level0.getElementType().getName()));
		}
	}

	/**
	 * Get the Next Frame
	 *
	 * @return The next Frame in the queue, or null if the file has ended
	 */
	public Frame getNextFrame() {
		if (this.frameQueue.isEmpty()) {
			fillFrameQueue();
		}

		// If FrameQueue is still empty, must be the end of the file
		if (this.frameQueue.isEmpty()) {
			return null;
		}
		return this.frameQueue.remove();
	}

	/**
	 * Get the Next Frame, limited by TrackNo
	 *
	 * @param trackNo The track number to only get Frame(s) from
	 * @return The next Frame in the queue, or null if there are no more frames for the TrackNo track
	 */
	public Frame getNextFrame(final int trackNo) {
		int tryCount = 0;
		try {
			while (true) {
				if (this.frameQueue.isEmpty()) {
					fillFrameQueue();
				}
				if (this.frameQueue.isEmpty()) {
					return null;
				}
				final Iterator<Frame> iter = this.frameQueue.iterator();
				while (iter.hasNext()) {
					final Frame frame = iter.next();
					if (frame.getTrackNo() == trackNo) {
						synchronized (this.frameQueue) {
							iter.remove();
						}
						return frame;
					}
				}
				// No matching frame found in current queue, load more
				fillFrameQueue();
				if (++tryCount > CLUSTER_TRACK_SEARCH_COUNT) {
					return null;
				}
			}
		} catch (final RuntimeException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Checks whether seeking is supported by the underlying data source.
	 *
	 * @return {@code true} if seeking is supported
	 */
	public boolean isSeekable() {
		return this.ioDS.isSeekable();
	}

	/**
	 * Seeks to the requested timecode by scanning clusters and discarding frames
	 * until reaching the nearest timecode less than or equal to the requested one.
	 * The scan starts from the cluster the Cues give for that timecode. Without usable
	 * Cues it starts from the first cluster, which finds the same frame after reading
	 * everything that comes before it.
	 *
	 * <p>For example, if frames exist at 0ms, 50ms, 100ms, ... 450ms and we seek to 333ms,
	 * the seek lands on Frame 7 at 300ms (the last frame with timecode &lt;= 333ms).</p>
	 *
	 * @param timecode timecode to seek to in milliseconds
	 * @return the actual timecode reached, or {@code 0} if no frame was found
	 */
	public long seek(final long timecode) {
		synchronized (this.fillLock) {
			return seekLocked(timecode);
		}
	}

	private long seekLocked(final long timecode) {
		if (this.level0 == null) {
			throw new IllegalStateException("Call readFile() before seeking");
		}
		if (this.firstClusterFilePosition < 0) {
			throw new IllegalStateException("No cluster found in file, cannot seek");
		}
		if (!this.ioDS.isSeekable()) {
			throw new UnsupportedOperationException("DataSource is not seekable");
		}

		if (this.cueIndex == null) {
			this.cueIndex = readCueIndex();
		}
		// Clusters overlap in time: blocks are not sorted across tracks, so the cluster before the one the cue
		// names can still hold frames that are due, and the frame the cue names may not come first in its own.
		// One cue earlier gives what a scan from the start of the file would find; it goes further back only
		// when that cluster already starts past the timecode.
		int cue = Math.max(lastCueAt(timecode) - 1, -1);
		while (true) {
			final boolean usable = cue >= 0 && isClusterAt(positionOfCue(cue));
			if (cue >= 0 && !usable) {
				LOG.warn("The cues point at {}, where there is no cluster: reading from the first one",
						positionOfCue(cue));
			}
			final Frame seekTarget = scanTo(usable ? positionOfCue(cue) : this.firstClusterFilePosition, timecode);
			if (seekTarget != null) {
				// Re-insert the seek target at the head of the queue.
				// Drain, prepend, re-add.
				final ArrayList<Frame> remaining = new ArrayList<>(this.frameQueue);
				this.frameQueue.clear();
				this.frameQueue.add(seekTarget);
				this.frameQueue.addAll(remaining);
				return seekTarget.getTimecode();
			}
			if (!usable) {
				return 0;
			}
			cue--;
		}
	}

	/**
	 * Reads frames from a cluster until one is past the timecode, leaving the file there.
	 *
	 * @return the last frame at or before the timecode, removed from the queue, or {@code null} if none was found
	 */
	private Frame scanTo(final long clusterPosition, final long timecode) {
		// Clear any buffered frames
		this.frameQueue.clear();

		// Move to the header of the cluster
		this.ioDS.seek(clusterPosition);
		// Reset the Segment element's usedSize so readNextChild() can re-read clusters
		((MasterElement) this.level0).setUsedSize(clusterPosition - this.segmentDataPosition);

		// Read frames, discarding those before the target timecode.
		// Per the Javadoc: discard until we hit a timecode larger than requested,
		// then the seek target is the last frame with timecode <= requested.
		// After seek(), getNextFrame() returns that frame.
		Frame seekTarget = null;
		while (true) {
			if (this.frameQueue.isEmpty()) {
				fillFrameQueue();
			}
			if (this.frameQueue.isEmpty()) {
				// End of file. Return what we have.
				return seekTarget;
			}
			final Frame head = this.frameQueue.peek();
			if (head.getTimecode() > timecode) {
				// Passed the target. seekTarget is the last frame <= timecode.
				return seekTarget;
			}
			// This frame's timecode <= requested. Remove and keep as candidate.
			this.frameQueue.remove();
			seekTarget = head;
		}
	}

	/** The index of the last cue at or before the timecode, -1 if there is none. */
	private int lastCueAt(final long timecode) {
		int found = -1;
		for (int i = 0; i < this.cueIndex.size() && this.cueIndex.get(i).timecode() <= timecode; i++) {
			found = i;
		}
		return found;
	}

	private long positionOfCue(final int cue) {
		return this.segmentDataPosition + this.cueIndex.get(cue).clusterPosition();
	}

	private boolean isClusterAt(final long position) {
		try {
			this.ioDS.seek(position);
			final Element element = this.reader.readNextElement();
			return element != null && element.isType(DocTypes.Cluster.getType());
		} catch (final RuntimeException ex) {
			LOG.debug("Can not read at {}", position, ex);
			return false;
		}
	}

	/**
	 * Reads the Cues as positions in the file. An index that can not be used comes back empty: seeking
	 * then only costs more, while a wrong index would land on the wrong frames.
	 */
	private List<CuePosition> readCueIndex() {
		try {
			if (this.cuesPosition < 0 && this.seekHeadPosition >= 0) {
				this.ioDS.seek(this.segmentDataPosition + this.seekHeadPosition);
				final Element seekHead = this.reader.readNextElement();
				if (seekHead != null && seekHead.isType(DocTypes.SeekHead.getType())) {
					parseSeekHead(seekHead);
				}
			}
			if (this.cuesPosition < 0) {
				return List.of();
			}
			this.ioDS.seek(this.segmentDataPosition + this.cuesPosition);
			final Element cues = this.reader.readNextElement();
			if (cues == null || !cues.isType(DocTypes.Cues.getType())) {
				LOG.warn("The SeekHead points at {}, where there are no cues", this.cuesPosition);
				return List.of();
			}
			final List<CuePosition> index = new ArrayList<>();
			Element cuePoint = ((MasterElement) cues).readNextChild(this.reader);
			while (cuePoint != null) {
				if (cuePoint.isType(DocTypes.CuePoint.getType())) {
					index.add(parseCuePoint(cuePoint));
				}
				cuePoint.skipData(this.ioDS);
				cuePoint = ((MasterElement) cues).readNextChild(this.reader);
			}
			return followsTheFile(index) ? index : List.of();
		} catch (final RuntimeException ex) {
			LOG.warn("Can not read the cues: {}", ex.getMessage());
			return List.of();
		}
	}

	/** Several tracks may share a cue point: the earliest of their clusters is where all of them can be found. */
	private CuePosition parseCuePoint(final Element cuePoint) {
		long timecode = -1;
		long clusterPosition = Long.MAX_VALUE;
		Element child = ((MasterElement) cuePoint).readNextChild(this.reader);
		while (child != null) {
			if (child.isType(DocTypes.CueTime.getType())) {
				child.readData(this.ioDS);
				timecode = ((UnsignedIntegerElement) child).getValue();

			} else if (child.isType(DocTypes.CueTrackPositions.getType())) {
				Element position = ((MasterElement) child).readNextChild(this.reader);
				while (position != null) {
					if (position.isType(DocTypes.CueClusterPosition.getType())) {
						position.readData(this.ioDS);
						clusterPosition = Math.min(clusterPosition, ((UnsignedIntegerElement) position).getValue());
					}
					position.skipData(this.ioDS);
					position = ((MasterElement) child).readNextChild(this.reader);
				}
			}
			child.skipData(this.ioDS);
			child = ((MasterElement) cuePoint).readNextChild(this.reader);
		}
		return new CuePosition(timecode, clusterPosition);
	}

	/**
	 * An index is only worth following if time and position advance together. In a file whose clusters are out
	 * of order, the last cue before a timecode can sit past frames that are still to come.
	 */
	private boolean followsTheFile(final List<CuePosition> index) {
		long timecode = 0;
		long position = this.firstClusterFilePosition - this.segmentDataPosition;
		for (final CuePosition cue : index) {
			if (cue.timecode() < timecode || cue.clusterPosition() < position
					|| cue.clusterPosition() == Long.MAX_VALUE) {
				LOG.debug("Ignoring the cues: {} does not follow the file", cue);
				return false;
			}
			timecode = cue.timecode();
			position = cue.clusterPosition();
		}
		return true;
	}

	private void fillFrameQueue() {
		if (this.level0 == null) {
			throw new java.lang.IllegalStateException("Call readFile() before reading frames");
		}

		synchronized (this.fillLock) {
			Element level1 = ((MasterElement) this.level0).readNextChild(this.reader);
			while (level1 != null) {
				if (level1.isType(DocTypes.Cluster.getType())) {
					parseNextCluster(level1);
					level1.skipData(this.ioDS);
					break;
				}

				level1.skipData(this.ioDS);
				level1 = ((MasterElement) this.level0).readNextChild(this.reader);
			}
		}
	}

	private void parseNextCluster(final Element level1) {
		Element level2 = null;
		Element level3 = null;
		long clusterTimecode = 0;
		level2 = ((MasterElement) level1).readNextChild(this.reader);

		while (level2 != null) {
			if (level2.isType(DocTypes.Timecode.getType())) {
				level2.readData(this.ioDS);
				clusterTimecode = ((UnsignedIntegerElement) level2).getValue();

			} else if (level2.isType(DocTypes.SimpleBlock.getType())) {
				level2.readData(this.ioDS);
				Block block = null;
				final long blockDuration = Long.MIN_VALUE;
				block = new Block(level2.getData());

				block.parseBlock();
				final Frame frame = new Frame();
				frame.setTrackNo(block.getTrackNo());
				frame.setTimecode(block.getAdjustedBlockTimecode(clusterTimecode, this.timecodeScale));
				frame.setDuration(blockDuration);
				frame.setData(block.getFrame(0));
				frame.setKeyFrame(block.isKeyFrame());
				synchronized (this.frameQueue) {
					this.frameQueue.add(new Frame(frame));
				}

				if (block.getFrameCount() > 1) {
					for (int f = 1; f < block.getFrameCount(); f++) {
						frame.setData(block.getFrame(f));
						this.frameQueue.add(new Frame(frame));
					}
				}
				level2.skipData(this.ioDS);

			} else if (level2.isType(DocTypes.BlockGroup.getType())) {
				long blockDuration = 0;
				long discardPadding = 0;
				boolean hasReference = false;
				final ArrayList<Long> references = new ArrayList<>();
				level3 = ((MasterElement) level2).readNextChild(this.reader);
				Block block = null;
				while (level3 != null) {
					if (level3.isType(DocTypes.Block.getType())) {
						level3.readData(this.ioDS);
						block = new Block(level3.getData());
						block.parseBlock();

					} else if (level3.isType(DocTypes.BlockDuration.getType())) {
						level3.readData(this.ioDS);
						blockDuration = ((UnsignedIntegerElement) level3).getValue();

					} else if (level3.isType(DocTypes.DiscardPadding.getType())) {
						level3.readData(this.ioDS);
						discardPadding = ((SignedIntegerElement) level3).getValue();

					} else if (level3.isType(DocTypes.ReferenceBlock.getType())) {
						level3.readData(this.ioDS);
						hasReference = true;
						references.add(((SignedIntegerElement) level3).getValue());
					}

					level3.skipData(this.ioDS);
					level3 = ((MasterElement) level2).readNextChild(this.reader);
				}

				if (block == null) {
					throw new java.lang.NullPointerException("BlockGroup element with no child Block!");
				}

				final Frame frame = new Frame();
				frame.setTrackNo(block.getTrackNo());
				frame.setTimecode(block.getAdjustedBlockTimecode(clusterTimecode, this.timecodeScale));
				frame.setDuration(blockDuration);
				// In Matroska, a BlockGroup without ReferenceBlock is a keyframe
				frame.setKeyFrame(!hasReference);
				for (final Long ref : references) {
					frame.addReferences(ref);
				}
				frame.setData(block.getFrame(0));
				// The padding comes off the end of the block: that is its last frame.
				frame.setDiscardPadding(block.getFrameCount() == 1 ? discardPadding : 0);
				this.frameQueue.add(new Frame(frame));

				if (block.getFrameCount() > 1) {
					for (int f = 1; f < block.getFrameCount(); f++) {
						frame.setData(block.getFrame(f));
						frame.setDiscardPadding(f == block.getFrameCount() - 1 ? discardPadding : 0);
						/*
						 * if (badMP3Headers()) { throw new RuntimeException("Bad Data!"); }
						 */

						this.frameQueue.add(new Frame(frame));
						/*
						 * if (badMP3Headers()) { throw new RuntimeException("Bad Data!"); }
						 */
					}
				}
			}

			level2.skipData(this.ioDS);
			level2 = ((MasterElement) level1).readNextChild(this.reader);
		}
	}

	protected boolean badMP3Headers() {
		final Iterator<Frame> iter = this.frameQueue.iterator();
		while (iter.hasNext()) {
			final Frame frame = iter.next();
			if (frame.getTrackNo() == 2 && frame.getData().get(3) != 0x54) {
				throw new RuntimeException("Bad MP3 Header! Index: " + iter);
			}
		}
		return false;
	}

	/** Where an element is, relative to the Segment. Right after reading its header, before anything else. */
	private long positionInSegment(final Element element) {
		final long headerSize = element.getTotalSize() - element.getSize();
		return this.ioDS.getFilePointer() - headerSize - this.segmentDataPosition;
	}

	private void parseSeekHead(final Element level1) {
		Element seek = ((MasterElement) level1).readNextChild(this.reader);
		while (seek != null) {
			if (seek.isType(DocTypes.Seek.getType())) {
				parseSeekEntry(seek);
			}
			seek.skipData(this.ioDS);
			seek = ((MasterElement) level1).readNextChild(this.reader);
		}
	}

	private void parseSeekEntry(final Element seek) {
		ByteBuffer id = null;
		long position = -1;
		Element child = ((MasterElement) seek).readNextChild(this.reader);
		while (child != null) {
			if (child.isType(DocTypes.SeekID.getType())) {
				child.readData(this.ioDS);
				id = child.getData();

			} else if (child.isType(DocTypes.SeekPosition.getType())) {
				child.readData(this.ioDS);
				position = ((UnsignedIntegerElement) child).getValue();
			}
			child.skipData(this.ioDS);
			child = ((MasterElement) seek).readNextChild(this.reader);
		}
		if (position >= 0 && DocTypes.Cues.getType().equals(id)) {
			this.cuesPosition = position;
		}
	}

	private void parseSegmentInfo(final Element level1, Element level2) {
		level2 = ((MasterElement) level1).readNextChild(this.reader);

		while (level2 != null) {
			if (level2.isType(DocTypes.Title.getType())) {
				level2.readData(this.ioDS);
				this.segmentTitle = ((StringElement) level2).getValue();

			} else if (level2.isType(DocTypes.DateUTC.getType())) {
				level2.readData(this.ioDS);
				this.segmentDate = ((DateElement) level2).getDate();

			} else if (level2.isType(DocTypes.MuxingApp.getType())) {
				level2.readData(this.ioDS);
				this.muxingApp = ((StringElement) level2).getValue();

			} else if (level2.isType(DocTypes.WritingApp.getType())) {
				level2.readData(this.ioDS);
				this.writingApp = ((StringElement) level2).getValue();

			} else if (level2.isType(DocTypes.Duration.getType())) {
				level2.readData(this.ioDS);
				this.duration = ((FloatElement) level2).getValue();

			} else if (level2.isType(DocTypes.TimecodeScale.getType())) {
				level2.readData(this.ioDS);

				this.timecodeScale = ((UnsignedIntegerElement) level2).getValue();
			}

			level2.skipData(this.ioDS);
			level2 = ((MasterElement) level1).readNextChild(this.reader);
		}
	}

	private void parseTracks(final Element level1, Element level2) {
		level2 = ((MasterElement) level1).readNextChild(this.reader);

		while (level2 != null) {
			if (level2.isType(DocTypes.TrackEntry.getType())) {
				this.trackList.add(Track.fromElement(level2, this.ioDS, this.reader));
			}
			level2.skipData(this.ioDS);
			level2 = ((MasterElement) level1).readNextChild(this.reader);
		}
	}

	private void parseTags(final Element level1, Element level2) {
		Element level3 = null;
		Element level4 = null;
		level2 = ((MasterElement) level1).readNextChild(this.reader);

		while (level2 != null) {
			if (level2.isType(DocTypes.Tag.getType())) {
				final TagEntry tag = new TagEntry();
				level3 = ((MasterElement) level2).readNextChild(this.reader);

				while (level3 != null) {
					if (level3.isType(DocTypes.Targets.getType())) {
						level4 = ((MasterElement) level3).readNextChild(this.reader);

						while (level4 != null) {
							if (level4.isType(DocTypes.TagTrackUID.getType())) {
								level4.readData(this.ioDS);
								tag.trackUID.add(Long.valueOf(((UnsignedIntegerElement) level4).getValue()));

							} else if (level4.isType(DocTypes.TagChapterUID.getType())) {
								level4.readData(this.ioDS);
								tag.chapterUID.add(Long.valueOf(((UnsignedIntegerElement) level4).getValue()));

							} else if (level4.isType(DocTypes.TagAttachmentUID.getType())) {
								level4.readData(this.ioDS);
								tag.attachmentUID.add(Long.valueOf(((UnsignedIntegerElement) level4).getValue()));
							}

							level4.skipData(this.ioDS);
							level4 = ((MasterElement) level3).readNextChild(this.reader);
						}

					} else if (level3.isType(DocTypes.SimpleTag.getType())) {
						tag.simpleTags.add(parseTagsSimpleTag(level3, level4));
					}
					level3.skipData(this.ioDS);
					level3 = ((MasterElement) level2).readNextChild(this.reader);
				}
				this.tagList.add(tag);
			}

			level2.skipData(this.ioDS);
			level2 = ((MasterElement) level1).readNextChild(this.reader);
		}
	}

	private Tag parseTagsSimpleTag(final Element level3, Element level4) {
		final Tag simpleTag = new Tag();
		level4 = ((MasterElement) level3).readNextChild(this.reader);

		while (level4 != null) {
			if (level4.isType(DocTypes.TagName.getType())) {
				level4.readData(this.ioDS);
				simpleTag.name = ((StringElement) level4).getValue();

			} else if (level4.isType(DocTypes.TagString.getType())) {
				level4.readData(this.ioDS);
				simpleTag.value = ((StringElement) level4).getValue();

			}

			level4.skipData(this.ioDS);
			level4 = ((MasterElement) level3).readNextChild(this.reader);
		}

		return simpleTag;
	}

	/**
	 * Get a String report for the Matroska file. Call readFile() before this method, else the report will be empty.
	 *
	 * @return String Report
	 */
	public String getReport() {
		final java.io.StringWriter s = new java.io.StringWriter();
		int t;

		s.write("Reader report\n");

		s.write("Infomation Segment \n");
		s.write("\tSegment Title: " + this.segmentTitle + "\n");
		s.write("\tSegment Date: " + this.segmentDate + "\n");
		s.write("\tMuxing App : " + this.muxingApp + "\n");
		s.write("\tWriting App : " + this.writingApp + "\n");
		s.write("\tDuration : " + this.duration / 1000 + "sec \n");
		s.write("\tTimecodeScale : " + this.timecodeScale + "\n");

		s.write("Track Count: " + this.trackList.size() + "\n");
		for (t = 0; t < this.trackList.size(); t++) {
			s.write("\tTrack " + t + "\n");
			s.write(this.trackList.get(t).toString());
		}

		s.write("Tag Count: " + this.tagList.size() + "\n");
		for (t = 0; t < this.tagList.size(); t++) {
			s.write("\tTag Entry \n");
			s.write(this.tagList.get(t).toString());
		}

		s.write("End report\n");

		return s.getBuffer().toString();
	}

	/**
	 * Returns the writing application string from the segment info.
	 *
	 * @return the writing application name, or {@code null} if not present
	 */
	public String getWritingApp() {
		return this.writingApp;
	}

	/**
	 * Returns an array of the tracks. If there are no Tracks to return the returned array will have a size of 0.
	 *
	 * @return Array of Track's
	 */
	public Track[] getTrackList() {
		if (this.trackList.size() > 0) {
			final Track[] tracks = new Track[this.trackList.size()];
			for (int t = 0; t < this.trackList.size(); t++) {
				tracks[t] = this.trackList.get(t);
			}
			return tracks;
		} else {
			return new Track[0];
		}
	}

	/**
	 * <p>
	 * This differs from the getTrackList method in that this method scans each track and returns the one that has the same track number as TrackNo.
	 * </p>
	 *
	 * <p>
	 * Note: TrackNo != track index
	 * </p>
	 *
	 * @param trackNo The actual track number of the Track you would like to get
	 * @return null if no Track is found with the requested TrackNo
	 */
	public Track getTrack(final int trackNo) {
		for (final Track track : this.trackList) {
			if (track.getTrackNo() == trackNo) {
				return track;
			}
		}
		return null;
	}

	/**
	 * Get the timecode scale for this Reader. In Matroska the timecodes are stored scaled by this value. However any Frame you get
	 * through the methods of this class will already have the timecodes correctly scaled to millseconds.
	 *
	 * @return TimecodeScale
	 */
	public long getTimecodeScale() {
		return this.timecodeScale;
	}

	/**
	 * Returns the segment title from the file metadata.
	 *
	 * @return the segment title, or {@code null} if not present
	 */
	public String getSegmentTitle() {
		return this.segmentTitle;
	}

	/**
	 * Returns the muxing application string from the segment info.
	 *
	 * @return the muxing application name, or {@code null} if not present
	 */
	public String getMuxingApp() {
		return this.muxingApp;
	}

	/**
	 * Get the duration for this Reader. This is the duration value stored in the segment info. Which may or may not be the exact length of all,
	 * some, or one of the tracks.
	 *
	 * @return Duration in seconds
	 */
	public double getDuration() {
		return this.duration;
	}

	/**
	 * Sets if the readFile() method should scan the first cluster for infomation. Set to false for faster parsing.
	 */
	public void setScanFirstCluster(final boolean scanFirstCluster) {
		this.scanFirstCluster = scanFirstCluster;
	}

	/**
	 * Gets if the readFile() method should scan the first cluster for infomation. When set to false parsing is slightly faster.
	 */
	public boolean getScanFirstCluster() {
		return this.scanFirstCluster;
	}
}
