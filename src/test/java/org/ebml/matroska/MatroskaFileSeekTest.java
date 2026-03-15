package org.ebml.matroska;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.ebml.io.FileDataSource;
import org.ebml.io.FileDataWriter;
import org.ebml.matroska.MatroskaFileTrack.MatroskaVideoTrack;
import org.ebml.matroska.MatroskaFileTrack.TrackType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the seek() method in MatroskaFile.
 *
 * Per the Javadoc: seek discards frames until hitting a timecode
 * larger than requested, then returns the previous frame's timecode
 * (rounded down to the nearest frame boundary).
 */
public class MatroskaFileSeekTest {
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

	private MatroskaFile openReader() throws IOException {
		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final MatroskaFile reader = new MatroskaFile(inputDataSource);
		reader.readFile();
		return reader;
	}

	private void addVideoTrack(final MatroskaFileWriter writer) {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setTrackNo(1);
		track.setTrackType(TrackType.VIDEO);
		track.setCodecID("V_VP8");
		track.setDefaultDuration(33333333L);
		final MatroskaVideoTrack videoInfo = new MatroskaVideoTrack();
		videoInfo.setPixelWidth((short) 320);
		videoInfo.setPixelHeight((short) 240);
		track.setVideo(videoInfo);
		writer.addTrack(track);
	}

	/**
	 * Write a file with 10 frames at 0, 50, 100, ..., 450 ms.
	 */
	private void writeTenFrames() {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);
		for (int i = 0; i < 10; i++) {
			final MatroskaFileFrame frame = new MatroskaFileFrame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 50L);
			frame.setData(ByteBuffer.wrap(new byte[]{(byte) i}));
			writer.addFrame(frame);
		}
		writer.close();
	}

	@Test
	public void testSeekToExactTimecode() throws IOException {
		writeTenFrames();
		final MatroskaFile reader = openReader();

		// Seek to exactly 200ms - should land on the frame at 200ms
		final long actual = reader.seek(200);
		Assertions.assertEquals(200, actual);

		// Next frame should be the one at 200ms
		final MatroskaFileFrame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(200, frame.getTimecode());
	}

	@Test
	public void testSeekBetweenTimecodes() throws IOException {
		writeTenFrames();
		final MatroskaFile reader = openReader();

		// Seek to 333ms - should round down to 300ms (frame 7)
		final long actual = reader.seek(333);
		Assertions.assertEquals(300, actual);

		final MatroskaFileFrame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(300, frame.getTimecode());
	}

	@Test
	public void testSeekToZero() throws IOException {
		writeTenFrames();
		final MatroskaFile reader = openReader();

		// First consume some frames
		reader.getNextFrame();
		reader.getNextFrame();
		reader.getNextFrame();

		// Seek back to 0
		final long actual = reader.seek(0);
		Assertions.assertEquals(0, actual);

		final MatroskaFileFrame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(0, frame.getTimecode());
	}

	@Test
	public void testSeekPastEnd() throws IOException {
		writeTenFrames();
		final MatroskaFile reader = openReader();

		// Seek past the last frame (450ms)
		final long actual = reader.seek(9999);
		Assertions.assertEquals(450, actual);
	}

	@Test
	public void testSeekThenReadRemaining() throws IOException {
		writeTenFrames();
		final MatroskaFile reader = openReader();

		// Seek to 200ms
		reader.seek(200);

		// Should be able to read frames from 200ms onwards
		int count = 0;
		MatroskaFileFrame frame = reader.getNextFrame();
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
		final MatroskaFile reader = openReader();

		// First seek to 300ms
		long actual = reader.seek(300);
		Assertions.assertEquals(300, actual);

		// Read one frame
		MatroskaFileFrame frame = reader.getNextFrame();
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
		final MatroskaFile reader = openReader();

		// Read all frames
		while (reader.getNextFrame() != null) {
			// consume
		}

		// Seek back to 150ms
		final long actual = reader.seek(150);
		Assertions.assertEquals(150, actual);

		final MatroskaFileFrame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(150, frame.getTimecode());
	}

	@Test
	public void testSeekToLastFrame() throws IOException {
		writeTenFrames();
		final MatroskaFile reader = openReader();

		// Seek to exactly the last frame
		final long actual = reader.seek(450);
		Assertions.assertEquals(450, actual);

		final MatroskaFileFrame frame = reader.getNextFrame();
		Assertions.assertNotNull(frame);
		Assertions.assertEquals(450, frame.getTimecode());

		// No more frames after last
		Assertions.assertNull(reader.getNextFrame());
	}

	@Test
	public void testIsSeekable() throws IOException {
		writeTenFrames();
		final MatroskaFile reader = openReader();
		Assertions.assertTrue(reader.isSeekable());
	}
}
