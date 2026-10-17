# 2026-10-17 — 音乐助手新增播放导航工具与不消耗模型的内置帮助

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：让AI能操作更多播放器功能并指导不熟悉软件的用户。

## 需求或问题

原助手可以管理曲库/歌单与下载，但不能完整控制播放或跳转相关页面；普通使用问题也会发送模型，回答可能与真实控件不一致。

## 原因与依据

JVM助手不应直接持有Compose或平台播放器，需通过受限动作队列交给前台执行；教程已有真实内容，可直接检索回答，减少模型调用。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [AssistantHelp.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/tutorial/AssistantHelp.kt) | entries / search / isHelpQuestion | 复用TutorialCatalog与新功能说明，常见帮助本地回答，提供页面建议 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | chat / runTool / queueAction / libraryContext | 新增帮助/教程/导航/播放暂停/进度/上下曲/来源/空间查询/歌源搜索下载工具；上下文带页面与播放状态，曲库去重压缩 |
| [MusicAssistantServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt)、[MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | updateUiContext / acknowledgeAction / onAction | 共享受限动作契约 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | LaunchedEffect / 动作执行 | 顺序执行并确认，真实播放器/页面导航接线，离线曲目先按来源恢复 |
| [MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | builtInHelpDoesNotSendAnyModelRequest | 帮助0模型调用的夹具回归 |

## 修改前后行为

| 场景 | 修改前 | 修改后（待运行验证） |
| --- | --- | --- |
| “怎么把每日推荐加入歌单” | 发送模型、可能泛答 | 检索内置说明并提供页面入口，无模型请求 |
| 助手播放/跳页面 | 缺少动作链路 | 限定动作队列交前台执行；工具返回“已请求”，不冒称已播放完成 |
| 歌源下载工具 | 无 | 只能使用此前源搜索返回的真实ID与条目 |

## 平台与数据影响

动作仅在内存中存在、最多20项，不随重启重放，不进入任务重试或同步。教程与帮助不联网；普通工具聊天仍受现有模型预算约束。未开放shell、文件系统、任意URL或系统控制，原删除授权边界保留。UI状态不包含凭据；源脚本也不获得模型Key。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| git -c core.safecrlf=false diff --check | Windows | 通过 |
| 新增本地帮助0请求测试 | desktopTest 源码 | 已编写，未运行 |
| 最终双端编译、真实工具往返、动作失败/取消与后台前台切换 | Android / Windows | 未执行，按用户要求交给 Antigravity |

## 限制与后续

帮助检索为关键词规则，模糊提问未命中仍走模型。界面动作需要活跃前台，动作回执目前是队列确认，不是持久化执行结果；必须验证失效ID、重复执行、切后台后播放时序。详见 [交接](../HANDOFF-2026-10-17.md)。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/HANDOFF-2026-10-17.md。
