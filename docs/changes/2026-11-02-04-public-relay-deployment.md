# 2026-11-02 — 部署自有一起听中继并验证公网加密通道

- 修改日期：2026-11-02（Asia/Shanghai）；类型：生产部署、配置、验收测试。
- 客户端版本：0.6.5（39），未升版、未发布安装包；服务端 Linux，两端共用 JVM 传输。
- 关联：[上线准备](2026-11-02-03-public-relay-supervision-preparation.md)、[公共 WSS](2026-11-01-15-public-websocket-relay.md)。
- 用户已明确批准独立回环 45902 服务、每日 2 GiB、备份后启动项及专用 Tunnel 路由，并允许短暂重启。

## 需求、原因及现场差异

客户端已经默认自有 WSS，但服务器只有官网单一入口，服务尚未上线。本次安装和启用服务，保持官网、论坛、用户数据及端口不变。

实际 Cloudflare 页面显示 leonard-web 为 **locally managed**，路由按钮禁用；早期“托管 Tunnel”推断不准确。服务器的既有 Java 启动器通过 `--url http://127.0.0.1:50034` 指定单一回源。只核对启动器源码和默认配置文件是否存在，没有读取 Token。所有默认配置文件均不存在。

因此同一条已批准的路径规则落在本地 `cloudflared` 配置中，不迁移 Tunnel、不改 DNS、不替换 `server.jar`。运行版本 2026.10.7 的 [ParseIngressFromConfigAndCLI](https://github.com/cloudflare/cloudflared/blob/2026.10.7/ingress/ingress.go) 优先使用有效 ingress，再回退 CLI；[官方配置说明](https://developers.cloudflare.com/tunnel/features/locally-managed-tunnels/configuration-file/) 说明路径匹配与兜底规则。现场执行路由校验、重启和公网请求确认该行为。

## 具体修改

| 文件/位置 | 配置或符号 | 实际内容 |
| --- | --- | --- |
| `/home/container/resonance-relay/` | 独立服务及 `.venv` | 安装准备阶段的 5 个白名单文件；依赖仅 websockets 17.1；部署后的源码摘要与仓库逐一一致 |
| `/home/container/start.sh` | `start-hook.sh` | 在既有网站命令前增加独立 supervisor；保留 worker、Gunicorn 和端口 |
| `/home/container/resonance-relay-data/` | `budget.json` / `supervisor.lock` | 0700 私有目录；UTC 日 2 GiB 应用负载、重启保留预算、Linux 单实例锁 |
| [cloudflared-ingress.yml](../../services/relay/cloudflared-ingress.yml) | 新增配置模板 | 安装到 `/home/container/.cloudflared/config.yml`；精确路径优先转发 45902，其他请求保留原 50034 兜底；不含凭据 |
| [TogetherWebSocketTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherWebSocketTest.kt) | `deployedPinnedTlsRoundTripOverWebSocket` | 显式环境变量开启公网验收，复用真实 Kotlin 传输与内层证书锁定 TLS；普通测试默认跳过公网，临时身份和样本不涉及用户数据 |
| [验收台账](../audit/2026-11-02/relay-deployment-verification.json) | 摘要与结果 | 安装回执、路由校验、现场源码摘要、回环/公网测试及网站状态 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| WSS 专用路径 | 没有服务回源 | 公网连接、配对、双向字节及 Kotlin 内层 TLS 往返通过 |
| 官网/下载/论坛 | 原有单一回源 | 继续 50034，三页 HTTP 200，论坛 DYNAMIC |
| 网站/Tunnel 重启 | 没有中继守护 | relay 跟随启动、保持单实例；旧预算不清零 |
| 额外路径 `/resonance-relay/v1/extra` | 官网处理 | 仍由官网处理并返回 404，不落入 relay |

新用户默认服务器配置已有，旧自定义或关闭状态继续优先。没有修改网站源码、数据库、邮件配置、音频、收藏或 API 配置，没有发布新客户端；不意味着所有已安装的旧版客户端自动获得 WSS 支持。

## 验证

| 命令/操作 | 环境 | 结果 |
| --- | --- | --- |
| 安装包/原启动文件 SHA-256 校验、备份、`bash -n` | 生产容器 | 通过；原文件变更则拒绝覆盖 |
| `cloudflared tunnel --config <候选文件> ingress validate` 及 `ingress rule <URL>` | 2026.10.7 | 通过；精确路径、根、下载、论坛、额外子路径共 5 项 |
| 既有控制台 `restart` | 生产容器 | 已执行两次：启用守护、随后加载本地路由；最终官网正常 |
| loopback WS 8 KiB 双向、锁、预算、监听、源码摘要 | 生产 Linux | 通过；仅 1 个 `127.0.0.1:45902` LISTEN；预算预留由重启前 1 MiB 增至 2 MiB，没有重置；目录 0700 |
| `unittest` 加载两份 relay 测试，临时目录/随机回环端口 | 生产 Linux 的独立 relay venv | **9 通过，0 失败/跳过**；含真实子进程终止、有限重试和预算保护，不消耗真实站点预算 |
| 公网 WSS 两连接传输随机样本、关闭 | 本机至 leonard.asia | 通过，双向共 8 KiB；正常 TLS 证书校验 |
| `:composeApp:desktopTest --tests '*TogetherWebSocketTest' --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain` | Windows JVM；设置两个验收环境变量 | **4 通过，0 失败/跳过**；包括公网证书锁定内层 TLS、回环 TLS、地址拒绝及原配置兼容；构建 47 秒 |
| 只读 GET 首页/下载/论坛 | 公网，UA `ResonanceDeploymentCheck/1.0` | 200 / 200 / 200；额外路径 404 |

公网测试只在设置 `RESONANCE_RELAY_ACCEPTANCE_URL=wss://leonard.asia/resonance-relay/v1` 时运行；本地 Python TLS 测试另设 `RESONANCE_RELAY_TEST_PYTHON`。后续重新验收应对 `:composeApp:desktopTest` 使用任务选项 `--rerun`，避免 Gradle up-to-date 被误记为新验证。不要在普通 CI 自动联系生产。

首次回环网站 GET 缺正确 Host，得到 HTTPError；修正探测 Host 后 200。首次公网 Python 默认 UA 返回 403；使用明确验收 UA 后三页 200。未关闭安全规则，未把初次失败抹除；原始结果保存在 `build/relay-deployment-20261102/`，摘要在验收台账中。最终容器约 878 MiB RAM / 2455 MiB 磁盘，仅是当时快照，不是容量测试。

## 安全检查点与回滚

本地沿用 `codex/deploy-20261102-081440` 和 `.git/audit-checkpoints/deploy-20261102-081440`；未清理原有脏工作区。服务器启动文件私有备份为 `/home/container/resonance-relay-backups/20261102/start.sh`。

原启动 SHA-256：`4ea0e3ad8b18eb1f41b41093c2e4eeb32c74c4ce70b1012c22f775246b3e63b0`；部署后：`559889190bbe59bfa4b4260905231a4c7e92f5d0c0c95bbb1fd02065f086554f`；新路由文件：`9bb2c3b8b22e187c15b80f570690f7ed1247f004bd02b066f8e21f32f00044fe`。

回滚先核对现有文件摘要，遇到他人修改停止覆盖：将这次新增的 `.cloudflared/config.yml` 移入上述私有备份目录（此前不存在，不删除其他配置）；恢复已校验的 `start.sh` 备份，再使用既有 `restart`。原 Java 启动器恢复单一网站回源；旧 supervisor 因父进程退出回收自己的 relay。检查官网 200、45902 不再监听。保留 relay 代码、虚拟环境和预算文件，不删除真实数据，不清零预算。回滚步骤已记录，**未实际执行回滚演练**。

## 限制与后续

- 公网基础通道与共享 Kotlin 传输已经验收；真实 Android ↔ Windows 在独立移动网络上的建房、播放、多人和长时间重连仍未验收，不能用本机两连接替代。
- 未压测、未耗尽生产 2 GiB 预算；限额拒绝由隔离单元测试覆盖。应用负载限额不包含握手、Tunnel/TLS 开销，不是计费硬限额。
- 未新增 Cloudflare 边缘限速规则；目前使用 relay 自身连接、速率、帧与流量限制，握手前的边缘防护仍需单独评估。
- 没有重新运行无关的全量 Kotlin/Lint/Android 构建；生产应用源码及候选包没有变化。本次只新增配置和验收测试，不复用历史构建冒充本次结果。
- 未获取真实美元费用；账户额度百分比不能换算任务费用，无新付费服务、子代理或模型调用。

同步更新文档索引、开发说明一起听/部署边界、优化报告、relay README/部署方案与官网运维说明。
