package org.atriasoft.jimbe.matroska;

import org.atriasoft.jimbe.matroska.model.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.io.DataSource;
import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriterTest {
	private static final Logger LOG = LoggerFactory.getLogger(WriterTest.class);
	private File destination;
	private FileDataWriter ioDW;
	private Track testTrack;
	private int timecode = 1337;

	@BeforeEach
	public void setUp() throws Exception {
		this.destination = File.createTempFile("test", ".mkv");
		this.ioDW = new FileDataWriter(this.destination.getPath());
		this.testTrack = new Track();
		this.testTrack.setTrackNo(42);
		this.testTrack.setTrackType(TrackType.SUBTITLE);
		this.testTrack.setCodecID("some subtitle codec");
		this.testTrack.setDefaultDuration(33);
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.ioDW.close();
		this.destination.delete();
	}

	@Test
	public void testWrite() throws FileNotFoundException, IOException {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(this.testTrack);
		writer.addFrame(generateFrame("I know a song...", 42));
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();
		Assertions.assertEquals(TrackType.SUBTITLE, reader.getTrackList()[0].getTrackType());
		Assertions.assertEquals(42, reader.getTrackList()[0].getTrackNo());
		LOG.info(reader.getReport());
		testDocTraversal();
	}

	@Test
	public void testMultipleTracks() throws Exception {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(this.testTrack);
		writer.addFrame(generateFrame("I know a song...", 42));
		final Track nextTrack = new Track();
		nextTrack.setTrackNo(2);
		nextTrack.setTrackType(TrackType.CONTROL);
		nextTrack.setCodecID("some logo thingy");
		nextTrack.setDefaultDuration(4242);
		writer.addTrack(nextTrack);
		writer.addFrame(generateFrame("that gets on everybody's nerves", 2));

		final Track virtualTrack = new Track();
		virtualTrack.setTrackNo(3);
		virtualTrack.setTrackType(TrackType.CONTROL);
		virtualTrack.setCodecID("virtual tracky!");
		virtualTrack.setDefaultDuration(1313);
		final TrackOperation operation = new TrackOperation();
		operation.addVirtualTrackPart(42);
		operation.addVirtualTrackPart(2);
		virtualTrack.setOperation(operation);
		writer.addTrack(virtualTrack);

		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();
		Assertions.assertEquals(TrackType.SUBTITLE, reader.getTrackList()[0].getTrackType());
		Assertions.assertEquals(42, reader.getTrackList()[0].getTrackNo());
		LOG.info(reader.getReport());
		testDocTraversal();
	}

	@Test
	public void testSilentTrack() throws FileNotFoundException, IOException {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(this.testTrack);
		writer.silenceTrack(13);
		writer.addFrame(generateFrame("I know a song...", 42));
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();
		Assertions.assertEquals(TrackType.SUBTITLE, reader.getTrackList()[0].getTrackType());
		Assertions.assertEquals(42, reader.getTrackList()[0].getTrackNo());
		LOG.info(reader.getReport());
		testDocTraversal();
	}

	public void testDocTraversal() throws FileNotFoundException, IOException {
		final FileDataSource ioDS = new FileDataSource(this.destination.getPath());
		final org.atriasoft.jimbe.ebml.Reader reader = new org.atriasoft.jimbe.ebml.Reader(ioDS);
		Element level0 = reader.readNextElement();
		while (level0 != null) {
			traverseElement(level0, ioDS, reader, 0);
			level0.skipData(ioDS);
			level0 = reader.readNextElement();
		}
	}

	private void traverseElement(
			final Element levelN,
			final DataSource ioDS,
			final org.atriasoft.jimbe.ebml.Reader reader,
			final int level) {
		if (levelN == null) {
			return;
		}
		LOG.info("Found element {} at level {}", levelN.getElementType().getName(), level);

		final int elemLevel = levelN.getElementType().getLevel();
		if (elemLevel != -1) {
			Assertions.assertEquals(level, elemLevel);
		}
		if (levelN instanceof MasterElement) {
			Element levelNPlusOne = ((MasterElement) levelN).readNextChild(reader);
			while (levelNPlusOne != null) {
				traverseElement(levelNPlusOne, ioDS, reader, level + 1);
				levelNPlusOne.skipData(ioDS);
				levelNPlusOne = ((MasterElement) levelN).readNextChild(reader);
			}
		}
	}

	private Frame generateFrame(final String string, final int trackNo) {
		final Frame frame = new Frame();
		frame.setData(ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8)));
		frame.setTrackNo(trackNo);
		frame.setTimecode(++this.timecode);
		return frame;
	}
}
