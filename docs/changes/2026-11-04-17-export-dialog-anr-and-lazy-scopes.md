# 2026-11-04 — 实机定位 Android 导出选项 Haze ANR 并限制歌单首屏加载

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复、设备验证
- 适用版本：`version.properties` 为 0.6.6 / 40，本次不升版；手机复现包为较早的 0.6.5 / 39
- 影响平台：共享 Compose UI（Android、Windows、Linux）；ANR 在 Android 16 / API 36 真机复现
- 关联记录：[通用弹窗隔离](2026-11-04-15-volume-dialog-window-isolation.md)、[手机原安装包](2026-11-04-14-frosted-glass-palette-and-native-controls.md)、[三端发布](2026-11-04-16-release-0.6.6-three-platforms.md)

## 需求或问题

用户反馈 Android 导出音乐包再次卡死，并明确发生于点击导出、选项弹窗刚打开时，尚未确认打包。此次手机已连接 ADB，直接在正式包的「工具 → 歌单导入与导出 → 导出歌单」复现，弹窗显示后 UI 不再响应。

## 原因与依据

**ANR risk: yes (HIGH)，归因 APP_DEFECT。** 这次抓到的是前台应用的真实输入超时，不再仅根据桌面测试推测手机行为。

### 1. 线程栈与环境

- 进程：`com.resonance.player`，Foreground=Yes；手机 Android 16 / API 36。
- ANR：2026-11-04 20:27:16.438 +0800，`Input dispatching timed out`，等待窗口 `FocusEvent(hasFocus=true)` 超过 5000ms。
- 线程转储：20:27:16.445，距 ANR 约 7ms，Immediate；主线程 `tid=1` 为 Runnable。
- 主线程链：`GlobalSnapshotManager → Snapshot.sendApplyNotifications → SnapshotStateObserver.sendNotifications/notifyInvalidatedScopes → HazeSourceNode.onObservedReadsChanged → SnapshotStateSet.isEmpty → SnapshotKt.readable`。
- 当次进程 CPU 104%（98% user），设备总 CPU 32%、iowait 0%；RenderThread 在 `epoll_pwait`。证据指向应用主线程的 Haze 状态通知，未发现外部 IO 饱和证据，不能误判为同步屏障泄漏或 ZIP 压缩阻塞。
- 原始日志、ANR、UI dump 和手机截图保存在忽略目录 `build/export-dialog-fix-2026-11-04/`，含用户本机信息，不提交到仓库。

手机安装 APK 的 SHA-256 通过设备 `pm path` / `sha256sum` 实际读取：`e67ee588cd6505038d6c4376fd2a1168a13335cb2366fb67b7146bb466bf6220`，对应变更 14 的 0.6.5 包，尚未包含变更 15 的通用窗口隔离。本轮先验签并覆盖安装既有隔离包（实际 SHA-256 `56803e3c950996d219eb59e5a583f579bc372cfa316494a20630745ec19ebeec`），在同一手机、同一正式曲库成功打开、读取 UI 树、取消并返回响应中的宿主页。这是对既有修复的新增真机因果验证，不能把该隔离代码写成本轮新实现。

### 2. 同链路的大歌单加载缺陷

`SyncPackageDialog` 原来使用 `Column + verticalScroll + playlists.forEach`，260dp 高度限制只限制可视区域，所有行仍在 Compose UI 线程同步创建和测量。新增回归在生产修改前失败：300 个歌单首帧产生全部 300 个选项，离屏首帧 1363ms。此测量只说明加载成本，不代表手机帧率，也不把大歌单缺陷冒充本次 Haze ANR 的直接原因。

### 3. 已排除候选

- `SyncScreen.onExportSync` / `App.syncAction`：只改内存 UI 状态，没有磁盘／网络／压缩。
- Android `exportSyncPackage`：只在确认后调用，函数体为 `withContext(Dispatchers.IO)`，不属于这次打开时主线程阻塞。
- 音频／封面读取和 ZIP SHA-256：发生在实际打包阶段，未到达，保持原有容错和校验。
- 未发现此调用链的后台线程修改已附着 Android View 的证据，不将其列为同步屏障泄漏。

## 具体修改

| 文件 | 关键类／函数 | 具体修改与目的 |
| --- | --- | --- |
| [SyncPackageDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncPackageDialog.kt) | `SyncPackageDialog`、`PlaylistOption` | 正文改为最大 360dp 的 `LazyColumn`，说明和全库／歌单一起滚动；稳定 key 和 contentType 复用行，仅创建视口所需内容。标题／确认／取消继续使用已隔离的 Material Dialog。整行至少 48dp，`selectableGroup`／`Role.RadioButton` 提供单选语义，圆形指示器不再单独重复点击 |
| [SyncPackageDialogTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/SyncPackageDialogTest.kt) | 三项桌面回归 | 验证 300 歌单首屏有界；1000 歌单末项可选；浅／深色与正常／短屏／大字动作可达；取消、全库 null、导入 null 及确认前无导出副作用 |
| [SyncPackageDialogDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/SyncPackageDialogDeviceTest.kt) | `exportScopesScrollConfirmCancelAndReopenWithoutFreezingHost` | 原生宿主 Haze source + 真实 `SyncScreen` 入口 + 独立 Dialog，虚构 1000 个歌单；两主题重复打开取消、滚到末项确认、重开恢复全库、导入确认及宿主继续响应，不读取／打包用户歌曲 |

调用链仍是 `SyncScreen → App.syncAction → SyncPackageDialog → onConfirm → Android/DesktopPlatformServices.exportSyncPackage`。打开、滚动、改变选择和取消均不触发导出；数据格式和平台 IO 实现不变。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 原手机较早 0.6.5 包打开导出 | 真实 Haze 主线程 ANR，输入超时 | 同机安装窗口隔离包后成功打开／取消；最终交付包结果见验证 |
| 大歌单列表打开 | 全部歌单同时创建，只限制可视高度 | 仅创建可见行，滚动后再按需加载 |
| 短屏／大字号 | 说明固定占据正文，挤压歌单可视高度 | 说明随正文滚动，Material 操作区保持独立 |
| 歌单选择 | 圆形控件和整行各有点击语义 | 一行一个单选目标，选择后仍需显式确认 |

## 平台与数据影响

共享 UI 影响 Android、Windows、Linux；未修改平台接口。`.resonance` 格式、SHA-256、路径边界、大小限制、缺封面回退、导入合并、音频转换、播放任务代次及持久化顺序保持原实现。无新增权限、外部请求、凭据或配置键，不删除原音频、不写标签。覆盖安装保留原正式包应用数据；本轮不公开发布。

## 验证

环境：Linux x86_64、已有 Temurin JDK 17／Android SDK 36，使用仓库 Wrapper；手机为 Android 16 / API 36。没有直接读取或输出真实签名配置、私钥或 API Key。

| 命令或操作 | 结果与证据 |
| --- | --- |
| 原正式包打开导出，`dumpsys activity lastanr`／`dumpsys dropbox --print data_app_anr` | **已复现**：20:27:16 输入超时，Immediate 主线程栈及 Haze／CPU 证据见上文 |
| 已有通用窗口隔离包验签、`adb install -r`、同机打开与取消导出 | **通过**；原曲库保留，UI 树恢复可读、取消后宿主响应；该包不是本次新打包产物 |
| 生产修改前 `desktopTest --tests 'com.resonance.player.ui.SyncPackageDialogTest'` | **预期失败**：300 个歌单全部创建，首帧样本 1363ms；XML 已保存在 `baseline-results/` |
| 最终 `desktopTest`，过滤 `SyncPackageDialogTest`、`PlaybackSettingsDialogTest`、`PlayerRecoveryUiTest`、`PlainSyncPackageTest`、`features.PlaylistArchiveFormatTest` | **通过**：13 项、0 失败／错误／跳过；覆盖 MP3 无封面往返、FLAC 扩展名与 MIME、SHA-256 拒绝损坏、旧加密包拒绝，以及弹窗操作。300 歌单仅创建 5 行，首帧单次离屏样本 119ms；不是同机性能基准或 Android 帧率结论 |
| `:composeApp:assembleRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest` | **通过**：串行构建 `BUILD SUCCESSFUL in 1m 44s`，118 tasks；Release、Debug 和仪器测试 APK 均为本轮源码产物 |
| `:composeApp:lintRelease` 与上述最终 13 项回归联合执行 | **通过**：`BUILD SUCCESSFUL in 56s`；Lint XML 为 0 Error、107 Warning，不将现有 Warning 写成零警告 |
| 最终 APK `apksigner verify --verbose`／`zipalign -c -P 16 4`／`aapt dump badging` | **通过**：v2=true、1 个签名者；v1=false；16KB 对齐退出码 0；正式包名 `com.resonance.player`，0.6.6 / 40 |
| `adb shell am instrument -w -r -e class com.resonance.player.SyncPackageDialogDeviceTest,com.resonance.player.PlaybackSettingsDialogDeviceTest,com.resonance.player.PlayerDialogsDeviceTest com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | **通过**：同一 Android 16 手机，`OK (3 tests)`，11.701s。导出两主题反复打开取消、1000 歌单末项确认、全库／导入 null，以及音量调整和音效选择后宿主播放控件继续响应；使用虚构曲目和歌单，不打包用户音乐 |
| 最终正式包 `adb install -r` 与同机真实曲库复测 | **通过**：保留正式应用数据更新为 0.6.6 / 40；设备上实际 APK SHA-256 与本次产物完全一致。连续 5 次打开导出 → 选择歌单 → 取消 → 宿主可响应，每次重开默认全库；导入选项打开／取消同样通过。`lastanr` 仍是原包的 20:27:16 记录，无新 ANR；结构化结果在 `final-device-result.json`，截图已实际查看，临时手机 UI dump 已清理 |

最终两组 Wrapper 命令均使用 `--no-configuration-cache --console=plain --max-workers=1 -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process`；本机工具目录只通过该次进程环境传入，不提交路径。完整输出在忽略目录 `build/export-dialog-fix-2026-11-04/` 的 `serial-build.log`、`final-regression-and-lint.log`，XML 和源码摘要另存其中。

中间尝试如实保留：一次新增测试因 nullable 期望列表类型推断错误未编译，显式 `List<String?>` 后通过；首次默认并发联合任务完成 12 项桌面回归后，构建机 15GiB 内存耗尽，主动终止该构建及其 Java 进程，不能将那次整体记为通过。改用上述受限串行设置后重新完成构建、完整 13 项定向回归及 Lint。第一次测试 APK 安装遇到旧 `com.resonance.player.debug.test` 签名不同，确认其为仅针对 debug 的 instrumentation runner 后仅重装该测试包；正式包和 debug 主应用均未卸载。

### 本次独立安装包

- [Resonance-0.6.6-export-dialog-fix-2026-11-04.apk](../../release/Resonance-0.6.6-export-dialog-fix-2026-11-04.apk)
- 大小：167,402,220 字节（约 159.65 MiB）。
- SHA-256：`1799dedf93b8b6b6e5dc081bb590d777e7c6d772786a667956f89f8766ddfb42`；由本次实际 APK 计算，[校验文件](../../release/Resonance-0.6.6-export-dialog-fix-2026-11-04.apk.sha256)。
- APK／校验文件为忽略的本地交付产物，未上传或公开发布。

## 限制与后续

这次已在用户原手机复现旧包卡死，并完成隔离包对照、最终 Debug 三项原生回归和最终正式包真实曲库五轮交互复测；不能再沿用上一轮“设备未连接”的限制。本次确认的是打开／选择／取消导出选项的卡死处理，没有在手机打包或复制用户真实音频，也未执行真实全库大文件导出。包格式往返、无封面、FLAC 和校验边界由本次 JVM 回归验证。

没有运行全量 Kotlin 测试、其他 Android ROM／设备、Windows 或 Linux 可见桌面窗口验收；Linux JVM 离屏测试不等于 Windows 实机验证。没有改动或验证官网、服务器、网络同步部署。若其他入口再出现卡死，应先核对设备 APK 哈希与 ANR 主线程栈，再判断是否同一窗口采样问题，不能仅凭“弹窗”二字复用结论。

## 同步更新的文档

- [文档入口与变更索引](../README.md)。
- [开发说明第 9 节与第 14 节](../DEVELOPMENT.md#sync)：补充打开／确认线程边界、歌单虚拟化和定点回归入口；保留其他尚未提交的文档修改。
