# 2026-10-17 — 新增推荐历史与个人反馈管理页

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

点赞、踩和评论会在清理音频后保留，但用户只能从当前推荐卡片编辑；误踩或想找回以前的推荐缺少入口。

## 原因与依据

原来只有哈希身份到时间的冷却历史，无法从哈希还原歌名；反馈数据也没有独立页面。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [RecommendationHistoryScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/RecommendationHistoryScreen.kt) | `RecommendationHistoryScreen` | 汇总推荐与反馈，按歌名／歌手／评论搜索，筛选点赞／踩／评论，撤销、编辑或重新查找。 |
| [MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt) | `RecommendationsScreen` | 增加历史入口，卡片显示临时与已保存状态，纠正要求先导入歌源的空页文案。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `restoreState / recommendInternal / writeRecommendationFeedback` | 保存可回看的历史名称和时间，365 天裁剪；撤销踩时同步清除旧拒绝记录，保留评论。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 清理后只能被动等待歌曲再次出现，不能集中管理历史反馈。 | 独立查看历史与反馈并重新查找；点赞仍独立于收藏，卡片明确提示如何保留音频。 |

## 平台与数据影响

JSON 新增 recommendationHistoryDetails，旧冷却时间表不变，旧反馈可直接查看。新详情从本次更新后积累；旧哈希无法恢复的歌名不虚构。个人反馈仍不进入歌单包，不发公共评论；普通编辑不调用模型。没有删除音频或改变收藏语义。

## 验证

MusicAssistantTest 覆盖反馈不改文件／收藏、不调用模型、撤销拒绝、换批清理后历史与评论保留；RecommendationFeedbackPolicyTest、RecommendationPolicyTest 一并回归。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

历史不是云端同步；旧版本仅有哈希的全部历史无法还原，需从升级后的推荐开始积累。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

