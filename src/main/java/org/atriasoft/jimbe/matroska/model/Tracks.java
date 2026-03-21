package org.atriasoft.jimbe.matroska.model;

import java.util.ArrayList;

import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for Matroska track entries, responsible for writing and updating the
 * Tracks element in a Matroska file.
 * <p>
 * The tracks element is written with reserved padding space so it can be updated
 * in place after initial writing (e.g., to add tracks during muxing).
 * </p>
 *
 * @see Track
 */
public class Tracks {
	private static final int BLOCK_SIZE = 4096;
	private static final Logger LOG = LoggerFactory.getLogger(Tracks.class);

	private final ArrayList<Track> tracks = new ArrayList<>();

	private final long myPosition;

	/**
	 * Constructs a Tracks container at the given file position.
	 *
	 * @param position the byte position in the output where the tracks element will be written
	 */
	public Tracks(final long position) {
		this.myPosition = position;
	}

	/**
	 * Adds a track to this container.
	 *
	 * @param track the track to add
	 */
	public void addTrack(final Track track) {
		this.tracks.add(track);
	}

	/**
	 * Writes all tracks as a Matroska Tracks element, followed by void padding
	 * to fill the reserved block size.
	 *
	 * @param ioDW the data writer to write to
	 * @return the total number of bytes written (including padding)
	 */
	public long writeTracks(final DataWriter ioDW) {
		final MasterElement tracksElem = DocTypes.Tracks.getInstance();

		for (final Track track : this.tracks) {
			tracksElem.addChildElement(track.toElement());
		}
		tracksElem.writeElement(ioDW);
		assert BLOCK_SIZE > tracksElem.getTotalSize();
		new VoidElement(BLOCK_SIZE - tracksElem.getTotalSize()).writeElement(ioDW);
		return BLOCK_SIZE;
	}

	/**
	 * Updates the tracks element in place by seeking back to the original position
	 * and rewriting it.
	 *
	 * @param ioDW the data writer containing this tracks element
	 */
	public void update(final DataWriter ioDW) {
		LOG.info("Updating tracks list!");
		final long start = ioDW.getFilePointer();
		ioDW.seek(this.myPosition);
		writeTracks(ioDW);
		ioDW.seek(start);
	}
}
