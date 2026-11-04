# 2026-11-04 — 导出歌单按歌单名保存并直接打开所在目录

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复／交互完善、正式包安装验证
- 适用版本：0.6.6 / 40；不升版
- 影响平台：Android 目录跳转、Android／Windows／Linux 导出命名
- 关联记录：[导出完成反馈](2026-11-04-18-export-location-feedback.md)、[导出选项 ANR](2026-11-04-17-export-dialog-anr-and-lazy-scopes.md)

## 需求或问题

用户重新连接手机并要求安装验证后，前轮导出反馈正式包已成功覆盖安装，真实 8 首歌单完成导出，文件大小 32,764,403 字节，完成提示和复制位置可用。用户随后指出两个剩余问题：查看下载没有直接打开保存目录；时间戳文件名在文件管理中难以辨认，需要使用简洁的歌单名。

## 原因与依据

1. 前轮使用 `DownloadManager.ACTION_VIEW_DOWNLOADS`，它是系统下载入口，未携带导出子目录信息；不能把成功发起该 Intent 当作已打开正确文件夹。原手机查询目录 VIEW 的处理器有五个，其中含通配类型的音乐／网盘应用，任由系统解析还可能进入应用选择器。
2. Android `saveToDownloads` 固定使用 `Resonance-Sync-<毫秒>.resonance`；桌面选择器默认固定 `Resonance-Sync.resonance`。平台未使用当前选中歌单的名称作为建议文件名。
3. 本轮在原手机先通过 `ACTION_VIEW + content 文档 URI + vnd.android.document/directory + 系统 DocumentsUI` 验证可打开 `primary:Download/Resonance`；重复打开时进一步发现 FilesActivity 会复用上次停留的父目录。最终加入 `NEW_TASK | NEW_DOCUMENT | MULTIPLE_TASK`，使用新的目录文档任务处理每次明确跳转；并增加验证 Intent flags 及没有 URI 读写授予的设备回归。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [SyncPackageNaming.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/SyncPackageNaming.kt) | `syncPackageFileName`、`uniqueSyncPackageFileName` | 单歌单用名称，全库用「整个音乐库」；保留中文和正常 emoji，清理路径／控制／Windows 非法字符，处理空名称和保留设备名；限制长度时不截断代理对；已有名称后追加 `(2)`、`(3)` 等序号 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `exportSyncPackage`、`saveToDownloads` | 传入实际歌单名；29+ 查询同目录已有名称并选空闲序号，插入后读取实际 DISPLAY_NAME，避免系统自动去重后提示的文件名不符。旧版应用下载目录沿用原位置，复制时不覆盖已有文件 |
| [DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `exportSyncPackage`、`chooseSyncPackageOnEdt` | Windows／Linux 的保存选择器默认显示可读歌单名；确认后再次检查重名并选择序号；结果继续返回实际路径和文件名 |
| [AndroidExportLocation.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidExportLocation.kt) | `exportDirectoryUri`、`openAndroidExportDirectory` | 使用固定 `Download/Resonance` 文档 URI 和目录 MIME 的 VIEW；优先系统 DocumentsUI，其次明确声明目录 MIME 的系统浏览器，显式指定组件，避免误入网盘／音乐应用或选择器；用新文档任务避免复用已停在其他目录的窗口。拒绝其他目录／来源 URI，找不到处理器时保留复制位置回退 |
| [AndroidManifest.xml](../../composeApp/src/androidMain/AndroidManifest.xml) | `<queries>` | 仅声明 content 目录 VIEW 的包可见性查询，不新增权限、公开组件或 URI 授权 |
| [LibraryModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt)、[PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt)、[SyncPackageFeedback.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncPackageFeedback.kt) | `ExportedPackage.locationUri`、`openExportLocation` | 显示位置与平台跳转 URI 分离；Android 传明确 URI，桌面仍传原生目录路径；按钮文案为「打开所在目录」 |
| [SyncPackageDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncPackageDialog.kt) | 导出说明 | 明确按歌单名命名及重名自动加序号，继续沿用有界懒加载和窗口隔离 |
| [SyncPackageNamingTest.kt](../../composeApp/src/commonTest/kotlin/com/resonance/player/model/SyncPackageNamingTest.kt)、[AndroidExportLocationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AndroidExportLocationTest.kt) | 名称与目录边界 | 新增中文／全库／扩展名、保留名、非法路径字符、Unicode 长度、重名序号和目录 URI 拒绝边界回归；更新原生反馈测试验证跳转使用 URI 而非显示文本 |

不更改歌单本身、音频或包内部清单，仅改变导出包外部名称和保存后查看入口。目录规则及 API 依据见 [DocumentsContract 官方文档](https://developer.android.com/reference/android/provider/DocumentsContract) 与 [包可见性查询文档](https://developer.android.com/training/package-visibility/declaring)。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 导出「某歌单」 | 文件名是长时间戳，列表中难以辨认 | `某歌单.resonance` |
| 再次导出同一歌单 | 继续生成时间戳名称 | `某歌单 (2).resonance` 等，保留已有导出 |
| 导出整个库 | 仍用含义不清的 Sync 名称 | `整个音乐库.resonance` |
| 点击完成提示的查看动作 | 打开通用下载入口 | 直接打开系统浏览器中的 `Download/Resonance` |
| 名称含斜线、控制字符或过长 emoji | 没有对应命名规则 | 转为安全单个文件名，并保持 `.resonance` 扩展名 |

## 平台与数据影响

Android 29+ 的保存位置仍为公共 `Download/Resonance`；26–28 保留应用外部下载目录及复制位置能力。Windows／Linux 继续使用原生保存对话框，默认命名和去重共用新规则。已有时间戳导出包不重命名、不删除。包格式、校验、导入兼容、原音频、标签、播放任务、配置写入顺序均不变；`locationUri` 为内存字段，不写入归档或曲库。目录跳转只使用固定 URI，新增的是窄范围包查询，不新增存储权限、URI 读写授予、外部请求或凭据。

## 验证

环境：Linux x86_64、既有 JDK 17 / Android SDK 36；用户原 Android 16 / API 36 手机。使用仓库 Wrapper，受限串行参数为 `--no-configuration-cache --console=plain --max-workers=1 -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process`。

| 命令或操作 | 实际结果 |
| --- | --- |
| `:composeApp:desktopTest`，过滤 `model.SyncPackageNamingTest`、`ui.SyncPackageFeedbackTest`、`ui.SyncPackageDialogTest`、`ui.PlaybackSettingsDialogTest`、`ui.PlayerRecoveryUiTest`、`sync.PlainSyncPackageTest`、`features.PlaylistArchiveFormatTest` | **通过**：20 项、0 失败／错误／跳过，新增命名 4 项；覆盖中文／全库／保留名／Unicode／重复序号，前轮 UI 与 MP3／FLAC／包校验回归同轮运行 |
| 上述回归联合 `:composeApp:assembleRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease` | **通过**：`BUILD SUCCESSFUL in 3m 19s`，134 tasks；Lint 0 Error / 107 Warning；此时尚未加入新目录文档任务 flags |
| 加入目录任务 flags 后重新构建三个 Android APK 与 `:composeApp:lintRelease` | **通过**：接续构建 `BUILD SUCCESSFUL in 56s`，123 tasks；Lint 仍 0 Error / 107 Warning。共享／桌面实现与前述 20 项通过时一致，不把未重跑的桌面测试说成本轮重跑 |
| 安装最终 Debug／测试 APK 后，`adb shell am instrument -w -r -e class com.resonance.player.AndroidExportLocationTest,com.resonance.player.SyncPackageFeedbackDeviceTest,com.resonance.player.SyncPackageDialogDeviceTest,com.resonance.player.PlaybackSettingsDialogDeviceTest,com.resonance.player.PlayerDialogsDeviceTest com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | **通过**：最终 `OK (6 tests)`，12.545s；固定目录 URI、拒绝任意位置、system component、文档任务 flags、无 URI 读写授予、结果回退及三类播放器／导出弹窗均覆盖。初次未加任务 flags 时五项也通过，但不能替代最终六项 |
| 最终独立 APK `apksigner verify --verbose`、`zipalign -c -P 16 4`、`aapt dump badging` | **通过**：v2=true，1 signer、v1=false；16KB 对齐退出码 0；正式包 `com.resonance.player`，0.6.6 / 40 |
| 最终正式包安装与三轮重复目录跳转、命名／保留文件校验 | **通过**：`adb install -r` 保留应用数据；设备实际安装 APK SHA-256 与最终产物一致。8 首真实歌单导出为「歌单名 (3).resonance」，32,764,403 字节，完成提示与实际文件名一致。先前无序号和 `(2)` 的两个包 SHA-256 均保持原值，未覆盖。连续三次先把系统文件管理器移到父目录，再从完成提示点击「打开所在目录」，每次都直接显示 `Download > Resonance` 面包屑、当前包名及其余导出文件，无应用选择器；复制位置／完成关闭通过，无新 ANR。结构化结果为 `final-real-result.json`；最终完成提示、目录实机截图已实际查看 |

早期真机验证如实保留：第一个歌单包成功使用歌单名，第二次成功使用 `(2)`。原脚本先假定系统浏览器一次返回即可回应用，断言失败；改为显式回应用后，又发现重复跳转时浏览器复用父目录的真实问题。因此没有按「Intent 启动成功」判为完成，补了新文档任务 flags 后重新构建／回归。中途续接请求打断过一次构建，未把不完整日志当作成功，接续 Wrapper 后才得到上述最终构建结果。荣耀安装身份验证由用户在手机上完成，没有读取密码或更改手机安全设置。

日志、截图、UI XML、测试报告和源码摘要保存在忽略目录 `build/export-names-directory-2026-11-04/`；真实曲库、设备信息和音频没有提交到源码。前轮反馈包接续验证在 `build/export-feedback-2026-11-04/`，不可替代本轮新包验证。

### 本次最终独立安装包

- [Resonance-0.6.6-export-names-directory-final-2026-11-04.apk](../../release/Resonance-0.6.6-export-names-directory-final-2026-11-04.apk)
- 大小：167,402,349 字节，约 159.65 MiB。
- SHA-256：`5fa5a104b4f2e46099af64519efac7d203dc46e94d99728d64f7633286f68e0c`，从本次实际文件计算；[校验文件](../../release/Resonance-0.6.6-export-names-directory-final-2026-11-04.apk.sha256)。
- 未上传、未公开发布、不升版；本次新名称不应用于用户以前导出的时间戳文件。

## 限制与后续

本次已完成原 Android 16 手机上的正式包覆盖更新、真实小歌单导出、重名文件保留及重复目录定位验收。为验证生成的三份约 32.76 MB 包保留在用户下载目录，没有删除旧时间戳包、重命名既有包或改动原歌曲。没有在最终包进行整库／超大包长时导出、Android 26–28／其他 ROM、Windows／Linux 可见窗口实测；全库名称和跨平台命名边界有共享回归，桌面实现在本轮通过编译。系统没有可用目录浏览器时继续显示保存路径和复制回退，不请求更大权限。

未重新执行全量 Kotlin 测试、网络同步或官网部署／发布。后续其他设备若系统文件管理器不处理目录 VIEW，应先检查匹配的系统组件与实际面包屑，不以启动 Intent 返回成功替代目录验证。

## 同步更新的文档

[文档入口与变更索引](../README.md)、[开发说明第 9 与第 14 节](../DEVELOPMENT.md#sync)；[前轮反馈记录](2026-11-04-18-export-location-feedback.md)保留早期断连事实，并补充接续验证入口。
