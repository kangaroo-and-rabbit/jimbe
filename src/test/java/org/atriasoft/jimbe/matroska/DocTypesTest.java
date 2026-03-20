package org.atriasoft.jimbe.matroska;

import org.atriasoft.jimbe.matroska.model.*;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.ProtoType;
import org.atriasoft.jimbe.ebml.model.StringElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DocTypesTest {

	@Test
	public void testEBMLHeaderId() {
		// Use duplicate() to avoid mutating the internal ByteBuffer of the ProtoType
		final ByteBuffer type = DocTypes.EBML.getType().duplicate();
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
		final ByteBuffer type = DocTypes.Segment.getType().duplicate();
		final byte[] bytes = new byte[4];
		type.get(bytes);
		Assertions.assertEquals((byte) 0x18, bytes[0]);
		Assertions.assertEquals((byte) 0x53, bytes[1]);
		Assertions.assertEquals((byte) 0x80, bytes[2]);
		Assertions.assertEquals((byte) 0x67, bytes[3]);
	}

	@Test
	public void testEBMLLevel0() {
		Assertions.assertEquals(0, DocTypes.EBML.getLevel());
	}

	@Test
	public void testSegmentLevel0() {
		Assertions.assertEquals(0, DocTypes.Segment.getLevel());
	}

	@Test
	public void testTracksLevel1() {
		Assertions.assertEquals(1, DocTypes.Tracks.getLevel());
	}

	@Test
	public void testClusterLevel1() {
		Assertions.assertEquals(1, DocTypes.Cluster.getLevel());
	}

	@Test
	public void testCuesLevel1() {
		Assertions.assertEquals(1, DocTypes.Cues.getLevel());
	}

	@Test
	public void testSeekHeadLevel1() {
		Assertions.assertEquals(1, DocTypes.SeekHead.getLevel());
	}

	@Test
	public void testInfoLevel1() {
		Assertions.assertEquals(1, DocTypes.Info.getLevel());
	}

	@Test
	public void testVoidLevelMinus1() {
		Assertions.assertEquals(-1, DocTypes.Void.getLevel());
	}

	@Test
	public void testCRC32LevelMinus1() {
		Assertions.assertEquals(-1, DocTypes.CRC_32.getLevel());
	}

	@Test
	public void testGetInstanceReturnsCorrectClass() {
		final Element ebml = DocTypes.EBML.getInstance();
		Assertions.assertTrue(ebml instanceof MasterElement);

		final Element docType = DocTypes.DocType.getInstance();
		Assertions.assertTrue(docType instanceof StringElement);

		final Element version = DocTypes.EBMLVersion.getInstance();
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
		final ByteBuffer type = DocTypes.SimpleBlock.getType().duplicate();
		Assertions.assertEquals(1, type.remaining());
		Assertions.assertEquals((byte) 0xA3, type.get());
	}

	@Test
	public void testTimecodeScaleId() {
		final ByteBuffer type = DocTypes.TimecodeScale.getType().duplicate();
		Assertions.assertEquals(3, type.remaining());
	}

	@Test
	public void testTrackEntryLevel2() {
		Assertions.assertEquals(2, DocTypes.TrackEntry.getLevel());
	}

	@Test
	public void testChaptersLevel1() {
		Assertions.assertEquals(1, DocTypes.Chapters.getLevel());
	}

	@Test
	public void testAttachmentsLevel1() {
		Assertions.assertEquals(1, DocTypes.Attachments.getLevel());
	}

	@Test
	public void testTagsLevel1() {
		Assertions.assertEquals(1, DocTypes.Tags.getLevel());
	}
}
