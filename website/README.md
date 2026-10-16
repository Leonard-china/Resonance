# Resonance 官网

独立于 Kotlin 播放器的自托管网站。浅灰绿界面，支持电脑、平板和手机；无需注册，论坛采用帖子与回复。

## 已实现

- 首页使用应用当前蓝绿音符图标，四个入口：下载、论坛、GitHub、充电。
- 下载页显示已发布版本、更新内容、Windows EXE / Android APK、文件大小与实际 SHA-256，支持 HTTP Range 断点下载。
- 作者同时上传两端新包：流式写入临时目录 → 格式、版本和空间检查 → SQLite 事务切换最新版 → 删除旧包。历史版本只提供 GitHub Release 地址，不在服务器归档旧安装包。
- 匿名帖子、回复、昵称、截图；UI / 功能 / 其他话题。用户申请新话题，作者批准后才公开。
- 公告、邮箱确认订阅、推广单独选择且默认不勾选、退订、持久邮件队列与失败重试。
- 随机私有作者地址、密码登录、8 小时作者会话、公告更新、留言删除、话题审核、版本发布、邮件预览/测试/发送、收款码上传。

没有默认作者密码，公共页面、源码中的公共资源、robots.txt 均不包含实际作者地址。收款码未提供时展示「收款码待添加」；SMTP 未配置时订阅明确关闭，不假装成功。

## 本地运行

需要 Python 3.10 或更高版本（本次验证为 3.12.14）。Windows 使用 `python`；Ubuntu 通常使用 `python3`。

```powershell
cd website
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe app.py
```

打开 <http://127.0.0.1:8860>。开发服务器仅用于本地预览，Ubuntu 部署使用下文 Gunicorn 配置。

需要作者功能时，在网站目录运行以下命令，自行设置至少 12 位密码。入口只在服务器终端显示。设置或修改密码后重启网站；旧作者会话自动失效。

```powershell
.\.venv\Scripts\python.exe manage.py setup
.\.venv\Scripts\python.exe manage.py owner-url
```

在完整 Resonance 工程中，可把当前 `version.properties`、对应发布说明和实际发布产物导入官网：

```powershell
.\.venv\Scripts\python.exe manage.py import-release
```

该命令读取工程中的版本唯一来源，不修改应用版本。自定义位置可用 `--version-file`、`--windows`、`--android`、`--notes`。从源码部署包安装的网站没有应用工程；首次版本直接在作者页面上传两个实际安装包即可。

## Ubuntu 部署

以下假定网站代码位于 `/opt/resonance/website`，运行数据位于 `/var/lib/resonance-site`。代码包中不含任何密码、实际作者入口、订阅数据或安装包。

1. 在本机运行 `python website/package.py`，得到 `build/website/Resonance-website.zip`，上传到服务器。
2. 服务器安装依赖并解压（已有同名用户时跳过创建）：

```bash
sudo apt update
sudo apt install python3 python3-venv nginx unzip certbot python3-certbot-nginx
sudo useradd --system --home-dir /opt/resonance --shell /usr/sbin/nologin resonance
sudo mkdir -p /opt/resonance
sudo unzip Resonance-website.zip -d /opt/resonance
cd /opt/resonance/website
sudo python3 -m venv .venv
sudo .venv/bin/pip install -r requirements.txt
sudo install -d -m 700 -o resonance -g resonance /var/lib/resonance-site
sudo cp .env.example .env
sudo chown root:resonance .env
sudo chmod 640 .env
sudo nano .env
```

3. 将 `.env` 中的 `SITE_URL` 改为你的真实 HTTPS 域名，`DATA_DIR` 改为 `/var/lib/resonance-site`，保留 `PRODUCTION=1` 与 `TRUST_PROXY=1`。SMTP 可稍后配置。不要把真实 `.env`、`data` 或数据库上传到 GitHub。
4. 初始化作者密码：

```bash
sudo -u resonance .venv/bin/python manage.py setup
sudo cp deploy/resonance-site.service deploy/resonance-mail.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now resonance-site resonance-mail
sudo cp deploy/nginx.conf /etc/nginx/sites-available/resonance-site
sudo nano /etc/nginx/sites-available/resonance-site
```

5. 把 Nginx 中的 `music.example.com` 改成域名；确认域名 A/AAAA 指向这台服务器，再启用 HTTPS：

```bash
sudo ln -s /etc/nginx/sites-available/resonance-site /etc/nginx/sites-enabled/resonance-site
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d 你的域名
```

网站与邮件任务需使用同一份 `.env`、同一个数据目录。提供的配置使用 1 个 Gunicorn 进程、4 个线程，网站内存上限 512 MB，邮件任务上限 192 MB；没有前端构建或常驻 Node 服务。**这些是配置限额，不是已完成的服务器压力测试结果。**

应用仅监听 `127.0.0.1:8860`，由 Nginx 终止 HTTPS。只在此单层代理结构下开启 `TRUST_PROXY=1`，Nginx 会覆盖客户端伪造的转发地址。不要把 8860 端口直接对外开放。

生产配置参考 [Flask Gunicorn 部署说明](https://flask.palletsprojects.com/en/stable/deploying/gunicorn/)、[Nginx](https://flask.palletsprojects.com/en/stable/deploying/nginx/) 和 [代理信任设置](https://flask.palletsprojects.com/en/stable/deploying/proxy_fix/)。

## 更新与磁盘占用

作者页面上传 EXE 与 APK，文件名必须包含填写的应用实际版本号，新版本必须高于本站当前版本。网站不会替播放器升版。

校验完成之前旧版本继续提供下载；数据库切换成功后立即清理旧包。Linux 已打开的下载可继续完成；Windows 正在使用的旧包由邮件/清理任务每分钟重试删除。`python manage.py cleanup` 可手动重试。中断遗留的临时目录在超过 1 小时后清理，未提交的目录不会成为公开下载。

更新时会暂时同时占用旧包、新包及上传临时文件空间，需要约 1 GB 可用空间（以实际安装包大小为准）；完成后仅保留一套最新版。单包上限 400 MB，两包总请求上限 512 MB。截图单张上限 3 MB / 1200 万像素，转成最长边 1600px 的 WebP，去掉 EXIF；收款码采用无损编码，图片总空间上限 256 MB。留言附图删除时一并移除。

旧版发布记录保留在数据库，下载跳转 GitHub，不保留历史安装包。首次导入的本机安装包也只是复制，不会移动或删除原工程中的发布产物。

## 邮件

配置 `.env` 中的 `SMTP_HOST`、`SMTP_PORT`、`SMTP_SECURITY`、`SMTP_USER`、`SMTP_PASSWORD`、`MAIL_FROM`，`MAIL_TEST_TO` 填自己的测试邮箱。支持 465 TLS 或 587 STARTTLS，证书正常验证，不支持明文 SMTP。

- 用户提交订阅后先收到确认邮件，打开页面再点击确认。链接 48 小时有效；邮件扫描器的 GET 不会自动订阅。
- 「版本更新」和「推广与活动」是独立偏好，推广默认不选。再次申请修改偏好，也需通过邮件确认。
- 作者先保存草稿，预览受众数量，可发给配置的作者测试邮箱；勾选确认后才能建立群发队列。
- 所有邮件通过 `python manage.py worker` 发送；默认每封间隔 3 秒。重启保留队列，失败最多尝试 5 次，逐步延迟。
- 每位收件人单独收到邮件，推广仅发送给已确认且主动选择推广的用户。发送前再次检查退订/偏好，附退订入口并支持邮件客户端的一键退订。
- 成功和取消任务会清空邮件正文以节省空间，保留发送状态。worker 在 SMTP 成功后、记录成功前若异常中断，重试可能导致极少量重复邮件；不承诺 SMTP 的恰好一次送达。
- 配置 SMTP 并启动邮件任务后，先通过作者测试邮箱验证真实投递，再进行群发。修改配置后重启两个 systemd 服务。

## 管理和备份

作者页面的日常操作：

1. 在服务器执行 `manage.py setup` 设置密码并保存显示的私有地址；重启网站后，用该地址登录。公共页面没有后台入口，忘记地址可执行 `manage.py owner-url` 查看。
2. 「公告」可更新论坛顶部的通知或推广文字；「话题申请」可批准或拒绝用户申请；帖子详情中的作者操作可删除帖子和回复。
3. 「发布版本」填写真实版本号与更新内容，同时选择对应的 Windows EXE 和 Android APK。发布成功后下载页才切换，并清理旧包；失败时仍提供原版本。
4. 「邮件」选择版本更新或推广受众，先保存草稿、查看人数，并发到自己的测试邮箱确认，再建立发送队列。发布安装包不会自动群发邮件；用户未主动订阅推广时不会收到推广邮件。
5. 「充电收款码」分别上传微信与支付宝收款码。未上传时，公共页面继续显示待添加。
6. 用完点击退出。修改密码后重启网站，已有作者会话会失效。

SMTP 未配置时，可以正常使用论坛、下载和其他作者操作；订阅与邮件发送会明确提示尚未启用。部署包不附带作者密码、私有地址或安装包，需要在服务器完成首次设置。

```bash
# 在网站目录执行；按实际运行账户与路径调整
sudo -u resonance .venv/bin/python manage.py owner-url
sudo systemctl status resonance-site resonance-mail
sudo journalctl -u resonance-site -u resonance-mail --since today
sudo -u resonance .venv/bin/python manage.py backup /var/lib/resonance-site/backup.sqlite3
```

需要保留的数据：`site.sqlite3`、`secret.key`、`owner.json`、`uploads/`，以及正在提供下载的 `releases/`。数据库备份通过 SQLite 在线备份 API，**不要仅复制运行中的主数据库而丢弃 WAL**。备份时也要保存密钥文件，否则现有会话和邮件退订链接会失效。真实凭据文件权限保持 600，数据目录保持 700。不要把备份放入 `static`，不要为整个数据目录设置 Nginx alias。

网站发布新版本与发送邮件是两个独立操作。发布不会未经预览自动群发。更新官网源代码时保留 `.env` 和数据目录，然后重启服务。

## 验证

```bash
python -m unittest discover -s tests -v
python -m compileall -q app.py db.py releases.py mailing.py manage.py
```

测试使用临时数据库、微型安装包夹具和模拟 SMTP，不访问用户音频，不真实发送邮件。发布测试覆盖失败保留旧版、拒绝降版、成功切换后清理旧包、Range 下载和目录边界；论坛/订阅测试覆盖 CSRF、匿名 XSS、审核、删除权限、确认、可选推广与退订。

页面在真实浏览器中检查 320 / 390 / 768 / 1440px 宽度。首页预览图来自工程已有的 UI 测试截图，带样例歌曲；它们不是本次官网任务拍摄的最新版播放器截图。
