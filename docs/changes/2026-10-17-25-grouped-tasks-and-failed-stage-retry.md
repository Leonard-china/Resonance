# 2026-10-17 — 按一次操作汇总任务并批量重试失败媒体步骤

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

任务页原来把每个下载、封面和歌词步骤平铺；大批导入时难以判断总进度，也要逐条点重试。

## 原因与依据

MusicTaskEntry 和任务 JSON 缺少父级 ID。原 activeTasks 栈也不适合并发封面／歌词步骤的父级归属。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [MusicTaskGroups.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicTaskGroups.kt) | `groupMusicTasks / MusicTaskGroup.failures` | 构建操作分组，兼容旧记录、父级缺失和循环；恢复成功的来源尝试保留诊断但不再要求重试。 |
| [MusicTaskJournal.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicTaskJournal.kt) | `begin / entries` | 持久化 parentId，保留活跃父链并继续限定最近 100 条。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `step / retryFailedTasks / retryRecordedTask` | 以协程 ThreadLocal 上下文跟踪并发步骤；逐首记录截图导入进度；批量重试排除收费歌词、去掉被选父项覆盖的子项。 |
| [MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt) | `MusicTasksScreen` | 操作卡显示总进度和异常，详情折叠，支持仅重试失败项；收费歌词仍独立提示。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 按步骤平铺，缺少操作总览，只能逐项重试。 | 按导入／推荐等操作分组，显示逐首完成数；展开错误，批量继续失败的免费媒体步骤。 |

## 平台与数据影响

任务 JSON 增加可选 parentId，旧记录独立显示。继续保留最近 100 条边界，长批次仍可能淘汰早期子步骤。既有任务互斥、取消、原子持久化保留；不会自动重放聊天、删除或收费识别。来源与歌曲请求加入可重试 payload，不存凭据。

## 验证

MusicTaskGroupsTest 验证多层分组、旧孤立记录、循环与已恢复来源；MusicAssistantTest 验证封面批量重试不重复下载音频、历史重启与收费步骤拒绝。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

分组不是完整无限历史；只覆盖保留的 100 条记录。手动媒体重试仍依赖来源有效，新的网络错误会继续显示。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

