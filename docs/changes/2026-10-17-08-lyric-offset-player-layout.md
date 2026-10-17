# 2026-10-17 — 持久化单曲歌词校准并调整手机播放器与歌词布局

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / Bug 修复 / UI
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：部分歌词在人声前滚动，优先优化手机播放器及歌词页。

## 需求或问题

用户反馈推荐下载的歌词提前；播放器里歌词偏移设置未形成独立持久化链路。MV对白/不同剪辑与歌词版本不一致是可能原因，未拿用户音频验证，不能认定所有提前都由同一原因造成。

## 原因与依据

歌词与下载音频来源可能不同；显示前奏没有明确状态，偏移刷新可能丢失。原播放器把音量、变速等设置持续展开，手机歌词空间被挤占；标题与歌词行存在影响阅读的位移动画。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [LyricOffsetStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/LyricOffsetStore.kt) | read / write / apply | 按Track ID原子保存本机偏移，限制±10分钟，覆盖不同来源歌词刷新 |
| [LrclibLyricsRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/lyrics/LrclibLyricsRepository.kt)、[JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | fetch / saveLyricOffset / cachedSynchronizedLyrics | 本地/在线/生成歌词统一应用校准；歌源歌词优先使用同一歌曲条目 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 歌词偏移回调 | 更新显示、原文和翻译状态，持久写入服务 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | NowPlayingOverlay / lyrics controls | 前奏等待人声，明确“歌词提前了/落后了”方向及复位；固定标题、降低歌词运动，翻译层级；歌词/队列页把播放设置收进弹窗；来源详情替代固定320K标识 |

## 修改前后行为

| 场景 | 修改前 | 修改后（待运行验证） |
| --- | --- | --- |
| 歌词提前 | 不易判断偏移方向，刷新可能丢失 | 点击“歌词提前了”使歌词延后，偏移持久化，可复位 |
| 首句时间未到 | 容易把预览当已开唱 | 显示“前奏 · 等待人声” |
| 手机歌词页 | 设置挤占空间、行缩放/标题移动 | 次要设置折叠，双行标题稳定，保留阅读空间 |

## 平台与数据影响

共享Compose界面与状态，两端使用同一JVM偏移存储 lyrics-cache/offsets/<TrackID hash>.json。只写应用缓存，不改用户 LRC/KRC/音频标签，不进入歌单同步包。实际编解码/播放任务防竞争沿用原有逻辑。本文 UI 工作参考本仓库 emil-design-eng 技能，控制装饰性运动，保留原生界面。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| git -c core.safecrlf=false diff --check | Windows | 通过 |
| 最终双端编译、手机窄屏/长标题/双语、前奏/拖动/切歌/重启偏移 | Android / Windows | 未执行，按用户要求交给 Antigravity |

用户指令前最后一次编译缺 collectAsState 导入已补；补后未复编，不能认定最终编译通过。

## 限制与后续

固定偏移只能修正整体提前/落后，不能修复现场/对白剪辑导致的不同时间轴或渐进漂移；需更换对应版本或用户显式生成音频时间戳歌词。未进行视觉截图或设备交互验证。详见 [交接](../HANDOFF-2026-10-17.md)。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/HANDOFF-2026-10-17.md。
