"""Compose an original 40 s product film from MuMu native recordings. Pillow + NumPy + FFmpeg.

All typography, sleeves, music, and graphics are original; the product footage is not simulated HTML.
The machine-specific executable/font paths are CLI arguments, never build defaults.
"""
import argparse
import json
import math
import subprocess
import wave
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

DURATIONS = [3, 7.5, 8.75, 6.25, 6.25, 6.25, 5]
TRANSITION = .5
TOTAL = sum(DURATIONS) - TRANSITION * (len(DURATIONS) - 1)


def run(ffmpeg, *args):
    subprocess.run([ffmpeg, "-hide_banner", "-loglevel", "warning", "-filter_complex_threads", "2", "-filter_threads", "2", "-y", *map(str, args)], check=True)


def soundtrack(path):
    """Original warm electronic cue, 96 BPM, stereo, with pad/pluck/bass/percussion/reverb."""
    sr = 48000
    n = int(TOTAL * sr)
    audio = np.zeros((n, 2), np.float64)
    rng = np.random.default_rng(290626)

    def note(start, length, midi, amp, kind="pad", pan=0):
        count = min(int(length * sr), n - int(start * sr))
        if count <= 0:
            return
        t = np.arange(count) / sr
        f = 440 * 2 ** ((midi - 69) / 12)
        if kind == "pad":
            env = np.minimum(t / .7, 1) * np.minimum((length - t) / 1.1, 1).clip(0, 1)
            sound = (np.sin(2 * np.pi * f * t) + .28 * np.sin(2 * np.pi * f * 1.002 * t) + .15 * np.sin(2 * np.pi * 2 * f * t)) * env
        elif kind == "pluck":
            sound = (np.sin(2 * np.pi * f * t) + .32 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 3) + .13 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t * 7)) * np.exp(-t * 2.3) * np.minimum(t / .008, 1)
        else:
            sound = (np.sin(2 * np.pi * f * t) + .16 * np.sin(4 * np.pi * f * t)) * np.minimum(t / .03, 1) * np.minimum((length - t) / .18, 1).clip(0, 1)
        i = int(start * sr)
        audio[i:i + count] += amp * sound[:, None] * np.array([math.sqrt((1 - pan) / 2), math.sqrt((1 + pan) / 2)])

    chords = [[57, 60, 64, 71], [53, 57, 60, 64], [48, 55, 59, 64], [55, 59, 62, 69]]
    melody = [[76, 71, 72, 64, 67], [72, 69, 76, 72, 64], [71, 67, 74, 71, 67], [74, 71, 69, 67, 64]]
    beat = .625
    for bar in range(math.ceil(TOTAL / 2.5)):
        start = bar * 2.5
        chord = chords[(bar // 2) % 4]
        for j, midi in enumerate(chord):
            note(start, 3.4, midi, .065, pan=-.55 + j * .36)
        note(start, 1.65, chord[0] - 12, .13, "bass")
        note(start + 1.875, .6, chord[0] - 12, .075, "bass")
        if bar > 0:
            for j, midi in enumerate(melody[(bar // 2) % 4]):
                note(start + [0, .625, .9375, 1.5625, 2.1875][j], 1.6, midi, .13 if j == 0 else .09, "pluck", -.3 if j % 2 else .3)
        for b in range(4):
            i = int((start + b * beat) * sr)
            count = min(int(.28 * sr), n - i)
            t = np.arange(count) / sr
            kick = np.sin(2 * np.pi * (49 * t + 1.7 * (1 - np.exp(-t * 22)))) * np.exp(-t * 19) * .115
            audio[i:i + count] += kick[:, None]
            if bar >= 3:
                i = int((start + b * beat + .3125) * sr)
                count = min(int(.095 * sr), n - i)
                noise = rng.normal(0, 1, count)
                high = noise - np.r_[0, noise[:-1]]
                audio[i:i + count] += (high * np.exp(-np.arange(count) / sr * 65) * .012)[:, None]
    dry = audio.copy()
    for delay, gain in [(.15625, .20), (.3125, .15), (.46875, .10), (.78, .07), (1.09, .04)]:
        shift = int(delay * sr)
        audio[shift:] += dry[:-shift, ::-1] * gain
    t = np.arange(n) / sr
    envelope = np.minimum(t / 1.5, 1) * np.minimum((TOTAL - t) / 2.8, 1).clip(0, 1)
    audio = np.tanh(audio * 1.4) * envelope[:, None]
    audio *= .88 / max(float(np.max(np.abs(audio))), .001)
    with wave.open(str(path), "wb") as out:
        out.setnchannels(2)
        out.setsampwidth(2)
        out.setframerate(sr)
        out.writeframes((audio * 32767).astype("<i2").tobytes())


def font(a, size, bold=False):
    return ImageFont.truetype(str(a.bold_font if bold else a.font), size)


def plate(a, chapter, vertical=False):
    w, h = (1080, 1920) if vertical else (1920, 1080)
    dark = chapter == 5
    ink = "#eaf2e9" if dark else "#173b3c"
    muted = "#a0bab2" if dark else "#5f7b76"
    accent = "#a9d7c0" if dark else "#387a69"
    yy, xx = np.mgrid[0:h, 0:w]
    g = np.exp(-(((xx - w * .83) / (w * .7)) ** 2 + ((yy - h * .48) / (h * .9)) ** 2))
    base, tint = ((np.array([12, 32, 35]), np.array([8, 27, 23])) if dark else (np.array([247, 246, 239]), np.array([-21, -9, -17])))
    arr = (base + g[..., None] * tint).clip(0, 255).astype(np.uint8)
    im = Image.fromarray(arr, "RGB").convert("RGBA")
    draw = ImageDraw.Draw(im)
    margin = 76 if vertical else 112
    draw.text((margin, 54), "RESONANCE", font=font(a, 27, True), fill=ink)
    draw.text((w - margin, 60), "听 见 此 刻", font=font(a, 20), fill=muted, anchor="ra")
    draw.line((margin, 108, w - margin, 108), fill=(130, 159, 148, 65), width=1)
    # Quiet waveform engraving, deliberately outside the product viewport.
    for row in range(13):
        points = []
        for x in range(0, w + 2, 5):
            y = h * (.75 if not vertical else .85) + row * 8 + math.sin(x / 130 + row * .22) * (18 + 12 * math.sin(x / 510))
            points.append((x, y))
        draw.line(points, fill=(31, 66, 60) if dark else (199, 215, 201), width=1)
    if chapter in (0, 6):
        cx, cy = w // 2, (720 if vertical else 395)
        for radius in (96, 118, 153, 205):
            draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), outline=(64, 129, 106, 45), width=1)
        for i, bh in enumerate([28, 55, 90, 130, 76, 42, 66, 106, 54]):
            x = cx - 96 + i * 24
            draw.rounded_rectangle((x, cy - bh / 2, x + 9, cy + bh / 2), radius=5, fill=accent)
        draw.text((cx, cy + 245), "Resonance", font=font(a, 100 if not vertical else 96), fill=ink, anchor="mm")
        draw.text((cx, cy + 350), "此刻，听见共鸣。" if chapter == 0 else "让音乐，回到你身边。", font=font(a, 39), fill=muted, anchor="mm")
        if chapter == 6:
            draw.text((cx, cy + 452), "Android   ·   Windows", font=font(a, 26), fill=accent, anchor="mm")
            draw.text((cx, cy + 513), "leonard.asia", font=font(a, 25), fill=muted, anchor="mm")
        box = None
    else:
        titles = {1: ("01 / 此刻", ["把此刻", "交给旋律。"], "封面氛围随音乐流动", "原生播放器  /  细腻波浪进度"),
                  2: ("02 / 读懂", ["每一句", "都在眼前。"], "同步滚动，让当前句始终居中", "原文与译文  /  长句完整呈现"),
                  3: ("03 / 自在", ["随心翻阅", "轻触即归。"], "想多看一句，就停留片刻", "手动浏览  /  一键回到当前播放"),
                  4: ("04 / 随心", ["下一首", "由你决定。"], "喜欢的旋律，排在前面", "播放队列  /  下一首播放与调序"),
                  5: ("05 / 沉浸", ["白昼与夜晚", "都有共鸣。"], "让喜欢的声音，陪伴此刻", "浅色与深色  /  自适应封面氛围")}
        label, lines, description, detail = titles[chapter]
        tx = margin
        ty = 150 if vertical else 260
        draw.text((tx, ty), label, font=font(a, 24), fill=accent)
        for i, line in enumerate(lines):
            draw.text((tx - 4, ty + 65 + i * (109 if not vertical else 78)), line, font=font(a, 90 if not vertical else 66, True), fill=ink)
        dy = ty + (330 if not vertical else 250)
        draw.text((tx, dy), description, font=font(a, 29 if not vertical else 28), fill=muted)
        draw.text((tx, dy + 60), detail, font=font(a, 23), fill=accent)
        if vertical:
            sw, sh, sx, sy = 702, 1248, 189, 560
        else:
            sw, sh, sx, sy = 459, 816, 1256, 138
        # Physical frame, restrained shadow, no invented controls over the native footage.
        shadow = Image.new("RGBA", (w, h))
        sd = ImageDraw.Draw(shadow)
        sd.rounded_rectangle((sx - 19, sy + 8, sx + sw + 19, sy + sh + 34), radius=49, fill=(5, 23, 24, 85))
        im = Image.alpha_composite(im, shadow.filter(ImageFilter.GaussianBlur(24)))
        draw = ImageDraw.Draw(im)
        draw.rounded_rectangle((sx - 14, sy - 14, sx + sw + 14, sy + sh + 14), radius=43, fill="#193033", outline="#68817c", width=2)
        box = (sx, sy, sw, sh)
    draw.text((margin, h - 66), "NATIVE MUSIC PLAYER", font=font(a, 19), fill=muted)
    draw.text((w - margin, h - 66), "MuMu 模拟器实录 · 原创演示数据", font=font(a, 19), fill=muted, anchor="ra")
    return im.convert("RGB"), box


def take_marks(folder, theme):
    meta = json.loads((folder / f"capture-{theme}.json").read_text(encoding="utf-8"))
    marks = dict(line.split(",") for line in (folder / f"promo-{theme}-marks.csv").read_text().splitlines())
    # The camera rig writes its monotonic epoch, allowing host-independent cut positions.
    offset = float(marks.pop("epoch")) / 1000 - meta["recorder_start_uptime"]
    return {key: float(value) / 1000 + offset for key, value in marks.items()}


def render(a, vertical=False):
    kind = "vertical" if vertical else "landscape"
    work = a.output / kind
    work.mkdir(exist_ok=True)
    light, dark = take_marks(a.output, "light"), take_marks(a.output, "dark")
    cuts = {1: ("light", light["cover"] - .2), 2: ("light", light["lyrics"] + .5),
            3: ("light", light["browse"] - .75), 4: ("light", light["queue"] + .4), 5: ("dark", dark["next"] - .5)}
    clips = []
    for chapter, duration in enumerate(DURATIONS):
        im, box = plate(a, chapter, vertical)
        image_path = work / f"plate-{chapter}.png"
        im.save(image_path)
        clip = work / f"chapter-{chapter}.mp4"
        clips.append(clip)
        if a.assemble_only:
            if not clip.is_file() or clip.stat().st_size == 0:
                raise RuntimeError(f"Missing rendered chapter: {clip}")
            continue
        common = ["-c:v", "libx264", "-preset", "medium", "-crf", "17", "-pix_fmt", "yuv420p", "-threads", "4", "-an", "-t", duration, "-r", "30", "-movflags", "+faststart", clip]
        if box is None:
            # Subtle physical reveal, with typography and sound leading the title card.
            width, height = im.size
            run(a.ffmpeg, "-loop", "1", "-framerate", "30", "-i", image_path, "-vf",
                f"zoompan=z='1.015-0.015*min(on/{duration * 30},1)':x='iw/2-iw/zoom/2':y='ih/2-ih/zoom/2':d=1:s={width}x{height}:fps=30,setsar=1", *common)
        else:
            sx, sy, sw, sh = box
            theme, start = cuts[chapter]
            # Full recording resized to a phone-shaped viewport; system edge cropped by --crop-top/bottom.
            mask_path = work / "screen-mask.png"
            mask = Image.new("L", (sw, sh))
            ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, sh - 1), radius=30, fill=255)
            mask.save(mask_path)
            crop = f"crop=iw:ih-{a.crop_top + a.crop_bottom}:0:{a.crop_top},scale={sw}:{sh}:flags=lanczos,setsar=1,fps=30"
            graph = f"[1:v]{crop}[screen];[screen][2:v]alphamerge[rounded];[0:v][rounded]overlay={sx}:{sy}:shortest=1,format=yuv420p[v]"
            run(a.ffmpeg, "-loop", "1", "-framerate", "30", "-i", image_path, "-ss", start, "-analyzeduration", "1000000", "-probesize", "1048576", "-i", a.output / f"raw-{theme}.mp4", "-loop", "1", "-i", mask_path,
                "-filter_complex", graph, "-map", "[v]", *common)
        print(f"Rendered {kind} chapter {chapter}", flush=True)
    inputs = [entry for clip in clips for entry in ("-i", clip)]
    filters, previous, offset = [], "0:v", DURATIONS[0] - TRANSITION
    for i in range(1, len(clips)):
        target = f"x{i}"
        filters.append(f"[{previous}][{i}:v]xfade=transition=fade:duration={TRANSITION}:offset={offset}[{target}]")
        previous = target
        offset += DURATIONS[i] - TRANSITION
    filters.append(f"[{previous}]fade=t=in:d=0.4,fade=t=out:st={TOTAL - .6}:d=0.6[v]")
    filters.append(f"[{len(clips)}:a]loudnorm=I=-16:TP=-1.2:LRA=8,afade=t=in:d=0.3,afade=t=out:st={TOTAL - 1.8}:d=1.8[a]")
    final = a.output / f"Resonance-{kind}-40s.mp4"
    run(a.ffmpeg, *inputs, "-i", a.output / "soundtrack-original.wav", "-filter_complex", ";".join(filters), "-map", "[v]", "-map", "[a]",
        "-c:v", "libx264", "-preset", "slow", "-crf", "17", "-threads", "4", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "256k", "-ar", "48000", "-t", TOTAL, "-movflags", "+faststart", final)
    return final


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--ffmpeg", required=True)
    p.add_argument("--font", type=Path, required=True)
    p.add_argument("--bold-font", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--crop-top", type=int, default=0)
    p.add_argument("--crop-bottom", type=int, default=0)
    p.add_argument("--format", choices=["landscape", "vertical", "both"], default="both")
    p.add_argument("--prepare-only", action="store_true")
    p.add_argument("--assemble-only", action="store_true", help="Reuse this run's rendered chapters; only when footage, cuts and design are unchanged")
    a = p.parse_args()
    a.output.mkdir(parents=True, exist_ok=True)
    soundtrack(a.output / "soundtrack-original.wav")
    if a.prepare_only:
        for vertical in (False, True):
            plate(a, 2, vertical)[0].save(a.output / f"design-{'vertical' if vertical else 'landscape'}.png")
        print(f"Original soundtrack and design plates prepared, duration {TOTAL}s")
        return
    outputs = []
    for vertical in (False, True):
        if a.format in ("both", "vertical" if vertical else "landscape"):
            outputs.append(str(render(a, vertical)))
    (a.output / "production.json").write_text(json.dumps({"duration_seconds": TOTAL, "fps": 30, "original_soundtrack": True, "source": "MuMu native Compose recording", "outputs": outputs}, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
