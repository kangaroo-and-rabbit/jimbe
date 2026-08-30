package org.atriasoft.jimbe.matroska;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.TrackType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Where the seek head says the cues are, the cues have to be.
 *
 * <p>
 * A player reads the seek head to find the index, and reads the index to jump. An entry that
 * points a little short of the cues lands in the middle of the last cluster, where there is no
 * index to be found: the player then has nothing left but to read the whole file to jump in it,
 * which on a film is the difference between a jump and a download.
 * </p>
 */
public class SeekHeadPointsAtCuesTest {
	/** EBML id of the Cues element. */
	private static final byte[] CUES_ID = { (byte) 0x1C, (byte) 0x53, (byte) 0xBB, (byte) 0x6B };

	/** EBML id of the SeekHead element. */
	private static final byte[] SEEK_HEAD_ID = { (byte) 0x11, (byte) 0x4D, (byte) 0x9B, (byte) 0x74 };

	/** EBML id of the Segment element. */
	private static final byte[] SEGMENT_ID = { (byte) 0x18, (byte) 0x53, (byte) 0x80, (byte) 0x67 };

	/** EBML id of a Seek entry. */
	private static final byte[] SEEK_ID = { (byte) 0x4D, (byte) 0xBB };

	/** EBML id of SeekID, naming what an entry points at. */
	private static final byte[] SEEK_TARGET_ID = { (byte) 0x53, (byte) 0xAB };

	/** EBML id of SeekPosition, saying where it is. */
	private static final byte[] SEEK_POSITION_ID = { (byte) 0x53, (byte) 0xAC };

	/** Last offset at which the needle appears, -1 when it does not. */
	private static int lastIndexOf(final byte[] haystack, final byte[] needle) {
		for (int iii = haystack.length - needle.length; iii >= 0; iii--) {
			boolean found = true;
			for (int jjj = 0; jjj < needle.length; jjj++) {
				if (haystack[iii + jjj] != needle[jjj]) {
					found = false;
					break;
				}
			}
			if (found) {
				return iii;
			}
		}
		return -1;
	}

	/** First offset at which the needle appears, -1 when it does not. */
	private static int indexOf(final byte[] haystack, final byte[] needle, final int from) {
		for (int iii = from; iii <= haystack.length - needle.length; iii++) {
			boolean found = true;
			for (int jjj = 0; jjj < needle.length; jjj++) {
				if (haystack[iii + jjj] != needle[jjj]) {
					found = false;
					break;
				}
			}
			if (found) {
				return iii;
			}
		}
		return -1;
	}

	/** Read an EBML variable length integer, and say how many bytes it took.
	 *
	 * @param data the file
	 * @param offset where the number starts
	 * @return the value, and the offset just after it */
	private static long[] readVint(final byte[] data, final int offset) {
		final int first = data[offset] & 0xff;
		for (int length = 1; length <= 8; length++) {
			if ((first & 0x80 >> length - 1) != 0) {
				long value = first & 0xff >> length;
				for (int iii = 1; iii < length; iii++) {
					value = value << 8 | data[offset + iii] & 0xff;
				}
				return new long[] { value, offset + length };
			}
		}
		throw new IllegalStateException("not a valid EBML number at " + offset);
	}

	@Test
	public void theSeekHeadEntryLandsOnTheCues() throws Exception {
		final File tempFile = File.createTempFile("seekhead-cues-", ".webm");
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
			// Enough key frames, far enough apart, to close several clusters and index each.
			for (int iii = 0; iii < 300; iii++) {
				final Frame frame = new Frame();
				frame.setTrackNo(1);
				frame.setTimecode(iii * 100L);
				frame.setKeyFrame(iii % 20 == 0);
				frame.setData(ByteBuffer.wrap(new byte[] { (byte) iii }));
				writer.addFrame(frame);
			}
			writer.close();
			output.close();

			final byte[] data = Files.readAllBytes(tempFile.toPath());

			// Where the segment holds its content: every seek position counts from there.
			final int segmentId = indexOf(data, SEGMENT_ID, 0);
			Assertions.assertTrue(segmentId >= 0, "no segment in the file");
			final long[] segmentSize = readVint(data, segmentId + SEGMENT_ID.length);
			final int segmentDataStart = (int) segmentSize[1];

			// Where the cues really are. The same four bytes also name them inside the seek head,
			// so the element itself is the last of the two: the cues are written at the end.
			final int cuesPosition = lastIndexOf(data, CUES_ID);
			Assertions.assertTrue(cuesPosition >= 0, "the file carries no cues");

			// What the seek head says about them.
			final int seekHead = indexOf(data, SEEK_HEAD_ID, segmentDataStart);
			Assertions.assertTrue(seekHead >= 0, "the file carries no seek head");
			Long announced = null;
			int cursor = seekHead;
			while (true) {
				final int entry = indexOf(data, SEEK_ID, cursor + 1);
				if (entry < 0 || entry > cuesPosition) {
					break;
				}
				final int target = indexOf(data, SEEK_TARGET_ID, entry);
				if (target >= 0 && target < entry + 16) {
					final long[] targetSize = readVint(data, target + SEEK_TARGET_ID.length);
					final int idStart = (int) targetSize[1];
					if (targetSize[0] == CUES_ID.length && indexOf(data, CUES_ID, idStart) == idStart) {
						final int position = indexOf(data, SEEK_POSITION_ID, idStart);
						final long[] positionSize = readVint(data, position + SEEK_POSITION_ID.length);
						long value = 0;
						for (int iii = 0; iii < positionSize[0]; iii++) {
							value = value << 8 | data[(int) positionSize[1] + iii] & 0xff;
						}
						announced = value;
						break;
					}
				}
				cursor = entry;
			}
			Assertions.assertNotNull(announced, "the seek head says nothing about the cues");
			Assertions.assertEquals(cuesPosition - segmentDataStart, announced.longValue(),
					"the seek head points somewhere other than at the cues");
		} finally {
			tempFile.delete();
		}
	}
}
