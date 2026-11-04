"""Independent relay child of the existing website launcher; never restarts the website."""
import argparse
import os
from pathlib import Path
import signal
import subprocess
import sys
import time


def stop_child(child):
    if child.poll() is not None:
        return
    try:
        if os.name == "posix":
            os.killpg(child.pid, signal.SIGTERM)
        else:
            child.terminate()
    except ProcessLookupError:
        child.wait(timeout=5)
        return
    try:
        child.wait(timeout=10)
    except subprocess.TimeoutExpired:
        try:
            if os.name == "posix":
                os.killpg(child.pid, signal.SIGKILL)
            else:
                child.kill()
        except ProcessLookupError:
            pass
        child.wait(timeout=5)


def supervise(command, alive, *, max_failures=5, stable_seconds=60, retry_base=2, poll_seconds=.25):
    """Retry a crashed relay within a finite budget; retire when our parent retires."""
    failures = 0
    while alive():
        started = time.monotonic()
        child = None
        try:
            child = subprocess.Popen(command, start_new_session=os.name == "posix")
            while child.poll() is None and alive():
                time.sleep(poll_seconds)
            if not alive():
                return 0
            if time.monotonic() - started >= stable_seconds:
                failures = 0
        except OSError:
            print("relay_supervisor child_start_failed", flush=True)
        finally:
            if child is not None:
                stop_child(child)
        failures += 1
        if failures >= max_failures:
            print("relay_supervisor retry_budget_exhausted; website unaffected", flush=True)
            return 1
        print(f"relay_supervisor retry={failures}", flush=True)
        deadline = time.monotonic() + min(30, retry_base * 2 ** (failures - 1))
        while alive() and time.monotonic() < deadline:
            time.sleep(poll_seconds)
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--parent-pid", type=int, required=True)
    args = parser.parse_args()
    if os.name != "posix" or args.parent_pid <= 1:
        parser.error("Deployment requires a Linux launcher parent")
    import fcntl
    os.umask(0o077)
    root = Path(__file__).resolve().parent
    data = Path(os.environ.get("RESONANCE_RELAY_DATA", "/home/container/resonance-relay-data"))
    data.mkdir(parents=True, exist_ok=True)
    stopped = False

    def stop(_signum, _frame):
        nonlocal stopped
        stopped = True

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    alive = lambda: not stopped and os.getppid() == args.parent_pid
    # Keep the lock until the relay child has exited, so two writers cannot share budget.json.
    with (data / "supervisor.lock").open("a") as lock:
        deadline = time.monotonic() + 15
        while alive():
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except BlockingIOError:
                if time.monotonic() >= deadline:
                    print("relay_supervisor another_owner_still_running", flush=True)
                    return 1
                time.sleep(.25)
        if not alive():
            return 0
        return supervise(["/bin/sh", str(root / "run-public-relay.sh")], alive)


if __name__ == "__main__":
    sys.exit(main())
