# 2026-11-02 — 双主题、播放稳定性与公共一起听优化报告

这是用户批准方案后的分批实现结果，不代表审计中所有待办、真实跨网和商用容量已经验收。原生 Kotlin / Compose 架构保留；版本仍为 0.6.5（39）；未发布安装包；公共中继已在用户明确批准后部署，见文末最新验收。

## 已修复与升级

| 问题 | 本次行为与证据 | 独立记录 |
| --- | --- | --- |
| 损坏听歌记录可能被空数据覆盖，写失败无反馈 | 严格读取、有效备份恢复、损坏原文另存；无有效副本拒绝覆盖；失败上报并保留待写增量 | [统计完整性与批量写入](changes/2026-11-01-12-listening-stats-integrity-and-batching.md) |
| 每秒全量 JSON 写盘进入 UI 调用链 | 两端 IO 写盘，正常 10 秒批量；暂停、切歌、组合退出 flush | 同上 |
| 拖动进度反复重启桌面解码 | 本地实时预览，释放只提交一次；切歌撤销旧手势；真实原生指针测试 | [进度提交与播放错误](changes/2026-11-01-13-seek-commit-and-visible-playback-errors.md) |
| 全屏播放器遮挡失败信息 | 独立错误弹窗，本机提供重试；房间播放不绕过同步控制 | 同上 |
| 深浅主题质感与层级不一致，一起听设置过多 | 石墨银灰 / 雾白深灰，保留系统模式与 IceBlue；创建/加入优先，设置和高级选项折叠 | [双主题与一起听 UI](changes/2026-11-01-14-dual-theme-and-simpler-together-ui.md) |
| 单分配端口官网不能代理原始 TCP/TLS 中继 | 新增独立 WSS 字节通道，复用 OkHttp，保留内层房间 TLS；默认自有 URL、旧配置及关闭状态兼容；连续失败最多尝试 5 次 | [公共 WSS 中继](changes/2026-11-01-15-public-websocket-relay.md) |

## 第一批验证台账（历史记录）

以下只描述第一批冻结源码，不能代替第二批修改后的验收。机器可读台账见 [quality-verification.json](audit/2026-11-01/quality-verification.json)。开始验证后生产与测试源码 SHA-256 未改变。

| 检查 | 本次最终结果 |
| --- | --- |
| Kotlin 桌面与 Android 编译、Android Debug 构建 | 同一条 Gradle 命令成功，7m 47s；Debug 任务确认当前输入为 up-to-date，未发布安装包 |
| 桌面单元/集成/原生 UI | 280 项：276 通过、4 明确跳过、0 失败/错误 |
| 新 WSS 联测 | 3 项全过：地址验证、配置兼容、真实 WebSocket 内层双向证书 TLS 往返 |
| 原有寻址服务互操作 | 本次设置本地 Python 后执行并通过；没有公网请求 |
| Android Lint | 0 errors / 110 warnings（与审计计数相同）；项目未配置独立 KtLint/Detekt 任务 |
| Python 公共中继 | 6 项通过 |
| 原生视觉 | 70 张页面 + 2 张错误弹窗；精选 12 张已保存并实际查看重点页 |

完整命令：`./gradlew.bat :composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain`。测试使用隔离的 `RESONANCE_DATA` / `RESONANCE_LIBRARY`，两个 Python 联测变量指向本机解释器。4 个跳过项是付费 AI 宣传探针和 3 个需要真实设备/模拟器桥接的用例，未把它们算成通过。

- 第一轮定向：`desktopTest --tests '*ListeningStatsStoreTest' --tests '*UiVisualContractTest'`，19 项通过。生成并实际查看原生明暗主题截图。
- 中间双端编译发现 Socket 参数类型未统一、Android 不提供 `Files.readString`；均已修正，没有把失败隐藏或计入通过。
- 中间完整检查：`desktopTest lintRelease assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2` 成功；280 项桌面测试，275 通过、5 条件跳过；Lint 0 errors / 110 warnings。其后增加双向 TLS 身份认证及损坏原文留存断言，已由上表最终复测通过。
- Python 公共中继：`python -m unittest discover -s services/relay -p 'test_*.py' -q`，6 项通过；覆盖真实 WebSocket 双向传输、退出清理、非法请求/等待到期、容量/流量拒绝、预算重启与损坏保护。
- `bash -n services/relay/run-public-relay.sh` 通过。仅语法检查，不是 Ubuntu 守护进程验收。
- Android 实机播放、Conscrypt 上的嵌套 TLS、不同网络、长时间会话、实际服务器部署、商业并发和 Windows 安装包发布：**未执行**。设备/生产环境验收与本地单元测试有明确区别。
- 本轮未修改官网业务，无需重跑官网全套；审计阶段官网测试的历史通过不重复计成本轮部署结果。
- 原始日志：`build/quality-20261101/targeted.log`、`full.log`（失败记录）、`full-2.log`、`final.log`；测试 XML 在 `composeApp/build/test-results/desktopTest/`。这些构建目录可再生成，不作为发布资源。

## 第二批稳定性修复与验证

- [变更 17](changes/2026-11-01-17-native-listening-session-accounting.md)：移除 UI 计时，原生会话拥有统计，暂停恢复不重复完播，按目录共享写入器并保留失败增量。
- [变更 18](changes/2026-11-01-18-ordered-playback-preferences.md)：捕获完整随机/循环偏好，顺序保存，移除实时 setter 隐式写盘；设置保存失败在全屏也可见。
- [变更 19](changes/2026-11-01-19-scoped-playback-failures.md)：原生失败携带曲目/请求身份；试听与保存失败不暂停主播放；桌面实际音频就绪才发播放状态。完整加载/缓冲阶段仍未实现。

本次 36 项定向回归 **36 通过、0 失败、0 跳过**，其中新增 10 项测试，修改 1 个既有偏好测试的契约断言及清理。Windows、Android 编译通过；未把旧全量/Lint/安装包结果算成本批结果。最终 Android 时间截止点的单独修正另外编译验证。机器证据与准确命令见 [stability-verification.json](audit/2026-11-01/stability-verification.json)，原始日志在 build/stability-20261101。首轮存在旧 setter 隐式持久化断言失败，原因及最终验证见变更 18。

新增/修改 Kotlin 文件共 18 个，完整清单、符号与验证范围分别见上述记录及台账。安全检查点：`.git/audit-checkpoints/stability-20261101-215043/files`；分支 `codex/stability-20261101-215043` 只标记 HEAD，未提交改动依赖文件检查点恢复。工作区仍保留此前所有未提交成果；未自动提交或升版。

本次另生成并实际查看浅/深色错误弹窗原生截图，保存在 [stability/](audit/2026-11-01/stability/)。没有重做未修改的整套页面或虚构新性能指标。Android Activity 销毁后继续播放、真实快速切歌、音频焦点、真实输出就绪仍需设备验收。

## 第一批性能和视觉证据

统计使用完全独立的 10,000 条合成历史，不读取真实用户库。审计时每次同步写盘中位 20 ms / P95 71 ms；新缓冲更新中位 85 μs / P95 143 μs，显式安全刷盘 83 ms，落盘总数 10,031 与增量一致。后者 30 次连续更新没有真实 1 秒间隔，机器同时构建；此结果说明热路径移除了全量序列化，不用于宣称整体响应提高固定倍数。安全备份增加单次刷盘成本，因此迁到 IO 并减少频率；首次读取/统计快照同步锁成本仍待进一步测量。

原生 UI 使用 Compose `ImageComposeScene` / Skia 与既有 PreviewLibrary、原创歌词测试夹具。验证 7 页 × 2 主题 × 5 窗口/字号，共 70 张页面截图，另有两张播放错误截图；这些是测试夹具的真实渲染，不是用户数据或 Photoshop 效果图。重点人工检查播放器与一起听的浅/深主题、360 宽 1.5 倍字体，以及 1280 宽布局。

审计前截图见 [审计目录](audit/2026-11-01/)，本轮截图精选交付于同目录 [after/](audit/2026-11-01/after/)。[播放器修改前](audit/2026-11-01/player-dark-1280.png) → [修改后](audit/2026-11-01/after/player-dark-1280.png)；[浅色播放器](audit/2026-11-01/after/player-light-1280.png)、[浅色一起听大字](audit/2026-11-01/after/together-light-360-large-text.png)、[深色一起听大字](audit/2026-11-01/after/together-dark-360-large-text.png)。前后对比以实际保留的播放器深色基线为限；没有一起听修改前截图，不虚构该页面的前后对比。原生指针及语义测试补充了截图无法证明的操作行为。

## 变更文件与数据边界

本轮修改文件完整相对路径清单见 `docs/audit/2026-11-01/quality-changed-files.txt`，按进入实现前检查点比较，不把其他 AI/用户已有改动算作本轮成果。主要模块为统计存储/平台接口、App 反馈、播放器进度、主题、Together UI/传输/配置、独立 relay 服务及对应测试；每个独立记录列出关键符号。

没有移除歌曲、收藏、歌单、历史或 API 配置；未读取/打印签名私钥或真实 API Key；新增服务不需要客户端共享访问密钥。合法统计格式兼容，恢复备份可能回退一个刷盘周期；异常终止可能损失尚未落盘的约 10 秒。旧的自定义中继与显式关闭设置优先于新默认值。

安全检查点：分支 `codex/quality-20261101-203513` 标记原 HEAD，完整未提交文件快照/清单位于 `.git/audit-checkpoints/20261101-203513`。工作分支保持原状态，没有提交、stash、reset 或清理已有改动。回滚先审阅本轮文件清单与快照，再逐文件处理，不可用分支指针覆盖整个脏工作区。

## 公共服务上线边界

客户端默认值已编写为 `wss://leonard.asia/resonance-relay/v1`，**2026-11-02 已部署，公网 WSS 及真实 Kotlin 内层 TLS 往返通过；独立网络双端原生播放仍未验收**。新邀请需要双方使用支持 WSS 的版本。服务器传递内层 TLS 密文，但看到网络元数据；协调设备仍可读取房间消息。

准备包位于 `build/quality-20261101/resonance-public-relay-review.zip`，只包含新服务源码、固定依赖、启动脚本与方案，不含凭据或运行数据。可审阅的方案：[部署、限制与回滚](../services/relay/DEPLOYMENT-leonard.md)。只新增回环 45902 服务及 Tunnel 专用路径，保留官网 50034、根路由和数据；现场确认 Tunnel 为本地管理，用户批准后已通过新增本地 ingress 配置启用专用路径，保留网站兜底；见最新上线记录。

初始全站上限 64 个升级连接、2 GiB/UTC 日应用负载，不代表无限用户或计费硬上限；握手、协议与 Tunnel 开销另计。没有购买服务或提升套餐。限制达到时拒绝/断开并提示，不提供虚假成功。Linux 守护及重启保留预算已现场验证；尚未新增 Cloudflare 边缘限速规则。

## 剩余风险与后续顺序

1. **P1**：公共服务已上线，继续完成 Windows ↔ Android / 不同网络、多成员、退出重连、预算耗尽和重启验收后再发布客户端默认配置。
2. **设备验收**：第二批已实现原生统计生命周期及完播去重，仍需 Activity 重建/后台、循环、seek/变速实测。
3. **P1 部分完成**：偏好顺序、请求归属及保存失败可见性已修复并定向验证；完整 Preparing/Buffering 状态仍待设计，Android 跨进程事件和两端真实输出需实测。
4. **P2**：大库渲染、首读锁、图片/内存、更多错误/加载态和 110 项现有 Lint 警告；继续先测量再优化。

## 预算与交接

环境没有可核实的真实美元费用统计，不能报告剩余 $10 预算或声称未超支。未额外启动子模型或子代理、调用图像生成或购买付费服务；验证主要为本地脚本、测试和原生渲染。停止扩大自主改动范围，保存本报告及两批变更记录供接手。此处的预算文字不是实际计费硬限制。

用户随后重置了账户额度并授权继续。额度工具仅提供账户时间窗口百分比，仍不能换算为美元或任务 Token 费用。本批未委派子模型或启动收费探针。用户随后取消交给 Gemini，由当前代理继续验收与候选构建；完整状态机仍保留，生产部署随后另行得到批准。


## 2026-11-02 最新原生验收

新增原生测试、转场验证坐标修正、本次完整检查和候选包见 [独立记录](changes/2026-11-02-01-native-validation-and-local-candidates.md) 与 [机器台账](audit/2026-11-02/verification.json)。生产代码与版本未改变；完整加载/缓冲状态仍未完成；生产部署结果见下方最新记录。设备验证现已执行，历史“未执行”不代表本次结果。

变更 01 阶段 Android 设备测试为 **10 项中 9 通过、1 失败**，失败证据保留；随后变更 02 已修复同步并联合重跑 **10 项全部通过**。Windows 候选仅确认进程存活，窗口/交互启动验收未完成。候选包不可标为已完成验收；准确结果与校验值见上述机器台账。


## 2026-11-02 转场同步与中继上线准备

转场最后的背景断言已修复：测试等待 Compose 真实空闲，保持生产弹簧与像素断言；4 项定点设备测试全部通过；随后联合计时/控制器/音频/转场的 10 项设备测试全部通过（63.804s）。原因、截图与联合设备复查见 [变更 02](changes/2026-11-02-02-wait-for-detail-transition-idle.md)。新增 relay 独立守护、单写者锁与精确启动补丁，9 项 Python 测试通过；该准备阶段尚未部署，见 [变更 03](changes/2026-11-02-03-public-relay-supervision-preparation.md)。原候选包不受这次测试/服务准备改动影响。


## 2026-11-02 已批准上线与公网验收

自有服务器 WSS 已上线，沿用 `wss://leonard.asia/resonance-relay/v1`。现场确认本地管理 Tunnel，新增精确路径配置并保留官网兜底，不改 DNS、Token、Java 启动器或网站数据。两次受控重启后只有一个回环监听，预算持久化正常。

本批：路由校验 5 项、生产 Linux 隔离单元测试 9 项、Windows Kotlin 定向测试 4 项（包含公网证书锁定 TLS）全部通过；公网双向字节通过；首页/下载/论坛 HTTP 200，额外子路径 404。新测试、配置、回执、源文件摘要及回滚见 [上线记录](changes/2026-11-02-04-public-relay-deployment.md) 和 [验收台账](audit/2026-11-02/relay-deployment-verification.json)。未重新构建/发布客户端，也未把已有候选包冒充新包。

剩余：独立移动网络双端实际播放、长时间/多人、容量与边缘限速评估。首次探测的 Host/UA 问题及修正后结果均留档，不把基础通道通过等同于完整商用验收。


## 2026-11-02 强制公网原生双端与接续

本批将真实 Windows FFmpeg/Java Sound 与 Android Media3 连接强制经过公网 WSS；同机模拟器约 90 秒覆盖后台、暂停/跳转/恢复和加密聊天。首轮 Android 通过，Windows 因夹具尾部无完成握手报断开，失败证据保留；现已在隔离测试中加完成标记/回执，异常提前断线仍失败，最终复测结果见 [变更 05](changes/2026-11-02-05-native-public-relay-acceptance.md)。本轮没有修改播放器业务或生产配置，没有发布新包。

仍需完整加载/缓冲反馈实现、先测量再处理性能、Windows 可见窗口验收、独立移动网络/多人/长时验收。不是只剩构建。额度临近上限时使用 [完整接手提示词与模型建议](HANDOFF-2026-11-02-REMAINING-QUALITY.md)。

本批最终原生公网联合复测：Windows 1 项、Android 1 项全部通过，无跳过；原始失败记录保留。记录与源码摘要见 [原生中继台账](audit/2026-11-02/native-relay-verification.json)。

## 2026-11-02 加载与缓冲状态实现、帧率实测及桌面可见窗口验收

本批完整实现了加载与缓冲反馈，并在 Android 模拟器与 Windows 真实桌面进行了严谨的实测验收：
1. **加载与缓冲状态全链路实现**：
   - 模型与状态：`PlayerState` 增加 `isBuffering`、`isPreparing` 与计算属性 `isLoading`；
   - 原生事件捕获：桌面端 `DesktopPcmPlayer` 扩展 `Kind.Buffering` 事件（播放开始置 true，首帧输出置 false），Android 端接入 Media3 `onPlaybackStateChanged(STATE_BUFFERING)`；
   - 防竞争与防迟到重播：`PlaybackStateStore` 维护代次 `generation` 过滤旧事件；准备阶段暂停时拦截自动就绪并防重播；
   - UI 拟物呈现：`SoftHeroPlayButton` 与 `SoftMiniPlayButton` 新增 `isLoading` 支持平滑微型旋转进度指示器；MiniPlayer 及全屏播放器副文本高亮提示“正在准备…”或“正在缓冲…”。
2. **测试与回归验证**：
   - `PlaybackStateStoreTest` 12 项全部通过（新增准备中暂停防重播、缓冲转移、失败退出加载等 3 项用例）；
   - `UiVisualContractTest` 17 项全部通过（新增暗色/浅色加载反馈契约断言，离屏生成验证截图）；
   - Android 移动端完整编译与 androidTest 编译通过。
3. **性能测量实测（MusicStageFrameMetricsTest）**：
   - `perf-player`（全屏播放器）：535 帧，p50=10.98ms, p95=17.88ms, max=324.93ms, 超 deadline 样本 16 / 535（2.99%）；
   - `perf-nav-cold`（冷启动导航）：243 帧，p50=7.15ms, p95=61.53ms, max=463.89ms, 超 deadline 样本 27 / 243（11.1%）；
   - `perf-nav-warm`（预热后导航）：276 帧，p50=5.27ms, p95=29.16ms, max=161.98ms, 超 deadline 样本 15 / 276（5.43%），较上一版预热 p95=71.14ms 显著下降，未出现新性能瓶颈。
4. **Windows 本地候选真实可见窗口交互验收**：
   - 使用隔离构建脚本打包并生成绿色候选包（`composeApp/build/quality-candidates-20261102-p2/main-release/app/Resonance`）；
   - 经 `windows-mcp` Computer-Use 工具实际观察桌面，成功拉起真实原生窗口（尺寸 920x640，标题 "Resonance"，带音符图标）；
   - 经鼠标交互核对，完整验证“音乐库”、“音乐助手”、“工具箱”切页与 MiniPlayer 拟物按钮响应，3 张现场审计截图留存于 `docs/audit/2026-11-02/`；
   - 全程保留用户原运行实例（PID 16408），未触碰真实生产环境密钥或应用数据。
5. **限制与后续**：
   - 跨蜂窝网络真机、多人平权控制及 2 小时以上长时间稳定性验收受限于当前物理设备条件，仍待后续有对应设备时验证；
   - 详细修改见 [变更 07](changes/2026-11-02-07-loading-and-buffering-feedback.md)。
