package org.atriasoft.jimbe.matroska.model;

/**
 * Enumerates the lacing modes available for packing multiple frames into a single Matroska block.
 * <p>
 * Lacing allows storing multiple frames in a single block to reduce overhead.
 * The mode determines how the individual frame sizes are encoded:
 * </p>
 * <ul>
 *   <li>{@link #NONE} - No lacing; a single frame per block</li>
 *   <li>{@link #XIPH} - Xiph-style lacing with variable-length size encoding</li>
 *   <li>{@link #EBML} - EBML-style lacing with EBML-coded sizes</li>
 *   <li>{@link #FIXED} - Fixed-size lacing where all frames have equal size</li>
 * </ul>
 */
public enum LaceMode {
	NONE(0), XIPH(1), EBML(3), FIXED(2);

	private final int representation;

	private LaceMode(final int representation) {
		this.representation = representation;
	}

	/** Returns the numeric representation of this lace mode as used in the Matroska block header. */
	public int getRepresentation() {
		return this.representation;
	}

	/**
	 * Returns the {@link LaceMode} corresponding to the given numeric representation.
	 *
	 * @param representation the numeric lace mode value from a block header
	 * @return the matching lace mode, or {@code null} if no match is found
	 */
	public static LaceMode fromRepresentation(final int representation) {
		for (final LaceMode mode : values()) {
			if (mode.representation == representation) {
				return mode;
			}
		}
		return null;
	}
}
