import contextlib
import io
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest

from supervise import supervise


class SupervisorTest(unittest.TestCase):
    def test_crashing_real_child_stops_at_budget(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "starts"
            script = "from pathlib import Path;import sys;p=Path(sys.argv[1]);p.write_text((p.read_text() if p.exists() else '')+'x');sys.exit(2)"
            with contextlib.redirect_stdout(io.StringIO()):
                result = supervise([sys.executable, "-c", script, str(path)], lambda: True,
                                   max_failures=3, retry_base=.01, poll_seconds=.01)
            self.assertEqual(1, result)
            self.assertEqual("xxx", path.read_text())

    def test_parent_retirement_terminates_live_child(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ready"
            late = Path(directory) / "should-not-exist"
            script = "from pathlib import Path;import sys,time;Path(sys.argv[1]).touch();time.sleep(1);Path(sys.argv[2]).touch();time.sleep(30)"
            deadline = time.monotonic() + 5
            result = supervise([sys.executable, "-c", script, str(path), str(late)],
                               lambda: not path.exists() and time.monotonic() < deadline,
                               poll_seconds=.01)
            self.assertTrue(path.exists(), "Child must actually launch")
            self.assertEqual(0, result)
            time.sleep(1.1)
            self.assertFalse(late.exists(), "Retired supervisor must not leave its child running")

    def test_missing_executable_does_not_retry_forever(self):
        with tempfile.TemporaryDirectory() as directory, contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(1, supervise([str(Path(directory) / "missing-executable")], lambda: True,
                                         max_failures=2, retry_base=.01, poll_seconds=.01))


if __name__ == "__main__":
    unittest.main()
