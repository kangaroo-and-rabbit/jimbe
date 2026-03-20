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

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.Reader;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.FloatElement;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.StringElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Matroska Track Class </p>
 * <p>Mandatory fields are:
 * <ul>
 *    <li>Track Type</li>
 *    <li>Codec ID</li>
 *    <li>Default duration</li>
 * </ul>
 * </p>
 * <p>Note that if the TrackType is Audio, you must add the AudioTrack member. Likewise VideoTrack for Video.</p>
 */
public class Track
{
  private static final Logger LOG = LoggerFactory.getLogger(Track.class);

  private int trackNo = 1;
  private long trackUID = 1337;
  private boolean flagEnabled = true;
  private boolean flagDefault = true;
  private boolean flagForced = false;
  private boolean flagLacing = true;
  private int minCache = 0;
  private int maxBlockAdditionalId = 0;
  private String name = "unnamed";
  private String language = "eng";
  private String codecID;
  private ByteBuffer codecPrivate;
  private long defaultDuration;
  private boolean codecDecodeAll = true;
  private int seekPreroll = 0;

  private TrackType trackType;

  private VideoTrack video = null;

  private AudioTrack audio = null;

  private TrackOperation operation = null;
  private final ArrayList<Long> overlayUids = new ArrayList<>();

  /**
   * Converts the Track to String form
   *
   * @return String form of Track data
   */
  @Override
  public String toString()
  {
    String s = new String();

    s += "\t\t" + "TrackNo: " + getTrackNo() + "\n";
    s += "\t\t" + "TrackUID: " + getTrackUID() + "\n";
    s += "\t\t" + "TrackType: " + getTrackType().name() + "\n";
    s += "\t\t" + "DefaultDuration: " + getDefaultDuration() + "\n";
    s += "\t\t" + "Name: " + getName() + "\n";
    s += "\t\t" + "Language: " + getLanguage() + "\n";
    s += "\t\t" + "CodecID: " + getCodecID() + "\n";
    if (getCodecPrivate() != null)
    {
      s += "\t\t" + "CodecPrivate: " + getCodecPrivate().remaining() + " byte(s)" + "\n";
    }

    if (getTrackType() == TrackType.VIDEO)
    {
      s += "\t\t" + "PixelWidth: " + video.getPixelWidth() + "\n";
      s += "\t\t" + "PixelHeight: " + video.getPixelHeight() + "\n";
      s += "\t\t" + "DisplayWidth: " + video.getDisplayWidth() + "\n";
      s += "\t\t" + "DisplayHeight: " + video.getDisplayHeight() + "\n";
    }

    if (getTrackType() == TrackType.AUDIO)
    {
      s += "\t\t" + "SamplingFrequency: " + audio.getSamplingFrequency() + "\n";
      if (audio.getOutputSamplingFrequency() != 0)
      {
        s += "\t\t" + "OutputSamplingFrequency: " + audio.getOutputSamplingFrequency() + "\n";
      }
      s += "\t\t" + "Channels: " + audio.getChannels() + "\n";
      if (audio.getBitDepth() != 0)
      {
        s += "\t\t" + "BitDepth: " + audio.getBitDepth() + "\n";
      }
    }

    return s;
  }

  public static Track fromElement(final Element level2, final DataSource ioDS, final Reader reader)
  {
    // TODO: read all possible elements
    Element level3 = ((MasterElement) level2).readNextChild(reader);
    Element level4 = null;
    final Track track = new Track();
    LOG.debug("Reading track from doc!");
    while (level3 != null)
    {
      if (level3.isType(DocTypes.TrackNumber.getType()))
      {
        level3.readData(ioDS);
        track.setTrackNo((int) ((UnsignedIntegerElement) level3).getValue());
      }
      else if (level3.isType(DocTypes.TrackUID.getType()))
      {
        level3.readData(ioDS);
        track.setTrackUID(((UnsignedIntegerElement) level3).getValue());

      }
      else if (level3.isType(DocTypes.TrackType.getType()))
      {
        level3.readData(ioDS);
        track.setTrackType(TrackType.fromOrdinal(((UnsignedIntegerElement) level3).getValue()));
      }
      else if (level3.isType(DocTypes.DefaultDuration.getType()))
      {
        level3.readData(ioDS);
        track.setDefaultDuration(((UnsignedIntegerElement) level3).getValue());

      }
      else if (level3.isType(DocTypes.Name.getType()))
      {
        level3.readData(ioDS);
        track.setName(((StringElement) level3).getValue());

      }
      else if (level3.isType(DocTypes.Language.getType()))
      {
        level3.readData(ioDS);
        track.setLanguage(((StringElement) level3).getValue());

      }
      else if (level3.isType(DocTypes.CodecID.getType()))
      {
        level3.readData(ioDS);
        track.setCodecID(((StringElement) level3).getValue());

      }
      else if (level3.isType(DocTypes.CodecPrivate.getType()))
      {
        level3.readData(ioDS);
        track.setCodecPrivate(((BinaryElement) level3).getData());

      }
      else if (level3.isType(DocTypes.Video.getType()))
      {
        level4 = ((MasterElement) level3).readNextChild(reader);
        track.video = new VideoTrack();
        while (level4 != null)
        {
          if (level4.isType(DocTypes.PixelWidth.getType()))
          {
            level4.readData(ioDS);
            track.video.setPixelWidth((short) ((UnsignedIntegerElement) level4).getValue());

          }
          else if (level4.isType(DocTypes.PixelHeight.getType()))
          {
            level4.readData(ioDS);
            track.video.setPixelHeight((short) ((UnsignedIntegerElement) level4).getValue());

          }
          else if (level4.isType(DocTypes.DisplayWidth.getType()))
          {
            level4.readData(ioDS);
            track.video.setDisplayWidth((short) ((UnsignedIntegerElement) level4).getValue());

          }
          else if (level4.isType(DocTypes.DisplayHeight.getType()))
          {
            level4.readData(ioDS);
            track.video.setDisplayHeight((short) ((UnsignedIntegerElement) level4).getValue());
          }

          level4.skipData(ioDS);
          level4 = ((MasterElement) level3).readNextChild(reader);
        }

      }
      else if (level3.isType(DocTypes.Audio.getType()))
      {
        level4 = ((MasterElement) level3).readNextChild(reader);
        track.audio = new AudioTrack();
        while (level4 != null)
        {
          if (level4.isType(DocTypes.SamplingFrequency.getType()))
          {
            level4.readData(ioDS);
            track.audio.setSamplingFrequency((float) ((FloatElement) level4).getValue());

          }
          else if (level4.isType(DocTypes.OutputSamplingFrequency.getType()))
          {
            level4.readData(ioDS);
            track.audio.setOutputSamplingFrequency((float) ((FloatElement) level4).getValue());

          }
          else if (level4.isType(DocTypes.Channels.getType()))
          {
            level4.readData(ioDS);
            track.audio.setChannels((short) ((UnsignedIntegerElement) level4).getValue());

          }
          else if (level4.isType(DocTypes.BitDepth.getType()))
          {
            level4.readData(ioDS);
            track.audio.setBitDepth((byte) ((UnsignedIntegerElement) level4).getValue());
          }

          level4.skipData(ioDS);
          level4 = ((MasterElement) level3).readNextChild(reader);
        }

      }
      level3.skipData(ioDS);
      level3 = ((MasterElement) level2).readNextChild(reader);
    }
    LOG.debug("Read track from doc!");
    return track;
  }

  Element toElement()
  {
    final MasterElement trackEntryElem = DocTypes.TrackEntry.getInstance();

    final UnsignedIntegerElement trackNoElem = DocTypes.TrackNumber.getInstance();
    trackNoElem.setValue(this.getTrackNo());

    final UnsignedIntegerElement trackUIDElem = DocTypes.TrackUID.getInstance();
    trackUIDElem.setValue(this.getTrackUID());

    final UnsignedIntegerElement trackTypeElem = DocTypes.TrackType.getInstance();
    trackTypeElem.setValue(this.getTrackType().type);
    LOG.info("Track type set to {}", getTrackType().type);

    final UnsignedIntegerElement trackFlagEnabledElem = DocTypes.FlagEnabled.getInstance();
    trackFlagEnabledElem.setValue(this.isFlagEnabled() ? 1 : 0);

    final UnsignedIntegerElement trackFlagDefaultElem = DocTypes.FlagDefault.getInstance();
    trackFlagDefaultElem.setValue(this.isFlagDefault() ? 1 : 0);

    final UnsignedIntegerElement trackFlagForcedElem = DocTypes.FlagForced.getInstance();
    trackFlagForcedElem.setValue(this.isFlagForced() ? 1 : 0);

    final UnsignedIntegerElement trackFlagLacingElem = DocTypes.FlagLacing.getInstance();
    trackFlagLacingElem.setValue(this.isFlagLacing() ? 1 : 0);

    final UnsignedIntegerElement trackMinCacheElem = DocTypes.MinCache.getInstance();
    trackMinCacheElem.setValue(this.getMinCache());

    final UnsignedIntegerElement trackMaxBlockAddIdElem = DocTypes.MaxBlockAdditionID.getInstance();
    trackMaxBlockAddIdElem.setValue(this.getMaxBlockAdditionalId());

    final StringElement trackNameElem = DocTypes.Name.getInstance();
    trackNameElem.setValue(this.getName());

    final StringElement trackLangElem = DocTypes.Language.getInstance();
    trackLangElem.setValue(this.getLanguage());

    final StringElement trackCodecIDElem = DocTypes.CodecID.getInstance();
    trackCodecIDElem.setValue(this.getCodecID());

    trackEntryElem.addChildElement(trackNoElem);
    trackEntryElem.addChildElement(trackUIDElem);
    trackEntryElem.addChildElement(trackTypeElem);

    trackEntryElem.addChildElement(trackFlagEnabledElem);
    trackEntryElem.addChildElement(trackFlagDefaultElem);
    trackEntryElem.addChildElement(trackFlagForcedElem);
    trackEntryElem.addChildElement(trackFlagLacingElem);
    trackEntryElem.addChildElement(trackMinCacheElem);
    trackEntryElem.addChildElement(trackMaxBlockAddIdElem);

    trackEntryElem.addChildElement(trackNameElem);
    trackEntryElem.addChildElement(trackLangElem);
    trackEntryElem.addChildElement(trackCodecIDElem);

    if (codecPrivate != null && codecPrivate.hasRemaining())
    {
      final BinaryElement trackCodecPrivateElem = DocTypes.CodecPrivate.getInstance();
      trackCodecPrivateElem.setData(this.getCodecPrivate());
      trackEntryElem.addChildElement(trackCodecPrivateElem);

    }

    final UnsignedIntegerElement trackDefaultDurationElem = DocTypes.DefaultDuration.getInstance();
    trackDefaultDurationElem.setValue(this.getDefaultDuration());

    final UnsignedIntegerElement trackCodecDecodeAllElem = DocTypes.CodecDecodeAll.getInstance();
    trackCodecDecodeAllElem.setValue(this.codecDecodeAll ? 1 : 0);

    trackEntryElem.addChildElement(trackDefaultDurationElem);
    trackEntryElem.addChildElement(trackCodecDecodeAllElem);

    if (!overlayUids.isEmpty())
    {
      for (final Long overlay: overlayUids)
      {
        final UnsignedIntegerElement trackOverlayElem = DocTypes.TrackOverlay.getInstance();
        trackOverlayElem.setValue(overlay);
        trackEntryElem.addChildElement(trackOverlayElem);
      }
    }

    // Now we add the audio/video dependant sub-elements
    if (this.getTrackType() == TrackType.VIDEO)
    {
      final MasterElement trackVideoElem = DocTypes.Video.getInstance();

      final UnsignedIntegerElement trackVideoPixelWidthElem = DocTypes.PixelWidth.getInstance();
      trackVideoPixelWidthElem.setValue(this.video.getPixelWidth());

      final UnsignedIntegerElement trackVideoPixelHeightElem = DocTypes.PixelHeight.getInstance();
      trackVideoPixelHeightElem.setValue(this.video.getPixelHeight());

      final UnsignedIntegerElement trackVideoDisplayWidthElem = DocTypes.DisplayWidth.getInstance();
      trackVideoDisplayWidthElem.setValue(this.video.getDisplayWidth());

      final UnsignedIntegerElement trackVideoDisplayHeightElem = DocTypes.DisplayHeight.getInstance();
      trackVideoDisplayHeightElem.setValue(this.video.getDisplayHeight());

      trackVideoElem.addChildElement(trackVideoPixelWidthElem);
      trackVideoElem.addChildElement(trackVideoPixelHeightElem);
      trackVideoElem.addChildElement(trackVideoDisplayWidthElem);
      trackVideoElem.addChildElement(trackVideoDisplayHeightElem);

      trackEntryElem.addChildElement(trackVideoElem);
    }
    else if (this.getTrackType() == TrackType.AUDIO)
    {
      final MasterElement trackAudioElem = DocTypes.Audio.getInstance();

      final UnsignedIntegerElement trackAudioChannelsElem = DocTypes.Channels.getInstance();
      trackAudioChannelsElem.setValue(this.audio.getChannels());

      final UnsignedIntegerElement trackAudioBitDepthElem = DocTypes.BitDepth.getInstance();
      trackAudioBitDepthElem.setValue(this.audio.getBitDepth());

      final FloatElement trackAudioSamplingRateElem = DocTypes.SamplingFrequency.getInstance();
      trackAudioSamplingRateElem.setValue(this.audio.getSamplingFrequency());

      final FloatElement trackAudioOutputSamplingFrequencyElem = DocTypes.OutputSamplingFrequency.getInstance();
      trackAudioOutputSamplingFrequencyElem.setValue(this.audio.getOutputSamplingFrequency());

      trackAudioElem.addChildElement(trackAudioChannelsElem);
      trackAudioElem.addChildElement(trackAudioBitDepthElem);
      trackAudioElem.addChildElement(trackAudioSamplingRateElem);
      trackAudioElem.addChildElement(trackAudioOutputSamplingFrequencyElem);

      trackEntryElem.addChildElement(trackAudioElem);
    }
    if (operation != null)
    {
      final MasterElement trackOpElem = DocTypes.TrackOperation.getInstance();
      final MasterElement trackJoinElem = DocTypes.TrackJoinBlocks.getInstance();
      for (final Long uid: operation.joinUIDs)
      {
        final UnsignedIntegerElement joinUidElem = DocTypes.TrackJoinUID.getInstance();
        joinUidElem.setValue(uid);
        trackJoinElem.addChildElement(joinUidElem);
      }
      trackOpElem.addChildElement(trackJoinElem);
      trackEntryElem.addChildElement(trackOpElem);
    }
    return trackEntryElem;
  }

  public int getTrackNo()
  {
    return trackNo;
  }

  public void setTrackNo(final int trackNo)
  {
    this.trackNo = trackNo;
  }

  public long getTrackUID()
  {
    return trackUID;
  }

  public void setTrackUID(final long trackUID)
  {
    this.trackUID = trackUID;
  }

  public TrackType getTrackType()
  {
    return trackType;
  }

  public void setTrackType(final TrackType trackType)
  {
    this.trackType = trackType;
  }

  public boolean isFlagEnabled()
  {
    return flagEnabled;
  }

  public void setFlagEnabled(final boolean flagEnabled)
  {
    this.flagEnabled = flagEnabled;
  }

  public boolean isFlagDefault()
  {
    return flagDefault;
  }

  public void setFlagDefault(final boolean flagDefault)
  {
    this.flagDefault = flagDefault;
  }

  public boolean isFlagForced()
  {
    return flagForced;
  }

  public void setFlagForced(final boolean flagForced)
  {
    this.flagForced = flagForced;
  }

  public boolean isFlagLacing()
  {
    return flagLacing;
  }

  public void setFlagLacing(final boolean flagLacing)
  {
    this.flagLacing = flagLacing;
  }

  public int getMinCache()
  {
    return minCache;
  }

  public void setMinCache(final int minCache)
  {
    this.minCache = minCache;
  }

  public int getMaxBlockAdditionalId()
  {
    return maxBlockAdditionalId;
  }

  public void setMaxBlockAdditionalId(final int maxBlockAdditionalId)
  {
    this.maxBlockAdditionalId = maxBlockAdditionalId;
  }

  public String getName()
  {
    return name;
  }

  public void setName(final String name)
  {
    this.name = name;
  }

  public String getLanguage()
  {
    return language;
  }

  public void setLanguage(final String language)
  {
    this.language = language;
  }

  public String getCodecID()
  {
    return codecID;
  }

  public void setCodecID(final String codecID)
  {
    this.codecID = codecID;
  }

  public ByteBuffer getCodecPrivate()
  {
    return codecPrivate;
  }

  public void setCodecPrivate(final ByteBuffer codecPrivate)
  {
    this.codecPrivate = codecPrivate;
  }

  public long getDefaultDuration()
  {
    return defaultDuration;
  }

  public void setDefaultDuration(final long defaultDuration)
  {
    this.defaultDuration = defaultDuration;
  }

  public boolean isCodecDecodeAll()
  {
    return codecDecodeAll;
  }

  public void setCodecDecodeAll(final boolean codecDecodeAll)
  {
    this.codecDecodeAll = codecDecodeAll;
  }

  public int getSeekPreroll()
  {
    return seekPreroll;
  }

  public void setSeekPreroll(final int seekPreroll)
  {
    this.seekPreroll = seekPreroll;
  }

  public VideoTrack getVideo()
  {
    return video;
  }

  public void setVideo(final VideoTrack video)
  {
    this.video = video;
  }

  public AudioTrack getAudio()
  {
    return audio;
  }

  public void setAudio(final AudioTrack audio)
  {
    this.audio = audio;
  }

  public TrackOperation getOperation()
  {
    return operation;
  }

  public void setOperation(final TrackOperation operation)
  {
    this.operation = operation;
  }

  public void addTrackOverlay(final long overlay)
  {
    overlayUids.add(overlay);
  }
}
