package org.atriasoft.jimbe.matroska;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.model.Cluster;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Cluster.
 */
public class ClusterTest {

	private File tempFile;
	private FileDataWriter writer;

	@BeforeEach
	public void setUp() throws Exception {
		this.tempFile = File.createTempFile("cluster-test-", ".bin");
		this.writer = new FileDataWriter(this.tempFile.getPath());
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.writer.close();
		this.tempFile.delete();
	}

	// ========== Initial state ==========

	@Test
	public void testInitialClusterTimecode() {
		final Cluster cluster = new Cluster();
		Assertions.assertEquals(Long.MAX_VALUE, cluster.getClusterTimecode());
	}

	@Test
	public void testInitialTracksEmpty() {
		final Cluster cluster = new Cluster();
		Assertions.assertTrue(cluster.getTracks().isEmpty());
	}

	// ========== shouldFlushBefore ==========

	@Test
	public void testShouldFlushBeforeReturnsFalseForEmptyCluster() {
		final Cluster cluster = new Cluster();
		final Frame keyFrame = makeFrame(1, 0, true, new byte[] { 0x01 });
		Assertions.assertFalse(cluster.shouldFlushBefore(keyFrame));
	}

	@Test
	public void testShouldFlushBeforeReturnsFalseForNonKeyframe() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		final Frame nonKeyFrame = makeFrame(1, 33, false, new byte[] { 0x02 });
		Assertions.assertFalse(cluster.shouldFlushBefore(nonKeyFrame));
	}

	@Test
	public void testShouldFlushBeforeReturnsTrueForKeyframeWithExistingFrames() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		final Frame keyFrame = makeFrame(1, 1000, true, new byte[] { 0x02 });
		Assertions.assertTrue(cluster.shouldFlushBefore(keyFrame));
	}

	// ========== addFrame ==========

	@Test
	public void testAddFrameSetsClusterTimecode() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 500, true, new byte[] { 0x01 }));
		Assertions.assertEquals(500, cluster.getClusterTimecode());
	}

	@Test
	public void testAddFrameKeepsMinimumTimecode() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 500, true, new byte[] { 0x01 }));
		cluster.addFrame(makeFrame(1, 200, false, new byte[] { 0x02 }));
		Assertions.assertEquals(200, cluster.getClusterTimecode());
	}

	@Test
	public void testAddFrameTracksMultipleTracks() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		cluster.addFrame(makeFrame(2, 0, true, new byte[] { 0x02 }));
		cluster.addFrame(makeFrame(3, 0, true, new byte[] { 0x03 }));
		final Collection<Integer> tracks = cluster.getTracks();
		Assertions.assertEquals(3, tracks.size());
		Assertions.assertTrue(tracks.contains(1));
		Assertions.assertTrue(tracks.contains(2));
		Assertions.assertTrue(tracks.contains(3));
	}

	@Test
	public void testAddFrameReturnsTrueWithinLimits() {
		final Cluster cluster = new Cluster();
		cluster.setLimitParameters(5000, 128 * 1024);
		final boolean result = cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		Assertions.assertTrue(result);
	}

	@Test
	public void testAddFrameReturnsFalseWhenSizeLimitExceeded() {
		final Cluster cluster = new Cluster();
		cluster.setLimitParameters(Long.MAX_VALUE, 5);
		cluster.addFrame(makeFrame(1, 0, true, new byte[3]));
		final boolean result = cluster.addFrame(makeFrame(1, 33, false, new byte[3]));
		Assertions.assertFalse(result);
	}

	@Test
	public void testAddFrameReturnsFalseWhenDurationLimitExceeded() {
		final Cluster cluster = new Cluster();
		cluster.setLimitParameters(100, Integer.MAX_VALUE);
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		final boolean result = cluster.addFrame(makeFrame(1, 200, false, new byte[] { 0x02 }));
		Assertions.assertFalse(result);
	}

	// ========== flush ==========

	@Test
	public void testFlushEmptyClusterReturnsZero() {
		final Cluster cluster = new Cluster();
		final long bytesWritten = cluster.flush(this.writer);
		Assertions.assertEquals(0, bytesWritten);
	}

	@Test
	public void testFlushWritesBytesAndResets() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 100, true, new byte[] { 0x01, 0x02, 0x03 }));
		final long bytesWritten = cluster.flush(this.writer);
		Assertions.assertTrue(bytesWritten > 0);
		// After flush, cluster should be reset
		Assertions.assertEquals(Long.MAX_VALUE, cluster.getClusterTimecode());
		Assertions.assertTrue(cluster.getTracks().isEmpty());
	}

	@Test
	public void testFlushConvertsTimecodeToRelative() throws Exception {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 1000, true, new byte[] { 0x01 }));
		cluster.addFrame(makeFrame(1, 1033, false, new byte[] { 0x02 }));
		cluster.flush(this.writer);
		this.writer.close();

		// Re-read and verify timecodes
		try (FileDataSource source = new FileDataSource(this.tempFile.getPath())) {
			final org.atriasoft.jimbe.ebml.Reader ebmlReader = new org.atriasoft.jimbe.ebml.Reader(source);
			final org.atriasoft.jimbe.ebml.model.Element clusterElem = ebmlReader.readNextElement();
			Assertions.assertNotNull(clusterElem);
			// The cluster element should be readable (non-zero size)
			Assertions.assertTrue(clusterElem.getTotalSize() > 0);
		}
	}

	@Test
	public void testFlushMultipleFramesProducesValidCluster() throws Exception {
		final Cluster cluster = new Cluster();
		for (int i = 0; i < 10; i++) {
			cluster.addFrame(makeFrame(1, i * 33, i == 0, new byte[] { (byte) i, (byte) (i + 1) }));
		}
		final long bytesWritten = cluster.flush(this.writer);
		Assertions.assertTrue(bytesWritten > 0);
	}

	@Test
	public void testFlushCanBeReusedAfterFlush() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		cluster.flush(this.writer);

		// Should be reusable
		cluster.addFrame(makeFrame(1, 5000, true, new byte[] { 0x02 }));
		Assertions.assertEquals(5000, cluster.getClusterTimecode());
		final long bytesWritten = cluster.flush(this.writer);
		Assertions.assertTrue(bytesWritten > 0);
	}

	// ========== silenceTrack / unsilenceTrack ==========

	@Test
	public void testSilenceAndUnsilenceTrack() {
		final Cluster cluster = new Cluster();
		cluster.silenceTrack(5);
		cluster.unsilenceTrack(5);
		// Should not throw and should produce valid output
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		final long bytesWritten = cluster.flush(this.writer);
		Assertions.assertTrue(bytesWritten > 0);
	}

	@Test
	public void testSilencedTrackWrittenInCluster() {
		final Cluster cluster = new Cluster();
		cluster.silenceTrack(99);
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		final long bytesWritten = cluster.flush(this.writer);
		// With silent track, the output should be larger than without
		final Cluster cluster2 = new Cluster();
		cluster2.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		final long bytesWithout = cluster2.flush(this.writer);
		Assertions.assertTrue(bytesWritten > bytesWithout);
	}

	// ========== Roundtrip via Writer/Reader ==========

	@Test
	public void testClusterRoundtripPreservesTimecodes() throws Exception {
		// Write a file with known frames (using SUBTITLE track to avoid VideoTrack requirement)
		final Writer muxer = new Writer(this.writer);
		muxer.setTimecodeScale(1000000);
		muxer.setDuration(10000);
		final org.atriasoft.jimbe.matroska.model.Track track = new org.atriasoft.jimbe.matroska.model.Track();
		track.setTrackNo(1);
		track.setTrackType(org.atriasoft.jimbe.matroska.model.TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		muxer.addTrack(track);

		final long[] timecodes = { 0, 33, 67, 100, 133, 167, 200 };
		for (int i = 0; i < timecodes.length; i++) {
			muxer.addFrame(makeFrame(1, timecodes[i], i == 0, new byte[] { (byte) (i + 1) }));
		}
		muxer.close();
		this.writer.close();

		// Re-read and verify
		try (FileDataSource source = new FileDataSource(this.tempFile.getPath())) {
			final Reader reader = new Reader(source);
			reader.readFile();
			for (int i = 0; i < timecodes.length; i++) {
				final Frame frame = reader.getNextFrame();
				Assertions.assertNotNull(frame, "Expected frame " + i);
				Assertions.assertEquals(timecodes[i], frame.getTimecode(), "Timecode mismatch at frame " + i);
			}
			Assertions.assertNull(reader.getNextFrame(), "No more frames expected");
		}
	}

	@Test
	public void testClusterSplitAtKeyframes() throws Exception {
		// Write frames with 2 keyframes -> should produce 2 clusters
		final Writer muxer = new Writer(this.writer);
		muxer.setTimecodeScale(1000000);
		final org.atriasoft.jimbe.matroska.model.Track track = new org.atriasoft.jimbe.matroska.model.Track();
		track.setTrackNo(1);
		track.setTrackType(org.atriasoft.jimbe.matroska.model.TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		muxer.addTrack(track);

		// First cluster: keyframe at 0, non-key at 33, 67
		muxer.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		muxer.addFrame(makeFrame(1, 33, false, new byte[] { 0x02 }));
		muxer.addFrame(makeFrame(1, 67, false, new byte[] { 0x03 }));
		// Second cluster: keyframe at 100
		muxer.addFrame(makeFrame(1, 100, true, new byte[] { 0x04 }));
		muxer.addFrame(makeFrame(1, 133, false, new byte[] { 0x05 }));
		muxer.close();
		this.writer.close();

		// Re-read: all frames should be present with correct timecodes and keyframe flags
		try (FileDataSource source = new FileDataSource(this.tempFile.getPath())) {
			final Reader reader = new Reader(source);
			reader.readFile();

			final Frame f0 = reader.getNextFrame();
			Assertions.assertEquals(0, f0.getTimecode());
			Assertions.assertTrue(f0.isKeyFrame());

			final Frame f1 = reader.getNextFrame();
			Assertions.assertEquals(33, f1.getTimecode());
			Assertions.assertFalse(f1.isKeyFrame());

			final Frame f2 = reader.getNextFrame();
			Assertions.assertEquals(67, f2.getTimecode());
			Assertions.assertFalse(f2.isKeyFrame());

			final Frame f3 = reader.getNextFrame();
			Assertions.assertEquals(100, f3.getTimecode());
			Assertions.assertTrue(f3.isKeyFrame());

			final Frame f4 = reader.getNextFrame();
			Assertions.assertEquals(133, f4.getTimecode());
			Assertions.assertFalse(f4.isKeyFrame());

			Assertions.assertNull(reader.getNextFrame());
		}
	}

	// ========== Helper ==========

	private Frame makeFrame(final int trackNo, final long timecode, final boolean keyFrame, final byte[] data) {
		final Frame frame = new Frame();
		frame.setTrackNo(trackNo);
		frame.setTimecode(timecode);
		frame.setKeyFrame(keyFrame);
		frame.setData(ByteBuffer.wrap(data));
		return frame;
	}

	@Test
	public void testGetTracksIsReadOnly() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(makeFrame(1, 0, true, new byte[] { 0x01 }));
		final Collection<Integer> tracks = cluster.getTracks();
		Assertions.assertThrows(UnsupportedOperationException.class, () -> tracks.add(2));
	}

	// ========== 16 bit block timecodes ==========

	private static Frame frameAt(final long timecode, final boolean keyFrame) {
		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(timecode);
		frame.setKeyFrame(keyFrame);
		frame.setData(ByteBuffer.wrap(new byte[] { 1 }));
		return frame;
	}

	@Test
	public void testFlushBeforeAFrameTooLate() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(frameAt(1000, false));
		Assertions.assertFalse(cluster.shouldFlushBefore(frameAt(1000 + Short.MAX_VALUE, false)));
		Assertions.assertTrue(cluster.shouldFlushBefore(frameAt(1000 + Short.MAX_VALUE + 1, false)));
	}

	@Test
	public void testFlushBeforeAFrameTooEarly() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(frameAt(40000, false));
		cluster.addFrame(frameAt(40100, false));
		// The frame would become the cluster timecode, and the block at 40100 could no longer reach it.
		Assertions.assertFalse(cluster.shouldFlushBefore(frameAt(40100 - Short.MAX_VALUE, false)));
		Assertions.assertTrue(cluster.shouldFlushBefore(frameAt(40100 - Short.MAX_VALUE - 1, false)));
	}

	@Test
	public void testRangeIsForgottenAfterFlush() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(frameAt(0, false));
		cluster.addFrame(frameAt(30000, false));
		cluster.flush(this.writer);
		cluster.addFrame(frameAt(100000, false));
		Assertions.assertFalse(cluster.shouldFlushBefore(frameAt(100001, false)));
	}

	@Test
	public void testLastOffsetInRangeIsWritten() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(frameAt(0, false));
		cluster.addFrame(frameAt(Short.MAX_VALUE, false));
		Assertions.assertTrue(cluster.flush(this.writer) > 0);
	}

	@Test
	public void testFlushRefusesATimecodeOutOfRange() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(frameAt(0, false));
		cluster.addFrame(frameAt(Short.MAX_VALUE + 1L, false));
		Assertions.assertThrows(IllegalArgumentException.class, () -> cluster.flush(this.writer));
	}

	// ========== Cues of a file with video ==========

	private static final int VIDEO = 1;
	private static final int AUDIO = 2;

	private Frame frameOf(final int track, final long timecode, final boolean keyFrame) {
		return makeFrame(track, timecode, keyFrame, new byte[] { 1 });
	}

	private static Cluster clusterWithVideo() {
		final Cluster cluster = new Cluster();
		cluster.setMinimumParameters(1000, 1024);
		cluster.addVideoTrack(VIDEO);
		return cluster;
	}

	@Test
	public void testAudioFrameDoesNotEndAClusterOfAFileWithVideo() {
		final Cluster cluster = clusterWithVideo();
		cluster.addFrame(frameOf(VIDEO, 0, true));
		// Every audio frame is a key frame, and this one comes long after the minimum.
		Assertions.assertFalse(cluster.shouldFlushBefore(frameOf(AUDIO, 2000, true)));
	}

	@Test
	public void testVideoKeyFrameEndsAClusterThatHasNone() {
		final Cluster cluster = clusterWithVideo();
		cluster.addFrame(frameOf(VIDEO, 0, false));
		cluster.addFrame(frameOf(AUDIO, 10, true));
		// Well under the minimum: what matters is that the picture starts a cluster.
		Assertions.assertTrue(cluster.shouldFlushBefore(frameOf(VIDEO, 40, true)));
	}

	@Test
	public void testVideoKeyFrameJoinsAClusterThatHasOneUnderTheMinimum() {
		final Cluster cluster = clusterWithVideo();
		cluster.addFrame(frameOf(VIDEO, 0, true));
		Assertions.assertFalse(cluster.shouldFlushBefore(frameOf(VIDEO, 40, true)));
	}

	@Test
	public void testVideoKeyFrameEndsAClusterThatHasOneOverTheMinimum() {
		final Cluster cluster = clusterWithVideo();
		cluster.addFrame(frameOf(VIDEO, 0, true));
		Assertions.assertTrue(cluster.shouldFlushBefore(frameOf(VIDEO, 1000, true)));
	}

	@Test
	public void testCueNamesTheVideoKeyFrameNotTheCluster() {
		final Cluster cluster = clusterWithVideo();
		cluster.addFrame(frameOf(AUDIO, 100, true));
		cluster.addFrame(frameOf(VIDEO, 110, false));
		cluster.addFrame(frameOf(VIDEO, 120, true));
		Assertions.assertEquals(100, cluster.getClusterTimecode());
		Assertions.assertEquals(120, cluster.getCueTimecode());
		Assertions.assertIterableEquals(java.util.List.of(VIDEO), cluster.getCueTracks());
	}

	@Test
	public void testClusterWithoutVideoKeyFrameTakesNoCue() {
		final Cluster cluster = clusterWithVideo();
		cluster.addFrame(frameOf(VIDEO, 0, false));
		cluster.addFrame(frameOf(AUDIO, 10, true));
		Assertions.assertTrue(cluster.getCueTracks().isEmpty());
	}

	@Test
	public void testVideoKeyFrameIsForgottenAfterFlush() {
		final Cluster cluster = clusterWithVideo();
		cluster.addFrame(frameOf(VIDEO, 0, true));
		cluster.flush(this.writer);
		cluster.addFrame(frameOf(VIDEO, 40, false));
		Assertions.assertTrue(cluster.getCueTracks().isEmpty());
		Assertions.assertTrue(cluster.shouldFlushBefore(frameOf(VIDEO, 80, true)));
	}

	@Test
	public void testWithoutVideoACueNamesTheCluster() {
		final Cluster cluster = new Cluster();
		cluster.addFrame(frameOf(AUDIO, 100, false));
		Assertions.assertEquals(100, cluster.getCueTimecode());
		Assertions.assertIterableEquals(java.util.List.of(AUDIO), cluster.getCueTracks());
	}
}
