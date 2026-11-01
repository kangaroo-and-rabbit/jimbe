package org.atriasoft.jimbe.matroska;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.atriasoft.jimbe.io.DataSource;
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

/**
 * Tests for the seek() method in Reader.
 *
 * Per the Javadoc: seek discards frames until hitting a timecode
 * larger than requested, then returns the previous frame's timecode
 * (rounded down to the nearest frame boundary).
 */
public class ReaderSeekTest {
	private File destination;
	private FileDataWriter ioDW;

	@BeforeEach
	public void setUp() throws Exception {
		this.destination = File.createTempFile("test-seek", ".mkv");
		this.ioDW = new FileDataWriter(this.destination.getPath());
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.ioDW.close();
		this.destination.delete();
	}

	private Reader openReader() throws IOException {
		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();
		return reader;
	}

	private void addVideoTrack(final Writer writer) {
		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.VIDEO);
		track.setCodecID("V_VP8");
		track.setDefaultDuration(33333333L);
		final VideoTrack videoInfo = new VideoTrack();
		videoInfo.setPixelWidth((short) 320);
		videoInfo.setPixelHeight((short) 240);
		track.setVideo(videoInfo);
		writer.addTrack(track);
	}

	/**
	 * Write a file with 10 frames at 0, 50, 100, ..., 450 ms.
	 */
	private void writeTenFrames() {
		final Writer writer = new Writer(this.ioDW);
		addVideoTrack(writer);
		for (int i = 0; i < 10; i++) {
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 50L);
			frame.setData(ByteBuffer.wrap(new byte[] { (byte) i }));
			writer.addFrame(frame);
		}
		writer.close();
	}

	@Test
	public void testSeekToExactTimecode() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// Seek to exactly 200ms - should land on the frame at 200ms
		final long actual = reader.seek(200);
		Assertions.assertEquals(200, actual);

		// Next frame should be the one at 200ms
		final Frame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(200, frame.getTimecode());
	}

	@Test
	public void testSeekBetweenTimecodes() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// Seek to 333ms - should round down to 300ms (frame 7)
		final long actual = reader.seek(333);
		Assertions.assertEquals(300, actual);

		final Frame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(300, frame.getTimecode());
	}

	@Test
	public void testSeekToZero() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// First consume some frames
		reader.getNextFrame();
		reader.getNextFrame();
		reader.getNextFrame();

		// Seek back to 0
		final long actual = reader.seek(0);
		Assertions.assertEquals(0, actual);

		final Frame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(0, frame.getTimecode());
	}

	@Test
	public void testSeekPastEnd() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// Seek past the last frame (450ms)
		final long actual = reader.seek(9999);
		Assertions.assertEquals(450, actual);
	}

	@Test
	public void testSeekThenReadRemaining() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// Seek to 200ms
		reader.seek(200);

		// Should be able to read frames from 200ms onwards
		int count = 0;
		Frame frame = reader.getNextFrame();
		while (frame != null) {
			Assertions.assertTrue(frame.getTimecode() >= 200,
					"Frame timecode " + frame.getTimecode() + " should be >= 200");
			count++;
			frame = reader.getNextFrame();
		}
		// Frames at 200, 250, 300, 350, 400, 450 = 6 frames
		Assertions.assertEquals(6, count);
	}

	@Test
	public void testSeekTwice() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// First seek to 300ms
		long actual = reader.seek(300);
		Assertions.assertEquals(300, actual);

		// Read one frame
		Frame frame = reader.getNextFrame();
		Assertions.assertEquals(300, frame.getTimecode());

		// Seek again to 100ms (backwards)
		actual = reader.seek(100);
		Assertions.assertEquals(100, actual);

		frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(100, frame.getTimecode());
	}

	@Test
	public void testSeekAfterReadingAllFrames() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// Read all frames
		while (reader.getNextFrame() != null) {
			// consume
		}

		// Seek back to 150ms
		final long actual = reader.seek(150);
		Assertions.assertEquals(150, actual);

		final Frame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(150, frame.getTimecode());
	}

	@Test
	public void testSeekToLastFrame() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();

		// Seek to exactly the last frame
		final long actual = reader.seek(450);
		Assertions.assertEquals(450, actual);

		final Frame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(450, frame.getTimecode());

		// No more frames after last
		Assertions.assertNull(reader.getNextFrame());
	}

	@Test
	public void testIsSeekable() throws IOException {
		writeTenFrames();
		final Reader reader = openReader();
		Assertions.assertTrue(reader.isSeekable());
	}

	@Test
	public void testSeekBeforeReadFileThrows() throws IOException {
		writeTenFrames();
		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		// Do NOT call readFile() -> level0 is null
		Assertions.assertThrows(IllegalStateException.class, () -> reader.seek(0));
	}

	@Test
	public void testSeekWithNoClusterThrows() throws IOException {
		// Write a minimal EBML file with Segment but scanFirstCluster=false
		// and no cluster encountered (file has only headers, no cluster in the loop).
		// The easiest way: write a normal file, readFile with scanFirstCluster=false,
		// then overwrite firstClusterFilePosition... but that field is private.
		// Instead: create a file, read it with scanFirstCluster=false.
		// Actually the cluster IS found during readFile (break happens after setting
		// firstClusterFilePosition). We need a file with no Cluster at all.
		// Build a minimal EBML+Segment with only Info and Tracks, no frames.
		// The writer always writes at least one cluster though.
		// Simplest approach: write a raw truncated file containing only EBML header + Segment start + Info + Tracks.
		final byte[] fullFile;
		writeTenFrames();
		try (final java.io.RandomAccessFile raf = new java.io.RandomAccessFile(this.destination, "r")) {
			fullFile = new byte[(int) raf.length()];
			raf.readFully(fullFile);
		}
		// Find the Cluster position and truncate before it
		int clusterPos = -1;
		for (int i = 0; i < fullFile.length - 3; i++) {
			if (fullFile[i] == (byte) 0x1F && fullFile[i + 1] == (byte) 0x43 && fullFile[i + 2] == (byte) 0xB6
					&& fullFile[i + 3] == (byte) 0x75) {
				clusterPos = i;
			}
		}
		Assertions.assertTrue(clusterPos > 0);
		try (final java.io.RandomAccessFile raf = new java.io.RandomAccessFile(this.destination, "rw")) {
			raf.setLength(clusterPos);
		}

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();
		// No cluster was found -> firstClusterFilePosition stays -1
		Assertions.assertThrows(IllegalStateException.class, () -> reader.seek(0));
	}

	@Test
	public void testSeekWithNonSeekableDataSourceThrows() throws IOException {
		writeTenFrames();
		// Wrap the FileDataSource in a non-seekable delegate
		final FileDataSource realSource = new FileDataSource(this.destination.getPath());
		final DataSource nonSeekable = new DataSource() {
			@Override
			public byte readByte() {
				return realSource.readByte();
			}

			@Override
			public int read(final ByteBuffer buff) {
				return realSource.read(buff);
			}

			@Override
			public long skip(final long offset) {
				return realSource.skip(offset);
			}

			@Override
			public long length() {
				return realSource.length();
			}

			@Override
			public long getFilePointer() {
				return realSource.getFilePointer();
			}

			@Override
			public boolean isSeekable() {
				return false;
			}

			@Override
			public long seek(final long pos) {
				return realSource.seek(pos);
			}
		};
		final Reader reader = new Reader(nonSeekable);
		reader.readFile();
		Assertions.assertThrows(UnsupportedOperationException.class, () -> reader.seek(0));
	}

	// ========== seeking through the cues ==========

	/** Counts what is read, to tell a seek through the cues from a scan of the whole file. */
	private static final class CountingSource implements DataSource {
		private final DataSource source;
		private long bytesRead = 0;

		private CountingSource(final DataSource source) {
			this.source = source;
		}

		@Override
		public byte readByte() {
			this.bytesRead++;
			return this.source.readByte();
		}

		@Override
		public int read(final ByteBuffer buff) {
			final int count = this.source.read(buff);
			this.bytesRead += count;
			return count;
		}

		@Override
		public long skip(final long offset) {
			return this.source.skip(offset);
		}

		@Override
		public long length() {
			return this.source.length();
		}

		@Override
		public long getFilePointer() {
			return this.source.getFilePointer();
		}

		@Override
		public boolean isSeekable() {
			return this.source.isSeekable();
		}

		@Override
		public long seek(final long pos) {
			return this.source.seek(pos);
		}
	}

	private static final int LONG_FILE_SECONDS = 600;
	private static final int FRAME_SIZE = 4096;

	/** Ten minutes at 25 key frames a second, several clusters of it, each frame numbered. */
	private void writeLongFile() {
		final Writer writer = new Writer(this.ioDW);
		addVideoTrack(writer);
		for (int i = 0; i < LONG_FILE_SECONDS * 25; i++) {
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 40L);
			final byte[] data = new byte[FRAME_SIZE];
			ByteBuffer.wrap(data).putInt(i);
			frame.setData(ByteBuffer.wrap(data));
			writer.addFrame(frame);
		}
		writer.close();
	}

	private static int numberOf(final Frame frame) {
		return frame.getData().getInt();
	}

	private long[] seekPoints() {
		final long end = LONG_FILE_SECONDS * 1000L;
		return new long[] { 0, 1, 39, 40, 41, 999, 5000, 123456, 299999, end - 41, end - 40, end - 1, end, end + 5000 };
	}

	private void assertSeekFindsEveryFrame(final Reader reader) {
		for (final long target : seekPoints()) {
			final long expected = Math.min(target / 40, LONG_FILE_SECONDS * 25L - 1);
			Assertions.assertEquals(expected * 40, reader.seek(target), "Seek to " + target);
			Assertions.assertEquals((int) expected, numberOf(reader.getNextFrame()), "Frame after seek to " + target);
			if (expected + 1 < LONG_FILE_SECONDS * 25L) {
				Assertions.assertEquals((int) expected + 1, numberOf(reader.getNextFrame()),
						"Second frame after seek to " + target);
			}
		}
	}

	@Test
	public void testSeekThroughTheCuesFindsEveryFrame() throws IOException {
		writeLongFile();
		assertSeekFindsEveryFrame(openReader());
	}

	@Test
	public void testSeekThroughTheCuesOnlyReadsTheEndOfTheFile() throws IOException {
		writeLongFile();
		final CountingSource source = new CountingSource(new FileDataSource(this.destination.getPath()));
		final Reader reader = new Reader(source);
		reader.readFile();
		reader.getNextFrame();

		final long before = source.bytesRead;
		Assertions.assertEquals((LONG_FILE_SECONDS - 1) * 1000L, reader.seek((LONG_FILE_SECONDS - 1) * 1000L));
		final long read = source.bytesRead - before;

		Assertions.assertTrue(read < this.destination.length() / 50,
				"Read " + read + " bytes of " + this.destination.length());
	}

	/** Replaces every occurrence of the Cues ID in the SeekHead area, so the file no longer says where they are. */
	private void hideTheCues() throws IOException {
		final byte[] bytes = Files.readAllBytes(this.destination.toPath());
		for (int i = 0; i < 512; i++) {
			if (bytes[i] == 0x1C && bytes[i + 1] == 0x53 && bytes[i + 2] == (byte) 0xBB && bytes[i + 3] == 0x6B) {
				bytes[i + 3] = 0x6C;
			}
		}
		Files.write(this.destination.toPath(), bytes);
	}

	@Test
	public void testSeekWithoutCuesFindsTheSameFrames() throws IOException {
		writeLongFile();
		hideTheCues();
		assertSeekFindsEveryFrame(openReader());
	}

	@Test
	public void testSeekHeadPointingElsewhereFallsBackToTheScan() throws IOException {
		writeLongFile();
		final byte[] bytes = Files.readAllBytes(this.destination.toPath());
		for (int i = bytes.length - 1; i >= 3; i--) {
			if (bytes[i - 3] == 0x1C && bytes[i - 2] == 0x53 && bytes[i - 1] == (byte) 0xBB && bytes[i] == 0x6B) {
				bytes[i] = 0x6C; // the Cues element itself, the SeekHead still points at it
				break;
			}
		}
		Files.write(this.destination.toPath(), bytes);
		assertSeekFindsEveryFrame(openReader());
	}

	@Test
	public void testCueWithoutClusterFallsBackToTheScan() throws IOException {
		writeLongFile();
		final byte[] bytes = Files.readAllBytes(this.destination.toPath());
		// The cues end the file: the last bytes are the position of the last cluster, one byte too far now.
		int patched = 0;
		for (int size = 1; size <= 4; size++) {
			final int id = bytes.length - size - 2;
			if (bytes[id] == (byte) 0xF1 && bytes[id + 1] == (byte) (0x80 | size)) {
				bytes[bytes.length - 1]++;
				patched++;
			}
		}
		Assertions.assertEquals(1, patched, "The last cluster position should end the file");
		Files.write(this.destination.toPath(), bytes);
		assertSeekFindsEveryFrame(openReader());
	}

	@Test
	public void testClustersOutOfOrderIgnoreTheCues() throws IOException {
		final Writer writer = new Writer(this.ioDW);
		addVideoTrack(writer);
		final long[] timecodes = { 3600000, 3600040, 0, 40 };
		for (final long timecode : timecodes) {
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(timecode);
			frame.setKeyFrame(false);
			frame.setData(ByteBuffer.wrap(new byte[] { 1 }));
			writer.addFrame(frame);
		}
		writer.close();

		final Reader reader = openReader();
		// Read from the start like the file is laid out: the frame found is the one before the first that is too late.
		Assertions.assertEquals(3600000, reader.seek(3600010));
		Assertions.assertEquals(3600000, reader.getNextFrame().getTimecode());
		Assertions.assertEquals(3600040, reader.getNextFrame().getTimecode());
	}

	@Test
	public void testFrameNamedByTheCueDoesNotComeFirstInItsCluster() throws IOException {
		final Writer writer = new Writer(this.ioDW);
		addVideoTrack(writer);
		final Track audio = new Track();
		audio.setTrackNo(2);
		audio.setTrackType(TrackType.AUDIO);
		audio.setCodecID("A_OPUS");
		final AudioTrack audioInfo = new AudioTrack();
		audioInfo.setChannels((short) 2);
		audioInfo.setSamplingFrequency(48000);
		audio.setAudio(audioInfo);
		writer.addTrack(audio);
		// The audio at 1001 opens the second cluster; the video at 1000 sets its timecode, and so the cue.
		writer.addFrame(seekFrame(1, 0, true));
		writer.addFrame(seekFrame(1, 900, false));
		writer.addFrame(seekFrame(2, 1001, true));
		writer.addFrame(seekFrame(1, 1000, true));
		writer.addFrame(seekFrame(1, 1040, false));
		writer.close();

		final Reader reader = openReader();
		Assertions.assertEquals(900, reader.seek(1000));
		Assertions.assertEquals(900, reader.getNextFrame().getTimecode());
		Assertions.assertEquals(1001, reader.getNextFrame().getTimecode());
		Assertions.assertEquals(1000, reader.getNextFrame().getTimecode());
	}

	private static Frame seekFrame(final int trackNo, final long timecode, final boolean keyFrame) {
		final Frame frame = new Frame();
		frame.setTrackNo(trackNo);
		frame.setTimecode(timecode);
		frame.setKeyFrame(keyFrame);
		frame.setData(ByteBuffer.wrap(new byte[] { 1 }));
		return frame;
	}

	@Test
	public void testClustersOverlappingInTimeLoseNoFrame() throws IOException {
		final Writer writer = new Writer(this.ioDW);
		addVideoTrack(writer);
		final Track audio = new Track();
		audio.setTrackNo(2);
		audio.setTrackType(TrackType.AUDIO);
		audio.setCodecID("A_OPUS");
		final AudioTrack audioInfo = new AudioTrack();
		audioInfo.setChannels((short) 2);
		audioInfo.setSamplingFrequency(48000);
		audio.setAudio(audioInfo);
		writer.addTrack(audio);
		// The first cluster reaches 5001, the second starts at 4990: the cue for 4995 names the second.
		writer.addFrame(seekFrame(1, 0, true));
		writer.addFrame(seekFrame(1, 4994, false));
		writer.addFrame(seekFrame(1, 5001, false));
		writer.addFrame(seekFrame(2, 4990, true));
		writer.addFrame(seekFrame(1, 5040, false));
		writer.close();

		final Reader reader = openReader();
		Assertions.assertEquals(4994, reader.seek(4995));
		Assertions.assertEquals(4994, reader.getNextFrame().getTimecode());
		Assertions.assertEquals(5001, reader.getNextFrame().getTimecode());
		Assertions.assertEquals(4990, reader.getNextFrame().getTimecode());
		Assertions.assertEquals(5040, reader.getNextFrame().getTimecode());
	}

	@Test
	public void testDamagedSeekHeadOpensAndSeeks() throws IOException {
		writeLongFile();
		final byte[] bytes = Files.readAllBytes(this.destination.toPath());
		// Every child of the SeekHead announces more than the file holds.
		for (int i = 0; i + 4 < 256; i++) {
			if (bytes[i] == 0x53 && bytes[i + 1] == (byte) 0xAB) {
				bytes[i + 2] = (byte) 0x01;
				bytes[i + 3] = (byte) 0xFF;
			}
		}
		Files.write(this.destination.toPath(), bytes);
		assertSeekFindsEveryFrame(openReader());
	}
}
