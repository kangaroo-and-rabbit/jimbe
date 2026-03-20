package org.atriasoft.ebml2.matroska.model;

import java.util.Collection;

import org.atriasoft.ebml2.Element;
import org.atriasoft.ebml2.MasterElement;
import org.atriasoft.ebml2.UnsignedIntegerElement;
import org.atriasoft.ebml2.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cues
{
  private static final Logger LOG = LoggerFactory.getLogger(Cues.class);
  private MasterElement cues = DocTypes.Cues.getInstance();
  private long endOfEbmlHeaderBytePosition;
  
  public Cues(long endOfEbmlHeaderBytePosition)
  {
    this.endOfEbmlHeaderBytePosition = endOfEbmlHeaderBytePosition;
  }

  public void addCue(long positionInFile, long timecodeOfCluster, Collection<Integer> clusterTrackNumbers)
  {
    LOG.debug("Adding matroska cue to cues element at position [{}], using timecode [{}], for track numbers [{}]", positionInFile, timecodeOfCluster, clusterTrackNumbers);

    UnsignedIntegerElement cueTime = DocTypes.CueTime.getInstance();
    cueTime.setValue(timecodeOfCluster);
    MasterElement cuePoint = DocTypes.CuePoint.getInstance();
    MasterElement cueTrackPositions = createCueTrackPositions(positionInFile, clusterTrackNumbers);
    
    cues.addChildElement(cuePoint);
    cuePoint.addChildElement(cueTime);
    cuePoint.addChildElement(cueTrackPositions);
    
    LOG.debug("Finished adding matroska cue to cues element");
  }

  private MasterElement createCueTrackPositions(final long positionInFile, final Collection<Integer> trackNumbers)
  {
    MasterElement cueTrackPositions = DocTypes.CueTrackPositions.getInstance();
    
    for (Integer trackNumber : trackNumbers)
    {
      UnsignedIntegerElement cueTrack = DocTypes.CueTrack.getInstance();
      cueTrack.setValue(trackNumber);
      
      UnsignedIntegerElement cueClusterPosition = DocTypes.CueClusterPosition.getInstance();
      cueClusterPosition.setValue(getPositionRelativeToSegmentEbmlElement(positionInFile));
      
      cueTrackPositions.addChildElement(cueTrack);
      cueTrackPositions.addChildElement(cueClusterPosition);
    }
    return cueTrackPositions;
  }
  
  public Element write(DataWriter ioDW, MetaSeek metaSeek)
  {
    long currentBytePositionInFile = ioDW.getFilePointer();
    LOG.debug("Writing matroska cues at file byte position [{}]", currentBytePositionInFile);
    long numberOfBytesInCueData = cues.writeElement(ioDW);
    LOG.debug("Done writing matroska cues, number of bytes was [{}]", numberOfBytesInCueData);
    
    metaSeek.addIndexedElement(cues, getPositionRelativeToSegmentEbmlElement(currentBytePositionInFile));
    
    return cues;
  }

  private long getPositionRelativeToSegmentEbmlElement(long currentBytePositionInFile)
  {
    return currentBytePositionInFile - endOfEbmlHeaderBytePosition;
  }
}
