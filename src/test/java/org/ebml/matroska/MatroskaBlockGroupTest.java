package org.ebml.matroska;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ebml.Element;
import org.ebml.io.FileDataSource;
import org.ebml.io.FileDataWriter;
import org.ebml.matroska.MatroskaFileTrack.MatroskaAudioTrack;
import org.ebml.matroska.MatroskaFileTrack.MatroskaVideoTrack;
import org.ebml.matroska.MatroskaFileTrack.TrackType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the BlockGroup parsing path in MatroskaFile.parseNextCluster()
 * and for the badMP3Headers() validation method.
 *
 * Per the Matroska specification, a BlockGroup contains:
 *   - Block (0xA1): the actual frame data
 *   - BlockDuration (0x9B): optional duration of the block
 *   - ReferenceBlock (0xFB): optional reference to another block (for P/B frames)
 *
 * When a frame has a duration set, MatroskaSimpleBlock writes a BlockGroup
 * instead of a SimpleBlock.
 */
public class MatroskaBlockGroupTest {
	private File destination;
	private FileDataWriter ioDW;

	@BeforeEach
	public void setUp() throws Exception {
		this.destination = File.createTempFile("test-blockgroup", ".mkv");
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

	private void addAudioTrack(final MatroskaFileWriter writer, final int trackNo) {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setTrackNo(trackNo);
		track.setTrackType(TrackType.AUDIO);
		track.setCodecID("A_MPEG/L3");
		track.setDefaultDuration(26000000L);
		final MatroskaAudioTrack audioInfo = new MatroskaAudioTrack();
		audioInfo.setSamplingFrequency(44100.0f);
		audioInfo.setChannels((short) 2);
		audioInfo.setBitDepth(16);
		track.setAudio(audioInfo);
		writer.addTrack(track);
	}

	// ========== BlockGroup tests ==========

	@Test
	public void testBlockGroupWithDuration() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		// Setting duration triggers BlockGroup instead of SimpleBlock
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setDuration(33);
		frame.setData(ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03, 0x04}));
		writer.addFrame(frame);

		writer.close();

		final MatroskaFile reader = openReader();
		final MatroskaFileFrame readFrame = reader.getNextFrame();
		Assertions.assertNotNull(readFrame);
		Assertions.assertEquals(1, readFrame.getTrackNo());
		Assertions.assertEquals(33, readFrame.getDuration());
		Assertions.assertEquals(4, readFrame.getData().remaining());
	}

	@Test
	public void testBlockGroupFrameData() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		final byte[] payload = {0x0A, 0x0B, 0x0C, 0x0D, 0x0E};
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setDuration(100);
		frame.setData(ByteBuffer.wrap(payload));
		writer.addFrame(frame);

		writer.close();

		final MatroskaFile reader = openReader();
		final MatroskaFileFrame readFrame = reader.getNextFrame();
		Assertions.assertNotNull(readFrame);

		// Verify data content
		final ByteBuffer data = readFrame.getData();
		final byte[] readBytes = new byte[data.remaining()];
		data.get(readBytes);
		Assertions.assertArrayEquals(payload, readBytes);
	}

	@Test
	public void testBlockGroupTimecode() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTrackNo(1);
		frame.setTimecode(500);
		frame.setDuration(50);
		frame.setData(ByteBuffer.wrap(new byte[]{0x01}));
		writer.addFrame(frame);

		writer.close();

		final MatroskaFile reader = openReader();
		final MatroskaFileFrame readFrame = reader.getNextFrame();
		Assertions.assertNotNull(readFrame);
		Assertions.assertEquals(500, readFrame.getTimecode());
	}

	@Test
	public void testMultipleBlockGroupFrames() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		for (int i = 0; i < 3; i++) {
			final MatroskaFileFrame frame = new MatroskaFileFrame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 33);
			frame.setDuration(33);
			frame.setData(ByteBuffer.wrap(new byte[]{(byte) i}));
			writer.addFrame(frame);
		}

		writer.close();

		final MatroskaFile reader = openReader();
		for (int i = 0; i < 3; i++) {
			final MatroskaFileFrame readFrame = reader.getNextFrame();
			Assertions.assertNotNull(readFrame, "Frame " + i + " should not be null");
			Assertions.assertEquals(i * 33, readFrame.getTimecode());
			Assertions.assertEquals(33, readFrame.getDuration());
		}
	}

	@Test
	public void testMixedSimpleBlockAndBlockGroup() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		// Frame without duration -> SimpleBlock
		final MatroskaFileFrame simpleFrame = new MatroskaFileFrame();
		simpleFrame.setTrackNo(1);
		simpleFrame.setTimecode(0);
		simpleFrame.setData(ByteBuffer.wrap(new byte[]{0x01}));
		writer.addFrame(simpleFrame);

		// Frame with duration -> BlockGroup
		final MatroskaFileFrame blockGroupFrame = new MatroskaFileFrame();
		blockGroupFrame.setTrackNo(1);
		blockGroupFrame.setTimecode(33);
		blockGroupFrame.setDuration(33);
		blockGroupFrame.setData(ByteBuffer.wrap(new byte[]{0x02}));
		writer.addFrame(blockGroupFrame);

		writer.close();

		final MatroskaFile reader = openReader();
		final MatroskaFileFrame readSimple = reader.getNextFrame();
		Assertions.assertNotNull(readSimple);
		Assertions.assertEquals(0, readSimple.getTimecode());
		// SimpleBlock doesn't carry duration
		Assertions.assertEquals(0, readSimple.getDuration());

		final MatroskaFileFrame readBlockGroup = reader.getNextFrame();
		Assertions.assertNotNull(readBlockGroup);
		Assertions.assertEquals(33, readBlockGroup.getTimecode());
		Assertions.assertEquals(33, readBlockGroup.getDuration());
	}

	// ========== BlockGroup with ReferenceBlock (raw EBML injection) ==========

	/**
	 * Write a raw EBML element: type ID bytes + EBML-coded size + data.
	 */
	private void writeRawElement(final ByteArrayOutputStream out, final byte[] typeId, final byte[] data) {
		out.writeBytes(typeId);
		out.writeBytes(Element.makeEbmlCodedSize(data.length));
		out.writeBytes(data);
	}

	/**
	 * Encode a signed integer in big-endian.
	 */
	private byte[] encodeSignedInt(final long value) {
		final int numBytes = Element.getMinByteSize(value);
		final byte[] result = new byte[Math.max(numBytes, 1)];
		for (int i = result.length - 1; i >= 0; i--) {
			result[i] = (byte) (value >>> ((result.length - 1 - i) * 8));
		}
		return result;
	}

	/**
	 * Encode an unsigned integer in big-endian minimal bytes.
	 */
	private byte[] encodeUnsignedInt(final long value) {
		if (value == 0) {
			return new byte[]{0};
		}
		final int numBytes = (int) Element.getMinByteSizeUnsigned(value);
		final byte[] result = new byte[numBytes];
		for (int i = numBytes - 1; i >= 0; i--) {
			result[i] = (byte) (value >>> ((numBytes - 1 - i) * 8));
		}
		return result;
	}

	/**
	 * Build a Block element inner data for a single frame.
	 * Format: EBML-coded track number + 2-byte timecode + 1-byte flags + payload
	 */
	private byte[] buildBlockInnerData(final int trackNo, final short timecode,
			final byte flags, final byte[] payload) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		// EBML-coded track number
		out.writeBytes(Element.makeEbmlCodedSize(trackNo));
		// 2-byte timecode (big-endian)
		out.write((timecode >> 8) & 0xFF);
		out.write(timecode & 0xFF);
		// flags byte (no lacing for single frame)
		out.write(flags);
		// payload
		out.writeBytes(payload);
		return out.toByteArray();
	}

	/**
	 * Build a BlockGroup element with Block + BlockDuration + ReferenceBlock.
	 */
	private byte[] buildBlockGroupWithReference(final int trackNo, final short timecode,
			final byte[] payload, final long duration, final long reference) {
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();

		// Block (0xA1)
		final byte[] blockData = buildBlockInnerData(trackNo, timecode, (byte) 0x00, payload);
		writeRawElement(inner, new byte[]{(byte) 0xA1}, blockData);

		// BlockDuration (0x9B)
		writeRawElement(inner, new byte[]{(byte) 0x9B}, encodeUnsignedInt(duration));

		// ReferenceBlock (0xFB) - signed integer
		writeRawElement(inner, new byte[]{(byte) 0xFB}, encodeSignedInt(reference));

		// Wrap in BlockGroup (0xA0)
		final ByteArrayOutputStream bg = new ByteArrayOutputStream();
		writeRawElement(bg, new byte[]{(byte) 0xA0}, inner.toByteArray());
		return bg.toByteArray();
	}

	/**
	 * Build a Cluster element: Timecode + child elements.
	 */
	private byte[] buildCluster(final long clusterTimecode, final byte[]... children) {
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();
		// Timecode (0xE7)
		writeRawElement(inner, new byte[]{(byte) 0xE7}, encodeUnsignedInt(clusterTimecode));
		for (final byte[] child : children) {
			inner.writeBytes(child);
		}
		final ByteArrayOutputStream cluster = new ByteArrayOutputStream();
		// Cluster (0x1F43B675)
		writeRawElement(cluster,
				new byte[]{(byte) 0x1F, (byte) 0x43, (byte) 0xB6, (byte) 0x75},
				inner.toByteArray());
		return cluster.toByteArray();
	}

	/**
	 * Find the last occurrence of the Cluster ID (0x1F43B675) in the file bytes.
	 */
	private int findClusterPosition(final byte[] fileBytes) {
		int lastPos = -1;
		for (int i = 0; i < fileBytes.length - 3; i++) {
			if (fileBytes[i] == (byte) 0x1F && fileBytes[i + 1] == (byte) 0x43
					&& fileBytes[i + 2] == (byte) 0xB6 && fileBytes[i + 3] == (byte) 0x75) {
				lastPos = i;
			}
		}
		return lastPos;
	}

	/**
	 * Replace the Cluster and everything after it with injected bytes.
	 */
	private void replaceCluster(final byte[] injectedBytes) throws IOException {
		final byte[] fileBytes;
		try (final RandomAccessFile raf = new RandomAccessFile(this.destination, "r")) {
			fileBytes = new byte[(int) raf.length()];
			raf.readFully(fileBytes);
		}

		final int clusterPos = findClusterPosition(fileBytes);
		Assertions.assertTrue(clusterPos > 0, "Could not find Cluster element in file");

		try (final RandomAccessFile raf = new RandomAccessFile(this.destination, "rw");
			 final FileChannel ch = raf.getChannel()) {
			ch.position(0);
			ch.write(ByteBuffer.wrap(fileBytes, 0, clusterPos));
			ch.write(ByteBuffer.wrap(injectedBytes));
			ch.truncate(ch.position());
		}
	}

	/**
	 * Write a minimal .mkv base file with a video track 1.
	 */
	private void writeBaseMkv() throws IOException {
		final FileDataWriter writer = new FileDataWriter(this.destination.getPath());
		final MatroskaFileWriter mkvWriter = new MatroskaFileWriter(writer);
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		mkvWriter.addTrack(track);

		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap("hello".getBytes()));
		mkvWriter.addFrame(frame);
		mkvWriter.close();
		writer.close();
	}

	@Test
	public void testBlockGroupWithReferenceBlock() throws IOException {
		writeBaseMkv();

		// Build a cluster containing a BlockGroup with ReferenceBlock
		final byte[] payload = {0x10, 0x20, 0x30};
		final byte[] cluster = buildCluster(0,
				buildBlockGroupWithReference(1, (short) 0, payload, 50, -33));

		replaceCluster(cluster);

		// Use default scanFirstCluster=true so the injected cluster is parsed
		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final MatroskaFile reader = new MatroskaFile(inputDataSource);
		reader.readFile();

		final MatroskaFileFrame readFrame = reader.getNextFrame();
		Assertions.assertNotNull(readFrame);
		Assertions.assertEquals(1, readFrame.getTrackNo());
		Assertions.assertEquals(50, readFrame.getDuration());
		// ReferenceBlock is parsed and stored via addReferences
		Assertions.assertFalse(readFrame.getReferences().isEmpty(),
				"Frame should have at least one reference");
		Assertions.assertEquals(-33L, readFrame.getReferences().get(0).longValue());
	}

	// ========== Multi-frame laced SimpleBlock ==========

	/**
	 * Build a SimpleBlock element with fixed-size lacing for multiple frames.
	 * Fixed lacing is simpler and avoids the library's EBML/Xiph lacing bugs.
	 *
	 * SimpleBlock format:
	 *   EBML-coded track number, 2-byte timecode, 1-byte flags, [lacing data], frame data
	 *
	 * Fixed lacing flags: bit 5 set (0x04 in the lace flag bits).
	 * Flags byte bits: [keyframe:1][reserved:3][invisible:1][lacing:2][discardable:1]
	 * Fixed lacing = bits 5 set, 6 clear -> lace flag = 0x04
	 * But in the flags byte, lace bits are at positions 1-2 (counting from bit 0 as MSB of the nibble).
	 * Actually, from MatroskaBlock.parseBlock(): laceFlag = flagsByte & 0x06
	 *   0x02 = Xiph, 0x04 = Fixed, 0x06 = EBML
	 * So for fixed lacing: flags byte = 0x04 (lace bits) | 0x80 (keyframe) = 0x84
	 */
	private byte[] buildFixedLacedSimpleBlock(final int trackNo, final short timecode,
			final byte[][] frames) {
		final int frameSize = frames[0].length;
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		// EBML-coded track number
		out.writeBytes(Element.makeEbmlCodedSize(trackNo));
		// 2-byte timecode
		out.write((timecode >> 8) & 0xFF);
		out.write(timecode & 0xFF);
		// flags: keyframe (0x80) + fixed lacing (0x04) = 0x84
		out.write(0x84);
		// lace count (number of frames minus 1)
		out.write(frames.length - 1);
		// frame data (all same size for fixed lacing)
		for (final byte[] frame : frames) {
			out.writeBytes(frame);
		}
		return out.toByteArray();
	}

	/**
	 * Build a SimpleBlock element wrapping raw inner data.
	 */
	private byte[] buildSimpleBlockElement(final byte[] innerData) {
		final ByteArrayOutputStream sb = new ByteArrayOutputStream();
		// SimpleBlock (0xA3)
		writeRawElement(sb, new byte[]{(byte) 0xA3}, innerData);
		return sb.toByteArray();
	}

	/**
	 * Test that a fixed-laced SimpleBlock with multiple frames is parsed
	 * correctly by parseNextCluster(). This covers the getFrameCount() > 1
	 * path in the SimpleBlock branch.
	 */
	@Test
	public void testLacedSimpleBlockMultipleFrames() throws IOException {
		writeBaseMkv();

		// Build a fixed-laced SimpleBlock with 3 frames of 2 bytes each
		final byte[][] frameData = {
			{0x10, 0x20},
			{0x30, 0x40},
			{0x50, 0x60}
		};
		final byte[] simpleBlock = buildSimpleBlockElement(
				buildFixedLacedSimpleBlock(1, (short) 0, frameData));
		final byte[] cluster = buildCluster(0, simpleBlock);

		replaceCluster(cluster);

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final MatroskaFile reader = new MatroskaFile(inputDataSource);
		reader.readFile();

		// First frame
		final MatroskaFileFrame firstFrame = reader.getNextFrame();
		Assertions.assertNotNull(firstFrame, "First laced frame should not be null");
		Assertions.assertEquals(0, firstFrame.getTimecode());
		Assertions.assertEquals(1, firstFrame.getTrackNo());
		Assertions.assertEquals(2, firstFrame.getData().remaining(),
				"First frame data should be 2 bytes");

		// Second frame (covers the getFrameCount() > 1 loop)
		final MatroskaFileFrame secondFrame = reader.getNextFrame();
		Assertions.assertNotNull(secondFrame, "Second laced frame should not be null");
		Assertions.assertEquals(0, secondFrame.getTimecode());
		Assertions.assertEquals(1, secondFrame.getTrackNo());
		Assertions.assertEquals(2, secondFrame.getData().remaining(),
				"Second frame data should be 2 bytes");

		// Third frame
		final MatroskaFileFrame thirdFrame = reader.getNextFrame();
		Assertions.assertNotNull(thirdFrame, "Third laced frame should not be null");
		Assertions.assertEquals(2, thirdFrame.getData().remaining(),
				"Third frame data should be 2 bytes");
	}

	// ========== Multi-frame laced BlockGroup ==========

	/**
	 * Build a BlockGroup with a fixed-laced Block + BlockDuration.
	 * This is a rare case (lacing inside a BlockGroup), but parseNextCluster
	 * supports it via the block.getFrameCount() > 1 loop in the BlockGroup
	 * branch (lines ~491-505 in MatroskaFile.java).
	 */
	private byte[] buildLacedBlockGroup(final int trackNo, final short timecode,
			final byte[][] frames, final long duration) {
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();

		// Block (0xA1) with fixed-size lacing
		final byte[] blockInner = buildFixedLacedBlock(trackNo, timecode, frames);
		writeRawElement(inner, new byte[]{(byte) 0xA1}, blockInner);

		// BlockDuration (0x9B)
		writeRawElement(inner, new byte[]{(byte) 0x9B}, encodeUnsignedInt(duration));

		// Wrap in BlockGroup (0xA0)
		final ByteArrayOutputStream bg = new ByteArrayOutputStream();
		writeRawElement(bg, new byte[]{(byte) 0xA0}, inner.toByteArray());
		return bg.toByteArray();
	}

	/**
	 * Build Block inner data with fixed-size lacing (no keyframe flag).
	 */
	private byte[] buildFixedLacedBlock(final int trackNo, final short timecode,
			final byte[][] frames) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(Element.makeEbmlCodedSize(trackNo));
		out.write((timecode >> 8) & 0xFF);
		out.write(timecode & 0xFF);
		// flags: no keyframe + fixed lacing (0x04)
		out.write(0x04);
		// lace count
		out.write(frames.length - 1);
		for (final byte[] frame : frames) {
			out.writeBytes(frame);
		}
		return out.toByteArray();
	}

	/**
	 * Test that a BlockGroup containing a fixed-laced Block with multiple
	 * frames is parsed correctly. This covers the getFrameCount() > 1 path
	 * in the BlockGroup branch of parseNextCluster().
	 *
	 * The commented-out badMP3Headers() calls in the source (lines ~496-503)
	 * were debug checks from the original author, left commented out rather
	 * than deleted. They would have validated MP3 frame headers on track 2
	 * after each laced frame was added to the queue.
	 */
	@Test
	public void testLacedBlockGroupMultipleFrames() throws IOException {
		writeBaseMkv();

		final byte[][] frameData = {
			{0x11, 0x22, 0x33},
			{0x44, 0x55, 0x66},
			{0x77, (byte) 0x88, (byte) 0x99}
		};
		final byte[] blockGroup = buildLacedBlockGroup(1, (short) 0, frameData, 100);
		final byte[] cluster = buildCluster(0, blockGroup);

		replaceCluster(cluster);

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final MatroskaFile reader = new MatroskaFile(inputDataSource);
		reader.readFile();

		// First frame from laced BlockGroup
		final MatroskaFileFrame firstFrame = reader.getNextFrame();
		Assertions.assertNotNull(firstFrame, "First laced BlockGroup frame should not be null");
		Assertions.assertEquals(0, firstFrame.getTimecode());
		Assertions.assertEquals(1, firstFrame.getTrackNo());
		Assertions.assertEquals(100, firstFrame.getDuration());
		Assertions.assertEquals(3, firstFrame.getData().remaining());

		// Second frame (covers the getFrameCount() > 1 loop in BlockGroup branch)
		final MatroskaFileFrame secondFrame = reader.getNextFrame();
		Assertions.assertNotNull(secondFrame, "Second laced BlockGroup frame should not be null");
		Assertions.assertEquals(100, secondFrame.getDuration());
		Assertions.assertEquals(3, secondFrame.getData().remaining());

		// Third frame
		final MatroskaFileFrame thirdFrame = reader.getNextFrame();
		Assertions.assertNotNull(thirdFrame, "Third laced BlockGroup frame should not be null");
		Assertions.assertEquals(3, thirdFrame.getData().remaining());
	}

	/**
	 * Test that a BlockGroup with no Block child throws NullPointerException.
	 * This covers the guard at line 478-481 in parseNextCluster().
	 */
	@Test
	public void testBlockGroupWithNoBlockThrows() throws IOException {
		writeBaseMkv();

		// Build a BlockGroup containing only a BlockDuration (no Block child)
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();
		// BlockDuration (0x9B) with value 100
		writeRawElement(inner, new byte[]{(byte) 0x9B}, encodeUnsignedInt(100));
		// Wrap in BlockGroup (0xA0)
		final ByteArrayOutputStream bg = new ByteArrayOutputStream();
		writeRawElement(bg, new byte[]{(byte) 0xA0}, inner.toByteArray());

		final byte[] cluster = buildCluster(0, bg.toByteArray());
		replaceCluster(cluster);

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final MatroskaFile reader = new MatroskaFile(inputDataSource);

		// readFile() with scanFirstCluster=true parses the first cluster eagerly,
		// so the exception is thrown during readFile(), not getNextFrame().
		Assertions.assertThrows(NullPointerException.class, () -> reader.readFile());
	}

	// ========== Multi-cluster ==========

	@Test
	public void testMultiClusterParsing() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		// Write frames spanning more than 5000ms to trigger cluster flush.
		// The cluster limit is 5000ms duration.
		for (int i = 0; i < 10; i++) {
			final MatroskaFileFrame frame = new MatroskaFileFrame();
			frame.setTrackNo(1);
			frame.setTimecode(i * 1000L);
			frame.setData(ByteBuffer.wrap(new byte[]{(byte) i}));
			writer.addFrame(frame);
		}

		writer.close();

		final MatroskaFile reader = openReader();
		int count = 0;
		MatroskaFileFrame readFrame = reader.getNextFrame();
		while (readFrame != null) {
			Assertions.assertEquals(count * 1000L, readFrame.getTimecode());
			count++;
			readFrame = reader.getNextFrame();
		}
		Assertions.assertEquals(10, count, "Should have read 10 frames across multiple clusters");
	}

	// ========== Multi-track ==========

	@Test
	public void testMultiTrackFrameParsing() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);
		addAudioTrack(writer, 2);

		// Write interleaved frames on track 1 and track 2
		for (int i = 0; i < 3; i++) {
			final MatroskaFileFrame videoFrame = new MatroskaFileFrame();
			videoFrame.setTrackNo(1);
			videoFrame.setTimecode(i * 33L);
			videoFrame.setData(ByteBuffer.wrap(new byte[]{(byte) (0xA0 + i)}));
			writer.addFrame(videoFrame);

			final MatroskaFileFrame audioFrame = new MatroskaFileFrame();
			audioFrame.setTrackNo(2);
			audioFrame.setTimecode(i * 33L);
			audioFrame.setData(ByteBuffer.wrap(new byte[]{(byte) (0xB0 + i)}));
			writer.addFrame(audioFrame);
		}

		writer.close();

		final MatroskaFile reader = openReader();
		int videoCount = 0;
		int audioCount = 0;
		MatroskaFileFrame readFrame = reader.getNextFrame();
		while (readFrame != null) {
			if (readFrame.getTrackNo() == 1) {
				videoCount++;
			} else if (readFrame.getTrackNo() == 2) {
				audioCount++;
			}
			readFrame = reader.getNextFrame();
		}
		Assertions.assertEquals(3, videoCount, "Should have 3 video frames");
		Assertions.assertEquals(3, audioCount, "Should have 3 audio frames");
	}

	@Test
	public void testGetNextFrameByTrackNo() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);
		addAudioTrack(writer, 2);

		for (int i = 0; i < 3; i++) {
			final MatroskaFileFrame videoFrame = new MatroskaFileFrame();
			videoFrame.setTrackNo(1);
			videoFrame.setTimecode(i * 33L);
			videoFrame.setData(ByteBuffer.wrap(new byte[]{(byte) i}));
			writer.addFrame(videoFrame);

			final MatroskaFileFrame audioFrame = new MatroskaFileFrame();
			audioFrame.setTrackNo(2);
			audioFrame.setTimecode(i * 33L);
			audioFrame.setData(ByteBuffer.wrap(new byte[]{(byte) (0x10 + i)}));
			writer.addFrame(audioFrame);
		}

		writer.close();

		final MatroskaFile reader = openReader();
		// Read only track 2 (audio) frames using getNextFrame(trackNo)
		for (int i = 0; i < 3; i++) {
			final MatroskaFileFrame audioFrame = reader.getNextFrame(2);
			Assertions.assertNotNull(audioFrame, "Audio frame " + i + " should not be null");
			Assertions.assertEquals(2, audioFrame.getTrackNo());
		}
	}

	// ========== badMP3Headers tests ==========

	/**
	 * badMP3Headers() iterates the frameQueue and checks that track 2 frames
	 * have 0x54 at absolute index 3 in their data ByteBuffer.
	 * Note: this method is buggy - it checks absolute byte index 3 of the
	 * internal block ByteBuffer (which is the flags byte), not the 4th byte
	 * of the frame payload. It is also unused (commented out in source).
	 *
	 * We test it with only track 1 frames to verify it returns false
	 * when no track 2 data is present.
	 */
	@Test
	public void testBadMP3HeadersNoTrack2() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03, 0x04}));
		writer.addFrame(frame);

		writer.close();

		// No track 2 frames in queue -> badMP3Headers returns false
		final MatroskaFile reader = openReader();
		Assertions.assertFalse(reader.badMP3Headers());
	}

	@Test
	public void testBadMP3HeadersEmptyQueue() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);

		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap(new byte[]{0x01, 0x02}));
		writer.addFrame(frame);

		writer.close();

		final MatroskaFile reader = openReader();
		// Drain the queue
		while (reader.getNextFrame() != null) {
			// consume all frames
		}
		// Empty queue -> returns false
		Assertions.assertFalse(reader.badMP3Headers());
	}

	/**
	 * Test that badMP3Headers throws RuntimeException when track 2 frames
	 * are present in the queue.
	 *
	 * The method checks frame.getData().get(3) which reads absolute index 3
	 * of the ByteBuffer (the flags byte in the block header), not the payload.
	 * Since the flags byte is unlikely to be 0x54, a RuntimeException is thrown.
	 */
	@Test
	public void testBadMP3HeadersThrowsWithTrack2() throws IOException {
		final MatroskaFileWriter writer = new MatroskaFileWriter(this.ioDW);
		addVideoTrack(writer);
		addAudioTrack(writer, 2);

		// Write a video frame first (track 1)
		final MatroskaFileFrame videoFrame = new MatroskaFileFrame();
		videoFrame.setTrackNo(1);
		videoFrame.setTimecode(0);
		videoFrame.setData(ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03, 0x04}));
		writer.addFrame(videoFrame);

		// Write an audio frame on track 2 with enough data
		final MatroskaFileFrame audioFrame = new MatroskaFileFrame();
		audioFrame.setTrackNo(2);
		audioFrame.setTimecode(0);
		audioFrame.setData(ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}));
		writer.addFrame(audioFrame);

		writer.close();

		final MatroskaFile reader = openReader();
		// frameQueue has frames from the first cluster. Track 2 frame is present.
		// badMP3Headers checks getData().get(3) which is the flags byte, not 0x54
		Assertions.assertThrows(RuntimeException.class, () -> reader.badMP3Headers());
	}
}
