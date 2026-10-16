# 2026-10-16 — 修复 Android 导出歌单因部分音频无封面导致 No album art found 异常中断

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.4.2（保持既有版本号，不修改 `version.properties`）
- 影响平台：Android / 共享同步包流程
- 关联记录或需求：用户反馈在「歌单导入与导出」中点击导出歌单时出现提示“同步失败: No album art found”

## 需求或问题

用户在 Android 手机（已成功安装并运行 Resonance v0.4.2）上使用「工具」->「歌单导入与导出」->「导出歌单」功能时，顶部弹出绿色横幅警告：
```
同步失败: No album art found
```
导致歌单归档文件（`.resonance`）无法完成打包和导出。

## 原因与依据

1. **底层异常抛出源头**：
   - 当应用扫描 Android 系统 MediaStore 导入本地音频时，会为每首歌曲分配系统缩略图 URI，格式为：
     `content://media/external/audio/albumart/<albumId>`
   - 当手机内某些本地音频（如部分翻唱歌曲、未内嵌 ID3 APIC 封面的 MP3/FLAC 等）在系统 `MediaProvider` 中不存在封面缩略图缓存时，调用：
     `activity.contentResolver.openInputStream(Uri.parse(value))`
     Android 系统底层会直接抛出 `java.io.FileNotFoundException: No album art found`。
2. **缺乏异常保护与降级机制**：
   - 在 [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) 原实现中：
     ```kotlin
     private fun openTrackSource(value: String?): java.io.InputStream? {
         value ?: return null
         return if (value.startsWith("content://")) {
             activity.contentResolver.openInputStream(android.net.Uri.parse(value))
         ...
     ```
     未对 `openInputStream` 添加 `runCatching` 保护。
   - `openArtworkSource` 直接委托给 `openTrackSource`，一旦抛出 `FileNotFoundException: No album art found`，异常直接向上穿透至 [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) 的全局捕获块，显示为 `同步失败：No album art found`。
   - [PlainSyncPackage.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt) 的设计规范中，`openArtwork` 允许返回 `null`（代表该曲目无封面，只打包音频与元数据，不写入 `art/<id>.cover` 条目）。原实现因抛出异常而未能正常返回 `null`。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `exportSyncPackage` | 将 `openArtwork` 回调入参升级为完整 `track` 对象，以便在封面 URI 失效时回退读取音频源文件 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `openArtworkSource` | 增加 `runCatching` 保护，捕获 `FileNotFoundException: No album art found` 等系统异常；当 MediaStore 封面不存在时，优雅降级调用 `extractEmbeddedArtworkStream` 读取音频内嵌封面；若仍无封面则安全返回 `null` |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `extractEmbeddedArtworkStream` | 新增内嵌封面提取流辅助方法，利用 `MediaMetadataRetriever` 解析音频二进制中的 `embeddedPicture` |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `openTrackSource` | 为 `contentResolver.openInputStream` 及文件流增加 `runCatching` 容错保护，避免不可读流直接使进程崩溃 |
| [PlainSyncPackageTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/sync/PlainSyncPackageTest.kt) | `exportAndImportSucceedWhenArtworkIsMissing` | 新增回归测试用例，验证当曲目无封面（`openArtwork` 返回 `null`）时，歌单能正常导出明文归档且顺利导入还原 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 导出包含无封面或 MediaStore 未索引封面歌曲的歌单 | 抛出 `FileNotFoundException: No album art found`，提示“同步失败: No album art found”，导出中断 | 优先读取系统封面，若报 `No album art found` 自动降级提取音频内嵌封面；若完全无封面则跳过封面条目，音频与歌单结构完整打包导出，提示“已导出到下载/Resonance” |

## 平台与数据影响

- **共享逻辑**：`.resonance` 归档格式规范保持不变，依旧为标准 Zip 包，带 SHA-256 校验和防篡改安全清单。
- **Android 端**：消除了对系统 MediaStore albumart 缓存必须存在的硬性依赖，提升了导出大歌单时的健壮性。
- **Windows 端**：无不良影响。
- **数据与权限**：不删除、不篡改用户本地真实音频与歌单数据。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --tests "com.resonance.player.sync.PlainSyncPackageTest"` | 本机 JVM / Gradle | **通过**（覆盖全部 4 项单元测试，包括新增的 `exportAndImportSucceedWhenArtworkIsMissing` 测试） |
| `.\gradlew.bat :composeApp:compileReleaseKotlinAndroid` | 本机 Android SDK 36 / Kotlin | **通过**（编译无错误） |

## 限制与后续

- 本次修改聚焦于修复 Android 导出歌单时的无封面流异常阻断与回退处理。

## 同步更新的文档

- [文档入口与开发变更索引](../README.md)
