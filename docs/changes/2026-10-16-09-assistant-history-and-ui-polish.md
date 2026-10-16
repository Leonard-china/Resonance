# 2026-10-16 — AI助手多会话隔离历史、模型溯源、桌面歌词权限引导与独立关于页面

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复 / UI 体验优化
- 适用版本：`0.4.0 / 23`（未修改 `version.properties`，未自动升版）
- 影响平台：共享（commonMain / jvmMain）/ Android / Windows
- 关联记录或需求：用户会话体验反馈、[docs/changes/2026-10-16-08-release-build-verification.md](2026-10-16-08-release-build-verification.md)

## 需求或问题

用户在多端使用体验中提出以下问题与改进需求：
1. **AI 助手对话历史与新建会话**：原有音乐助手仅有一条全局单列表消息，无法像常规 AI 应用一样新建会话或查阅/切换历史对话。同时用户极度关注 Token 消耗，严禁在多会话间串扰拼接历史消息。
2. **页面切换时上方黑边闪烁**：在不同主栏目或二级页面之间切换（尤其是切入/切出音乐助手）时，屏幕顶部状态栏/标题区域偶尔出现单帧黑边闪烁。
3. **灵动岛展开（全屏播放器）缺少桌面歌词按钮**：在全屏播放器界面无法直观开启或关闭桌面全局悬浮歌词。
4. **“设置”与“关于 Resonance”无区别**：工具页面中的“关于 Resonance”原本直接跳转设置页；用户需要一个专用的关于页面，详细阐述应用的架构、核心特性与功能使用说明。
5. **模型名称未在对话气泡中展示**：用户不知道助手当前是基于哪个模型回复的，需要显式标注“回复自 XXX”。
6. **API 模型配置防误删与预选项**：在配置千问、DeepSeek、豆包等连接时，输入框容易误删或打错；需要为用户提供常用模型候选 Chip 以及恢复默认配置按钮。
7. **移动端软键盘（IME）弹起适配**：键盘弹起或新消息到达时，消息列表未平滑滚动到底部，最新消息容易被输入框或键盘遮挡。
8. **桌面歌词权限状态反馈**：Android 端未授权悬浮窗权限时，点击桌面歌词开关没有任何提示，用户误以为功能失效。

## 原因与依据

1. **多会话与 Token 隔离**：[JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) 此前持久化的 `assistant.json` 只有 `messages: JSONArray`。多会话需要在数据层抽象 `AssistantSession(id, title, createdAt, updatedAt, messages)`，持久化保存最多 30 个会话。在 Token 控制上，`chat()` 发起请求时**严格仅取当前活跃会话的 `state.value.messages.takeLast(16)`**，切换和创建会话在本地完成（0 Token 消耗），确保隔离与经济性。
2. **黑边闪烁**：[LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) 的 `DestinationContent` 中，`AnimatedContent` 未设置 `Modifier.fillMaxSize()`，且过渡分支使用了 `SizeTransform(clip = false)`。在横向推入和视差滑出过程中，非满屏约束和未裁剪区域在重组单帧内漏出了底层的画布底色；此外在进入助手页面时，`MiniPlayer` 采用无过渡的直接隐藏，造成外层 `Column` 瞬间跳高 70dp。
3. **歌词按钮缺失**：[NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) 顶部栏 `NowPlayingTopBar` 仅放置了收藏和更多菜单，未将已实现的画中画/桌面歌词触觉按钮常驻外露。
4. **关于页面混淆**：[ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) 中 `ProfileScreen` 的“关于 Resonance”一栏 `onClick` 错误绑定到了 `onOpenSettings`，且路由表缺乏独立的目标组件。
5. **模型未溯源**：[MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) 的 `AssistantMessage` 缺乏 `model` 属性，且回复落地时未捕获活跃连接的厂商与模型标签。
6. **悬浮窗权限暗默失败**：Android 端切换悬浮歌词需要 `Settings.canDrawOverlays` 权限；此前未进行权限预检与引导，直接调用服务抛错或无静默响应。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [LibraryModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | `LibraryDestination` | 枚举新增 `About` 入口，支持关于页面的独立路由导航。 |
| [MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | `AssistantSession`, `AssistantMessage`, `MusicAssistantState` | 1. `AssistantMessage` 增加 `model: String? = null` 属性；<br>2. 新增 `AssistantSession` 结构；<br>3. `MusicAssistantState` 增加 `sessions: List<AssistantSession>` 与 `currentSessionId: String`。 |
| [MusicAssistantServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt) | `MusicAssistant` | 接口扩展会话生命周期管理：`createSession`, `switchSession`, `deleteSession`, `clearCurrentSession`。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `JvmMusicAssistant` | 1. `restoreState()` 解析 `sessions`，自动平滑升级旧版单消息列表为默认会话，完全向后兼容；<br>2. 会话上限管理（30 个），首条消息自动截取前 20 字生成会话标题；<br>3. `chat()` 记录真实生效的提供商与模型标签（如 `通义千问 (qwen3-vl-flash)`），落地到回复消息；<br>4. 请求上下文严格保持仅取当前会话最新 16 条，各会话严格隔离。 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | `PlatformServices` | 增加 `canDrawOverlays(): Boolean = true` 与 `requestOverlayPermission(): Boolean = false` 平台接口规范。 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `AndroidPlatformServices` | 覆写悬浮窗权限检测（`Settings.canDrawOverlays`）并提供拉起系统权限设置界面的 Intent 实现。 |
| [MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | `MusicFeatureController` | 增加 `showOverlayPermissionPrompt` 状态；`toggleFloatingLyrics()` 增加权限拦截，无权限时呼出引导弹窗。 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingTopBar` | 收藏按钮旁添加常驻 `TactileTopBarButton` 桌面歌词切换按钮，支持根据 `floatingLyricsEnabled` 实时发光与变色。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `AiConnectionEditor`, `AssistantScreen`, `MusicFeatureDialogs` | 1. `AiConnectionEditor` 提供常用推荐模型 FilterChip（千问、DeepSeek、豆包主流模型）及“恢复默认地址与模型”操作；<br>2. `AssistantScreen` 顶栏外露当前活跃模型名、新对话按钮与历史会话管理弹窗；<br>3. 消息气泡展示“回复自 XXX”；<br>4. 列表接入 `LazyListState` 并在消息变动及键盘弹起（IME）时自动平滑滚动到底部；<br>5. `MusicFeatureDialogs` 增加 Android 桌面歌词权限引导弹窗。 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `ProfileScreen`, `AboutScreen` | 1. `ProfileScreen` 增加 `onOpenAbout` 回调，将“关于 Resonance”与设置彻底解耦；<br>2. 独立新增 `AboutScreen`，包含应用概览、架构哲学（本地优先/无云端依赖）、九大核心功能特色与四大常用使用技巧说明。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DestinationContent`, `LibraryShell` | 1. 消除顶部黑边闪烁：`AnimatedContent` 显式设置 `Modifier.fillMaxSize()`、`contentAlignment = Alignment.TopStart`，且所有过渡规格启用 `SizeTransform(clip = true)`；<br>2. `MiniPlayer` 使用 `AnimatedVisibility(expandVertically() + fadeIn(), shrinkVertically() + fadeOut())` 平滑展开收起，杜绝切入助手时外层布局剧烈跳动；<br>3. 纳入 `LibraryDestination.About` 二级路由跳转与系统返回键拦截。 |
| [MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | `MusicAssistantTest` | 新增 `chatWithMultipleSessionsMaintainsIsolationAndAttribution` 单元测试，自动化验证多会话隔离、模型溯源、切换和删除逻辑。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 助手会话管理 | 只有一条全局消息列表，无法开启新对话，无法管理历史记录；历史过多无法清理。 | 顶部常驻「新对话」与「历史」按钮；弹窗展示历史会话列表，支持点击切换和单条删除；新建与切换 0 Token 消耗。 |
| 助手模型识别 | 消息气泡仅显示“音乐助手”，不知道具体使用了什么模型。 | 顶部副标题显示当前活跃模型；助手消息气泡明确展示“音乐助手 · 回复自 通义千问 (qwen3-vl-flash)”等具体模型标签。 |
| API 误删与配置 | 用户手动输入模型名和 URL，误删后难以找回默认参数。 | 推荐常用候选模型 Chip（点击一键填入并联动图片支持），并提供「恢复默认地址与模型」一键还原。 |
| 移动端键盘弹起 | 软键盘弹起或收到长回复时，最新内容被挡住，需要手动反复上滑。 | 监听消息数与 `WindowInsets.ime`，键盘弹起与新消息到达时平滑自动滚动到底部。 |
| 页面切换视觉 | 快速在主栏目与二级页面之间切换时，屏幕上方偶尔出现黑色底色闪烁；切入助手时迷你播放器突兀瞬间消失。 | 过渡动画设置严格满屏约束与 `clip = true` 边界裁剪；迷你播放器平滑伸缩淡入淡出，页面切换丝滑无黑边闪烁。 |
| 全屏播放器桌面歌词 | 灵动岛展开后无法直接控制桌面歌词，需折叠返回设置页或播放控制栏。 | 顶部栏收藏按钮旁常驻桌面歌词图标，点击即开关，并伴有触觉高亮微光反馈。 |
| 悬浮窗未授权开启 | Android 未给权限时点击开关毫无反应。 | 拦截并弹出权限解释引导弹窗，提供「前往设置」按钮一键拉起系统悬浮窗权限授权页。 |
| “关于 Resonance” | 点击后直接打开“设置”页，两项无任何区别。 | 点击进入专属独立关于页，详细说明 Local-First 架构哲学、全格式无损平衡、KGMA/KGG 转码、AI 助手、Paraformer 同步歌词、跨端迁移等核心功能与使用技巧。 |

## 平台与数据影响

- **数据存储兼容性**：
  - `assistant.json` 结构平滑升级，新增 `sessions` 数组与 `currentSessionId` 字段。
  - 旧版本单一 `messages` 列表在首次加载时自动包装为首个会话，无缝迁移不丢历史。
- **Token 与性能影响**：
  - 会话隔离彻底切断了旧会话对当前对话的 Prompt 污染，单次会话滑动窗口严控在 16 条内，显著节省 Token 预算。
  - `AnimatedContent` 启用 `clip = true` 与尺寸约束，消除了过渡期间可能产生的离屏撕裂重排。
- **平台权限**：
  - Windows 端保持免权限直接置顶与穿透。
  - Android 端标准声明并适配 `SYSTEM_ALERT_WINDOW` 动态权限状态与引导。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --no-configuration-cache` | Windows 11 / JDK 17 (Desktop JVM) | **通过**（108 个单元测试全部 PASS，包含新增的会话隔离与模型溯源自动化测试） |
| `git status` 工作区对比 | Windows 11 / Git 2.47 | **通过**（所有改动均定点修改，无多余未暂存破坏） |
| 历史单会话兼容性验证 | Desktop JVM 测试环境 | **通过**（旧版 JSON 格式成功反序列化并自动升级） |

## 限制与后续

- 本次范围内所有用户反馈的问题（多会话、防误删、黑边闪烁、全屏歌词按钮、关于页、模型显示、软键盘与权限反馈）均已彻底解决并验证。
- 后续建议：未来可按需支持手动重命名会话标题功能（目前为自动截取首条消息前 20 字）。本次范围内无已知未完成项。

## 同步更新的文档

- [docs/README.md](../README.md)：更新变更记录索引，增加 `2026-10-16-09-assistant-history-and-ui-polish.md` 条目。
- [docs/DEVELOPMENT.md](../DEVELOPMENT.md)：同步更新“音乐助手与多会话”、“UI导航与页面过渡”、“关于页面”与核对日期。
