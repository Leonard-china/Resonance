# 2026-10-31 — 完整中止下一首预取并默认等待用户开启

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.5 / 39，不升版
- 影响平台：共享 JVM 协议与 Android/Windows 原生设置
- 关联：[会话与预取](2026-10-30-02-listen-together-session-and-native-playback.md)

## 需求或问题

动态网络检查拒绝继续预取时，朋友端可能一直显示正在预备下一首，且保留半成品。用户关闭预取后，已经开始的传输原先也没有检查开关。虽然文档称预取可选，界面初值却为开启。

## 原因与依据

`sendMedia` 因计费网络抛出 `CancellationException`，直接结束传输子任务，没有给接收方发送结束或失败帧。循环也只检查网络，未检查 `prefetchEnabled`。

## 具体修改

| 文件 | 符号 | 修改与目的 |
| --- | --- | --- |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `hostPeer.sendMedia` | 网络拒绝或用户关闭时发有界 `mediaError` 结束本次预取，接收方沿用已验证清理路径删除拥有的半成品、取消预取状态；不影响当前已验证音频；释放本次预取尝试标记，网络恢复后可再尝试 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | `prefetchNext` | 新进入界面默认关闭，明确用户开启后才预取 |
| [TogetherPrefetchCancellationTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherPrefetchCancellationTest.kt) | 实际 TLS 双场景 | 下一首读取到 64KB 后模拟网络拒绝/关闭开关，验证 96KB 内停止、半成品清理、当前歌保留和状态继续传输 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 传输中检查拒绝继续 | 无结束帧，可能保留预取状态 | 显式中止并清理半成品 |
| 用户关闭开关 | 已开始的传输继续 | 下一块前检查并停止 |
| 新界面默认值 | 自动勾选开启 | 默认关闭，等待主动选择 |

## 平台与数据影响

两端使用已有 `mediaError` 帧，协议向后兼容；不删除用户音频，只清理会话拥有的临时文件。Android 使用实时网络检查；Windows 既有网络成本查询缓存最多 30 秒，本修复不将它宣传成即时网络切换保护，也不改动其它下载任务的策略。

## 验证

新增实际 TLS 回归，执行结果待补。既有 `dynamicMeteredNetworkPreventsAndCancelsPrefetch` 实际只覆盖开始前阻止和允许后开始，不能替代本次传输中中止测试。

## 限制与后续

网络成本在系统报告前仍可能有检测延迟；明确进行的当前歌曲传输不属于下一首预取，仍保持原有行为。新版客户端与旧版朋友需在最终设备/Release 验收中检查。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 第 7.1 节。
