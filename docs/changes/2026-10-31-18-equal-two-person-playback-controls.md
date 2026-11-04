# 2026-10-31 — 新版双人房间的共享播放操作免审批

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：功能 / 协议兼容调整
- 适用版本：0.6.5 / 39，未升版
- 影响平台：Android、Windows 共享房间协议与播放 UI
- 关联：[多人平权需求](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)

## 需求或问题

维护者不希望房主拥有播放特权。当前客户端只能申请暂停、继续、下一首或选歌，等待房主批准；播放页的上一首和拖动进度不能控制共享播放。

## 原因与依据

`JvmListenTogetherService.hostPeer` 将请求写入房主审批列表，`App` 又阻止客端通过播放器入口操作。新旧客户端需要协商能力，避免新客户端向旧房主发送其不认识的命令导致断连。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `TogetherControl`、`equalControls` | 增加上一首/进度命令和协商状态，默认 false 保留旧服务兼容 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | 握手、`requestControl`、`hostPeer` | 新版欢迎消息声明免审批能力；授权连接的成员请求经参数检查、速率限制及有界 ID 去重后交现有播放控制流，回复接收状态；旧服务未声明能力时禁发新增命令 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 播放/暂停、切歌、进度、助手及浮动歌词入口 | 客端请求发送给现有同步播放器，不乐观伪造已完成状态；本机音频中断时先恢复本机跟听 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | 状态和按钮 | 新版显示直接控制；旧版仍明确显示需对方确认 |
| [TogetherSessionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherSessionTest.kt) | 共享队列回归 | 相应改写为等待自动接收的控制事件；此轮未执行 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 新版两端客端控制 | 需创建者审批 | 播放/暂停、前后切歌、队列选歌和进度操作自动投递，等待播放状态同步 |
| 连接旧版创建端 | 旧协议审批 | 保持旧协议审批，不发送新增上一首/进度命令 |
| 相同请求 ID 在同一连接重放 | 可能重复进入请求队列 | 有界缓存已处理结果，避免再次投递 |

## 平台与数据影响

无需新增权限、存储或聊天格式；新增握手字段可被旧版忽略。此修改仍使用现有单对端传输和创建端队列：尚不是三人以上，也尚未支持所有成员贡献本地歌曲或平等管理全部队列；创建端进程退出的房间迁移尚未实现。控制投递依赖创建端的应用播放控制收集器；当它不可用或限速时返回失败，未实现可靠的执行级确认。

## 验证

按维护者本轮“只写代码，不用构建和测试”的明确要求，本次更改**未构建、未运行测试和设备验收**。原有 R5 安装包与历史测试不包含这次修改。需在有授权时验证新旧双向兼容、重复控制、后台/进程重建、拖动/本机音频中断以及命令快速冲突。

## 限制与后续

这是双人共享控制的增量，不等同于完整的“没有房主特权”的多人房间。需继续实现成员集合、共同队列、房间协调节点退出恢复、跨网连接及相应验收。

## 同步更新的文档

`docs/README.md` 变更索引和 `docs/DEVELOPMENT.md` 7.1 节的当前实现边界。
