package org.atriasoft.jimbe.matroska.model;

import java.util.Date;

import org.atriasoft.jimbe.ebml.model.DateElement;
import org.atriasoft.jimbe.ebml.model.FloatElement;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.StringElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the Matroska Segment Info element, which stores metadata about the segment
 * such as the timecode scale, duration, creation date, and application identifiers.
 * <p>
 * The segment info is written with reserved space so it can be updated in place
 * (e.g., to set the final duration after all frames have been written).
 * </p>
 */
public class SegmentInfo {
	private static final Logger LOG = LoggerFactory.getLogger(SegmentInfo.class);
	private static final long BLOCK_SIZE = 128;

	private long timecodeScale = 1000000;
	private Double duration;
	private Date segmentDate = new Date();
	private final long myPosition;

	/**
	 * Constructs a SegmentInfo at the given file position.
	 *
	 * @param position the byte position in the output where the segment info will be written
	 */
	public SegmentInfo(final long position) {
		this.myPosition = position;
	}

	/**
	 * Writes the Segment Info element to the output, followed by padding to fill
	 * the reserved block size.
	 *
	 * @param ioDW the data writer to write to
	 * @return 1 on success
	 */
	public long writeElement(final DataWriter ioDW) {
		final MasterElement segmentInfoElem = DocTypes.Info.getInstance();

		final StringElement writingAppElem = DocTypes.WritingApp.getInstance();
		writingAppElem.setValue("Matroska File Writer v1.0");

		final StringElement muxingAppElem = DocTypes.MuxingApp.getInstance();
		muxingAppElem.setValue("JEBML v1.0");

		final DateElement dateElem = DocTypes.DateUTC.getInstance();
		dateElem.setDate(this.segmentDate);

		// Add timecode scale
		final UnsignedIntegerElement timecodescaleElem = DocTypes.TimecodeScale.getInstance();
		timecodescaleElem.setValue(this.timecodeScale);

		segmentInfoElem.addChildElement(dateElem);
		segmentInfoElem.addChildElement(timecodescaleElem);

		if (this.duration != null) {
			final FloatElement durationElem = DocTypes.Duration.getInstance();
			durationElem.setValue(this.duration);
			segmentInfoElem.addChildElement(durationElem);
		}

		segmentInfoElem.addChildElement(writingAppElem);
		segmentInfoElem.addChildElement(muxingAppElem);

		final long len = segmentInfoElem.writeElement(ioDW);
		final VoidElement spacer = new VoidElement(BLOCK_SIZE - len);
		spacer.writeElement(ioDW);
		return 1;
	}

	/**
	 * Updates the segment info in place by seeking back to the original position
	 * and rewriting it (e.g., to update the duration after muxing is complete).
	 *
	 * @param ioDW the data writer containing this segment info
	 */
	public void update(final DataWriter ioDW) {
		LOG.debug("Updating segment info header");
		final long startingPos = ioDW.getFilePointer();
		ioDW.seek(this.myPosition);
		writeElement(ioDW);
		ioDW.seek(startingPos);
	}

	/** Returns the duration of the segment in timecode scale units. */
	public double getDuration() {
		return this.duration;
	}

	/** Sets the duration of the segment in timecode scale units. */
	public void setDuration(final double duration) {
		this.duration = duration;
	}

	/** Returns the creation date of the segment. */
	public Date getDate() {
		return this.segmentDate;
	}

	/** Sets the creation date of the segment. */
	public void setDate(final Date date) {
		this.segmentDate = date;
	}

	/** Returns the timecode scale in nanoseconds (default is 1000000, i.e., 1ms). */
	public long getTimecodeScale() {
		return this.timecodeScale;
	}

	/** Sets the timecode scale in nanoseconds. */
	public void setTimecodeScale(final long timecodeScale) {
		this.timecodeScale = timecodeScale;
	}
}
