# 2026-11-01 — 拒绝旧房间迟到的聊天附着与成员列表

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：房间聊天生命周期修复
- 适用版本：0.6.5 / 39，未升版
- 影响平台：Android、Windows 共用 JVM 房间服务
- 关联：[房间动作隔离](2026-10-31-35-bind-room-actions-to-session.md)、[群聊路由](2026-10-31-28-group-chat-routing-and-admission-safety.md)

## 需求或问题

关闭旧房间并建立新房间后，旧握手或排队的成员列表操作可能比新房间更晚进入聊天互斥区，覆盖聊天目标、发送器或成员列表。

## 原因与依据

先前聊天附着只校验 ID 格式，不知道调用它的连接是否已被房间服务淘汰；成员列表通过异步 `act` 更新，也没有指定属于哪个会话。外围握手的世代检查和聊天室自己的互斥锁之间存在时间窗口。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `attach`、`channelCurrent` | 在聊天室互斥区内校验附着仍属于当前连接，保存连接有效性回调；收包、发包、补发和配对也检查该回调 |
| 同上 | `updateMembers` | 排队操作携带预期会话 ID，执行时不匹配则跳过 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | 主客端握手、`updateMemberList`、`guestPeer` | 将世代及精确成员/socket/wire 对象绑定到聊天附着；访客记录并校验协商后的房间 ID，成员列表使用对应会话 ID |

## 修改前后行为

按源码设计，旧连接若已淘汰，迟到附着不能替换聊天室的当前发送器；旧会话成员列表不能覆盖新会话。发送时仍使用已捕获的连接，不改为读取新房间的发送器。配对操作需要对应连接仍有效。

## 平台与数据影响

不改变聊天磁盘格式、用户音频、版本或外部服务；内部附着方法新增默认参数保持现有调用者兼容。新多人房间 ID 限定为 32 位十六进制，旧单对端房间只接受其邀请证书指纹作为会话 ID；尚未发布的早期实验群协议不作为兼容承诺。

## 验证

仅静态阅读并修改互斥区、握手及异步成员更新链路；依用户要求未构建、未执行测试、未进行多端和切房压力验收。

## 限制与后续

仍需完整处理多对端聊天室当前对端选择、协调设备移交、逐成员消息送达和离线补发；此修改也不替代密码学签名、成员授权和对抗验证。附着中的挂起操作、关闭与配对竞争均待后续测试。

## 同步更新的文档

更新 `docs/README.md`、`docs/DEVELOPMENT.md` 与继续工作检查点。
