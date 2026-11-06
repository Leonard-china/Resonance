# 2026-11-06 — Resonance 0.6.9 曲库与截图导入优化

## 主要更新

- 截图导入草稿检查点恢复：导入失败或中断后保留已确认歌名、选择与剩余草稿，支持恢复重试并防止旧保存覆盖。
- 暂停准备恢复：修复快速换曲在暂停状态下完成准备后未恢复选中曲目的问题，保持暂停并在播放时平滑起播。
- 语义搜索联合筛选：支持明确歌名、歌手与目标 BPM 节拍范围联合过滤。
- 批量重新定位音频优化：批量修复失效音频时一次性合并元数据并单次原子提交曲库索引。

## 安装包

- Android 使用 0.6.9 / 43，延续正式版签名。
- Windows 提供 EXE，Linux x86_64 提供便携包和 DEB。

[发布与验证记录](changes/2026-11-06-07-publish-0.6.9-release.md)

<!-- GENERATED-CHECKSUMS -->

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| `Resonance-0.6.9-release.apk` | 167539412 | `44a4a134c59b29cddf3e1ff6c20f94715681ebfc8928cf8fa2fa0e4668e97681` |
| `Resonance-0.6.9.exe` | 276278784 | `9e79c1f988dd5ef9fa95d4bccefa4e9b92d752c0b9406c1b9f990a3c471874ac` |
| `Resonance-0.6.9-linux-x86_64.tar.gz` | 267109404 | `f80873d4f327d0a6b3cbb0576e82cc93c7d773248ac61a6e3d27e4a9f2de6b55` |
| `Resonance-0.6.9-linux-x86_64.deb` | 246096182 | `5f285d858dad2fcf80dc788ddcb15bab2fc46fd414b7e8d2d64c906dfcfd9388` |

<!-- END-GENERATED-CHECKSUMS -->
