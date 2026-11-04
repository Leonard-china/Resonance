# 2026-11-01 — 明确 AI 直接生成歌词文本并区分音频时间轴识别

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：歌词功能与交互修正
- 适用版本：0.6.5 / 39，未升版
- 影响平台：共享 Compose 界面、Android、Windows 共用 AI 实现
- 关联：[歌词跟随与手动浏览](2026-10-29-02-lyrics-centered-follow-and-manual-browsing.md)

## 需求或问题

维护者提供模型直接返回歌词文本的截图，要求不必让模型先搜索。原 `requestAiLyrics` 底层已通过不带工具的 `completeText` 直接请求模型，但界面称“AI 查找歌词”，而“生成同步歌词”实际上上传音频至语音识别服务，两种行为容易混淆。普通加载时也没有直接生成入口。

## 原因与依据

调用链为 `App` → 两端 `PlatformServices.requestAiLyrics` → `DeepSeekClient.generateLyrics` → `JvmMusicAssistant.completeText` → 不带工具的 `MusicAiClient`。该路径不执行 LRCLIB 或网页搜索。另发现批量补全将模型返回的 LRC 外观内容直接标记为同步歌词，不具备音频对齐依据。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [DeepSeekClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ai/DeepSeekClient.kt) | `generateLyrics`、`parseGeneratedLyrics` | 直接要求当前模型返回原文歌词行 JSON，不传搜索工具；使用歌曲元数据作为数据；处理无法提供、纯音乐及旧纯文本/LRC 结果，保留顺序与重复，统一去掉时间戳并标明 AI 生成、未经核验 |
| [MetadataEnricher.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/MetadataEnricher.kt) | AI 歌词补全 | 复用文本解析，不再把模型输出的时间戳当作已验证同步时间轴 |
| [MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | `generateTextLyrics`、`cancelTextLyrics` | 独立生成状态和错误提示；防重复请求；切歌取消并核对请求代次，防止迟到结果覆盖当前歌曲；失败保留原歌词，成功更新并清除旧译文 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | AI 请求和切歌 | 统一走控制器，切歌时取消；同步缓存加载与新文本请求也检查代次，避免缓存结果反盖新生成结果 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt)、[NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `TranslationControls`、歌词入口 | 明确分为“AI 生成歌词文本”和“音频识别同步歌词”；加载中和无歌词时也可主动生成；提示当前模型用量、不上传音频，展示文本来源 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | `requestAiLyrics` | 注释明确直接生成文本的契约，不改接口签名 |

## 修改前后行为

| 场景 | 修改前 | 修改后（源码设计，未运行验收） |
| --- | --- | --- |
| 点击 AI 歌词 | 直接请求模型，但界面称“查找” | 明确调用当前模型生成原文文本，不执行网页搜索 |
| 自动歌词仍在加载 | 通常只能等待 | 可以主动选择 AI 生成文本 |
| 模型返回带时间戳文本 | 独立入口去掉时间戳，批量补全却可能误当同步 | 两条路径统一按普通文本处理，不提供未经对齐的滚动时间轴 |
| 切歌后旧请求完成或失败 | 旧失败可能覆盖另一首歌的状态 | 取消并核对请求代次及曲目，失败保留已有歌词 |

## 平台与数据影响

生成/翻译忙状态在调度任务前设置，避免同一帧重复发起；翻译结果也核对歌词请求世代，避免旧译文在新 AI 原文生成后覆盖界面。尚未运行竞争复现。

沿用两端当前音乐助手模型配置和既有用量计数；仅主动点击才调用，发送曲名、歌手、专辑与时长，不发送音频，也不添加搜索服务。结果使用既有歌词缓存格式，自动加载仍按本地/缓存/免费源路径进行，不自动付费。没有新增密钥或读取真实凭据。已存在的本地歌词和已识别同步歌词仍遵循原有加载优先级；本次不清理或迁移旧缓存。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 阅读两端请求、无工具文本请求、批量补全与显示链路 | 当前工作区 | 已静态核对调用关系，不能替代编译或运行 |
| 构建、测试、真实模型调用和 UI 验收 | Android / Windows | 按用户明确要求未执行；未使用用户 API Key |

## 限制与后续

模型不保证知道某首歌或返回准确完整的原词；无法提供、拒答或格式不完整时显示失败并保留原歌词。模型知识和纯文本输出不能证明与具体录音版本匹配；本功能不生成真实时间轴，若需要逐句同步仍应通过单独、明确的音频识别流程。生成入口、取消、缓存优先级、返回格式与旧数据均尚未编译或运行验证。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md` 歌词章节与 `docs/THIRD_PARTY.md` 请求边界。
