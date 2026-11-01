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
 * in place after initial writing (e.g., to add tracks during muxing). Writing it as late
 * as possible, once the tracks are known, is what makes that space sufficient.
 * </p>
 *
 * @see Track
 */
public class Tracks {
	private static final int BLOCK_SIZE = 4096;

	/** What is kept free beyond the tracks when they fill the usual block on their own. */
	private static final int SPARE_BYTES = 1024;

	/** The smallest gap a Void element can fill. Tracks must leave at least that or nothing would mark the gap. */
	private static final int MIN_PADDING = 2;

	private static final Logger LOG = LoggerFactory.getLogger(Tracks.class);

	private final ArrayList<Track> tracks = new ArrayList<>();

	private final long myPosition;

	/** The room taken in the file, chosen on the first write and fixed afterwards. 0 until then. */
	private long reservedSize = 0;

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
	 * @throws IllegalStateException if the element was already written and the track no longer fits in the
	 *         room it took: what comes next in the file can not move
	 */
	public void addTrack(final Track track) {
		this.tracks.add(track);
		if (this.reservedSize != 0 && toElement().getTotalSize() + MIN_PADDING > this.reservedSize) {
			this.tracks.remove(this.tracks.size() - 1);
			throw new IllegalStateException("Track " + track.getTrackNo() + " does not fit in the " + this.reservedSize
					+ " bytes reserved for the tracks: add it before the first cluster is written");
		}
	}

	private MasterElement toElement() {
		final MasterElement tracksElem = DocTypes.Tracks.getInstance();
		for (final Track track : this.tracks) {
			tracksElem.addChildElement(track.toElement());
		}
		return tracksElem;
	}

	/**
	 * Writes all tracks as a Matroska Tracks element, followed by void padding to fill the reserved room.
	 * The first write sets that room: {@value #BLOCK_SIZE} bytes, more when the tracks need it.
	 *
	 * @param ioDW the data writer to write to
	 * @return the total number of bytes written (including padding)
	 * @throws IllegalStateException if the tracks have outgrown the room reserved by the first write
	 */
	public long writeTracks(final DataWriter ioDW) {
		final MasterElement tracksElem = toElement();
		final long size = tracksElem.getTotalSize();
		if (this.reservedSize == 0) {
			this.reservedSize = Math.max(BLOCK_SIZE, size + SPARE_BYTES);
		}
		if (size + MIN_PADDING > this.reservedSize) {
			throw new IllegalStateException(
					"The tracks take " + size + " bytes, more than the " + this.reservedSize + " reserved");
		}
		tracksElem.writeElement(ioDW);
		new VoidElement(this.reservedSize - size).writeElement(ioDW);
		return this.reservedSize;
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
