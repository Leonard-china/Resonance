# 2026-10-16 — 修复静止歌词并添加按音频时间戳生成同步歌词

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.0 / 23，本次按构建规则不自动升版
- 影响平台：共享、Android、Windows

## 需求、原因与依据

部分 B 站歌曲歌词完全不动：来源可能只有纯文本，预览却只显示第一句；LRCLIB 精确命中纯文本后提前返回；播放界面又用元数据时长估算毫秒位置。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [lyrics/LrclibLyricsRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/LrclibLyricsRepository.kt) | fetchRemote / usableTimeline / fetch | 精确纯文本后继续检索同步版本；检索失败保留已得到文本；拒绝差距超过 max(8秒,4%) 的搜索版本；零时间轴或超出曲长的时间轴降级纯文本；优先读取已生成的持久同步歌词。 |
| [lyrics/ParaformerLyricsClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/ParaformerLyricsClient.kt) | SpeechLyricsTransport / recognize / parseSentence | OkHttp 4.12.0 WSS，按官方 run-task→task-started→二进制 PCM→finish-task 协议；只接纳最终句/词时间戳；限制单首 30 分钟，禁止携凭据重定向，连接/结果超时、取消断连；不使用聊天模型猜时间。 |
| [lyrics/AudioLyricAlignment.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/AudioLyricAlignment.kt) | fromWords / align | 从词的真实起止时间按停顿、标点、行长分句；已有歌词用字符序列对齐到识别词，整体至少70%且逐行至少一半命中才使用原文本；低匹配回退音频识别文本，绝不均分时间。 |
| [lyrics/GeneratedLyricsStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/GeneratedLyricsStore.kt) | read / write | 原子保存生成歌词与原文备份；按歌曲ID、来源、标题歌手和时长指纹复用，不给每次播放付费。 |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | generateLyricsInternal | 独立百炼语音 Key 或复用千问 Key；上传前按 PCM 实际时长检查/预留月秒数预算，失败/取消也保守计入；结果不覆盖原歌词。 |
| [features/MusicFeatureCodec.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicFeatureCodec.kt) | settings / encode | 新增 speechApiKey（原有凭据加密）、speechUrl、monthlySpeechSeconds，默认月60分钟；旧设置兼容。 |
| [platform/PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | playbackPositionMs | 两端提供原生当前毫秒位置，PlayerState.positionMs 用于播放器与悬浮歌词，不依赖缺失的元数据时长。 |
| [ui/NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | LyricsContent / HeroSyncedLyricsPreview | 显式显示同步/纯文本状态；纯文本可滚动，预览不再将第一句伪装成同步；播放菜单和歌词页都有生成入口与上传/计费说明。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 只有纯文本 | 预览长期停在第一句 | 标识纯文本并可浏览/生成同步 |
| 元数据时长未准备 | 毫秒推算为0 | 读取原生播放位置 |
| 未找到同步歌词 | 只能获取文本 | 显式音频识别生成，保存后免费复用本机结果 |

## 平台与数据影响

两端在应用私有speech-staging目录生成并清理临时音频；Android content来源读取上限200MB。FFmpeg生成单声道16 kHz、16-bit PCM 临时文件；Android 支持先读取 content URI。仅主动生成时上传整首音频到所配置百炼兼容语音接口；不申请麦克风权限。Android和共享路径调用均使用API26可用的Paths.get，移除Path.of的API34依赖；设备回归覆盖API32。语音计费与聊天 Token 分离；取消/失败可能被服务计费，保守预留秒数不代表供应商账单。自动导入/推荐不会触发付费音频识别。

## 验证

本次实际构建、测试、签名及产物结果统一见 [构建验证记录](2026-10-16-08-release-build-verification.md)。相关测试已随本次修改加入源码；不将旧版结果用作本次通过证据。

## 限制与后续

AudioLyricAlignmentTest、LrclibLyricsRepositoryTest、FloatingLyricsTest 及 MusicAssistantTest 覆盖真实词时间戳、重复副歌、低匹配回退、缓存、预算提前拒绝、原生位置。Android MusicFeaturesIntegrationTest 实际 FFmpegKit 输出 PCM。无用户语音 Key，未调用收费识别服务；人声与伴奏混合、翻唱/方言可能识别有误。

## 同步更新的文档

已更新 [开发说明](../DEVELOPMENT.md)、[文档索引](../README.md)、[本次发布版构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md)。
