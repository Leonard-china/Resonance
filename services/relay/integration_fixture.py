"""Loopback-only JVM integration fixture. Never use as a public server."""
import asyncio
import tempfile
from pathlib import Path
from websockets.asyncio.server import serve
from websocket_server import DailyBudget, PublicRelay


async def main():
    with tempfile.TemporaryDirectory() as directory:
        relay = PublicRelay(DailyBudget(Path(directory) / "budget.json"))
        async with serve(relay.handle, "127.0.0.1", 0, compression=None, max_size=65536) as server:
            print(server.sockets[0].getsockname()[1], flush=True)
            await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
