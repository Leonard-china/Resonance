# 2026-10-31 — 为一起听聊天接入 Android 和 Windows 消息提醒

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：功能
- 适用版本：0.6.5 / 39，未升版
- 影响平台：Android、Windows
- 关联：[去重事件](2026-10-31-16-deduplicate-incoming-chat-alert-events.md)、[维护者新需求](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)

## 需求或问题

朋友发来聊天消息时，用户离开聊天界面不能及时得知；提醒不应泄露聊天正文，也不应把重连重发的旧消息当成新消息反复提醒。

## 原因与依据

聊天已提供持久保存后才发出的去重事件，但此前两端都没有消费该事件并产生系统通知。Android 13 起普通通知需要运行时授权，系统和通知渠道也可能由用户关闭；Windows 托盘可能不可用或被系统专注模式抑制。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [AndroidTogetherRuntime.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/together/AndroidTogetherRuntime.kt) | `showChatNotification` | 进程级监听已保存的新消息事件，创建独立消息通知渠道；权限/系统设置允许时发送不含正文的通知，点击打开应用，权限竞态安全退出 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt)、[PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | 通知状态、显式启用 | 查询运行时权限、应用和已有渠道开关；在用户点击开启时请求权限或打开相应系统设置，回到应用重新查询 |
| [TogetherCompanionPanel.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt)、[TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | 通知入口 | 未开启时解释可能错过提醒，并提供显式开启入口 |
| [Main.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/Main.kt) | `TrayState` | 支持系统托盘时消费同一事件并展示通用提示，不包含消息正文 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 进程存活、授权允许且收到新消息 | 仅聊天面板变化 | Android 系统通知或 Windows 托盘提醒 |
| 重连重发已保存消息 | 无系统提醒 | 仍 ACK，但不生成新提醒事件 |
| Android 通知未获授权/被关闭 | 没有开关说明 | 聊天面板提供说明及用户主动启用入口 |

## 平台与数据影响

不添加 Manifest 权限，复用已有 `POST_NOTIFICATIONS`；不改变聊天文件或协议，不把消息正文、伙伴身份或密钥放进通知。关闭应用进程、系统杀进程、网络断开、系统屏蔽通知或事件缓冲溢出时不保证提醒；没有服务器离线推送。部分 Windows 环境不支持系统托盘。通知同一 ID 会合并，不代表逐条已读。

## 验证

去重基础的上一轮 `message-events-1.log` 在本次用户要求不构建/测试之前已成功，`TogetherCompanionTest` 7 项通过且 Android Debug Kotlin 编译通过。**本次通知接线依维护者明确要求未构建、未运行测试或设备验证**；实际系统弹窗、锁屏、渠道关闭、桌面托盘与前后台行为仍待验证。不能把先前 7 项结果当成本次系统通知通过。

## 限制与后续

通知点击目前打开应用首页，未定向到具体聊天；多人群聊需要带房间及发送方绑定的事件。后续按维护者指示决定何时构建/验收，并在发布前验证权限拒绝与重复重发。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 7.1 节；后续测试结果不得冒充本次已执行。
