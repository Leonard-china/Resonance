# 2026-10-17 — 每日推荐增加独立点赞、踩和评论并参与下次选曲

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：点赞表示不错但未到收藏，评论也供后续推荐参考；[推荐历史/歌单](2026-10-17-06-recommendation-history-playlists-reasons.md)。

## 需求或问题

用户需要独立于收藏的轻度正反馈，以及踩和评论；这些应影响后续选曲。原“不喜欢并删除”会删除文件及歌单关系，不适合普通踩操作；也没有独立的轻偏好持久记录。

## 原因与依据

原推荐偏好主要基于收藏/自建歌单，dislikes记录对应物理删除。临时推荐清理后若仅依附Track ID，用户反馈可能丢失，需按歌曲身份独立保存。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [RecommendationFeedback.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/RecommendationFeedback.kt)、MusicFeatureModels.kt | RecommendationVote / RecommendationFeedback / recommendationFeedback | Neutral/Like/Dislike互斥投票、独立评论与时间戳，进入助手状态 |
| [MusicAssistantServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt)、[JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | feedbackFor / setRecommendationVote / saveRecommendationComment / recommendInternal | 原子存储、同曲多文件共享反馈；普通投票不删文件/不收藏/不触发模型；推荐过滤踩过原曲并排序相似候选 |
| [RecommendationFeedbackPolicy.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/RecommendationFeedbackPolicy.kt) | Codec / seeds / score / summary | 持久编解码；收藏种子权重高于点赞；同歌手/专辑与显式评论词规则；AI仅取最近24项/评论160字符 |
| [RecommendationPolicy.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/RecommendationPolicy.kt) | reason | 同歌手点赞证据可用于推荐理由，不凭评论编造风格事实 |
| [MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt) | RecommendationsScreen | 卡片独立点赞/踩（可撤销）、本地评论编辑/清空删除；FlowRow适配窄屏；收藏与删除单独标示 |
| [RecommendationFeedbackPolicyTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/RecommendationFeedbackPolicyTest.kt)、MusicAssistantTest.kt | 反馈回归 | 同曲身份、撤销保留评论、轻偏好权重、评论信号、摘要上限、不改音频/收藏、不发模型及换批后保留 |

## 修改前后行为

| 场景 | 修改前 | 修改后（待运行验证） |
| --- | --- | --- |
| 觉得不错，暂不收藏 | 只有收藏表达正反馈 | 点赞增加相似候选偏好，不保留音频；再点撤销 |
| 普通负反馈 | 原不喜欢会物理删除 | 卡片“踩”只改推荐偏好，可撤销；歌曲信息中删除仍单独提供 |
| 写评价 | 无独立评论 | 最多500字符，可编辑/留空删除；随反馈保存，换批清理后仍在 |
| 下一次推荐 | 主要用保存曲目 | 普通模式用加权种子/元数据与评论词排序；AI模式同时参考限长反馈摘要 |

## 平台与数据影响

music-assistant JSON新增recommendationFeedback，以NFKC歌手+歌名为键，字段title/artist/album/vote/comment/updatedAt；旧配置缺省空记录。互斥投票只改vote，修改评论只改comment，两者均清空才删记录。同曲不同Track ID共享反馈；不会随音频清理删偏好，不进入歌单文件。点赞、踩、评论本身不发模型/不转码/不改变收藏或音频所有权。普通踩过原曲不再入选，相关歌手/专辑降权但不整位封禁。

评论在本机存储；用户显式选AI精选或使用模型助手时，最近24条限长摘要可作为偏好数据发送所选模型，评论不是可执行指令。普通歌源模式不增加模型Token，最多四个搜索种子内给显式评论风格词留位置。支持的本地词包括抒情/轻快/摇滚/民谣/电子/爵士/古典/纯音乐/乐器/语言等固定集合及有限否定识别，不声称完整理解自由评论。原“不喜欢并删除”及其原因也写入反馈，原删除授权/行为保留；显式点赞可撤销旧同曲拒绝记录。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| git -c core.safecrlf=false diff --check -- composeApp docs | Windows | 通过 |
| 新增反馈策略与服务回归 | desktopTest源码 | 已编写，未运行 |
| 双端编译、反馈重启/去重、真实源排序、卡片窄屏 | Android / Windows | 未执行，按用户要求交Antigravity |

## 限制与后续

评论是个人本机偏好，不是公开社交评论。当前从仍在推荐页的曲目编辑，没有独立全历史反馈管理页。规则只用明确元数据与有限关键词；复杂否定、隐含风格或跨歌手相似度应由用户选AI模式参考，不能保证每次满足。历史标签混乱仍可能身份匹配失败。源选择与结果数量受插件能力约束。详见 [交接](../HANDOFF-2026-10-17.md)。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/THIRD_PARTY.md、docs/HANDOFF-2026-10-17.md。
