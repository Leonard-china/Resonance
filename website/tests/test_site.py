import io
import re
import sqlite3
import sys
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app import create_app
from db import connect, transaction
from releases import active_release, cleanup, publish, safe_remove
import mailing
from werkzeug.security import generate_password_hash


class SiteTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.password = 'only-for-isolated-tests-123'
        cls.password_hash = generate_password_hash(cls.password)

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.data = Path(self.temporary.name)
        self.app = create_app(dict(TESTING=True, DATA_DIR=self.data, SITE_URL='http://localhost',
            PRODUCTION=False, OWNER_PATH='owner-for-isolated-tests', OWNER_HASH=self.password_hash,
            SMTP_HOST='smtp.invalid', MAIL_FROM='site@example.com', SMTP_USER='',
            MAIL_TEST_TO='author@example.com', DISABLE_RATE_LIMITS=True))
        self.client = self.app.test_client()
        self.client.get('/')
        self.private = '/' + self.app.config['OWNER_PATH']

    def tearDown(self):
        self.temporary.cleanup()

    def csrf(self):
        with self.client.session_transaction() as session:
            return session['csrf']

    def submit(self, url, data=None, **kwargs):
        response = self.client.post(url, data={'csrf': self.csrf(), **(data or {})}, **kwargs)
        self.addCleanup(response.close)
        self.addCleanup(response.request.input_stream.close)
        return response

    def query(self, sql, parameters=()):
        with connect(self.app.config['DATABASE']) as db:
            return db.execute(sql, parameters).fetchall()

    def login(self):
        response = self.submit(self.private, {'password': self.password})
        self.assertEqual(response.status_code, 303)

    def add_post(self, body='实际问题描述', **extra):
        response = self.submit('/forum/new', dict(topic='2', title='界面反馈', body=body, **extra))
        self.assertEqual(response.status_code, 303)
        return self.query('SELECT id FROM posts ORDER BY id DESC')[0]['id']

    def publish(self, version='0.4.1'):
        return publish(self.data, self.app.config['DATABASE'], version, '1. 改进歌词\n2. 修复播放',
                       {'windows': io.BytesIO(b'MZtest-executable'), 'android': io.BytesIO(b'PKtest-apk')})

    def subscribe(self, email, updates=True, promotions=False):
        form = {'email': email}
        if updates:
            form['updates'] = '1'
        if promotions:
            form['promotions'] = '1'
        response = self.submit('/subscribe', form)
        self.assertEqual(response.status_code, 200)
        job = self.query('SELECT * FROM outbox ORDER BY id DESC')[0]
        value = re.search(r'/subscription/confirm/(\S+)', job['body']).group(1)
        return value

    def confirm(self, value):
        response = self.submit('/subscription/confirm/' + value)
        self.assertEqual(response.status_code, 200)

    def add_campaign(self, kind='updates'):
        with transaction(self.app.config['DATABASE']) as db:
            return db.execute('INSERT INTO campaigns(kind,subject,body,created_at) VALUES(?,?,?,1)',
                              (kind, '测试标题', '测试正文')).lastrowid

    def test_public_pages_have_no_author_entry_or_old_slogan(self):
        for url in ['/', '/downloads', '/forum', '/support', '/forum/new', '/forum/request-topic', '/robots.txt']:
            response = self.client.get(url)
            self.assertEqual(response.status_code, 200, url)
            html = response.get_data(as_text=True)
            self.assertNotIn(self.private, html)
            self.assertNotIn('让音乐，与', html)
            self.assertNotIn('为你的音乐而生', html)
        html = self.client.get('/forum').get_data(as_text=True)
        self.assertIn('无需注册', html)
        self.assertRegex(html, r'name="updates"[^>]* checked')
        self.assertNotRegex(html, r'name="promotions"[^>]*checked')

    def test_csrf_protects_public_and_author_mutations(self):
        self.assertEqual(self.client.post('/forum/new', data={'title':'x'}).status_code, 400)
        self.assertEqual(self.client.post(self.private, data={'password':self.password}).status_code, 400)
        self.assertEqual(len(self.query('SELECT * FROM posts')), 0)

    def test_anonymous_post_reply_and_xss_are_safe(self):
        post_id = self.add_post('<script>alert(1)</script>', nickname='作者')
        page = self.client.get(f'/forum/post/{post_id}').get_data(as_text=True)
        self.assertIn('&lt;script&gt;', page)
        self.assertNotIn('<script>alert', page)
        self.assertIn('作者（访客）', page)
        self.assertEqual(self.query('SELECT is_author FROM posts')[0]['is_author'], 0)
        self.assertEqual(self.submit(f'/forum/post/{post_id}', {'body':'匿名回复'}).status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM replies')), 1)
        self.assertIn('匿名回复', self.client.get(f'/forum/post/{post_id}').get_data(as_text=True))

    def test_topic_request_is_private_until_approved(self):
        self.assertEqual(self.submit('/forum/request-topic', {'name':'音乐分享','reason':'想讨论喜欢的音乐和歌单'}).status_code, 200)
        self.assertNotIn('音乐分享', self.client.get('/forum').get_data(as_text=True))
        self.assertEqual(self.submit(self.private + '/topic/1/approve').status_code, 302)
        self.assertEqual(len(self.query("SELECT * FROM topics WHERE category='其他'")), 0)
        self.login()
        self.assertEqual(self.submit(self.private + '/topic/1/approve').status_code, 303)
        self.assertIn('音乐分享', self.client.get('/forum').get_data(as_text=True))

    def test_author_deletion_is_protected_and_cascades(self):
        post_id = self.add_post()
        self.submit(f'/forum/post/{post_id}', {'body':'待删除回复'})
        self.assertEqual(self.submit(self.private + f'/delete/post/{post_id}').status_code, 302)
        self.assertEqual(len(self.query('SELECT * FROM posts')), 1)
        self.login()
        self.assertEqual(self.submit(self.private + f'/delete/post/{post_id}').status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM posts')), 0)
        self.assertEqual(len(self.query('SELECT * FROM replies')), 0)

    def test_password_change_revokes_existing_author_session(self):
        self.login()
        self.assertEqual(self.client.get(self.private + '/dashboard').status_code, 200)
        self.app.config['OWNER_HASH'] = generate_password_hash('different-long-test-password')
        self.assertEqual(self.client.get(self.private + '/dashboard').status_code, 302)

    def test_login_throttle_persists_and_does_not_store_plain_ip(self):
        self.app.config['DISABLE_RATE_LIMITS'] = False
        for _ in range(5):
            self.assertEqual(self.submit(self.private, {'password':'wrong'}).status_code, 200)
        self.assertEqual(self.submit(self.private, {'password':'wrong'}).status_code, 429)
        self.assertNotIn('127.0.0.1', self.query('SELECT key FROM rate_limits')[0]['key'])

    def test_subscription_needs_email_confirmation(self):
        value = self.subscribe('reader@example.com')
        row = self.query('SELECT * FROM subscribers')[0]
        self.assertEqual(row['confirmed'], 0)
        # A link-scanning GET does not confirm the subscription.
        self.assertEqual(self.client.get('/subscription/confirm/' + value).status_code, 200)
        self.assertEqual(self.query('SELECT confirmed FROM subscribers')[0][0], 0)
        self.confirm(value)
        row = self.query('SELECT * FROM subscribers')[0]
        self.assertEqual((row['confirmed'], row['updates'], row['promotions']), (1,1,0))

    def test_confirmation_is_bound_to_the_selected_preferences(self):
        first = self.subscribe('reader@example.com', True, False)
        self.subscribe('reader@example.com', False, True)
        self.confirm(first)
        row = self.query('SELECT * FROM subscribers')[0]
        self.assertEqual((row['updates'],row['promotions']), (1,0))

    def test_promotion_campaign_only_includes_explicit_opt_in(self):
        self.confirm(self.subscribe('updates@example.com'))
        self.confirm(self.subscribe('both@example.com', True, True))
        campaign = self.add_campaign('promotions')
        self.assertEqual(mailing.queue_campaign(self.app, campaign), 1)
        jobs = self.query('SELECT * FROM outbox WHERE campaign_id=?', (campaign,))
        self.assertEqual(jobs[0]['recipient'], 'both@example.com')
        with self.assertRaises(ValueError):
            mailing.queue_campaign(self.app, campaign)

    def test_unsubscribe_cancels_pending_mail_and_old_confirm_link(self):
        confirm_value = self.subscribe('reader@example.com')
        self.confirm(confirm_value)
        campaign = self.add_campaign()
        mailing.queue_campaign(self.app, campaign)
        row = self.query('SELECT * FROM subscribers')[0]
        value = mailing.token(self.app, row, 'unsubscribe')
        self.assertEqual(self.client.get('/subscription/unsubscribe/' + value).status_code, 200)
        self.assertEqual(self.query('SELECT confirmed FROM subscribers')[0][0], 1)
        self.assertEqual(self.submit('/subscription/unsubscribe/' + value).status_code, 200)
        self.assertEqual(self.query('SELECT confirmed FROM subscribers')[0][0], 0)
        self.assertEqual(self.query('SELECT status FROM outbox WHERE campaign_id=?', (campaign,))[0][0], 'cancelled')
        self.assertFalse(mailing.confirm(self.app, confirm_value))

    def test_one_click_unsubscribe_uses_a_valid_capability_token(self):
        self.confirm(self.subscribe('reader@example.com'))
        row = self.query('SELECT * FROM subscribers')[0]
        value = mailing.token(self.app, row, 'unsubscribe')
        self.assertEqual(self.client.post('/subscription/unsubscribe/' + value,
                         data={'List-Unsubscribe':'One-Click'}).status_code, 200)
        self.assertEqual(self.query('SELECT confirmed FROM subscribers')[0][0], 0)
        self.assertEqual(self.client.post('/subscription/unsubscribe/forged',
                         data={'List-Unsubscribe':'One-Click'}).status_code, 400)

    def test_no_smtp_means_no_false_subscription_success(self):
        self.app.config['SMTP_HOST'] = ''
        self.assertEqual(self.submit('/subscribe', {'email':'reader@example.com','updates':'1'}).status_code, 503)
        self.assertEqual(len(self.query('SELECT * FROM subscribers')), 0)

    def test_worker_persists_attempts_and_retries_without_real_mail(self):
        self.subscribe('reader@example.com')
        with patch.object(mailing, 'deliver', side_effect=TimeoutError()):
            self.assertTrue(mailing.process_one(self.app))
        row = self.query('SELECT * FROM outbox')[0]
        self.assertEqual((row['status'], row['attempts'], row['last_error']), ('pending',1,'TimeoutError'))
        with transaction(self.app.config['DATABASE']) as db:
            db.execute('UPDATE outbox SET available_at=?', (int(time.time()),))
        with patch.object(mailing, 'deliver', return_value=True) as sender:
            self.assertTrue(mailing.process_one(self.app))
            sender.assert_called_once()
        self.assertEqual(self.query('SELECT status FROM outbox')[0][0], 'sent')

    def test_worker_rechecks_opt_out_before_connecting_to_smtp(self):
        self.confirm(self.subscribe('reader@example.com'))
        campaign = self.add_campaign()
        mailing.queue_campaign(self.app, campaign)
        job = self.query('SELECT * FROM outbox WHERE campaign_id=?', (campaign,))[0]
        with transaction(self.app.config['DATABASE']) as db:
            db.execute('UPDATE subscribers SET updates=0')
        with patch.object(mailing.smtplib, 'SMTP_SSL') as smtp:
            self.assertFalse(mailing.deliver(self.app, job))
            smtp.assert_not_called()

    def test_release_switch_removes_old_packages_but_keeps_history(self):
        first = self.publish()
        second = self.publish('0.4.2')
        self.assertFalse((self.data/'releases'/first).exists())
        self.assertTrue((self.data/'releases'/second/'windows.exe').exists())
        with connect(self.app.config['DATABASE']) as db:
            self.assertEqual(active_release(db)['version'], '0.4.2')
        self.assertEqual(len(self.query('SELECT * FROM releases')), 2)
        html = self.client.get('/downloads').get_data(as_text=True)
        self.assertIn('/releases/tag/v0.4.1', html)
        self.assertNotIn('/download/0.4.1', html)

    def test_failed_upload_preserves_active_release_and_cleans_staging(self):
        first = self.publish()
        with self.assertRaises(ValueError):
            publish(self.data,self.app.config['DATABASE'],'0.4.2','new notes',
                    {'windows':io.BytesIO(b'MZgood'), 'android':io.BytesIO(b'bad-format')})
        self.assertTrue((self.data/'releases'/first/'windows.exe').exists())
        self.assertEqual([p.name for p in (self.data/'releases').iterdir()], [first])
        with self.assertRaises(ValueError):
            self.publish('0.4.0')

    def test_low_disk_space_keeps_the_previous_release(self):
        first = self.publish()
        with patch('releases.shutil.disk_usage') as usage:
            usage.return_value.free = 50 * 1024 * 1024
            with self.assertRaises(ValueError):
                self.publish('0.4.2')
        self.assertTrue((self.data/'releases'/first/'windows.exe').exists())
        self.assertEqual(len(list((self.data/'releases').iterdir())), 1)

    def test_author_release_route_requires_both_correctly_named_packages(self):
        first = self.publish()
        self.login()
        response = self.submit(self.private+'/release', {'version':'0.4.2','notes':'一次真实更新',
                    'windows':(io.BytesIO(b'MZnew'),'Resonance-0.4.2.exe'),
                    'android':(io.BytesIO(b'PKnew'),'wrong-name.apk')})
        self.assertEqual(response.status_code, 400)
        self.assertTrue((self.data/'releases'/first).exists())
        response = self.submit(self.private+'/release', {'version':'0.4.2','notes':'版本边界测试',
                    'windows':(io.BytesIO(b'MZnew'),'Resonance-0.4.20.exe'),
                    'android':(io.BytesIO(b'PKnew'),'Resonance-0.4.2-release.apk')})
        self.assertEqual(response.status_code, 400)
        response = self.submit(self.private+'/release', {'version':'0.4.2','notes':'一次真实更新',
                    'windows':(io.BytesIO(b'MZnew'),'Resonance-0.4.2.exe'),
                    'android':(io.BytesIO(b'PKnew'),'Resonance-0.4.2-release.apk')})
        self.assertEqual(response.status_code, 303)
        self.assertFalse((self.data/'releases'/first).exists())

    def test_author_forms_render_and_announcement_is_escaped(self):
        self.login()
        self.assertEqual(self.client.get(self.private+'/dashboard').status_code, 200)
        self.assertEqual(self.submit(self.private+'/announcement', {'title':'新公告','body':'<script>test</script>'}).status_code, 303)
        html = self.client.get('/forum').get_data(as_text=True)
        self.assertIn('新公告', html)
        self.assertIn('&lt;script&gt;test&lt;/script&gt;', html)
        self.assertNotIn(self.private, html)

    def test_large_author_upload_uses_the_release_limit_not_public_limit(self):
        self.login()
        response = self.submit(self.private+'/release', {'version':'0.4.1','notes':'测试大文件上传',
                    'windows':(io.BytesIO(b'MZ'+b'x'*(5*1024*1024)),'Resonance-0.4.1.exe'),
                    'android':(io.BytesIO(b'PK'+b'y'*(1024*1024)),'Resonance-0.4.1-release.apk')})
        self.assertEqual(response.status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM assets')), 2)

    def test_latest_downloads_and_range_requests(self):
        self.publish()
        response = self.client.get('/download/windows')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data, b'MZtest-executable')
        self.assertIn('Resonance-0.4.1.exe', response.headers['Content-Disposition'])
        response.close()
        response = self.client.get('/download/windows', headers={'Range':'bytes=0-3'})
        self.assertEqual(response.status_code, 206)
        self.assertEqual(response.data, b'MZte')
        response.close()
        self.assertEqual(self.client.get('/download/windows', headers={'Range':'bytes=99999-'}).status_code, 416)
        self.assertEqual(self.client.get('/download/linux').status_code, 404)
        self.assertEqual(self.client.get('/media/secret.key').status_code, 404)
        self.assertEqual(self.client.get('/data/site.sqlite3').status_code, 404)

    def test_cleanup_never_deletes_outside_its_owned_directory(self):
        root = self.data/'releases'
        root.mkdir()
        with self.assertRaises(ValueError):
            safe_remove(root, self.data)
        with self.assertRaises(ValueError):
            safe_remove(root, root)
        self.publish()
        cleanup(self.data,self.app.config['DATABASE'])
        self.assertEqual(len(list(root.iterdir())), 1)

    def test_public_upload_is_verified_and_reencoded_and_removed_with_post(self):
        from PIL import Image
        image = io.BytesIO()
        Image.new('RGB',(10,10),'green').save(image,'PNG')
        image.seek(0)
        post_id = self.add_post(image=(image,'test.png'))
        row = self.query('SELECT image FROM posts')[0]
        self.assertTrue(row['image'].endswith('.webp'))
        self.assertTrue((self.data/'uploads'/row['image']).exists())
        self.login()
        self.submit(self.private+f'/delete/post/{post_id}')
        self.assertFalse((self.data/'uploads'/row['image']).exists())

    def test_fake_svg_image_is_rejected(self):
        response = self.submit('/forum/new', {'topic':'2','title':'图片测试','body':'附图内容',
                                             'image':(io.BytesIO(b'<svg onload="alert(1)"></svg>'),'fake.png')})
        self.assertEqual(response.status_code, 400)
        self.assertEqual(len(self.query('SELECT * FROM posts')), 0)

    def test_post_survives_application_restart(self):
        self.add_post('持久化测试')
        app2 = create_app(dict(TESTING=True, DATA_DIR=self.data, SITE_URL='http://localhost', PRODUCTION=False))
        self.assertIn('持久化测试', app2.test_client().get('/forum').get_data(as_text=True))

    def test_author_campaign_requires_explicit_confirmation(self):
        campaign = self.add_campaign()
        self.login()
        self.assertEqual(self.submit(self.private+f'/campaign/{campaign}', {'action':'send'}).status_code, 400)
        self.assertEqual(self.submit(self.private+f'/campaign/{campaign}', {'action':'send','confirm':'yes'}).status_code, 303)
        self.assertEqual(self.submit(self.private+f'/campaign/{campaign}', {'action':'send','confirm':'yes'}).status_code, 400)

    def test_response_headers_and_owner_robots(self):
        response = self.client.get('/')
        self.assertEqual(response.headers['X-Content-Type-Options'], 'nosniff')
        self.assertIn("frame-ancestors 'none'", response.headers['Content-Security-Policy'])
        self.assertEqual(self.client.get(self.private).headers['X-Robots-Tag'], 'noindex, nofollow, noarchive')


if __name__ == '__main__':
    unittest.main()
