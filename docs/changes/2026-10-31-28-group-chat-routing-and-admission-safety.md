# 2026-10-31 — 修正群聊会话、发送者序号和并发入房边界

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：多人聊天及准入修复；版本 0.6.5 / 39，不升版
- 影响平台：Android、Windows 的共享 JVM 服务和界面
- 关联：[多人房间初始实现](2026-10-31-23-multiplayer-room-coordinator-and-equal-control.md)

## 需求或问题

多位成员聊天需要各自独立的消息序号、正确的房间会话及明确加入确认；初始多人实现可能把校验码当成会话 ID、不同成员的相同序号互相去重、将并发审批指向错误成员，并接受不属于成员贡献队列的上传。

## 原因与依据

静态检查发现：群聊和私聊都用创建者设备证书指纹作为持久化文件 ID；接收序号仅一个全局值；发送队列检查 `peerId` 而不是当前会话；多人握手共用单个 `pendingApproval`，且连接池插入和成员容量检查不原子。Windows 或 Android 实际运行情况未验证。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [TogetherCompanionStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionStore.kt) | `receiveGroup`、`incomingBySender`、`clear` | 群消息按发送者分别保存序号并在加密清空标记中保存底线，限制最多 128 个发送者；保留原私聊单序号格式 |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `attach`、`flush`、`receive` | 发送队列检查实际会话；校验收到的会话 ID；群广播按发送者去重；房主附着不将所有待发送消息错误地重发给单名访客 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `host`、`sendChat`、`handleMemberWire` | 新建房间生成随机会话 ID，区分设备身份与房间；拒绝非当前房间的群发，发送时固定收件成员及房间世代以免延迟任务转发到新房间；访客使用界面传入的目标；入房审批不能覆盖其他待确认人，成员插入/清理核对身份及世代；仅接收已接纳队列中的贡献上传；显式退出移除恢复凭据 |
| [TogetherCompanionPanel.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 目标与说明 | 配对使用对端设备 ID；说明协调设备转发及群消息断线重发限制，群消息确认只标为协调设备收到 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | 群能力入口 | 仅多人能力协商成功时显示贡献歌曲入口；访客基础播放控制沿用已接入播放器的 `request` 格式，队列增删移动才使用群 `action` |

## 修改前后行为

| 场景 | 修改前 | 新设计（未运行验证） |
| --- | --- | --- |
| 两人各发送序号 1 | 第二条可能被视为重复 | 按每位发送者分别记录 |
| 访客发送房间消息 | 格式化校验码可能与会话 ID 不匹配 | 使用当前界面绑定会话 ID |
| 多人几乎同时申请加入 | 后来请求可能覆盖确认对象 | 同时只展示一个审批，其他并发请求拒绝并需重试 |
| 群聊与私聊 | 设备指纹可能作为相同存储 ID | 新房间随机 ID；不自动迁移或删除旧记录 |

## 平台与数据影响

新群会话使用随机 ID，本机加密数据新增 `incomingBySender` 字段及清空标记；旧私聊格式保留。尚未交付实验性旧群记录不会自动合并，新旧群标识切换会使其不显示在新房间中，旧文件不删除。协调设备仍参与并可读取转发消息，群发送者签名尚未接入；不能将链路 TLS 说成协调设备不可见的群组端到端加密。

## 验证

仅静态阅读并修改源码。依用户要求未构建、未运行测试、未进行多端或对抗验收，不能将早期 R5 结果用于本次代码。

## 限制与后续

群广播仍未实现每个接收者的持久 ACK、离线补发和成员级端到端签名；断线可能漏消息，房主发出消息的投递状态尚不可靠。引入独立 `multiplayer` 握手标记并对旧端过滤群消息，旧端只能单独入房且暂不支持新群聊天；此兼容路径未经验证。多人队列、入房兼容、上传并发、协调节点迁移及安全性仍需完整实现和验证。当前尚不能发布或宣称多人可用。

## 同步更新的文档

`docs/README.md` 与 `docs/DEVELOPMENT.md`；本记录勘误早期多人记录对完整性和验证的过度判断。
