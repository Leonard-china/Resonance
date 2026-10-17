# 2026-10-17 — 修复 AndroidMusicAccess 中 Path.of 引发的 Release Lint NewApi 错误

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：Bug 修复 / 构建合规
- 适用版本：0.4.9 / 32 → 0.5.0 / 33
- 影响平台：Android
- 关联记录或需求：交接文档构建检查中运行 `lintRelease` 静态代码分析；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

在执行正式构建流程的 Release Lint（`gradlew :composeApp:lintRelease`）时，构建阻断失败并报错：
```text
D:\Project\Resonance\composeApp\src\androidMain\kotlin\com\resonance\player\features\AndroidMusicAccess.kt:34: Error: Call requires API level 34, or core library desugaring (current min is 26): java.nio.file.Path#of [NewApi]
            else Files.isRegularFile(if (source.startsWith("file:")) Path.of(java.net.URI(source)) else Path.of(source))
```
导致 Release APK 构建前置校验无法通过。

## 原因与依据

`java.nio.file.Path.of(...)` 是 Java 11 引入并在 Android API 34+ 才原生支持的 API。本项目最低支持版本为 Android 8.0（API 26），在低版本 Android 设备上直接调用该方法将导致运行时 `NoSuchMethodError` 崩溃。在 `AndroidPlatformServices.kt` 的 `audioFileAvailable` 现存实现中，使用的是兼容全版本 Android 的标准 `java.io.File(path).isFile` 判定。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [AndroidMusicAccess.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidMusicAccess.kt) | `AndroidMusicAccess.audioAvailable` | 将 `Files.isRegularFile(Path.of(...))` 替换为兼容 API 26+ 的 `java.io.File(if (source.startsWith("file:")) java.net.URI(source).path else source).isFile`，消除 `NewApi` 违规。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| `gradlew :composeApp:lintRelease` 校验 | 因 NewApi 错误导致 Task FAILED 退出码 1，阻止打包 | 静态分析 0 Error，`lintRelease` BUILD SUCCESSFUL |
| Android API 26~33 设备运行本地音频可用性判断 | 潜在 `NoSuchMethodError` 崩溃隐患 | 使用通用 `java.io.File` 检查，全 API 版本稳定兼容 |

## 平台与数据影响

仅修正 Android 平台私有媒体库检查代码的实现细节，不改变业务返回值与数据结构，不涉及数据库、存储模型或偏好键改动。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:lintRelease --no-configuration-cache --console=plain` | Windows 11, JDK 17, Android SDK 36 | **通过**。耗时 49s，30 actionable tasks，0 error，报告生成于 `build/reports/lint-results-release.html` |
| `.\gradlew.bat :composeApp:allTests` | Windows 11, JDK 17 | **通过**。包含 Desktop / Android 全量单元测试 107 项 |

## 限制与后续

本次范围内无已知未完成项。

## 同步更新的文档

- [文档索引](../README.md)
