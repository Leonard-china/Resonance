# 2026-11-05 — 修复FFmpeg 4.4响度分析兼容性与配对重连回归等待

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug修复 / 回归测试
- 适用版本：0.6.6 / 40，保持未公开草稿版本
- 影响平台：共享响度参数、Linux/Windows/Android；配对测试为桌面JVM
- 关联：[三端发布](2026-11-05-29-publish-three-platform-release.md)

## 需求与原因

发布前Ubuntu 22.04 CI的435项桌面回归中两项失败，Windows同源码通过。FFmpeg 4.4的loudnorm参数LRA范围是1—20，当前50超限使进程退出并返回空测量；新版FFmpeg接受50，本机8.0.1没有暴露问题。依据：[FFmpeg 4.4.2官方实现](https://github.com/FFmpeg/FFmpeg/blob/n4.4.2/libavfilter/af_loudnorm.c#L97)。配对测试只等待connected就立刻断言paired及历史，但TogetherCompanionSession.attach先发布connected，再refresh本地持久化状态，两次StateFlow发布间存在可观察调度窗口。

## 具体修改

| 文件/符号 | 修改 |
| --- | --- |
| composeApp/src/jvmMain/kotlin/com/resonance/player/audio/LoudnessAnalysis.kt / FILTER | LRA由50改为所有目标版本均接受的20；仅消费input_i/input_tp原始输入测量，目标范围不改变增益公式 |
| composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherCompanionTest.kt / realTlsChatPairingLocalClearAndStableDeviceIdentityAcrossNewRooms | 重建服务后等待配对状态与已保存历史恢复，再保留全部身份、配对、清空及投递断言；沿用10秒有界等待，不添加固定长睡眠 |

## 前后行为与数据影响

旧FFmpeg从无法测量变为正常整轨测量，新版保持峰值余量与增益上下限。三端共享参数；不改变缓存版本、数据格式、偏好、音频文件、权限或网络请求。配对生产行为不变，测试按实际异步契约等待，不跳过或放宽数据断言。

## 验证

初次CI失败保留：[42008685794](https://github.com/Leonard-china/Resonance/actions/runs/42008685794)。修正后的双系统CI、正式包重建和本地验证结果待本次发布完成后填入，不能用先前产物冒充修复后包。

## 限制与后续

长时跨公网共听、付费接口及全发行版矩阵未执行；本次只修复实际CI发现的两处发布阻碍。

## 同步文档

文档索引、开发说明、0.6.6发布说明及本轮发布记录。
