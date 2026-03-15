package org.atriasoft.ebml2.matroska;

import java.nio.ByteBuffer;

import org.atriasoft.ebml2.BinaryElement;
import org.atriasoft.ebml2.Element;
import org.atriasoft.ebml2.FloatElement;
import org.atriasoft.ebml2.MasterElement;
import org.atriasoft.ebml2.ProtoType;
import org.atriasoft.ebml2.SignedIntegerElement;
import org.atriasoft.ebml2.StringElement;
import org.atriasoft.ebml2.UnsignedIntegerElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Extended tests for MatroskaDocTypes verifying element types, hierarchy,
 * and known element IDs per the Matroska specification.
 * https://www.matroska.org/technical/elements.html
 */
public class MatroskaDocTypesExtendedTest {

	// ============ EBML Header Elements ============

	@Test
	public void testEBMLVersionIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.EBMLVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testEBMLReadVersionIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.EBMLReadVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testEBMLMaxIDLengthIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.EBMLMaxIDLength.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testEBMLMaxSizeLengthIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.EBMLMaxSizeLength.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testDocTypeIsStringElement() {
		final Element elem = MatroskaDocTypes.DocType.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	@Test
	public void testDocTypeVersionIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.DocTypeVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testDocTypeReadVersionIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.DocTypeReadVersion.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Segment Structure ============

	@Test
	public void testSegmentIsMasterElement() {
		final Element elem = MatroskaDocTypes.Segment.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testInfoIsMasterElement() {
		final Element elem = MatroskaDocTypes.Info.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testTracksIsMasterElement() {
		final Element elem = MatroskaDocTypes.Tracks.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testClusterIsMasterElement() {
		final Element elem = MatroskaDocTypes.Cluster.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testCuesIsMasterElement() {
		final Element elem = MatroskaDocTypes.Cues.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	// ============ Track Elements ============

	@Test
	public void testTrackNumberIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.TrackNumber.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testTrackUIDIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.TrackUID.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testTrackTypeIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.TrackType.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testCodecIDIsStringElement() {
		final Element elem = MatroskaDocTypes.CodecID.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	@Test
	public void testCodecPrivateIsBinaryElement() {
		final Element elem = MatroskaDocTypes.CodecPrivate.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testDefaultDurationIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.DefaultDuration.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Audio Elements ============

	@Test
	public void testSamplingFrequencyIsFloatElement() {
		final Element elem = MatroskaDocTypes.SamplingFrequency.getInstance();
		Assertions.assertTrue(elem instanceof FloatElement);
	}

	@Test
	public void testOutputSamplingFrequencyIsFloatElement() {
		final Element elem = MatroskaDocTypes.OutputSamplingFrequency.getInstance();
		Assertions.assertTrue(elem instanceof FloatElement);
	}

	@Test
	public void testChannelsIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.Channels.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testBitDepthIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.BitDepth.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Video Elements ============

	@Test
	public void testPixelWidthIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.PixelWidth.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testPixelHeightIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.PixelHeight.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Cluster Elements ============

	@Test
	public void testTimecodeIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.Timecode.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testSimpleBlockIsBinaryElement() {
		final Element elem = MatroskaDocTypes.SimpleBlock.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testBlockIsBinaryElement() {
		final Element elem = MatroskaDocTypes.Block.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testBlockGroupIsMasterElement() {
		final Element elem = MatroskaDocTypes.BlockGroup.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testBlockDurationIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.BlockDuration.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	@Test
	public void testReferenceBlockIsSignedInteger() {
		final Element elem = MatroskaDocTypes.ReferenceBlock.getInstance();
		Assertions.assertTrue(elem instanceof SignedIntegerElement);
	}

	// ============ Tag Elements ============

	@Test
	public void testTagsIsMasterElement() {
		final Element elem = MatroskaDocTypes.Tags.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testTagNameIsStringElement() {
		final Element elem = MatroskaDocTypes.TagName.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	@Test
	public void testTagStringIsStringElement() {
		final Element elem = MatroskaDocTypes.TagString.getInstance();
		Assertions.assertTrue(elem instanceof StringElement);
	}

	// ============ Chapter Elements ============

	@Test
	public void testChaptersIsMasterElement() {
		final Element elem = MatroskaDocTypes.Chapters.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	// ============ Attachment Elements ============

	@Test
	public void testAttachmentsIsMasterElement() {
		final Element elem = MatroskaDocTypes.Attachments.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	// ============ SeekHead Elements ============

	@Test
	public void testSeekHeadIsMasterElement() {
		final Element elem = MatroskaDocTypes.SeekHead.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testSeekIsMasterElement() {
		final Element elem = MatroskaDocTypes.Seek.getInstance();
		Assertions.assertTrue(elem instanceof MasterElement);
	}

	@Test
	public void testSeekIDIsBinaryElement() {
		final Element elem = MatroskaDocTypes.SeekID.getInstance();
		Assertions.assertTrue(elem instanceof BinaryElement);
	}

	@Test
	public void testSeekPositionIsUnsignedInteger() {
		final Element elem = MatroskaDocTypes.SeekPosition.getInstance();
		Assertions.assertTrue(elem instanceof UnsignedIntegerElement);
	}

	// ============ Level verification for hierarchy ============

	@Test
	public void testTopLevelElementsAtLevel1() {
		// Per Matroska spec, these elements are direct children of Segment (level 1)
		Assertions.assertEquals(1, MatroskaDocTypes.Info.getLevel());
		Assertions.assertEquals(1, MatroskaDocTypes.Tracks.getLevel());
		Assertions.assertEquals(1, MatroskaDocTypes.Cluster.getLevel());
		Assertions.assertEquals(1, MatroskaDocTypes.Cues.getLevel());
		Assertions.assertEquals(1, MatroskaDocTypes.SeekHead.getLevel());
		Assertions.assertEquals(1, MatroskaDocTypes.Tags.getLevel());
		Assertions.assertEquals(1, MatroskaDocTypes.Chapters.getLevel());
		Assertions.assertEquals(1, MatroskaDocTypes.Attachments.getLevel());
	}

	@Test
	public void testLevel2Elements() {
		Assertions.assertEquals(2, MatroskaDocTypes.TrackEntry.getLevel());
		Assertions.assertEquals(2, MatroskaDocTypes.CuePoint.getLevel());
	}

	@Test
	public void testLevel3TrackElements() {
		Assertions.assertEquals(3, MatroskaDocTypes.TrackNumber.getLevel());
		Assertions.assertEquals(3, MatroskaDocTypes.TrackUID.getLevel());
		Assertions.assertEquals(3, MatroskaDocTypes.TrackType.getLevel());
		Assertions.assertEquals(3, MatroskaDocTypes.CodecID.getLevel());
	}

	@Test
	public void testVoidAndCRCLevelMinus1() {
		// Void and CRC-32 can appear at any level
		Assertions.assertEquals(-1, MatroskaDocTypes.Void.getLevel());
		Assertions.assertEquals(-1, MatroskaDocTypes.CRC_32.getLevel());
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
