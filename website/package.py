"""Create a source-only deployment ZIP, never including data, keys or binaries."""
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
FILES = ['app.py', 'db.py', 'releases.py', 'mailing.py', 'manage.py', 'wsgi.py',
         'requirements.txt', 'README.md', 'OPERATIONS.md', '.env.example', '.gitignore', 'package.py', 'announcements.py']
DIRECTORIES = ['templates', 'static', 'deploy', 'tests', 'content']


def package():
    destination = ROOT.parent / 'build' / 'website' / 'Resonance-website.zip'
    destination.parent.mkdir(parents=True, exist_ok=True)
    paths = [ROOT / name for name in FILES]
    for name in DIRECTORIES:
        paths.extend(path for path in (ROOT / name).rglob('*')
                     if path.is_file() and '__pycache__' not in path.parts and path.suffix != '.pyc')
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in paths:
            if path.is_symlink() or not path.resolve().is_relative_to(ROOT):
                raise ValueError('Deployment package refuses external paths.')
            entry = zipfile.ZipInfo.from_file(path, 'website/' + path.relative_to(ROOT).as_posix())
            entry.create_system = 3
            entry.external_attr = 0o100644 << 16
            entry.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(entry, path.read_bytes())
    print(destination)


if __name__ == '__main__':
    package()
