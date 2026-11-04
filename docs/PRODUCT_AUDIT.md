# 2026-11-01 — Resonance 产品质量审计与分阶段优化方案

审计范围：当前工作区的原生音乐播放器，以及独立官网、可选寻址/中继的工程边界。版本 **0.6.5 / 39**。这是第一阶段审计，未修改产品代码、依赖、版本、用户数据或生产配置；正式修复等待维护者确认。本文区分本次实验、源码可确认行为与尚待设备复现的风险，不将测试通过解释为产品全部可靠。

## 1. 基线与可恢复性

- 起点为 `main` / `fdf4735`，有 **35 个已跟踪文件改动、112 个未跟踪文件**。包含此前的多人房间、聊天、歌词翻译等实现，不能以 HEAD 代替工作区审计。
- 创建本机备份分支 `codex/audit-checkpoint-20261101-193212`，没有切换分支、提交、推送、stash 或重置。
- 分支只固定 HEAD；未提交内容另以原始字节保存在 `.git/audit-checkpoints/20261101-193212/files/`，共 **146 个文件**。`manifest.json` 记录路径、SHA-256 和起点提交，`initial-status.txt` 保存初始状态。唯一未纳入的文件属于 `.codex-remote-attachments/`；它保持原位，本轮不触碰。忽略的音乐、配置、签名及其他运行数据不属于该源码检查点。
- 恢复时先比较目标文件与 manifest，再从检查点取回需要的文件；不要整树覆盖后续工作。检查点在本机 `.git` 内，不随分支推送，也不是用户数据备份。
- 本次验证输出在忽略目录 `build/product-audit-20261101/`；原生测试渲染位于 `composeApp/build/ui-gallery/screenshots/`。没有读取真实 Key、签名属性或用户音乐作为审计输入。

## 2. 当前架构与已确认能力

### 工程结构

| 部分 | 当前实现与职责 |
| --- | --- |
| 构建 | 单个 `:composeApp` 模块；Gradle Wrapper 8.11.1、Kotlin 2.3.20、Compose Multiplatform 1.11.0、AGP 8.9.2；JDK 17；Android minSdk 26、compile/target 36 |
| `commonMain` | Compose 页面、主题、模型、平台契约、播放状态、顺序持久化；入口 `App.kt`、`PlatformServices.kt` |
| `jvmMain` | AI/音乐来源/歌词、JSON 与 Properties、原子文件、音频 DSP、导入包、房间 TLS/聊天协议 |
| `androidMain` | Media3 1.10.1 的 MediaSessionService、MediaStore/SAF、FFmpegKit、后台任务、系统歌词悬浮窗与系统凭据保护 |
| `desktopMain` | FFmpeg 解码 → 48 kHz PCM → Java Sound；Windows 文件访问、DPAPI、托盘及悬浮歌词；原生房间驱动 |
| UI/媒体依赖 | Material 3 `1.10.0-alpha05`、Haze `2.0.0-alpha02`、ONNX OCR、JNA、OkHttp/TLS、ZXing；预发布 UI 依赖属于回归关注点，本轮不升级或删除 |
| 测试/CI | commonTest、desktopTest、Android instrumentation；CI 运行 `allTests + lintRelease + assembleDebug`，保留报告；没有播放器 TypeScript 工程，也没有发现播放器的 ESLint/tsconfig/独立 detekt 配置 |
| `website/` | 独立 Flask / SQLite / HTML / CSS / JS 官网和匿名论坛，不参与 Gradle，不连接本机曲库 |
| `services/` | 可选 Python 寻址、字节中继；与官网隔离，默认无公共部署。寻址有本地测试，中继缺少独立自动测试文件 |

精确依赖见 [版本目录](../gradle/libs.versions.toml)、[应用构建](../composeApp/build.gradle.kts) 和 [CI](../.github/workflows/verify.yml)。本文是源码事实核对，未进行外部依赖漏洞库或供应链专项审计。

### 已接入的功能与边界

| 能力 | 核对入口与边界 |
| --- | --- |
| 本地曲库、搜索/筛选、收藏、歌单、专辑/歌手、队列、下一首、随机/循环 | `App`、`LibraryShell`、`LibrarySearchIndex` 与两端平台实现；不是云端账号曲库 |
| 原生播放、暂停、进度、音量/增益、速度、音效、睡眠定时、恢复 | `PlaybackStateStore`、`PlaybackService`、`DesktopPcmPlayer`；冷启动暂停恢复与存活会话附着分开处理 |
| 歌词、翻译、歌词偏移、悬浮歌词 | `NowPlayingOverlay`、`MusicFeatureController`、歌词仓库；纯文本不能等同于准确同步歌词，完整原文件写回能力有限 |
| 导入、音频转换、导出/导入歌单包 | `PlainSyncPackage`、Scanner/FFmpeg；KGMA 支持两端，KGG 仅 Windows；扫描转换保留输入，主动删除会影响真实文件 |
| AI 助手、语义搜索、截图 OCR、推荐、元数据补全、B 站导入 | `JvmMusicAssistant`、`MusicAiClient`、`BilibiliClient`；存在付费/网络边界，本次未调用真实付费服务 |
| 一起听、共享队列、伙伴、群聊/回执及邀请 | `ListenTogetherService`、原生 PlaybackDriver、JVM 房间实现；协调设备能读取群消息，无协调者平滑迁移，无任意 NAT 可达保证 |
| 听歌统计、音频特征、品味画像 | `ListeningStatsStore`、音频特征及画像 UI；已发现统计可靠性问题，见下表 |
| 导航和外观 | 主导航音乐库/助手/工具；小于 720dp 底栏、720–1099dp rail、1100dp 起侧栏；已有浅/深色、天气配色、减少动态效果、键盘入口、教程 |

不是每个功能都已做本次端到端验收。设备/LAN 同步及酷狗歌单链接已移除，不作为待恢复功能；没有新增 Web 播放器、付费后端或账号体系的计划。

### 应保留的已有质量基础

- `PlaybackStateStore` 已有 generation、旧任务取消、用户音量/增益版本和原生曲目确认保护，不能简单替换为按 trackId 的副作用。
- `PersistenceQueue` 提供顺序屏障和按键合并；`MusicJsonStore`、`AtomicPropertiesFile` 已有备份恢复，主数据损坏不会直接当空数据正常保存。**听歌统计尚未沿用这套保护**。
- `ArtworkMemoryCache` 有 3 路解码限制、相同路径合并、40 张/800 万像素上限，不应凭空归因于“所有封面同时加载”。
- AI 请求限定 HTTPS、关闭自动重定向、有连接/读取超时、响应大小限制、取消断开；导入 ZIP 有路径、hash、条目及解压大小检查。
- 主要播放控件已有 48dp 语义目标、Slider 语义、焦点描边；舞台动效在暂停/后台/减少动态时停止。不是从零建设可访问性和动画系统。

## 3. 最值得优先处理的 10 项

P0：立即阻断交付的数据破坏/严重安全问题；P1：影响可靠性、关键交互或数据可信度；P2：视觉一致性和维护成本。本轮**未确认 P0**，不代表已证明不存在 P0。

| # / 优先级 | 问题、证据及复现条件 | 建议、收益 | 修改风险与验证方法 |
| --- | --- | --- | --- |
| 1 / P1 | **统计文件损坏后静默丢历史，保存失败也不报告。** `ListeningStatsStore.loadFromDisk/saveToDisk` 均吞异常，无备份；临时损坏 JSON 在下一次 recordPlayback 后被覆盖，已实验复现。不可写位置也正常返回且内存显示已记账。 | 复用原子文件/有效备份策略；损坏时保留证据，保存失败可观测；保护历史及画像可信度。 | 中：涉及旧数据；用损坏主文件/有效备份/两份损坏/不可写/不支持原子移动的用例验证，不操作真实历史。 |
| 2 / P1 | **统计在 UI 调用链每秒全量落盘。** `App.kt:627` 的 LaunchedEffect 调用两端非 suspend 的 recordListeningDuration，直接进入同步 JSON 编码/文件操作。万条合成历史的本机同步更新中位 20ms、P95 71ms。 | 先将写盘移到单写者 IO，再按时间/暂停/切歌/退出合并；降低周期性卡顿与磁盘写入。 | 中：不能牺牲顺序和退出排空；对同一规模重测写入次数/延迟，补异常退出与合并持久化回归。不能由该微实验推导手机帧率提升。 |
| 3 / P1 | **拖动进度连续重启 Windows 解码。** `WavySlider` 每次 onValueChange 都调用 onSeek；`DesktopPcmPlayer.seekTo` 直接 play，关闭旧进程并创建解码线程。代码链路已确认，真实听感/进程峰值待测。 | 拖动时本地预览，松手提交最终 seek；保留键盘与辅助技术单步定位；减少卡顿和进程抖动。 | 中：涉及暂停/房间同步；以一次连续拖动只提交最终位置、取消/切歌不提交旧位置为验收，记录解码启动次数及实际音频恢复。 |
| 4 / P1 | **播放器内播放失败没有对应可见反馈。** `App.kt:622` 写 importMessage；消息在底层 Library/Tools 等页面展示，`LibraryShell` 的全屏 NowPlayingOverlay 没有传入播放错误。源码可确认显示路径缺口。 | 在当前播放器内显示错误及重试/定位文件行动，保持错误原因；关闭播放器后不遗失重要错误。 | 低至中：集中通知不可遮挡控制；用播放失败事件/失效音频测试全屏、助手和普通曲库可见性及重试。听感仍需设备验证。 |
| 5 / P1 | **统计生命周期与原生播放脱节。** 唯一每秒计数循环在 App；Android Activity 销毁但服务继续时没有同等计时。`completedRecorded` 在播放/暂停 effect 重启后重置，95% 后暂停再继续可能重复算完播。 | 由原生播放会话按单调时间记账，完播按会话去重；让后台时长与播放次数可信。 | 中高：避免两端重复记账；验证 95% 暂停/恢复、seek、变速、循环、Activity 重建/后台服务；目前属源码确认缺口，未做设备复现。 |
| 6 / P1 | **随机/循环偏好写入绕过顺序队列。** `App.kt:463,863,874` 使用独立 launch；桌面 savePlaybackMode 切 IO。锁保护单次读改写，却不保证最后一次点击最后写入，已有顺序读写测试不覆盖乱序完成。 | 捕获完整偏好对并经 PersistenceQueue 顺序保存；避免重启恢复成旧模式。 | 低至中：竞态尚待受控延迟测试复现，不宣称现场已发生；注入相反完成顺序验证最后操作优先，并测试恢复。 |
| 7 / P2 | **旧凭据兼容仍允许明文。** DesktopLibraryStore 的旧 deepseek.apiKey 读写入口及开发文档确认历史残留；新模型配置已有 DPAPI/KeyStore。未检查用户实际文件，不断言其 Key 已泄漏。 | 先审计旧入口使用与迁移，再设计可回滚迁移；降低旧主文件/备份的凭据暴露面。 | 高：不能擅自清理配置/备份或使连接失效；只用假测试凭据做升级、恢复、解密失败测试；真实历史清理另行明确授权。 |
| 8 / P1 | **共享播放状态缺少准备/缓冲/错误阶段。** PlayerState 只有 isPlaying 等字段，不能完整区分准备中、暂停和失败；桌面启动路径还存在先发播放意图、后等音频就绪的时段。 | 保留原生状态为事实来源，补充明确阶段和关联曲目/请求；为慢盘、解码失败和断网提供可信反馈。 | 中高：涉及双端契约；不能让新 UI 状态破坏已有代次保护。以延迟就绪、失败、快速 A→B→C、旧事件迟到验证。 |
| 9 / P2 | **视觉规范尚未收敛到本次银灰极简方向。** 当前主题以 Sage/IceBlue 为主，播放波形仍有独立绿色常量，主题字号存在 displayMedium 30sp 大于 displayLarge 28sp；大量尺寸分散在页面。已有设计系统，应渐进收敛。 | 扩展现有语义 Token、补间距/层级，统一主播放器、曲库、设置的密度、焦点与状态；保留封面作为视觉焦点。 | 低至中：不要全局替换颜色或破坏浅色/天气设置；原生截图、对比度、150% 字号、键盘、窄窗验证。实际截图观察见第 5 节。 |
| 10 / P2 | **改动面与验收边界难追踪。** LibraryShell 2850 行、JvmListenTogetherService 2090 行、AndroidPlatformServices 1996 行；文档同时声称同批代码已验证/未构建；CI 不运行设备验收和 Python 测试。 | 先修正验证事实，按下一次小修提取最小状态/控制职责；把稳定本地回归接入现有 CI。降低回归与交接成本。 | 低（文档/CI）、高（大拆分）；不按文件大小直接重构。每批只验证受影响路径，保留基线；三端/弱网/Release 性能单列验收。 |

源码定位：[统计](../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsStore.kt)、[App](../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt)、[进度控件](../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt)、[桌面播放器](../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt)、[桌面平台](../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt)、[共享状态](../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt)。

## 4. 性能、安全与稳定性补充边界

### 本次统计实验

实验直接调用当前编译的 `ListeningStatsStore`，所有输入为 `build/product-audit-20261101/stats-probe-*` 下生成的临时记录；不是用户歌曲、真实听歌历史或优化后的产品演示。每种规模连续写 30 次，计时覆盖同步方法返回。结果：

| 合成历史条目 | 文件大小 | 同步更新中位数 | P95 |
| --- | --- | --- | --- |
| 1,000 | 98,987 bytes | 6ms | 9ms |
| 10,000 | 998,988 bytes | 20ms | 71ms |

复现源文件为本机 `build/product-audit-20261101/StatsProbe.java`，结果为 `stats-probe.txt`。使用 JDK 17 编译，classpath 为当前 desktop/main 类、项目已缓存的 Kotlin stdlib 2.3.20 与 org.json 20250517。测试与 Gradle 基线并行，存在 CPU/磁盘竞争，数字只用于证明调用包含不可忽略的同步工作，**不是正式性能基准、优化收益或真实设备掉帧结论**。

复现方法：向临时 listening-stats.json 写入损坏 JSON，构造 Store 后 recordPlayback，检查原文件/备份；另把 storageDir 设为临时普通文件模拟写失败；规模实验先生成 tracks/daily JSON 再记录 30 次。正式修复时应将这些场景补进已有 `ListeningStatsStoreTest`，本阶段没有修改测试源码。

### 尚待测量/复现，不能据此直接改架构

- `App` 在 progress 更新时复制完整 PlayerState，并以整个 player 为 key 更新悬浮歌词；需要重组计数/帧数据确定实际成本。
- 每两秒保存快照的循环没有暂停/值未变门槛；可以测磁盘写次数，但不能贸然取消最后位置保存。
- 桌面关闭路径同步关闭音频设备并最多等进程三秒；窗口退出卡顿需要原生场景复现，不能仅凭 waitFor 就判定每次卡三秒。
- 桌面回调流主要携带 Float/Boolean/Unit，未统一携带会话 ID；现有 active 检查及 PlaybackStateStore 提供部分保护，仍需受控旧事件延迟测试。
- 房间缓存上限只计算进程内登记文件；异常退出后的遗留缓存需测量。只能清理确属服务所有的临时文件，不能扩大到用户音乐目录。
- Android 允许 cleartext；应先核对现有用户歌源协议再考虑按域收紧，不能在本次审计擅自改变导入能力。没有进行 APK 对抗测试或真实网络抓包。
- 导入包允许较大的解压预算，仍需验证磁盘剩余空间和取消；现有路径/hash 防护必须保留。

## 5. 统一视觉方向与原生检查

方向：**深色录音室式工作界面，银灰控制面、清晰排版、封面主导、少量半透明材质**。保持 Resonance 原生产品身份；不照搬品牌资产，不新增网页外壳。不把银色做成整屏高光渐变，玻璃只用于浮层/播放底座，常规列表保持稳定实色背景。

| Token 层 | 拟定值/用法（待确认，不是已实现） |
| --- | --- |
| 基色 | canvas `#101114`、surface `#191B20`、raised `#24272E` |
| 文字/强调 | primary text `#F1F3F5`、secondary text `#ADB3BE`、silver accent `#CCD2DB`；错误/成功沿用明确语义色，不能只靠颜色传达 |
| 排版 | 复用系统无衬线及中文回退：12/14/16/20/28/36sp；正文14–16，标题20–28；不额外打包大字体；150%字号实测 |
| 间距/形状 | 4/8/12/16/24/32dp；列表行8、控件10、卡片12、浮层20dp圆角；阴影只区分浮起层级，不逐行加阴影 |
| 动效 | 按压100–120ms、状态160ms、播放器220–280ms；主要改变透明度/位移；减少动态时停止装饰运动；高频导航避免持续回弹干扰 |
| 布局 | 保留720/1100dp现有断点；宽窗侧栏→曲库/封面→歌词或队列；底部控制持续可达；窄窗单列、工具折叠，错误就近显示 |
| 可访问性 | 保留48dp命中目标、Slider语义；补全焦点/快捷键路径，弹窗关闭后恢复焦点；对比度和布局以实际渲染核验 |

本轮不会强制删除浅色或天气配色设置。拟先在深色方案和主要播放路径收敛 Token，再验证其他外观模式的兼容。

本次 `UiVisualContractTest` 实际生成 **70张**原生截图，覆盖7类页面、深浅色、360×800、1280×800、640×360及部分150%字号组合。人工查看了下列4张并保存为长期审计证据；其余截图只经过测试现有断言，不能声称逐张完成视觉审查。测试使用 `ImageComposeScene` 与明确标注的测试夹具，不代表真实音频播放或网络服务成功。

| 本次截图 | 可观察结论 |
| --- | --- |
| [桌面播放器 1280×800](audit/2026-11-01/player-dark-1280.png) | 封面、歌词与底部控制层次清楚；适合保留结构、减少背景装饰并收敛银灰控制面。测试夹具暂停时标题仍是“正在播放”，这是页面名称，不能将它作为真实播放成功证据。 |
| [桌面曲库 1280×800](audit/2026-11-01/library-dark-1280.png) | 当前是歌单标签；推荐横幅约占主要内容顶部200px，主操作与推荐引导竞争。建议降低横幅高度，让更多音乐内容进入首屏。夹具含“清空预览”，不是用户真实曲库。 |
| [手机播放器 360×800、150%字号](audit/2026-11-01/player-dark-360-large-text.png) | 主控制和底部标签均可见，保留现有大字适配；英文曲名/歌手与中文歌词的字号层级还可统一。 |
| [设置 640×360、150%字号](audit/2026-11-01/settings-dark-640-large-text.png) | 标题、迷你播放器和底栏占用较多高度，首屏仅见一个设置项目。需要短窗密度方案；截图不能证明其余设置无法滚动到达。 |

上述是**优化前**基线，不是设计升级成果。本阶段没有优化后截图，也没有宣称全页面键盘/读屏验收完成。

## 6. 本次验证台账

所有命令使用现有工具和依赖，没有安装新包、调用真实付费模型、安装/卸载应用或触碰生产网站。

| 检查 | 本次结果 |
| --- | --- |
| Git 状态、检查点及 SHA-256 | 已完成；审计中间核对146份源文件与检查点一致 |
| 桌面测试（含commonTest） | **通过：273项，268成功、5跳过、0失败/错误**；本次65份XML报告，已保存在 `build/product-audit-20261101/desktop-test-results/` |
| Android Release Lint | **通过，0错误、110警告**；本次重新执行了Release Kotlin编译和Lint分析；保留现有弃用/依赖/风格等告警，不为了清零盲目升级 |
| Android Debug APK | **增量构建检查通过，assembleDebug/packageDebug为UP-TO-DATE**；没有重新打包Debug APK，不能称为本次新产物 |
| 统计故障/同步耗时实验 | 已复现上文结果；不是修复，也不是正式基准 |
| 寻址服务 `python -m unittest discover -s services/rendezvous -p 'test_*.py' -q` | **通过：7项** |
| 官网使用系统 Python | **环境受阻：缺少 Flask**，发现5项中2项导入/依赖错误；未安装依赖 |
| 官网使用已有虚拟环境 | **通过：42项**；`.\website\.venv\Scripts\python.exe -m unittest discover -s website/tests -p 'test_*.py' -q`，17.516秒 |
| ADB 设备检查 | 命令成功，0台连接设备；Android实际播放/后台/仪器测试未执行 |
| Release 打包/签名/安装 | 未执行；不以已有产物作为本次通过证据 |
| 类型检查 | Kotlin 编译纳入 Gradle；播放器没有TypeScript类型检查任务 |
| 三端房间/跨网/弱网/长时播放、蓝牙音频焦点 | 未执行；需设备及专用验收场景；本地协议测试不能替代 |
| 官网浏览器/SMTP/远程部署、中继公网部署 | 未执行；本轮范围是播放器审计与隔离单元检查 |
| 文档/源码保护 | 新报告相对链接均有效；相对检查点只改动原有README/DEVELOPMENT文档，原有产品/测试源码哈希未变化；本次文档差异没有空白错误 |

Gradle整体退出码0，`BUILD SUCCESSFUL in 10m 54s`；77项任务中12执行、1来自缓存、64为UP-TO-DATE。桌面测试实际重跑，不是引用旧XML；桌面编译为UP-TO-DATE。未运行Android instrumentation、`allTests`或Release安装包生成。

Lint的110条警告包括：UseKtx 38、NewerVersionAvailable 24、UseTomlInstead 23、GradleDependency 9、ApplySharedPref 7、其余9条。唯一StaticFieldLeak命中AndroidTogetherRuntime的静态单例；源码保存的是 `context.applicationContext`，不能将这条告警直接判定为Activity泄漏。需保留后续生命周期/堆检查，不在审计中盲目抑制或重构。

五项跳过为 `PromoAssistantProbeTest`、`TogetherDeviceBridgeTest`、`TogetherNativeBridgeTest`、`TogetherReverseNativeBridgeTest` 的显式 opt-in，以及 `TogetherRendezvousTest.javaSignaturesInteroperateAndChangedPartnerEndpointCanBeInvited` 未配置 `RESONANCE_RENDEZVOUS_PYTHON`。Python寻址7项通过不能替代最后一项JVM/Python互通测试。部分音频测试还使用条件返回，绿色计数不等于本轮验证了每种解码器和音频输出设备。

Gradle 基线命令：

```powershell
# 当前命令进程中隔离两个根目录，并禁用真实AI/设备联调测试开关。
$env:RESONANCE_DATA = "$PWD/build/product-audit-20261101/data"
$env:RESONANCE_LIBRARY = "$PWD/build/product-audit-20261101/music"
$env:RESONANCE_PROMO_AI = '0'
$env:RESONANCE_NATIVE_BRIDGE = '0'
$env:RESONANCE_REVERSE_BRIDGE = '0'
$env:RESONANCE_DEVICE_BRIDGE = '0'
.\gradlew.bat :composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain
```

## 7. 10美元预算下的执行顺序与交接

费用工具只给出账户时间窗口用量百分比，**没有本次输入/输出Token费用、API美元消费或可核实的10美元剩余额度**；账户额度不能换算为本任务成本。不承诺提示词预算等同于计费硬限制。本阶段暂停高成本多模型委派、图像生成和重复全库扫描，仅使用单代理定点审计及本地工具。

下列金额是维护者可选择的**规划上限分配**，不是已花费、报价或已配置的硬限制。进入正式实现前，必须从实际计费端确认剩余额度；若仍不可见，应先确定可控的一小批工作，不启动全部优化。

| 顺序 | 累计预算计划内的分配 | 具体交付与停止点 |
| --- | --- | --- |
| A | 审计/定位最多$2 | 本文、检查点、基线与方案；本次实际费用未知，不能宣称小于$2 |
| B | 最多$3 | 首批只做#1/#2统计存储与写盘、#3进度提交、#4播放器错误；每项独立变更记录，必要回归通过后停止评估 |
| C | 最多$2 | #9现有Token与播放器/曲库视觉收敛；先两页面，截图确认后再扩到设置/弹窗，不同时重做全部页面 |
| D | 最多$2 | 按剩余额度处理#5/#6/#8，优先可复现且影响日常播放的问题；凭据迁移、大拆分和公网服务留后续 |
| E | 保留$1 | 受影响测试、原生截图对比、结果报告与交接；不足时减少范围，不减少验证或声称全完成 |

如计费环境能确认 GPT-6 Astra 可用及预算，可仅用于关键架构/竞态或一轮视觉评审，普通实施由低成本模型按小任务接续。本轮没有额外启动模型，也未验证该模型可用性，不能承诺自动切换或价格。

第一批预计涉及 `ListeningStatsStore`、`App`、两端 `PlatformServices`、`SoftNeumorphicButtons`、`LibraryShell/NowPlayingOverlay` 及对应回归。第二批视觉涉及 `ResonanceTheme`、`GallerySurfaces`、`MiniPlayer`、`LibraryShell`，保持公共接口和业务动作。首批不重写房间协议、不改同步包、不做真实凭据清理、不升级依赖或发布。

后续每阶段记录真实测试与剩余问题；仅在正式优化完成后生成 `docs/OPTIMIZATION_REPORT.md`，包含修改清单、回归、前后截图及限制。本轮不提前创建“优化完成”报告。

**下一步：等待维护者确认首批范围和视觉方向，再进入代码修改。**
