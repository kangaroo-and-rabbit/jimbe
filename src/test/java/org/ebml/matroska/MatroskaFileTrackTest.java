package org.ebml.matroska;

import org.ebml.matroska.MatroskaFileTrack.TrackType;
import org.ebml.matroska.MatroskaFileTrack.MatroskaVideoTrack;
import org.ebml.matroska.MatroskaFileTrack.MatroskaAudioTrack;
import org.ebml.matroska.MatroskaFileTrack.TrackOperation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MatroskaFileTrackTest {

	@Test
	public void testDefaultValues() {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		Assertions.assertEquals(1, track.getTrackNo());
		Assertions.assertEquals(1337, track.getTrackUID());
		Assertions.assertTrue(track.isFlagEnabled());
		Assertions.assertTrue(track.isFlagDefault());
		Assertions.assertFalse(track.isFlagForced());
		Assertions.assertTrue(track.isFlagLacing());
		Assertions.assertEquals("unnamed", track.getName());
		Assertions.assertEquals("eng", track.getLanguage());
	}

	@Test
	public void testTrackTypeFromOrdinalVideo() {
		Assertions.assertEquals(TrackType.VIDEO, TrackType.fromOrdinal(1));
	}

	@Test
	public void testTrackTypeFromOrdinalAudio() {
		Assertions.assertEquals(TrackType.AUDIO, TrackType.fromOrdinal(2));
	}

	@Test
	public void testTrackTypeFromOrdinalComplex() {
		Assertions.assertEquals(TrackType.COMPLEX, TrackType.fromOrdinal(3));
	}

	@Test
	public void testTrackTypeFromOrdinalLogo() {
		Assertions.assertEquals(TrackType.LOGO, TrackType.fromOrdinal(0x10));
	}

	@Test
	public void testTrackTypeFromOrdinalSubtitle() {
		Assertions.assertEquals(TrackType.SUBTITLE, TrackType.fromOrdinal(0x11));
	}

	@Test
	public void testTrackTypeFromOrdinalButtons() {
		Assertions.assertEquals(TrackType.BUTTONS, TrackType.fromOrdinal(0x12));
	}

	@Test
	public void testTrackTypeFromOrdinalControl() {
		Assertions.assertEquals(TrackType.CONTROL, TrackType.fromOrdinal(0x20));
	}

	@Test
	public void testTrackTypeFromOrdinalUnknown() {
		Assertions.assertNull(TrackType.fromOrdinal(99));
		Assertions.assertNull(TrackType.fromOrdinal(0));
		Assertions.assertNull(TrackType.fromOrdinal(-1));
	}

	@Test
	public void testSetGetTrackProperties() {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setTrackNo(5);
		track.setTrackUID(12345L);
		track.setTrackType(TrackType.VIDEO);
		track.setName("My Video");
		track.setLanguage("fra");
		track.setCodecID("V_MPEG4/ISO/AVC");
		track.setDefaultDuration(33333333L);

		Assertions.assertEquals(5, track.getTrackNo());
		Assertions.assertEquals(12345L, track.getTrackUID());
		Assertions.assertEquals(TrackType.VIDEO, track.getTrackType());
		Assertions.assertEquals("My Video", track.getName());
		Assertions.assertEquals("fra", track.getLanguage());
		Assertions.assertEquals("V_MPEG4/ISO/AVC", track.getCodecID());
		Assertions.assertEquals(33333333L, track.getDefaultDuration());
	}

	@Test
	public void testVideoTrackProperties() {
		final MatroskaVideoTrack video = new MatroskaVideoTrack();
		video.setPixelWidth((short) 1920);
		video.setPixelHeight((short) 1080);
		video.setDisplayWidth((short) 1920);
		video.setDisplayHeight((short) 1080);

		Assertions.assertEquals(1920, video.getPixelWidth());
		Assertions.assertEquals(1080, video.getPixelHeight());
		Assertions.assertEquals(1920, video.getDisplayWidth());
		Assertions.assertEquals(1080, video.getDisplayHeight());
	}

	@Test
	public void testVideoTrackDefaultDisplaySize() {
		final MatroskaVideoTrack video = new MatroskaVideoTrack();
		Assertions.assertEquals(0, video.getDisplayWidth());
		Assertions.assertEquals(0, video.getDisplayHeight());
	}

	@Test
	public void testAudioTrackProperties() {
		final MatroskaAudioTrack audio = new MatroskaAudioTrack();
		audio.setSamplingFrequency(44100.0f);
		audio.setOutputSamplingFrequency(44100.0f);
		audio.setChannels((short) 2);
		audio.setBitDepth(16);

		Assertions.assertEquals(44100.0f, audio.getSamplingFrequency());
		Assertions.assertEquals(44100.0f, audio.getOutputSamplingFrequency());
		Assertions.assertEquals(2, audio.getChannels());
		Assertions.assertEquals(16, audio.getBitDepth());
	}

	@Test
	public void testTrackOperation() {
		final TrackOperation op = new TrackOperation();
		op.addVirtualTrackPart(1L);
		op.addVirtualTrackPart(2L);

		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setOperation(op);
		Assertions.assertNotNull(track.getOperation());
	}

	@Test
	public void testFlagAccessors() {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setFlagEnabled(false);
		track.setFlagDefault(false);
		track.setFlagForced(true);
		track.setFlagLacing(false);

		Assertions.assertFalse(track.isFlagEnabled());
		Assertions.assertFalse(track.isFlagDefault());
		Assertions.assertTrue(track.isFlagForced());
		Assertions.assertFalse(track.isFlagLacing());
	}

	@Test
	public void testToStringSubtitleTrack() {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setTrackType(TrackType.SUBTITLE);
		track.setCodecID("S_TEXT/UTF8");
		track.setDefaultDuration(0);
		final String result = track.toString();
		Assertions.assertTrue(result.contains("SUBTITLE"));
		Assertions.assertTrue(result.contains("S_TEXT/UTF8"));
	}

	@Test
	public void testToStringVideoTrack() {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setTrackType(TrackType.VIDEO);
		track.setCodecID("V_MPEG4/ISO/AVC");
		track.setDefaultDuration(33333333L);
		final MatroskaVideoTrack video = new MatroskaVideoTrack();
		video.setPixelWidth((short) 1920);
		video.setPixelHeight((short) 1080);
		track.setVideo(video);
		final String result = track.toString();
		Assertions.assertTrue(result.contains("VIDEO"));
		Assertions.assertTrue(result.contains("1920"));
		Assertions.assertTrue(result.contains("1080"));
	}

	@Test
	public void testToStringAudioTrack() {
		final MatroskaFileTrack track = new MatroskaFileTrack();
		track.setTrackType(TrackType.AUDIO);
		track.setCodecID("A_AAC");
		track.setDefaultDuration(23000000L);
		final MatroskaAudioTrack audio = new MatroskaAudioTrack();
		audio.setSamplingFrequency(48000.0f);
		audio.setChannels((short) 2);
		track.setAudio(audio);
		final String result = track.toString();
		Assertions.assertTrue(result.contains("AUDIO"));
		Assertions.assertTrue(result.contains("48000"));
	}
}
