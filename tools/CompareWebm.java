import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.Reader;
import org.atriasoft.jimbe.matroska.Writer;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.Track;
import org.atriasoft.jimbe.matroska.model.TrackType;

/**
 * Validates jimbe demux roundtrip: reads an MKV, extracts video with jimbe,
 * re-reads the output and compares frame-by-frame with the video frames
 * read from the original source.
 *
 * Usage:
 *   java CompareWebm <input.mkv>              - roundtrip test (source video frames vs jimbe output)
 *   java CompareWebm <input.mkv> <ref.webm>   - compare jimbe output with external reference
 *   java CompareWebm --batch <dir>             - test all MKV files in directory
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
		if (args.length < 1) {
			System.out.println("Usage:");
			System.out.println("  java CompareWebm <input.mkv>              - roundtrip test");
			System.out.println("  java CompareWebm <input.mkv> <ref.webm>   - compare with reference");
			System.out.println("  java CompareWebm --batch <dir>             - test all MKV files in dir");
			System.exit(1);
		}

		if ("--batch".equals(args[0])) {
			final String dir = args.length > 1 ? args[1] : ".";
			runBatch(dir);
			return;
		}

		final String inputPath = args[0];
		final boolean useExternalRef = args.length >= 2;
		final String refPath = useExternalRef ? args[1] : null;

		final boolean pass = runSingleTest(inputPath, refPath);
		if (!pass) {
			System.exit(1);
		}
	}

	static boolean runSingleTest(final String inputPath, final String refPath) throws Exception {
		final String jimbeOutput = "/tmp/jimbe-compare-output.webm";

		// Step 1: Read video frames from source
		System.out.println("--- Reading video frames from source ---");
		final List<FrameInfo> sourceVideoFrames = readVideoFrames(inputPath);
		System.out.println("  Video frames: " + sourceVideoFrames.size()
				+ " (keyframes: " + countKeyframes(sourceVideoFrames) + ")");

		// Step 2: Extract video-only with jimbe
		System.out.println("--- Extracting video with jimbe ---");
		extractVideoWithJimbe(inputPath, jimbeOutput);
		System.out.println("  Output: " + jimbeOutput + " (" + new File(jimbeOutput).length() + " bytes)");

		// Step 3: Re-read the jimbe output
		System.out.println("--- Re-reading jimbe output ---");
		final List<FrameInfo> jimbeFrames = readAllFrames(jimbeOutput);
		System.out.println("  Frames: " + jimbeFrames.size()
				+ " (keyframes: " + countKeyframes(jimbeFrames) + ")");

		// Step 4: Compare source video frames vs jimbe roundtrip
		System.out.println("--- Roundtrip comparison (source video vs jimbe re-read) ---");
		final boolean roundtripPass = compareFrames(sourceVideoFrames, jimbeFrames);

		// Step 5: If external reference provided, also compare with it
		if (refPath != null) {
			System.out.println();
			System.out.println("--- Reading external reference ---");
			final List<FrameInfo> refFrames = readAllFrames(refPath);
			System.out.println("  Frames: " + refFrames.size()
					+ " (keyframes: " + countKeyframes(refFrames) + ")");
			System.out.println("--- Reference comparison (ref vs jimbe re-read) ---");
			compareFrames(refFrames, jimbeFrames);
		}

		return roundtripPass;
	}

	static void runBatch(final String dirPath) throws Exception {
		final File dir = new File(dirPath);
		final File[] files = dir.listFiles((d, name) -> name.endsWith(".mkv") && !name.endsWith("_video_only.mkv"));
		if (files == null || files.length == 0) {
			System.out.println("No MKV files found in " + dirPath);
			System.exit(1);
		}
		java.util.Arrays.sort(files);

		int totalPass = 0;
		int totalFail = 0;
		final List<String> failures = new ArrayList<>();

		for (final File mkv : files) {
			final String name = mkv.getName();
			System.out.println();
			System.out.println("================================================================");
			System.out.println("TESTING: " + name);
			System.out.println("================================================================");

			try {
				final boolean pass = runSingleTest(mkv.getAbsolutePath(), null);
				if (pass) {
					totalPass++;
				} else {
					totalFail++;
					failures.add(name);
				}
			} catch (final Exception ex) {
				System.out.println("*** ERROR: " + ex.getMessage());
				totalFail++;
				failures.add(name + " (ERROR: " + ex.getMessage() + ")");
			}
		}

		System.out.println();
		System.out.println("================================================================");
		System.out.println("BATCH RESULTS: " + totalPass + "/" + (totalPass + totalFail) + " passed");
		if (!failures.isEmpty()) {
			System.out.println("FAILURES:");
			for (final String f : failures) {
				System.out.println("  - " + f);
			}
		}
		System.out.println("================================================================");

		if (totalFail > 0) {
			System.exit(1);
		}
	}

	/**
	 * Read only video frames from a multi-track MKV file.
	 */
	static List<FrameInfo> readVideoFrames(final String path) throws Exception {
		final List<FrameInfo> frames = new ArrayList<>();
		try (FileDataSource source = new FileDataSource(path)) {
			final Reader reader = new Reader(source);
			reader.readFile();
			final Track[] tracks = reader.getTrackList();
			final Set<Integer> videoTrackNos = new java.util.HashSet<>();
			for (final Track track : tracks) {
				if (track.getTrackType() == TrackType.VIDEO) {
					videoTrackNos.add(track.getTrackNo());
				}
			}
			Frame frame;
			while ((frame = reader.getNextFrame()) != null) {
				if (videoTrackNos.contains(frame.getTrackNo())) {
					// Normalize track number to 1 for comparison
					frame.setTrackNo(1);
					frames.add(new FrameInfo(frame));
				}
			}
		}
		return frames;
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

	static int countKeyframes(final List<FrameInfo> frames) {
		int count = 0;
		for (final FrameInfo f : frames) {
			if (f.keyFrame) {
				count++;
			}
		}
		return count;
	}

	static boolean compareFrames(final List<FrameInfo> refFrames, final List<FrameInfo> testFrames) {
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

		System.out.println("  Frames: ref=" + refFrames.size() + "  test=" + testFrames.size()
				+ "  | Keyframes: ref=" + totalKeyRef + "  test=" + totalKeyTest);
		System.out.println("  Mismatches: track=" + mismatchTrack + " tc=" + mismatchTimecode
				+ " key=" + mismatchKeyFrame + " size=" + mismatchDataSize + " data=" + mismatchData);

		final boolean pass = refFrames.size() == testFrames.size() && mismatchTimecode == 0
				&& mismatchKeyFrame == 0 && mismatchDataSize == 0 && mismatchData == 0 && mismatchTrack == 0;

		if (pass) {
			System.out.println("  *** PASS ***");
		} else {
			System.out.println("  *** FAIL ***");
		}
		return pass;
	}

	static List<FrameInfo> readAllFrames(final String path) throws Exception {
		final List<FrameInfo> frames = new ArrayList<>();
		try (FileDataSource source = new FileDataSource(path)) {
			final Reader reader = new Reader(source);
			reader.readFile();
			Frame frame;
			while ((frame = reader.getNextFrame()) != null) {
				frames.add(new FrameInfo(frame));
			}
		}
		return frames;
	}
}
