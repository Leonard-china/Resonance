"""Publish reviewed public content without touching owner credentials or mail."""
import json
from pathlib import Path

from db import transaction
from releases import active_release, VERSION


def publish_announcement(database, content_file):
    path = Path(content_file)
    if path.stat().st_size > 64000:
        raise ValueError('公告文件过大。')
    content = json.loads(path.read_text(encoding='utf-8-sig'))
    version, title, body = (content.get(key) for key in ('version', 'title', 'body'))
    if not isinstance(version, str) or not VERSION.fullmatch(version):
        raise ValueError('公告需要有效的三段式版本号。')
    if not isinstance(title, str) or not 1 <= len(title.strip()) <= 100:
        raise ValueError('公告标题需要 1–100 个字符。')
    if not isinstance(body, str) or not 1 <= len(body.strip()) <= 2000:
        raise ValueError('公告正文需要 1–2000 个字符。')
    with transaction(database) as connection:
        release = active_release(connection)
        if not release or release['version'] != version:
            raise ValueError('请先发布公告对应版本的双端安装包；未修改现有公告。')
        connection.executemany('INSERT INTO settings(key,value) VALUES (?,?) '
                               'ON CONFLICT(key) DO UPDATE SET value=excluded.value',
                               [('announcement_title', title.strip()), ('announcement_body', body.strip())])
    return version
