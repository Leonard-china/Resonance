# 2026-11-03 — 准备阿里云独立托管并将一起听房间改为统一经服务器连接

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：部署迁移／功能／构建配置
- 适用版本：`version.properties` 的 0.6.5／39，不改变版本号
- 影响平台：Android／Windows／Linux 共享 JVM 一起听；独立 Python 官网及中继
- 关联需求：用户新购阿里云 Ubuntu 服务器，要求官网与一起听迁入，不再使用翼龙和设备直连。
- 状态：客户端代码与独立部署包已准备并验证；新机安装、数据副本恢复、回环与重启验证已完成；公开切换待用户补足购买期并备案，再完成最终同步与受信 HTTPS／DNS。**不能将本记录视为公开迁移上线完成。**

## 需求或问题

原官网、邮件队列和公共 WSS 中继运行于翼龙容器，依赖 Java 启动器与 Cloudflare Tunnel。一起听客户端先拨对方设备地址，失败后才尝试公共中继，异地连接受设备网络条件影响。

用户指定新购北京区 Ubuntu 24.04 轻量服务器作为目标。控制台及命令助手实际确认 2 vCPU、约 894 MiB 可见内存、30 GiB 系统盘、约 26 GiB 可用空间；当前账号备案页面显示尚无备案信息，实例当前购买期为一个月。阿里云要求中国内地公开服务备案，用于备案的轻量服务器累计购买至少三个月，公开切换仍有外部前置条件。

## 原因与依据

`JvmListenTogetherService.connectGuest` 原先将直连异常当作中继回退触发条件；房主监听所有网卡并把设备候选地址带入邀请。现有中继只实现 Cloudflare 可信头，直接改用 Nginx 会把全部连接当成同一回环 IP，影响每 IP 配额。

现有通用官网 service 可复用；中继缺少直接 systemd 入口，同站 Nginx 配置缺少 WebSocket 精确路径。完整迁移必须保留 SQLite WAL 数据、作者身份、退订密钥、SMTP、上传图片、当前安装包和流量账目，不能初始化一个空站后声称迁移完成。

备案依据：[阿里云使用须知](https://help.aliyun.com/zh/simple-application-server/product-overview/usage-notes)、[域名核验](https://help.aliyun.com/zh/icp-filing/basic-icp-service/user-guide/prepare-and-check-the-domain-name)。`.asia` 当前在阿里云支持后缀列表内；本次没有据历史印象断言该后缀不可备案，也没有替用户续费、购买或提交实名。

## 具体修改

| 文件 | 关键符号／配置 | 实际修改与目的 |
| --- | --- | --- |
| [JVM 一起听](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `host`、`invitation`、`connectGuest` | 配有服务器的房间仅绑定回环；邀请不公布设备 IP／候选；访客直接拨服务器，服务失败不退回设备直连；保留有界重试、取消、内层 TLS 与授权 |
| [一起听说明](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt)、[连接设置](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 网络与邀请提示 | 说明配置服务器后统一经服务器连接；确认加入时显示实际服务器，不把占位回环地址当公网入口 |
| [中继](../../services/relay/websocket_server.py) | `PublicRelay`、`--trust-nginx` | 新增单层 Nginx 的 `X-Real-IP` 信任模式，与旧 Tunnel 模式互斥；默认不信任访客 IP 头 |
| [Nginx 配置](../../website/deploy/nginx-aliyun.conf) | HTTPS／精确 WS location | 官网 8860，中继 45902；显式 Upgrade、覆盖真实 IP 头、禁用敏感访问日志；保留公开受信证书校验 |
| [独立中继服务](../../services/relay/deploy/resonance-relay.service) | systemd／flock | 独立账户、私有预算目录、有界失败重启、128 MiB 内存限制；同一 `supervisor.lock` 防多个预算写者 |
| [部署入口](../../services/deploy/aliyun/README.md)、[准备脚本](../../services/deploy/aliyun/prepare.sh) | 固定安装路径与数据恢复 | 两套 venv、三个独立 service；1 GiB 主机的网站／邮件分别限制 320／96 MiB；不覆盖已有 `.env`，准备阶段不自动启动业务、改 DNS 或停止旧机 |
| [源码打包](../../services/deploy/aliyun/package.py) | `package` | 复用官网明确白名单并加入必要中继／部署文件；拒绝外部路径及链接；不含运行数据、凭据或安装包；为本次 ZIP 实际生成摘要 |
| [连接回归](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherServerRoutingTest.kt) | 两项测试 | 服务器失败但设备直连地址可达时也不拨设备；新房间不发布设备网络候选 |
| [中继回归](../../services/relay/test_websocket_server.py) | IP 预算测试 | 验证 Nginx 不同客户端独立计数、默认拒绝信任自报 IP 头，保留配对／取消／预算回归 |

## 修改前后行为

| 场景 | 修改前 | 当前源码／配置 |
| --- | --- | --- |
| 带服务器地址加入房间 | 先拨设备，失败后中继 | 只拨服务器，失败明确提示服务器不可用 |
| 配置服务器的新房间 | 设备监听所有网卡，邀请公布设备地址 | 本地回环监听，邀请不公布设备候选 |
| 无服务器旧邀请／明确关闭中继 | 支持设备直连 | 保留兼容，不覆盖已有偏好 |
| 新主机托管 | 缺少同站 WS／systemd 中继入口 | 官网、邮件、中继独立服务，Nginx 直接 HTTPS |
| 原官网数据 | 原主机持续服务 | 使用 SQLite 在线快照与私有转移；公开切换尚未完成，旧服务未停止 |

## 平台与数据影响

Android、Windows 和 Linux 共用 JVM 网络实现，平台接口没有变化。默认 URL 继续为 `wss://leonard.asia/resonance-relay/v1`，不硬编码服务器 IP，不降低证书校验。既有自定义或关闭状态保留，无服务器的旧邀请仍能直连。

房间创建者仍是协调节点；常听伙伴原有寻址／直接邀请链路未改为服务端消息队列。内层 TLS、聊天身份校验、音频传输与临时缓存规则保留，不引入云曲库、账号或服务端房间数据库。若用户要求这些业务也全部迁到服务端，需要另行实现协议，不能将字节中继部署包装成该功能已完成。

不改变曲库、同步包、音频转换、标签或文件删除语义。官网运行数据和中继预算不进入源码包；准备脚本不重置作者身份、不启动真实邮件群发、不升版或发布客户端。

## 验证

以下为本次实际执行；本机工具与密钥路径仅存在忽略的 `build/`，不提交。

| 命令／操作 | 环境 | 结果 |
| --- | --- | --- |
| `python -m unittest discover -s website/tests -v`（在 website 工作目录用 `-s tests`） | 临时 venv，Python 3.12 | 42 项通过，临时库和模拟邮件 |
| `python -m unittest discover -s services/relay -p 'test_*.py' -v` | 本机 Python 3.12 | 11 项通过 |
| `:composeApp:desktopTest`，定向 `TogetherServerRoutingTest`／`TogetherWebSocketTest`／`TogetherConnectionBoundsTest`／`ListenTogetherTest` | 本机 JDK17，显式设置本地 Python TLS 夹具 | 最终 18 项中 17 通过，1 项公网验收按未设置生产验收变量跳过；真实本地 WebSocket 内层 TLS 往返通过 |
| `:composeApp:compileReleaseKotlinAndroid` | 本机 JDK17／项目既有 SDK | 通过；没有构建或发布新安装包 |
| `nginx -t` | 隔离解包 Ubuntu Nginx 1.28.3；测试证书和非特权端口，仅用于配置校验 | 通过；最初因隔离夹具未配置 FastCGI 临时路径失败，修正夹具后通过，生产配置未因此放宽 |
| `sh -n services/deploy/aliyun/prepare.sh`、Python compileall、源码 ZIP 白名单检查 | 本机 | 语法通过；源码包和本次摘要已生成 |
| 阿里云命令助手只读环境检查 | 目标 Ubuntu 24.04 | 退出码 0；不是仅凭截图推测主机资源 |
| 源码包上传／目标机 SHA-256 校验、安装依赖和 `prepare.sh` | 目标机 | 已执行通过；Nginx 暂时 mask，未公开空站 |
| `systemd-analyze verify` 三项新 service；隔离中继测试 | 目标机 | 验证通过；中继 8 项通过；系统自带 cloudmonitor 的历史警告未顺带修改 |
| `python -m unittest discover -s /opt/resonance/website/tests -v` | 目标机，临时数据库／模拟邮件 | 42 项通过 |
| 私有快照加密传输、原文摘要、SQLite、两端安装包摘要核对及副本启动 | 新旧主机 | 最终通过；不是公开零停机切换 |
| 四页 200／两端 Range 206／中继配对，重启后再验 | 目标机，仅回环 | 均通过；邮件 worker inactive，Nginx masked，无新生产预算 |
| 部署关键源码摘要与本地比对；临时授权撤销及再次登录拒绝验证 | 目标机与本机 | 均通过，原有密钥保留，临时交换与加密中间材料已清理 |
| 旧官网首页／下载／论坛只读复核 | 现有公网 HTTPS，常规浏览器 UA | 均为 200；首次默认 urllib UA 为 HTTPError，未把失败探测计为通过 |
| 实际证书／DNS 切换、独立网络两端或三端播放、容量／长时测试、真实 SMTP 投递 | 目标环境 | 未执行；备案和公开切换未就绪，不能使用旧报告代替 |

原站首先生成 SQLite 在线快照及私有 TAR，旧文件下载接口仅 HTTP，因此先在旧机用新机持有私钥的临时公开证书加密，下载密文后校验摘要。第一种 CMS 大文件解密在新机触发 OOM；尝试 2 GiB 临时交换空间仍失败，随后撤销并移除该交换文件，改用 RSA-OAEP 包装随机口令、OpenSSL 流式 AES-256-CBC／PBKDF2 加密，并在解密后再核对完整原文 SHA-256。凭据不以明文经过旧 HTTP 下载口，密钥不写入源码或日志。原站未停止，当前为迁移副本，正式切换前仍需最终同步。

初次 Kotlin 回归新增测试因表达式推断出非 Unit 返回值被 JUnit 拒绝；显式声明 `runBlocking<Unit>` 后复测通过。保留初次失败日志，不将失败过程隐去。

## 远端操作与限制

已获用户明确允许，通过命令助手追加两小时过期、禁止端口／代理转发的迁移专用 SSH 公钥；原密钥和密码保留。服务器主机公钥与命令助手回执核对一致。私钥仅用于本次忽略目录中的临时操作；收尾已原子撤销新机上的这把公钥，保留所有原有授权，并实际验证该密钥再次登录被拒绝，随后删除本机私钥。

本次已恢复 0.6.5 迁移副本：SQLite 完整性及业务记录数与快照一致，作者配置、会话／退订密钥、上传图片及两端安装包完整复制；实际安装包摘要与数据库一致。官网与中继已 `enable --now`，重启后回环四页 200、两端 Range 206、WebSocket 配对均通过，进程空载观测约 35／16 MiB。新邮件 worker 未启动，Nginx 保持 mask；没有产生新中继转发负载或初始化预算，正式切换必须迁入旧预算。临时交换文件和加密中间材料已清理，私有原始快照保留。公开服务目前尚未迁完，原官网／邮件／中继保持原入口。用户已选择继续使用北京机、补足购买期并办理备案；本次不代替用户完成付费、协议接受或实名核验。

## 同步更新的文档

- [本次脱敏机器台账](../audit/2026-11-03/aliyun-staging-verification.json)：源代码摘要、实际测试计数、私有服务状态与公开切换前置条件。
- `docs/README.md` 变更索引与最新迁移状态。
- `docs/DEVELOPMENT.md` 第 7.1、16 节及源码核对日期。
- `docs/THIRD_PARTY.md` 新部署与网络边界。
- `website/README.md`、`website/OPERATIONS.md`、`services/relay/README.md` 增加迁移入口，保留旧部署作为历史和回滚依据。
