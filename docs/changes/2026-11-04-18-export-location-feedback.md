# 2026-11-04 — 导出歌单完成后直接提示保存位置并提供查看入口

> 2026-11-04 接续：手机重连后，本记录的正式 APK 已成功覆盖安装，真实 8 首歌单导出、完成提示及复制位置通过（32,764,403 字节）。下文「断连未安装」保留早期验证时点；用户随后要求直达文件夹及按歌单名命名，当前实现和接续验收见[记录 19](2026-11-04-19-playlist-names-and-direct-export-directory.md)。本记录原有下载入口不能当作直达目录验证。

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复／交互补全
- 适用版本：0.6.6 / 40；不修改 `version.properties`
- 影响平台：共享 Compose UI、Android、Windows、Linux
- 关联需求：用户导出成功后不知道文件存在哪里，要求应用中主动提示；前轮卡死修复见[导出选项 ANR](2026-11-04-17-export-dialog-anr-and-lazy-scopes.md)

## 需求或问题

用户点击导出并生成文件后，需要回到对话里询问保存位置。原 Android 平台已返回「已导出到下载/Resonance：文件名」，但 App 仅赋值给通用 `importMessage`，在导入导出页顶部显示；没有明确的导出中状态、完成提示或查看入口。大包导出时用户等待或切页，很容易错过反馈。

## 原因与依据

`App` 的 `SyncPackageDialog.onConfirm` 原先关闭选择弹窗后直接启动协程；任务没有独立运行状态，完成消息也没有 App 级结果视图。`SyncReport` 仅有一句消息，没有结构化文件名与保存位置，UI 无法可靠提供查看／复制动作。Android 26–28 实际写入应用外部下载目录，旧消息却始终写公共下载目录，此处同时改为实际路径。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [LibraryModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | `SyncReport`、`ExportedPackage` | 添加可选的结构化导出文件名、保存位置、查看按钮文案，以及默认 false 的取消标志；不解析显示消息来猜测文件位置或判断取消 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `syncInProgress`、`showSyncProgress`、`syncResult`、归档确认回调 | 启动时立即显示处理状态；关闭处理中提示只隐藏窗口，协程继续。完成／失败由 App 级结果提示呈现，切页后也可见；取消选择器不误报失败；确认时和入口均防重复，finally 释放状态，协程取消继续传播 |
| [SyncPackageFeedback.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncPackageFeedback.kt) | `SyncPackageProgressDialog`、`SyncPackageResultDialog` | 明确「正在导出歌单」「歌单导出完成」，展示保存位置和完整可滚动文件名、复制位置、查看下载／打开文件夹。打开失败保留结果并给出手动查找提示；使用已隔离 Haze 的 `FrostedAlertDialog`，不重新引入跨窗口采样 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt)、[ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `SyncScreen.operationInProgress` | 归档处理中禁用导入／导出入口，页面顶部继续保留状态消息 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `saveToDownloads`、`exportSyncPackage`、`openExportLocation` | 成功发布 MediaStore 文件后返回文件名和公共下载位置；26–28 返回真实应用目录。29+ 在主线程启动系统下载列表，无法处理时返回 false，不要求额外存储权限 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt)、[DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `openExportLocation`、桌面导出结果 | 新平台能力默认 false；Windows／Linux 共享桌面实现验证目录与系统能力后在 IO 线程调用系统打开；返回真实绝对父目录和文件名 |
| [SyncPackageFeedbackTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/SyncPackageFeedbackTest.kt)、[SyncPackageFeedbackDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/SyncPackageFeedbackDeviceTest.kt) | 反馈回归 | 覆盖长文件名、两主题／大字短屏、按钮可达、失败不冒充成功、运行时不可重复启动，以及原生结果窗口中查看失败和复制回退 |

调用链仍为 `SyncScreen → SyncPackageDialog → App 协程 → 平台 exportSyncPackage → PlainSyncPackage`。归档内容仍在 IO 执行；仅成功返回后显示成功结果。没有伪造百分比，也没有把关闭提示写成取消任务。处理状态仅属于本次进程，不承诺进程终止后继续导出。

Android「查看下载」会打开系统下载列表，并不承诺直接选中某个文件夹；文件夹路线和文件名仍在结果中清晰显示。API 依据为 [DownloadManager.ACTION_VIEW_DOWNLOADS 官方文档](https://developer.android.com/reference/android/app/DownloadManager#ACTION_VIEW_DOWNLOADS)。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 开始导出大歌单 | 选项关闭，无明确处理提示 | 显示正在打包和等待说明，可关闭提示继续使用应用 |
| 导出成功 | 仅原页面顶部一条长消息 | 主动显示完成结果、保存位置、文件名和可用查看动作 |
| 等待期间切换页面 | 可能错过原页面消息 | 完成结果由 App 级显示 |
| 重复点击 | 可能同时启动多个归档任务 | 入口和确认回调都阻止重复运行 |
| 文件管理器不可用 | 无查看能力 | 保留路径和文件名，说明手动查找，仍可复制位置 |
| 系统文件选择器取消 | 无结构化取消状态 | 取消不弹失败结果，释放运行状态 |

## 平台与数据影响

共享 UI 和新平台能力均已接线到 Android／桌面；Windows 与 Linux 共用 `java.awt.Desktop` 的系统打开能力。没有新增依赖、权限、联网请求或凭据；不修改音乐、标签、曲库、归档格式、SHA-256、路径校验、导入合并、播放竞争或配置写入顺序。新报告字段只在内存中，默认参数兼容旧调用。Android 写包仍按原 MediaStore pending／异常删除流程完成后才报告成功；26–28 的实际保存目录不变，只修正显示。

## 验证

环境：Linux x86_64、既有 JDK 17 / Android SDK 36；手机 Android 16 / API 36。使用仓库 Wrapper，不修改版本、构建配置或工具路径。

| 命令或操作 | 本次结果 |
| --- | --- |
| `:composeApp:compileKotlinDesktop` | **通过**，首轮编译 `BUILD SUCCESSFUL in 1m 27s` |
| `:composeApp:desktopTest`，过滤 `SyncPackageFeedbackTest`、`SyncPackageDialogTest`、`PlaybackSettingsDialogTest`、`PlayerRecoveryUiTest`、`PlainSyncPackageTest`、`features.PlaylistArchiveFormatTest` | **通过**：16 项、0 失败／错误／跳过；包含新反馈三项、前轮导出／音量回归及 MP3／FLAC／包校验回归 |
| 增加离屏截图输出后，仅重跑 `SyncPackageFeedbackTest` | **通过**：3 项、0 失败／跳过，4s；浅／深色 × 正常／短高大字共 4 张截图在 `composeApp/build/export-feedback/screenshots/`，实际查看浅色短高大字和深色正常高：保存位置可读、文件名正文可滚动、底部按钮保持可达；这是桌面离屏视觉检查，不是正式手机包截图 |
| 同一 Wrapper 联合执行 `:composeApp:assembleRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease` | **通过**，`BUILD SUCCESSFUL in 3m 16s`，134 tasks；三个 APK 产出；Lint XML 为 0 Error / 107 Warning |
| 安装本次 Debug 与仪器测试 APK，`am instrument -w -r -e class com.resonance.player.SyncPackageFeedbackDeviceTest,com.resonance.player.SyncPackageDialogDeviceTest,com.resonance.player.PlaybackSettingsDialogDeviceTest,com.resonance.player.PlayerDialogsDeviceTest com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | **通过**：同一原手机 `OK (4 tests)`，14.717s；反馈显示、复制、打开失败回退、关闭后宿主响应，以及导出／音量／音效窗口回归通过 |
| `apksigner verify --verbose`、`zipalign -c -P 16 4`、`aapt dump badging` 独立正式 APK | **通过**：v2=true、1 signer，v1=false；对齐退出码 0；正式包 `com.resonance.player`，0.6.6 / 40 |
| 检查本次 APK 解压后的 DEX | **通过**：新 `SyncPackageFeedback` 类和「歌单导出完成」文案存在，未误用前轮旧安装包 |
| 最终正式 APK 覆盖安装、真实小歌单导出与「查看下载」拉起 | **环境受阻，未执行**：前述 Debug 仪器测试通过后，原手机从 ADB 断开，`adb install -r` 返回 `no devices/emulators found`；本轮没有成功覆盖正式包，也未启动真实歌单导出 |

联合 Wrapper 使用 `--no-configuration-cache --console=plain --max-workers=1 -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process`，沿用前轮已验证的受限串行方式避免构建机内存争用。日志、XML、签名校验及受阻日志保存在忽略目录 `build/export-feedback-2026-11-04/`。没有把旧产物或前轮验证写成本次通过。

### 本次独立安装包

- [Resonance-0.6.6-export-feedback-2026-11-04.apk](../../release/Resonance-0.6.6-export-feedback-2026-11-04.apk)
- 大小：167,402,218 字节，约 159.65 MiB。
- SHA-256：`d2a96ade1e39e4a94b4d80374b4e3d12de6ae4f6d9ec112c81fe6592b361b56d`，从本次实际 APK 计算；[校验文件](../../release/Resonance-0.6.6-export-feedback-2026-11-04.apk.sha256)。
- 本地构建并验签，未上传或公开发布；手机最终正式包安装受阻，不能写成已覆盖更新。

## 限制与后续

这次已实际运行原手机 Debug 四项回归；最终正式 APK 的真实歌单导出、完成提示和系统下载列表拉起，因手机在最后安装阶段断开而未验证。用户可以重新连接后继续，或自行安装上方 APK；不能将 Debug 组件测试说成正式包真实音频导出验收。

未运行全量 Kotlin 回归、Android 26–28、其他 ROM、Windows／Linux 可见文件管理器打开验收。Windows 和 Linux 的共享实现已检查并经桌面编译；无可用系统文件管理器时以返回 false 和保留路径／复制为兜底。关闭进程后归档任务不保证继续，未新增后台服务或任务恢复。本轮没有执行官网、网络同步部署或线上发布。

## 同步更新的文档

- [文档入口与变更索引](../README.md)。
- [开发说明第 9、14 节](../DEVELOPMENT.md#sync)：当前归档状态、结构化结果、平台查看差异和回归入口。
