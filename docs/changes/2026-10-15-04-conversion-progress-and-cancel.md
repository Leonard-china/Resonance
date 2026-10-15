# 2026-10-15 — 修复目录转换无进度、取消与长时间等待

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：Bug 修复
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

旧流程扫描/解密/FFmpeg 转码串行但缺少清晰阶段，阻塞等待难取消；Android KGG 实际无解密实现，却与 KGMA 共用入口说明。

## 具体修改

- 新增 ImportProgress StateFlow 与 ImportControl，显示枚举、当前文件、已完成/总量、字节或活动进度条。
- KGMA 按块检查取消；Windows FFmpeg 每200ms轮询、读有界 stderr、10分钟超时；Android 用异步独立会话并定向取消。
- Android 遍历与转换取消后返回已完成曲目，停止剩余步骤；两端清理 staging/part 并保留输入。
- Android 标明 KGMA，KGG 立即给出 Windows 转换提示；Windows 保留本机数据库密钥实现。

| 文件 | 关键修改 |
| --- | --- |
| [model/MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | ImportProgress |
| [io/ImportControl.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/io/ImportControl.kt) | AtomicBoolean 取消/检查 |
| [conversion/DesktopManagedMp3Converter.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/conversion/DesktopManagedMp3Converter.kt) | 受控进程与临时文件 |
| [platform/AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | scanDocumentTree/decryptKgmaMp3/会话取消 |

## 修改前后

以前长时间无可见进展；现在可辨阶段、计数和取消，部分成功结果保留。

## 平台与数据影响

源加密文件不覆盖；输出仍为托管 MP3。Android只 KGMA v3/slot1；Windows加 KGG v5。

## 验证

DesktopManagedMp3ConverterTest：真实 WAV/FLAC 转码、源 hash 保留、取消解密清理；导入报告回归。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

超时按单个转码计，不代表整目录时长；访问很慢的 SAF provider 仍受系统 I/O 延迟。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
