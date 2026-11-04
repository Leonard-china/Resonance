import http.server
import threading
import unittest
from unittest.mock import patch

import benchmark


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(500 if self.path == "/support" else 200)
        self.end_headers()
        self.wfile.write(b"fixture")

    def log_message(self, *_):
        pass


class BenchmarkTest(unittest.TestCase):
    def test_failed_requests_are_counted_and_not_reported_as_success(self):
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            result = benchmark.measure_web(f"http://127.0.0.1:{server.server_port}", None, 3, 10)
            self.assertEqual(10, result["requests"])
            self.assertEqual(2, result["errors"])
            self.assertEqual(["HTTPError"], result["error_types"])
            self.assertIn("p95_ms", result)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(2)

    def test_memory_guard_prevents_network_load(self):
        with patch.object(benchmark, "available_memory", return_value=95 * benchmark.MIB), patch.object(benchmark.urllib.request, "urlopen") as network:
            result = benchmark.measure_web("http://127.0.0.1:1", None, 2, 5)
            self.assertEqual(5, result["errors"])
            self.assertEqual(["MemoryError"], result["error_types"])
            self.assertNotIn("p95_ms", result)
            network.assert_not_called()
