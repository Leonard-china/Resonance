# 2026-11-05 — 桌面支持音乐文件多选、文件夹与整窗拖入导入

- 修改日期：2026-11-05（Asia/Shanghai；实现开始于 11 月 4 日，跨日验证交付）
- 类型：Bug 修复 / 功能
- 适用版本：`0.6.6 / 40`；不修改版本号
- 影响平台：Linux、Windows；共享 App 导入完成处理
- 关联需求：用户反馈 Linux 不能导入音乐，要求从文件管理器直接拖入；Windows 尚未实测。

## 需求或问题

桌面导入原来只接受 `JFileChooser.DIRECTORIES_ONLY` 的单个文件夹，没有窗口拖放接收。用户选择单曲或拖入文件时没有导入入口。旧选择器使用空父窗口，Linux 窗口管理器下可能失去与主窗的关联；此项为源码发现的风险，不能据此断言所有 Linux 导入失败均由它造成。

## 原因与依据

`DesktopPlatformServices.importMusic → chooseFolderOnEdt → DesktopMusicScanner.scan(root)` 只能接收单个目录，`Main` 没有 drop target。扫描使用 `Files.walk`，单个目录访问异常会中断枚举；没有正常候选时返回空报告，也没有说明。此次增加入口并复用既有音频读取、转换、进度、取消、持久化和 UI 重载链路，不顺带修复历史审计中无关的同步包事务、离线记录或删除授权问题。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [桌面平台服务](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `chooseMusicOnEdt`、`importSelectedMusic`、`offerDroppedMusic`、`DesktopMusicScanner.scan` | 系统选择器接受多文件/目录，绑定真实主窗；拖放请求使用容量 1 的通道和 pending 标志，现有导入 CAS 防止并发；扫描统一接收路径列表，以真实路径去重并按文件隔离错误。 |
| [拖放接收](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopMusicDrop.kt) | `DesktopMusicDropHost`、`droppedMusicPaths` | 整窗 Compose drop target、释放提示和忙碌反馈；接受 Java 文件列表与本机 `text/uri-list`，正确解析编码的中文/空格/%/#，拒绝网络地址；URI 文本限制 1 MiB、条目限制 10,000；协商 Copy，避免文件管理器将导入解释为移动源文件。 |
| [桌面入口](../../composeApp/src/desktopMain/kotlin/com/resonance/player/Main.kt) | `Window`、`DisposableEffect` | 包装拖放宿主并管理选择器父窗生命周期。 |
| [平台契约](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | `externalMusicImports`、`importMusicPaths` | 默认空事件流及不支持实现；Android 继续使用已有权限感知的原生选择器。 |
| [App](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `runMusicImport` | 选择与拖入共用完成处理，先等待已接收持久化任务，后导入/重载/歌单匹配；启动加载或其他操作未结束时等待，成功后显示实际曲库。 |
| [工具页](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `ImportScreen` | 桌面说明可直接拖入，选择入口改为文件/文件夹；保留实际进度、取消和失败信息。 |
| [回归](../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/DesktopMusicImportTest.kt) | 6 项导入回归 | 真实合成 WAV、重复目录/文件、持久化重启、坏文件隔离、无音频提示、取消、Unicode/URI 与超大输入。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 导入一首或多首本地音乐 | 只能选文件夹 | 选择多个文件/文件夹，或直接拖进窗口 |
| 拖入目录与其子文件 | 无接收 | 同一真实路径只处理一次，递归目录但不追随子目录符号链接循环 |
| 部分文件损坏/目录不可读 | 扫描可能整体中断 | 保留健康文件的处理，报告失败数量/原因 |
| 没有支持的音频 | 空报告无说明 | 明确提示选择支持的音频或目录 |
| 取消导入 | 有既有取消链路 | 继续保留已完成项，清理转换中间文件并保留源文件 |

## 平台与数据影响

桌面共用 JVM 实现，Windows Java 文件列表与 Linux 文件列表/URI payload 均接入；各系统真实拖放需分别验证。Android 默认事件为空，原 SAF/MediaStore 权限不变。没有新增网络请求、配置键、同步格式或凭据。不删除、移动、改写输入音频；主动转换仍生成既有托管文件，原子保存、备份、播放任务代次和删除语义不变。原文件导入继续引用其本机路径，不承诺自动复制到托管目录。

## 验证

构建与测试使用配置好的 JDK17、SDK36 和系统 FFmpeg；日志及临时合成数据位于忽略的 `build/am-reference-2026-11-04/`，跨日验证完成于 11 月 5 日。

| 命令/操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:desktopTest --tests '*DesktopMusicImportTest' --tests '*DesktopManagedMp3ConverterTest'`（首轮同时执行 UI/歌词） | Linux JVM，真实合成 WAV/FFmpeg | 导入 6 项、转换 3 项通过；源文件哈希、错误隔离、取消与重复路径覆盖。首轮 UI 的旧文案断言失败单独保留在 `first-results/`。 |
| `:composeApp:desktopTest` 最终完整复测 | Linux JVM | **364 项：354 通过、10 跳过、0 失败/错误**；`final-desktop-tests.log`、`final-test-summary.json`、`final-results/`。 |
| 成品原生窗口 `DropTarget` 的 DragEnter/DragOver/Drop 事件注入 | 本机 Linux/Wayland 会话，隔离资料，合成中文/空格路径 WAV | **通过**：窗口上的真实接收器→共享事件通道→App→扫描→持久化→UI 重载，显示「已导入 1 首音乐」；Move 候选事件最终 `acceptDrop(Copy)`，`dropComplete=true`。证据 `native-smoke/result.txt` 和 `native-after-drop.png`。不是文件管理器鼠标手动验收。 |
| `sha256sum -c …/native-smoke/source-before.sha256` | 同一合成 WAV | **通过**，导入前后源字节不变。 |
| AWT Robot 真实鼠标拖动尝试 | 当前 Wayland 会话 | **环境受阻**：窗口可见、指针位置可读，但测试源未收到 mousePressed/mouseDragged，未发生源导出；不能计作文件管理器拖入通过。改用上行原生事件注入验证完整接收链路，保留此限制。 |
| `:composeApp:assembleRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease :composeApp:packageLinuxPortable`（附带定向 UI 回归） | Linux JDK17 / SDK36 | **通过**，`review-fixes-build.log`；Release Lint 0 Error / 107 Warning；Android 仅构建，无设备连接。 |

首次桌面编译曾因新增 API opt-in/重复导入缺失而失败，已修正。全量首轮另发现同名音量 slider/设置按钮的测试定位歧义，改为「播放音量」后定向及完整复测通过；失败不冒充通过。10 项跳过保留各夹具条件，不能视为原生桥、真实模型或生产外部服务已验证。

## 限制与后续

Windows 原生文件管理器和 Linux 文件管理器鼠标手动拖放尚未在本轮实测；Linux 成品已验证原生接收事件到入库/UI 更新的完整链路。当前 Linux 反馈没有提供失败文件/错误文本，修复可确认的入口和选择器窗口关联缺口，不能推断每一种编码/损坏音频都已支持。版本不变，不自动发布 GitHub/官网。

## 同步更新的文档

`docs/README.md` 变更索引；`docs/DEVELOPMENT.md` 第 6/14 节；Linux 使用说明。
