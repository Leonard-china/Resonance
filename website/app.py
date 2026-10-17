"""Self-hosted Resonance website. Run behind the supplied Ubuntu reverse proxy."""
import hashlib
import hmac
import io
import json
import os
import re
import secrets
import sqlite3
import time
import uuid
from datetime import datetime, timedelta, timezone
from functools import wraps
from pathlib import Path
from urllib.parse import urlsplit

from flask import (Flask, abort, flash, g, redirect, render_template, request,
                   send_file, session, url_for)
from werkzeug.security import check_password_hash
from werkzeug.middleware.proxy_fix import ProxyFix

import mailing
from db import connect, initialize, rate_limit, settings, transaction
from releases import GITHUB, active_release, publish

ROOT = Path(__file__).resolve().parent


def load_env():
    env_file = ROOT / '.env'
    if env_file.exists():
        for line in env_file.read_text(encoding='utf-8-sig').splitlines():
            if line.strip() and not line.lstrip().startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def create_app(overrides=None):
    load_env()
    app = Flask(__name__)
    static_root = ROOT / 'static'
    static_versions = {path.relative_to(static_root).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()[:16]
                       for path in static_root.rglob('*') if path.is_file() and not path.is_symlink()}

    @app.url_defaults
    def static_version(endpoint, values):
        if endpoint == 'static' and values.get('filename') in static_versions:
            values.setdefault('v', static_versions[values['filename']])

    data = Path(os.environ.get('DATA_DIR', ROOT / 'data'))
    if not data.is_absolute():
        data = ROOT / data
    app.config.update(
        DATA_DIR=data.resolve(), SITE_URL=os.environ.get('SITE_URL', 'http://127.0.0.1:8860').rstrip('/'),
        PRODUCTION=os.environ.get('PRODUCTION') == '1', MAX_CONTENT_LENGTH=512 * 1024 * 1024,
        MAX_FORM_MEMORY_SIZE=256 * 1024, MAX_FORM_PARTS=24,
        SESSION_COOKIE_NAME='resonance_site', SESSION_COOKIE_HTTPONLY=True,
        SESSION_REFRESH_EACH_REQUEST=False,
        SESSION_COOKIE_SAMESITE='Lax', PERMANENT_SESSION_LIFETIME=timedelta(days=30),
        SMTP_HOST=os.environ.get('SMTP_HOST', ''), SMTP_PORT=int(os.environ.get('SMTP_PORT', '465')),
        SMTP_SECURITY=os.environ.get('SMTP_SECURITY', 'tls'), SMTP_USER=os.environ.get('SMTP_USER', ''),
        SMTP_PASSWORD=os.environ.get('SMTP_PASSWORD', ''), MAIL_FROM=os.environ.get('MAIL_FROM', ''),
        MAIL_TEST_TO=os.environ.get('MAIL_TEST_TO', ''),
        MAIL_INTERVAL_SECONDS=max(1, float(os.environ.get('MAIL_INTERVAL_SECONDS', '3'))),
        DISABLE_RATE_LIMITS=False,
    )
    if overrides:
        app.config.update(overrides)
    data = Path(app.config['DATA_DIR']).resolve()
    data.mkdir(parents=True, exist_ok=True)
    app.config['DATA_DIR'] = data
    app.config['DATABASE'] = str(data / 'site.sqlite3')
    key_file = data / 'secret.key'
    if not key_file.exists():
        candidate = data / ('key-' + secrets.token_hex(8) + '.tmp')
        try:
            with os.fdopen(os.open(str(candidate), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
                stream.write(secrets.token_hex(48))
                stream.flush()
                os.fsync(stream.fileno())
            try:
                os.link(candidate, key_file)  # Publish a complete key atomically across web/worker startup.
            except FileExistsError:
                pass
        finally:
            candidate.unlink(missing_ok=True)
    app.secret_key = key_file.read_text().strip()
    if len(app.secret_key) < 64:
        raise RuntimeError('网站会话密钥不完整，请恢复 data/secret.key。')
    owner_file = data / 'owner.json'
    owner = json.loads(owner_file.read_text()) if owner_file.exists() else {}
    app.config.setdefault('OWNER_HASH', owner.get('password_hash', ''))
    app.config.setdefault('OWNER_PATH', owner.get('path', 'owner-' + hmac.new(
        app.secret_key.encode(), b'owner-path', hashlib.sha256).hexdigest()[:24]))
    if not re.fullmatch(r'[a-zA-Z0-9_-]{16,80}', app.config['OWNER_PATH']):
        raise RuntimeError('作者入口路径需为 16–80 个字母、数字、下划线或连字符。')
    base_url = urlsplit(app.config['SITE_URL'])
    if base_url.scheme not in ('http', 'https') or not base_url.hostname or base_url.path or base_url.query or base_url.fragment or base_url.username:
        raise RuntimeError('SITE_URL 必须是完整的网站根地址，例如 https://music.example.com。')
    if app.config['PRODUCTION'] and base_url.scheme != 'https':
        raise RuntimeError('正式部署的 SITE_URL 需要使用 HTTPS。')
    if app.config['SMTP_SECURITY'] not in ('tls', 'starttls'):
        raise RuntimeError('SMTP_SECURITY 只支持 tls 或 starttls。')
    app.config['SESSION_COOKIE_SECURE'] = app.config['PRODUCTION']
    app.config['TRUSTED_HOSTS'] = [base_url.hostname] if app.config['PRODUCTION'] else None
    if os.environ.get('TRUST_PROXY') == '1':
        app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1)
    initialize(app.config['DATABASE'])
    private = '/' + app.config['OWNER_PATH']

    def is_owner():
        expected = hmac.new(app.secret_key.encode(), app.config['OWNER_HASH'].encode(), hashlib.sha256).hexdigest()
        return bool(app.config['OWNER_HASH'] and time.time() - session.get('owner_at', 0) < 8 * 3600
                    and hmac.compare_digest(session.get('owner_auth', ''), expected))

    def owner_only(fn):
        @wraps(fn)
        def protected(*args, **kwargs):
            if not is_owner():
                return redirect(private)
            return fn(*args, **kwargs)
        return protected

    def limit(kind, count, seconds):
        if app.config['DISABLE_RATE_LIMITS']:
            return
        # Never persist a visitor's raw IP address or send it to other visitors.
        identity = hmac.new(app.secret_key.encode(),
                            (kind + ':' + (request.remote_addr or '')).encode(), hashlib.sha256).hexdigest()
        if not rate_limit(app.config['DATABASE'], identity, count, seconds):
            abort(429, description='操作有点频繁，请稍后再试。')

    def text(name, maximum, minimum=1):
        value = request.form.get(name, '').strip()
        if not minimum <= len(value) <= maximum or '\x00' in value:
            abort(400, description=f'请检查填写内容，长度需为 {minimum}–{maximum} 字。')
        return value

    def author():
        if is_owner():
            return '作者', 1
        name = request.form.get('nickname', '').strip()
        if len(name) > 24 or any(c in name for c in '\r\n\x00'):
            abort(400, description='昵称最多 24 字。')
        return name + '（访客）' if name else '访客·' + session['visitor'][:4].upper(), 0

    def save_image(field='image', max_bytes=3 * 1024 * 1024, lossless=False):
        upload = request.files.get(field)
        if not upload or not upload.filename:
            return None
        content = upload.stream.read(max_bytes + 1)
        if len(content) > max_bytes:
            abort(413, description='图片不能超过 3 MB。')
        from PIL import Image, ImageOps, UnidentifiedImageError
        try:
            with Image.open(io.BytesIO(content)) as image:
                if image.format not in ('PNG', 'JPEG', 'WEBP') or image.width * image.height > 12_000_000:
                    raise ValueError()
                image.load()
                image = ImageOps.exif_transpose(image)
                if 'A' in image.getbands() or image.mode == 'P':
                    rgba = image.convert('RGBA')
                    image = Image.new('RGB', rgba.size, 'white')
                    image.paste(rgba, mask=rgba.getchannel('A'))
                else:
                    image = image.convert('RGB')
                image.thumbnail((1600, 1600))
                output = io.BytesIO()
                image.save(output, 'WEBP', quality=82, lossless=lossless, method=4)
        except (UnidentifiedImageError, OSError, ValueError, Image.DecompressionBombError):
            abort(400, description='请上传有效的 PNG、JPG 或 WebP 图片。')
        folder = data / 'uploads'
        folder.mkdir(exist_ok=True)
        if sum(p.stat().st_size for p in folder.glob('*.webp')) + output.tell() > 256 * 1024 * 1024:
            abort(503, description='图片空间暂时已满，请先使用文字反馈。')
        filename = uuid.uuid4().hex + '.webp'
        (folder / filename).write_bytes(output.getvalue())
        return filename

    def remove_images(filenames):
        for name in filenames:
            if name and re.fullmatch(r'[a-f0-9]{32}\.webp', name):
                (data / 'uploads' / name).unlink(missing_ok=True)

    @app.before_request
    def prepare():
        if request.endpoint in ('static', 'download'):
            return None  # Public assets and download binaries must not create or refresh session cookies.
        session.permanent = True
        if 'visitor' not in session:
            session['visitor'] = secrets.token_hex(12)
        if 'csrf' not in session:
            session['csrf'] = secrets.token_hex(32)
        if request.endpoint != 'owner_release' or not is_owner():
            request.max_content_length = 4 * 1024 * 1024
        if request.method == 'POST':
            if request.endpoint == 'subscription_unsubscribe' and request.form.get('List-Unsubscribe') == 'One-Click':
                return None  # Capability-token URL is independently verified below.
            supplied = request.form.get('csrf', '')
            if not hmac.compare_digest(supplied, session['csrf']):
                abort(400, description='页面已过期，请刷新后重新提交。')
            if request.form.get('website', ''):
                abort(400, description='无法提交此表单。')
        return None

    @app.after_request
    def headers(response):
        response.headers['X-Content-Type-Options'] = 'nosniff'
        response.headers['Referrer-Policy'] = 'same-origin'
        response.headers['X-Frame-Options'] = 'DENY'
        response.headers['Content-Security-Policy'] = (
            "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            "font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'")
        if app.config['PRODUCTION']:
            response.headers['Strict-Transport-Security'] = 'max-age=31536000'
        if request.path.startswith(private) or request.path.startswith('/subscription/'):
            response.headers['X-Robots-Tag'] = 'noindex, nofollow, noarchive'
        if request.endpoint == 'static' and response.status_code in (200, 206, 304):
            version = static_versions.get((request.view_args or {}).get('filename'))
            if version and request.args.get('v') == version:
                response.headers['Cache-Control'] = 'public, max-age=31536000, immutable'
        elif request.endpoint == 'download' and response.status_code in (200, 206):
            response.headers['Cache-Control'] = 'public, max-age=86400, stale-while-revalidate=3600'
        else:
            response.headers['Cache-Control'] = 'no-store'
        return response

    @app.context_processor
    def context():
        with connect(app.config['DATABASE']) as db:
            site_settings = settings(db)
            current = active_release(db)
        return dict(site=site_settings, release=current, github=GITHUB,
                    csrf=session.get('csrf'), mail_ready=mailing.mail_ready(app), owner=is_owner())

    @app.template_filter('date')
    def date(value, detailed=False):
        if not value:
            return ''
        return datetime.fromtimestamp(value, timezone(timedelta(hours=8))).strftime('%Y-%m-%d %H:%M' if detailed else '%Y-%m-%d')

    @app.template_filter('filesize')
    def filesize(value):
        return f'{value / (1024 * 1024):.1f} MB'

    @app.errorhandler(400)
    @app.errorhandler(403)
    @app.errorhandler(404)
    @app.errorhandler(413)
    @app.errorhandler(416)
    @app.errorhandler(429)
    @app.errorhandler(503)
    def error_page(error):
        messages = {404: '这个页面不存在。', 413: '提交内容太大，请缩小文件后再试。',
                    416: '下载范围无效，请重新点击下载。',
                    503: '这个功能暂未开放，请稍后再试。'}
        description = error.description if error.description and not error.description.isascii() and error.code != 404 else messages.get(error.code, '暂时无法完成操作。')
        return render_template('message.html', title='暂时无法完成', message=description), error.code

    @app.get('/')
    def home():
        return render_template('home.html', active='home')

    @app.get('/downloads')
    def downloads():
        with connect(app.config['DATABASE']) as db:
            current = active_release(db)
            assets = {a['platform']: a for a in db.execute('SELECT * FROM assets WHERE release_id=?',
                       (current['id'] if current else '',))}
            history = db.execute('SELECT * FROM releases ORDER BY published_at DESC LIMIT 10').fetchall()
        lines = current['notes'].splitlines() if current else []
        highlights = [re.sub(r'^\d+[.、)]\s*', '', line).rstrip('：:') for line in lines
                      if re.match(r'^\d+[.、)]', line)][:3]
        if not highlights:
            highlights = [line.strip().lstrip('-• ')[:120] for line in lines if line.strip()][:3]
        return render_template('downloads.html', active='downloads', assets=assets, history=history, highlights=highlights)

    @app.get('/download/<platform>')
    def download(platform):
        if platform not in ('windows', 'android'):
            abort(404)
        for _ in range(2):
            with connect(app.config['DATABASE']) as db:
                current = active_release(db)
                asset = db.execute('SELECT * FROM assets WHERE release_id=? AND platform=?',
                                   (current['id'] if current else '', platform)).fetchone()
            if not asset:
                abort(503, description='最新版安装包还在准备中，请稍后再试。')
            path = data / 'releases' / current['id'] / asset['filename']
            try:
                handle = path.open('rb')
            except FileNotFoundError:
                continue  # Publication may have replaced the release between query and open.
            ext = '.exe' if platform == 'windows' else '-release.apk'
            name = 'Resonance-' + current['version'] + ext
            # Opening before returning keeps active downloads valid when Unix unlinks the old release.
            try:
                response = send_file(handle, as_attachment=True, download_name=name,
                                     mimetype='application/octet-stream', conditional=False)
                response.content_length = asset['size']
                response.set_etag(asset['sha256'])
                response.make_conditional(request, accept_ranges=True, complete_length=asset['size'])
            except BaseException:
                handle.close()
                raise
            response.call_on_close(handle.close)
            return response
        abort(503, description='版本刚刚更新，请重新点击下载。')

    @app.get('/media/<filename>')
    def media(filename):
        if not re.fullmatch(r'[a-f0-9]{32}\.webp', filename):
            abort(404)
        path = data / 'uploads' / filename
        if not path.is_file():
            abort(404)
        return send_file(path, mimetype='image/webp')

    @app.get('/support')
    def support():
        return render_template('support.html', active='support')

    def forum_data(topic_slug=None, page=1):
        with connect(app.config['DATABASE']) as db:
            topics = db.execute('SELECT * FROM topics ORDER BY position,id').fetchall()
            selected = next((topic for topic in topics if topic['slug'] == topic_slug), None)
            if topic_slug and not selected:
                abort(404)
            clause, parameters = ('WHERE p.topic_id=?', [selected['id']]) if selected else ('', [])
            total = db.execute('SELECT COUNT(*) FROM posts p ' + clause, parameters).fetchone()[0]
            posts = db.execute('SELECT p.*,t.name AS topic_name,t.slug AS topic_slug,'
                               '(SELECT COUNT(*) FROM replies WHERE post_id=p.id) AS reply_count '
                               'FROM posts p JOIN topics t ON t.id=p.topic_id ' + clause +
                               ' ORDER BY p.is_pinned DESC,p.updated_at DESC,p.id DESC LIMIT 20 OFFSET ?',
                               parameters + [(page - 1) * 20]).fetchall()
        return dict(topics=topics, selected=selected, posts=posts, page=page, total=total)

    @app.get('/forum')
    @app.get('/forum/topic/<slug>')
    def forum(slug=None):
        page = request.args.get('page', 1, type=int)
        page = max(1, min(page, 100000))
        return render_template('forum.html', active='forum', **forum_data(slug, page))

    @app.route('/forum/new', methods=['GET', 'POST'])
    def new_post():
        with connect(app.config['DATABASE']) as db:
            topics = db.execute('SELECT * FROM topics ORDER BY position,id').fetchall()
        if request.method == 'POST':
            limit('post', 5, 600)
            title, body = text('title', 100), text('body', 6000)
            topic_id = request.form.get('topic', type=int)
            if not any(t['id'] == topic_id for t in topics):
                abort(400, description='请选择一个已开放的话题。')
            name, verified = author()
            image = save_image()
            try:
                with transaction(app.config['DATABASE']) as db:
                    now = int(time.time())
                    post_id = db.execute('INSERT INTO posts(topic_id,title,body,author,visitor,is_author,created_at,updated_at,image) '
                                         'VALUES(?,?,?,?,?,?,?,?,?)', (topic_id, title, body, name, session['visitor'], verified, now, now, image)).lastrowid
            except BaseException:
                remove_images([image])
                raise
            return redirect(url_for('post', post_id=post_id), code=303)
        return render_template('compose.html', active='forum', topics=topics, selected_id=request.args.get('topic', type=int))

    @app.route('/forum/post/<int:post_id>', methods=['GET', 'POST'])
    def post(post_id):
        with connect(app.config['DATABASE']) as db:
            entry = db.execute('SELECT p.*,t.name AS topic_name,t.slug AS topic_slug FROM posts p '
                               'JOIN topics t ON t.id=p.topic_id WHERE p.id=?', (post_id,)).fetchone()
            replies = db.execute('SELECT * FROM replies WHERE post_id=? ORDER BY is_author DESC,id LIMIT 200', (post_id,)).fetchall()
            topics = db.execute('SELECT * FROM topics ORDER BY position,id').fetchall()
        if not entry:
            abort(404)
        if request.method == 'POST':
            limit('reply', 20, 600)
            body = text('body', 3000)
            name, verified = author()
            image = save_image()
            try:
                with transaction(app.config['DATABASE']) as db:
                    if not db.execute('SELECT 1 FROM posts WHERE id=?', (post_id,)).fetchone():
                        abort(404)
                    if db.execute('SELECT COUNT(*) FROM replies WHERE post_id=?', (post_id,)).fetchone()[0] >= 200:
                        abort(400, description='这个帖子已达到 200 条回复上限，请新建帖子继续讨论。')
                    now = int(time.time())
                    db.execute('INSERT INTO replies(post_id,body,author,visitor,is_author,created_at,image) VALUES(?,?,?,?,?,?,?)',
                               (post_id, body, name, session['visitor'], verified, now, image))
                    db.execute('UPDATE posts SET updated_at=? WHERE id=?', (now, post_id))
            except BaseException:
                remove_images([image])
                raise
            return redirect(url_for('post', post_id=post_id) + '#replies', code=303)
        return render_template('post.html', active='forum', entry=entry, replies=replies,
                               topics=topics, selected=next(t for t in topics if t['id'] == entry['topic_id']))

    @app.route('/forum/request-topic', methods=['GET', 'POST'])
    def request_topic():
        if request.method == 'POST':
            limit('topic-request', 3, 86400)
            name, reason = text('name', 30, 2), text('reason', 1000, 5)
            with transaction(app.config['DATABASE']) as db:
                if db.execute('SELECT id FROM topics WHERE name=?', (name,)).fetchone():
                    abort(400, description='这个话题已经存在。')
                db.execute('INSERT INTO topic_requests(name,reason,created_at) VALUES(?,?,?)', (name, reason, int(time.time())))
            return render_template('message.html', title='申请已提交', message='作者审核通过后，新话题会出现在论坛左侧。')
        return render_template('topic_request.html', active='forum')

    @app.post('/subscribe')
    def subscribe():
        limit('subscribe', 5, 3600)
        if not mailing.mail_ready(app):
            abort(503, description='邮件订阅暂未开放，请稍后再试。')
        email = text('email', 254).lower()
        if not re.fullmatch(r'[A-Za-z0-9.!#$%&\x27*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?\.[A-Za-z]{2,63}', email):
            abort(400, description='请填写有效的邮箱地址。')
        updates, promotions = request.form.get('updates') == '1', request.form.get('promotions') == '1'
        if not updates and not promotions:
            abort(400, description='请至少选择一种邮件。')
        mailing.queue_confirmation(app, email, updates, promotions)
        return render_template('message.html', title='请查看邮箱', message='确认邮件已加入发送队列。打开邮件里的链接并确认后，订阅才会生效。')

    @app.route('/subscription/confirm/<value>', methods=['GET', 'POST'])
    def subscription_confirm(value):
        row = mailing.resolve_token(app, value, 'confirm')
        if not row:
            abort(400, description='确认链接已失效，请重新订阅。')
        if request.method == 'POST':
            if not mailing.confirm(app, value):
                abort(400, description='确认链接已失效，请重新订阅。')
            return render_template('message.html', title='订阅已确认', message='你会收到所选类型的邮件，每封邮件都带有退订入口。')
        choices = mailing.serializer(app, 'confirm').loads(value, max_age=48 * 3600)
        return render_template('subscription.html', kind='confirm', choices=choices)

    @app.route('/subscription/unsubscribe/<value>', methods=['GET', 'POST'])
    def subscription_unsubscribe(value):
        if not mailing.resolve_token(app, value, 'unsubscribe'):
            abort(400, description='退订链接已失效或已完成退订。')
        if request.method == 'POST':
            mailing.unsubscribe(app, value)
            return render_template('message.html', title='已退订', message='后续邮件不会再发送到这个邮箱。')
        return render_template('subscription.html', kind='unsubscribe')

    @app.get('/robots.txt')
    def robots():
        return app.response_class('User-agent: *\nAllow: /\nDisallow: /subscription/\n', mimetype='text/plain')

    @app.get('/health')
    def health():
        with connect(app.config['DATABASE']) as db:
            db.execute('SELECT 1').fetchone()
        return {'status': 'ok'}

    @app.route(private, methods=['GET', 'POST'])
    def owner_login():
        if not app.config['OWNER_HASH']:
            abort(404)
        if is_owner():
            return redirect(private + '/dashboard')
        if request.method == 'POST':
            limit('owner-login', 5, 900)
            password = request.form.get('password', '')
            if len(password) > 512 or not check_password_hash(app.config['OWNER_HASH'], password):
                flash('密码不正确。', 'error')
            else:
                expected = hmac.new(app.secret_key.encode(), app.config['OWNER_HASH'].encode(), hashlib.sha256).hexdigest()
                session.clear()
                session.update(visitor=secrets.token_hex(12), csrf=secrets.token_hex(32),
                               owner_auth=expected, owner_at=int(time.time()), permanent=True)
                return redirect(private + '/dashboard', code=303)
        return render_template('owner_login.html', private=private)

    @app.post(private + '/logout')
    @owner_only
    def owner_logout():
        session.clear()
        return redirect('/')

    @app.get(private + '/dashboard')
    @owner_only
    def owner_dashboard():
        q = request.args.get('q', '').strip()
        with connect(app.config['DATABASE']) as db:
            requests = db.execute("SELECT * FROM topic_requests WHERE status='pending' ORDER BY id").fetchall()
            topics = db.execute(
                'SELECT t.*, (SELECT COUNT(*) FROM posts WHERE topic_id=t.id) AS post_count '
                'FROM topics t ORDER BY t.position, t.id'
            ).fetchall()
            if q:
                filter_num = int(q) if q.isdigit() else -1
                posts = db.execute(
                    'SELECT p.*,t.name AS topic_name FROM posts p JOIN topics t ON t.id=p.topic_id '
                    'WHERE p.id=? OR p.title LIKE ? OR p.body LIKE ? OR p.author LIKE ? '
                    'ORDER BY p.id DESC LIMIT 100',
                    (filter_num, f'%{q}%', f'%{q}%', f'%{q}%')
                ).fetchall()
            else:
                posts = db.execute('SELECT p.*,t.name AS topic_name FROM posts p JOIN topics t ON t.id=p.topic_id ORDER BY p.id DESC LIMIT 100').fetchall()
            replies = db.execute('SELECT r.*,p.title FROM replies r JOIN posts p ON p.id=r.post_id ORDER BY r.id DESC LIMIT 100').fetchall()
            campaigns = db.execute('SELECT c.*,(SELECT COUNT(*) FROM outbox WHERE campaign_id=c.id AND status=\'sent\') AS sent,'
                                   '(SELECT COUNT(*) FROM outbox WHERE campaign_id=c.id) AS recipients '
                                   'FROM campaigns c ORDER BY id DESC LIMIT 50').fetchall()
            counts = dict(db.execute('SELECT status,COUNT(*) FROM outbox GROUP BY status').fetchall())
            subscribers = db.execute('SELECT COUNT(*) AS total,COALESCE(SUM(updates),0) AS updates,'
                                     'COALESCE(SUM(promotions),0) AS promotions FROM subscribers WHERE confirmed=1').fetchone()
            stats = dict(
                posts=db.execute('SELECT COUNT(*) FROM posts').fetchone()[0],
                replies=db.execute('SELECT COUNT(*) FROM replies').fetchone()[0],
                topics=len(topics),
                requests=len(requests),
                subscribers=subscribers['total'] if subscribers else 0,
            )
        return render_template('owner.html', private=private, requests=requests, topics=topics, posts=posts, replies=replies,
                               campaigns=campaigns, counts=counts, subscribers=subscribers, stats=stats, q=q)

    @app.post(private + '/announcement')
    @owner_only
    def owner_announcement():
        title, body = text('title', 100), text('body', 2000)
        with transaction(app.config['DATABASE']) as db:
            for key, value in [('announcement_title', title), ('announcement_body', body)]:
                db.execute('UPDATE settings SET value=? WHERE key=?', (value, key))
        flash('公告已更新。', 'success')
        return redirect(private + '/dashboard#announcement', code=303)

    @app.post(private + '/release')
    @owner_only
    def owner_release():
        version, notes = text('version', 32), text('notes', 16000)
        streams = {}
        for platform, extension in [('windows', '.exe'), ('android', '.apk')]:
            upload = request.files.get(platform)
            if (not upload or not upload.filename.lower().endswith(extension)
                    or not re.search(r'(?<![\d.])' + re.escape(version) + r'(?!\d|\.\d)', upload.filename)):
                abort(400, description='请上传文件名包含本次版本号的 Windows EXE 和 Android APK。')
            streams[platform] = upload.stream
        try:
            publish(data, app.config['DATABASE'], version, notes, streams)
        except (ValueError, sqlite3.IntegrityError) as exc:
            flash(str(exc) if isinstance(exc, ValueError) else '这个版本已经发布。', 'error')
        else:
            flash('新版本已发布，旧包已进入自动清理流程。历史版本仍可通过 GitHub 下载。', 'success')
        return redirect(private + '/dashboard#release', code=303)

    @app.post(private + '/topic/<int:request_id>/<decision>')
    @owner_only
    def owner_topic(request_id, decision):
        if decision not in ('approve', 'reject'):
            abort(404)
        with transaction(app.config['DATABASE']) as db:
            row = db.execute("SELECT * FROM topic_requests WHERE id=? AND status='pending'", (request_id,)).fetchone()
            if not row:
                abort(404)
            if decision == 'approve':
                if db.execute('SELECT id FROM topics WHERE name=?', (row['name'],)).fetchone():
                    flash('同名话题已存在，申请已合并处理。', 'success')
                else:
                    max_pos = db.execute('SELECT COALESCE(MAX(position), 0) FROM topics').fetchone()[0]
                    db.execute('INSERT INTO topics(slug,category,name,position) VALUES(?,?,?,?)',
                               ('other-' + secrets.token_hex(6), '其他', row['name'], max_pos + 10))
            db.execute('UPDATE topic_requests SET status=? WHERE id=?', (decision, request_id))
        flash('申请已处理。', 'success')
        return redirect(private + '/dashboard#topics', code=303)

    @app.post(private + '/topic/create')
    @owner_only
    def owner_topic_create():
        name = text('name', 30, 2)
        category = text('category', 20, 1)
        raw_slug = request.form.get('slug', '').strip().lower()
        if raw_slug:
            if not re.fullmatch(r'[a-z0-9_-]{2,40}', raw_slug):
                abort(400, description='话题标识需为 2–40 位小写字母、数字、下划线或短横线。')
            slug = raw_slug
        else:
            slug = 'topic-' + secrets.token_hex(4)
        pos = request.form.get('position', type=int)
        with transaction(app.config['DATABASE']) as db:
            if db.execute('SELECT id FROM topics WHERE name=?', (name,)).fetchone():
                flash(f'同名话题「{name}」已存在。', 'error')
                return redirect(private + '/dashboard#topics', code=303)
            while db.execute('SELECT id FROM topics WHERE slug=?', (slug,)).fetchone():
                slug = 'topic-' + secrets.token_hex(4)
            if pos is None:
                max_pos = db.execute('SELECT COALESCE(MAX(position), 0) FROM topics').fetchone()[0]
                pos = max_pos + 10
            db.execute('INSERT INTO topics(slug,category,name,position) VALUES(?,?,?,?)',
                       (slug, category, name, pos))
        flash(f'新话题「{name}」已添加并向论坛开放。', 'success')
        return redirect(private + '/dashboard#topics', code=303)

    @app.post(private + '/topic/<int:topic_id>/delete')
    @owner_only
    def owner_topic_delete(topic_id):
        with transaction(app.config['DATABASE']) as db:
            topic = db.execute('SELECT * FROM topics WHERE id=?', (topic_id,)).fetchone()
            if not topic:
                abort(404)
            count = db.execute('SELECT COUNT(*) FROM posts WHERE topic_id=?', (topic_id,)).fetchone()[0]
            if count > 0:
                flash(f'无法删除话题「{topic["name"]}」：该话题下仍有 {count} 篇帖子，请先清理或迁移其中的帖子。', 'error')
                return redirect(private + '/dashboard#topics', code=303)
            db.execute('DELETE FROM topics WHERE id=?', (topic_id,))
        flash(f'话题「{topic["name"]}」已删除。', 'success')
        return redirect(private + '/dashboard#topics', code=303)

    @app.post(private + '/topic/<int:topic_id>/edit')
    @owner_only
    def owner_topic_edit(topic_id):
        name = text('name', 30, 2)
        category = text('category', 20, 1)
        pos = request.form.get('position', 100, type=int)
        with transaction(app.config['DATABASE']) as db:
            topic = db.execute('SELECT * FROM topics WHERE id=?', (topic_id,)).fetchone()
            if not topic:
                abort(404)
            conflict = db.execute('SELECT id FROM topics WHERE name=? AND id!=?', (name, topic_id)).fetchone()
            if conflict:
                flash('已有其他话题使用此名称。', 'error')
                return redirect(private + '/dashboard#topics', code=303)
            db.execute('UPDATE topics SET name=?, category=?, position=? WHERE id=?', (name, category, pos, topic_id))
        flash(f'话题「{name}」已更新。', 'success')
        return redirect(private + '/dashboard#topics', code=303)

    @app.post(private + '/pin/post/<int:post_id>')
    @owner_only
    def owner_pin_post(post_id):
        with transaction(app.config['DATABASE']) as db:
            row = db.execute('SELECT is_pinned FROM posts WHERE id=?', (post_id,)).fetchone()
            if not row:
                abort(404)
            new_status = 0 if row['is_pinned'] else 1
            db.execute('UPDATE posts SET is_pinned=? WHERE id=?', (new_status, post_id))
        flash('帖子已置顶。' if new_status else '已取消置顶。', 'success')
        ref = request.referrer
        if ref and (private in ref or '/forum' in ref):
            return redirect(ref, code=303)
        return redirect(private + '/dashboard#moderation', code=303)

    @app.post(private + '/delete/post-by-query')
    @owner_only
    def owner_delete_post_by_query():
        raw = request.form.get('query', '').strip()
        match = re.search(r'(\d+)', raw)
        if not match:
            flash('请输入有效的帖子 ID 或帖子链接。', 'error')
            return redirect(private + '/dashboard#moderation', code=303)
        post_id = int(match.group(1))
        with transaction(app.config['DATABASE']) as db:
            row = db.execute('SELECT * FROM posts WHERE id=?', (post_id,)).fetchone()
            if not row:
                flash(f'未找到 ID 为 {post_id} 的帖子。', 'error')
                return redirect(private + '/dashboard#moderation', code=303)
            images = [row['image']]
            images += [r['image'] for r in db.execute('SELECT image FROM replies WHERE post_id=?', (post_id,))]
            db.execute('DELETE FROM posts WHERE id=?', (post_id,))
        remove_images(images)
        flash(f'帖子 #{post_id}「{row["title"]}」及其关联回复和附图已删除。', 'success')
        return redirect(private + '/dashboard#moderation', code=303)

    @app.post(private + '/delete/reply-by-query')
    @owner_only
    def owner_delete_reply_by_query():
        raw = request.form.get('query', '').strip()
        match = re.search(r'(\d+)', raw)
        if not match:
            flash('请输入有效的回复 ID。', 'error')
            return redirect(private + '/dashboard#moderation', code=303)
        reply_id = int(match.group(1))
        with transaction(app.config['DATABASE']) as db:
            row = db.execute('SELECT * FROM replies WHERE id=?', (reply_id,)).fetchone()
            if not row:
                flash(f'未找到 ID 为 {reply_id} 的回复。', 'error')
                return redirect(private + '/dashboard#moderation', code=303)
            image = row['image']
            db.execute('DELETE FROM replies WHERE id=?', (reply_id,))
        remove_images([image])
        flash(f'回复 #{reply_id} 已删除。', 'success')
        return redirect(private + '/dashboard#moderation', code=303)

    @app.post(private + '/delete/<kind>/<int:item_id>')
    @owner_only
    def owner_delete(kind, item_id):
        if kind not in ('post', 'reply'):
            abort(404)
        with transaction(app.config['DATABASE']) as db:
            table = 'posts' if kind == 'post' else 'replies'
            row = db.execute(f'SELECT * FROM {table} WHERE id=?', (item_id,)).fetchone()
            if not row:
                abort(404)
            images = [row['image']]
            if kind == 'post':
                images += [r['image'] for r in db.execute('SELECT image FROM replies WHERE post_id=?', (item_id,))]
            db.execute(f'DELETE FROM {table} WHERE id=?', (item_id,))
        remove_images(images)
        flash('内容已删除。', 'success')
        ref = request.referrer
        if ref:
            if kind == 'post' and f'/forum/post/{item_id}' in ref:
                return redirect('/forum', code=303)
            if private in ref or '/forum' in ref:
                return redirect(ref, code=303)
        return redirect(private + '/dashboard#moderation', code=303)

    @app.post(private + '/payment')
    @owner_only
    def owner_payment():
        if request.form.get('provider') not in ('wechat', 'alipay'):
            abort(400)
        image = save_image(lossless=True)
        if not image:
            abort(400, description='请选择收款码图片。')
        key = request.form['provider'] + '_qr'
        with transaction(app.config['DATABASE']) as db:
            old = db.execute('SELECT value FROM settings WHERE key=?', (key,)).fetchone()['value']
            db.execute('UPDATE settings SET value=? WHERE key=?', (image, key))
        remove_images([old])
        flash('收款码已更新。', 'success')
        return redirect(private + '/dashboard#payment', code=303)

    @app.post(private + '/campaign')
    @owner_only
    def owner_campaign():
        kind, subject, body = text('kind', 20), text('subject', 120), text('body', 12000)
        if kind not in ('updates', 'promotions') or '\n' in subject or '\r' in subject:
            abort(400, description='请检查邮件类型和标题。')
        with transaction(app.config['DATABASE']) as db:
            campaign_id = db.execute('INSERT INTO campaigns(kind,subject,body,created_at) VALUES(?,?,?,?)',
                                     (kind, subject, body, int(time.time()))).lastrowid
        return redirect(private + '/campaign/' + str(campaign_id), code=303)

    @app.route(private + '/campaign/<int:campaign_id>', methods=['GET', 'POST'])
    @owner_only
    def owner_campaign_preview(campaign_id):
        with connect(app.config['DATABASE']) as db:
            campaign = db.execute('SELECT * FROM campaigns WHERE id=?', (campaign_id,)).fetchone()
            if not campaign:
                abort(404)
            column = 'promotions' if campaign['kind'] == 'promotions' else 'updates'
            recipients = db.execute(f'SELECT COUNT(*) FROM subscribers WHERE confirmed=1 AND {column}=1').fetchone()[0]
        if request.method == 'POST':
            if not mailing.mail_ready(app):
                abort(503, description='请先配置邮件服务。')
            action = request.form.get('action')
            if action == 'test':
                email = app.config['MAIL_TEST_TO']
                if not email or '\n' in email or '\r' in email or '@' not in email:
                    abort(400, description='请先在服务器配置 MAIL_TEST_TO 为你自己的测试邮箱。')
                with transaction(app.config['DATABASE']) as db:
                    db.execute('INSERT INTO outbox(purpose,recipient,subject,body,available_at) VALUES(?,?,?,?,?)',
                               ('test', email, '[测试] ' + campaign['subject'], campaign['body'], int(time.time())))
                flash('测试邮件已加入队列。', 'success')
            elif action == 'send' and request.form.get('confirm') == 'yes':
                try:
                    count = mailing.queue_campaign(app, campaign_id)
                except ValueError as exc:
                    abort(400, description=str(exc))
                flash(f'已为 {count} 位符合订阅选择的用户建立发送任务。', 'success')
            else:
                abort(400, description='请先确认收件人范围。')
            return redirect(private + '/campaign/' + str(campaign_id), code=303)
        return render_template('campaign.html', private=private, campaign=campaign, recipients=recipients)

    return app


if __name__ == '__main__':
    create_app().run(host='127.0.0.1', port=8860, debug=False)
