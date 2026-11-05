"""Build a source-only migration bundle; runtime data is transferred privately."""
import hashlib
import runpy
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]


def package():
    site = ROOT / "website"
    rules = runpy.run_path(str(site / "package.py"))
    paths = [site / name for name in rules["FILES"]]
    for name in rules["DIRECTORIES"]:
        paths.extend(path for path in (site / name).rglob("*")
                     if path.is_file() and "__pycache__" not in path.parts and path.suffix != ".pyc")
    paths.extend(Path(__file__).parent / name for name in (
        "package.py", "prepare.sh", "README.md", "benchmark.py", "test_benchmark.py",
        "resonance-site.override.conf", "resonance-mail.override.conf"))
    destination = ROOT / "build/aliyun/Resonance-aliyun.zip"
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(set(paths)):
            if path.is_symlink() or not path.resolve().is_relative_to(ROOT):
                raise ValueError("Deployment bundle refuses external paths")
            entry = zipfile.ZipInfo.from_file(path, path.relative_to(ROOT).as_posix())
            entry.create_system = 3
            entry.external_attr = (0o100755 if path.suffix == ".sh" else 0o100644) << 16
            entry.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(entry, path.read_bytes())
    digest = hashlib.sha256(destination.read_bytes()).hexdigest()
    destination.with_suffix(".zip.sha256").write_text(f"{digest}  {destination.name}\n")
    print(destination)
    print("SHA-256: " + digest)


if __name__ == "__main__":
    package()
