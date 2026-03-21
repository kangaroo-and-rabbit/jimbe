package org.atriasoft.jimbe.matroska.model;

import java.util.Collection;

import org.atriasoft.jimbe.ebml.model.Element;
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
	 * @param timecodeOfCluster the timecode of the cluster
	 * @param clusterTrackNumbers the track numbers that have data in the cluster
	 */
	public void addCue(
			final long positionInFile,
			final long timecodeOfCluster,
			final Collection<Integer> clusterTrackNumbers) {
		LOG.debug("Adding matroska cue to cues element at position [{}], using timecode [{}], for track numbers [{}]",
				positionInFile, timecodeOfCluster, clusterTrackNumbers);

		final UnsignedIntegerElement cueTime = DocTypes.CueTime.getInstance();
		cueTime.setValue(timecodeOfCluster);
		final MasterElement cuePoint = DocTypes.CuePoint.getInstance();
		final MasterElement cueTrackPositions = createCueTrackPositions(positionInFile, clusterTrackNumbers);

		this.cues.addChildElement(cuePoint);
		cuePoint.addChildElement(cueTime);
		cuePoint.addChildElement(cueTrackPositions);

		LOG.debug("Finished adding matroska cue to cues element");
	}

	private MasterElement createCueTrackPositions(final long positionInFile, final Collection<Integer> trackNumbers) {
		final MasterElement cueTrackPositions = DocTypes.CueTrackPositions.getInstance();

		for (final Integer trackNumber : trackNumbers) {
			final UnsignedIntegerElement cueTrack = DocTypes.CueTrack.getInstance();
			cueTrack.setValue(trackNumber);

			final UnsignedIntegerElement cueClusterPosition = DocTypes.CueClusterPosition.getInstance();
			cueClusterPosition.setValue(getPositionRelativeToSegmentEbmlElement(positionInFile));

			cueTrackPositions.addChildElement(cueTrack);
			cueTrackPositions.addChildElement(cueClusterPosition);
		}
		return cueTrackPositions;
	}

	/**
	 * Writes the accumulated cue data to the output and registers the cues element
	 * in the provided {@link MetaSeek} index.
	 *
	 * @param ioDW the data writer to write to
	 * @param metaSeek the MetaSeek index to register this element in
	 * @return the written Cues element
	 */
	public Element write(final DataWriter ioDW, final MetaSeek metaSeek) {
		final long currentBytePositionInFile = ioDW.getFilePointer();
		LOG.debug("Writing matroska cues at file byte position [{}]", currentBytePositionInFile);
		final long numberOfBytesInCueData = this.cues.writeElement(ioDW);
		LOG.debug("Done writing matroska cues, number of bytes was [{}]", numberOfBytesInCueData);

		metaSeek.addIndexedElement(this.cues, getPositionRelativeToSegmentEbmlElement(currentBytePositionInFile));

		return this.cues;
	}

	private long getPositionRelativeToSegmentEbmlElement(final long currentBytePositionInFile) {
		return currentBytePositionInFile - this.endOfEbmlHeaderBytePosition;
	}
}
