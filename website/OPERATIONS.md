# leonard.asia 现网运维

2026-11-06 已受控发布 0.6.9。官网备份数据库及旧包，在候选副本通过 45 项测试后事务切换三端下载。源站和公网 Range 校验通过。[发布记录](../docs/changes/2026-11-06-07-publish-0.6.9-release.md)

2026-11-06 已受控发布 0.6.8。官网备份数据库及旧包，在候选副本通过 45 项测试后事务切换三端下载。源站和公网 Range 校验通过。[发布记录](../docs/changes/2026-11-06-02-publish-0.6.8-assistant-release.md)

2026-11-06 已受控发布 0.6.7。服务器从 GitHub 下载三端安装包并核对 SHA-256，备份 SQLite 和旧包，在候选副本通过 45 项回归后事务切换下载。公网首页、下载页、论坛以及三端 Range 响应均已核对。[发布记录](../docs/changes/2026-11-05-43-publish-0.6.7-harness-release.md)

2026-11-05：0.6.6已公开GitHub并受控切换官网三端，首页/五图/下载页更新，备份及公开校验见[本轮发布](../docs/changes/2026-11-05-29-publish-three-platform-release.md)。仅HUP Gunicorn master，保留Java启动器、Tunnel、中继和运行数据；未发邮件。服务器默认临时盘约56MiB不足发布保护的100MiB，候选回归须将TMPDIR/TEMP/TMP指向网站数据所在文件系统的独立目录，45项通过后再切换。普通首页/下载页缓存仍可能旧版，用户选择等待自然更新，不把源站上线写成全网已刷新；即时检查使用`/?v=0.6.6`及`/downloads?v=0.6.6`。

2026-11-04 三端发布核对：Linux 便携包加入原受控 `releases.publish` 流程，Windows／Android 仍为必需，Linux 可选；三端同时校验后事务切换。部署保留 `.env`、运行数据、原启动器及中继；先做 SQLite 在线备份和原包备份，再导入全部实际产物。成功状态需通过数据库三端哈希、公开下载页与 Range 检查确认，不能仅凭发送命令成功；本轮实际结果见[发布记录](../docs/changes/2026-11-04-20-publish-0.6.6-three-platforms.md)。

2026-11-05：一起听已从客户端和部署源码移除，阿里云准备脚本仅保留官网／邮件两个服务，见[部署入口](../services/deploy/aliyun/README.md)。本轮未重新部署或停用远端服务；旧迁移／中继验收只作为历史，不代表当前状态。

官网基线核对日期：2026-10-17；一起听共存部署补充核对：2026-11-02。官网：[leonard.asia](https://leonard.asia)，下载：[downloads](https://leonard.asia/downloads)，论坛：[forum](https://leonard.asia/forum)。本文不保存后台私有地址、密码、API Key 或 Cloudflare token。

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

`data/site.sqlite3` 为 WAL 数据库，`data/owner.json` 保存作者散列及入口，`data/secret.key` 用于会话和退订链接；均需私有保存。只更新页面代码时不得覆盖或重建这些文件。邮件队列是否真实投递需要单独验证 SMTP，不能从 worker 进程存在推断发送成功；不向真实订阅者投递测试内容。现网已于 2026-10-17 正式配置 QQ 邮箱 SMTP（465 端口 SSL/TLS 发信），生产环境已开放双重确认邮件订阅与版本更新/推广邮件投递，见 [SMTP配置与订阅开放记录](../docs/changes/2026-10-17-22-configure-production-smtp-and-enable-email-subscription.md)。

本次生产检查记录见 [运维交接](../docs/changes/2026-10-16-20-production-site-handoff.md)。原生播放器的构建和版本不由网站部署自动变更。

## 边缘缓存与国内直连加速（2026-10-17）

- 现网在 Cloudflare 控制台（`Caching → Cache Rules`）配置了 `Cache Public Pages` 规则，表达式 `http.request.uri.path in {"/" "/downloads" "/support"}`，动作 `Eligible for cache` 并设置 Edge TTL 覆盖源站为 2 小时（`Cache-Control: max-age=14400`）。国内访客打开公开页面由离用户最近的 Anycast 节点直接命中返回（`cf-cache-status: HIT`），无需穿透跨洋隧道回源。
- 论坛（`/forum`）及作者后台保持排除在缓存规则外（`cf-cache-status: DYNAMIC`），保持匿名发帖与 CSRF 实时隔离。
- 发布新版本播放器或修改公开页面内容后，如需全网立刻同步最新内容，可至 Cloudflare 控制台 `Caching → Configuration → Purge Everything` 清除全部缓存，或按 URL 清除对应路径缓存。见 [边缘缓存优化记录](../docs/changes/2026-10-17-10-cloudflare-edge-cache-for-public-pages.md)。
- 官网下载页面下载通道配置：主按钮保持为免费免登录的 GitHub 镜像直链（`gh-proxy.com`），辅助链接提供【官网直连备用】。原【123云盘备用】通道已于 2026-10-18 下线移除。后续大版本可通过 `D:\Project\mcServer\deploy_resonance.py` 一键同步至生产容器。



## 历史：一起听独立中继（2026-11-02，2026-11-05 源码退役）

用户明确批准后，新增 `/home/container/resonance-relay/` 独立 venv/服务和 `/home/container/resonance-relay-data/` 私有预算目录，回环 45902，每日 UTC 2 GiB 应用负载。start.sh 在原网站命令前加入独立 supervisor，网站退出时回收 relay；连续失败最多 5 次，Linux 文件锁防止重复写预算。

Tunnel 现场实际为本地管理，dashboard 路由不可编辑。新增此前不存在的 `/home/container/.cloudflared/config.yml`：leonard.asia 的精确 `^/resonance-relay/v1$` 优先回源 45902，其他请求继续 50034；配置不含凭据，不改 DNS、Token 或 server.jar。新版网站同步脚本仍须保留这些独立运行目录/文件。

启动备份 `/home/container/resonance-relay-backups/20261102/start.sh`。已验证两次 restart、单监听/预算保留、Linux 9 项隔离测试、Kotlin 4 项定向测试、公网 WSS 与官网首页/下载/论坛 200。网站源码/数据及邮件配置未修改；没有发送测试邮件。真实独立网络播放、容量、边缘限速和回滚演练未完成。精确摘要、回滚次序与验收边界见 [上线记录](../docs/changes/2026-11-02-04-public-relay-deployment.md)。
