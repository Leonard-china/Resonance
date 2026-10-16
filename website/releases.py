"""Publish both platforms as one release, then remove superseded local files."""
import hashlib
import os
import re
import shutil
import time
import uuid
from pathlib import Path

from db import connect, transaction

GITHUB = 'https://github.com/Leonard-china/Resonance'
VERSION = re.compile(r'\d{1,4}\.\d{1,4}\.\d{1,4}\Z')
PLATFORMS = {'windows': ('.exe', b'MZ'), 'android': ('.apk', b'PK')}


def sync_directory(path):
    if os.name != 'nt':
        descriptor = os.open(str(path), os.O_RDONLY | getattr(os, 'O_DIRECTORY', 0))
        try:
            os.fsync(descriptor)
        finally:
            os.close(descriptor)


def active_release(db):
    return db.execute("SELECT * FROM releases WHERE id=(SELECT value FROM settings "
                      "WHERE key='active_release')").fetchone()


def safe_remove(root, folder):
    root, folder = Path(root).resolve(), Path(folder).resolve()
    if folder.parent != root or not re.fullmatch(r'(?:stage-)?[a-f0-9]{32}', folder.name):
        raise ValueError('拒绝清理下载目录之外的路径。')
    if folder.exists():
        shutil.rmtree(folder)


def cleanup(data_dir, database):
    """The worker retries files held open by Windows downloads after publication."""
    root = Path(data_dir) / 'releases'
    if not root.exists():
        return
    with transaction(database) as db:
        current = active_release(db)
        known = {r['id'] for r in db.execute('SELECT id FROM releases')}
        for folder in root.iterdir():
            retired = folder.name in known and (not current or folder.name != current['id'])
            orphan = (re.fullmatch(r'(?:stage-)?[a-f0-9]{32}', folder.name) and folder.is_dir()
                      and folder.name not in known and time.time() - folder.stat().st_mtime > 3600)
            if retired or orphan:
                try:
                    safe_remove(root, folder)
                except (OSError, ValueError):
                    pass


def publish(data_dir, database, version, notes, streams):
    if not VERSION.fullmatch(version) or not notes.strip() or len(notes) > 16000:
        raise ValueError('请填写三段式版本号和不超过 16000 字的更新内容。')
    if set(streams) != set(PLATFORMS):
        raise ValueError('请同时提供 Windows EXE 和 Android APK。')
    root = Path(data_dir) / 'releases'
    root.mkdir(parents=True, exist_ok=True)
    release_id = uuid.uuid4().hex
    stage, destination = root / ('stage-' + release_id), root / release_id
    stage.mkdir()
    assets = []
    committed = False
    try:
        for platform, stream in streams.items():
            extension, magic = PLATFORMS[platform]
            filename = platform + extension
            digest, size = hashlib.sha256(), 0
            with (stage / filename).open('wb') as target:
                first = stream.read(1024 * 1024)
                if not first.startswith(magic):
                    raise ValueError('安装包格式不正确，请检查 ' + platform + ' 文件。')
                chunk = first
                while chunk:
                    size += len(chunk)
                    if size > 400 * 1024 * 1024:
                        raise ValueError('单个安装包不能超过 400 MB。')
                    if shutil.disk_usage(root).free < len(chunk) + 100 * 1024 * 1024:
                        raise ValueError('服务器剩余空间不足，请先清理空间。')
                    target.write(chunk)
                    digest.update(chunk)
                    chunk = stream.read(1024 * 1024)
                target.flush()
                os.fsync(target.fileno())
            assets.append((release_id, platform, filename, size, digest.hexdigest()))
        sync_directory(stage)
        # Serialize publishers: stale or duplicate versions cannot replace a newer build.
        with transaction(database) as db:
            current = active_release(db)
            if current and tuple(map(int, version.split('.'))) <= tuple(map(int, current['version'].split('.'))):
                raise ValueError('新版本号需要高于当前版本。')
            stage.rename(destination)
            sync_directory(root)
            db.execute('INSERT INTO releases VALUES(?,?,?,?,?)',
                       (release_id, version, notes.strip(), int(time.time()), GITHUB + '/releases/tag/v' + version))
            db.executemany('INSERT INTO assets VALUES(?,?,?,?,?)', assets)
            db.execute("INSERT INTO settings VALUES('active_release',?) "
                       "ON CONFLICT(key) DO UPDATE SET value=excluded.value", (release_id,))
        committed = True
    finally:
        if not committed:
            safe_remove(root, stage)
            safe_remove(root, destination)
    cleanup(data_dir, database)
    return release_id
