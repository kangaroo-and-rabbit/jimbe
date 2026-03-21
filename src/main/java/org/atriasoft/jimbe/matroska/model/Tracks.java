package org.atriasoft.jimbe.matroska.model;

import java.util.ArrayList;

import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tracks {
	private static final int BLOCK_SIZE = 4096;
	private static final Logger LOG = LoggerFactory.getLogger(Tracks.class);

	private final ArrayList<Track> tracks = new ArrayList<>();

	private final long myPosition;

	public Tracks(final long position) {
		this.myPosition = position;
	}

	public void addTrack(final Track track) {
		this.tracks.add(track);
	}

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

	public void update(final DataWriter ioDW) {
		LOG.info("Updating tracks list!");
		final long start = ioDW.getFilePointer();
		ioDW.seek(this.myPosition);
		writeTracks(ioDW);
		ioDW.seek(start);
	}
}
