import asyncio
import json
import tempfile
import unittest
from pathlib import Path

from websockets.asyncio.client import connect
from websockets.asyncio.server import serve
from websocket_server import DailyBudget, PublicRelay, PATH


class RelayTest(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.budget = DailyBudget(Path(self.temp.name) / "budget.json", 1024**2)
        self.relay = PublicRelay(self.budget, wait_seconds=.2, session_bytes=100)
        self.server = await serve(self.relay.handle, "127.0.0.1", 0, max_size=65536, compression=None)
        self.url = f"ws://127.0.0.1:{self.server.sockets[0].getsockname()[1]}{PATH}"

    async def asyncTearDown(self):
        self.server.close()
        await self.server.wait_closed()
        self.temp.cleanup()

    async def hello(self, role, route="a" * 64):
        ws = await connect(self.url, proxy=None)
        await ws.send(json.dumps(dict(version=1, role=role, route=route)).encode())
        return ws

    async def test_pair_bidirectional_bytes_and_cleanup(self):
        host = await self.hello("host")
        self.assertIn(b"waiting", await host.recv())
        guest = await self.hello("guest")
        self.assertIn(b"paired", await host.recv())
        self.assertIn(b"paired", await guest.recv())
        await host.send(b"encrypted-fixture")
        self.assertEqual(b"encrypted-fixture", await guest.recv())
        await guest.send(b"reply")
        self.assertEqual(b"reply", await host.recv())
        await host.close()
        await asyncio.wait_for(guest.wait_closed(), 2)
        await asyncio.sleep(.02)
        self.assertEqual({}, self.relay.waiting)
        self.assertEqual(0, self.relay.connections)

    async def test_invalid_hello_and_wait_timeout_release_slots(self):
        invalid = await self.hello("host", "invalid")
        await asyncio.wait_for(invalid.wait_closed(), 2)
        host = await self.hello("host")
        await host.recv()
        await asyncio.wait_for(host.wait_closed(), 2)
        self.assertEqual({}, self.relay.waiting)
        self.assertEqual(0, self.relay.connections)

    async def test_session_limit_closes_both_peers(self):
        host = await self.hello("host")
        await host.recv()
        guest = await self.hello("guest")
        await host.recv(); await guest.recv()
        await host.send(b"x" * 101)
        await asyncio.wait_for(guest.wait_closed(), 2)
        await asyncio.wait_for(host.wait_closed(), 2)

    async def test_capacity_rejects_without_success(self):
        self.relay.max_waiting = 1
        host = await self.hello("host")
        await host.recv()
        other = await self.hello("host", "b" * 64)
        self.assertIn(b"unavailable", await other.recv())
        await other.close(); await host.close()

    async def test_nginx_clients_have_separate_ip_budgets(self):
        self.relay.trust_nginx = True
        clients = []
        try:
            for address in ("192.0.2.1", "192.0.2.2"):
                ws = await connect(self.url, proxy=None, additional_headers={
                    "X-Real-IP": address, "CF-Connecting-IP": "198.51.100.1"})
                clients.append(ws)
                await ws.send(json.dumps(dict(version=1, role="host", route="c" * 64)).encode())
                self.assertIn(b"waiting", await ws.recv())
            self.assertEqual({"192.0.2.1": 1, "192.0.2.2": 1}, dict(self.relay.by_ip))
        finally:
            for ws in clients:
                await ws.close()

    async def test_default_does_not_trust_client_ip_headers(self):
        ws = await connect(self.url, proxy=None, additional_headers={
            "X-Real-IP": "192.0.2.1", "CF-Connecting-IP": "198.51.100.1"})
        try:
            await ws.send(json.dumps(dict(version=1, role="host", route="d" * 64)).encode())
            self.assertIn(b"waiting", await ws.recv())
            self.assertEqual({"127.0.0.1": 1}, dict(self.relay.by_ip))
        finally:
            await ws.close()

    async def test_corrupt_budget_is_preserved_and_refuses_startup(self):
        raw = '{"day":"invalid","reserved":0}'
        self.budget.path.write_text(raw)
        with self.assertRaises(ValueError):
            DailyBudget(self.budget.path, self.budget.limit)
        self.assertEqual(raw, self.budget.path.read_text())

    async def test_durable_budget_cannot_reset_on_restart(self):
        self.assertTrue(self.budget.consume(20))
        restarted = DailyBudget(self.budget.path, self.budget.limit)
        self.assertFalse(restarted.consume(1))  # unspent reservation is conservatively charged
        self.assertTrue(self.budget.consume(20))


if __name__ == "__main__":
    unittest.main()
