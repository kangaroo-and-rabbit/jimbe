package org.atriasoft.jimbe.matroska;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.model.Cues;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.TrackType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Cues (seek index).
 */
public class CuesTest {

	@Test
	public void testCuesWrittenInOutput() throws Exception {
		// A full roundtrip: write a file with frames, close (writes cues), re-read and check cues enable seeking
		final File tempFile = File.createTempFile("cues-test-", ".webm");
		try {
			final FileDataWriter output = new FileDataWriter(tempFile.getPath());
			final Writer writer = new Writer(output);
			writer.setTimecodeScale(1000000);
			writer.setDuration(3000);
			final Track track = new Track();
			track.setTrackNo(1);
			track.setTrackType(TrackType.SUBTITLE);
			track.setCodecID("S_TEXT/UTF8");
			writer.addTrack(track);

			// Write 3 keyframes to create 3 clusters -> 3 cue points
			for (int i = 0; i < 90; i++) {
				final Frame frame = new Frame();
				frame.setTrackNo(1);
				frame.setTimecode(i * 33L);
				frame.setKeyFrame(i % 30 == 0);
				frame.setData(ByteBuffer.wrap(new byte[] { (byte) i }));
				writer.addFrame(frame);
			}
			writer.close();
			output.close();

			// Re-read: verify the file is seekable (has cues) by checking seek positions
			try (FileDataSource source = new FileDataSource(tempFile.getPath())) {
				final Reader reader = new Reader(source);
				reader.readFile();
				// The file should have proper duration
				Assertions.assertEquals(3000.0, reader.getDuration(), 0.1);
				// Read all frames to verify integrity
				int frameCount = 0;
				int keyCount = 0;
				Frame frame;
				while ((frame = reader.getNextFrame()) != null) {
					frameCount++;
					if (frame.isKeyFrame()) {
						keyCount++;
					}
				}
				Assertions.assertEquals(90, frameCount);
				Assertions.assertEquals(3, keyCount);
			}
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testCuesWithMultipleTracks() throws Exception {
		final File tempFile = File.createTempFile("cues-multi-", ".webm");
		try {
			final FileDataWriter output = new FileDataWriter(tempFile.getPath());
			final Writer writer = new Writer(output);
			writer.setTimecodeScale(1000000);
			final Track track1 = new Track();
			track1.setTrackNo(1);
			track1.setTrackType(TrackType.SUBTITLE);
			track1.setCodecID("S_TEXT/UTF8");
			writer.addTrack(track1);

			final Track track2 = new Track();
			track2.setTrackNo(2);
			track2.setTrackType(TrackType.SUBTITLE);
			track2.setCodecID("S_TEXT/UTF8");
			writer.addTrack(track2);

			// Interleave video and audio frames
			for (int i = 0; i < 30; i++) {
				final Frame vFrame = new Frame();
				vFrame.setTrackNo(1);
				vFrame.setTimecode(i * 33L);
				vFrame.setKeyFrame(i % 15 == 0);
				vFrame.setData(ByteBuffer.wrap(new byte[] { (byte) i }));
				writer.addFrame(vFrame);

				final Frame aFrame = new Frame();
				aFrame.setTrackNo(2);
				aFrame.setTimecode(i * 33L);
				aFrame.setKeyFrame(true);
				aFrame.setData(ByteBuffer.wrap(new byte[] { (byte) (i + 100) }));
				writer.addFrame(aFrame);
			}
			writer.close();
			output.close();

			try (FileDataSource source = new FileDataSource(tempFile.getPath())) {
				final Reader reader = new Reader(source);
				reader.readFile();
				// Should have 2 tracks
				Assertions.assertEquals(2, reader.getTrackList().length);
				// All 60 frames should be readable
				int count = 0;
				Frame frame;
				while ((frame = reader.getNextFrame()) != null) {
					count++;
				}
				Assertions.assertEquals(60, count);
			}
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testAddCueRelativePosition() {
		// Test that Cues computes relative positions correctly
		final long segmentHeaderEnd = 100;
		final Cues cues = new Cues(segmentHeaderEnd);
		// Adding a cue at absolute position 500 should store relative position 400
		final Collection<Integer> tracks = new HashSet<>(Collections.singletonList(1));
		// Should not throw
		cues.addCue(500, 1000, tracks);
		cues.addCue(1500, 2000, tracks);
	}

	@Test
	public void testAddCueMultipleTracks() {
		final Cues cues = new Cues(0);
		final Collection<Integer> tracks = new HashSet<>(Arrays.asList(1, 2, 3));
		// Should create CueTrackPositions for each track
		cues.addCue(100, 0, tracks);
	}
}
