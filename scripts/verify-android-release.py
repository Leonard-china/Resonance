#!/usr/bin/env python3
"""Check the signing identity of an APK intended to update Resonance."""

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path


OFFICIAL_CERT_SHA256 = "b2b4ad35b8c7136569d219103448eaee75d4bbd30f3872c601373df43b303fe5"
PACKAGE_NAME = "com.resonance.player"
BUILD_TOOLS_VERSION = "36.0.0"


def run(*command):
    result = subprocess.run(command, capture_output=True, text=True, errors="replace")
    if result.returncode:
        raise ValueError((result.stderr or result.stdout).strip() or f"{command[0]} failed")
    return result.stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--version", help="Expected versionName")
    parser.add_argument("--sdk", default=os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT"))
    args = parser.parse_args()
    if not args.sdk:
        parser.error("set ANDROID_HOME or pass --sdk")

    tools = Path(args.sdk) / "build-tools" / BUILD_TOOLS_VERSION
    suffix = ".exe" if os.name == "nt" else ""
    java_home = os.environ.get("JAVA_HOME")
    java = Path(java_home) / "bin" / ("java" + suffix) if java_home else "java"
    try:
        apk = args.apk.resolve(strict=True)
        certs = run(java, "-jar", tools / "lib/apksigner.jar", "verify", "--print-certs", apk)
        digests = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$", certs, re.M)
        if [digest.lower() for digest in digests] != [OFFICIAL_CERT_SHA256]:
            raise ValueError(f"Expected the official Resonance certificate. Found: {', '.join(digests) or 'none'}")
        badging = run(tools / ("aapt2" + suffix), "dump", "badging", apk)
        package = re.search(r"^package: name='([^']+)' versionCode='\d+' versionName='([^']+)'", badging, re.M)
        if not package or package[1] != PACKAGE_NAME or "application-debuggable" in badging:
            raise ValueError(f"Expected a non-debuggable {PACKAGE_NAME} APK")
        if args.version and package[2] != args.version:
            raise ValueError(f"Expected version {args.version}, found {package[2]}")
        run(tools / ("zipalign" + suffix), "-c", "-P", "16", "4", apk)
    except (OSError, ValueError) as error:
        print(f"Android release APK rejected: {error}", file=sys.stderr)
        return 1
    print(f"Verified {PACKAGE_NAME} {package[2]} with official certificate {OFFICIAL_CERT_SHA256}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
