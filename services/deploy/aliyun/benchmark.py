"""Bounded, loopback-only deployment benchmark. Run with the relay venv as root.

Uses GET requests against the staging site. Nginx, relay data, TLS fixture and
relay unit are private test instances; production budgets and DNS are untouched.
"""
import asyncio
import concurrent.futures
import contextlib
import fcntl
import json
import math
import os
from pathlib import Path
import pwd
import secrets
import signal
import socket
import ssl
import subprocess
import tempfile
import threading
import time
import urllib.error
import urllib.request


ROOT = Path("/opt/resonance")
MIB = 1024 * 1024
PAGES = ("/", "/downloads", "/forum", "/support", "/static/site.css")


def stats(values):
    ordered = sorted(values)
    if not ordered:
        return {}
    return {name: round(ordered[min(len(ordered) - 1, math.ceil(len(ordered) * fraction) - 1)], 3)
            for name, fraction in (("median_ms", .5), ("p95_ms", .95), ("p99_ms", .99))}


def available_memory():
    for line in Path("/proc/meminfo").read_text().splitlines():
        if line.startswith("MemAvailable:"):
            return int(line.split()[1]) * 1024
    raise RuntimeError("No memory telemetry")


def request(base, context, path):
    started = time.perf_counter()
    try:
        if available_memory() < 96 * MIB:
            raise MemoryError("Low-memory test guard")
        req = urllib.request.Request(base + path, headers={"Host": "leonard.asia"})
        with urllib.request.urlopen(req, context=context, timeout=8) as response:
            body = response.read(MIB + 1)
            if response.status != 200 or len(body) > MIB:
                raise ValueError("Unexpected response")
        return (time.perf_counter() - started) * 1000, len(body), None
    except Exception as error:
        if isinstance(error, urllib.error.HTTPError):
            error.close()
        return (time.perf_counter() - started) * 1000, 0, type(error).__name__


def measure_web(base, context, concurrency, count):
    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool:
        rows = list(pool.map(lambda i: request(base, context, PAGES[i % len(PAGES)]), range(count)))
    errors = [row[2] for row in rows if row[2]]
    return dict(concurrency=concurrency, requests=count, errors=len(errors),
                error_types=sorted(set(errors)), **stats([row[0] for row in rows if not row[2]]),
                requests_per_second=round(count / (time.perf_counter() - started), 2))


def download(base, context, platform, paced=False):
    wanted = 16 * MIB
    req = urllib.request.Request(base + "/download/" + platform, headers={
        "Host": "leonard.asia", "Range": f"bytes=0-{wanted - 1}"})
    started = time.perf_counter()
    total = 0
    with urllib.request.urlopen(req, context=context, timeout=12) as response:
        if response.status != 206 or not response.headers.get("Content-Range", "").startswith(f"bytes 0-{wanted - 1}/"):
            raise ValueError("Invalid range response")
        for block in iter(lambda: response.read(128 * 1024), b""):
            total += len(block)
            if total > wanted:
                raise ValueError("Unbounded download")
            if paced:
                time.sleep(.025)
    if total != wanted:
        raise ValueError("Truncated download")
    return dict(platform=platform, bytes=total, mib_per_second=round(total / MIB / (time.perf_counter() - started), 2))


def mixed_downloads(base, context):
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        downloads = [pool.submit(download, base, context, platform, True) for platform in ("android", "windows")]
        pages = measure_web(base, context, 4, 80)
        return dict(pages=pages, downloads=[job.result() for job in downloads])


async def relay_pair(url, context, mode="burst"):
    from websockets.asyncio.client import connect
    route = secrets.token_hex(32)
    started = time.perf_counter()
    async with connect(url, ssl=context, proxy=None, compression=None, max_size=65536, max_queue=4) as host:
        await host.send(json.dumps(dict(version=1, role="host", route=route)).encode())
        if b"waiting" not in await host.recv():
            raise ValueError("Host not admitted")
        async with connect(url, ssl=context, proxy=None, compression=None, max_size=65536, max_queue=4) as guest:
            await guest.send(json.dumps(dict(version=1, role="guest", route=route)).encode())
            if b"paired" not in await host.recv() or b"paired" not in await guest.recv():
                raise ValueError("Guest not paired")
            paired_ms = (time.perf_counter() - started) * 1000
            times, sent, received, ticks = {}, 0, 0, []
            target_bytes = 0 if mode == "idle" else 512 * 1024 if mode == "paced" else 8 * MIB
            async def echo():
                async for frame in guest:
                    await guest.send(frame)
            async def consume():
                nonlocal received
                while received < target_bytes or len(ticks) < 20:
                    frame = await host.recv()
                    if frame.startswith(b"P:"):
                        ticks.append((time.perf_counter() - times.pop(frame)) * 1000)
                    elif frame == b"D" * 65536:
                        received += len(frame)
                    else:
                        raise ValueError("Corrupt forwarded bytes")
            async def bulk():
                nonlocal sent
                while sent < target_bytes:
                    await host.send(b"D" * 65536)
                    sent += 65536
                    if mode == "paced":
                        await asyncio.sleep(.25)
            async def probes():
                for i in range(20):
                    frame = f"P:{i}".encode()
                    times[frame] = time.perf_counter()
                    await host.send(frame)
                    await asyncio.sleep(.05)
            echo_task = asyncio.create_task(echo())
            transfer_started = time.perf_counter()
            try:
                await asyncio.wait_for(asyncio.gather(bulk(), probes(), consume()), 15)
            finally:
                echo_task.cancel()
                await asyncio.gather(echo_task, return_exceptions=True)
            return dict(pairing_ms=round(paired_ms, 3), bytes_round_trip=received,
                        # The interval also includes the deliberate 1-second probe schedule.
                        sustained_round_trip_mib_per_second=round(received / MIB / (time.perf_counter() - transfer_started), 2),
                        **stats(ticks))


async def measure_relay(url, context, steady=False):
    stages = []
    profiles = ((1, "idle"), (6, "paced")) if steady else ((1, "burst"), (3, "burst"), (6, "burst"))
    for pairs, mode in profiles:
        rows = await asyncio.gather(*(relay_pair(url, context, mode) for _ in range(pairs)), return_exceptions=True)
        good = [row for row in rows if isinstance(row, dict)]
        stages.append(dict(pairs=pairs, mode=mode, errors=len(rows) - len(good),
                           error_types=sorted({type(row).__name__ for row in rows if not isinstance(row, dict)}),
                           results=good))
    return stages


def run(*args):
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout.strip()


def group_for(unit):
    return Path("/sys/fs/cgroup") / run("systemctl", "show", unit, "-p", "ControlGroup", "--value").lstrip("/")


class Sampler:
    def __init__(self, units):
        self.groups = {unit: group_for(unit) for unit in units}
        self.peaks = dict.fromkeys(units, 0)
        self.oom_start = {unit: self.oom(group) for unit, group in self.groups.items()}
        self.cpu_start = {unit: self.cpu(group) for unit, group in self.groups.items()}
        self.started = time.perf_counter()
        self.min_available = available_memory()
        self.stop = threading.Event()
        self.thread = threading.Thread(target=self.sample, daemon=True)
    @staticmethod
    def oom(group):
        return dict(line.split() for line in (group / "memory.events").read_text().splitlines()).get("oom_kill", "0")
    @staticmethod
    def cpu(group):
        return int(dict(line.split() for line in (group / "cpu.stat").read_text().splitlines())["usage_usec"])
    def sample(self):
        while not self.stop.wait(.1):
            self.min_available = min(self.min_available, available_memory())
            for name, group in self.groups.items():
                with contextlib.suppress(OSError):
                    self.peaks[name] = max(self.peaks[name], int((group / "memory.current").read_text()))
    def finish(self):
        self.stop.set()
        self.thread.join(2)
        return dict(peak_memory_mib={name: round(value / MIB, 2) for name, value in self.peaks.items()},
                    minimum_host_available_mib=round(self.min_available / MIB, 2),
                    average_cpu_percent_of_one_core={name: round((self.cpu(group) - self.cpu_start[name]) / 10000 / (time.perf_counter() - self.started), 2) for name, group in self.groups.items()},
                    new_oom_kills={name: int(self.oom(group)) - int(self.oom_start[name]) for name, group in self.groups.items()})


def wait_port(port):
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        try:
            with socket.create_connection(("127.0.0.1", port), .3):
                return
        except OSError:
            time.sleep(.05)
    raise TimeoutError("Private test listener failed")


def main():
    if os.geteuid() != 0 or not (ROOT / "website/app.py").is_file():
        raise RuntimeError("Run on the prepared Ubuntu host as root")
    os.umask(0o077)
    process_lock = open("/run/lock/resonance-benchmark.lock", "a")
    fcntl.flock(process_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    for port in (18443, 45912):
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", port))
    signal.signal(signal.SIGALRM, lambda *_: (_ for _ in ()).throw(TimeoutError("Benchmark time limit")))
    signal.alarm(100)
    account = pwd.getpwnam("resonance")
    before = {name: run("systemctl", "show", name, "-p", "ActiveState", "-p", "NRestarts")
              for name in ("resonance-site", "resonance-relay", "resonance-mail", "nginx")}
    live_budget = Path("/var/lib/resonance-relay/budget.json")
    budget_before = live_budget.read_bytes() if live_budget.exists() else None
    name = "resonance-benchmark-" + secrets.token_hex(4)
    report = dict(kind="server-loopback-https-wss", user_audio_used=False, public_listener_created=False,
                  client_generator_on_same_host=True, website=[], downloads=[], relay=[])
    profile = os.environ.get("RESONANCE_BENCHMARK_PROFILE", "full")
    if profile not in ("full", "steady-relay"):
        raise ValueError("Unknown benchmark profile")
    report["profile"] = profile
    destination = Path("/root/resonance-migration") / (name + ".json")
    nginx = sampler = None
    with tempfile.TemporaryDirectory(prefix=name + "-", dir="/var/lib") as temporary:
        work = Path(temporary)
        os.chown(work, account.pw_uid, account.pw_gid)
        try:
            run("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1", "-subj", "/CN=localhost",
                "-addext", "subjectAltName=DNS:localhost,IP:127.0.0.1", "-keyout", str(work / "test.key"), "-out", str(work / "test.crt"))
            source = (ROOT / "website/deploy/nginx-aliyun.conf").read_text()
            source = source[source.index("server {", source.index("server {") + 1):]
            source = source.replace("listen 443 ssl;", "listen 127.0.0.1:18443 ssl;").replace("listen [::]:443 ssl;", "")
            source = source.replace("/etc/letsencrypt/live/leonard.asia/fullchain.pem", str(work / "test.crt"))
            source = source.replace("/etc/letsencrypt/live/leonard.asia/privkey.pem", str(work / "test.key"))
            source = source.replace("127.0.0.1:45902", "127.0.0.1:45912")
            (work / "site.conf").write_text(source)
            (work / "nginx.conf").write_text(f"user resonance;\nworker_processes 1;\npid {work}/nginx.pid;\nerror_log {work}/nginx.log warn;\nevents {{ worker_connections 256; }}\nhttp {{ access_log off; client_body_temp_path {work}/body; proxy_temp_path {work}/proxy; fastcgi_temp_path {work}/fastcgi; uwsgi_temp_path {work}/uwsgi; scgi_temp_path {work}/scgi; include {work}/site.conf; }}\n")
            run("systemd-run", "--quiet", "--collect", "--unit=" + name, "--property=User=resonance", "--property=Group=resonance",
                "--property=MemoryMax=128M", "--property=MemorySwapMax=0", "--property=TasksMax=32", "--property=RuntimeMaxSec=110",
                str(ROOT / "services/relay/.venv/bin/python"), str(ROOT / "services/relay/websocket_server.py"), "--port", "45912",
                "--budget-file", str(work / "budget.json"), "--daily-bytes", "2147483648", "--trust-nginx")
            wait_port(45912)
            run("nginx", "-t", "-p", str(work), "-c", str(work / "nginx.conf"))
            nginx = subprocess.Popen(["nginx", "-p", str(work), "-c", str(work / "nginx.conf"), "-g", "daemon off;"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            wait_port(18443)
            context = ssl.create_default_context(cafile=str(work / "test.crt"))
            base = "https://localhost:18443"
            sampler = Sampler(("resonance-site", "resonance-relay", name))
            sampler.thread.start()
            if profile == "full":
                for concurrency, count in ((1, 30), (4, 80), (12, 120)):
                    report["website"].append(measure_web(base, context, concurrency, count))
                report["downloads"] = [download(base, context, platform) for platform in ("android", "windows")]
                report["mixed"] = mixed_downloads(base, context)
            report["relay"] = asyncio.run(measure_relay("wss://localhost:18443/resonance-relay/v1", context, profile == "steady-relay"))
        except Exception as error:
            report["failure"] = type(error).__name__
        finally:
            if sampler:
                report["resources"] = sampler.finish()
            if nginx:
                nginx.terminate()
                try:
                    nginx.wait(5)
                except subprocess.TimeoutExpired:
                    nginx.kill()
                    nginx.wait()
            with contextlib.suppress(Exception):
                run("systemctl", "stop", name)
    report["services_unchanged"] = before == {unit: run("systemctl", "show", unit, "-p", "ActiveState", "-p", "NRestarts") for unit in before}
    report["production_budget_unchanged"] = budget_before == (live_budget.read_bytes() if live_budget.exists() else None)
    report["test_listeners_removed"] = True
    for port in (18443, 45912):
        try:
            with socket.create_connection(("127.0.0.1", port), .3):
                report["test_listeners_removed"] = False
        except OSError:
            pass
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2))
    print(json.dumps(report, separators=(",", ":")), flush=True)
    stages = report["website"] + report["relay"]
    if "mixed" in report:
        stages.append(report["mixed"]["pages"])
    if ("failure" in report or any(stage["errors"] for stage in stages)
            or not all(report[key] for key in ("services_unchanged", "production_budget_unchanged", "test_listeners_removed"))):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
