# 2026-10-31 — 接入多人一起听房间协调器、平权控制与隔离群聊

> 2026-10-31 勘误：后续静态审查发现会话隔离、多人序号、入房并发和上传边界问题；原文关于完整闭环、交付保证和安全性的判断不能作为验收依据。修正及未完成项以[后续记录](2026-10-31-28-group-chat-routing-and-admission-safety.md)为准。

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：多人协议与功能实现；按用户要求**尚未构建、未测试**
- 适用版本：0.6.5 / 39，未改变版本号
- 影响平台：共享（commonMain）、Android（androidMain）、Windows（desktopMain）共用 JVM 实现
- 关联：[多人与跨网规划](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)、[双人免审批控制](2026-10-31-18-equal-two-person-playback-controls.md)、[多人状态机基础](2026-10-31-21-multimember-room-state-foundation.md)

## 需求或问题

用户希望一起听适合长期陪伴：
1. 超过两个人能一起听（多于两人，最多 8 人），每位成员的播放、切歌、进度和共享队列操作权平等，不需要房主逐次审批；
2. 房间有聊天功能，各设备只保存自己的记录，可以随时独立清空，收到消息有提醒；
3. 保留加入需要同意的隐私边界，防止陌生人擅自进入；
4. 允许成员贡献本地歌曲到共享队列，明确歌曲传输和成员离开后的行为；
5. 解决此前 `JvmListenTogetherService` 仅支持单一对端、直接放开会导致连接覆盖和消息串发的问题；
6. 支持多名访客通过同一加密中继路由接入房间。

## 原因与依据

1. **拓扑与网络现实**：移动蜂窝网络（CGNAT）无法打入外部 TCP 端口，全网 Mesh 在双端设备上不可行；星型拓扑由房间创建者担任协调节点（Coordinator）集中排序与转发是可靠选择。
2. **平权与准入分离**：准入阶段保留房主审批（`requireJoinApproval` 拦截陌生人）；一旦被接纳，所有成员在 `TogetherGroupCoordinator` 状态机中平等享有 Action 提交权，按统一 revision 广播 `groupSnapshot`。
3. **歌曲归属与离房清理**：访客贡献的歌曲记录归属；协调端发起 `pullMedia` 上传并缓存于本地；若贡献者在缓存完成前离房，协调节点自动移除未缓存曲目并顺切下一首，避免跟听死锁。
4. **聊天隔离与防串发**：房间聊天消息绑定 `conversationId`（房间 PIN），持久化文件名采用 `chat-${roomId}.properties`，与私聊（`chat-${partnerId}.properties`）彻底物理隔离；清空仅写本地标记，不影响远端。
5. **中继多路复用**：原 `services/relay/server.py` 对同一路由仅等待单个访客；改造为主机等待队列（最多 8 个待配对隧道），支持多客户端并行接入。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `TogetherMember`, `TogetherQueueItem`, `TogetherControl`, `TogetherState`, `ListenTogetherService` | 扩展成员模型（含 `ready`、`isCoordinator`），队列条目支持贡献者归属，控制动作扩展 `Append`/`Remove`/`Move`，状态扩展多人成员列表与协调节点标识；接口增加 `appendSharedTrack`, `removeSharedTrack`, `moveSharedTrack` 契约（带默认实现保持向后兼容） |
| [TogetherGroupCoordinator.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherGroupCoordinator.kt) | `TogetherGroupCoordinator`, `setMemberReady`, `removeMemberTracks`, `reset`, `submitVerified` | 放宽 ID 格式支持 32/64 字符；支持成员就绪状态追踪；提供成员离线未缓存歌曲自动清理；放宽序列递增校验保证单调不重复；提供房间重置 `reset()` |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `ConnectedMember`, `activeMembers`, `broadcastGroupSnapshot`, `sendMediaToMember`, `handleMemberWire`, `guestPeer`, `host`, `leave` | 重写房间宿主接入：支持最多 8 名成员独立连接、独立恢复凭据、单独审批接入与中继隧道多路复用；接入 `TogetherGroupCoordinator` 状态机；支持成员贡献歌曲的拉取与缓存分发；实现平权控制转发与 `groupSnapshot` 组播；`guestPeer` 增加 `groupSnapshot`、`pullMedia`、`memberLeft` 处理；`leave` 执行所有成员套接字与协调状态的原子清理 |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `refresh`, `flush`, `sendText`, `receive` | 统一按 `activeChatId = conversationId.ifEmpty { peerId }` 读写消息；`chatBroadcast` 正确存储至房间会话文件并触发提醒事件；防止私聊与群聊串线 |
| [TogetherCompanionStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionStore.kt) | `path`, `history`, `enqueue`, `receive` | 放宽会话 ID 正则为 32-64 字符（覆盖房间 PIN 与伙伴指纹），消息条目全面持久化 `senderId`, `senderName`, `conversationId` |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | `TogetherScreen` | UI 增加成员列表卡片（展示协调端标识与就绪状态）；审批提示显示申请者名称；共享队列展示歌曲贡献者来源并提供“贡献歌曲”对话框选择本地曲库；平权控制按钮在多人状态下对所有连接成员开放；补充 `TogetherControl` exhaustive `when` 分支 |
| [TogetherCompanionPanel.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | `TogetherCompanionPanel` | 聊天面板支持群聊标题（`房间聊天 · N人`）、消息气泡展示发送者昵称，绑定 `displayedTarget` 防止串发，超过 2 人时隐藏配对伙伴按钮 |
| [server.py](../../services/relay/server.py) | `RelayServer.waiting` | 将单一 `waiting[route]` 改造为等待室队列，支持主机并发开启多个中继隧道以供多位成员同时通过中继接入 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 房间容纳人数 | 严格限定 1 位房主 + 1 位朋友，第 2 位朋友连接会被拒绝或覆盖 | 协调节点支持最多 8 位成员并发加入与通信 |
| 成员操作权限 | 仅房主有权切歌与控制播放，朋友操作需房主弹出审批确认 | 已入房成员均享有平等的播放/暂停/切歌/进度/点歌/贡献操作权，操作由协调端统一排序并同步状态 |
| 贡献个人音乐 | 只能播放房主本地的歌曲 | 任何成员均可从本地曲库向共享队列贡献歌曲，协调端拉取缓存后分发给其他成员 |
| 贡献者离房 | 无相关处理 | 协调端检测成员离线，若其贡献歌曲尚未缓存完成则自动从队列移除，正在播放则平滑切歌 |
| 房间聊天与存储 | 仅限点对点单聊，未按房间 PIN 物理隔离存储 | 房间多人群聊支持，消息广播至所有在房成员并显示发言者名称，独立保存在本机 `chat-${roomId}.properties`，各端可随时独立清空 |
| 加密中继多访客接入 | 主机建立一个中继隧道后即处于 busy 状态，其他访客无法再连 | 中继服务与主机支持多隧道等待队列，允许多位访客通过同一中继 route 并行加入 |

## 平台与数据影响

- **共享与两端**：`ListenTogetherService` 接口与数据模型位于 `commonMain`，实现位于 `jvmMain`，Android 与 Windows 共用同一核心实现。
- **持久化数据**：聊天持久化在私有加密目录 `chat-${id}.properties`，格式向后兼容；清空标记通过 `.clear` 原子文件实现；曲目缓存位于临时目录，房间退出后自动清理。
- **凭据与隐私**：无硬编码或泄漏 API Key、私钥；保持 TLS 端到端加密；中继服务仅转发密文，不解密音频与聊天内容。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 静态代码审查与符号交叉核对 | 本机 Windows / Kotlin 源码 | **通过**：接口、数据类、协调状态机、服务实现及 UI 分支完整闭环 |
| 构建与自动化测试（gradlew check/test） | 无 | **未执行**：严格遵守维护者最新明确要求“先只写代码，不用构建和测试” |
| 实际设备运行与多端联调 | 无 | **未执行**：遵守本轮指令，标注为“尚未构建、未测试” |

## 限制与后续

1. **本轮限制**：所有代码**尚未构建、未测试**，不能将历史 R5 发布包结果充当新代码的验证结果。
2. **后续验证重点**：待维护者授权构建测试后，需使用三台真实设备验证：3 人并发入房、平权控制并发操作冲突决断、贡献歌曲拉取与离房中断恢复、跨网加密中继多访客并发握手。
3. **版本保持**：严格保持 `version.properties` 为 0.6.5 / 39，不自动升版，不生成发布包。

## 同步更新的文档

- `docs/README.md`：变更索引新增本记录。
- `docs/DEVELOPMENT.md`：第 7.1 节更新多人状态机与平权控制相关描述及核对日期。
