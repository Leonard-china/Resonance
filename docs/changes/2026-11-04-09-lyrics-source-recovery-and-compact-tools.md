# 2026-11-04 — 歌词查找接入真实歌源并收起空态工具

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复 / UI
- 适用版本：0.6.5 / 39；未升版
- 影响平台：Android、Windows、Linux 的共享 UI 与 JVM 歌词逻辑
- 关联需求：DeepSeek 从未成功提供歌词，提示“AI 暂时无法提供”；用户截图显示歌词工具拥挤、操作被裁切。

## 需求或问题

《夏天的风》／温岚／4:01 没有歌词，原“AI 查找歌词”实际调用模型直接生成原曲文本。截图同时显示大段实现说明、重复操作及被播放器控制区裁掉的底部按钮。

## 原因与依据

`App` 的旧 AI 按钮进入 `MusicFeatureController.generateTextLyrics`，最终是无搜索工具的文本 completion；`DeepSeekClient` 要求模型从记忆返回原词，模型返回 `status:unavailable` 就显示通用失败。这不是可靠的歌词检索链路，也不能据该消息推断 API Key 错误。自动歌词之前只查本地与 LRCLIB，没有调用已安装歌源的 `getLyric`。LRCLIB 的旧未命中缓存可阻断后续新来源。空态和翻译工具重复占据固定高度。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | `findLyrics` | 查询最多 8 个启用且支持歌词的歌源，单源 8 秒；严格核对歌名、歌手、版本和时长，只取歌词、不取音频；跳过前面空结果以使用后续有效来源 |
| [LrclibLyricsRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/LrclibLyricsRepository.kt) | `fetch`、`fetchOriginal`、`fetchRemote` | LRCLIB 未命中或失败后回退歌源；旧未命中缓存仍可尝试新来源；成功缓存复用；非法精确参数避免发送、HTTP 400 可继续搜索、未知歌手别名统一 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `builtinStartup`、`findSourceLyrics` | 等待内置源初始化后查词；Android/桌面 `PlatformServices.loadLyrics` 接入此回退 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 歌词自动加载、刷新 | 异常结束 Loading；刷新取消旧 AI 文本任务并清空旧结果，避免覆盖新查找 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricsStatusContent` | 空态仅一个“查找歌词”，工具默认收起；展开后移走重复空态、工具从顶部显示，整区可滚动；进度和错误通过工具／收起状态显示 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `TranslationControls` | 缩短常驻说明，保留 AI 额度和音频上传计费提示 |
| [DeepSeekClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ai/DeepSeekClient.kt) | `generateLyrics` | 模型拒绝明确说明当前模型未提供原词，指向歌源查找或音频识别，避免暗示盲目重试会成功 |
| [LyricsAcquisitionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/LyricsAcquisitionTest.kt)、[PlayerRecoveryUiTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerRecoveryUiTest.kt) | 检索/缓存/模型/布局回归 | 前源空、后源命中，错误歌手/时长排除；模型时间戳剥离；三种窗口及放大文字 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 无歌词时查找 | 用户以为在搜索，实际让模型回忆 | 查询真实歌词库与启用歌源，不自动调用模型 |
| 旧 LRCLIB 未命中缓存 | 新来源没有机会 | 可继续歌源回退并缓存成功结果 |
| 模型拒绝 | “查找失败”，反复重试 | 说明当前模型无法提供，建议可执行的其他入口 |
| 小屏和大字空态 | 重复工具、底部裁切 | 主操作保留，更多工具展开后可滚动 |

## 平台与数据影响

三平台共用同一匹配、缓存与 UI。保留本地/生成歌词优先、单曲偏移、纯文本不伪造时间轴及切歌代次保护。歌源查找仅发送歌曲元数据并取回已发布歌词，不上传音频、不读取模型 Key、不自动付费。原歌词缓存格式与音频不变。可选 AI 仍可能拒绝或错误；音频识别仍需原有明确上传/计费确认。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| Wrapper `:composeApp:desktopTest`（显式开启公网歌词验收） | Linux / JDK 17 | 通过：346 项，337 通过、9 项集成入口跳过；发生于最终歌词 UI 间距调整之前 |
| 最终 8 类针对性测试 + `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease` | 最后源码，Linux / Android SDK 36 | 通过：33 项全部通过、0 跳过；Debug／测试 APK 成功，Lint 0 Error／107 Warning |
| 原生公共歌源查所报歌曲 | JVM / QuickJS / 真实网络 | 通过：《夏天的风》／温岚，QQ 来源 46 行同步歌词；未保存原词 |
| 模拟模型 unavailable／ok、缓存恢复、匹配与原生布局 | 最终针对性测试 | 通过；没有伪造同步时间，前源空仍使用后源；小屏／大字／横屏截图已核对 |
| DeepSeek 真实生成、Android 真机歌词显示、Windows 窗口 | 本轮 | 未执行；未读取 Key，设备已断开 |

完整命令、跳过名单、原生截图、最后源码摘要、产物校验与早期失败的处理见[本轮验证报告](../audit/2026-11-04/follow-up/VALIDATION.md)。

## 限制与后续

本次不使用真实模型 Key，不把模拟模型返回当作 DeepSeek 真实服务成功；已改变主查找链路，不能承诺任何模型总能生成原词。公开服务覆盖、同名版本及网络随环境变化。Android 真机已断开，未执行最终设备歌词显示验收；Windows 未运行。本轮公网联测只保存来源、行数及同步标志，不保存原词。

## 同步更新的文档

[文档索引](../README.md)、[开发说明第 8、11 节](../DEVELOPMENT.md#lyrics)、[第三方请求说明](../THIRD_PARTY.md)。歌源新增和 VPN DNS 兼容另见 [12 号记录](2026-11-04-12-more-builtin-sources-and-vpn-dns.md)。
