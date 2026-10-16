"""Confirmed opt-in and a persistent, preference-aware SMTP outbox."""
import secrets
import smtplib
import ssl
import time
from email.message import EmailMessage
from email.utils import formatdate, make_msgid

from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired

from db import connect, transaction


def serializer(app, purpose):
    return URLSafeTimedSerializer(app.secret_key, salt='resonance-' + purpose)


def token(app, subscriber, purpose):
    return serializer(app, purpose).dumps({'id': subscriber['id'], 'nonce': subscriber['nonce']})


def resolve_token(app, value, purpose):
    try:
        data = serializer(app, purpose).loads(value, max_age=48 * 3600 if purpose == 'confirm' else None)
    except (BadSignature, SignatureExpired):
        return None
    with connect(app.config['DATABASE']) as db:
        row = db.execute('SELECT * FROM subscribers WHERE id=?', (data.get('id'),)).fetchone()
    if not row or not secrets.compare_digest(row['nonce'], str(data.get('nonce', ''))):
        return None
    return row


def mail_ready(app):
    return bool(app.config['SMTP_HOST'] and app.config['MAIL_FROM'])


def queue_confirmation(app, email, updates, promotions):
    now = int(time.time())
    with transaction(app.config['DATABASE']) as db:
        row = db.execute('SELECT * FROM subscribers WHERE email=?', (email,)).fetchone()
        if row:
            # Keep an existing confirmed subscriber's choices intact until reconfirmed.
            # Do not rotate their unsubscribe token on an unconfirmed public request.
            db.execute('UPDATE subscribers SET pending_updates=?,pending_promotions=? WHERE id=?',
                       (int(updates), int(promotions), row['id']))
        else:
            db.execute('INSERT INTO subscribers(email,pending_updates,pending_promotions,nonce,created_at) '
                       'VALUES(?,?,?,?,?)', (email, int(updates), int(promotions), secrets.token_hex(24), now))
        row = db.execute('SELECT * FROM subscribers WHERE email=?', (email,)).fetchone()
        # Bind choices into the signed confirmation; later requests cannot change this link's intent.
        value = serializer(app, 'confirm').dumps({'id': row['id'], 'nonce': row['nonce'],
                                                  'updates': bool(updates), 'promotions': bool(promotions)})
        link = app.config['SITE_URL'] + '/subscription/confirm/' + value
        choices = '、'.join(x for x, enabled in [('版本更新', updates), ('推广与活动', promotions)] if enabled)
        body = f'你申请订阅 Resonance 的{choices}邮件。\n\n请打开下面的地址并确认：\n{link}\n\n链接 48 小时内有效。如果不是你提交的申请，忽略这封邮件即可。'
        db.execute('INSERT INTO outbox(subscriber_id,purpose,recipient,subject,body,available_at) '
                   'VALUES(?,?,?,?,?,?)', (row['id'], 'confirm', email, '确认订阅 Resonance', body, now))


def confirm(app, value):
    row = resolve_token(app, value, 'confirm')
    if not row:
        return False
    data = serializer(app, 'confirm').loads(value, max_age=48 * 3600)
    with transaction(app.config['DATABASE']) as db:
        changed = db.execute('UPDATE subscribers SET confirmed=1,updates=?,promotions=? '
                             'WHERE id=? AND nonce=?',
                             (int(bool(data.get('updates'))), int(bool(data.get('promotions'))),
                              row['id'], row['nonce'])).rowcount
    return bool(changed)


def unsubscribe(app, value):
    row = resolve_token(app, value, 'unsubscribe')
    if not row:
        return False
    with transaction(app.config['DATABASE']) as db:
        db.execute('UPDATE subscribers SET confirmed=0,updates=0,promotions=0,nonce=? WHERE id=?',
                   (secrets.token_hex(24), row['id']))
        db.execute("UPDATE outbox SET status='cancelled' WHERE subscriber_id=? AND status='pending'", (row['id'],))
    return True


def queue_campaign(app, campaign_id):
    with transaction(app.config['DATABASE']) as db:
        campaign = db.execute('SELECT * FROM campaigns WHERE id=?', (campaign_id,)).fetchone()
        if not campaign or campaign['status'] != 'draft':
            raise ValueError('这封邮件已加入发送队列，不能重复发送。')
        column = 'promotions' if campaign['kind'] == 'promotions' else 'updates'
        rows = db.execute(f'SELECT * FROM subscribers WHERE confirmed=1 AND {column}=1').fetchall()
        for row in rows:
            db.execute('INSERT INTO outbox(subscriber_id,campaign_id,purpose,recipient,subject,body,available_at) '
                       'VALUES(?,?,?,?,?,?,?)', (row['id'], campaign_id, campaign['kind'], row['email'],
                                               campaign['subject'], campaign['body'], int(time.time())))
        db.execute("UPDATE campaigns SET status='queued' WHERE id=?", (campaign_id,))
    return len(rows)


def deliver(app, job):
    message = EmailMessage()
    message['From'] = app.config['MAIL_FROM']
    message['To'] = job['recipient']
    message['Subject'] = job['subject']
    message['Date'] = formatdate(localtime=False)
    message['Message-ID'] = make_msgid(domain=app.config['MAIL_FROM'].split('@')[-1])
    body = job['body']
    if job['purpose'] in ('updates', 'promotions'):
        with connect(app.config['DATABASE']) as db:
            row = db.execute('SELECT * FROM subscribers WHERE id=?', (job['subscriber_id'],)).fetchone()
        if not row or not row['confirmed'] or not row[job['purpose']]:
            return False
        link = app.config['SITE_URL'] + '/subscription/unsubscribe/' + token(app, row, 'unsubscribe')
        message['List-Unsubscribe'] = '<' + link + '>'
        message['List-Unsubscribe-Post'] = 'List-Unsubscribe=One-Click'
        body += '\n\n你已订阅 Resonance 的' + ('推广与活动' if job['purpose'] == 'promotions' else '版本更新')
        body += '邮件。\n退订：' + link
    message.set_content(body)
    context = ssl.create_default_context()
    if app.config['SMTP_SECURITY'] == 'tls':
        smtp = smtplib.SMTP_SSL(app.config['SMTP_HOST'], app.config['SMTP_PORT'], timeout=25, context=context)
    else:
        smtp = smtplib.SMTP(app.config['SMTP_HOST'], app.config['SMTP_PORT'], timeout=25)
    with smtp:
        if app.config['SMTP_SECURITY'] == 'starttls':
            smtp.starttls(context=context)
        if app.config['SMTP_USER']:
            smtp.login(app.config['SMTP_USER'], app.config['SMTP_PASSWORD'])
        smtp.send_message(message)
    return True


def process_one(app):
    if not mail_ready(app):
        return False
    now = int(time.time())
    with transaction(app.config['DATABASE']) as db:
        db.execute("UPDATE outbox SET status='pending' WHERE status='sending' AND locked_at<?", (now - 600,))
        db.execute("UPDATE outbox SET status='cancelled' WHERE purpose='confirm' AND status='pending' "
                   'AND available_at<?', (now - 48 * 3600,))
        row = db.execute("SELECT * FROM outbox WHERE status='pending' AND available_at<=? ORDER BY id LIMIT 1", (now,)).fetchone()
        if not row:
            return False
        db.execute("UPDATE outbox SET status='sending',locked_at=?,attempts=attempts+1 WHERE id=?", (now, row['id']))
    try:
        sent = deliver(app, row)
    except Exception as exc:
        # Persist only an exception class; SMTP errors can contain addresses and credentials.
        with transaction(app.config['DATABASE']) as db:
            db.execute('UPDATE outbox SET status=?,last_error=?,available_at=? WHERE id=?',
                       ('failed' if row['attempts'] >= 4 else 'pending', type(exc).__name__,
                        now + min(3600, 60 * 2 ** row['attempts']), row['id']))
    else:
        with transaction(app.config['DATABASE']) as db:
            db.execute("UPDATE outbox SET status=?,last_error=NULL,body='' WHERE id=?",
                       ('sent' if sent else 'cancelled', row['id']))
    return True
