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

public class SegmentInfo
{
  private static final Logger LOG = LoggerFactory.getLogger(SegmentInfo.class);
  private static final long BLOCK_SIZE = 128;

  private long timecodeScale = 1000000;
  private Double duration;
  private Date segmentDate = new Date();
  private final long myPosition;

  public SegmentInfo(final long position)
  {
    myPosition = position;
  }

  public long writeElement(final DataWriter ioDW)
  {
    final MasterElement segmentInfoElem = DocTypes.Info.getInstance();

    final StringElement writingAppElem = DocTypes.WritingApp.getInstance();
    writingAppElem.setValue("Matroska File Writer v1.0");

    final StringElement muxingAppElem = DocTypes.MuxingApp.getInstance();
    muxingAppElem.setValue("JEBML v1.0");

    final DateElement dateElem = DocTypes.DateUTC.getInstance();
    dateElem.setDate(segmentDate);

    // Add timecode scale
    final UnsignedIntegerElement timecodescaleElem = DocTypes.TimecodeScale.getInstance();
    timecodescaleElem.setValue(timecodeScale);

    segmentInfoElem.addChildElement(dateElem);
    segmentInfoElem.addChildElement(timecodescaleElem);

    if (duration != null)
    {
      final FloatElement durationElem = DocTypes.Duration.getInstance();
      durationElem.setValue(duration);
      segmentInfoElem.addChildElement(durationElem);
    }

    segmentInfoElem.addChildElement(writingAppElem);
    segmentInfoElem.addChildElement(muxingAppElem);

    final long len = segmentInfoElem.writeElement(ioDW);
    final VoidElement spacer = new VoidElement(BLOCK_SIZE - len);
    spacer.writeElement(ioDW);
    return 1;
  }

  public void update(final DataWriter ioDW)
  {
    LOG.debug("Updating segment info header");
    final long startingPos = ioDW.getFilePointer();
    ioDW.seek(myPosition);
    writeElement(ioDW);
    ioDW.seek(startingPos);
  }

  public double getDuration()
  {
    return duration;
  }

  public void setDuration(final double duration)
  {
    this.duration = duration;
  }

  public Date getDate()
  {
    return segmentDate;
  }

  public void setDate(final Date date)
  {
    this.segmentDate = date;
  }

  public long getTimecodeScale()
  {
    return timecodeScale;
  }

  public void setTimecodeScale(final long timecodeScale)
  {
    this.timecodeScale = timecodeScale;
  }
}
