"""Opt-in, in-memory reachability service. Never accepts room keys, audio or chat.

Bind to loopback behind an HTTPS reverse proxy for deployment. No shared user accounts.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import ipaddress
import json
import re
import secrets
import threading
import time
from collections import OrderedDict
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

ID = re.compile(r"[a-f0-9]{64}\Z")
MAX_BODY = 16_384


class RequestError(Exception):
    def __init__(self, status=400):
        self.status = status


def b64(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def unb64(value: str, maximum: int) -> bytes:
    if not isinstance(value, str) or len(value) > maximum or not re.fullmatch(r"[A-Za-z0-9_-]*", value):
        raise RequestError()
    try:
        return base64.b64decode(value + "=" * (-len(value) % 4), altchars=b"-_", validate=True)
    except ValueError as exc:
        raise RequestError() from exc


def strict_json(data: bytes):
    def pairs(items):
        result = {}
        for key, value in items:
            if key in result:
                raise RequestError()
            result[key] = value
        return result
    try:
        value = json.loads(data, object_pairs_hook=pairs, parse_constant=lambda _: (_ for _ in ()).throw(RequestError()))
        if not isinstance(value, dict):
            raise RequestError()
        return value
    except (ValueError, RecursionError, UnicodeError) as exc:
        raise RequestError() from exc


def fields(data, expected):
    if set(data) != set(expected):
        raise RequestError()


def device_id(value):
    if not isinstance(value, str) or not ID.fullmatch(value):
        raise RequestError()
    return value


class Registry:
    def __init__(self, clock=time.monotonic, ttl=120, capacity=10_000):
        self.clock, self.ttl, self.capacity = clock, ttl, capacity
        self.lock = threading.RLock()
        self.challenges = OrderedDict()
        self.presence = OrderedDict()
        self.rate = OrderedDict()

    def prune(self):
        now = self.clock()
        for rows in (self.challenges, self.presence, self.rate):
            for key in list(rows):
                if rows[key][0] <= now:
                    del rows[key]

    def limit(self, address):
        with self.lock:
            self.prune()
            now = self.clock()
            expires, count = self.rate.get(address, (now + 60, 0))
            if count >= 120:
                raise RequestError(429)
            self.rate[address] = (expires, count + 1)
            while len(self.rate) > self.capacity:
                self.rate.popitem(last=False)

    def challenge(self, data):
        fields(data, ("device",))
        identity = device_id(data["device"])
        with self.lock:
            self.prune()
            nonce = b64(secrets.token_bytes(32))
            self.challenges[(identity, nonce)] = (self.clock() + 60, nonce)
            while len(self.challenges) > self.capacity:
                self.challenges.popitem(last=False)
            return {"challenge": nonce}

    def authenticated(self, action, envelope):
        if action not in ("register", "lookup", "withdraw"):
            raise RequestError(404)
        fields(envelope, ("publicKey", "challenge", "payload", "signature"))
        public = unb64(envelope["publicKey"], 256)
        try:
            key = serialization.load_der_public_key(public)
        except ValueError as exc:
            raise RequestError() from exc
        if not isinstance(key, ec.EllipticCurvePublicKey) or not isinstance(key.curve, ec.SECP256R1):
            raise RequestError()
        # Use canonical SubjectPublicKeyInfo bytes so alternate DER encodings cannot alias identity.
        canonical = key.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
        if canonical != public:
            raise RequestError()
        identity = hashlib.sha256(public).hexdigest()
        challenge = envelope["challenge"]
        payload = envelope["payload"]
        decoded = unb64(payload, 12_000)
        unb64(challenge, 43)
        signed = f"Resonance rendezvous v1\n{action}\n{challenge}\n{payload}".encode("ascii")
        try:
            key.verify(unb64(envelope["signature"], 128), signed, ec.ECDSA(hashes.SHA256()))
        except InvalidSignature as exc:
            raise RequestError(403) from exc
        data = strict_json(decoded)
        with self.lock:
            self.prune()
            expected = self.challenges.get((identity, challenge))
            if expected is None or not secrets.compare_digest(expected[1], challenge):
                raise RequestError(403)
            del self.challenges[(identity, challenge)]  # Single-use, consumed only after a verified signature.
            if action == "withdraw":
                fields(data, ())
                self.presence.pop(identity, None)
                return {"ok": True}
            if action == "register":
                fields(data, ("addresses", "port", "partners"))
                addresses, port, partners = data["addresses"], data["port"], data["partners"]
                if not isinstance(addresses, list) or not 1 <= len(addresses) <= 8:
                    raise RequestError()
                clean = []
                for address in addresses:
                    if not isinstance(address, str) or "%" in address:
                        raise RequestError()
                    try:
                        ip = ipaddress.ip_address(address)
                    except ValueError as exc:
                        raise RequestError() from exc
                    if ip.is_multicast or ip.is_unspecified or ip.is_link_local:
                        raise RequestError()
                    clean.append(str(ip))
                if type(port) is not int or not 1 <= port <= 65535 or not isinstance(partners, list) or len(partners) > 32:
                    raise RequestError()
                allowed = set(map(device_id, partners))
                self.presence[identity] = (self.clock() + self.ttl, {"addresses": list(dict.fromkeys(clean)), "port": port}, allowed)
                while len(self.presence) > self.capacity:
                    self.presence.popitem(last=False)
                return {"ok": True, "ttl": self.ttl}
            fields(data, ("partner",))
            peer = device_id(data["partner"])
            caller, target = self.presence.get(identity), self.presence.get(peer)
            if caller is None or target is None or peer not in caller[2] or identity not in target[2]:
                # Same response for absent, expired and unauthorized: no online directory.
                return {"reachable": False}
            return {"reachable": True, **target[1], "ttl": max(1, int(target[0] - self.clock()))}


class Server(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, address, registry=None):
        self.registry = registry or Registry()
        self.permits = threading.BoundedSemaphore(32)
        super().__init__(address, Handler)

    def process_request(self, request, client_address):
        if not self.permits.acquire(blocking=False):
            request.close()
            return
        try:
            super().process_request(request, client_address)
        except BaseException:
            self.permits.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self.permits.release()


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"
    def setup(self):
        super().setup()
        self.connection.settimeout(5)

    def log_message(self, *_):
        pass  # No identifiers, IPs, payloads or capability URLs in access logs.

    def reply(self, status, data):
        body = json.dumps(data, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        self.reply(200 if self.path == "/health" else 404, {"ok": self.path == "/health"})

    def do_POST(self):
        try:
            self.server.registry.limit(self.client_address[0])
            if self.headers.get("Transfer-Encoding") or self.headers.get_content_type() != "application/json":
                raise RequestError()
            lengths = self.headers.get_all("Content-Length", [])
            if len(lengths) != 1 or not lengths[0].isdigit() or not 1 <= int(lengths[0]) <= MAX_BODY:
                raise RequestError(413)
            size = int(lengths[0])
            body = self.rfile.read(size)
            if len(body) != size:
                raise RequestError()
            data = strict_json(body)
            if self.path == "/v1/challenge":
                result = self.server.registry.challenge(data)
            elif self.path in ("/v1/register", "/v1/lookup", "/v1/withdraw"):
                result = self.server.registry.authenticated(self.path.removeprefix("/v1/"), data)
            else:
                raise RequestError(404)
            self.reply(200, result)
        except RequestError as error:
            self.reply(error.status, {"error": "request_rejected"})
        except (ValueError, TypeError, KeyError, OSError):
            self.reply(400, {"error": "request_rejected"})


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    server = Server((args.host, args.port))
    print(f"Resonance rendezvous listening on port {server.server_port}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
