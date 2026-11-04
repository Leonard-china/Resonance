# 2026-11-02 — 构建发布版 Android Release APK (0.6.5)

- 修改日期：2026-11-02（Asia/Shanghai）
- 类型：构建与产物交付
- 适用版本：0.6.5（versionCode 39，未修改版本号）
- 影响平台：Android
- 关联记录或需求：[2026-11-02-07-loading-and-buffering-feedback.md](2026-11-02-07-loading-and-buffering-feedback.md)

## 需求或问题

用户请求构建一个正式的 release-apk 进行体验和日常使用。修改前，构建产物目录中的 Release APK 为 2026-11-01 生成的历史版本，尚未合入 2026-11-02 完成的播放全链路加载与缓冲状态（`PlayerState.isBuffering`、`isPreparing`、微型缓冲进度条与提示文本）、转场空闲等待优化及渲染性能调优。

## 原因与依据

依据 `docs/DEVELOPMENT.md` 的构建规范，使用正式的 Gradle Release 任务 `:composeApp:assembleRelease` 重新编译打包。构建过程读取本地 `keystore.properties`（安全隔离，不打印私钥和密码）进行 v1/v2 签名，确保生成可直接安装于真实 Android 设备和模拟器的独立 Release 安装包。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| `composeApp/build/outputs/apk/release/Resonance-0.6.5-release.apk` | Release APK 产物 | 执行 `:composeApp:assembleRelease`，完整包含缓冲加载反馈与全部最新提交的改动 |
| `release/0.6.5/Resonance-0.6.5-release.apk` | 正式发布目录产物 | 同步最新构建的 Release APK，方便用户直接获取 |
| `release/0.6.5/SHA256SUMS.txt` | 校验和清单 | 更新 `Resonance-0.6.5-release.apk` 的最新 SHA-256 实际校验和 |
| `docs/RELEASE_NOTES-0.6.5.md` | 双端安装包校验表 | 更新 Release APK 大小与 SHA-256 记录，确保产物与文档严格一致 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Release APK 产物状态 | 产物为 2026-11-01 旧版本，缺少缓冲加载指示等最新功能 | 产物包含全部最新改动，大小 159.57 MiB，通过 v2 签名验证 |
| 设备安装与运行 | 需手动安装旧包 | 通过 `adb install -r` 顺利完成流式覆盖安装，拉起后界面与服务正常运行，留存屏幕截图 |

## 平台与数据影响

- **平台影响**：仅影响 Android 平台的 Release APK 安装包与发布校验文件。Windows 平台 EXE 保持既有状态。
- **数据影响**：无数据库模式或配置结构修改，完全向后兼容本地歌曲数据库、历史听歌统计与配置数据。
- **安全与凭据**：签名过程全程由本地安全凭据完成，未暴露私钥、密码或 API Key。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:assembleRelease --no-configuration-cache --console=plain` | Windows 11 / JDK 17 | 通过。54 个任务（11 个执行，43 个 UP-TO-DATE），耗时 3m 48s 成功生成 APK |
| `apksigner.bat verify --verbose composeApp/build/outputs/apk/release/Resonance-0.6.5-release.apk` | Android SDK 36.0.0 build-tools | 通过。`Verifies: Verified using v2 scheme (APK Signature Scheme v2): true`，签名者数量为 1 |
| `adb install -r composeApp/build/outputs/apk/release/Resonance-0.6.5-release.apk` | Android 12 (MuMu 12 / 127.0.0.1:16384) | 通过。`Performing Streamed Install -> Success` |
| `adb shell am start -n com.resonance.player/com.resonance.player.MainActivity` | Android 12 (MuMu 12 / 127.0.0.1:16384) | 通过。主 Activity 正常拉起，进程 PID 5513 存活且聚焦，截图存于 `docs/audit/2026-11-02/release-apk-launch-verification.png` |

## 产物信息

- **文件名**：`Resonance-0.6.5-release.apk`
- **文件路径**：
  - 构建产物路径：`D:\Project\Resonance\composeApp\build\outputs\apk\release\Resonance-0.6.5-release.apk`
  - 归档发布路径：`D:\Project\Resonance\release\0.6.5\Resonance-0.6.5-release.apk`
- **文件大小**：167,317,180 字节（约 159.57 MiB）
- **SHA-256**：`b60217cd4fe5f9baf2eeed17fcc4f60daf2e75c026310f4ee896d58aef44aa4d`
- **签名方案**：v2 Scheme 签名有效

## 限制与后续

本次仅针对用户提出的 Release APK 需求完成构建与端侧安装验收，未同步构建 Windows Release EXE。本次范围内无已知未完成项。

## 同步更新的文档

- [docs/README.md](../README.md)
- [docs/RELEASE_NOTES-0.6.5.md](../RELEASE_NOTES-0.6.5.md)
- [release/0.6.5/SHA256SUMS.txt](../../release/0.6.5/SHA256SUMS.txt)
