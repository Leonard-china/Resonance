# 2026-11-01 — leonard.asia 公共一起听服务部署与回滚方案

状态（2026-11-02）：**已获用户批准并部署**，公网 WSS 与 Kotlin 内层 TLS 往返通过。真实独立网络双设备播放、容量与长时验收仍未完成；见 [上线记录](../../docs/changes/2026-11-02-04-public-relay-deployment.md)。

## 已知环境与目标

只读确认官网容器 2 GiB RAM、10 GiB 磁盘、一个分配端口 50034。官网 Gunicorn 与既有 Cloudflare Tunnel 保留；现场确认 Tunnel 实为本地管理。新增进程监听 **127.0.0.1:45902**，无需新分配公网端口。客户端默认 `wss://leonard.asia/resonance-relay/v1`；未配置用户启用，已保存自定义地址或明确关闭状态保留。

现有私有 `server.py` 是原始 TCP/TLS，不是 HTTP。新 `websocket_server.py` 仅传二进制字节；房间原有证书锁定 TLS、邀请验证和设备身份校验仍在内部。服务器看到 IP、时长、流量、路由摘要，不保存音频或聊天内容。房间协调设备仍可读取群消息。

## 部署步骤与实际差异

1. 对现有启动文件和 Tunnel 路由保存服务器私有备份，不把含 Tunnel Token 的内容放入仓库或日志。不改 `website/`、数据库、`.env`、现有虚拟环境、官网绑定端口和域名根路由。
2. 只上传本目录的 `websocket_server.py`、`requirements.txt`、`run-public-relay.sh`、`supervise.py` 到 `/home/container/resonance-relay/`；另建该目录自己的 `.venv`。Python 依赖只有固定版本 `websockets==17.1`；安装前在容器确认 Python 兼容性及可用空间。
3. 创建权限仅供服务用户的 `/home/container/resonance-relay-data/`，长期保存 `budget.json`。按照 `start-hook.sh` 在现有 start.sh 的 PATH 设置后插入独立守护项。supervise.py 继承原启动脚本的父 PID，启动脚本 exec 为 Gunicorn 后父 PID 不变；官网进程退出时只回收自身 relay 子进程组。连续 5 次失败后停止并报告，不重启官网。Linux flock 保证同一数据目录单写者，旧实例退出最多等待 15 秒。先读取/核对启动脚本哈希，再应用最小补丁，不能替换 server.jar 或覆盖他人修改。仅启动临时后台进程不算完成部署。关闭时先 TERM、等退出，不广泛杀死 Python/Java 进程。
4. 在现有 Tunnel 增加**优先于官网兜底**的发布规则：hostname `leonard.asia`，path `^/resonance-relay/v1$`，service `http://127.0.0.1:45902`。保留根路由 `http://127.0.0.1:50034`。无需修改 Tunnel Token。现场 dashboard 禁止编辑本地管理 Tunnel；实际新增此前不存在的 `/home/container/.cloudflared/config.yml`，内容见 `cloudflared-ingress.yml`。2026.10.7 优先读取有效 ingress，再回退启动器 `--url`，已经路由校验与公网验证，不改启动器/凭据。
5. 实际 WebSocket Upgrade 及双向通道已通过，无需登录或挑战。未新增边缘限速规则，当前连接速率由 relay 自身限制；握手前防护仍待单独评估，不购买付费套餐。

Cloudflare 官方支持路径正则路由并保留原始路径，也支持代理 WebSocket：[路由配置](https://developers.cloudflare.com/tunnel/features/locally-managed-tunnels/configuration-file/)、[WebSocket](https://developers.cloudflare.com/network/websockets/)。这是方案依据，不是对本服务器实际配置的验证。

## 容量与失败行为

| 限制 | 默认值 / 行为 |
| --- | --- |
| 全局 / 同 IP 已升级连接 | 64 / 16 |
| 等待总数 / 同路由 | 32 / 8 |
| 每 IP 新连接 | 每 60 秒 30 次；最多记录 1024 个活跃 IP |
| 单帧 / 接收队列 | 64 KiB / 4 帧；客户端队列 8 帧并有超时反压 |
| 配对等待 / 会话总长 / 单方向空闲 | 90 秒 / 2 小时 / 120 秒 |
| 单配对双向数据 / 全站每日 UTC 数据 | 512 MiB / 2 GiB |
| 预算持久化 | 发送前以 1 MiB 块预留，重启不返还未花完预留；损坏拒绝启动 |
| 客户端失败重试 | 递增间隔，连续 5 次后停止该邀请的自动重试；刷新邀请可重试 |

2 GiB 是**应用转发负载**限制，非运营商账单硬限额：TLS/WebSocket/Tunnel 开销、握手和被拒绝的请求也产生流量。它是保护现有官网的小规模初始上限，不代表所有用户都能同时使用。达到上限会断开并向客户端呈现失败，保留直连。不得未经批准提高上限。缺乏真实并发/出口带宽数据，目前不能承诺商用容量。

此进程只能监听回环，`--trust-cloudflare` 仅在请求确实经可信 Tunnel 到达时使用；不能直接公开同一监听器并信任客户端自行传入的 IP 头。WebSocket 握手前的攻击仍需边缘与容器资源限制。

## 上线验收（部分已完成）

先测试容器回环字节往返和原有官网页面/论坛只读访问，再测试真实 WSS 证书、双设备内层 TLS、不同网络创建/加入、多人音频/消息、断线/退出/重启与预算拒绝。确认普通用户不需要服务器地址或密钥；自定义和关闭状态仍保留。至少观察进程重启后存活、资源和错误计数。只记录脱敏结果，不记录邀请或聊天。

基础公网、Kotlin 内层 TLS、Linux 守护/预算及官网只读回归已完成；独立网络双端原生播放、长时/多人、压力及实际回滚仍未执行。当前没有升级版本或发布安装包。

## 回滚

先核对新路由与 start.sh 摘要，再将此次新增的 `.cloudflared/config.yml` 移入私有备份目录（此前不存在），恢复已校验的 start.sh 备份并使用既有 restart；旧 supervisor 跟随父进程退出回收自身 relay，原 Java 启动器恢复网站单一回源。核对官网 200 与 45902 无监听，不动官网数据。保留 relay 源码版本与 `budget.json`，不通过删除预算绕过上限。客户端临时可在「连接设置 → 自定义服务器 → 关闭中继」关闭，后续版本可回退默认 URL。仓库代码按本轮独立检查点逐文件回退，不能 reset 整个已有脏工作区。

## 2026-11-02 准备状态

现网资源及启动脚本已经重新只读核对，Python 虚拟环境为 3.12。准备包与精确补丁在 build/transition-relay-20261102；9 项本地测试通过。该准备阶段尚未部署；随后用户已登录并批准，最新安装/路由/9 项 Linux 测试/4 项 Kotlin 测试结果及备份见 [上线记录](../../docs/changes/2026-11-02-04-public-relay-deployment.md)。历史准备细节见 [守护准备记录](../../docs/changes/2026-11-02-03-public-relay-supervision-preparation.md)。
