# 2026-11-05 — 阿里云 Ubuntu 独立官网部署

当前范围仅为官网与邮件两个 systemd 服务。一起听已经移除，源码包、准备脚本和性能工具不再安装或启动中继／寻址服务。本轮未操作远端；2026-11-03 的迁移与中继验收保留在[历史记录](../../../docs/changes/2026-11-03-06-aliyun-hosting-and-server-routed-together.md)，不能作为当前线上状态。

## 源码包与首次安装

```bash
python3 services/deploy/aliyun/package.py
```

输出 `build/aliyun/Resonance-aliyun.zip` 与从本次包生成的 `.zip.sha256`。官网沿用 `website/package.py` 白名单，另带本目录部署工具；不包含真实 `.env`、数据库、上传图片、作者身份、签名材料、安装包或中继源码／服务。已有生产安装先备份，不直接覆盖解压。

在已准备好 Python3、venv、Nginx 的 Ubuntu 主机上，将源码解压至 `/opt/resonance` 后执行：

```bash
sh /opt/resonance/services/deploy/aliyun/prepare.sh
```

脚本只创建 `resonance` 账户、`/var/lib/resonance-site` 私有数据目录、官网 venv、官网／邮件 service 与内存覆盖配置；已有 `.env` 不覆盖。官网／邮件内存上限为 320／96 MiB。不启动服务，不修改 DNS／Nginx／旧主机，不重置作者身份。不能直接用它清除已安装的旧中继或私有数据。

## 网站迁移与公开入口

维护现有部署先读[网站运维](../../../website/OPERATIONS.md)。已有合法运维入口才执行远端操作，不将私钥、API Key 或作者地址写入仓库。公开网站前由维护者确认备案、域名、受信证书及购买条件。

1. 备份源码、原 DNS／代理路由；切换窗口停止旧站写入和邮件 worker，使用 `manage.py backup` 的 SQLite 在线备份，不单独复制活动 WAL 数据库。
2. 私密转移数据库、`secret.key`、`owner.json`、`uploads/`、`releases/`、真实 `.env` 和邮件队列。核对摘要与 SQLite 完整性，保留作者身份、退订密钥、帖子与安装包；不公开任何备份。
3. 数据放 `/var/lib/resonance-site`（账户 `resonance`、0700），`.env` 为 root:resonance／0640；只按需要调整 `SITE_URL`、`DATA_DIR`、`PRODUCTION`、`TRUST_PROXY`，保留其他真实配置。
4. Nginx 仅反代官网回环端口 8860；从网站模板配置主机名、TLS 与续期后执行 `nginx -t`。已有未提交的 Nginx 配置需维护者单独核对，本次不覆盖。启动 `resonance-site`；确认旧邮件 worker 停止及队列迁入后再启动 `resonance-mail`，不自动生成群发任务。
5. 核对首页／下载／论坛、静态缓存、实际三端安装包的 Range 与摘要；临时数据库执行网站回归，不向真实订阅者发送测试邮件。切换失败时保留新写入，再恢复原路由，不能覆盖丢弃新帖子。

本轮只移除源码依赖，没有关停旧服务、释放云资源、修改账单或替换已发布客户端。已有旧服务的线上退役与网站发布须单独实际执行并留记录。

## 有界官网性能工具

`benchmark.py` 使用官网 venv，以 root 在已准备的 Ubuntu 主机运行，启动私有回环 Nginx／TLS 夹具。仅测五个只读页面（1／4／12 并发）、Android／Windows 各 16 MiB Range 及混合下载；保留排他锁、100 秒时限、内存保护与夹具清理。没有中继负载或 websockets 依赖，不更改生产服务／DNS／凭据。

```bash
/opt/resonance/website/.venv/bin/python /opt/resonance/services/deploy/aliyun/benchmark.py
python3 -m unittest discover -s services/deploy/aliyun -p 'test_benchmark.py' -v
```

性能结果写入目标机私有 `resonance-benchmark-*.json`。检查错误、`failure`、服务未改变及监听已移除字段，异常返回非零；本轮未在 Ubuntu 上执行该性能工具。历史负载数值仅见[原记录](../../../docs/changes/2026-11-03-07-aliyun-deployment-performance-validation.md)。
