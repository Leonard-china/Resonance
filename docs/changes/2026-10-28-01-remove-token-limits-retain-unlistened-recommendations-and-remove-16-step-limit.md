# 2026-10-28 — 取消 AI Token 使用量限制、每日推荐未听歌曲到期保留不换、取消助手 16 轮工具步骤限制

- 修改日期：2026-10-28（Asia/Shanghai）
- 类型：功能 / 体验优化
- 适用版本：0.6.2 / 36（未改动版本号）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：用户明确要求“取消所有有关token使用量的限制，每日推荐的歌曲如果还没听过就不换，即使到时间了，取消16次步骤限制”

## 需求或问题

1. **取消 Token 使用量限制**：此前系统在执行音乐助手对话、歌词翻译、歌单 OCR、每日推荐等 AI 请求前，会估算当前输入与输出并校验月度预算（`check(used + estimatedInput + maxTokens < config.monthlyTokenLimit)`）。当本月 Token 用量接近或超出设置的上限时，会直接抛出 `IllegalStateException("本次请求预计超过本月 AI Token 预算；可在设置调整上限")` 拦截请求，阻断用户正常使用。
2. **每日推荐未听歌曲到期保留不换**：此前当到达推荐更新间隔（如 24 小时）后，自动定时调度或前台到期检查会无条件重新生成一批推荐，并将旧批次中未被用户收藏且未加入个人歌单的推荐歌曲自动物理删除或替换移出。如果用户未能在当天及时收听，错过的推荐歌曲就会被永久刷新清除。
3. **取消 16 次步骤限制**：此前音乐助手在处理复杂的批量管理指令时，工具调用循环被固定限制在 `repeat(16)`（最多 16 轮）。当模型需要连续执行搜索、建歌单、批量添加歌曲等多步动作超过 16 轮时，循环会提前终止并强制输出“本次已达到 16 轮操作上限”，导致后续步骤未完成。

## 原因与依据

1. **Token 限制机制**：[`JvmMusicAssistant.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) 的 `ask` 函数中包含硬编码的月度预算校验，设置页还提供了“每月 AI Token 上限”输入框。用户需要取消该限制，使得请求不再受任何 Token 上限阻拦，仅保留后台统计作为信息参考。
2. **推荐到期检查与收听状态判定**：[`PlaybackStateStore`](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) 与 [`ListeningStatsStore.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsStore.kt) 已经精确记录每首歌曲的播放时长与完播次数（`totalSeconds`、`playCount`）。但此前推荐检查 [`JvmMusicAssistant.kt:recommend`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) 仅校验了时间间隔（`now - createdAt < intervalHours * 3_600_000L`）和网络环境，未检查用户是否实际听过本批推荐曲目。
3. **步骤轮数固定限制**：[`JvmMusicAssistant.kt:chat`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) 中使用固定循环 `repeat(16)` 控制大模型工具调用交互。当模型单次对话需要较多步操作时，受制于 16 轮硬限制无法自动闭环。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`JvmMusicAssistant.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `JvmMusicAssistant.ask` | 移除 `check(used + estimatedInput + maxTokens < config.monthlyTokenLimit)` 预算拦截，将默认单次最大生成 token 提升至 8192，取消所有有关 token 使用量的限制；继续按月记录 Token 输入/缓存/输出统计 |
| [`JvmMusicAssistant.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `JvmMusicAssistant.chat` | 移除 `repeat(16)` 轮数限制和 16 轮中断提示，改用 `while (!finished)` 循环配合协程状态感知（`currentCoroutineContext().ensureActive()`），工具调用持续执行至模型明确结束或任务取消 |
| [`JvmMusicAssistant.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `JvmMusicAssistant.recommend` / `hasUnlistenedRecommendations` | 新增 `hasListened` 与 `hasUnlistenedRecommendations` 判定：当推荐批次中存在未听过（收听时长与播放次数均为 0、且无点赞/点踩/评论反馈）的歌曲时，自动定时或到期检查（`!force`）直接跳过刷新，保留原推荐不换 |
| [`MusicLibraryAccess.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicLibraryAccess.kt) | `MusicLibraryAccess.hasListened` | 平台接口增加 `fun hasListened(trackId: String): Boolean = false` 契约，使核心层能直接获知歌曲播放统计 |
| [`AndroidMusicAccess.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidMusicAccess.kt) | `AndroidMusicAccess.hasListened` | 实现 Android 端根据 `ListeningStatsStore` 判定歌曲播放时长是否大于 0 或已完播 |
| [`DesktopMusicAccess.kt`](../../composeApp/src/desktopMain/kotlin/com/resonance/player/features/DesktopMusicAccess.kt) | `DesktopMusicAccess.hasListened` | 实现 Desktop 端根据 `ListeningStatsStore` 判定歌曲播放时长是否大于 0 或已完播 |
| [`RecommendationScheduleStatus.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/RecommendationScheduleStatus.kt) | `RecommendationScheduleStatus` | 当本批推荐歌曲尚有未听过的曲目且已到期时，状态明确展示「本批推荐尚有歌曲未听过，保留不换」 |
| [`MusicFeatureScreens.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `MusicFeatureSettingsSection` | 移除已失效的“每月 AI Token 上限”设置输入框，更新为无限制说明提示 |
| [`MusicAssistantTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | 测试套件 | 更新预算拦截测试为无限制验证，新增未听推荐到期保留不换单元测试与超过 16 步工具调用测试 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| AI Token 使用达到或超出预算 | 抛出异常中断请求：“本次请求预计超过本月 AI Token 预算；可在设置调整上限” | 请求正常发起与完成，不设任何使用量上限，仅在助手状态中记录输入/输出统计 |
| 到达推荐间隔（如 24 小时后），用户尚未收听推荐歌曲 | 自动调度或启动时直接请求新一批候选，清空/替换未收藏的上批推荐 | 检查发现本批推荐尚有未听歌曲，自动跳过更新，完整保留当前推荐列表等待收听；设置页显示“本批推荐尚有歌曲未听过，保留不换” |
| 手动点击“更新推荐” | 立即联网生成新一批推荐 | 保持显式手动更新能力（`force = true`），用户主动要求时仍可强制换批 |
| 音乐助手单次会话工具调用超过 16 步 | 触发第 16 轮强制停止，输出“本次已达到 16 轮操作上限” | 使用循环持续执行直到任务全部完成（测试覆盖 20 步连续建歌单与执行无阻断） |

## 平台与数据影响

- **共享层**：`JvmMusicAssistant`、`MusicLibraryAccess`、UI 设置页及状态组件逻辑统一，双端表现完全一致。
- **Android / Windows**：双端各自在 `AndroidMusicAccess` 与 `DesktopMusicAccess` 中无缝接通已有的 `ListeningStatsStore` 单曲播放记录；未听过定义为 `totalSeconds <= 0 && playCount <= 0`。
- **存储与兼容性**：不改变历史存储格式；`MusicFeatureSettings` 中仍保留数据字段兼容旧配置；`listening-stats.json` 无需迁移；任务日志与推荐去重历史完全向后兼容。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `./gradlew desktopTest` | Windows 11, OpenJDK 17, Gradle 8.11.1 | **通过**：跨平台全量 198 项单元与集成测试全部通过，包括新增的未听推荐保留测试、超过 16 步工具调用测试与 Token 无限制测试 |
| `tokenUsageRestrictionsAreCancelledAndDoNotRejectRequests` | Desktop JVM 单测 | **通过**：模拟 `monthlyTokenLimit = 10_000` 并发送超过 20,000 字符的长请求，请求不再被抛出异常拦截，成功调用模型并返回 |
| `recommendationRetainedWhenUnlistenedEvenIfIntervalExpired` | Desktop JVM 单测 | **通过**：模拟推荐生成时间超过 48 小时，歌曲未听时 `recommend(false)` 完整保留推荐；将歌曲标记为已听后再调用即成功换批 |
| `chatToolExecutionExceeds16StepsWithoutLimit` | Desktop JVM 单测 | **通过**：连续排队 20 轮 `create_playlist` 工具调用，成功执行全部 20 步建歌单并正常结束，未提示 16 轮上限 |

## 限制与后续

- 自动推荐保留机制基于单曲是否产生播放统计（`ListeningStatsStore`）或用户明确互动（点赞、点踩、本地短评）；若用户已主动对推荐歌曲进行点踩，该歌曲已被移除，不阻塞后续换批。
- 本次范围内无已知未完成项。

## 同步更新的文档

- [`docs/DEVELOPMENT.md`](../DEVELOPMENT.md)：在第 15 节工具调用说明中更新取消 16 轮限制说明，在 Token 预算说明中更新取消使用量限制说明，在推荐生命周期中增加未听保护机制与状态说明。
- [`docs/README.md`](../README.md)：变更记录索引追加 `2026-10-28-01` 项。
