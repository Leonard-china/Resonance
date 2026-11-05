# 2026-11-05 — 修复FFmpeg 4.4整轨响度分析参数不兼容

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug修复
- 适用版本：0.6.6 / 40，保持未公开草稿版本
- 影响平台：共享响度参数、Linux/Windows/Android
- 关联：[三端发布](2026-11-05-29-publish-three-platform-release.md)

## 需求与原因

发布前Ubuntu 22.04 CI的整轨响度集成测试返回空结果，Windows同源码通过。FFmpeg 4.4的loudnorm参数LRA范围是1—20，当前50超限使进程退出并返回空测量；新版FFmpeg接受50，本机8.0.1没有暴露问题。依据：[FFmpeg 4.4.2官方实现](https://github.com/FFmpeg/FFmpeg/blob/n4.4.2/libavfilter/af_loudnorm.c#L97)。

## 具体修改

| 文件/符号 | 修改 |
| --- | --- |
| composeApp/src/jvmMain/kotlin/com/resonance/player/audio/LoudnessAnalysis.kt / FILTER | LRA由50改为所有目标版本均接受的20；仅消费input_i/input_tp原始输入测量，目标范围不改变增益公式 |

## 前后行为与数据影响

旧FFmpeg从无法测量变为正常整轨测量，新版保持峰值余量与增益上下限。三端共享参数；不改变缓存版本、数据格式、偏好、音频文件、权限或网络请求。

## 验证

- 初次失败保留：[42008685794](https://github.com/Leonard-china/Resonance/actions/runs/42008685794)。
- 修正后[Windows/Ubuntu主验证42013729108](https://github.com/Leonard-china/Resonance/actions/runs/42013729108)两项job均成功；Ubuntu 22.04使用系统FFmpeg 4.4实际整轨测试，不跳过失败用例。
- [原生Windows正式构建42013729491](https://github.com/Leonard-china/Resonance/actions/runs/42013729491)成功，桌面435项中416通过、0失败、19条件跳过。
- 当前完整Android 194份/桌面202份源码重新编译，四份正式发行文件重新生成；签名、文件完整性及隔离启动的实际结果见29号发布记录。

## 限制与后续

长时跨公网共听、付费接口及全发行版矩阵未执行；本次只修复实际CI发现的FFmpeg参数兼容问题；异步测试修正另见32号记录。

## 同步文档

文档索引、开发说明、0.6.6发布说明及本轮发布记录。
