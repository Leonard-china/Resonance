"""Record the opt-in native Compose camera rig on an explicitly selected Android emulator."""
import argparse
import json
import subprocess
import time
import uuid
from pathlib import Path


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--adb", required=True)
    p.add_argument("--serial", required=True)
    p.add_argument("--theme", choices=["light", "dark"], required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--package", default="com.resonance.player.codex", choices=["com.resonance.player.codex", "com.resonance.player.debug"])
    a = p.parse_args()
    a.output.mkdir(parents=True, exist_ok=True)
    base = [a.adb, "-s", a.serial]
    take = a.theme
    remote = f"/sdcard/resonance-promo-{take}-{uuid.uuid4().hex}.mp4"
    old_rotation = {key: subprocess.check_output(base + ["shell", "settings", "get", "system", key], text=True).strip()
                    for key in ("accelerometer_rotation", "user_rotation")}
    # Prevent MuMu sensor rotation from recreating the test host during a take. Restore afterwards.
    subprocess.run(base + ["shell", "settings", "put", "system", "accelerometer_rotation", "0"], check=True)
    subprocess.run(base + ["shell", "settings", "put", "system", "user_rotation", "0"], check=True)
    time.sleep(.8)
    uptime = float(subprocess.check_output(base + ["shell", "cat", "/proc/uptime"], text=True).split()[0])
    recorder = subprocess.Popen(base + ["shell", f"echo $$; exec screenrecord --size 1080x1920 --bit-rate 16000000 --time-limit 180 {remote}"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    recorder_pid = recorder.stdout.readline().decode("ascii").strip()
    if not recorder_pid.isdecimal():
        recorder.terminate()
        for key, value in old_rotation.items():
            subprocess.run(base + ["shell", "settings", "put", "system", key, value], capture_output=True)
        raise RuntimeError("Could not identify the owned screen recorder")
    try:
        time.sleep(1)
        rig = "PromoCaptureDeviceTest"
        flag = "capturePromo"
        result = subprocess.run(base + ["shell", "am", "instrument", "-w", "-r", "-e", "class", "com.resonance.player." + rig, "-e", flag, "true", "-e", "promoTheme", a.theme, a.package + ".test/androidx.test.runner.AndroidJUnitRunner"], capture_output=True, text=True, encoding="utf-8", timeout=240)
        (a.output / f"capture-{take}.log").write_text(result.stdout + result.stderr, encoding="utf-8")
        if "OK (1 test)" not in result.stdout:
            raise RuntimeError(f"Capture scenario failed; see capture-{take}.log")
    finally:
        command = subprocess.run(base + ["shell", "cat", f"/proc/{recorder_pid}/comm"], capture_output=True, text=True)
        if command.stdout.strip() == "screenrecord":
            subprocess.run(base + ["shell", "kill", "-INT", recorder_pid], capture_output=True)
        try:
            recorder.communicate(timeout=12)
        except subprocess.TimeoutExpired:
            recorder.terminate()
            recorder.communicate(timeout=5)
        for key, value in old_rotation.items():
            subprocess.run(base + ["shell", "settings", "put", "system", key, value], capture_output=True)
    subprocess.run(base + ["pull", remote, str(a.output / f"raw-{take}.mp4")], check=True)
    subprocess.run(base + ["pull", f"/sdcard/Android/data/{a.package}/files/promo-{take}-marks.csv", str(a.output)], check=True)
    subprocess.run(base + ["shell", "rm", remote], check=True)
    (a.output / f"capture-{take}.json").write_text(json.dumps({"serial": a.serial, "theme": a.theme, "package": a.package, "recorder_start_uptime": uptime, "capture": "native Compose controls, original fixture data", "ai_live_reply": False}, indent=2), encoding="utf-8")
    print(f"Native {take} take saved to {a.output}")


if __name__ == "__main__":
    main()
