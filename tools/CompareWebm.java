import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.Reader;
import org.atriasoft.jimbe.matroska.Writer;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.TrackType;

/**
 * Extracts video from an MKV using jimbe, then compares the result frame-by-frame
 * with an ffmpeg-extracted reference WebM.
 *
 * Usage: java CompareWebm <input.mkv> <ffmpeg-reference.webm>
 */
public class CompareWebm {

	static class FrameInfo {
		final int trackNo;
		final long timecode;
		final boolean keyFrame;
		final int dataSize;
		final int dataHash;

		FrameInfo(final Frame frame) {
			this.trackNo = frame.getTrackNo();
			this.timecode = frame.getTimecode();
			this.keyFrame = frame.isKeyFrame();
			final ByteBuffer buf = frame.getData();
			this.dataSize = buf.remaining();
			int hash = 0;
			final ByteBuffer dup = buf.duplicate();
			while (dup.hasRemaining()) {
				hash = 31 * hash + (dup.get() & 0xFF);
			}
			this.dataHash = hash;
		}
	}

	public static void main(final String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Usage: java CompareWebm <input.mkv> <ffmpeg-reference.webm>");
			System.exit(1);
		}
		final String inputPath = args[0];
		final String refPath = args[1];

		// Step 1: Extract video-only with jimbe
		final String jimbeOutput = "/tmp/jimbe-compare-output.webm";
		System.out.println("=== Step 1: Extracting video with jimbe ===");
		extractVideoWithJimbe(inputPath, jimbeOutput);
		System.out.println("  Output: " + jimbeOutput);
		System.out.println("  Size: " + new File(jimbeOutput).length() + " bytes");
		System.out.println();

		// Step 2: Compare
		System.out.println("=== Step 2: Comparing with ffmpeg reference ===");
		System.out.println("  Reference: " + refPath);
		System.out.println("  Size: " + new File(refPath).length() + " bytes");
		System.out.println();

		System.out.println("--- Metadata comparison ---");
		compareMetadata(refPath, jimbeOutput);
		System.out.println();

		System.out.println("--- Reading frames from reference (ffmpeg) ---");
		final List<FrameInfo> refFrames = readAllFrames(refPath);
		System.out.println("  Total frames: " + refFrames.size());

		System.out.println("--- Reading frames from jimbe output ---");
		final List<FrameInfo> testFrames = readAllFrames(jimbeOutput);
		System.out.println("  Total frames: " + testFrames.size());
		System.out.println();

		// Compare
		final boolean pass = compareFrames(refFrames, testFrames);

		// Keep output for inspection
		System.out.println("Jimbe output kept at: " + jimbeOutput);

		if (!pass) {
			System.exit(1);
		}
	}

	static void extractVideoWithJimbe(final String inputPath, final String outputPath) throws Exception {
		try (FileDataSource source = new FileDataSource(inputPath)) {
			final FileDataWriter output = new FileDataWriter(outputPath);
			try {
				final Reader reader = new Reader(source);
				reader.readFile();
				final Writer writer = new Writer(output);
				writer.setTimecodeScale(reader.getTimecodeScale());
				final double duration = reader.getDuration();
				if (duration > 0) {
					writer.setDuration(duration);
				}
				final Track[] tracks = reader.getTrackList();
				int newTrackNo = 1;
				final Map<Integer, Integer> trackMapping = new LinkedHashMap<>();
				for (final Track track : tracks) {
					if (track.getTrackType() == TrackType.VIDEO) {
						final Track videoTrack = copyTrack(track, newTrackNo);
						writer.addTrack(videoTrack);
						trackMapping.put(track.getTrackNo(), newTrackNo);
						newTrackNo++;
						System.out.println("  Video track " + track.getTrackNo() + " -> " + trackMapping.get(track.getTrackNo())
								+ " codec=" + track.getCodecID());
					}
				}
				Frame frame;
				int count = 0;
				int keyCount = 0;
				while ((frame = reader.getNextFrame()) != null) {
					final Integer mappedTrackNo = trackMapping.get(frame.getTrackNo());
					if (mappedTrackNo != null) {
						frame.setTrackNo(mappedTrackNo);
						writer.addFrame(frame);
						count++;
						if (frame.isKeyFrame()) {
							keyCount++;
						}
						if (count % 10000 == 0) {
							System.out.println("    ... " + count + " frames written (" + keyCount + " keyframes)");
						}
					}
				}
				System.out.println("  Total written: " + count + " frames (" + keyCount + " keyframes)");
				writer.close();
			} finally {
				output.close();
			}
		}
	}

	static Track copyTrack(final Track source, final int newTrackNo) {
		final Track dest = new Track();
		dest.setTrackNo(newTrackNo);
		dest.setTrackUID(source.getTrackUID());
		dest.setTrackType(source.getTrackType());
		dest.setCodecID(source.getCodecID());
		dest.setCodecPrivate(source.getCodecPrivate());
		dest.setDefaultDuration(source.getDefaultDuration());
		dest.setName(source.getName());
		dest.setLanguage(source.getLanguage());
		dest.setFlagEnabled(source.isFlagEnabled());
		dest.setFlagDefault(source.isFlagDefault());
		dest.setFlagForced(source.isFlagForced());
		dest.setFlagLacing(source.isFlagLacing());
		dest.setVideo(source.getVideo());
		dest.setAudio(source.getAudio());
		return dest;
	}

	static boolean compareFrames(final List<FrameInfo> refFrames, final List<FrameInfo> testFrames) {
		System.out.println("--- Frame-by-frame comparison ---");
		final int minFrames = Math.min(refFrames.size(), testFrames.size());

		if (refFrames.size() != testFrames.size()) {
			System.out.println("WARNING: Frame count mismatch! ref=" + refFrames.size() + " test=" + testFrames.size());
		}

		int mismatchTimecode = 0;
		int mismatchKeyFrame = 0;
		int mismatchDataSize = 0;
		int mismatchData = 0;
		int mismatchTrack = 0;
		int totalKeyRef = 0;
		int totalKeyTest = 0;

		for (int i = 0; i < minFrames; i++) {
			final FrameInfo ref = refFrames.get(i);
			final FrameInfo test = testFrames.get(i);

			if (ref.keyFrame) {
				totalKeyRef++;
			}
			if (test.keyFrame) {
				totalKeyTest++;
			}
			if (ref.trackNo != test.trackNo) {
				mismatchTrack++;
				if (mismatchTrack <= 5) {
					System.out.println("  Frame " + i + ": track mismatch ref=" + ref.trackNo + " test=" + test.trackNo);
				}
			}
			if (ref.timecode != test.timecode) {
				mismatchTimecode++;
				if (mismatchTimecode <= 5) {
					System.out.println("  Frame " + i + ": timecode mismatch ref=" + ref.timecode + " test=" + test.timecode);
				}
			}
			if (ref.keyFrame != test.keyFrame) {
				mismatchKeyFrame++;
				if (mismatchKeyFrame <= 10) {
					System.out.println("  Frame " + i + ": keyFrame mismatch ref=" + ref.keyFrame + " test=" + test.keyFrame
							+ " (tc=" + ref.timecode + ", size=" + ref.dataSize + ")");
				}
			}
			if (ref.dataSize != test.dataSize) {
				mismatchDataSize++;
				if (mismatchDataSize <= 5) {
					System.out.println("  Frame " + i + ": dataSize mismatch ref=" + ref.dataSize + " test=" + test.dataSize
							+ " (tc=" + ref.timecode + ")");
				}
			}
			if (ref.dataHash != test.dataHash) {
				mismatchData++;
				if (mismatchData <= 5) {
					System.out.println("  Frame " + i + ": data content mismatch (tc=" + ref.timecode
							+ " size ref=" + ref.dataSize + " test=" + test.dataSize + ")");
				}
			}
		}

		for (int i = minFrames; i < refFrames.size(); i++) {
			if (refFrames.get(i).keyFrame) {
				totalKeyRef++;
			}
		}
		for (int i = minFrames; i < testFrames.size(); i++) {
			if (testFrames.get(i).keyFrame) {
				totalKeyTest++;
			}
		}

		System.out.println();
		System.out.println("=== RESULTS ===");
		System.out.println("Total frames:     ref=" + refFrames.size() + "  test=" + testFrames.size());
		System.out.println("Total keyframes:  ref=" + totalKeyRef + "  test=" + totalKeyTest);
		System.out.println("Compared frames:  " + minFrames);
		System.out.println("Track mismatches: " + mismatchTrack);
		System.out.println("TC mismatches:    " + mismatchTimecode);
		System.out.println("Key mismatches:   " + mismatchKeyFrame);
		System.out.println("Size mismatches:  " + mismatchDataSize);
		System.out.println("Data mismatches:  " + mismatchData);

		final boolean pass = refFrames.size() == testFrames.size() && mismatchTimecode == 0
				&& mismatchKeyFrame == 0 && mismatchDataSize == 0 && mismatchData == 0 && mismatchTrack == 0;

		if (pass) {
			System.out.println("\n*** PASS: Files are identical frame-by-frame ***");
		} else {
			System.out.println("\n*** FAIL: Files differ ***");
		}
		return pass;
	}

	static void compareMetadata(final String path1, final String path2) throws Exception {
		try (FileDataSource src1 = new FileDataSource(path1);
				FileDataSource src2 = new FileDataSource(path2)) {
			final Reader r1 = new Reader(src1);
			r1.readFile();
			final Reader r2 = new Reader(src2);
			r2.readFile();

			System.out.println("  TimecodeScale: ref=" + r1.getTimecodeScale() + "  test=" + r2.getTimecodeScale());
			System.out.println("  Duration:      ref=" + r1.getDuration() + "  test=" + r2.getDuration());

			final Track[] tracks1 = r1.getTrackList();
			final Track[] tracks2 = r2.getTrackList();
			System.out.println("  Tracks:        ref=" + (tracks1 != null ? tracks1.length : 0)
					+ "  test=" + (tracks2 != null ? tracks2.length : 0));

			if (tracks1 != null) {
				for (final Track t : tracks1) {
					System.out.println("    ref track " + t.getTrackNo() + ": type=" + t.getTrackType()
							+ " codec=" + t.getCodecID());
				}
			}
			if (tracks2 != null) {
				for (final Track t : tracks2) {
					System.out.println("    test track " + t.getTrackNo() + ": type=" + t.getTrackType()
							+ " codec=" + t.getCodecID());
				}
			}
		}
	}

	static List<FrameInfo> readAllFrames(final String path) throws Exception {
		final List<FrameInfo> frames = new ArrayList<>();
		try (FileDataSource source = new FileDataSource(path)) {
			final Reader reader = new Reader(source);
			reader.readFile();
			Frame frame;
			int keyCount = 0;
			while ((frame = reader.getNextFrame()) != null) {
				final FrameInfo info = new FrameInfo(frame);
				frames.add(info);
				if (info.keyFrame) {
					keyCount++;
				}
				if (frames.size() % 10000 == 0) {
					System.out.println("    ... " + frames.size() + " frames read (" + keyCount + " keyframes)");
				}
			}
		}
		return frames;
	}
}
