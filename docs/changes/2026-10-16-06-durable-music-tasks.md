# 2026-10-16 — 新增持久任务列表和按失败步骤重试

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.0 / 23，本次按构建规则不自动升版
- 影响平台：共享、Android、Windows

## 需求、原因与依据

聊天、截图导入、推荐、下载及后续封面/歌词处理原来只有一个临时状态，离开页面后难以了解失败发生在哪一步。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [features/MusicTaskJournal.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicTaskJournal.kt) | MusicTaskJournal | 在现有原子 JSON 中保存最近 100 个步骤的标题、状态、阶段、进度摘要、时间及重试参数；重启将遗留 Running 标为 Interrupted，不自动执行。高频下载进度仅在 StateFlow 更新，阶段变化才落盘。 |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | task / step / retryTask / recordPendingRecommendation | 根任务和每首下载/封面/歌词/同步生成分别记账；失败或取消保留已入库歌曲；重试封面/歌词仅执行相应步骤。推荐音频入库时立即写 pending，防止取消后成为无法自动清理的孤立临时歌曲。 |
| [ui/MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt) | MusicTasksScreen | 音乐库与助手均可打开任务页；显示进行中/已完成/失败/取消/上次中断，提供取消与单步骤重试；收费同步歌词重试再次提示上传与费用。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 封面失败 | 临时错误易丢失 | 独立失败记录与重试 |
| 应用退出 | 进度不可追踪 | 保留步骤并标明上次被中断 |
| 下载后取消后处理 | 推荐 pending 可能来不及写 | 立即登记已入库推荐，后续成功批可按原规则清理 |

## 平台与数据影响

沿用同数据根 Mutex、前台/Worker 共享状态、JSON 备份恢复；不保存 Key 到任务 payload。不自动重放聊天/删除等有副作用工具；仅提供确定的媒体步骤重试。最多 100 条是步骤数量，嵌套任务各占一条。

## 验证

本次实际构建、测试、签名及产物结果统一见 [构建验证记录](2026-10-16-08-release-build-verification.md)。相关测试已随本次修改加入源码；不将旧版结果用作本次通过证据。

## 限制与后续

MusicAssistantTest 覆盖持久恢复、封面阶段重试和后处理取消后的推荐 pending 清理；系统杀进程仍需用户手动重试。

## 同步更新的文档

已更新 [开发说明](../DEVELOPMENT.md)、[文档索引](../README.md)、[本次发布版构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md)。
