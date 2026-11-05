# 2026-11-05 — 移除一起听、房间后台同步与服务器部署依赖

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能移除／原生播放接线／部署清理／文档
- 适用版本：`0.6.6 / 40`，`version.properties` 未修改，未升版
- 影响平台：共享、Android、Windows、Linux；独立官网仅更新说明与部署范围
- 需求：用户明确要求去掉一起听，不再承担房间服务器费用，也不继续实现无服务器点对点方案。
- 前置记录：[一起听服务器接入](2026-11-03-06-aliyun-hosting-and-server-routed-together.md)、[原生播放核心重构](2026-11-05-07-application-core-and-desktop-audio-refactor.md)。历史验收不替代本次验证。

## 需求或问题

主导航和播放菜单提供一起听；房间还关联邀请链接、聊天与伙伴、共享队列、音频预取、歌词共享、Android 服务和桌面后台同步。用户已经决定停止该功能；只隐藏页面仍会留下运行时与服务器依赖。

## 原因与依据

`DesktopPlatformServices` 原先在初始化作用域中实例化房间驱动并周期轮询；Android `PlaybackService` 装配 `AndroidTogetherRuntime`，Activity 与 Manifest 接收 `resonance-listen` 邀请。`App`／`PlaybackSession` 的播放、切歌、队列和错误重试受房间角色影响。阿里云源码包与准备脚本还会包含并安装中继。需要拆除完整链路并恢复本机直接控制。

## 具体修改

| 文件或范围 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [LibraryModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt)、[LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt)、[NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LibraryDestination`、`destinationItems`、`mainTabIndexOf`、播放菜单 | 删除房间目的地、主导航和“与朋友一起听”菜单；工具调整为第三项，手机底栏、窄桌面 rail、宽桌面侧栏共用三项导航 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 原房间订阅、控制与歌词绑定 | 删除房间状态、邀请弹窗、队列投影、分享歌词、远端请求及进出房间恢复；助手点播、浮窗、播放菜单、快照与错误重试直接作用于本机 |
| [PlaybackSession.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/PlaybackSession.kt)、[LyricsSession.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/LyricsSession.kt) | `startTrack`、`moveInQueue`、队列编辑、`load` | 删除角色门禁和房间转发／共享歌词分支；保留请求代次、取消检查、播放快照顺序写入、最新队列读取与迟到结果隔离 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt)、[PlaybackServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlaybackServices.kt) | 平台契约 | 移除房间、通知、邀请分享、临时本机快照和同步播放接口；检查 Android 与桌面实现。桌面独立音频回归使用具体平台的 `awaitPlaybackReady`，不再依赖房间就绪接口 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt)、[PlaybackService.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/PlaybackService.kt)、[AndroidLyricsOverlay.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidLyricsOverlay.kt) | 平台装配、原生服务、悬浮歌词 | 删除运行时、房间音频焦点分支及聊天通知状态；保留 Media3 音频焦点、耳机拔出处理、播放通知、睡眠定时与独立悬浮歌词 |
| [MainActivity.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/MainActivity.kt)、[AndroidManifest.xml](../../composeApp/src/androidMain/AndroidManifest.xml) | 邀请链接入口 | 删除 `resonance-listen` intent-filter、`onNewIntent` 邀请转发；保留应用启动与其他现有权限／服务 |
| [DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt)、[Main.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/Main.kt) | 房间服务、同步作用域、托盘消息 | 删除启动时创建的网络服务、房主／访客驱动、轮询与托盘聊天提示；Windows／Linux 共用独立播放链路，曲终事件继续受睡眠定时控制 |
| 原共享 `Together*` UI／模型／同步驱动、`ListenTogetherService`、JVM `together` 实现及 Android 房间运行时 | 房间、聊天、伙伴、邀请、TLS／WSS／STUN／寻址 | 删除专用生产代码及回归／设备桥接测试；未删除普通助手、原生播放和歌词测试。`PromoAssistantProbeTest` 虽在历史 together 测试包中，仍保留其独立助手验证用途 |
| [composeApp/build.gradle.kts](../../composeApp/build.gradle.kts) | `jvmMain.dependencies` | 删除房间证书专用 `okhttp-tls`；OkHttp 仍供语音同步歌词使用，ZXing 仍供 B 站扫码登录使用 |
| [services/relay](../../services/relay/README.md)、[services/rendezvous](../../services/rendezvous/README.md) | 服务源码、部署单元与测试 | 删除中继／寻址实现、启动配置与专用测试；只保留退役说明和原 `.gitignore`，防止旧私有运行数据暴露到 Git |
| [package.py](../../services/deploy/aliyun/package.py)、[prepare.sh](../../services/deploy/aliyun/prepare.sh)、[benchmark.py](../../services/deploy/aliyun/benchmark.py) | 源码白名单、systemd 装配、回环性能工具 | 不再打包／安装中继、创建中继目录或 venv；性能工具保留官网页面／下载／资源采样，删除 WSS 负载、预算和中继服务操作 |
| [capture.py](../../scripts/promo/capture.py)、[拍摄说明](../../scripts/promo/README.md) | 宣传片工具 | 删除房间／聊天特性摄影机和 `produce_features.py`，移除 `--scene features`；原生播放器录制仍保留 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt)、[DesktopAudioOutputIntegrationTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/DesktopAudioOutputIntegrationTest.kt) | 三项导航／菜单回归、原生就绪检查 | 更新原 UI 场景与夹具，新增实际渲染的三项导航可达性、迷你播放器保留和房间菜单缺席检查；保留真实音频与统计回归 |
| [官网首页](../../website/templates/home.html)、[网站测试](../../website/tests/test_site.py) | 联网说明 | 删除一起听服务宣传；保留在线歌词、歌源和 AI 的真实联网说明，并检查首页输出 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 主导航 | 音乐库／音乐助手／一起听／工具 | 音乐库／音乐助手／工具 |
| 播放菜单与外部邀请 | 可打开房间并接收邀请链接 | 无房间菜单或邀请链接处理 |
| 播放、切歌、seek、队列编辑 | 房间角色可能阻止本机操作或转发远端请求 | 直接调用本机播放器与队列用例 |
| 启动及后台 | 两端装配房间服务／同步驱动，桌面周期轮询 | 不创建房间网络或同步任务 |
| 歌词 | 访客等待房主共享，房主发布已显示歌词 | 对当前本机曲目独立查找和翻译 |
| 官网源码准备 | 打包并安装官网／邮件／中继 | 仅官网／邮件；无中继部署依赖 |

## 平台与数据影响

三端继续使用 Kotlin／Compose 原生工程。曲库、收藏、歌单、播放记录、统计、歌词、音频与 `.resonance` 同步包格式不变；没有迁移或删除用户音乐，没有写回音频标签。原子文件、备份恢复、转换保留源文件、顺序配置写入与播放请求防竞争逻辑保留。

旧房间／伙伴数据、聊天历史和临时音频缓存不再读取，不自动删除。少量 `together:` 旧临时 ID 排除条件继续作为统计、响度及文件操作的兼容保护，不能建立连接。通知权限仍被播放／推荐使用，未因移除聊天而删除；网络权限仍供歌词、助手、歌源等功能使用。

没有读取真实签名配置、私钥或 API Key；没有改变版本、签名和发布说明。开始时已有的 `keystore.properties.example`、`.codex-remote-attachments/`、`.impeccable/`、`website/deploy/nginx-aliyun.conf` 均保留。未修改已发布安装包、用户正在运行的客户端、线上官网／中继、云资源或账单。

## 验证

本次使用仓库已有的同版本 Kotlin 2.3.20／Compose 编译器 CLI，完整重编译生产源码与测试；源文件来自当前工作区，输出目录全新创建，测试运行类路径不包含旧应用 JAR。删除的 `okhttp-tls` 不在生产编译类路径中。不是普通 Gradle Release 构建或安装验证。

| 实际命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python3 build/remove-together-2026-11-05/run.py`，在下方有界 systemd 服务中运行 | 本机 Linux／JDK17，完整 C/J/D 和 C/J/A | 通过：桌面180份、Android172份生产源码；共享／桌面87份测试源码、Android28份设备测试源码编译通过 |
| 同一脚本的15类定向回归 | 隔离数据／合成音频，独立 Xvfb，真实 FFmpeg／Java Sound 输出 | **65项通过，0失败、0跳过**；涵盖切歌竞争、队列、歌词代次、快照／偏好顺序、睡眠、统计、持久化、歌单包及原生音频 |
| `visual_tests.py`／`recheck_menu.py` | 原生 ImageComposeScene／隔离 Xvfb | **最终4项全部通过，0失败、0跳过**（含390／960／1280宽度的导航与短横屏1／1.5倍字体）；首轮导航、短横屏与歌曲信息通过，新增菜单用例因严格平台夹具遗漏 `isVocalSuppressionEnabled` 失败；修正夹具 getter 后重新编译该测试并复查四项，生产代码与断言未因此改动 |
| `build/qa-2026-11-05/venv/bin/python -m unittest discover -s website/tests -p 'test_*.py' -v` | 临时数据库／模拟邮件 | **45项通过**；未访问真实订阅者或 SMTP |
| `python3 -m unittest discover -s services/deploy/aliyun -p 'test_*.py' -v` | 本机回环夹具 | **2项通过**；错误计数与低内存拒绝负载 |
| `sh -n services/deploy/aliyun/prepare.sh`、Python `py_compile` | 本机静态检查 | 通过，覆盖部署准备、打包、性能与拍摄脚本 |
| `python3 services/deploy/aliyun/package.py` 与 ZIP 条目核对 | 本次实际生成的源码 ZIP | 通过：64个条目，不含中继／寻址源码或服务单元；不含真实 `.env`、数据库、作者身份、私钥或安装包。只生成忽略目录中的本地包，不上传 |
| 源码／编译输出扫描、Manifest 与差异核对 | 当前工作区 | 生产代码无房间接口／运行时引用，双端新输出无房间类；Manifest 不再注册邀请 scheme；使用 `git -c core.whitespace=cr-at-eol diff --check` 兼容仓库原 CRLF；本次新增／更新的文档链接核对通过 |
| APK／桌面打包、手机／Windows 系统验收、线上服务退役／网站上线、Ubuntu 实际准备与性能工具 | 未执行 | 本次只交付源码变更与本机验证，不将历史包或上线记录算作本次结果 |

重量级验证使用共享 `build/resonance-heavy-validation.lock`，由已有 `when_build_idle.py` 检查启动余量与运行中1536 MiB保留量。主编译／回归进程树 `MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0`；UI复查为1G／768M／Swap0，始终串行。没有调整系统 swap、提高仓库默认内存或关闭用户应用。主服务日志记录内存峰值约1.4 GiB，最终UI复查峰值431.8 MiB；未触发 OOM 或余量中止。

本轮可复核日志／编译清单：忽略目录 `build/remove-together-2026-11-05/` 的 `desktop/`、`android/`、`visual-first/`、`visual/`；主服务 `resonance-remove-together-validation-20261105`，UI服务 `resonance-remove-together-ui-20261105` 和 `resonance-remove-together-ui-recheck-20261105`。这些本机路径不是提交依赖。

## 限制与后续

源码移除完成；旧版已安装／已发布客户端仍有一起听，须后续构建、安装或发布本次源码后才会改变。远端旧中继与云主机没有操作，不宣称已经停止收费或释放资源。现有未提交 Nginx 配置未覆盖。Ubuntu 部署修改仅作语法、单元及源码包检查；Android 设备测试只编译未实机执行，Windows 未做系统运行验证。

历史变更、发布说明和审计报告保留原事实，并由当前开发说明标明一起听已退役。下一位 AI 从本记录和 `docs/DEVELOPMENT.md` 第7.1节定位，不能再把原房间长时／多人待办当作当前开发需求。

## 同步更新的文档

- `docs/README.md`：最新状态与独立变更索引。
- `docs/DEVELOPMENT.md`：能力表、依赖／平台契约、播放／歌词、7.1节、UI、测试定位、网站部署和助手队列；原房间验收明确归入历史。
- `README.md`、`PRODUCT.md`、`DESIGN.md`：三项导航与独立播放范围。
- `docs/THIRD_PARTY.md`：移除房间传输和专用证书依赖，保留 B 站／语音识别用途。
- `services/relay/README.md`、`services/rendezvous/README.md`、`services/deploy/aliyun/README.md`、`scripts/promo/README.md`、`website/README.md`、`website/OPERATIONS.md`：退役、部署和历史边界。
