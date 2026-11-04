"""Public, bounded WSS relay behind a local HTTPS proxy. No audio/chat plaintext.

Bind to loopback only. Route just /resonance-relay/v1 here; leave the website upstream alone.
The clients authenticate each other with the existing pinned TLS inside binary WS frames.
"""
import argparse
import asyncio
import collections
import datetime
import json
import os
import re
import time
from dataclasses import dataclass
from pathlib import Path

from websockets.asyncio.server import serve
from websockets.exceptions import ConnectionClosed

PATH = "/resonance-relay/v1"
ROUTE = re.compile(r"[a-f0-9]{64}\Z")
FRAME_LIMIT = 65536


class DailyBudget:
    """Reserve durable 1MiB blocks before forwarding. Restarts cannot reset today's cap."""
    def __init__(self, path, limit=2 * 1024**3):
        self.path = Path(path)
        self.limit = limit
        self.day = ""
        self.reserved = 0
        self.remaining = 0
        if self.path.exists():
            state = json.loads(self.path.read_text())
            self.day = state["day"]
            self.reserved = state["reserved"]
            datetime.date.fromisoformat(self.day)
            if type(self.reserved) is not int or self.reserved < 0:
                raise ValueError("Invalid budget state; preserve file and investigate")

    def consume(self, count):
        today = datetime.datetime.now(datetime.timezone.utc).date().isoformat()
        if self.day != today:
            self.day, self.reserved, self.remaining = today, 0, 0
        if count > self.remaining:
            block = max(count, min(1024**2, self.limit - self.reserved))
            if self.reserved + block > self.limit:
                return False
            self.path.parent.mkdir(parents=True, exist_ok=True)
            temporary = self.path.with_suffix(".tmp")
            with temporary.open("w") as file:
                json.dump({"day": self.day, "reserved": self.reserved + block}, file)
                file.flush()
                os.fsync(file.fileno())
            os.replace(temporary, self.path)
            if os.name == "posix":
                descriptor = os.open(self.path.parent, os.O_DIRECTORY)
                try: os.fsync(descriptor)
                finally: os.close(descriptor)
            self.reserved += block
            self.remaining += block
        self.remaining -= count
        return True


@dataclass(eq=False)
class Waiting:
    peer: asyncio.Future
    done: asyncio.Future


class PublicRelay:
    def __init__(self, budget, max_connections=64, max_waiting=32, wait_seconds=90,
                 session_seconds=7200, idle_seconds=120, session_bytes=512 * 1024**2,
                 trust_cloudflare=False, trust_nginx=False):
        self.budget = budget
        self.max_connections, self.max_waiting = max_connections, max_waiting
        self.wait_seconds, self.session_seconds = wait_seconds, session_seconds
        self.idle_seconds, self.session_bytes = idle_seconds, session_bytes
        self.trust_cloudflare = trust_cloudflare
        self.trust_nginx = trust_nginx
        self.connections = 0
        self.by_ip = collections.Counter()
        self.attempts = {}
        self.waiting = {}

    async def status(self, ws, status):
        await ws.send((json.dumps({"status": status}) + "\n").encode())

    async def bridge(self, left, right):
        transferred = 0
        async def pump(source, destination):
            nonlocal transferred
            while True:
                data = await asyncio.wait_for(source.recv(), self.idle_seconds)
                if not isinstance(data, bytes) or len(data) > FRAME_LIMIT:
                    raise ValueError("Invalid binary frame")
                transferred += len(data)
                if transferred > self.session_bytes or not self.budget.consume(len(data)):
                    raise ValueError("Relay transfer limit")
                await asyncio.wait_for(destination.send(data), 15)
        tasks = [asyncio.create_task(pump(left, right)), asyncio.create_task(pump(right, left))]
        try:
            done, _ = await asyncio.wait(tasks, timeout=self.session_seconds, return_when=asyncio.FIRST_COMPLETED)
            for task in done:
                task.result()  # Preserve failure classification for the sanitized operational log.
            if not done:
                raise TimeoutError("Session lifetime exceeded")
        finally:
            for task in tasks:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)

    async def handle(self, ws):
        if ws.request.path != PATH:
            await ws.close(1008, "Unknown relay path")
            return
        ip = ws.remote_address[0]
        # The local proxy must overwrite this header, never append client-supplied values.
        if self.trust_nginx:
            ip = ws.request.headers.get("X-Real-IP", ip)
        # Legacy tunnel deployments remain supported during migration.
        if self.trust_cloudflare:
            ip = ws.request.headers.get("CF-Connecting-IP", ip)
        now = time.monotonic()
        self.attempts = {key: value for key, value in self.attempts.items() if now - value[0] < 60}
        start, attempts = self.attempts.get(ip, (now, 0))
        if (self.connections >= self.max_connections or self.by_ip[ip] >= 16 or
                attempts >= 30 or (ip not in self.attempts and len(self.attempts) >= 1024)):
            await ws.close(1013, "Relay busy")
            return
        self.attempts[ip] = (start, attempts + 1)
        self.connections += 1
        self.by_ip[ip] += 1
        room, route, host = None, None, False
        try:
            raw = await asyncio.wait_for(ws.recv(), 10)
            if not isinstance(raw, bytes) or len(raw) > 1024:
                return
            hello = json.loads(raw)
            if not isinstance(hello, dict) or set(hello) != {"version", "role", "route"} or hello["version"] != 1:
                return
            route = hello["route"]
            if not isinstance(route, str) or not ROUTE.fullmatch(route):
                return
            host = hello["role"] == "host"
            if host:
                entries = self.waiting.get(route, [])
                if sum(map(len, self.waiting.values())) >= self.max_waiting or len(entries) >= 8:
                    await self.status(ws, "unavailable")
                    return
                loop = asyncio.get_running_loop()
                room = Waiting(loop.create_future(), loop.create_future())
                self.waiting.setdefault(route, []).append(room)
                await self.status(ws, "waiting")
                closed = asyncio.create_task(ws.wait_closed())
                try:
                    done, _ = await asyncio.wait([room.peer, closed], timeout=self.wait_seconds,
                                                 return_when=asyncio.FIRST_COMPLETED)
                    if room.peer not in done or ws.close_code is not None:
                        if ws.close_code is None:
                            await self.status(ws, "expired")
                        return
                    other = room.peer.result()
                finally:
                    closed.cancel()
                    await asyncio.gather(closed, return_exceptions=True)
                await self.status(ws, "paired")
                await self.status(other, "paired")
                await self.bridge(ws, other)
            elif hello["role"] == "guest":
                entries = self.waiting.get(route, [])
                if not entries:
                    await self.status(ws, "unavailable")
                    return
                room = entries.pop(0)
                if not entries:
                    self.waiting.pop(route, None)
                room.peer.set_result(ws)
                await asyncio.shield(room.done)
        except (ConnectionClosed, TimeoutError, ValueError, OSError, KeyError, TypeError) as failure:
            # Class only: no address, invitation, audio, chat or exception contents.
            print("relay_connection_closed reason=" + type(failure).__name__, flush=True)
        finally:
            if host and room is not None:
                entries = self.waiting.get(route, [])
                if room in entries:
                    entries.remove(room)
                if not entries:
                    self.waiting.pop(route, None)
                if not room.done.done():
                    room.done.set_result(None)
            self.connections -= 1
            self.by_ip[ip] -= 1
            if not self.by_ip[ip]:
                del self.by_ip[ip]
            await ws.close()


async def run(args):
    relay = PublicRelay(DailyBudget(args.budget_file, args.daily_bytes),
                        trust_cloudflare=args.trust_cloudflare, trust_nginx=args.trust_nginx)
    async with serve(relay.handle, "127.0.0.1", args.port, origins=[None], compression=None,
                     max_size=FRAME_LIMIT, max_queue=4, write_limit=FRAME_LIMIT,
                     open_timeout=10, close_timeout=3, ping_interval=20, ping_timeout=20):
        print("Resonance public relay ready on loopback", flush=True)
        await asyncio.Future()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=45902)
    parser.add_argument("--budget-file", required=True)
    parser.add_argument("--daily-bytes", type=int, default=2 * 1024**3)
    proxy = parser.add_mutually_exclusive_group()
    proxy.add_argument("--trust-cloudflare", action="store_true")
    proxy.add_argument("--trust-nginx", action="store_true")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535 or not 1024 <= args.daily_bytes <= 100 * 1024**3:
        parser.error("Invalid limits")
    asyncio.run(run(args))
