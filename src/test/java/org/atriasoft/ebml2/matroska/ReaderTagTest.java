package org.atriasoft.ebml2.matroska;

import org.atriasoft.ebml2.matroska.model.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.atriasoft.ebml2.Element;
import org.atriasoft.ebml2.io.FileDataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for parseTags / parseTagsSimpleTag in Reader.
 *
 * Since Writer does not support writing tags,
 * we write a valid .mkv file with the writer, then inject
 * raw EBML binary for a Tags element before the Cluster.
 *
 * We construct raw bytes manually to avoid the library bug where
 * Element.writeHeaderData() corrupts ProtoType's internal ByteBuffer positions.
 *
 * Tag structure per Matroska spec:
 *   Tags (0x1254C367)
 *     Tag (0x7373)
 *       Targets (0x63C0)
 *         TagTrackUID (0x63C5) / TagChapterUID (0x63C4) / TagAttachmentUID (0x63C6)
 *       SimpleTag (0x67C8)
 *         TagName (0x45A3) - UTF-8 string
 *         TagString (0x4487) - UTF-8 string
 */
public class ReaderTagTest {
	private File destination;

	@BeforeEach
	public void setUp() throws Exception {
		this.destination = File.createTempFile("test-tags", ".mkv");
	}

	@AfterEach
	public void tearDown() throws Exception {
		this.destination.delete();
	}

	/**
	 * Write a raw EBML element: type ID bytes + EBML-coded size + data.
	 */
	private void writeRawElement(final ByteArrayOutputStream out, final byte[] typeId, final byte[] data) {
		out.writeBytes(typeId);
		out.writeBytes(Element.makeEbmlCodedSize(data.length));
		out.writeBytes(data);
	}

	/**
	 * Build a SimpleTag sub-element: SimpleTag(TagName + TagString).
	 */
	private byte[] buildSimpleTag(final String name, final String value) {
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();
		// TagName (0x45A3)
		final byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
		writeRawElement(inner, new byte[]{(byte) 0x45, (byte) 0xA3}, nameBytes);
		// TagString (0x4487)
		final byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
		writeRawElement(inner, new byte[]{(byte) 0x44, (byte) 0x87}, valueBytes);

		final ByteArrayOutputStream simpleTag = new ByteArrayOutputStream();
		// SimpleTag (0x67C8)
		writeRawElement(simpleTag, new byte[]{(byte) 0x67, (byte) 0xC8}, inner.toByteArray());
		return simpleTag.toByteArray();
	}

	/**
	 * Build a Targets sub-element with a single UID element.
	 */
	private byte[] buildTargets(final byte[] uidTypeId, final long uidValue) {
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();
		// UID element
		final byte[] uidData = encodeUnsignedInt(uidValue);
		writeRawElement(inner, uidTypeId, uidData);

		final ByteArrayOutputStream targets = new ByteArrayOutputStream();
		// Targets (0x63C0)
		writeRawElement(targets, new byte[]{(byte) 0x63, (byte) 0xC0}, inner.toByteArray());
		return targets.toByteArray();
	}

	/**
	 * Build an empty Targets sub-element.
	 */
	private byte[] buildEmptyTargets() {
		final ByteArrayOutputStream targets = new ByteArrayOutputStream();
		writeRawElement(targets, new byte[]{(byte) 0x63, (byte) 0xC0}, new byte[0]);
		return targets.toByteArray();
	}

	/**
	 * Build a complete Tag element: Tag(Targets + SimpleTag).
	 */
	private byte[] buildTag(final byte[] targetsBytes, final byte[] simpleTagBytes) {
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();
		inner.writeBytes(targetsBytes);
		inner.writeBytes(simpleTagBytes);

		final ByteArrayOutputStream tag = new ByteArrayOutputStream();
		// Tag (0x7373)
		writeRawElement(tag, new byte[]{(byte) 0x73, (byte) 0x73}, inner.toByteArray());
		return tag.toByteArray();
	}

	/**
	 * Build a complete Tags element wrapping one or more Tag elements.
	 */
	private byte[] buildTags(final byte[]... tagElements) {
		final ByteArrayOutputStream inner = new ByteArrayOutputStream();
		for (final byte[] tagElem : tagElements) {
			inner.writeBytes(tagElem);
		}
		final ByteArrayOutputStream tags = new ByteArrayOutputStream();
		// Tags (0x1254C367)
		writeRawElement(tags, new byte[]{(byte) 0x12, (byte) 0x54, (byte) 0xC3, (byte) 0x67}, inner.toByteArray());
		return tags.toByteArray();
	}

	/**
	 * Encode an unsigned integer in minimal bytes (big-endian).
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
	 * Write a base .mkv file using Writer.
	 */
	private void writeBaseMkv() throws IOException {
		final org.atriasoft.ebml2.io.FileDataWriter writer = new org.atriasoft.ebml2.io.FileDataWriter(this.destination.getPath());
		final Writer mkvWriter = new Writer(writer);
		final Track track = new Track();
		track.setTrackNo(1);
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(1000);
		mkvWriter.addTrack(track);

		final Frame frame = new Frame();
		frame.setTrackNo(1);
		frame.setTimecode(0);
		frame.setData(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
		mkvWriter.addFrame(frame);
		mkvWriter.close();
		writer.close();
	}

	/**
	 * Find the position of the actual Cluster element (0x1F43B675) in the file bytes.
	 * Skips occurrences inside SeekHead's SeekID entries by looking for the last match,
	 * since the Cluster element is always after MetaSeek/Info/Tracks.
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
	 * Inject raw bytes before the Cluster element in the file.
	 */
	private void injectBeforeCluster(final byte[] injectedBytes) throws IOException {
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
			ch.write(ByteBuffer.wrap(fileBytes, clusterPos, fileBytes.length - clusterPos));
			ch.truncate(ch.position());
		}
	}

	private Reader openReader() throws IOException {
		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.setScanFirstCluster(false);
		reader.readFile();
		return reader;
	}

	// TagTrackUID type ID: 0x63C5
	private static final byte[] TAG_TRACK_UID_ID = {(byte) 0x63, (byte) 0xC5};
	// TagChapterUID type ID: 0x63C4
	private static final byte[] TAG_CHAPTER_UID_ID = {(byte) 0x63, (byte) 0xC4};
	// TagAttachmentUID type ID: 0x63C6
	private static final byte[] TAG_ATTACHMENT_UID_ID = {(byte) 0x63, (byte) 0xC6};

	@Test
	public void testParseTagWithSimpleTag() throws IOException {
		writeBaseMkv();
		final byte[] tags = buildTags(
				buildTag(
						buildTargets(TAG_TRACK_UID_ID, 1),
						buildSimpleTag("TITLE", "Test Song")
				)
		);
		injectBeforeCluster(tags);

		final Reader reader = openReader();
		final String report = reader.getReport();
		Assertions.assertTrue(report.contains("Tag Count: 1"), "Expected 1 tag entry, report: " + report);
		Assertions.assertTrue(report.contains("TITLE"), "Expected tag name TITLE in report: " + report);
		Assertions.assertTrue(report.contains("Test Song"), "Expected tag value in report: " + report);
	}

	@Test
	public void testParseTagWithTrackUID() throws IOException {
		writeBaseMkv();
		final byte[] tags = buildTags(
				buildTag(
						buildTargets(TAG_TRACK_UID_ID, 42),
						buildSimpleTag("ARTIST", "Test Artist")
				)
		);
		injectBeforeCluster(tags);

		final Reader reader = openReader();
		final String report = reader.getReport();
		Assertions.assertTrue(report.contains("Tag Count: 1"), "Expected 1 tag entry, report: " + report);
		Assertions.assertTrue(report.contains("TrackUID: [42]"), "Expected TrackUID 42 in report: " + report);
		Assertions.assertTrue(report.contains("ARTIST"), "Expected ARTIST in report: " + report);
	}

	@Test
	public void testParseMultipleTags() throws IOException {
		writeBaseMkv();
		final byte[] tags = buildTags(
				buildTag(
						buildEmptyTargets(),
						buildSimpleTag("TITLE", "My Video")
				),
				buildTag(
						buildEmptyTargets(),
						buildSimpleTag("ARTIST", "Someone")
				)
		);
		injectBeforeCluster(tags);

		final Reader reader = openReader();
		final String report = reader.getReport();
		Assertions.assertTrue(report.contains("Tag Count: 2"), "Expected 2 tag entries, report: " + report);
		Assertions.assertTrue(report.contains("TITLE"), "Expected TITLE in report: " + report);
		Assertions.assertTrue(report.contains("ARTIST"), "Expected ARTIST in report: " + report);
	}

	@Test
	public void testParseTagWithChapterUID() throws IOException {
		writeBaseMkv();
		final byte[] tags = buildTags(
				buildTag(
						buildTargets(TAG_CHAPTER_UID_ID, 99),
						buildSimpleTag("DESCRIPTION", "Chapter description")
				)
		);
		injectBeforeCluster(tags);

		final Reader reader = openReader();
		final String report = reader.getReport();
		Assertions.assertTrue(report.contains("Tag Count: 1"), "Expected 1 tag entry, report: " + report);
		Assertions.assertTrue(report.contains("ChapterUID: [99]"), "Expected ChapterUID 99 in report: " + report);
		Assertions.assertTrue(report.contains("DESCRIPTION"), "Expected DESCRIPTION in report: " + report);
	}

	@Test
	public void testParseTagWithAttachmentUID() throws IOException {
		writeBaseMkv();
		final byte[] tags = buildTags(
				buildTag(
						buildTargets(TAG_ATTACHMENT_UID_ID, 77),
						buildSimpleTag("COMMENT", "Attachment comment")
				)
		);
		injectBeforeCluster(tags);

		final Reader reader = openReader();
		final String report = reader.getReport();
		Assertions.assertTrue(report.contains("Tag Count: 1"), "Expected 1 tag entry, report: " + report);
		Assertions.assertTrue(report.contains("AttachmentUID: [77]"), "Expected AttachmentUID 77 in report: " + report);
		Assertions.assertTrue(report.contains("COMMENT"), "Expected COMMENT in report: " + report);
	}

	@Test
	public void testNoTagsYieldsZeroTagCount() throws IOException {
		writeBaseMkv();

		final FileDataSource inputDataSource = new FileDataSource(this.destination.getPath());
		final Reader reader = new Reader(inputDataSource);
		reader.readFile();

		final String report = reader.getReport();
		Assertions.assertTrue(report.contains("Tag Count: 0"), "Expected 0 tags, report: " + report);
	}
}
