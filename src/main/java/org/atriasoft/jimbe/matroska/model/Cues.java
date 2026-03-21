package org.atriasoft.jimbe.matroska.model;

import java.util.Collection;

import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cues {
	private static final Logger LOG = LoggerFactory.getLogger(Cues.class);
	private final MasterElement cues = DocTypes.Cues.getInstance();
	private final long endOfEbmlHeaderBytePosition;

	public Cues(final long endOfEbmlHeaderBytePosition) {
		this.endOfEbmlHeaderBytePosition = endOfEbmlHeaderBytePosition;
	}

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
