# 2026-10-17 — 每日推荐按歌曲去重、持久冷却并支持保存到歌单

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：昨日推荐不再出现、重复 MP3 不重复计偏好、推荐曲目加入自建歌单。

## 需求或问题

重复文件会让推荐摘要误表述同一歌手有多首歌曲；旧批删除后可能再次推荐昨天的歌；推荐页缺少方便的入歌单操作。用户希望普通推荐尽量不用模型。

## 原因与依据

原摘要按曲库文件/Track 条目计数，理由来自模型自由生成；只过滤当前曲库与拒绝，没有独立的推荐历史。歌单关系能力与推荐页操作未形成完整链路。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [RecommendationPolicy.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/RecommendationPolicy.kt) | identity / unique / recentlyRecommended / reason | NFKC歌手+歌名去重，保留版本文字，滚动冷却与基于真实保存歌曲的理由 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | recommendInternal / saveToPlaylist / libraryContext | 默认歌源模式、可选 AI；偏好排除临时推荐；成功提交历史，至少一首成功才替换批次；摘要最多120首 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | RecommendationsScreen / 推荐设置 | 单首、多选、全选保存到已有或新建歌单；日期、冷却设置、理由展开和来源信息 |
| [RecommendationPolicyTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/RecommendationPolicyTest.kt)、[MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | 推荐回归 | 重复文件计一次、冷却/理由/配置兼容、旧推荐删后不重推、保存后保留 |

## 修改前后行为

| 场景 | 修改前 | 修改后（待运行验证） |
| --- | --- | --- |
| 同歌8个文件 | 可能按8条统计并被模型编造风格理由 | 按歌手歌名计1首，理由只引用实际保存记录 |
| 昨日曲目已被替换删除 | 可能再次入选 | 历史独立保存，默认7天不重推，可设1–365天 |
| 推荐加入歌单 | 操作不完整 | 选择已有歌单或新建，支持多选；保存后换批保留 |

## 平台与数据影响

music-assistant JSON 新增 recommendationHistory（歌曲身份→时间，保存365天）、批次 createdDate；旧批初始化历史。设置新增 recommendationMode（缺省 Source）、recommendationCooldownDays（缺省7）。普通歌源推荐无需模型 Key/Token，但需要可用歌源；AI模式仍计预算。未改同步包格式，已有收藏/自建歌单保留保护。源候选/下载全失败保留旧批与时间。历史按本机时间，显示日期按 Asia/Shanghai。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| git -c core.safecrlf=false diff --check | Windows | 通过 |
| 新增及调整推荐回归测试 | desktopTest 源码 | 已编写，未运行；旧 AI 夹具显式设 Ai 模式 |
| 最终双端编译、跨日/重启、UI点击保存 | Android / Windows | 未执行，按用户要求交给 Antigravity |

## 限制与后续

歌曲身份仍依赖可靠的歌名/歌手元数据，标签混乱可能漏合并或误合并；不做音频指纹。歌源推荐目前取已保存歌手候选或源榜单，未实现成熟的协同推荐。不通过模型编造数量或风格。详见 [交接](../HANDOFF-2026-10-17.md)。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/HANDOFF-2026-10-17.md。
