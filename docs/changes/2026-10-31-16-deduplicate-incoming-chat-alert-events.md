# 2026-10-31 — 为聊天提醒增加持久保存后的去重事件

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：提醒基础设施，仍需接通系统通知
- 适用版本：0.6.5 / 39，未升版
- 影响平台：共享契约及 Android / Windows 共用 JVM 聊天实现
- 关联：[多人与跨网新需求](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)、[本机加密聊天](2026-10-31-02-encrypted-local-chat-and-device-pairing.md)

## 需求或问题

维护者反馈朋友发消息时没有提醒。现有聊天在重连后可能重发未确认的消息，仅对消息列表变化发提醒会在重复投递或本机清空后的旧消息重放时重复提醒。

## 原因与依据

`TogetherCompanionStore.receive` 已按对端序号持久去重，并使用独立清空标记防止旧历史复活，但原接口不向会话报告此次消息是否新增；现有服务没有专门的入站消息事件。

## 具体修改

| 文件 | 符号 | 修改与目的 |
| --- | --- | --- |
| [TogetherCompanionStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionStore.kt) | `receive` | 保留原子加密保存和序号底线，仅在新消息成功保存后返回 true；重复或清空前重放返回 false |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `chatMessageEvents`、`receive` | 新增无正文的有界临时事件流，保存并刷新成功后才发出；重复包仍按原协议 ACK，不额外提醒 |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt)、[JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `chatMessageEvents` | 暴露跨平台事件，其他实现默认空流；不暴露消息内容 |
| [TogetherCompanionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherCompanionTest.kt) | 入站事件回归 | 检查事件到达时记录已保存，重复包和清空后旧包不再次触发 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 收到新消息 | 仅更新本机聊天 | 持久保存成功后发出无正文临时事件 |
| 重连重发或清空后的旧包 | 存储去重，但没有事件区分 | 不产生新消息事件，仍按原协议确认 |
| 没有事件订阅者或缓冲区满 | 没有提示能力 | 临时事件可能丢弃；这不是离线消息推送或系统通知保证 |

## 平台与数据影响

不更改存储格式、传输格式、权限或历史数据；Android 和 Windows 使用同一事件契约。事件不含正文、不写入日志。现有双人模型仍有单对端限制；群聊后需扩展带房间和发送方绑定的事件，不能把单对端 `Unit` 事件直接当成完整多人提醒设计。

## 验证

隔离 Gradle `TogetherCompanionTest` 7 项与 `compileDebugKotlinAndroid` 通过，日志 `output/together-2026-10-30/night/message-events-1.log`，3m35s；这是维护者要求不再构建/测试之前完成的结果。后续系统通知接线未编译或测试，见[通知记录](2026-10-31-17-chat-notifications-on-android-and-windows.md)。

## 限制与后续

后续已编写 Android 通知权限/渠道及 Windows 托盘接线，尚未构建或测试，详情见[通知记录](2026-10-31-17-chat-notifications-on-android-and-windows.md)。仍需区分权限拒绝、前后台和进程退出，验证重复投递与隐私；新代码未进入先前已传输的 R5 包。

## 同步更新的文档

`docs/README.md` 索引与 `docs/DEVELOPMENT.md` 7.1 节，明确提醒尚未接线。
