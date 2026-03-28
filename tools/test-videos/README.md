# Test Videos

MKV test videos for validating Matroska parsing in jimbe.
Multiple video codecs, keyframe intervals, audio track counts, and subtitle streams.

## Video Matrix

| File | Video codec | Audio tracks | Subtitles | Keyframes |
|------|-------------|--------------|-----------|-----------|
| `vp8_1s_1audio.mkv` | VP8 | 1 (Opus) | SRT + ASS | fixed 1s |
| `vp8_2s_2audio.mkv` | VP8 | 2 (Opus + Vorbis) | SRT + ASS | fixed 2s |
| `vp9_3s_1audio.mkv` | VP9 | 1 (Opus) | SRT + ASS | fixed 3s |
| `vp9_4s_2audio.mkv` | VP9 | 2 (Opus + Vorbis) | SRT + ASS | fixed 4s |
| `av1_1s_1audio.mkv` | AV1 (svtav1) | 1 (Opus) | SRT + ASS | fixed 1s |
| `av1_2s_2audio.mkv` | AV1 (svtav1) | 2 (Opus + Vorbis) | SRT + ASS | fixed 2s |
| `vp9_var_1audio.mkv` | VP9 | 1 (Opus) | SRT + ASS | variable |
| `av1_var_2audio.mkv` | AV1 (svtav1) | 2 (Opus + Vorbis) | SRT + ASS | variable |

Each file also has a `*_video_only.mkv` variant with only the video track (no audio, no subtitles).

## Specifications

- **Duration:** 120 seconds
- **Resolution:** 150x150
- **Framerate:** 30 fps
- **Container:** Matroska (.mkv)

### Video codecs
- **VP8** (libvpx) — 200 kbps CBR
- **VP9** (libvpx-vp9) — 150 kbps CBR
- **AV1** (libsvtav1) — CRF 35, preset 8

### Audio codecs
- **Opus** (libopus) — 64 kbps mono, white noise
- **Vorbis** (libvorbis) — 64 kbps mono, 440 Hz sine tone

### Subtitle formats
- **SRT** — cues every 5 seconds, English
- **ASS** — cues every 5 seconds, French

### Keyframe modes
- **Fixed:** keyframes at regular intervals (1s, 2s, 3s, or 4s)
- **Variable:** keyframes at explicit irregular timestamps:
  `0, 0.8, 2.5, 4, 5.3, 8, 10, 13, 15.5, 19, 22, 26, 30, 35, 40, 44, 50, 55, 62, 70, 78, 85, 93, 100, 108, 115, 120`

## Regeneration

```bash
cd tools/test-videos
bash generate.sh
```

Requires `ffmpeg` compiled with `libvpx`, `libvpx-vp9`, `libsvtav1`, `libopus`, and `libvorbis`.
