package org.atriasoft.jimbe.matroska.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumerates the Matroska track types as defined in the Matroska specification.
 * Each type corresponds to a specific kind of media content (video, audio, subtitles, etc.).
 */
public enum TrackType {
	VIDEO(1), AUDIO(2), COMPLEX(3), LOGO(0x10), SUBTITLE(0x11), BUTTONS(0x12), CONTROL(0x20);

	private static final Logger LOG = LoggerFactory.getLogger(TrackType.class);

	final byte type;

	private TrackType(final int type) {
		this.type = (byte) type;
	}

	/**
	 * Returns the {@link TrackType} corresponding to the given Matroska type value.
	 *
	 * @param l the numeric track type value from the Matroska element
	 * @return the matching track type, or {@code null} if no match is found
	 */
	public static TrackType fromOrdinal(final long l) {
		LOG.debug("Track type from ordinal: {}", l);
		return switch ((int) l) {
			case 1 -> VIDEO;
			case 2 -> AUDIO;
			case 3 -> COMPLEX;
			case 0x10 -> LOGO;
			case 0x11 -> SUBTITLE;
			case 0x12 -> BUTTONS;
			case 0x20 -> CONTROL;
			default -> null;
		};
	}
}
