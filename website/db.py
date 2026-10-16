"""Small, durable site database. Audio and application data never enter it."""
import sqlite3
import time
from contextlib import contextmanager
from pathlib import Path

SCHEMA = """
CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS topics (
 id INTEGER PRIMARY KEY, slug TEXT UNIQUE NOT NULL, category TEXT NOT NULL,
 name TEXT UNIQUE NOT NULL, position INTEGER NOT NULL DEFAULT 0);
CREATE TABLE IF NOT EXISTS posts (
 id INTEGER PRIMARY KEY, topic_id INTEGER NOT NULL REFERENCES topics(id),
 title TEXT NOT NULL, body TEXT NOT NULL, author TEXT NOT NULL,
 visitor TEXT NOT NULL, is_author INTEGER NOT NULL DEFAULT 0,
 created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, image TEXT);
CREATE INDEX IF NOT EXISTS post_topics ON posts(topic_id, updated_at DESC);
CREATE TABLE IF NOT EXISTS replies (
 id INTEGER PRIMARY KEY, post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
 body TEXT NOT NULL, author TEXT NOT NULL, visitor TEXT NOT NULL,
 is_author INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, image TEXT);
CREATE INDEX IF NOT EXISTS reply_posts ON replies(post_id, created_at);
CREATE TABLE IF NOT EXISTS topic_requests (
 id INTEGER PRIMARY KEY, name TEXT NOT NULL, reason TEXT NOT NULL,
 status TEXT NOT NULL DEFAULT 'pending', created_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS rate_limits (
 key TEXT PRIMARY KEY, count INTEGER NOT NULL, resets_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS releases (
 id TEXT PRIMARY KEY, version TEXT UNIQUE NOT NULL, notes TEXT NOT NULL,
 published_at INTEGER NOT NULL, github_url TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS assets (
 release_id TEXT NOT NULL REFERENCES releases(id), platform TEXT NOT NULL,
 filename TEXT NOT NULL, size INTEGER NOT NULL, sha256 TEXT NOT NULL,
 PRIMARY KEY(release_id, platform));
CREATE TABLE IF NOT EXISTS subscribers (
 id INTEGER PRIMARY KEY, email TEXT UNIQUE NOT NULL, confirmed INTEGER NOT NULL DEFAULT 0,
 updates INTEGER NOT NULL DEFAULT 0, promotions INTEGER NOT NULL DEFAULT 0,
 pending_updates INTEGER NOT NULL DEFAULT 0, pending_promotions INTEGER NOT NULL DEFAULT 0,
 nonce TEXT NOT NULL, created_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS campaigns (
 id INTEGER PRIMARY KEY, kind TEXT NOT NULL, subject TEXT NOT NULL, body TEXT NOT NULL,
 status TEXT NOT NULL DEFAULT 'draft', created_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS outbox (
 id INTEGER PRIMARY KEY, subscriber_id INTEGER REFERENCES subscribers(id),
 campaign_id INTEGER REFERENCES campaigns(id), purpose TEXT NOT NULL,
 recipient TEXT NOT NULL, subject TEXT NOT NULL, body TEXT NOT NULL,
 status TEXT NOT NULL DEFAULT 'pending', attempts INTEGER NOT NULL DEFAULT 0,
 available_at INTEGER NOT NULL, locked_at INTEGER, last_error TEXT,
 UNIQUE(campaign_id, subscriber_id));
CREATE INDEX IF NOT EXISTS mail_pending ON outbox(status, available_at);
"""

DEFAULT_TOPICS = [
    ('icon', 'UI', '软件图标'), ('interface', 'UI', '内容界面'),
    ('playback', '功能', '播放与音量'), ('lyrics', '功能', '歌词与翻译'),
    ('library', '功能', '曲库与歌单'), ('import', '功能', '音频导入与转换'),
    ('assistant', '功能', '音乐助手与每日推荐'),
]


class ClosingConnection(sqlite3.Connection):
    def __exit__(self, *args):
        try:
            return super().__exit__(*args)
        finally:
            self.close()


def connect(path):
    db = sqlite3.connect(str(path), timeout=15, factory=ClosingConnection)
    db.row_factory = sqlite3.Row
    db.execute('PRAGMA foreign_keys=ON')
    db.execute('PRAGMA busy_timeout=15000')
    return db


def initialize(path):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with connect(path) as db:
        db.execute('PRAGMA journal_mode=WAL')
        db.executescript(SCHEMA)
        for i, (slug, category, name) in enumerate(DEFAULT_TOPICS):
            db.execute('INSERT OR IGNORE INTO topics(slug,category,name,position) VALUES(?,?,?,?)',
                       (slug, category, name, i))
        for key, value in [('announcement_title', '欢迎来到 Resonance 论坛'),
                           ('announcement_body', '软件问题和功能建议可以发在这里。'),
                           ('wechat_qr', ''), ('alipay_qr', '')]:
            db.execute('INSERT OR IGNORE INTO settings VALUES(?,?)', (key, value))


@contextmanager
def transaction(path):
    db = connect(path)
    try:
        db.execute('BEGIN IMMEDIATE')
        yield db
        db.commit()
    except BaseException:
        db.rollback()
        raise
    finally:
        db.close()


def settings(db):
    return {row['key']: row['value'] for row in db.execute('SELECT * FROM settings')}


def rate_limit(path, key, limit, seconds):
    now = int(time.time())
    with transaction(path) as db:
        db.execute('DELETE FROM rate_limits WHERE resets_at <= ?', (now,))
        row = db.execute('SELECT * FROM rate_limits WHERE key=?', (key,)).fetchone()
        if row and row['count'] >= limit:
            return False
        db.execute('INSERT INTO rate_limits VALUES(?,1,?) ON CONFLICT(key) '
                   'DO UPDATE SET count=count+1', (key, now + seconds))
    return True
