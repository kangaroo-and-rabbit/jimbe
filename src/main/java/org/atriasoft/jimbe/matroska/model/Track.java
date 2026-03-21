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

import org.atriasoft.jimbe.ebml.Reader;
import org.atriasoft.jimbe.ebml.model.BinaryElement;
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
 * <p>Mandatory fields are:</p>
 * <ul>
 *    <li>Track Type</li>
 *    <li>Codec ID</li>
 *    <li>Default duration</li>
 * </ul>
 * <p>Note that if the TrackType is Audio, you must add the AudioTrack member. Likewise VideoTrack for Video.</p>
 */
public class Track {
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
	public String toString() {
		StringBuilder s = new StringBuilder().append(new String());

		s.append("\t\t").append("TrackNo: ").append(getTrackNo()).append("\n");
		s.append("\t\t").append("TrackUID: ").append(getTrackUID()).append("\n");
		s.append("\t\t").append("TrackType: ").append(getTrackType().name()).append("\n");
		s.append("\t\t").append("DefaultDuration: ").append(getDefaultDuration()).append("\n");
		s.append("\t\t").append("Name: ").append(getName()).append("\n");
		s.append("\t\t").append("Language: ").append(getLanguage()).append("\n");
		s.append("\t\t").append("CodecID: ").append(getCodecID()).append("\n");
		if (getCodecPrivate() != null) {
			s.append("\t\t").append("CodecPrivate: ").append(getCodecPrivate().remaining()).append(" byte(s)")
					.append("\n");
		}

		if (getTrackType() == TrackType.VIDEO) {
			s.append("\t\t").append("PixelWidth: ").append(this.video.getPixelWidth()).append("\n");
			s.append("\t\t").append("PixelHeight: ").append(this.video.getPixelHeight()).append("\n");
			s.append("\t\t").append("DisplayWidth: ").append(this.video.getDisplayWidth()).append("\n");
			s.append("\t\t").append("DisplayHeight: ").append(this.video.getDisplayHeight()).append("\n");
		}

		if (getTrackType() == TrackType.AUDIO) {
			s.append("\t\t").append("SamplingFrequency: ").append(this.audio.getSamplingFrequency()).append("\n");
			if (this.audio.getOutputSamplingFrequency() != 0) {
				s.append("\t\t").append("OutputSamplingFrequency: ").append(this.audio.getOutputSamplingFrequency())
						.append("\n");
			}
			s.append("\t\t").append("Channels: ").append(this.audio.getChannels()).append("\n");
			if (this.audio.getBitDepth() != 0) {
				s.append("\t\t").append("BitDepth: ").append(this.audio.getBitDepth()).append("\n");
			}
		}

		return s.toString();
	}

	/**
	 * Parses a Track from an EBML TrackEntry element, reading all sub-elements
	 * including video and audio properties.
	 *
	 * @param level2 the TrackEntry master element
	 * @param ioDS the data source to read element data from
	 * @param reader the EBML reader for parsing child elements
	 * @return the parsed Track instance
	 */
	public static Track fromElement(final Element level2, final DataSource ioDS, final Reader reader) {
		// TODO: read all possible elements
		Element level3 = ((MasterElement) level2).readNextChild(reader);
		Element level4 = null;
		final Track track = new Track();
		LOG.debug("Reading track from doc!");
		while (level3 != null) {
			if (level3.isType(DocTypes.TrackNumber.getType())) {
				level3.readData(ioDS);
				track.setTrackNo((int) ((UnsignedIntegerElement) level3).getValue());
			} else if (level3.isType(DocTypes.TrackUID.getType())) {
				level3.readData(ioDS);
				track.setTrackUID(((UnsignedIntegerElement) level3).getValue());

			} else if (level3.isType(DocTypes.TrackType.getType())) {
				level3.readData(ioDS);
				track.setTrackType(TrackType.fromOrdinal(((UnsignedIntegerElement) level3).getValue()));
			} else if (level3.isType(DocTypes.DefaultDuration.getType())) {
				level3.readData(ioDS);
				track.setDefaultDuration(((UnsignedIntegerElement) level3).getValue());

			} else if (level3.isType(DocTypes.Name.getType())) {
				level3.readData(ioDS);
				track.setName(((StringElement) level3).getValue());

			} else if (level3.isType(DocTypes.Language.getType())) {
				level3.readData(ioDS);
				track.setLanguage(((StringElement) level3).getValue());

			} else if (level3.isType(DocTypes.CodecID.getType())) {
				level3.readData(ioDS);
				track.setCodecID(((StringElement) level3).getValue());

			} else if (level3.isType(DocTypes.CodecPrivate.getType())) {
				level3.readData(ioDS);
				track.setCodecPrivate(level3.getData());

			} else if (level3.isType(DocTypes.Video.getType())) {
				level4 = ((MasterElement) level3).readNextChild(reader);
				track.video = new VideoTrack();
				while (level4 != null) {
					if (level4.isType(DocTypes.PixelWidth.getType())) {
						level4.readData(ioDS);
						track.video.setPixelWidth((short) ((UnsignedIntegerElement) level4).getValue());

					} else if (level4.isType(DocTypes.PixelHeight.getType())) {
						level4.readData(ioDS);
						track.video.setPixelHeight((short) ((UnsignedIntegerElement) level4).getValue());

					} else if (level4.isType(DocTypes.DisplayWidth.getType())) {
						level4.readData(ioDS);
						track.video.setDisplayWidth((short) ((UnsignedIntegerElement) level4).getValue());

					} else if (level4.isType(DocTypes.DisplayHeight.getType())) {
						level4.readData(ioDS);
						track.video.setDisplayHeight((short) ((UnsignedIntegerElement) level4).getValue());
					}

					level4.skipData(ioDS);
					level4 = ((MasterElement) level3).readNextChild(reader);
				}

			} else if (level3.isType(DocTypes.Audio.getType())) {
				level4 = ((MasterElement) level3).readNextChild(reader);
				track.audio = new AudioTrack();
				while (level4 != null) {
					if (level4.isType(DocTypes.SamplingFrequency.getType())) {
						level4.readData(ioDS);
						track.audio.setSamplingFrequency((float) ((FloatElement) level4).getValue());

					} else if (level4.isType(DocTypes.OutputSamplingFrequency.getType())) {
						level4.readData(ioDS);
						track.audio.setOutputSamplingFrequency((float) ((FloatElement) level4).getValue());

					} else if (level4.isType(DocTypes.Channels.getType())) {
						level4.readData(ioDS);
						track.audio.setChannels((short) ((UnsignedIntegerElement) level4).getValue());

					} else if (level4.isType(DocTypes.BitDepth.getType())) {
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

	Element toElement() {
		final MasterElement trackEntryElem = DocTypes.TrackEntry.getInstance();

		final UnsignedIntegerElement trackNoElem = DocTypes.TrackNumber.getInstance();
		trackNoElem.setValue(getTrackNo());

		final UnsignedIntegerElement trackUIDElem = DocTypes.TrackUID.getInstance();
		trackUIDElem.setValue(getTrackUID());

		final UnsignedIntegerElement trackTypeElem = DocTypes.TrackType.getInstance();
		trackTypeElem.setValue(getTrackType().type);
		LOG.info("Track type set to {}", getTrackType().type);

		final UnsignedIntegerElement trackFlagEnabledElem = DocTypes.FlagEnabled.getInstance();
		trackFlagEnabledElem.setValue(isFlagEnabled() ? 1 : 0);

		final UnsignedIntegerElement trackFlagDefaultElem = DocTypes.FlagDefault.getInstance();
		trackFlagDefaultElem.setValue(isFlagDefault() ? 1 : 0);

		final UnsignedIntegerElement trackFlagForcedElem = DocTypes.FlagForced.getInstance();
		trackFlagForcedElem.setValue(isFlagForced() ? 1 : 0);

		final UnsignedIntegerElement trackFlagLacingElem = DocTypes.FlagLacing.getInstance();
		trackFlagLacingElem.setValue(isFlagLacing() ? 1 : 0);

		final UnsignedIntegerElement trackMinCacheElem = DocTypes.MinCache.getInstance();
		trackMinCacheElem.setValue(getMinCache());

		final UnsignedIntegerElement trackMaxBlockAddIdElem = DocTypes.MaxBlockAdditionID.getInstance();
		trackMaxBlockAddIdElem.setValue(getMaxBlockAdditionalId());

		final StringElement trackNameElem = DocTypes.Name.getInstance();
		trackNameElem.setValue(getName());

		final StringElement trackLangElem = DocTypes.Language.getInstance();
		trackLangElem.setValue(getLanguage());

		final StringElement trackCodecIDElem = DocTypes.CodecID.getInstance();
		trackCodecIDElem.setValue(getCodecID());

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

		if (this.codecPrivate != null && this.codecPrivate.hasRemaining()) {
			final BinaryElement trackCodecPrivateElem = DocTypes.CodecPrivate.getInstance();
			trackCodecPrivateElem.setData(getCodecPrivate());
			trackEntryElem.addChildElement(trackCodecPrivateElem);

		}

		final UnsignedIntegerElement trackDefaultDurationElem = DocTypes.DefaultDuration.getInstance();
		trackDefaultDurationElem.setValue(getDefaultDuration());

		final UnsignedIntegerElement trackCodecDecodeAllElem = DocTypes.CodecDecodeAll.getInstance();
		trackCodecDecodeAllElem.setValue(this.codecDecodeAll ? 1 : 0);

		trackEntryElem.addChildElement(trackDefaultDurationElem);
		trackEntryElem.addChildElement(trackCodecDecodeAllElem);

		if (!this.overlayUids.isEmpty()) {
			for (final Long overlay : this.overlayUids) {
				final UnsignedIntegerElement trackOverlayElem = DocTypes.TrackOverlay.getInstance();
				trackOverlayElem.setValue(overlay);
				trackEntryElem.addChildElement(trackOverlayElem);
			}
		}

		// Now we add the audio/video dependant sub-elements
		if (getTrackType() == TrackType.VIDEO) {
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
		} else if (getTrackType() == TrackType.AUDIO) {
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
		if (this.operation != null) {
			final MasterElement trackOpElem = DocTypes.TrackOperation.getInstance();
			final MasterElement trackJoinElem = DocTypes.TrackJoinBlocks.getInstance();
			for (final Long uid : this.operation.joinUIDs) {
				final UnsignedIntegerElement joinUidElem = DocTypes.TrackJoinUID.getInstance();
				joinUidElem.setValue(uid);
				trackJoinElem.addChildElement(joinUidElem);
			}
			trackOpElem.addChildElement(trackJoinElem);
			trackEntryElem.addChildElement(trackOpElem);
		}
		return trackEntryElem;
	}

	/** Returns the track number. */
	public int getTrackNo() {
		return this.trackNo;
	}

	/** Sets the track number. */
	public void setTrackNo(final int trackNo) {
		this.trackNo = trackNo;
	}

	/** Returns the unique track identifier. */
	public long getTrackUID() {
		return this.trackUID;
	}

	/** Sets the unique track identifier. */
	public void setTrackUID(final long trackUID) {
		this.trackUID = trackUID;
	}

	/** Returns the type of this track (video, audio, subtitle, etc.). */
	public TrackType getTrackType() {
		return this.trackType;
	}

	/** Sets the type of this track. */
	public void setTrackType(final TrackType trackType) {
		this.trackType = trackType;
	}

	/** Returns {@code true} if this track is enabled. */
	public boolean isFlagEnabled() {
		return this.flagEnabled;
	}

	/** Sets whether this track is enabled. */
	public void setFlagEnabled(final boolean flagEnabled) {
		this.flagEnabled = flagEnabled;
	}

	/** Returns {@code true} if this track is the default track for its type. */
	public boolean isFlagDefault() {
		return this.flagDefault;
	}

	/** Sets whether this track is the default track for its type. */
	public void setFlagDefault(final boolean flagDefault) {
		this.flagDefault = flagDefault;
	}

	/** Returns {@code true} if this track is forced (must be displayed). */
	public boolean isFlagForced() {
		return this.flagForced;
	}

	/** Sets whether this track is forced. */
	public void setFlagForced(final boolean flagForced) {
		this.flagForced = flagForced;
	}

	/** Returns {@code true} if lacing is enabled for this track. */
	public boolean isFlagLacing() {
		return this.flagLacing;
	}

	/** Sets whether lacing is enabled for this track. */
	public void setFlagLacing(final boolean flagLacing) {
		this.flagLacing = flagLacing;
	}

	/** Returns the minimum cache size hint for this track. */
	public int getMinCache() {
		return this.minCache;
	}

	/** Sets the minimum cache size hint for this track. */
	public void setMinCache(final int minCache) {
		this.minCache = minCache;
	}

	/** Returns the maximum BlockAdditional ID used in this track. */
	public int getMaxBlockAdditionalId() {
		return this.maxBlockAdditionalId;
	}

	/** Sets the maximum BlockAdditional ID used in this track. */
	public void setMaxBlockAdditionalId(final int maxBlockAdditionalId) {
		this.maxBlockAdditionalId = maxBlockAdditionalId;
	}

	/** Returns the human-readable name of this track. */
	public String getName() {
		return this.name;
	}

	/** Sets the human-readable name of this track. */
	public void setName(final String name) {
		this.name = name;
	}

	/** Returns the language code of this track (e.g., "eng"). */
	public String getLanguage() {
		return this.language;
	}

	/** Sets the language code of this track. */
	public void setLanguage(final String language) {
		this.language = language;
	}

	/** Returns the codec identifier string (e.g., "V_VP9", "A_OPUS"). */
	public String getCodecID() {
		return this.codecID;
	}

	/** Sets the codec identifier string. */
	public void setCodecID(final String codecID) {
		this.codecID = codecID;
	}

	/** Returns the codec-specific private data, or {@code null} if not set. */
	public ByteBuffer getCodecPrivate() {
		return this.codecPrivate;
	}

	/** Sets the codec-specific private data. */
	public void setCodecPrivate(final ByteBuffer codecPrivate) {
		this.codecPrivate = codecPrivate;
	}

	/** Returns the default duration of each frame in nanoseconds. */
	public long getDefaultDuration() {
		return this.defaultDuration;
	}

	/** Sets the default duration of each frame in nanoseconds. */
	public void setDefaultDuration(final long defaultDuration) {
		this.defaultDuration = defaultDuration;
	}

	/** Returns {@code true} if the codec can decode all frames independently. */
	public boolean isCodecDecodeAll() {
		return this.codecDecodeAll;
	}

	/** Sets whether the codec can decode all frames independently. */
	public void setCodecDecodeAll(final boolean codecDecodeAll) {
		this.codecDecodeAll = codecDecodeAll;
	}

	/** Returns the seek pre-roll duration in nanoseconds. */
	public int getSeekPreroll() {
		return this.seekPreroll;
	}

	/** Sets the seek pre-roll duration in nanoseconds. */
	public void setSeekPreroll(final int seekPreroll) {
		this.seekPreroll = seekPreroll;
	}

	/** Returns the video track properties, or {@code null} if this is not a video track. */
	public VideoTrack getVideo() {
		return this.video;
	}

	/** Sets the video track properties. */
	public void setVideo(final VideoTrack video) {
		this.video = video;
	}

	/** Returns the audio track properties, or {@code null} if this is not an audio track. */
	public AudioTrack getAudio() {
		return this.audio;
	}

	/** Sets the audio track properties. */
	public void setAudio(final AudioTrack audio) {
		this.audio = audio;
	}

	/** Returns the track operation (combine/join), or {@code null} if not set. */
	public TrackOperation getOperation() {
		return this.operation;
	}

	/** Sets the track operation (combine/join). */
	public void setOperation(final TrackOperation operation) {
		this.operation = operation;
	}

	/**
	 * Adds a track overlay UID. Track overlays define tracks that should be combined during playback.
	 *
	 * @param overlay the UID of the overlay track
	 */
	public void addTrackOverlay(final long overlay) {
		this.overlayUids.add(overlay);
	}
}
