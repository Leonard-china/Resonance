# 2026-11-03 — 实测阿里云官网部署性能、中继排队与本地原生播放

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：部署验证／测试工具／文档
- 适用版本：`version.properties` 的 0.6.5／39，版本号不变
- 影响平台：Ubuntu 官网／中继、本机 Linux JVM；本次不修改 Android／Windows／Linux 产品行为
- 关联需求：用户要求先实际测试部署，确认官网和软件是否可用、是否流畅；接续[迁移记录 06](2026-11-03-06-aliyun-hosting-and-server-routed-together.md)。

## 需求或问题

新机已恢复迁移副本，页面、下载 Range 和中继配对曾通过，但没有负载／资源数据，不能据此判断使用是否流畅。原生播放器仍在设备上运行，服务器承载独立官网与一起听字节中继；不能把 Kotlin UI 当成网站部署到云机，也不能用本机播放结果代替新机公网音频验收。

## 原因与依据

目标为 Ubuntu 24.04、2 vCPU、约 894 MiB 可见内存。公开切换尚未完成，因此使用目标机本地 HTTPS／WSS 与独立中继预算夹具，不占用正式中继账目。负载发生器也运行在目标机，测量包含客户端生成开销，不能推导公网吞吐、移动网络延迟或正式容量。

原有 Nginx 配置复用于测试，只替换为回环测试端口、临时证书及独立中继上游。官网访问现有迁移副本，仅发送 GET；帖子写入、作者登录、SMTP 和真实音频不作为压力素材。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [有界负载工具](../../services/deploy/aliyun/benchmark.py) | `main`、`measure_web`、`mixed_downloads`、`measure_relay`、`Sampler` | 排他锁／总时限／低内存保护；创建私有 Nginx 和独立 systemd 中继；测页面、Range、混合下载、WSS 字节往返，采样服务 cgroup 内存／CPU／OOM；清理后检查服务和预算未变 |
| [工具回归](../../services/deploy/aliyun/test_benchmark.py) | `BenchmarkTest` | 真实本地 HTTP 500 不计为成功；低可用内存时阻止网络负载；HTTPError 响应显式关闭，避免资源泄漏 |
| [源码白名单](../../services/deploy/aliyun/package.py) | `package` | 将可复测工具及测试纳入源码包，继续排除运行数据与凭据 |
| [部署说明](../../services/deploy/aliyun/README.md) | 有界部署性能测试 | 说明命令、统计口径、恢复检查、观测到的排队及未验收范围 |
| [机器台账](../audit/2026-11-03/aliyun-performance-verification.json) | 云端报告、本机测试摘要 | 保存脱敏原始指标、实际源码摘要和执行范围 |

## 修改前后行为

| 场景 | 修改前 | 本次结果 |
| --- | --- | --- |
| 官网负载 | 仅有四页可达验证 | 五个只读路径的 1／4／12 并发共 230 次零错误；得到响应分位数 |
| 安装包与页面混用 | Range 可用，未观察干扰 | 两个下载与 80 次页面请求混合，零错误但出现约 1.95 秒的尾延迟 |
| 中继大文件突发 | 只有配对功能回执 | 1／3／6 对各 8 MiB 字节回传均完整，六对探测 p95 最高约 3 秒，不能称为高并发流畅 |
| 性能复测 | 无独立工具 | 可在回环复测并保留 JSON、错误和恢复检查；不改变正式公开入口 |

## 实际测量

首轮命令助手 `resonance-isolated-performance-20261103`，执行 ID `t-bj071zs7bo3t1j4`；2026-11-03 19:32:46–19:33:02，退出码 0。执行脚本 SHA-256 `657fdf32de1cfbea62b1dbf25fd5649a63f15506d43f1772545927e851526d68`。首轮脚本没有后加的稳态 profile 和非零退出聚合；本次通过判断依据为 JSON 中阶段错误数和三项恢复检查，不依赖退出码。

| 页面负载 | 请求数／错误 | 中位数 | p95 | p99 |
| --- | --- | --- | --- | --- |
| 1 并发 | 30／0 | 10.865 ms | 14.594 ms | 14.692 ms |
| 4 并发 | 80／0 | 40.411 ms | 61.245 ms | 76.772 ms |
| 12 并发 | 120／0 | 116.512 ms | 157.133 ms | 181.070 ms |
| 4 并发＋两个限速下载 | 80／0 | 49.681 ms | 145.034 ms | 1952.476 ms |

两个安装包各取 16 MiB，均为正确 206 和 Content-Range、长度正确。首轮纯回环读取约 105.63／97.92 MiB/s；混合阶段约 4.66／4.63 MiB/s，后者由客户端刻意限速。**这些值均不是用户公网下载速度。**

| WSS 突发负载 | 错误 | 每对探测 p95 范围 | 每对探测 p99 最大值 |
| --- | --- | --- | --- |
| 1 对，各 8 MiB | 0 | 456.210 ms | 539.179 ms |
| 3 对，各 8 MiB | 0 | 1258.004–1513.592 ms | 1706.714 ms |
| 6 对，各 8 MiB | 0 | 2150.521–3023.047 ms | 3607.199 ms |

这里的“一对”指合成 host／guest WSS 连接，服务只转发字节。探测与大块数据共用连接，结果说明这种负载下存在排队；尚未分离客户端调度、网络缓冲和中继处理的贡献，不能断言唯一瓶颈，也不等于原生六人房间结果。

首轮 cgroup 每 100 ms 采样峰值：官网 91.19 MiB，正式空载中继 14.91 MiB，隔离负载中继 32.23 MiB；全机最低可用 387.48 MiB。测试区间平均 CPU 为官网 19.57%、隔离中继 25.46%（各占单核心百分比，不是系统整体峰值）；这两个数不含 Nginx 与发生器。三项服务没有新 OOM，正式业务状态／NRestarts 与预算前后相同，测试监听已清理。只代表约 16 秒首轮负载。

## 平台与数据影响

不修改产品协议、房间协调方式、客户端配置、数据库格式或版本，不读用户歌曲，不删除／写回元数据。不新增公开端口、不改 DNS，不重新授权 SSH；本轮通过已有阿里云命令助手执行。私有中继用临时预算，测试结束删除夹具，正式预算不归零。原官网继续现有入口。

本地原生测试使用生成音频、临时存储和静音输出，FFmpeg／Java Sound 实际解码输出；没有运行 Compose 可视窗口的帧率测试，也没有本轮 Android／Windows 真机播放。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 阿里云命令助手执行首轮有界脚本（见上方执行 ID） | 目标 Ubuntu，临时可信 TLS 夹具、回环 HTTPS／WSS | 通过；网页 230＋混合 80 次零错误，全部 Range／字节回传完整，恢复检查三项 true；性能存在上述尾延迟 |
| `python3 -W error::ResourceWarning -m unittest discover -s services/deploy/aliyun -p 'test_benchmark.py' -v` | 本机 Python 3.14 | 2 项通过，HTTP 错误资源关闭和低内存保护通过 |
| Python AST 语法检查、`python3 services/deploy/aliyun/package.py`、ZIP CRC／源码字节／路径与运行数据排除／SHA-256 检查 | 本机 | 通过；64 项源码文件，本轮包摘要 `a4edb23a6a01c420e37e66591c05b5c4e25a2f7ab91a35b69c19142754321f87`；工具已纳入新包，本轮未再次上传覆盖目标机 |
| 本记录链接与索引、定向 `git -c core.whitespace=cr-at-eol diff --check` | 本机 | 通过；保留现有其他未提交改动 |
| Gradle `:composeApp:desktopTest`，过滤 `DesktopAudioOutputIntegrationTest`／`TogetherWebSocketTest`／`ListenTogetherTest`／`TogetherServerRoutingTest` | 本机 JDK 17，显式启用实际音频输出与本地 Python TLS 夹具，单 worker／384 MiB Gradle 堆 | BUILD SUCCESS；18 项中 17 通过、1 项生产地址验收因未设置变量而跳过 |
| 上述实际音频输出用例 | 本机 FFmpeg＋Java Sound，生成音频、静音输出 | 2 项通过；播放推进、暂停／恢复、seek、倍速、快速替换和统计边界；不是云端实际歌曲测试 |
| 稳态中继补测 | 新增 `steady-relay` profile | 尚未执行：阿里云浏览器控制失去连接，命令未提交；等待恢复后继续，不能将工具存在计为测试通过 |
| 公网下载、手机／电脑独立网络通过新机播歌、真实多人房间、长时稳定性 | 新公网入口与设备 | 未执行；新机尚未公开切换，本轮不能确认真实用户的跨网流畅程度 |

原生命令的关键参数为 `RESONANCE_DESKTOP_AUDIO_TEST=1`、`RESONANCE_FFMPEG=/usr/bin/ffmpeg`、`RESONANCE_RELAY_TEST_PYTHON=<隔离venv>/bin/python`，以及 `--max-workers=1 -Dorg.gradle.jvmargs=-Xmx384m --no-configuration-cache --console=plain`；工具路径和 XML 报告保留在忽略的 `build/aliyun-benchmark-20261103/`，不纳入部署包。

## 限制与后续

可以确认官网与中继能在这台机器上运行，轻载页面的服务器端响应较快；并发大文件与页面混合访问出现秒级尾延迟，不能直接宣传“完全流畅”。稳态 profile 只是待执行工具，不能先推断日常播放结果。

后续需要恢复浏览器控制完成稳态探测；公开切换具备条件后，再用 Android／桌面不同网络的实际歌曲做端到端播放、控制、聊天和断线恢复。公网带宽、设备帧率、真实 SMTP 与长期容量均未由本轮覆盖；本次不顺带更改流量预算、传输协议或扩容购买。

## 同步更新的文档

- `docs/README.md`：新增本记录与最新性能状态。
- `docs/DEVELOPMENT.md`：第 7.1／16 节增加本次性能依据和边界。
- `docs/DEVELOPMENT.md`：将原“公网 0 毫秒秒开”改为缓存命中减少等待，避免将预加载实现当成未测量的性能保证。
- `services/deploy/aliyun/README.md`：增加复测命令和指标解释。
