import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from announcements import publish_announcement
from db import connect, initialize


class AnnouncementTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.database = self.root / 'test.sqlite3'
        initialize(self.database)
        with connect(self.database) as db:
            db.execute("INSERT INTO releases VALUES ('fixture','0.4.6','fixture',1,'https://example.com')")
            db.execute("INSERT OR REPLACE INTO settings VALUES ('active_release','fixture')")
            db.execute("INSERT OR REPLACE INTO settings VALUES ('announcement_title','old')")

    def tearDown(self):
        self.temp.cleanup()

    def content(self, **changes):
        content = {'version': '0.4.6', 'title': '新版本', 'body': '更新公告与软件介绍'}
        content.update(changes)
        path = self.root / 'content.json'
        path.write_text(json.dumps(content), encoding='utf-8')
        return path

    def test_publication_only_updates_announcement_and_does_not_send_mail(self):
        self.assertEqual('0.4.6', publish_announcement(self.database, self.content()))
        with connect(self.database) as db:
            settings = dict(db.execute('SELECT key,value FROM settings'))
            self.assertEqual('新版本', settings['announcement_title'])
            self.assertEqual('更新公告与软件介绍', settings['announcement_body'])
            self.assertEqual('fixture', settings['active_release'])
            self.assertEqual(0, db.execute('SELECT count(*) FROM outbox').fetchone()[0])

    def test_wrong_release_and_invalid_body_preserve_previous_announcement(self):
        for changes in ({'version': '0.4.7'}, {'body': ''}, {'title': 'x' * 121}):
            with self.assertRaises(ValueError):
                publish_announcement(self.database, self.content(**changes))
            with connect(self.database) as db:
                self.assertEqual('old', db.execute("SELECT value FROM settings WHERE key='announcement_title'").fetchone()[0])

    def test_checked_in_public_content_matches_release_and_explains_network_boundary(self):
        content = json.loads((Path(__file__).resolve().parents[1] / 'content/0.4.6.json').read_text(encoding='utf-8'))
        self.assertEqual('0.4.6', content['version'])
        self.assertIn('本机', content['body'])
        self.assertIn('仍需联网', content['body'])
        self.assertIn('软件介绍', content['title'])

    def test_direct_download_urls_change_with_release_to_avoid_old_cached_installer(self):
        from app import create_app
        app = create_app(dict(TESTING=True, DATA_DIR=self.root / 'site', SITE_URL='http://localhost',
                              PRODUCTION=False, SMTP_HOST='', OWNER_HASH='', DISABLE_RATE_LIMITS=True))
        with connect(app.config['DATABASE']) as db:
            db.execute("INSERT INTO releases VALUES ('fixture','0.4.6','fixture',1,'https://example.com')")
            db.execute("INSERT OR REPLACE INTO settings VALUES ('active_release','fixture')")
            for platform, extension in (('windows', 'exe'), ('android', 'apk')):
                db.execute('INSERT INTO assets VALUES (?,?,?,?,?)', ('fixture', platform, platform + '.' + extension, 16, 'a' * 64))
        with app.test_client() as client:
            response = client.get('/downloads')
            self.assertEqual(200, response.status_code)
            self.assertIn('href="/download/windows?v=0.4.6"', response.get_data(as_text=True))
            self.assertIn('href="/download/android?v=0.4.6"', response.get_data(as_text=True))
