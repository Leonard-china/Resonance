# 2026-10-16 — v0.4.3：修复歌单导出无封面异常与提升数据归档健壮性

版本：**0.4.3 / versionCode 26**。本次提供 Android 签名正式版 APK（Android 8.0 / API 26 及以上，包名 `com.resonance.player`）与 Windows 64位独立安装包（`Resonance-0.4.3.exe`）。

## 主要更新

1. **修复歌单导出时因部分音频无封面导致 `No album art found` 中断的问题**：
   - 在 Android 设备上，当导出的歌曲在手机系统 MediaStore 缩略图库中不存在封面时（抛出 `FileNotFoundException: No album art found`），播放器自动执行安全捕获。
   - 新增双重优雅降级：优先回退读取音频文件内置的 ID3 APIC 原图封面；若歌曲完全无封面，则安全跳过封面打包，不再中断导出流程。
   - 彻底保证包含任意歌曲（无论是否有封面）的大型歌单均能顺利打包导出为专属 `.resonance` 归档文件。

2. **跨端数据迁移与归档测试强化**：
   - 新增针对无封面曲目的歌单导出与导入完整生命周期回归测试，确保明文 Zip、SHA-256 校验清单以及数据恢复的健壮性。

## 使用与升级

- **Android**：使用官方正式签名密钥签名，可从历史版本直接覆盖安装升级，曲库、歌单、设置及历史对话数据完整保留。
- **Windows**：运行 `Resonance-0.4.3.exe` 安装，内置完整执行环境与 FFmpeg。
- **数据自治**：保持完全本地优先（Local-First），不设云端账户，所有音频、歌词、封面及会话历史均安全保存在用户设备本地。

## 下载与校验

[GitHub v0.4.3](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.3) 提供 `Resonance-0.4.3-release.apk`、`Resonance-0.4.3.exe` 与 `SHA256SUMS.txt`。校验值由本次实际产物生成。

<!-- GENERATED-CHECKSUMS -->
| 文件 | 大小 | SHA-256 |
| --- | --- | --- |
| `Resonance-0.4.3-release.apk` | 60.87 MiB | `c21e5ec4e31d733ce941e6cfb8f61a1723291b17447767422d33a9cefb96b8a2` |
| `Resonance-0.4.3.exe` | 194.74 MiB | `9ec10c9098fe3a1dc1832234332894c2dec9cf87b635ebd9acbd2fcdbafb53a8` |
<!-- END-GENERATED-CHECKSUMS -->
