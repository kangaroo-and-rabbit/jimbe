#!/bin/bash
# Generate MKV test videos with various codecs, keyframe intervals,
# audio track counts, and subtitle streams.
#
# Requirements: ffmpeg with libvpx, libvpx-vp9, libsvtav1, libopus, libvorbis
#
# Each video is 120 seconds, 150x150, 30fps.
# Subtitles: SRT + ASS tracks on every file.
# Video-only versions (no audio/subs) are also extracted.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DURATION=120
SIZE="150x150"
FPS=30
SRT_FILE="${SCRIPT_DIR}/_subtitles.srt"
ASS_FILE="${SCRIPT_DIR}/_subtitles.ass"

# Variable keyframe timestamps (irregular spacing over 120s)
VAR_KEYFRAMES="0,0.8,2.5,4,5.3,8,10,13,15.5,19,22,26,30,35,40,44,50,55,62,70,78,85,93,100,108,115,120"

# ---------------------------------------------------------------------------
# Generate subtitle files
# ---------------------------------------------------------------------------
generate_srt() {
	local file="$1"
	local i=1
	local t=0
	rm -f "${file}"
	while [ "${t}" -lt "${DURATION}" ]; do
		local end=$((t + 5))
		if [ "${end}" -gt "${DURATION}" ]; then
			end=${DURATION}
		fi
		local ts_start
		local ts_end
		ts_start=$(printf "%02d:%02d:%02d,000" $((t/3600)) $(( (t%3600)/60 )) $((t%60)))
		ts_end=$(printf "%02d:%02d:%02d,000" $((end/3600)) $(( (end%3600)/60 )) $((end%60)))
		printf "%d\n%s --> %s\nSubtitle line %d - %ds to %ds\n\n" \
			"${i}" "${ts_start}" "${ts_end}" "${i}" "${t}" "${end}" >> "${file}"
		i=$((i + 1))
		t=${end}
	done
	echo "Generated SRT: ${file}"
}

generate_ass() {
	local file="$1"
	cat > "${file}" << 'HEADER'
[Script Info]
Title: Test Subtitles
ScriptType: v4.00+
PlayResX: 150
PlayResY: 150

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,12,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,1,0,2,10,10,10,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
HEADER
	local t=0
	local i=1
	while [ "${t}" -lt "${DURATION}" ]; do
		local end=$((t + 5))
		if [ "${end}" -gt "${DURATION}" ]; then
			end=${DURATION}
		fi
		local ts_start
		local ts_end
		ts_start=$(printf "%d:%02d:%02d.00" $((t/3600)) $(( (t%3600)/60 )) $((t%60)))
		ts_end=$(printf "%d:%02d:%02d.00" $((end/3600)) $(( (end%3600)/60 )) $((end%60)))
		printf "Dialogue: 0,%s,%s,Default,,0,0,0,,Subtitle line %d - %ds to %ds\n" \
			"${ts_start}" "${ts_end}" "${i}" "${t}" "${end}" >> "${file}"
		i=$((i + 1))
		t=${end}
	done
	echo "Generated ASS: ${file}"
}

# ---------------------------------------------------------------------------
# Generate a single video
# ---------------------------------------------------------------------------
# Args: output_name video_codec keyframe_mode audio_count
# keyframe_mode: "Ns" for fixed N-second interval, or "var" for variable
generate_video() {
	local name="$1"
	local vcodec="$2"
	local kf_mode="$3"
	local audio_count="$4"

	local output="${SCRIPT_DIR}/${name}.mkv"
	local video_only="${SCRIPT_DIR}/${name}_video_only.mkv"

	echo "========================================"
	echo "Generating: ${name}.mkv"
	echo "  Video: ${vcodec} | KF: ${kf_mode} | Audio tracks: ${audio_count}"
	echo "========================================"

	# --- Build ffmpeg input arguments ---
	local inputs=()
	inputs+=(-f lavfi -i "testsrc=duration=${DURATION}:size=${SIZE}:rate=${FPS}")
	# Audio 1: Opus (noise)
	inputs+=(-f lavfi -i "anoisesrc=duration=${DURATION}:sample_rate=48000:amplitude=0.05")
	if [ "${audio_count}" -ge 2 ]; then
		# Audio 2: Vorbis (sine tone)
		inputs+=(-f lavfi -i "sine=duration=${DURATION}:frequency=440:sample_rate=48000")
	fi
	# Subtitle inputs
	inputs+=(-i "${SRT_FILE}")
	inputs+=(-i "${ASS_FILE}")

	# --- Build codec arguments ---
	local codec_args=()

	# Video codec
	case "${vcodec}" in
		vp8)
			codec_args+=(-c:v libvpx -b:v 200k -cpu-used 8)
			;;
		vp9)
			codec_args+=(-c:v libvpx-vp9 -b:v 150k -cpu-used 8 -row-mt 1)
			;;
		av1)
			codec_args+=(-c:v libsvtav1 -preset 8)
			;;
	esac

	# Keyframe control
	case "${kf_mode}" in
		var)
			codec_args+=(-force_key_frames "${VAR_KEYFRAMES}")
			;;
		*s)
			local interval="${kf_mode%s}"
			local gop=$((interval * FPS))
			codec_args+=(-g "${gop}")
			;;
	esac

	# Audio codecs
	codec_args+=(-c:a:0 libopus -b:a:0 64k)
	if [ "${audio_count}" -ge 2 ]; then
		codec_args+=(-c:a:1 libvorbis -b:a:1 64k)
	fi

	# Subtitle codecs (copy SRT as-is, copy ASS as-is)
	codec_args+=(-c:s:0 srt -c:s:1 ass)

	# --- Build map arguments ---
	local maps=()
	maps+=(-map 0:v)   # video
	maps+=(-map 1:a)   # audio 1
	local sub_srt_idx=2
	if [ "${audio_count}" -ge 2 ]; then
		maps+=(-map 2:a)  # audio 2
		sub_srt_idx=3
	fi
	local sub_ass_idx=$((sub_srt_idx + 1))
	maps+=(-map "${sub_srt_idx}:s")  # SRT
	maps+=(-map "${sub_ass_idx}:s")  # ASS

	# Metadata for tracks
	local metadata=()
	metadata+=(-metadata:s:a:0 language=eng -metadata:s:a:0 title="Audio 1 (Opus)")
	if [ "${audio_count}" -ge 2 ]; then
		metadata+=(-metadata:s:a:1 language=fra -metadata:s:a:1 title="Audio 2 (Vorbis)")
	fi
	metadata+=(-metadata:s:s:0 language=eng -metadata:s:s:0 title="English SRT")
	metadata+=(-metadata:s:s:1 language=fra -metadata:s:s:1 title="French ASS")

	# --- Run ffmpeg ---
	ffmpeg -y "${inputs[@]}" "${maps[@]}" "${codec_args[@]}" "${metadata[@]}" "${output}"

	# --- Extract video-only ---
	echo "Extracting video-only -> ${video_only}"
	ffmpeg -y -i "${output}" -map 0:v -c:v copy "${video_only}"

	echo ""
}

# ===========================================================================
# Main
# ===========================================================================

echo "=== Generating subtitle files ==="
generate_srt "${SRT_FILE}"
generate_ass "${ASS_FILE}"
echo ""

# --- Video matrix ---
# name                  vcodec  kf_mode  audio_count
generate_video "vp8_1s_1audio"   vp8  1s  1
generate_video "vp8_2s_2audio"   vp8  2s  2
generate_video "vp9_3s_1audio"   vp9  3s  1
generate_video "vp9_4s_2audio"   vp9  4s  2
generate_video "av1_1s_1audio"   av1  1s  1
generate_video "av1_2s_2audio"   av1  2s  2
generate_video "vp9_var_1audio"  vp9  var  1
generate_video "av1_var_2audio"  av1  var  2

# --- Cleanup temp subtitle files ---
rm -f "${SRT_FILE}" "${ASS_FILE}"

# ===========================================================================
# Verification
# ===========================================================================
echo ""
echo "========================================="
echo "=== Verification ==="
echo "========================================="

for f in "${SCRIPT_DIR}"/*.mkv; do
	[ "$(basename "${f}")" = "*.mkv" ] && continue
	echo "--- $(basename "${f}") ---"
	ffprobe -v error -show_entries stream=index,codec_type,codec_name \
		-of csv=p=0 "${f}" 2>/dev/null
	echo "  Keyframes:"
	ffprobe -v error -select_streams v:0 \
		-show_entries packet=pts_time,flags -of csv=p=0 \
		"${f}" 2>/dev/null | grep ",K" | head -10
	echo "  ..."
	echo ""
done

echo "Done. Generated $(ls -1 "${SCRIPT_DIR}"/*.mkv 2>/dev/null | wc -l) MKV files."
