package org.atriasoft.jimbe.matroska.model;

import java.util.Collection;

import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages Matroska Cue points (seek index) for efficient seeking within a file.
 * <p>
 * Cues map timecodes to byte positions of clusters in the file, allowing players
 * to quickly seek to any point in the media. Each cue point records the timecode,
 * the file position of the cluster, and the track numbers present in that cluster.
 * </p>
 *
 * @see MetaSeek
 * @see Cluster
 */
public class Cues {
	private static final Logger LOG = LoggerFactory.getLogger(Cues.class);
	private final MasterElement cues = DocTypes.Cues.getInstance();
	private final long endOfEbmlHeaderBytePosition;

	/**
	 * Constructs a Cues instance.
	 *
	 * @param endOfEbmlHeaderBytePosition the byte position of the first byte after
	 *        the Segment element header, used as the reference for relative positions
	 */
	public Cues(final long endOfEbmlHeaderBytePosition) {
		this.endOfEbmlHeaderBytePosition = endOfEbmlHeaderBytePosition;
	}

	/**
	 * Adds a cue point entry for a cluster.
	 *
	 * @param positionInFile the absolute byte position of the cluster in the file
	 * @param timecode the timecode a player can start from in that cluster
	 * @param trackNumbers the track numbers it can start from there
	 */
	public void addCue(final long positionInFile, final long timecode, final Collection<Integer> trackNumbers) {
		LOG.debug("Adding matroska cue to cues element at position [{}], using timecode [{}], for track numbers [{}]",
				positionInFile, timecode, trackNumbers);

		final UnsignedIntegerElement cueTime = DocTypes.CueTime.getInstance();
		cueTime.setValue(timecode);
		final MasterElement cuePoint = DocTypes.CuePoint.getInstance();

		cuePoint.addChildElement(cueTime);
		addCueTrackPositions(cuePoint, positionInFile, trackNumbers);
		this.cues.addChildElement(cuePoint);

		LOG.debug("Finished adding matroska cue to cues element");
	}

	private void addCueTrackPositions(
			final MasterElement cuePoint,
			final long positionInFile,
			final Collection<Integer> trackNumbers) {
		for (final Integer trackNumber : trackNumbers) {
			final MasterElement cueTrackPositions = DocTypes.CueTrackPositions.getInstance();

			final UnsignedIntegerElement cueTrack = DocTypes.CueTrack.getInstance();
			cueTrack.setValue(trackNumber);

			final UnsignedIntegerElement cueClusterPosition = DocTypes.CueClusterPosition.getInstance();
			cueClusterPosition.setValue(getPositionRelativeToSegmentEbmlElement(positionInFile));

			cueTrackPositions.addChildElement(cueTrack);
			cueTrackPositions.addChildElement(cueClusterPosition);

			cuePoint.addChildElement(cueTrackPositions);
		}
	}

	/**
	 * Writes the accumulated cue data to the output and registers the cues element
	 * in the provided {@link MetaSeek} index.
	 *
	 * @param ioDW the data writer to write to
	 * @param metaSeek the MetaSeek index to register this element in
	 */
	public void write(final DataWriter ioDW, final MetaSeek metaSeek) {
		final long currentBytePositionInFile = ioDW.getFilePointer();
		LOG.debug("Writing matroska cues at file byte position [{}]", currentBytePositionInFile);
		final long numberOfBytesInCueData = this.cues.writeElement(ioDW);
		LOG.debug("Done writing matroska cues, number of bytes was [{}]", numberOfBytesInCueData);

		// The index wants where the element is in the file: it makes the position relative to
		// the segment itself. Handing it one already made relative subtracts that twice, and the
		// entry then points a little before the cues — into the last cluster, where a player
		// finds no index at all and falls back to reading the whole file to seek in it.
		metaSeek.addIndexedElement(this.cues, currentBytePositionInFile);
	}

	private long getPositionRelativeToSegmentEbmlElement(final long currentBytePositionInFile) {
		return currentBytePositionInFile - this.endOfEbmlHeaderBytePosition;
	}
}
