# 2026-11-05 — 配对重连回归等待持久化状态刷新

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：回归测试修复
- 适用版本：0.6.6 / 40，不改变版本
- 影响平台：桌面JVM测试；生产配对实现不变
- 关联：[发布验证](2026-11-05-29-publish-three-platform-release.md)

## 现象与原因

Ubuntu CI重建两端服务后，realTlsChatPairingLocalClearAndStableDeviceIdentityAcrossNewRooms在paired断言失败。TogetherCompanionSession.attach先发布connected/peerId，再refresh已保存的配对及历史；测试仅等待connected，两次StateFlow发布之间可被调度观察。初次失败保留在[42008685794](https://github.com/Leonard-china/Resonance/actions/runs/42008685794)。

## 具体修改

composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherCompanionTest.kt的该用例，在重连connected之后增加paired及host历史两条恢复条件，沿用已有10秒有界waitFor。保留全部设备ID、配对、清空后不重放、消息投递与解除配对断言，不改为跳过或扩大超时。

## 前后行为与影响

此前可能在持久状态刷新前检查结果；修改后等待实际异步契约完成再检查。只改测试，不改变Android/Windows/Linux运行逻辑、用户数据、加密、网络或音频文件。

## 验证

- 初次失败保留：[42008685794](https://github.com/Leonard-china/Resonance/actions/runs/42008685794)。
- 修正后[Windows/Ubuntu主验证42013729108](https://github.com/Leonard-china/Resonance/actions/runs/42013729108)两项job均成功；Ubuntu 22.04使用系统FFmpeg 4.4实际整轨测试，不跳过失败用例。
- [原生Windows正式构建42013729491](https://github.com/Leonard-china/Resonance/actions/runs/42013729491)成功，桌面435项中416通过、0失败、19条件跳过。
- 当前完整Android 194份/桌面202份源码重新编译，四份正式发行文件重新生成；签名、文件完整性及隔离启动的实际结果见29号发布记录。

## 限制与后续

不证明长期跨公网共听稳定性；本次范围只覆盖重启后的本地TLS双端恢复。

## 同步文档

文档索引、开发说明与本次发布验证记录。
