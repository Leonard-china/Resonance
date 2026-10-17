"""Local setup, release import, mail worker and SQLite backup commands."""
import argparse
import getpass
import json
import os
import secrets
import sqlite3
import time
from datetime import datetime
from pathlib import Path

from werkzeug.security import generate_password_hash

from app import ROOT, create_app
from releases import VERSION, cleanup, publish
import mailing


def main():
    parser = argparse.ArgumentParser(description='Resonance 官网管理工具')
    sub = parser.add_subparsers(dest='command', required=True)
    sub.add_parser('setup', help='设置作者密码并生成私有入口（只在终端显示）')
    sub.add_parser('owner-url', help='在服务器终端查看私有入口')
    sub.add_parser('worker', help='处理邮件队列和旧安装包清理')
    sub.add_parser('cleanup', help='重试删除被占用的旧版安装包')
    backup = sub.add_parser('backup', help='在线备份数据库；不备份安装包')
    backup.add_argument('destination')
    release = sub.add_parser('import-release', help='从当前工程的真实发布产物导入最新版')
    release.add_argument('--version-file', default=str(ROOT.parent / 'version.properties'))
    release.add_argument('--windows')
    release.add_argument('--android')
    release.add_argument('--notes')
    announcement = sub.add_parser('publish-announcement', help='发布已审核的更新公告与软件介绍（须先发布对应安装包）')
    announcement.add_argument('content', help='含 version、title、body 的 UTF-8 JSON 文件')
    args = parser.parse_args()
    app = create_app()
    data = Path(app.config['DATA_DIR'])
    if args.command == 'setup':
        password = getpass.getpass('设置作者密码（至少 12 位，不会显示）：')
        if len(password) < 12 or len(password) > 256:
            raise SystemExit('密码需为 12–256 位。')
        if getpass.getpass('再输入一次：') != password:
            raise SystemExit('两次密码不一致，未修改配置。')
        owner_file = data / 'owner.json'
        old = json.loads(owner_file.read_text()) if owner_file.exists() else {}
        owner = {'path': old.get('path', 'owner-' + secrets.token_hex(16)),
                 'password_hash': generate_password_hash(password)}
        temporary = data / ('owner-' + secrets.token_hex(6) + '.tmp')
        with os.fdopen(os.open(str(temporary), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
            json.dump(owner, stream)
        os.replace(temporary, owner_file)
        print('作者入口：' + app.config['SITE_URL'] + '/' + owner['path'])
        print('请保存入口地址，并重启网站进程使新密码生效。')
    elif args.command == 'owner-url':
        if not app.config['OWNER_HASH']:
            raise SystemExit('请先运行 python manage.py setup。')
        print(app.config['SITE_URL'] + '/' + app.config['OWNER_PATH'])
    elif args.command == 'import-release':
        version_file = Path(args.version_file)
        values = dict(line.strip().split('=', 1) for line in version_file.read_text(encoding='utf-8-sig').splitlines()
                      if '=' in line and not line.startswith('#'))
        version = values.get('versionName', '')
        if not VERSION.fullmatch(version):
            raise SystemExit('version.properties 中没有有效的 versionName。')
        repo = version_file.resolve().parent
        notes_path = Path(args.notes) if args.notes else repo / 'docs' / f'RELEASE_NOTES-{version}.md'
        notes = notes_path.read_text(encoding='utf-8-sig')
        if '## 主要更新' in notes:
            notes = notes.split('## 主要更新', 1)[1].split('\n## ', 1)[0].strip().replace('**', '')
        windows = Path(args.windows) if args.windows else repo / 'release' / version / f'Resonance-{version}.exe'
        android = Path(args.android) if args.android else repo / 'release' / version / f'Resonance-{version}-release.apk'
        with windows.open('rb') as win, android.open('rb') as apk:
            publish(data, app.config['DATABASE'], version, notes, {'windows': win, 'android': apk})
        print(f'已导入 v{version} 的 Windows / Android 真实产物，并计算本次 SHA-256。')
    elif args.command == 'publish-announcement':
        from announcements import publish_announcement
        version = publish_announcement(app.config['DATABASE'], args.content)
        print(f'已发布 v{version} 更新公告与软件介绍；未创建邮件群发。')
    elif args.command == 'cleanup':
        cleanup(data, app.config['DATABASE'])
        print('旧版安装包清理已执行。')
    elif args.command == 'backup':
        target = Path(args.destination).resolve()
        if target == Path(app.config['DATABASE']).resolve():
            raise SystemExit('备份目标不能覆盖原数据库。')
        target.parent.mkdir(parents=True, exist_ok=True)
        with sqlite3.connect(app.config['DATABASE']) as source, sqlite3.connect(target) as destination:
            source.backup(destination)
        print('数据库备份完成。请另行保护 data/secret.key 和 data/owner.json。')
    elif args.command == 'worker':
        print('邮件与清理任务已启动。SMTP 未配置时不会发送邮件。', flush=True)
        last_cleanup = 0
        try:
            while True:
                if time.time() - last_cleanup > 60:
                    cleanup(data, app.config['DATABASE'])
                    last_cleanup = time.time()
                handled = mailing.process_one(app)
                time.sleep(app.config['MAIL_INTERVAL_SECONDS'] if handled else 10)
        except KeyboardInterrupt:
            print('任务已停止。')


if __name__ == '__main__':
    main()
