"""Decode both films, check media metadata, and produce time-labelled contact sheets."""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--ffmpeg", required=True)
    p.add_argument("--ffprobe", help="Optional; falls back to FFmpeg's stream header when unavailable")
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--duration", type=float, default=40)
    p.add_argument("--suffix", default="40s", help="Movie filename suffix, e.g. features-50s")
    a = p.parse_args()
    results = []
    for kind, size in [("landscape", (1920, 1080)), ("vertical", (1080, 1920))]:
        movie = a.output / f"Resonance-{kind}-{a.suffix}.mp4"
        if a.ffprobe:
            metadata = json.loads(subprocess.check_output([a.ffprobe, "-v", "error", "-show_streams", "-show_format", "-of", "json", str(movie)], text=True))
        else:
            header = subprocess.run([a.ffmpeg, "-hide_banner", "-i", str(movie)], capture_output=True, text=True).stderr
            video_line = next(line for line in header.splitlines() if "Video:" in line)
            audio_line = next(line for line in header.splitlines() if "Audio:" in line)
            dims = re.search(r" (\d{2,})x(\d{2,})(?=\s|,|$)", video_line)
            fps = float(re.search(r" ([\d.]+) fps", video_line)[1])
            duration = re.search(r"Duration: (\d+):(\d+):([\d.]+)", header)
            metadata = {"streams": [
                {"codec_type": "video", "width": int(dims[1]), "height": int(dims[2]), "avg_frame_rate": f"{int(fps)}/1", "codec_name": re.search(r"Video: (\w+)", video_line)[1]},
                {"codec_type": "audio", "channels": 2 if "stereo" in audio_line else 1, "codec_name": re.search(r"Audio: (\w+)", audio_line)[1]}],
                "format": {"duration": str(int(duration[1]) * 3600 + int(duration[2]) * 60 + float(duration[3]))}}
        video = next(s for s in metadata["streams"] if s["codec_type"] == "video")
        audio = next(s for s in metadata["streams"] if s["codec_type"] == "audio")
        assert (video["width"], video["height"]) == size
        assert video["avg_frame_rate"] == "30/1"
        assert abs(float(metadata["format"]["duration"]) - a.duration) < .1
        assert audio["channels"] == 2
        assert video["codec_name"] == "h264" and audio["codec_name"] == "aac"
        decoded = subprocess.run([a.ffmpeg, "-hide_banner", "-v", "error", "-i", str(movie), "-f", "null", "-"], capture_output=True, text=True)
        assert decoded.returncode == 0 and not decoded.stderr.strip(), decoded.stderr
        times = [t * a.duration / 40 for t in [1.5, 6.0, 13.0, 20.5, 26.0, 32.0, 37.5]]
        thumbnails = []
        for i, second in enumerate(times):
            frame = a.output / f"qa-{kind}-{i}.png"
            subprocess.run([a.ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-ss", str(second), "-i", str(movie), "-frames:v", "1", "-update", "1", str(frame)], check=True)
            im = Image.open(frame).convert("RGB")
            im.thumbnail((640, 380) if kind == "landscape" else (270, 480))
            thumbnails.append(im)
        cellw, cellh = (660, 410) if kind == "landscape" else (290, 520)
        cols = 2 if kind == "landscape" else 4
        sheet = Image.new("RGB", (cols * cellw, ((len(times) + cols - 1) // cols) * cellh), "#dfe6df")
        draw = ImageDraw.Draw(sheet)
        for i, im in enumerate(thumbnails):
            x, y = (i % cols) * cellw + 10, (i // cols) * cellh + 10
            sheet.paste(im, (x, y))
            draw.text((x, y + im.height + 5), f"{kind} / {times[i]:.1f}s", fill="#173b3c")
        sheet.save(a.output / f"contact-{kind}.jpg", quality=93)
        result = {"file": movie.name, "bytes": movie.stat().st_size, "sha256": hashlib.file_digest(movie.open("rb"), "sha256").hexdigest(),
                  "duration": metadata["format"]["duration"], "size": size, "fps": video["avg_frame_rate"], "audio_channels": audio["channels"], "full_decode": "pass"}
        results.append(result)
        print(result)
    (a.output / "qa.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    (a.output / "SHA256SUMS.txt").write_text("".join(f"{r['sha256']}  {r['file']}\n" for r in results), encoding="utf-8")


if __name__ == "__main__":
    main()
