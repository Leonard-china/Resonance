"""Optional opaque TCP relay. Run only with an operator-provided TLS certificate and secret.

The application protocol and its TLS handshake remain inside this tunnel. Room routing IDs
are one-way digests of invitation capabilities; neither chat nor audio is parsed or saved.
"""
import argparse
import asyncio
import hmac
import json
import os
import re
import ssl
import time
from dataclasses import dataclass

ROUTE = re.compile(r"[a-f0-9]{64}\Z")
SECRET = re.compile(r"[A-Za-z0-9_-]{43}\Z")


@dataclass
class WaitingRoom:
    peer: asyncio.Future
    finished: asyncio.Future


class Relay:
    def __init__(self, secret, max_connections=128, max_rooms=32, wait_seconds=90,
                 idle_seconds=180, max_seconds=43200, max_bytes=4 * 1024**3):
        if not SECRET.fullmatch(secret):
            raise ValueError("RESONANCE_RELAY_SECRET must be a 32-byte base64url capability")
        self.secret = secret
        self.max_connections = max_connections
        self.max_rooms = max_rooms
        self.wait_seconds = wait_seconds
        self.idle_seconds = idle_seconds
        self.max_seconds = max_seconds
        self.max_bytes = max_bytes
        self.connections = 0
        self.waiting = {}

    async def respond(self, writer, status):
        writer.write(json.dumps({"status": status}, separators=(",", ":")).encode("ascii") + b"\n")
        await writer.drain()

    async def bridge(self, left_reader, left_writer, right_reader, right_writer):
        last_activity = time.monotonic()
        started = last_activity
        transferred = 0

        async def pump(reader, writer):
            nonlocal last_activity, transferred
            while True:
                data = await reader.read(65536)
                if not data:
                    return
                transferred += len(data)
                if transferred > self.max_bytes:
                    return
                last_activity = time.monotonic()
                writer.write(data)
                await writer.drain()

        async def watchdog():
            while True:
                await asyncio.sleep(5)
                now = time.monotonic()
                if now - started >= self.max_seconds or now - last_activity >= self.idle_seconds:
                    return

        tasks = [asyncio.create_task(pump(left_reader, right_writer)),
                 asyncio.create_task(pump(right_reader, left_writer)),
                 asyncio.create_task(watchdog())]
        try:
            await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
        finally:
            for task in tasks:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)

    async def handle(self, reader, writer):
        if self.connections >= self.max_connections:
            writer.close()
            await writer.wait_closed()
            return
        self.connections += 1
        route = None
        room = None
        try:
            line = await asyncio.wait_for(reader.readline(), timeout=10)
            if not line.endswith(b"\n") or len(line) > 1024:
                return
            request = json.loads(line.decode("utf-8"))
            if not isinstance(request, dict) or request.get("version") != 1:
                return
            route = request.get("route")
            role = request.get("role")
            if not isinstance(route, str) or not ROUTE.fullmatch(route):
                return
            if role == "host":
                token = request.get("token")
                if not isinstance(token, str) or not SECRET.fullmatch(token) or not hmac.compare_digest(token, self.secret):
                    return
                rooms = self.waiting.get(route)
                if rooms is None:
                    if len(self.waiting) >= self.max_rooms:
                        await self.respond(writer, "unavailable")
                        return
                    rooms = []
                    self.waiting[route] = rooms
                if len(rooms) >= 8:
                    await self.respond(writer, "unavailable")
                    return
                loop = asyncio.get_running_loop()
                room = WaitingRoom(loop.create_future(), loop.create_future())
                rooms.append(room)
                await self.respond(writer, "waiting")
                try:
                    other_reader, other_writer = await asyncio.wait_for(room.peer, self.wait_seconds)
                except asyncio.TimeoutError:
                    return
                finally:
                    if room in rooms:
                        rooms.remove(room)
                    if not rooms and self.waiting.get(route) is rooms:
                        self.waiting.pop(route, None)
                try:
                    await self.respond(writer, "paired")
                    await self.respond(other_writer, "paired")
                    await self.bridge(reader, writer, other_reader, other_writer)
                finally:
                    if not room.finished.done():
                        room.finished.set_result(None)
            elif role == "guest":
                rooms = self.waiting.get(route)
                if not rooms:
                    await self.respond(writer, "unavailable")
                    return
                # Claim the earliest waiting host connection for this room route.
                while rooms and rooms[0].peer.done():
                    rooms.pop(0)
                if not rooms:
                    if self.waiting.get(route) is rooms:
                        self.waiting.pop(route, None)
                    await self.respond(writer, "unavailable")
                    return
                room = rooms.pop(0)
                if not rooms and self.waiting.get(route) is rooms:
                    self.waiting.pop(route, None)
                room.peer.set_result((reader, writer))
                await room.finished
        except (asyncio.TimeoutError, ConnectionError, OSError, ValueError, UnicodeError, json.JSONDecodeError):
            pass
        finally:
            if locals().get("role") == "host" and room is not None:
                rooms = self.waiting.get(route)
                if rooms is not None and room in rooms:
                    rooms.remove(room)
                if rooms is not None and not rooms and self.waiting.get(route) is rooms:
                    self.waiting.pop(route, None)
                if not room.finished.done():
                    room.finished.set_result(None)
            writer.close()
            try:
                await writer.wait_closed()
            except (OSError, ssl.SSLError):
                pass
            self.connections -= 1


async def serve(args):
    relay = Relay(os.environ.get("RESONANCE_RELAY_SECRET", ""), args.max_connections,
                  args.max_rooms, args.wait_seconds, args.idle_seconds, args.max_seconds, args.max_bytes)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(args.cert, args.key)
    server = await asyncio.start_server(relay.handle, args.host, args.port, ssl=context, limit=2048,
                                        ssl_handshake_timeout=10)
    print("Resonance relay listening", flush=True)
    async with server:
        await server.serve_forever()


def main():
    parser = argparse.ArgumentParser(description="Optional encrypted-byte relay; disabled until explicitly run")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=45901)
    parser.add_argument("--cert", required=True)
    parser.add_argument("--key", required=True)
    parser.add_argument("--max-connections", type=int, default=128)
    parser.add_argument("--max-rooms", type=int, default=32)
    parser.add_argument("--wait-seconds", type=int, default=90)
    parser.add_argument("--idle-seconds", type=int, default=180)
    parser.add_argument("--max-seconds", type=int, default=43200)
    parser.add_argument("--max-bytes", type=int, default=4 * 1024**3)
    args = parser.parse_args()
    if not (1 <= args.port <= 65535 and 2 <= args.max_connections <= 10000 and
            1 <= args.max_rooms <= args.max_connections // 2 and
            10 <= args.wait_seconds <= 600 and 30 <= args.idle_seconds <= 3600 and
            60 <= args.max_seconds <= 86400 and 1024 <= args.max_bytes <= 1024**4):
        parser.error("Invalid relay resource limits")
    asyncio.run(serve(args))


if __name__ == "__main__":
    main()
