# 2026-11-05 — 修复 Linux 歌单包不可见与拖入后未导入

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：`0.6.6 / 40`，不改变版本号
- 影响平台：Linux、Windows 共用桌面导入；共享导入结果与界面刷新
- 关联需求：用户反馈 Linux 选择文件夹找不到 `.resonance` 包，也无法拖入导入；前轮[桌面音频导入](2026-11-05-01-desktop-files-and-drop-import.md)。

## 需求或问题

普通「导入音乐」的文件选择器默认只显示音频扩展名，`.resonance` 不可见。窗口虽然已接收文件管理器拖放，但后续 `DesktopMusicScanner` 只枚举音频，拖入歌单包或扫描包含包的目录时忽略归档。专用「歌单导入与导出 → 导入歌单」已有解包能力，普通选择与拖放没有接通。

## 原因与依据

`chooseMusicOnEdt` 的过滤器和 `DesktopMusicScanner.scan` 的候选扩展名均仅包含 `DESKTOP_AUDIO_EXTENSIONS`。`App.runMusicImport` 导入后原先仅从内存旧歌单重匹配并保存；直接在扫描器恢复歌单还会被这次保存覆盖，因此需同步携带导入歌单结果并重载已经合并的歌单。专用歌单选择器原先没有绑定主窗，本次一并对齐现有普通选择器的父窗和 EDT 调用方式。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [桌面平台服务](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `desktopMusicImportFilter`、`chooseMusicOnEdt`、`DesktopMusicScanner.scan` | 普通选择器显示 `.resonance`，含大小写扩展名；文件、递归目录和拖放共用候选枚举与真实路径去重。包分支调用既有 `PlainSyncPackage.import`，保留包内音频格式、元数据、ID 与歌单关系；单个坏包隔离并报告。 |
| 同上 | `DesktopLibraryStore.mergeImported`、`importSelectedMusic`、`importSyncPackage`、`chooseSyncPackageOnEdt` | 音轨和歌单按 ID 合并，复用原有原子属性文件与备份；专用歌单选择器绑定主窗，并支持从 EDT 直接调用。 |
| [共享模型](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | `ImportReport` | 增加默认空的 `playlists` 和默认零的 `importedPackageCount`，仅作为本次导入的内存结果。 |
| [应用入口](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `runMusicImport`、`importReportMessage`、歌单导入确认 | 有歌单包时重载已合并歌单，再匹配和保存，空歌单也刷新；成功提示音乐与歌单数量，转换入口不会声称包内音频被转码。普通导入与歌单任务互斥等待，专用导入前等待已排队持久化。 |
| [拖放宿主](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopMusicDrop.kt)、[工具页](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `DesktopMusicDropHost`、`ImportScreen` | 桌面说明明确支持 `.resonance` 歌单包。 |
| [新增回归](../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/DesktopSyncPackageImportTest.kt)、[结果文案回归](../../composeApp/src/commonTest/kotlin/com/resonance/player/ImportReportMessageTest.kt) | 选择过滤、包往返、URI/目录去重、校验失败、路径越界、取消、空歌单 | 使用临时目录和合成音频验证导入和重启后的关系；不使用真实用户歌曲。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 普通导入选择 `.resonance` | 默认音频过滤器不显示 | 与音频一起可见、可选择 |
| 拖入包或扫描其所在目录 | 包被扫描器忽略 | 校验、恢复音轨与歌单并刷新曲库 |
| 目录与同一包重叠选择 | 不处理包 | 同一真实路径只解包一次 |
| 经转换入口选择歌单包 | 不处理包 | 恢复原包格式，不将归档交给 FFmpeg |
| 坏包与健康文件混合 | 包不参与导入 | 报告失败项，继续健康文件；复用现有包校验与路径约束 |

## 平台与数据影响

Linux 和 Windows 共用桌面实现；Windows 原生文件管理器仍需独立验收。Android 保留原 SAF 选择和现有解包流程，新增结果字段使用默认值；共享任务等待与文案兼容原音频导入。不改变 `resonance-sync-1`、JSON/属性文件格式、配置键、权限、网络或凭据。输入包和音频不移动、不删除、不写标签；包内文件仍按既有规则复制到托管音乐目录，同 ID 目标仍可能替换，不宣称新增事务或永不覆盖。取消在扫描、普通音频处理和包与包之间生效；现有 `PlainSyncPackage` 单包解压没有新增中途取消能力。

## 验证

本次日志与隔离资料位于忽略目录 `build/resonance-package-import-2026-11-05/`。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 初次 `bash ./gradlew …` | Linux | **失败**：仓库 Wrapper 脚本含 CRLF，Bash 解析失败；后续使用同一个 Wrapper JAR 的 Java 入口，不修改脚本。 |
| 首次 Wrapper JAR 定向回归 | Linux JDK 17 | **中断，不能算通过**：系统内存耗尽，内核日志记录 Java 被 OOM killer 终止；后续重新执行。 |
| Wrapper JAR `:composeApp:desktopTest --tests '*DesktopSyncPackageImportTest' --tests '*DesktopMusicImportTest' --tests '*PlainSyncPackageTest' --tests '*ImportReportMessageTest' --tests '*DesktopManagedMp3ConverterTest'` | Linux JDK 17 / FFmpeg | **通过，25 项，0 失败/错误/跳过**；`targeted-tests-retry.log`、`targeted-summary.json`、`targeted-results/`。 |
| `:composeApp:compileDebugKotlinAndroid :composeApp:packageLinuxPortable` | Linux | **失败/环境受阻**：编译 Android 与 ProGuard 打包期间再次发生全机 OOM，未完成；不宣称 Android 编译或 Release 便携包通过。用户指出内存问题后不再重复这套构建。 |
| 限额服务内 `:composeApp:createDistributable --max-workers=1 --no-daemon --offline` | Linux JDK 17，整个进程树限制 2 GiB | **通过**，31 秒；systemd 记录整个服务峰值 **1.5 GiB**。`safe-linux-build.log`。产物为包含完整运行时的普通 app image，未跑 ProGuard，不冒充失败的 Release 便携包。 |
| 完整 app image 的原生 `DropTarget` URI 列表事件注入 | Linux/Wayland，隔离数据，中文/空格/%/# 包路径；服务限制 1 GiB，JVM 384 MiB | **通过**，真实窗口接收 → 通道 → App → 校验解包 → 保存 → 重载；显示「已导入 1 首音乐、1 个歌单」与测试歌单，等待 3.5 秒后歌单仍保存，`sourceAction=Copy`、`dropComplete=true`；峰值 **280.6 MiB**。`native/result.txt`、`playlist-result.txt`、`native-after-drop.png`。这是原生事件注入，未冒充文件管理器物理鼠标拖放。 |
| `sha256sum -c …/native/package-before.sha256` | 同一合成歌单包 | **通过**，导入后输入包字节不变。 |
| `git -c core.whitespace=cr-at-eol diff --check` | 当前含既有未提交修改的工作区 | **通过**；按仓库 CRLF 行尾检查，未重格式化用户原有改动。 |

资源纠正：本机 16 GiB RAM，约 512 MiB swap 已满；项目默认 Gradle `-Xmx4g` 与 Kotlin daemon `-Xmx3g`，还会另启打包进程，不能将这些独立上限当作整个构建的预算。本次后续仅用已通过回归的桌面源码构建 Linux 完整 app image，不再同时编译 Android/跑 ProGuard。使用独立 systemd 用户服务限制整个进程树：`MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0`；Gradle `--max-workers=1 --no-daemon`、`-Xmx768m`、`MaxMetaspaceSize=384m`、Kotlin `in-process`。限额已通过 `systemctl --user show` 核实；本次命令覆盖，不修改仓库或全局 Gradle 默认配置。

## 限制与后续

不将 Windows 文件管理器鼠标拖放、Android 实机或真实用户歌单包列为已验证。版本保持 `0.6.6 / 40`，本次不发布 GitHub/官网。历史同步导入事务等审计项没有在本需求中顺带修复。

## 同步更新的文档

`docs/README.md` 变更索引，`docs/DEVELOPMENT.md` 第 6/9 节，`docs/LINUX.md` 导入说明。
