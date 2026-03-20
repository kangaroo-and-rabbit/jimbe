package org.atriasoft.jimbe.matroska;

import org.atriasoft.jimbe.matroska.model.*;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.FloatElement;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.ProtoType;
import org.atriasoft.jimbe.ebml.model.SignedIntegerElement;
import org.atriasoft.jimbe.ebml.model.StringElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Extended tests for DocTypes verifying element types, hierarchy,
 * and known element IDs per the Matroska specification.
 * https://www.matroska.org/technical/elements.html
 */
public class DocTypesExtendedTest {

	// ============ EBML Header Elements ============

	@Test
	public void testEBMLVersionIsUnsignedInteger() {
		final Element elem = DocTypes.EBMLVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testEBMLReadVersionIsUnsignedInteger() {
		final Element elem = DocTypes.EBMLReadVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testEBMLMaxIDLengthIsUnsignedInteger() {
		final Element elem = DocTypes.EBMLMaxIDLength.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testEBMLMaxSizeLengthIsUnsignedInteger() {
		final Element elem = DocTypes.EBMLMaxSizeLength.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testDocTypeIsStringElement() {
		final Element elem = DocTypes.DocType.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	@Test
	public void testDocTypeVersionIsUnsignedInteger() {
		final Element elem = DocTypes.DocTypeVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testDocTypeReadVersionIsUnsignedInteger() {
		final Element elem = DocTypes.DocTypeReadVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Segment Structure ============

	@Test
	public void testSegmentIsMasterElement() {
		final Element elem = DocTypes.Segment.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testInfoIsMasterElement() {
		final Element elem = DocTypes.Info.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testTracksIsMasterElement() {
		final Element elem = DocTypes.Tracks.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testClusterIsMasterElement() {
		final Element elem = DocTypes.Cluster.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testCuesIsMasterElement() {
		final Element elem = DocTypes.Cues.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	// ============ Track Elements ============

	@Test
	public void testTrackNumberIsUnsignedInteger() {
		final Element elem = DocTypes.TrackNumber.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testTrackUIDIsUnsignedInteger() {
		final Element elem = DocTypes.TrackUID.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testTrackTypeIsUnsignedInteger() {
		final Element elem = DocTypes.TrackType.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testCodecIDIsStringElement() {
		final Element elem = DocTypes.CodecID.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	@Test
	public void testCodecPrivateIsBinaryElement() {
		final Element elem = DocTypes.CodecPrivate.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testDefaultDurationIsUnsignedInteger() {
		final Element elem = DocTypes.DefaultDuration.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Audio Elements ============

	@Test
	public void testSamplingFrequencyIsFloatElement() {
		final Element elem = DocTypes.SamplingFrequency.getInstance();
		Assertions.assertTrue(elem instanceof FloatElement);
	}

	@Test
	public void testOutputSamplingFrequencyIsFloatElement() {
		final Element elem = DocTypes.OutputSamplingFrequency.getInstance();
		Assertions.assertTrue(elem instanceof FloatElement);
	}

	@Test
	public void testChannelsIsUnsignedInteger() {
		final Element elem = DocTypes.Channels.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testBitDepthIsUnsignedInteger() {
		final Element elem = DocTypes.BitDepth.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Video Elements ============

	@Test
	public void testPixelWidthIsUnsignedInteger() {
		final Element elem = DocTypes.PixelWidth.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testPixelHeightIsUnsignedInteger() {
		final Element elem = DocTypes.PixelHeight.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Cluster Elements ============

	@Test
	public void testTimecodeIsUnsignedInteger() {
		final Element elem = DocTypes.Timecode.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testSimpleBlockIsBinaryElement() {
		final Element elem = DocTypes.SimpleBlock.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testBlockIsBinaryElement() {
		final Element elem = DocTypes.Block.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testBlockGroupIsMasterElement() {
		final Element elem = DocTypes.BlockGroup.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testBlockDurationIsUnsignedInteger() {
		final Element elem = DocTypes.BlockDuration.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testReferenceBlockIsSignedInteger() {
		final Element elem = DocTypes.ReferenceBlock.getInstance();
		Assertions.assertTrue(elem instanceof SignedIntegerElement);
	}

	// ============ Tag Elements ============

	@Test
	public void testTagsIsMasterElement() {
		final Element elem = DocTypes.Tags.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testTagNameIsStringElement() {
		final Element elem = DocTypes.TagName.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	@Test
	public void testTagStringIsStringElement() {
		final Element elem = DocTypes.TagString.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	// ============ Chapter Elements ============

	@Test
	public void testChaptersIsMasterElement() {
		final Element elem = DocTypes.Chapters.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	// ============ Attachment Elements ============

	@Test
	public void testAttachmentsIsMasterElement() {
		final Element elem = DocTypes.Attachments.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	// ============ SeekHead Elements ============

	@Test
	public void testSeekHeadIsMasterElement() {
		final Element elem = DocTypes.SeekHead.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testSeekIsMasterElement() {
		final Element elem = DocTypes.Seek.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testSeekIDIsBinaryElement() {
		final Element elem = DocTypes.SeekID.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testSeekPositionIsUnsignedInteger() {
		final Element elem = DocTypes.SeekPosition.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Level verification for hierarchy ============

	@Test
	public void testTopLevelElementsAtLevel1() {
		// Per Matroska spec, these elements are direct children of Segment (level 1)
		Assertions.assertEquals(1, DocTypes.Info.getLevel());
		Assertions.assertEquals(1, DocTypes.Tracks.getLevel());
		Assertions.assertEquals(1, DocTypes.Cluster.getLevel());
		Assertions.assertEquals(1, DocTypes.Cues.getLevel());
		Assertions.assertEquals(1, DocTypes.SeekHead.getLevel());
		Assertions.assertEquals(1, DocTypes.Tags.getLevel());
		Assertions.assertEquals(1, DocTypes.Chapters.getLevel());
		Assertions.assertEquals(1, DocTypes.Attachments.getLevel());
	}

	@Test
	public void testLevel2Elements() {
		Assertions.assertEquals(2, DocTypes.TrackEntry.getLevel());
		Assertions.assertEquals(2, DocTypes.CuePoint.getLevel());
	}

	@Test
	public void testLevel3TrackElements() {
		Assertions.assertEquals(3, DocTypes.TrackNumber.getLevel());
		Assertions.assertEquals(3, DocTypes.TrackUID.getLevel());
		Assertions.assertEquals(3, DocTypes.TrackType.getLevel());
		Assertions.assertEquals(3, DocTypes.CodecID.getLevel());
	}

	@Test
	public void testVoidAndCRCLevelMinus1() {
		// Void and CRC-32 can appear at any level
		Assertions.assertEquals(-1, DocTypes.Void.getLevel());
		Assertions.assertEquals(-1, DocTypes.CRC_32.getLevel());
	}

	// ============ Static lookup tests ============

	@Test
	public void testStaticLookupReturnsNullForUnknownType() {
		final ByteBuffer unknownType = ByteBuffer.wrap(new byte[]{(byte) 0x1F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
		final Element result = ProtoType.getInstance(unknownType);
		Assertions.assertNull(result);
	}

	@Test
	public void testStaticLookupSegmentById() {
		// Segment ID = 0x18538067
		final ByteBuffer segmentType = ByteBuffer.wrap(new byte[]{(byte) 0x18, (byte) 0x53, (byte) 0x80, (byte) 0x67});
		final Element result = ProtoType.getInstance(segmentType);
		Assertions.assertNotNull(result);
		Assertions.assertEquals("Segment", result.getElementType().getName());
	}

	@Test
	public void testStaticLookupClusterById() {
		// Cluster ID = 0x1F43B675
		final ByteBuffer clusterType = ByteBuffer.wrap(new byte[]{(byte) 0x1F, (byte) 0x43, (byte) 0xB6, (byte) 0x75});
		final Element result = ProtoType.getInstance(clusterType);
		Assertions.assertNotNull(result);
		Assertions.assertEquals("Cluster", result.getElementType().getName());
	}
}
