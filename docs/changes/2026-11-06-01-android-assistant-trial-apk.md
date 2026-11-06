# 2026-11-06 — 构建包含当前助手提示词的 Android 试用包

- 修改日期：2026-11-06（Asia/Shanghai）
- 类型：本地构建与交付
- 版本：`version.properties` 的 0.6.7 / 41
- 平台：Android
- 关联：[0.6.7 正式发布](2026-11-05-43-publish-0.6.7-harness-release.md)

## 需求与依据

用户需要一个 APK 试用。当前工作区的 [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) 中 `SYSTEM` 与已发布的 `v0.6.7` 不同，因此从当前工作区重新编译 Android Release，使用独立文件名交付。

## 具体修改与交付

| 文件或入口 | 作用 |
| --- | --- |
| `:composeApp:assembleRelease` | 使用当前 `commonMain`、`jvmMain`、`androidMain` 与生成的版本源码，生成正式签名 APK。 |
| `build/android-trial-2026-11-06-jra3Fe/Resonance-0.6.7-assistant-trial.apk` | 本地试用包，167539432 字节，SHA-256 `89f5f006c513d8cf74e7bf72997fcc7427aa7ebb14cf27db4d18caa60508e547`。 |
| 同目录下 `SHA256SUMS.txt`、`verification.json`、`build.log` | 实际产物校验值和构建验证记录。 |

## 修改前后与数据影响

已公开的 0.6.7 包使用发布时的提示词。本地试用包包含当前工作区的完整 `SYSTEM`，包名 `com.resonance.player`、版本号和签名与公开版相同，可用于覆盖同版本。此次操作只构建文件，未修改应用源码、写入设备数据或上传网站和 GitHub。

## 验证

| 实际操作 | 结果 |
| --- | --- |
| 单 worker、进程内 Kotlin 的一体化 `assembleRelease` | 编译触发 cgroup 内存回收节流后主动停止。日志保存在 `integrated-attempt.log`。 |
| 分段执行 Kotlin 编译、AGP D8 和 `:composeApp:assembleRelease` | 通过。完整 DEX 校验匹配 1334 个应用类，最后的组装及 `lintVitalRelease` 通过。沿用上次发布的分段编排，使用本次生成的 class 和 DEX 输入。进程树硬上限 2 GiB，软限 1536 MiB，swap 为 0，采样峰值约 1.50 GiB。 |
| `apksigner verify --verbose --print-certs`、`aapt2 dump badging`、`zipalign -c -P 16 4` | 通过。V2 签名有效，证书 SHA-256 与官网正式包一致，包名和 0.6.7 / 41 匹配，16 KiB 对齐通过。 |
| ZIP CRC、APK SHA-256、DEX 字符串核对 | 通过。`classes3.dex` 含当前完整 `SYSTEM`。APK SHA-256 与正式发布包不同。 |

## 限制与后续

本轮未安装手机、执行真机回归、调用 DeepSeek 或构建桌面包。

## 同步文档

更新 `docs/README.md` 的变更索引及 `docs/DEVELOPMENT.md` 的本地构建记录。
