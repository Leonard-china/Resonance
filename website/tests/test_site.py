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

    def test_home_and_forum_announcement_link_and_content(self):
        home = self.client.get('/').get_data(as_text=True)
        self.assertIn('/forum#announcement', home)
        self.assertIn('详细介绍在“论坛公告”（点击就能跳转）', home)
        forum = self.client.get('/forum').get_data(as_text=True)
        self.assertIn('id="announcement"', forum)
        self.assertIn('Resonance 软件详细介绍与核心特色', forum)
        self.assertIn('本地优先与隐私安全', forum)

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

    def test_author_can_participate_and_replies_are_pinned_for_every_reader(self):
        post_id = self.add_post()
        self.submit(f'/forum/post/{post_id}', {'body':'访客早回复'})
        self.login()
        page = self.client.get(f'/forum/post/{post_id}').get_data(as_text=True)
        self.assertIn('当前以作者身份回复', page)
        self.assertNotIn('name="nickname"', page)
        self.submit(f'/forum/post/{post_id}', {'body':'作者第一条回复', 'nickname':'假名字'})
        self.submit(f'/forum/post/{post_id}', {'body':'作者第二条回复'})
        author_post = self.add_post('作者发布的主题')
        self.assertEqual(self.query('SELECT is_author FROM posts WHERE id=?', (author_post,))[0][0], 1)
        self.assertIn('author-badge', self.client.get(f'/forum/post/{author_post}').get_data(as_text=True))
        self.submit(self.private + '/logout')
        self.client.get(f'/forum/post/{post_id}')  # A fresh visitor gets a fresh CSRF token after logout.
        self.submit(f'/forum/post/{post_id}', {'body':'访客晚回复', 'nickname':'作者', 'is_author':'1'})
        page = self.client.get(f'/forum/post/{post_id}').get_data(as_text=True)
        positions = [page.index(text) for text in ('作者第一条回复','作者第二条回复','访客早回复','访客晚回复')]
        self.assertEqual(positions, sorted(positions))
        self.assertEqual(page.count('class="pinned-label"'), 2)
        self.assertIn('作者（访客）', page)
        flags = self.query('SELECT is_author FROM replies WHERE post_id=? ORDER BY id', (post_id,))
        self.assertEqual([row[0] for row in flags], [0,1,1,0])

    def test_existing_author_replies_stay_pinned_after_restart(self):
        post_id = self.add_post()
        with transaction(self.app.config['DATABASE']) as db:
            for body, verified in [('旧访客留言',0),('旧作者说明',1)]:
                db.execute('INSERT INTO replies(post_id,body,author,visitor,is_author,created_at) VALUES(?,?,?,?,?,1)',
                           (post_id,body,'历史用户','fixture',verified))
        app2 = create_app(dict(TESTING=True, DATA_DIR=self.data, SITE_URL='http://localhost', PRODUCTION=False))
        page = app2.test_client().get(f'/forum/post/{post_id}').get_data(as_text=True)
        self.assertLess(page.index('旧作者说明'), page.index('旧访客留言'))
        self.assertEqual(page.count('class="pinned-label"'), 1)

    def test_author_deletion_is_protected_and_cascades(self):
        post_id = self.add_post()
        self.submit(f'/forum/post/{post_id}', {'body':'待删除回复'})
        self.assertEqual(self.submit(self.private + f'/delete/post/{post_id}').status_code, 302)
        self.assertEqual(len(self.query('SELECT * FROM posts')), 1)
        self.login()
        self.assertEqual(self.submit(self.private + f'/delete/post/{post_id}').status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM posts')), 0)
        self.assertEqual(len(self.query('SELECT * FROM replies')), 0)

    def test_owner_can_create_topic_and_delete_empty_topic_with_guard(self):
        self.login()
        response = self.submit(self.private + '/topic/create', {'name':'听歌打卡','category':'其他','slug':'check-in'})
        self.assertEqual(response.status_code, 303)
        topics = self.query("SELECT * FROM topics WHERE slug='check-in'")
        self.assertEqual(len(topics), 1)
        self.assertEqual(topics[0]['name'], '听歌打卡')
        topic_id = topics[0]['id']

        self.submit('/forum/new', {'topic': str(topic_id), 'title':'打卡第一天', 'body':'今天听了周杰伦'})
        self.assertEqual(len(self.query('SELECT * FROM posts WHERE topic_id=?', (topic_id,))), 1)

        del_resp = self.submit(self.private + f'/topic/{topic_id}/delete')
        self.assertEqual(del_resp.status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM topics WHERE id=?', (topic_id,))), 1)

        post_id = self.query('SELECT id FROM posts WHERE topic_id=?', (topic_id,))[0][0]
        self.submit(self.private + f'/delete/post/{post_id}')
        del_empty_resp = self.submit(self.private + f'/topic/{topic_id}/delete')
        self.assertEqual(del_empty_resp.status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM topics WHERE id=?', (topic_id,))), 0)

    def test_owner_quick_delete_post_and_reply_by_query(self):
        self.login()
        post_id = self.add_post(body='内容包含广告')
        self.submit(f'/forum/post/{post_id}', {'body':'违规测试回复'})
        reply_id = self.query('SELECT id FROM replies WHERE post_id=?', (post_id,))[0][0]

        self.assertEqual(self.submit(self.private + '/delete/reply-by-query', {'query': str(reply_id)}).status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM replies WHERE id=?', (reply_id,))), 0)

        self.assertEqual(self.submit(self.private + '/delete/post-by-query', {'query': f'https://leonard.asia/forum/post/{post_id}'}).status_code, 303)
        self.assertEqual(len(self.query('SELECT * FROM posts WHERE id=?', (post_id,))), 0)

    def test_owner_can_pin_and_unpin_post(self):
        p1 = self.add_post(body='第一篇普通讨论')
        time.sleep(1)
        p2 = self.add_post(body='第二篇后发的讨论')
        forum_html = self.client.get('/forum').get_data(as_text=True)
        self.assertLess(forum_html.index('第二篇后发的讨论'), forum_html.index('第一篇普通讨论'))

        self.login()
        self.assertEqual(self.submit(self.private + f'/pin/post/{p1}').status_code, 303)
        row = self.query('SELECT is_pinned FROM posts WHERE id=?', (p1,))[0]
        self.assertEqual(row['is_pinned'], 1)

        forum_html = self.client.get('/forum').get_data(as_text=True)
        self.assertLess(forum_html.index('第一篇普通讨论'), forum_html.index('第二篇后发的讨论'))
        self.assertIn('class="badge pinned-post-badge"', forum_html)

        self.assertEqual(self.submit(self.private + f'/pin/post/{p1}').status_code, 303)
        row = self.query('SELECT is_pinned FROM posts WHERE id=?', (p1,))[0]
        self.assertEqual(row['is_pinned'], 0)

    def test_owner_dashboard_stats_and_post_search(self):
        self.login()
        self.add_post(body='搜索目标专属词汇ResonanceBug')
        self.add_post(body='完全不相关的帖子内容')

        dash = self.client.get(self.private + '/dashboard').get_data(as_text=True)
        self.assertIn('搜索目标专属词汇ResonanceBug', dash)
        self.assertIn('完全不相关的帖子内容', dash)
        self.assertIn('论坛帖子', dash)
        self.assertIn('开放话题', dash)

        search_res = self.client.get(self.private + '/dashboard?q=ResonanceBug#moderation').get_data(as_text=True)
        self.assertIn('搜索目标专属词汇ResonanceBug', search_res)
        self.assertNotIn('完全不相关的帖子内容', search_res)

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
        self.assertEqual(response.headers['Cache-Control'], 'public, max-age=86400, stale-while-revalidate=3600')
        self.assertNotIn('Set-Cookie', response.headers)
        response.close()
        downloads_html = self.client.get('/downloads').get_data(as_text=True)
        self.assertIn('https://gh-proxy.com/', downloads_html)
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

    def test_versioned_public_assets_cache_without_cookies(self):
        html = self.client.get('/').get_data(as_text=True)
        asset = re.search(r'href="(/static/site.css\?v=[a-f0-9]{16})"', html).group(1)
        for client in (self.client, self.app.test_client()):
            with client.get(asset) as response:
                self.assertEqual(response.status_code, 200)
                self.assertEqual(response.headers['Cache-Control'], 'public, max-age=31536000, immutable')
                self.assertNotIn('Set-Cookie', response.headers)
                self.assertNotIn('Cookie', response.headers.get('Vary', ''))
                with client.get(asset, headers={'If-None-Match':response.headers['ETag']}) as conditional:
                    self.assertEqual(conditional.status_code, 304)
        self.assertEqual(self.client.get('/').headers['Cache-Control'], 'no-store')
        self.login()
        self.assertEqual(self.client.get(self.private+'/dashboard').headers['Cache-Control'], 'no-store')

    def test_unversioned_or_wrong_asset_versions_do_not_get_immutable_cache(self):
        for path in ('/static/site.css','/static/site.css?v=wrong','/static/missing.css?v=wrong'):
            with self.client.get(path) as response:
                self.assertNotIn('immutable', response.headers['Cache-Control'])

    def test_asset_version_matches_bytes_and_changes_with_content(self):
        import hashlib
        from flask import url_for
        with self.app.test_request_context():
            url = url_for('static', filename='site.css')
        version = url.split('v=')[1]
        self.assertEqual(version, hashlib.sha256((Path(__file__).resolve().parents[1]/'static/site.css').read_bytes()).hexdigest()[:16])
        fixture_root = self.data/'changed-source'
        (fixture_root/'static').mkdir(parents=True)
        (fixture_root/'static/site.css').write_text('/* changed asset fixture */', encoding='utf-8')
        with patch('app.ROOT', fixture_root):
            app2 = create_app(dict(TESTING=True, DATA_DIR=self.data, SITE_URL='http://localhost', PRODUCTION=False))
        with app2.test_request_context():
            self.assertNotEqual(url, url_for('static', filename='site.css'))


if __name__ == '__main__':
    unittest.main()
