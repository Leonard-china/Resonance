import hashlib
import json
import threading
import unittest
import urllib.error
import urllib.request

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from server import Registry, RequestError, Server, b64, strict_json


class Device:
    def __init__(self):
        self.key = ec.generate_private_key(ec.SECP256R1())
        self.public = self.key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
        self.id = hashlib.sha256(self.public).hexdigest()

    def signed(self, action, payload, nonce):
        encoded = b64(json.dumps(payload, separators=(",", ":")).encode())
        signed = f"Resonance rendezvous v1\n{action}\n{nonce}\n{encoded}".encode()
        return {"publicKey": b64(self.public), "challenge": nonce, "payload": encoded,
                "signature": b64(self.key.sign(signed, ec.ECDSA(hashes.SHA256())))}

    def call(self, registry, action, payload):
        nonce = registry.challenge({"device": self.id})["challenge"]
        return registry.authenticated(action, self.signed(action, payload, nonce))

    def register(self, registry, partners=()):
        return self.call(registry, "register", {"addresses": ["127.0.0.1", "2001:db8::1"], "port": 45874, "partners": list(partners)})


class RegistryTest(unittest.TestCase):
    def setUp(self):
        self.now = 1000
        self.registry = Registry(clock=lambda: self.now, capacity=16)
        self.a, self.b, self.stranger = Device(), Device(), Device()

    def test_only_mutually_authorized_online_partners_can_lookup(self):
        self.a.register(self.registry, [self.b.id])
        self.b.register(self.registry, [])
        self.assertEqual({"reachable": False}, self.a.call(self.registry, "lookup", {"partner": self.b.id}))
        self.b.register(self.registry, [self.a.id])
        answer = self.a.call(self.registry, "lookup", {"partner": self.b.id})
        self.assertTrue(answer["reachable"])
        self.assertEqual(["127.0.0.1", "2001:db8::1"], answer["addresses"])
        self.stranger.register(self.registry, [self.b.id])
        self.assertEqual({"reachable": False}, self.stranger.call(self.registry, "lookup", {"partner": self.b.id}))

    def test_expiration_removal_and_withdraw_are_not_online_status_oracles(self):
        self.a.register(self.registry, [self.b.id]); self.b.register(self.registry, [self.a.id])
        self.b.register(self.registry, [])
        self.assertFalse(self.a.call(self.registry, "lookup", {"partner": self.b.id})["reachable"])
        self.b.register(self.registry, [self.a.id])
        self.b.call(self.registry, "withdraw", {})
        self.assertFalse(self.a.call(self.registry, "lookup", {"partner": self.b.id})["reachable"])
        self.b.register(self.registry, [self.a.id])
        self.now += 121
        self.assertFalse(self.a.call(self.registry, "lookup", {"partner": self.b.id})["reachable"])
        self.assertEqual(0, len(self.registry.presence))

    def test_replay_tamper_expired_challenge_and_cross_action_signature_fail(self):
        nonce = self.registry.challenge({"device": self.a.id})["challenge"]
        envelope = self.a.signed("lookup", {"partner": self.b.id}, nonce)
        self.assertFalse(self.registry.authenticated("lookup", envelope)["reachable"])
        with self.assertRaises(RequestError):
            self.registry.authenticated("lookup", envelope)
        nonce = self.registry.challenge({"device": self.a.id})["challenge"]
        envelope = self.a.signed("withdraw", {}, nonce)
        with self.assertRaises(RequestError):
            self.registry.authenticated("register", envelope)
        # Invalid attempts must not consume the valid nonce.
        self.assertEqual({"ok": True}, self.registry.authenticated("withdraw", envelope))
        nonce = self.registry.challenge({"device": self.a.id})["challenge"]
        envelope = self.a.signed("withdraw", {}, nonce)
        self.now += 61
        with self.assertRaises(RequestError):
            self.registry.authenticated("withdraw", envelope)

    def test_concurrent_challenges_do_not_invalidate_each_other(self):
        first = self.registry.challenge({"device": self.a.id})["challenge"]
        second = self.registry.challenge({"device": self.a.id})["challenge"]
        for nonce in (first, second):
            self.assertEqual({"ok": True}, self.registry.authenticated("withdraw", self.a.signed("withdraw", {}, nonce)))

    def test_private_content_unknown_fields_and_unbounded_addresses_are_rejected(self):
        valid = {"addresses": ["127.0.0.1"], "port": 45874, "partners": []}
        for extra in ({"chat": "private"}, {"invitation": "secret"}, {"audio": "bytes"}):
            with self.assertRaises(RequestError):
                self.a.call(self.registry, "register", {**valid, **extra})
        for replacement in ({"addresses": ["127.0.0.1"] * 9}, {"addresses": ["example.com"]},
                            {"addresses": ["ff02::1"]}, {"port": True}, {"partners": [self.b.id] * 33}):
            with self.assertRaises(RequestError):
                self.a.call(self.registry, "register", {**valid, **replacement})
        with self.assertRaises(RequestError):
            strict_json(b'{"device":"x","device":"y"}')
        self.assertEqual(0, len(self.registry.presence))

    def test_capacity_and_rate_limits_bound_memory(self):
        for _ in range(20):
            self.registry.challenge({"device": Device().id})
        self.assertEqual(16, len(self.registry.challenges))
        for _ in range(120):
            self.registry.limit("same-client")
        with self.assertRaises(RequestError) as failed:
            self.registry.limit("same-client")
        self.assertEqual(429, failed.exception.status)
        self.now += 61
        self.registry.limit("same-client")


class HttpTest(unittest.TestCase):
    def test_http_roundtrip_duplicate_keys_and_body_limits(self):
        server = Server(("127.0.0.1", 0))
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        root = f"http://127.0.0.1:{server.server_port}"
        try:
            def post(path, data):
                request = urllib.request.Request(root + path, data=data, headers={"Content-Type": "application/json"})
                return urllib.request.urlopen(request, timeout=3)
            device = Device()
            with post("/v1/challenge", json.dumps({"device": device.id}).encode()) as response:
                nonce = json.load(response)["challenge"]
                self.assertEqual("no-store", response.headers["Cache-Control"])
            with post("/v1/withdraw", json.dumps(device.signed("withdraw", {}, nonce)).encode()) as response:
                self.assertEqual({"ok": True}, json.load(response))
            for data, code in ((b'{"device":"a","device":"b"}', 400), (b"x" * 16385, 413)):
                with self.assertRaises(urllib.error.HTTPError) as failed:
                    post("/v1/challenge", data)
                self.assertEqual(code, failed.exception.code)
            with urllib.request.urlopen(root + "/health", timeout=3) as response:
                self.assertEqual({"ok": True}, json.load(response))
        finally:
            server.shutdown(); server.server_close(); worker.join(timeout=3)


if __name__ == "__main__":
    unittest.main()
