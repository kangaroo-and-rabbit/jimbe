package org.atriasoft.jimbe.matroska.model;

public enum LaceMode {
	NONE(0), XIPH(1), EBML(3), FIXED(2);

	private final int representation;

	private LaceMode(final int representation) {
		this.representation = representation;
	}

	public int getRepresentation() {
		return this.representation;
	}

	public static LaceMode fromRepresentation(final int representation) {
		for (final LaceMode mode : values()) {
			if (mode.representation == representation) {
				return mode;
			}
		}
		return null;
	}
}
