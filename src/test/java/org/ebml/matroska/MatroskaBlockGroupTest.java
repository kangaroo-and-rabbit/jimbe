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

	/**
	 * Test the badMP3Headers() method.
	 * The method checks that frames on track 2 have 0x54 as the 4th byte (index 3).
	 * It's a protected method, so we test it via a subclass.
	 */
	/**
	 * badMP3Headers() iterates the frameQueue and checks that track 2 frames
	 * have 0x54 at absolute index 3 in their data ByteBuffer.
	 * Note: this method is unused (commented out in source) and buggy -
	 * it checks absolute byte index 3 of the internal block ByteBuffer
	 * (which is the flags byte), not the 4th byte of the frame payload.
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
}
