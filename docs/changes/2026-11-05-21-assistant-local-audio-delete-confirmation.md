# 2026-11-05 — 助手删除绑定具体音频并强制本机确认

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q04。

## 需求或问题

询问如何删除、解释风险或否定删除也可能获得delete_tracks授权；模型工具直接执行真实删除。

## 原因与依据

关键词只能表达意图，不能代替具体文件确认。新增内存中的删除提案；工具只返回awaiting_user_confirmation，本机按钮才调用物理删除入口。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [model/MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | AudioDeletionRequest / pendingAudioDeletion | 绑定提案ID、曲目/来源快照、会话和时间；不写入历史持久格式。 |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | allowsAudioDeletion / delete_tracks / confirmAudioDeletion | 询问/解释/否定拦截；提案单次消费、10分钟期限、会话与来源复核；逐项记录实际结果。 |
| [ui/AssistantAudioDeletionDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AssistantAudioDeletionDialog.kt) | AssistantDeletionPrompt | 列出具体歌名、歌手和文件名，取消不删，确认后刷新曲库。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | AssistantDeletionPrompt | 只在子组合观察助手状态，避免聊天进度重组整个App。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q04所述输入/操作 | 模型一轮工具即可删文件 | 模型只能提出最多300首提案；本机确认后执行；取消/重放/来源变更/会话切换拒绝 |

## 平台与数据影响

共享逻辑与原生双端界面；没有新增API请求、收费确认或持久权限。关闭/重启应用不会恢复旧提案。确认校验ID/来源，不声称检测同一路径被外部替换的字节内容。

## 验证

本次相关用例：MusicAssistantTest授权/具体文件/取消/重放/来源变更；AssistantDeletionConfirmationDeviceTest。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：MusicAssistantTest、AssistantDeletionConfirmationDeviceTest | 45通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

自然语言门禁采取保守判定，可能要求用户换成明确命令；实际用户AI账号/收费接口未调用。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
