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


DEFAULT_ANNOUNCEMENT_TITLE = 'Resonance 软件详细介绍与核心特色'
DEFAULT_ANNOUNCEMENT_BODY = (
    "Resonance（共鸣）是一款专注于纯粹听歌体验的原生跨平台本地音乐播放器，覆盖 Windows 与 Android 双平台。\n\n"
    "【核心理念与设计】\n"
    "• 本地优先与隐私安全：坚持 Local-First 理念，无云端账号体系，不采集任何用户隐私；所有音频、歌词、歌单、会话记录均完全保存在您的本地设备中。\n"
    "• 原生多端性能：基于 Kotlin & Compose Multiplatform 深度打磨原生体验，界面清爽流畅，告别臃肿的 Web 套壳与商业广告。\n\n"
    "【主要功能特色】\n"
    "1. 本地曲库与格式支持：\n"
    "   支持 FLAC、APE、WAV、MP3、AAC、OGG、M4A 等主流高品质音频格式；自动扫描本地目录，精准解析与编辑内嵌元数据和封面；支持 KGMA/KGG 等音频格式转换与 .resonance 独立离线歌单包导入导出。\n"
    "2. 智能音量平衡（EBU R128）：\n"
    "   内置响度增益平衡技术，自动分析曲目响度并平滑过渡，告别不同录音版本或平台音频忽大忽小的问题，杜绝爆音失真。\n"
    "3. 歌词解析与双语翻译：\n"
    "   支持本地 LRC/KRC 及 LRCLIB 在线检索；支持 AI 智能翻译与双语对照展示；Windows 支持置顶桌面悬浮歌词，Android 支持系统级悬浮歌词与锁屏控制。\n"
    "4. B 站音频导入与全端登录：\n"
    "   支持 BV 号与视频链接直接提取高保真音频入库，可自由编辑歌名、歌手与封面；支持全端矢量二维码扫码与 Android 网页快捷登录，轻松同步收藏。\n"
    "5. 多模型 AI 音乐助手与截图导歌单：\n"
    "   接入千问、DeepSeek、豆包等大语言模型或自定义端点；支持发送外部平台歌单长截图，智能 OCR 分片识别并一键检索入库，可用自然语言对话轻松整理曲库。\n"
    "6. 双模式新手教程：\n"
    "   内置首次启动“快速了解”图文速览与“跟着操作”真实界面交互教学，设置页常驻教程目录与离线学习进度，新手轻松掌握全部进阶功能。\n\n"
    "欢迎在论坛发帖交流、提出功能建议或反馈 Bug，感谢每一位朋友的支持与充电！"
)


def initialize(path):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with connect(path) as db:
        db.execute('PRAGMA journal_mode=WAL')
        db.executescript(SCHEMA)
        for i, (slug, category, name) in enumerate(DEFAULT_TOPICS):
            db.execute('INSERT OR IGNORE INTO topics(slug,category,name,position) VALUES(?,?,?,?)',
                       (slug, category, name, i))
        for key, value in [('announcement_title', DEFAULT_ANNOUNCEMENT_TITLE),
                           ('announcement_body', DEFAULT_ANNOUNCEMENT_BODY),
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
