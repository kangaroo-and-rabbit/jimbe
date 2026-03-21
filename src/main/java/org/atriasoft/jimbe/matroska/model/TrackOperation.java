package org.atriasoft.jimbe.matroska.model;

import java.util.ArrayList;

/**
 * Represents a Matroska TrackOperation element, used to combine multiple
 * virtual track parts into a single logical track via join operations.
 *
 * @see Track
 */
public class TrackOperation {
	final ArrayList<Long> joinUIDs = new ArrayList<>();

	/**
	 * Adds a virtual track part to this operation by its UID.
	 * The joined tracks are played sequentially as a single logical track.
	 *
	 * @param uid the UID of the track to join
	 */
	public void addVirtualTrackPart(final long uid) {
		this.joinUIDs.add(uid);
	}
}
