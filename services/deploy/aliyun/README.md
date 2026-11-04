# 2026-11-03 — 阿里云 Ubuntu 官网与一起听迁移

目标：Ubuntu 24.04、2 vCPU／1 GiB，直接 Nginx HTTPS → 官网 8860／一起听 WebSocket 45902。三个独立 systemd 服务，不使用翼龙、Java 启动器或 Cloudflare Tunnel。本次已安装三项 service，恢复网站迁移副本并启动官网／中继，回环和重启验证通过。邮件 worker 未启动，Nginx 保持 mask；**公开切换待续费备案、最终同步、受信证书与 DNS，原域名仍走旧入口**。

## 连接与公开入口

控制台可用现有命令助手检查主机，无需重设密码。SSH 需已有合法登录方式；没有现成登录方式时，需要用户允许后才能添加临时 SSH 授权；本次已获允许，专用公钥有两小时有效期且禁止转发，本次收尾已撤销并验证无法再登录，本机临时私钥已删除。后续接续需使用命令助手或重新授权，私钥不入库。域名继续使用 `leonard.asia`，客户端默认地址继续为 `wss://leonard.asia/resonance-relay/v1`，无需把 IP 写入客户端或关闭证书校验。

北京属中国内地。公开网站／域名接入前确认 ICP 备案及阿里云接入要求，不能把安装成功当作公网可访问。依据：[阿里云使用须知](https://www.alibabacloud.com/help/en/simple-application-server/product-overview/usage-notes)。本次在已登录备案页面确认该账号“尚无备案信息”；实例当前购买期一个月，而阿里云要求用于备案的轻量服务器累计购买至少三个月。续费、购买和实名提交由用户处理，不自动切断旧入口。

## 源码包与安装准备

在仓库执行：

```bash
python3 services/deploy/aliyun/package.py
```

得到 `build/aliyun/Resonance-aliyun.zip` 与实际生成的 `.zip.sha256`。官网沿用 `website/package.py` 白名单，另含中继源码、独立 service 及本目录；没有 `.env`、数据库、作者入口、签名文件、安装包、预算或本机工具。上传后在服务器校验 SHA-256；新主机创建 `/opt/resonance` 并解压到该目录。已有主机先备份源码，不用覆盖解压更新现有生产安装。

以下为首次安装命令，本次已执行；后续重复使用前先确认目标机已有状态：

```bash
apt-get update
apt-get install -y python3 python3-venv nginx unzip certbot
mkdir -p /opt/resonance
unzip Resonance-aliyun.zip -d /opt/resonance
sh /opt/resonance/services/deploy/aliyun/prepare.sh
```

`prepare.sh` 只在固定目录创建系统账户、私有数据目录、两套 venv、三个 service 和 1 GiB 主机的内存限制；已有 `.env` 不覆盖。不会启动服务、改 DNS、装入 Nginx 站点、重置作者密码或操作旧主机。官网／邮件／中继内存上限分别为 320／96／128 MiB。2026-11-03 的短时回环负载结果见下文，不能据此推定长期容量。

## 迁移现有数据

本次快照及校验回执在目标机私有目录 `/root/resonance-migration/20261103/`，数据副本在 `/var/lib/resonance-site`。当前只是准备副本，旧站继续接收更新；备案通过后的最终同步仍需执行。旧下载接口仅 HTTP，本次先加密再传，采用有界内存的 RSA-OAEP 包装随机口令＋AES-256-CBC／PBKDF2 流式加解密，并核对密文和完整明文 SHA-256。第一次 CMS 大包解密触发 OOM，临时交换空间尝试后已撤销；最终不依赖该交换文件。

这不是空站初始化。使用旧主机现有的安全运维入口取出既有数据，私密传输，不能把备份放在网站静态目录或公开下载路径。

1. 记录原 DNS／Tunnel 路由和源码版本；旧网站持续服务直至切换窗口。先在旧网站目录运行 `manage.py backup`，使用 SQLite 在线备份 API，不能单独复制正在运行的 WAL 主数据库。
2. 切换窗口暂时停止旧网站写入和邮件 worker，避免备份后丢帖、双重群发。做最终 SQLite 备份并一起转移 `secret.key`、`owner.json`、`uploads/`、`releases/` 和真实 `.env`。密钥和环境文件只做私有转移，不显示其值、不重新生成。停止写入到最终切换期间的用户请求须明确处理，不能声称零停机。
3. 数据恢复到 `/var/lib/resonance-site`，保留原作者身份、退订链接和安装包；`.env` 仅改 `SITE_URL=https://leonard.asia`、`DATA_DIR=/var/lib/resonance-site`、`PRODUCTION=1`、`TRUST_PROXY=1`，其余 SMTP 配置原样保留。数据目录归 `resonance` 且 0700，凭据文件仅服务账户可读；`.env` 采用 root:resonance／0640。
4. 停止旧中继后，私密转移当天 `budget.json` 到 `/var/lib/resonance-relay/budget.json`，不要通过迁移或重启归零。运行中的房间会断线，需要新邀请。
5. 开启新官网和中继前，用恢复后的数据库执行只读完整性检查、核对有效发布及文件摘要。真实业务回归使用临时库；不向订阅者发送测试邮件。

## HTTPS、Nginx 与切换

在 DNS 切换前取得公开受信任的 `leonard.asia` 证书，例如通过 DNS-01；私钥保留在新主机。证书路径须与 `website/deploy/nginx-aliyun.conf` 相符。选用支持现有 DNS 提供方的续期方式并测试续期；本仓库不含真实 DNS Token，也未替用户完成证书申请。

正式同步时先停止新机官网／中继并备份准备副本，在新的私有暂存目录解压最终快照、验证后交换数据目录并修正权限；若新机已有真实新写入，须先合并，不能直接覆盖。停止旧邮件 worker 并转移最终队列后，才启动新邮件 worker。

数据和证书就绪后：

```bash
systemctl enable --now resonance-site resonance-relay
install -m 644 /opt/resonance/website/deploy/nginx-aliyun.conf /etc/nginx/sites-available/resonance
ln -s /etc/nginx/sites-available/resonance /etc/nginx/sites-enabled/resonance
nginx -t
systemctl unmask nginx
systemctl enable --now nginx
systemctl reload nginx
```

新配置只代理精确 `/resonance-relay/v1` 到 45902，其余到官网 8860。Nginx 覆盖 `X-Real-IP`／`X-Forwarded-For`，中继使用 `--trust-nginx`，不信任访客传来的 Cloudflare 头；后端仅绑定回环，不能公开 8860／45902。WebSocket Upgrade 按 [Nginx 官方说明](https://nginx.org/en/docs/http/websocket.html) 显式转发。

先使用 `curl --resolve leonard.asia:443:<新IP> https://leonard.asia/` 验证证书和新源站，核对 `/downloads`、`/forum`、无登录／私密 URL 访问日志、静态缓存、两个实际安装包的 Range。公网端口仅需已批准的 80/443 和现有管理入口；不清空已有防火墙。

再把该域名原 Tunnel DNS 记录替换为新主机 A 记录；没有可用 IPv6 时不要留下指向旧服务的 AAAA。若用户目标为直接访问，关闭该记录的 CDN 代理并确认解析真的落到新主机；不取消整个 Cloudflare 账号或更改其他域名。确认旧邮件 worker 停止后，才在新主机启动 `resonance-mail`，不自动新建群发任务。

客户端新版中，带中继地址的邀请只走服务器，失败不会退回设备直连；新房间的本地监听限于回环，邀请不公布设备地址。旧版无服务器邀请保持兼容，已保存的自定义或关闭状态不覆盖。房间状态仍由创建者设备协调，内层 TLS 保留；本次并非把曲库和房间业务状态改成服务器数据库。

## 验收与回滚

在目标主机执行隔离 Python 测试，检查 systemd 重启及预算保留；使用显式生产验收地址验证 WSS 和 Kotlin 内层 TLS，再做 Android／Windows／Linux 不同网络的实际音频、聊天、退出与恢复。只读首页返回 200 不等于一起听验收。容量继续为每日 UTC 2 GiB 应用负载、每会话 512 MiB／2 小时，未因主机迁移提高；已测回环合成负载，真实多人房间与长时容量尚未测量。

切换失败时停止新站写入和新邮件 worker，先比对／保留切换后新帖子及队列，再恢复原 DNS 和旧服务；不能把旧数据库覆盖到已有新写入的数据上。新机数据库、预算与源码保留作排查，不能通过删除数据回滚。正式关闭旧主机必须在新入口验收通过后；本准备脚本不操作旧主机。

本次新中继只做配对握手，没有转发音频／聊天负载，预算文件尚未创建；正式切换前必须停止旧写者并迁入当日预算。不能因新机已运行就跳过这一步。常听伙伴直接邀请和房间协调仍有设备侧职责，客户端源码本次未发布。

## 有界部署性能测试（2026-11-03）

`benchmark.py` 仅在已准备好的目标机上运行，由 root 启动私有回环 Nginx／中继夹具，访问现有官网迁移副本。须确认没有其他人在执行同类测试；脚本有排他锁、100 秒总时限、低内存保护及临时中继资源上限。生产 Nginx、DNS、邮件 worker、凭据和生产中继预算均不改动。

```bash
/opt/resonance/services/relay/.venv/bin/python /opt/resonance/services/deploy/aliyun/benchmark.py
RESONANCE_BENCHMARK_PROFILE=steady-relay /opt/resonance/services/relay/.venv/bin/python /opt/resonance/services/deploy/aliyun/benchmark.py
```

第一条对五个只读页面执行 1／4／12 并发，取两端安装包各 16 MiB 的 Range，再混合页面／下载并测 1／3／6 对 WSS 的突发字节回传。第二条仅测 1 对空闲探测和 6 对限速传输（每方向约 256 KiB/s）。这些连接对不是六人原生房间；回传字节和探测消息没有运行播放器协议或解码器。测试 TLS 客户端显式信任当次夹具证书，不关闭验证。

报告写入目标机 `/root/resonance-migration/resonance-benchmark-*.json`，同时打印 JSON；必须检查各阶段错误数、`failure` 和末尾三项恢复检查，不能只看命令助手的“执行完成”。当前脚本遇到请求错误或恢复检查失败会返回非零。两项本机回归用 `python3 -m unittest discover -s services/deploy/aliyun -p 'test_benchmark.py' -v` 执行。

首轮实测：页面共 230 次请求零错误，12 并发 p95 为 157.133 ms；两个安装包 Range 均正确。混合下载时页面 p99 为 1952.476 ms；六对各突发 8 MiB 时探测往返 p95 最高 3023.047 ms，存在排队风险。官网 cgroup 采样峰值 91.19 MiB，隔离中继 32.23 MiB，主机最低可用 387.48 MiB，无新 OOM；服务状态／重启计数及生产预算未变，临时监听已移除。**所有压力发生在同一台主机回环，下载吞吐不代表公网带宽；短测不能证明跨网流畅或长期稳定。** 完整记录见 [性能测试记录](../../../docs/changes/2026-11-03-07-aliyun-deployment-performance-validation.md)。
