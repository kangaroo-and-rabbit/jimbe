package org.ebml.matroska;

import java.nio.ByteBuffer;

import org.ebml.Element;
import org.ebml.MasterElement;
import org.ebml.ProtoType;
import org.ebml.StringElement;
import org.ebml.UnsignedIntegerElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MatroskaDocTypesTest {

	@Test
	public void testEBMLHeaderId() {
		// Use duplicate() to avoid mutating the internal ByteBuffer of the ProtoType
		final ByteBuffer type = MatroskaDocTypes.EBML.getType().duplicate();
		Assertions.assertEquals(4, type.remaining());
		final byte[] bytes = new byte[4];
		type.get(bytes);
		Assertions.assertEquals((byte) 0x1A, bytes[0]);
		Assertions.assertEquals((byte) 0x45, bytes[1]);
		Assertions.assertEquals((byte) 0xDF, bytes[2]);
		Assertions.assertEquals((byte) 0xA3, bytes[3]);
	}

	@Test
	public void testSegmentId() {
		final ByteBuffer type = MatroskaDocTypes.Segment.getType().duplicate();
		final byte[] bytes = new byte[4];
		type.get(bytes);
		Assertions.assertEquals((byte) 0x18, bytes[0]);
		Assertions.assertEquals((byte) 0x53, bytes[1]);
		Assertions.assertEquals((byte) 0x80, bytes[2]);
		Assertions.assertEquals((byte) 0x67, bytes[3]);
	}

	@Test
	public void testEBMLLevel0() {
		Assertions.assertEquals(0, MatroskaDocTypes.EBML.getLevel());
	}

	@Test
	public void testSegmentLevel0() {
		Assertions.assertEquals(0, MatroskaDocTypes.Segment.getLevel());
	}

	@Test
	public void testTracksLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.Tracks.getLevel());
	}

	@Test
	public void testClusterLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.Cluster.getLevel());
	}

	@Test
	public void testCuesLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.Cues.getLevel());
	}

	@Test
	public void testSeekHeadLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.SeekHead.getLevel());
	}

	@Test
	public void testInfoLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.Info.getLevel());
	}

	@Test
	public void testVoidLevelMinus1() {
		Assertions.assertEquals(-1, MatroskaDocTypes.Void.getLevel());
	}

	@Test
	public void testCRC32LevelMinus1() {
		Assertions.assertEquals(-1, MatroskaDocTypes.CRC_32.getLevel());
	}

	@Test
	public void testGetInstanceReturnsCorrectClass() {
		final Element ebml = MatroskaDocTypes.EBML.getInstance();
		Assertions.assertTrue(ebml instanceof MasterElement);

		final Element docType = MatroskaDocTypes.DocType.getInstance();
		Assertions.assertTrue(docType instanceof StringElement);

		final Element version = MatroskaDocTypes.EBMLVersion.getInstance();
		Assertions.assertTrue(version instanceof UnsignedIntegerElement);
	}

	@Test
	public void testStaticLookupByType() {
		final ByteBuffer ebmlType = ByteBuffer.wrap(new byte[]{(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3});
		final Element result = ProtoType.getInstance(ebmlType);
		Assertions.assertNotNull(result);
		Assertions.assertEquals("EBML", result.getElementType().getName());
	}

	@Test
	public void testSimpleBlockId() {
		final ByteBuffer type = MatroskaDocTypes.SimpleBlock.getType().duplicate();
		Assertions.assertEquals(1, type.remaining());
		Assertions.assertEquals((byte) 0xA3, type.get());
	}

	@Test
	public void testTimecodeScaleId() {
		final ByteBuffer type = MatroskaDocTypes.TimecodeScale.getType().duplicate();
		Assertions.assertEquals(3, type.remaining());
	}

	@Test
	public void testTrackEntryLevel2() {
		Assertions.assertEquals(2, MatroskaDocTypes.TrackEntry.getLevel());
	}

	@Test
	public void testChaptersLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.Chapters.getLevel());
	}

	@Test
	public void testAttachmentsLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.Attachments.getLevel());
	}

	@Test
	public void testTagsLevel1() {
		Assertions.assertEquals(1, MatroskaDocTypes.Tags.getLevel());
	}
}
