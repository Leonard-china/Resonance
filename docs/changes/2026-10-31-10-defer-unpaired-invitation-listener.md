# 2026-10-31 — 首次使用延后伙伴邀请监听，减少防火墙打扰

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：连接体验修复
- 适用版本：0.6.5 / 39，不升版
- 平台：Android、Windows 共享房间服务
- 关联：[伙伴直邀](2026-10-31-03-paired-partner-direct-invitations.md)、[真实验收](2026-10-31-08-isolated-native-validation-and-release-build.md)

## 需求或问题

Windows Release 在全新隔离资料目录启动，欢迎页尚未操作就出现系统防火墙提示。维护者要求连接简便；尚无任何伙伴时没有必要为接收伙伴邀请开放监听。

## 原因与依据

`JvmListenTogetherService` 初始化无条件调用 `ensurePartnerHub`。原生播放驱动会在应用启动时取得服务，即使用户从未进入一起听也会监听。此次直接观察到真实 Windows 系统提示，没有点击或修改防火墙规则。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `companionStartup`、`preparedPartnerContact` | 启动时只为本机已配对伙伴开启邀请监听；没有伙伴时延后到主动申请或接受配对，单纯加入房间不开放伙伴监听；异步 IO 初始化仍与主线程分离 |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | 挂起的 contact 回调 | 只有真正需要发送配对联系方式时准备监听；拒绝配对不会开启监听 |
| [TogetherPartnerHub.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherPartnerHub.kt) | `listeningPort` | 明确区分尚未监听与实际端口，供启动状态回归核对 |
| [TogetherCompanionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherCompanionTest.kt) | 首次/已有伙伴双场景 | 等待初始化任务后检查真实监听；覆盖全新资料目录与记住的伙伴，不依赖不确定的 sleep |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 首次打开应用且没有伙伴 | 立即监听，可触发系统网络提示 | 不开启伙伴监听 |
| 已经配对过、重新打开应用 | 接收伙伴直邀 | 保留自动接收能力 |
| 无伙伴时建房/加入 | 同时准备伙伴监听 | 建房只开放该房间端口；加入不为伙伴接收开放端口 |
| 主动申请或接受配对 | 监听已提前开放 | 此时准备伙伴监听并发送实际联系方式 |

## 平台与数据影响

不改版本、邀请格式、密钥、聊天或伙伴存储。不修改 Windows 防火墙、用户安全设置或权限。首次明确提供网络功能时仍可能需要用户处理系统提示，不能承诺省略系统要求。

## 验证

Windows 原生 Release 首次启动问题已观察；初版启动修复的针对性 together 测试及双端构建 `first-launch-release-4.log` 通过（12m22s）。随后细化为仅配对时开启伙伴监听，新增真实 TLS 建房/加入/双向配对断言；`final-release-5.log` 最终针对性回归与双端构建通过，6 项 TogetherCompanionTest 包含新断言，详见[本次产物记录](2026-10-31-13-current-release-artifacts-and-device-verification.md)。最终窗口复测受现有 Windows 安全提示遮挡，已请用户手动决定，未操作该提示。06:48 启动的两小时测试使用固定 Windows 运行时快照与最新 Android 私有包，继续验证明确建房后的传输/播放/聊天链路，不把它冒充本次 Windows 新启动行为验证。

## 限制与后续

完成新包冷启动窗口复测和最终产物重新验签/哈希。跨网的防火墙授权仍属于用户系统决策，本轮未自动替用户允许。

## 同步更新

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 第 7.1 节。
