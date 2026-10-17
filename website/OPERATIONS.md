# leonard.asia 现网运维

核对日期：2026-10-17。官网：[leonard.asia](https://leonard.asia)，下载：[downloads](https://leonard.asia/downloads)，论坛：[forum](https://leonard.asia/forum)。本文不保存后台私有地址、密码、API Key 或 Cloudflare token。

## 实际部署

- 用户交接：Pterodactyl 独立 Ubuntu 22.04.5 x86_64 容器，约 2 GB 内存、10 GB 存储；不是需要重新配置的完整 systemd 主机。
- 工作目录 `/home/container`，`server.jar` 为 Java 启动器，负责守护网站及 cloudflared。控制台 `restart` 会重启网站与隧道；不要替换成 Minecraft 重启指令或覆盖启动器。
- `start.sh` 启动 `website/.venv` 的 Gunicorn（1 worker / 4 threads）和 `manage.py worker`，监听 `127.0.0.1:50034`。已通过指定脚本读取确认端口与两个进程命令存在；没有执行崩溃恢复演练。
- Cloudflare Tunnel 将 `https://leonard.asia` 转发到上述端口，token 仅由容器私有文件加载。不能把根目录 token 文件或整个数据目录变成静态站点。
- 源码 `/home/container/website`，运行数据 `/home/container/website/data`；Python 虚拟环境由容器内 uv 管理。源码 `deploy/gunicorn.conf.py` 的默认 8860 属于通用部署方案，不能误认为现网监听端口。

## 运维入口与边界

本机另有用户配置的运维工程，包含 `mc_api.py` 的 `PterodactylClient` 和 `deploy_resonance.py`。认证配置留在该工程本机，不复制进 Resonance 仓库。客户端可读文件、查资源、上传和发送控制台指令；不要打印客户端配置或原始认证错误体，也不要输出带 token 的进程参数、完整日志或凭据文件。

面板目前配置为 HTTP；本次不带认证的 HTTPS 探测发生 TLS 握手超时，没有禁用证书校验。需要改进控制面传输时，应先确认面板实际支持的 HTTPS/SFTP 入口，再变更客户端，不把地址机械替换后声称已验证。

现有一键同步脚本只上传网站源码/模板/静态资源和本机版本文件，再发送解压与 restart。**它不上传播放器 EXE/APK，不调用受控安装包发布，不安装新依赖，也没有检查远端命令退出码、备份或回滚。** 代码更新结束不等于下载页已更新，更不能仅凭固定 sleep 后打印“完成”作验收。

## 更新步骤

1. 检查仓库与线上待更新源文件的差异；保留另一工具已做的生产改动。先在临时数据库完成回归与手机/电脑浏览器验证。
2. 对待替换的源码做备份；数据库使用 SQLite 在线备份，同时另行保护 `secret.key`、`owner.json`、上传图片与当前安装包。不要将生产数据库拷进本机 UI 测试。
3. 上传明确白名单代码包。保留 `.env`、`data/`、`.venv/`、`start.sh`、Java 启动器与 tunnel token。更改依赖时单独处理安装并验证。
4. 在固定目录内校验文件摘要并解压，检查真实退出结果；重启后验证首页、论坛、动态页不缓存、静态版本缓存及两端 Range 下载。失败时恢复备份源码并重新启动，保护同一份数据。
5. 发布播放器安装包使用作者页面或受控 `releases.publish`，必须两端一起通过校验后切换，成功后才清理旧包。不要通过把 version.properties 上传到网站冒充二进制更新。

## 作者、数据与邮件

作者从私有地址登录后，在同一浏览器访问论坛仍保留身份；回复会显示作者与置顶标签。现有改密方式为服务器终端 `website/.venv/bin/python manage.py setup`，需要交互式输入并重启服务；不把真实密码放在控制台命令、exec.log 或 Git 中。当前版本没有网页改密表单。

`data/site.sqlite3` 为 WAL 数据库，`data/owner.json` 保存作者散列及入口，`data/secret.key` 用于会话和退订链接；均需私有保存。只更新页面代码时不得覆盖或重建这些文件。邮件队列是否真实投递需要单独验证 SMTP，不能从 worker 进程存在推断发送成功；不向真实订阅者投递测试内容。

本次生产检查记录见 [运维交接](../docs/changes/2026-10-16-20-production-site-handoff.md)。原生播放器的构建和版本不由网站部署自动变更。

## 边缘缓存与国内直连加速（2026-10-17）

- 现网在 Cloudflare 控制台（`Caching → Cache Rules`）配置了 `Cache Public Pages` 规则，表达式 `http.request.uri.path in {"/" "/downloads" "/support"}`，动作 `Eligible for cache` 并设置 Edge TTL 覆盖源站为 2 小时（`Cache-Control: max-age=14400`）。国内访客打开公开页面由离用户最近的 Anycast 节点直接命中返回（`cf-cache-status: HIT`），无需穿透跨洋隧道回源。
- 论坛（`/forum`）及作者后台保持排除在缓存规则外（`cf-cache-status: DYNAMIC`），保持匿名发帖与 CSRF 实时隔离。
- 发布新版本播放器或修改公开页面内容后，如需全网立刻同步最新内容，可至 Cloudflare 控制台 `Caching → Configuration → Purge Everything` 清除全部缓存，或按 URL 清除对应路径缓存。见 [边缘缓存优化记录](../docs/changes/2026-10-17-10-cloudflare-edge-cache-for-public-pages.md)。
- 官网下载页面已配置 123 云盘国内下载直链：主按钮保持为免费免登录的 GitHub 镜像直链（`gh-proxy.com`），辅助链接并列提供【官网直连备用】与【123云盘备用】（Windows 映射 `https://4006883198.share.123pan.cn/123pan/MMXkwh-9CdCd`，Android 映射 `https://4006883198.share.123pan.cn/123pan/MMXkwh-pPBud`）。后续大版本可在 `templates/downloads.html` 更新 `pan_urls`，并通过 `D:\Project\mcServer\deploy_resonance.py` 一键同步至生产容器。见 [123云盘备用通道记录](../docs/changes/2026-10-17-16-integrate-123pan-high-speed-downloads.md)。


