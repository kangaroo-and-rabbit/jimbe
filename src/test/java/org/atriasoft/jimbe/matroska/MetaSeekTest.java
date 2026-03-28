package org.atriasoft.jimbe.matroska;

import java.io.File;
import java.nio.ByteBuffer;

import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.model.DocTypes;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.MetaSeek;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.TrackType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MetaSeek (SeekHead element).
 */
public class MetaSeekTest {

	private File tempFile;
	private FileDataWriter writer;

	@BeforeEach
	public void setUp() throws Exception {
		this.tempFile = File.createTempFile("metaseek-test-", ".bin");
		this.writer = new FileDataWriter(this.tempFile.getPath());
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.writer.close();
		this.tempFile.delete();
	}

	@Test
	public void testWriteReserves256Bytes() {
		final MetaSeek metaSeek = new MetaSeek(0);
		final long bytesWritten = metaSeek.write(this.writer);
		Assertions.assertEquals(256, bytesWritten);
	}

	@Test
	public void testWritePositionIsConsistent() {
		final long startPos = this.writer.getFilePointer();
		final MetaSeek metaSeek = new MetaSeek(startPos);
		metaSeek.write(this.writer);
		Assertions.assertEquals(startPos + 256, this.writer.getFilePointer());
	}

	@Test
	public void testAddIndexedElementByType() {
		final MetaSeek metaSeek = new MetaSeek(0);
		// Adding by raw type bytes should not throw
		metaSeek.addIndexedElement(DocTypes.Info.getType(), 256);
		metaSeek.addIndexedElement(DocTypes.Tracks.getType(), 512);
		metaSeek.addIndexedElement(DocTypes.Cluster.getType(), 1024);
	}

	@Test
	public void testAddIndexedElementByElement() {
		final MetaSeek metaSeek = new MetaSeek(0);
		// Adding by Element instance should not throw
		metaSeek.addIndexedElement(DocTypes.Cues.getInstance(), 2048);
	}

	@Test
	public void testUpdateRewritesInPlace() {
		final MetaSeek metaSeek = new MetaSeek(0);
		metaSeek.write(this.writer);

		// Write some data after the seek head
		this.writer.write(ByteBuffer.wrap(new byte[100]));
		final long afterData = this.writer.getFilePointer();

		// Add an entry and update
		metaSeek.addIndexedElement(DocTypes.Info.getType(), 256);
		metaSeek.update(this.writer);

		// File pointer should be restored to where it was before update
		Assertions.assertEquals(afterData, this.writer.getFilePointer());
	}

	@Test
	public void testMetaSeekInFullFile() throws Exception {
		// Verify that a full file written with Writer has a seekable SeekHead
		final Writer muxer = new Writer(this.writer);
		muxer.setTimecodeScale(1000000);
		muxer.setDuration(1000);
		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		muxer.addTrack(track);

		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setKeyFrame(true);
		frame.setData(ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 }));
		muxer.addFrame(frame);
		muxer.close();
		this.writer.close();

		// Re-read: traverse the EBML structure and verify SeekHead exists
		try (FileDataSource source = new FileDataSource(this.tempFile.getPath())) {
			final org.atriasoft.jimbe.ebml.Reader ebmlReader = new org.atriasoft.jimbe.ebml.Reader(source);
			// First element should be EBML header
			final org.atriasoft.jimbe.ebml.model.Element ebmlHeader = ebmlReader.readNextElement();
			Assertions.assertNotNull(ebmlHeader);
			Assertions.assertTrue(ebmlHeader.isType(DocTypes.EBML.getType()));
			ebmlHeader.skipData(source);

			// Second element should be Segment
			final org.atriasoft.jimbe.ebml.model.Element segment = ebmlReader.readNextElement();
			Assertions.assertNotNull(segment);
			Assertions.assertTrue(segment.isType(DocTypes.Segment.getType()));

			// First child of Segment should be SeekHead
			final org.atriasoft.jimbe.ebml.model.Element seekHead =
					((org.atriasoft.jimbe.ebml.model.MasterElement) segment).readNextChild(ebmlReader);
			Assertions.assertNotNull(seekHead);
			Assertions.assertTrue(seekHead.isType(DocTypes.SeekHead.getType()));
		}
	}
}
