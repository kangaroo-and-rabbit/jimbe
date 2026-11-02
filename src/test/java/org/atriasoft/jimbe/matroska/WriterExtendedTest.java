package org.atriasoft.jimbe.matroska;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.model.AudioTrack;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.TrackType;
import org.atriasoft.jimbe.matroska.model.VideoTrack;
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
		frame.setData(ByteBuffer.wrap(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x67 }));
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
		frame.setData(ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03, 0x04 }));
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

	// ========== frames the blocks must keep apart, and timecodes they must keep exact ==========

	private static Track videoTrack() {
		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.VIDEO);
		track.setCodecID("V_VP8");
		final VideoTrack videoInfo = new VideoTrack();
		videoInfo.setPixelWidth((short) 320);
		videoInfo.setPixelHeight((short) 240);
		track.setVideo(videoInfo);
		return track;
	}

	private File writeFrames(final long... timecodes) throws Exception {
		final File file = File.createTempFile("test-frames", ".mkv");
		final FileDataWriter out = new FileDataWriter(file.getPath());
		final Writer writer = new Writer(out);
		writer.addTrack(videoTrack());
		for (int i = 0; i < timecodes.length; i++) {
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(timecodes[i]);
			frame.setKeyFrame(false);
			frame.setData(ByteBuffer.wrap(new byte[] { (byte) i, (byte) (i + 1) }));
			writer.addFrame(frame);
		}
		writer.close();
		out.close();
		return file;
	}

	private void assertFramesReadBack(final File file, final long... timecodes) throws Exception {
		final FileDataSource source = new FileDataSource(file.getPath());
		final Reader reader = new Reader(source);
		reader.readFile();
		for (int i = 0; i < timecodes.length; i++) {
			final Frame frame = reader.getNextFrame();
			Assertions.assertNotNull(frame, "Frame " + i + " is missing");
			Assertions.assertEquals(timecodes[i], frame.getTimecode(), "Timecode of frame " + i);
			Assertions.assertEquals(2, frame.getData().remaining(), "Size of frame " + i);
			Assertions.assertEquals((byte) i, frame.getData().get(), "Content of frame " + i);
		}
		Assertions.assertNull(reader.getNextFrame());
		source.close();
		file.delete();
	}

	@Test
	public void testFramesOfTheSameTimecodeStaySeparate() throws Exception {
		final long[] timecodes = { 0, 2000, 2000, 2000, 2000 };
		assertFramesReadBack(writeFrames(timecodes), timecodes);
	}

	@Test
	public void testFrameFarAfterTheOthersKeepsItsTimecode() throws Exception {
		final long[] timecodes = { 1000, 3600000, 3600040 };
		assertFramesReadBack(writeFrames(timecodes), timecodes);
	}

	@Test
	public void testFrameFarBeforeTheOthersKeepsItsTimecode() throws Exception {
		final long[] timecodes = { 3600000, 3600040, 0, 40 };
		assertFramesReadBack(writeFrames(timecodes), timecodes);
	}

	// ========== tracks larger than the usual reservation ==========

	private static Track audioTrack(final int number, final int codecPrivateSize) {
		final Track track = new Track();
		track.setTrackNo(number);
		track.setTrackUID(number);
		track.setTrackType(TrackType.AUDIO);
		track.setCodecID("A_VORBIS");
		final byte[] codecPrivate = new byte[codecPrivateSize];
		codecPrivate[codecPrivateSize - 1] = (byte) number;
		track.setCodecPrivate(ByteBuffer.wrap(codecPrivate));
		final AudioTrack audio = new AudioTrack();
		audio.setChannels((short) 2);
		audio.setSamplingFrequency(48000);
		track.setAudio(audio);
		return track;
	}

	private static Frame frameOf(final int trackNo, final long timecode) {
		final Frame frame = new Frame();
		frame.setTrackNo(trackNo);
		frame.setTimecode(timecode);
		frame.setData(ByteBuffer.wrap(new byte[] { (byte) trackNo, 2 }));
		return frame;
	}

	private void assertLargeTracksReadBack(final int... codecPrivateSizes) throws Exception {
		final FileDataSource source = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(source);
		reader.readFile();
		final Track[] tracks = reader.getTrackList();
		Assertions.assertEquals(codecPrivateSizes.length + 1, tracks.length);
		for (int i = 0; i < codecPrivateSizes.length; i++) {
			final ByteBuffer codecPrivate = tracks[i + 1].getCodecPrivate();
			Assertions.assertEquals(codecPrivateSizes[i], codecPrivate.remaining());
			Assertions.assertEquals((byte) (i + 2), codecPrivate.get(codecPrivate.limit() - 1));
		}
		Assertions.assertEquals(0, reader.getNextFrame().getTimecode());
		Assertions.assertEquals(40, reader.getNextFrame().getTimecode());
		Assertions.assertNull(reader.getNextFrame());
		source.close();
	}

	@Test
	public void testTracksLargerThanTheUsualBlock() throws Exception {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(videoTrack());
		writer.addTrack(audioTrack(2, 3132));
		writer.addTrack(audioTrack(3, 3132));
		writer.addFrame(frameOf(1, 0));
		writer.addFrame(frameOf(2, 40));
		writer.close();

		assertLargeTracksReadBack(3132, 3132);
	}

	@Test
	public void testTracksThatFillTheUsualBlockExactly() throws Exception {
		// Around the sizes where the padding left by a 4096 byte block would be 0, 1 or 2 bytes.
		for (int codecPrivateSize = 3900; codecPrivateSize < 4100; codecPrivateSize++) {
			final File file = File.createTempFile("test-tracks", ".mkv");
			final FileDataWriter out = new FileDataWriter(file.getPath());
			final Writer writer = new Writer(out);
			writer.addTrack(videoTrack());
			writer.addTrack(audioTrack(2, codecPrivateSize));
			writer.addFrame(frameOf(1, 0));
			writer.close();
			out.close();

			final FileDataSource source = new FileDataSource(file.getPath());
			final Reader reader = new Reader(source);
			reader.readFile();
			Assertions.assertEquals(codecPrivateSize, reader.getTrackList()[1].getCodecPrivate().remaining());
			Assertions.assertEquals(0, reader.getNextFrame().getTimecode());
			source.close();
			file.delete();
		}
	}

	@Test
	public void testTrackAddedAfterFramesButBeforeTheFirstClusterIsWritten() throws Exception {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(videoTrack());
		writer.addFrame(frameOf(1, 0));
		writer.addTrack(audioTrack(2, 6000));
		writer.addFrame(frameOf(2, 40));
		writer.close();

		assertLargeTracksReadBack(6000);
	}

	@Test
	public void testTrackAddedLateThatStillFits() throws Exception {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(videoTrack());
		writer.addFrame(frameOf(1, 0));
		writer.flush();
		writer.addTrack(audioTrack(2, 500));
		writer.addFrame(frameOf(2, 40));
		writer.close();

		assertLargeTracksReadBack(500);
	}

	@Test
	public void testTrackAddedLateThatDoesNotFitIsRefusedAtOnce() throws Exception {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(videoTrack());
		writer.addFrame(frameOf(1, 0));
		writer.flush();
		Assertions.assertThrows(IllegalStateException.class, () -> writer.addTrack(audioTrack(2, 8000)));
		// The refused track leaves the file as it was.
		writer.addFrame(frameOf(1, 40));
		writer.close();

		assertLargeTracksReadBack();
	}

	@Test
	public void testFileWithoutFrameKeepsItsTracks() throws Exception {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(videoTrack());
		writer.close();

		final FileDataSource source = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(source);
		reader.readFile();
		Assertions.assertEquals(1, reader.getTrackList().length);
		Assertions.assertNull(reader.getNextFrame());
		source.close();
	}

	// ========== cluster limits ==========

	@Test
	public void testClusterReaches512KilobytesBeforeANonKeyFrameEndsIt() throws Exception {
		final Writer writer = new Writer(this.ioDW);
		writer.addTrack(videoTrack());
		// 400 kB a second of frames without a key frame: only the 5 s or the 4 MB limit may end a cluster.
		for (int i = 0; i < 100; i++) {
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 40L);
			frame.setKeyFrame(i == 0);
			frame.setData(ByteBuffer.wrap(new byte[16 * 1024]));
			writer.addFrame(frame);
		}
		writer.close();

		final byte[] bytes = java.nio.file.Files.readAllBytes(this.destination.toPath());
		int clusters = 0;
		// Past the SeekHead, which names the cluster ID too.
		for (int i = 512; i + 3 < bytes.length; i++) {
			if (bytes[i] == 0x1F && bytes[i + 1] == 0x43 && bytes[i + 2] == (byte) 0xB6 && bytes[i + 3] == 0x75) {
				clusters++;
			}
		}
		Assertions.assertEquals(1, clusters);
	}
}
