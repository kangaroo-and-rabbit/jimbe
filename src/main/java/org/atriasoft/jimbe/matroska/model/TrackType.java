package org.atriasoft.jimbe.matroska.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum TrackType {
	VIDEO(1), AUDIO(2), COMPLEX(3), LOGO(0x10), SUBTITLE(0x11), BUTTONS(0x12), CONTROL(0x20);

	private static final Logger LOG = LoggerFactory.getLogger(TrackType.class);

	final byte type;

	private TrackType(final int type) {
		this.type = (byte) type;
	}

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
