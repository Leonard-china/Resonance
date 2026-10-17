# 2026-10-17 — 全链路功能验证、手势与反馈修复及双端Release构建

> 依据 `AGENTS.md`、`docs/AI-WORKFLOW.md` 及 `docs/HANDOFF-2026-10-17.md`，对 05~09 及追加开发 11~13 变更开展完整验证、必要修复、全量回归和两端 Release 构建。

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：Bug 修复 / 验证 / 测试与构建配置
- 适用版本：0.4.6 / 29（保持稳定，不自动升版）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：
  - [2026-10-17-05-music-sources-studio-provenance.md](2026-10-17-05-music-sources-studio-provenance.md)
  - [2026-10-17-06-recommendation-history-playlists-reasons.md](2026-10-17-06-recommendation-history-playlists-reasons.md)
  - [2026-10-17-07-managed-download-storage.md](2026-10-17-07-managed-download-storage.md)
  - [2026-10-17-08-lyric-offset-player-layout.md](2026-10-17-08-lyric-offset-player-layout.md)
  - [2026-10-17-09-assistant-tools-local-help.md](2026-10-17-09-assistant-tools-local-help.md)
  - [2026-10-17-11-horizontal-page-swipe.md](2026-10-17-11-horizontal-page-swipe.md)
  - [2026-10-17-12-recommendation-votes-and-comments.md](2026-10-17-12-recommendation-votes-and-comments.md)
  - [2026-10-17-13-about-version-highlights.md](2026-10-17-13-about-version-highlights.md)
  - [HANDOFF-2026-10-17.md](../HANDOFF-2026-10-17.md)

## 需求或问题

在接手工作区后，05~09 变更及 11~13 追加变更（歌源解析、推荐去重/历史/赞踩/评论、下载存储管理/回滚、歌词校准/窄屏适配、助手播放/导航工具/本地帮助、手势滑动换页、关于页版本高亮）已写入代码，但尚未经过完整编译与回归测试。
在初步编译与回归执行中暴露出以下问题：
1. **QuickJS 全局作用域重定义冲突**：`MusicSourceRegistry.kt` 在运行时 SHIM 中直接使用全局函数声明 `function axios(...)`、`function fetch(...)`、`function require(...)`。当第三方 MusicFree 插件脚本顶部包含 `const axios = require('axios')` 时，QuickJS 报语法错误 `SyntaxError: invalid redefinition of global identifier` 导致插件脚本无法执行。
2. **测试挂钩时序漂移**：在 `MusicAssistantTest.kt` 中，`downloadAudio` 取消与步骤日志测试原先以 `artworkSource` 作为“音频转码入库后”的取消拦截点。由于此前改动将专辑封面查找提前至转码前以便将封面打入 MP3 ID3 标签，导致拦截点在入库前触发，导致断言失败。
3. **编译警告**：`TrackSourceDetails.kt` 与 `MusicTasksScreen.kt` 存在对非空 smart-cast 值的多余安全调用（`?.`）警告。
4. **验证缺口**：需要针对手势冲突与边界阻尼、推荐历史跨重启持久化与冷却衰减、存储删除失败（Windows 文件锁）回滚保护、歌词正负偏移持久化、版本高亮动态生成等场景补全自动化回归，并完成双端 Release 产物构建。

## 原因与依据

1. **QuickJS 规范限制**：在 ECMAScript 规范中，词法作用域变量声明（`const` / `let`）在同一个执行上下文中不能覆盖顶层的 `var` 或顶层函数声明。在 `MusicSourceRegistry.kt` 将内置辅助函数绑定至 `globalThis`（`global.axios = ...`，`global.require = ...`），既能让插件脚本以属性方式全局访问，又允许脚本以 `const axios = require('axios')` 声明局部或顶层常量而不触发命名冲突。
2. **下载与标签嵌入流水线变更**：为了实现下载 MP3 带有原生内嵌封面，`JvmMusicAssistant.kt` 的流水线为：解析候选 -> 查询封面 -> 转码入库并写标签 -> 查询/下载对齐歌词。因此音频落盘与入库在封面获取之后、歌词获取之前发生。在 `MusicAssistantTest` 中将取消挂钩移至 `lyricSource`，能精确模拟音频入库后、后处理过程中的取消行为。
3. **手势防越界依据**：页面左右滑动（`HorizontalPageSwipe`）必须限制在 `[0, totalTabs - 1]` 之间，左滑（下一页）不能超出最大索引，右滑（上一页）不能低于 0，不发生循环回卷，避免与边缘手势及内部滚动冲突。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | `MusicSourceRegistry.SHIM` | 将内置 SHIM 函数（`axios`, `fetch`, `require`, `crypto`, `qs`, `btoa`, `atob`）由顶层函数定义改为注入 `globalThis`，解决第三方插件使用 `const axios` 重定义报错 |
| [MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | `MusicAssistantTest` | 将取消/步骤日志测试中的挂起拦截点从 `artworkSource` 调整至 `lyricSource`，匹配封面提前查询后的实际入库流水线 |
| [TrackSourceDetails.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TrackSourceDetails.kt) | `TrackSourceDetailsDialog` | 清理非空 Smart-cast 变量上的多余 `?.` 安全调用警告 |
| [MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt) | `MusicTasksScreen` | 清理 Smart-cast 安全调用警告 |
| [HorizontalPageSwipeUnitTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/HorizontalPageSwipeUnitTest.kt) | `HorizontalPageSwipeUnitTest` | 新增针对水平滑动方向判断、距离与速度阈值（50dp/400dp/s）、首尾页不回环及 `BuildVersion.kt` 5项更新亮点的单元测试 |
| [StorageAndAssistantExtraTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/StorageAndAssistantExtraTest.kt) | `StorageAndAssistantExtraTest` | 新增对 Windows 文件占用删除失败回滚、推荐历史持久化与 7 天冷却过滤、助手播放/导航工具执行与无效歌曲 ID 防御、歌词偏移双向调节（±500ms）与范围限制持久化（[-600s, 600s]）的自动化回归测试 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| MusicFree 插件加载（含 `const axios = require('axios')`） | QuickJS 执行 SHIM 与脚本时抛出 `SyntaxError: invalid redefinition of global identifier` | 顺利完成初始化并导出插件 `search` / `getMediaSource` 方法 |
| 音频下载与步骤中断测试 | 拦截点提前导致转码入库尚未发生，断言失败 | 在真实入库后拦截歌词阶段，正确验证步骤日志持久化与重试机制 |
| 页面水平滑动手势到首末页 | 缺乏独立单元测试覆盖 | 单元测试验证第一页左推、末页右推均拦截返回，不循环回环 |
| 文件被占用删除失败 | 缺少破坏性场景安全测试 | 测试锁定目标文件时安全捕获并抛出具名异常，不损坏原文件和曲库状态 |
| 推荐历史冷却跨进程 | 缺少重启恢复测试 | 测试重启后重新载入 JSON，7 天内歌曲均正确过滤，超出后恢复入选 |

## 平台与数据影响

- **跨平台共享逻辑**：`MusicSourceRegistry`、`HorizontalPageSwipe`、`RecommendationFeedback`、`LyricOffsetStore`、`ManagedMusicStorage` 在 Desktop 和 Android 行为统一。
- **持久化数据**：
  - 歌词偏移：`lyric-offsets.json` 记录每首歌的偏移量（毫秒），平滑加载。
  - 推荐历史：`recommendation-history.json` 保持 7 天冷却窗口。
  - 赞踩反馈：`recommendation-feedback.json` 存储用户点赞、点踩和标签理由。
  - 来源信息：`media-origins.json` 保存音频和封面的来源溯源。
- **构建产物**：构建 Android Release APK 与 Windows 安装包 EXE，不改变版本号（保持 `0.4.6 / 29`）。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:compileKotlinDesktop` | Windows 11, JDK 17.0.12 | 通过：Desktop 编译无错误 |
| `:composeApp:compileDebugKotlinAndroid` | Windows 11, Android SDK 36, JDK 17 | 通过：Android 编译无错误 |
| `:composeApp:generateAppVersion` | Windows 11, Gradle 8.11.1 | 通过：成功生成 `BuildVersion.kt`，含 5 项重点发布更新 |
| `:composeApp:desktopTest` | Windows 11, JVM | 通过：全部 40+ 测试类（含音频、歌词、手势、推荐、存储、AI助手）100% 通过 |
| `:composeApp:allTests` | Windows 11, JVM | 通过：所有通用与桌面测试全部通过 |
| `website/.venv/Scripts/python.exe -m unittest discover -s tests` | Windows 11, Python 3.12 | 通过：全量 42 个网站与论坛测试全部通过 |
| `:composeApp:lintRelease` | Windows 11, Android Lint 9.4.1 | 通过：无阻断错误，报告写入 `build/reports/lint-results-release.html` |
| `:composeApp:assembleRelease` | Windows 11, Gradle 8.11.1 | 通过：生成签名 `Resonance-0.4.6-release.apk`（166,657,788 字节，SHA-256: `6e1a0567c2a90a8174a82d48bee74464a226ece2f0eee29000026928c82477f9`） |
| `:composeApp:packageReleaseExe` | Windows 11, Gradle 8.11.1, Compose 1.11.0 | 通过：生成安装包 `Resonance-0.4.6.exe`（320,786,432 字节，SHA-256: `78bf66c9370c5c10be2ae5d7364ad15efd40105a8b243de8e2fe1e9a35092d28`） |
| `androidInstrumentedTest` | Windows 11（无连接的 Android 设备或模拟器） | 未执行（因环境限制：本机 ADB 无在线真机或模拟器） |

## 限制与后续

1. **Android 设备回归限制**：当前开发宿主机未启动 Android 模拟器且无连接的物理手机，Instrumentation UI 自动化测试（`HorizontalPageSwipeTest`、`LibraryNavigationTest`）已编译通过但未在真机执行。后续如需真机验证，可连接设备后执行 `./gradlew :composeApp:connectedDebugAndroidTest`。
2. **正式发布**：本次仅完成本地工作区全链路回归与 Release 产物构建验证，严格遵循“不自行升版或发布”的要求。

## 同步更新的文档

- [docs/README.md](../README.md)：新增本变更记录索引，更新待验证状态标记。
- [docs/DEVELOPMENT.md](../DEVELOPMENT.md)：更新第 17 节待验证状态，标记已由 Antigravity 完成全链路验证和双端构建。
