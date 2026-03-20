package org.atriasoft.ebml2.matroska;

import org.atriasoft.ebml2.matroska.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.atriasoft.ebml2.io.FileDataSource;
import org.atriasoft.ebml2.io.FileDataWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriterExtendedTest {
	private static final Logger LOG = LoggerFactory.getLogger(WriterExtendedTest.class);
	private File destination;
	private FileDataWriter ioDW;

	@BeforeEach
	public void setUp() throws Exception {
		this.destination = File.createTempFile("test-extended", ".mkv");
		this.ioDW = new FileDataWriter(this.destination.getPath());
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.ioDW.close();
		this.destination.delete();
	}

	@Test
	public void testVideoTrackWriteAndReadBack() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track videoTrack = new Track();
		videoTrack.setTrackNo(1);
		videoTrack.setTrackType(TrackType.VIDEO);
		videoTrack.setCodecID("V_MPEG4/ISO/AVC");
		videoTrack.setDefaultDuration(33333333L);
		final VideoTrack videoInfo = new VideoTrack();
		videoInfo.setPixelWidth((short) 640);
		videoInfo.setPixelHeight((short) 480);
		videoTrack.setVideo(videoInfo);

		writer.addTrack(videoTrack);

		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x00, 0x01, 0x67}));
		writer.addFrame(frame);

		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertEquals(1, reader.getTrackList().length);
		Assertions.assertEquals(TrackType.VIDEO, reader.getTrackList()[0].getTrackType());
		Assertions.assertEquals("V_MPEG4/ISO/AVC", reader.getTrackList()[0].getCodecID());
		Assertions.assertEquals(640, reader.getTrackList()[0].getVideo().getPixelWidth());
		Assertions.assertEquals(480, reader.getTrackList()[0].getVideo().getPixelHeight());
		LOG.info(reader.getReport());
	}

	@Test
	public void testAudioTrackWriteAndReadBack() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track audioTrack = new Track();
		audioTrack.setTrackNo(1);
		audioTrack.setTrackType(TrackType.AUDIO);
		audioTrack.setCodecID("A_AAC");
		audioTrack.setDefaultDuration(23000000L);
		final AudioTrack audioInfo = new AudioTrack();
		audioInfo.setSamplingFrequency(44100.0f);
		audioInfo.setChannels((short) 2);
		audioInfo.setBitDepth(16);
		audioTrack.setAudio(audioInfo);

		writer.addTrack(audioTrack);

		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03, 0x04}));
		writer.addFrame(frame);

		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertEquals(1, reader.getTrackList().length);
		Assertions.assertEquals(TrackType.AUDIO, reader.getTrackList()[0].getTrackType());
		Assertions.assertEquals("A_AAC", reader.getTrackList()[0].getCodecID());
		Assertions.assertEquals(44100.0f, reader.getTrackList()[0].getAudio().getSamplingFrequency(), 0.01);
		Assertions.assertEquals(2, reader.getTrackList()[0].getAudio().getChannels());
		Assertions.assertEquals(16, reader.getTrackList()[0].getAudio().getBitDepth());
		LOG.info(reader.getReport());
	}

	@Test
	public void testVideoAndAudioTracks() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track videoTrack = new Track();
		videoTrack.setTrackNo(1);
		videoTrack.setTrackType(TrackType.VIDEO);
		videoTrack.setCodecID("V_VP8");
		videoTrack.setDefaultDuration(33333333L);
		final VideoTrack videoInfo = new VideoTrack();
		videoInfo.setPixelWidth((short) 320);
		videoInfo.setPixelHeight((short) 240);
		videoTrack.setVideo(videoInfo);

		final Track audioTrack = new Track();
		audioTrack.setTrackNo(2);
		audioTrack.setTrackType(TrackType.AUDIO);
		audioTrack.setCodecID("A_VORBIS");
		audioTrack.setDefaultDuration(20000000L);
		final AudioTrack audioInfo = new AudioTrack();
		audioInfo.setSamplingFrequency(48000.0f);
		audioInfo.setChannels((short) 2);
		audioTrack.setAudio(audioInfo);

		writer.addTrack(videoTrack);
		writer.addTrack(audioTrack);

		for (int i = 0; i < 10; i++) {
			final Frame vFrame = new Frame();
			vFrame.setTrackNo(1);
			vFrame.setTimecode(i * 33);
			vFrame.setData(ByteBuffer.wrap(("video-frame-" + i).getBytes(StandardCharsets.UTF_8)));
			writer.addFrame(vFrame);

			final Frame aFrame = new Frame();
			aFrame.setTrackNo(2);
			aFrame.setTimecode(i * 20);
			aFrame.setData(ByteBuffer.wrap(("audio-frame-" + i).getBytes(StandardCharsets.UTF_8)));
			writer.addFrame(aFrame);
		}
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertEquals(2, reader.getTrackList().length);
		LOG.info(reader.getReport());
	}

	@Test
	public void testMultipleFramesAndReadBack() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track subtitleTrack = new Track();
		subtitleTrack.setTrackNo(1);
		subtitleTrack.setTrackType(TrackType.SUBTITLE);
		subtitleTrack.setCodecID("S_TEXT/UTF8");
		subtitleTrack.setDefaultDuration(1000);
		writer.addTrack(subtitleTrack);

		for (int i = 0; i < 20; i++) {
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 1000);
			frame.setData(ByteBuffer.wrap(("subtitle-" + i).getBytes(StandardCharsets.UTF_8)));
			writer.addFrame(frame);
		}
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		int frameCount = 0;
		Frame readFrame = reader.getNextFrame();
		while (readFrame != null) {
			frameCount++;
			readFrame = reader.getNextFrame();
		}
		Assertions.assertEquals(20, frameCount);
	}

	@Test
	public void testGetNextFrameByTrackNo() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track track1 = new Track();
		track1.setTrackNo(1);
		track1.setTrackType(TrackType.SUBTITLE);
		track1.setCodecID("S_TEXT/UTF8");
		track1.setDefaultDuration(1000);

		final Track track2 = new Track();
		track2.setTrackNo(2);
		track2.setTrackType(TrackType.SUBTITLE);
		track2.setCodecID("S_TEXT/UTF8");
		track2.setDefaultDuration(1000);

		writer.addTrack(track1);
		writer.addTrack(track2);

		for (int i = 0; i < 5; i++) {
			final Frame f1 = new Frame();
			f1.setTrackNo(1);
			f1.setTimecode(i * 1000);
			f1.setData(ByteBuffer.wrap(("track1-" + i).getBytes(StandardCharsets.UTF_8)));
			writer.addFrame(f1);

			final Frame f2 = new Frame();
			f2.setTrackNo(2);
			f2.setTimecode(i * 1000);
			f2.setData(ByteBuffer.wrap(("track2-" + i).getBytes(StandardCharsets.UTF_8)));
			writer.addFrame(f2);
		}
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		int track1Frames = 0;
		Frame readFrame = reader.getNextFrame(1);
		while (readFrame != null) {
			Assertions.assertEquals(1, readFrame.getTrackNo());
			track1Frames++;
			readFrame = reader.getNextFrame(1);
		}
		Assertions.assertEquals(5, track1Frames);
	}

	@Test
	public void testMuxingAppAndWritingApp() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		writer.addTrack(track);

		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
		writer.addFrame(frame);
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertNotNull(reader.getMuxingApp());
		Assertions.assertNotNull(reader.getWritingApp());
		Assertions.assertTrue(reader.getMuxingApp().length() > 0);
	}

	@Test
	public void testGetTrackByNumber() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track track = new Track();
		track.setTrackNo(42);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		writer.addTrack(track);

		final Frame frame = new Frame();
		frame.setTrackNo(42);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
		writer.addFrame(frame);
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertNotNull(reader.getTrack(42));
		Assertions.assertEquals(42, reader.getTrack(42).getTrackNo());
		Assertions.assertNull(reader.getTrack(999));
	}

	@Test
	public void testIsSeekable() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		writer.addTrack(track);

		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
		writer.addFrame(frame);
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertTrue(reader.isSeekable());
	}

	@Test
	public void testEmptyFileWithOnlyTrackAndClose() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		writer.addTrack(track);
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertEquals(1, reader.getTrackList().length);
		Assertions.assertNull(reader.getNextFrame());
	}

	@Test
	public void testTrackWithCustomLanguage() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		track.setLanguage("fre");
		track.setName("French Subtitles");
		writer.addTrack(track);

		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap("Bonjour".getBytes(StandardCharsets.UTF_8)));
		writer.addFrame(frame);
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		Assertions.assertEquals("fre", reader.getTrackList()[0].getLanguage());
		Assertions.assertEquals("French Subtitles", reader.getTrackList()[0].getName());
	}

	@Test
	public void testLargeNumberOfFrames() throws IOException {
		final Writer writer = new Writer(this.ioDW);

		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		writer.addTrack(track);

		final int totalFrames = 200;
		for (int i = 0; i < totalFrames; i++) {
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 100);
			frame.setData(ByteBuffer.wrap(("frame-" + i).getBytes(StandardCharsets.UTF_8)));
			writer.addFrame(frame);
		}
		writer.close();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		int frameCount = 0;
		while (reader.getNextFrame() != null) {
			frameCount++;
		}
		Assertions.assertEquals(totalFrames, frameCount);
	}
}
