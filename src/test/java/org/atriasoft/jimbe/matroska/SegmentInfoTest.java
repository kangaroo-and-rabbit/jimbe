package org.atriasoft.jimbe.matroska;

import java.io.File;
import java.nio.ByteBuffer;

import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.SegmentInfo;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.TrackType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SegmentInfo.
 */
public class SegmentInfoTest {

	// ========== Getters/setters ==========

	@Test
	public void testDefaultTimecodeScale() {
		final SegmentInfo info = new SegmentInfo(0);
		Assertions.assertEquals(1000000, info.getTimecodeScale());
	}

	@Test
	public void testSetTimecodeScale() {
		final SegmentInfo info = new SegmentInfo(0);
		info.setTimecodeScale(500000);
		Assertions.assertEquals(500000, info.getTimecodeScale());
	}

	@Test
	public void testSetDuration() {
		final SegmentInfo info = new SegmentInfo(0);
		info.setDuration(120000.0);
		Assertions.assertEquals(120000.0, info.getDuration(), 0.001);
	}

	@Test
	public void testSetDate() {
		final SegmentInfo info = new SegmentInfo(0);
		final java.util.Date date = new java.util.Date(1000000000000L);
		info.setDate(date);
		Assertions.assertEquals(date, info.getDate());
	}

	@Test
	public void testDefaultDateIsNotNull() {
		final SegmentInfo info = new SegmentInfo(0);
		Assertions.assertNotNull(info.getDate());
	}

	// ========== Roundtrip via Writer/Reader ==========

	@Test
	public void testTimecodeScaleRoundtrip() throws Exception {
		final File tempFile = File.createTempFile("seginfo-tc-", ".webm");
		try {
			writeAndReadBack(tempFile, 1000000, 5000.0, (final Reader reader) -> {
				Assertions.assertEquals(1000000, reader.getTimecodeScale());
			});
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testCustomTimecodeScaleRoundtrip() throws Exception {
		final File tempFile = File.createTempFile("seginfo-ctc-", ".webm");
		try {
			writeAndReadBack(tempFile, 500000, 10000.0, (final Reader reader) -> {
				Assertions.assertEquals(500000, reader.getTimecodeScale());
			});
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testDurationRoundtrip() throws Exception {
		final File tempFile = File.createTempFile("seginfo-dur-", ".webm");
		try {
			writeAndReadBack(tempFile, 1000000, 42000.0, (final Reader reader) -> {
				Assertions.assertEquals(42000.0, reader.getDuration(), 1.0);
			});
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testZeroDuration() throws Exception {
		final File tempFile = File.createTempFile("seginfo-zero-", ".webm");
		try {
			final FileDataWriter output = new FileDataWriter(tempFile.getPath());
			final Writer writer = new Writer(output);
			writer.setTimecodeScale(1000000);
			// Don't set duration
			final Track track = new Track();
			track.setTrackNo(1);
			track.setTrackType(TrackType.SUBTITLE);
			track.setCodecID("S_TEXT/UTF8");
			writer.addTrack(track);
			final Frame frame = new Frame();
			frame.setTrackNo(1);
			frame.setTimecode(0);
			frame.setKeyFrame(true);
			frame.setData(ByteBuffer.wrap(new byte[] { 0x01 }));
			writer.addFrame(frame);
			writer.close();
			output.close();

			try (FileDataSource source = new FileDataSource(tempFile.getPath())) {
				final Reader reader = new Reader(source);
				reader.readFile();
				// Duration should be 0 or not set
				Assertions.assertEquals(0.0, reader.getDuration(), 0.001);
			}
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testSegmentInfoUpdatePreservesData() throws Exception {
		// Verify that update() (called by Writer.close()) preserves the segment info
		final File tempFile = File.createTempFile("seginfo-update-", ".webm");
		try {
			final FileDataWriter output = new FileDataWriter(tempFile.getPath());
			final Writer writer = new Writer(output);
			writer.setTimecodeScale(1000000);
			writer.setDuration(99999.0);
			final Track track = new Track();
			track.setTrackNo(1);
			track.setTrackType(TrackType.SUBTITLE);
			track.setCodecID("S_TEXT/UTF8");
			writer.addTrack(track);

			// Write multiple keyframes across different clusters
			for (int i = 0; i < 5; i++) {
				final Frame frame = new Frame();
				frame.setTrackNo(1);
				frame.setTimecode(i * 1000L);
				frame.setKeyFrame(true);
				frame.setData(ByteBuffer.wrap(new byte[] { (byte) i }));
				writer.addFrame(frame);
			}
			writer.close();
			output.close();

			try (FileDataSource source = new FileDataSource(tempFile.getPath())) {
				final Reader reader = new Reader(source);
				reader.readFile();
				Assertions.assertEquals(1000000, reader.getTimecodeScale());
				Assertions.assertEquals(99999.0, reader.getDuration(), 1.0);
				// Verify all frames survived
				int count = 0;
				Frame frame;
				while ((frame = reader.getNextFrame()) != null) {
					Assertions.assertEquals(count * 1000L, frame.getTimecode());
					count++;
				}
				Assertions.assertEquals(5, count);
			}
		} finally {
			tempFile.delete();
		}
	}

	// ========== Helper ==========

	@FunctionalInterface
	private interface ReaderAssertion {
		void check(Reader reader) throws Exception;
	}

	private void writeAndReadBack(final File tempFile, final long timecodeScale, final double duration,
			final ReaderAssertion assertion) throws Exception {
		final FileDataWriter output = new FileDataWriter(tempFile.getPath());
		final Writer writer = new Writer(output);
		writer.setTimecodeScale(timecodeScale);
		writer.setDuration(duration);
		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		writer.addTrack(track);
		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setKeyFrame(true);
		frame.setData(ByteBuffer.wrap(new byte[] { 0x01 }));
		writer.addFrame(frame);
		writer.close();
		output.close();

		try (FileDataSource source = new FileDataSource(tempFile.getPath())) {
			final Reader reader = new Reader(source);
			reader.readFile();
			assertion.check(reader);
		}
	}
}
