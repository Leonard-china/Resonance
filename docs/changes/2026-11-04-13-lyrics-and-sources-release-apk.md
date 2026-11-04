# 2026-11-04 — 构建歌词与新增歌源修复后的 Android Release APK

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：构建 / 文档
- 适用版本：0.6.5 / 39；保持 `version.properties`，未升版
- 影响平台：Android；Windows／Linux 本轮不构建
- 关联需求：用户要求“行，构建 release”，承接[歌词与布局](2026-11-04-09-lyrics-source-recovery-and-compact-tools.md)、[音效弹窗](2026-11-04-10-audio-effect-dialog-window-isolation.md)、[收藏数量](2026-11-04-11-favorites-count-only.md)、[五个歌源与 VPN DNS](2026-11-04-12-more-builtin-sources-and-vpn-dns.md)。

## 需求或问题

上一轮最终源码只构建了 Debug／仪器测试 APK；已有 Release APK 来自更早构建，不能据相同版本号当作包含本轮修复。需要生成当前源码的手机正式签名包并提供可核验产物。

## 原因与依据

这是本地 Android Release 构建交付。版本号不变；调用现有 Gradle Release 签名配置，不读取或输出真实 keystore 配置／私钥。已完成的回归不能代替本次实际打包、签名或包内容检查。

## 具体修改

| 文件或产物 | 关键符号／操作 | 具体内容 |
| --- | --- | --- |
| Gradle Android 任务 | `:composeApp:assembleRelease` | 从当前工作区构建；不修改业务代码或构建配置 |
| [本次 Release APK](../../build/release-lyrics-sources-2026-11-04/Resonance-0.6.5-release.apk) | 正式包 `com.resonance.player` | 将新产物复制到本次独立交付目录，避免与旧的同名版本包混淆 |
| [产物校验](../../build/release-lyrics-sources-2026-11-04/SHA256SUMS.txt)／[验证摘要](../../build/release-lyrics-sources-2026-11-04/verification.json) | 实际文件 SHA-256、签名和资源 | 核对公开签名证书、正式包名／版本、非 debuggable、五个内置歌源脚本和当前源码一致 |
| [文档索引](../README.md)／[开发说明](../DEVELOPMENT.md) | 构建记录 | 增加本次构建入口和真实验证边界 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 当前修复的安装包 | 已有 Debug，旧 Release 内容过时 | 当前源码的独立 Android Release APK，附实际校验值 |
| 版本号相同的多个构建 | 不能凭 0.6.5 判断内容 | 独立目录、文件摘要与内置资源比对区分本次构建 |

## 平台与数据影响

没有改变包名、版本号、权限、配置格式、曲库或音频，也没有安装、卸载或清理设备数据。本次仅本地构建，不上传官网／应用商店、不更新 Linux 安装、不制作 Windows 产物。现有 `RELEASE_NOTES-0.6.5.md` 继续保留已发布基线及历史校验；本次本地重建的校验值写在本记录和独立产物目录，不混入旧双端发布校验。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :composeApp:assembleRelease --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx3g -Dfile.encoding=UTF-8" -Pkotlin.compiler.execution.strategy=in-process --no-configuration-cache --console=plain` | Linux／JDK17／Android SDK36 | **通过**，2m02s，退出码 0；当前源码重新编译、打包，`lintVitalRelease` 随任务通过；不记录本机工具路径 |
| `apksigner verify --verbose --print-certs` | 本次新 APK | **通过**，v2 签名验证成功；公开签名证书与前轮本地 Release 一致 |
| `apksigner verify --verbose --min-sdk-version 23` | 同一 APK，仅验证工具参数 | **通过**，补充核验 v1／v2 均 true；未修改 APK 的实际 minSdk 或签名 |
| `aapt2 dump badging`／`dump xmltree` | 同一 APK | **通过**，`com.resonance.player`、`0.6.5 / 39`；非 debuggable、没有 Debug 拍摄 Activity |
| `zipalign -c -P 16 4`、ZIP CRC 与五个内置 JS 逐字节比对 | 同一 APK／当前源码 | **通过**，对齐与完整性正常；网易云／酷我／酷狗／QQ／Archive 资源均与当前源码相同 |
| 22 份前轮最终实现／回归 SHA-256 复核 | 当前源码 | **通过**，未改变前轮已验证业务实现 |
| 复制产物并重新计算 SHA-256 | 独立交付目录 | **通过**，复制前后摘要一致，167402228 字节（159.65 MiB） |
| 手机安装、启动、真实音效／播放、Windows／Linux 构建 | 本轮 | **未执行**；本次只交付 Android Release |

本次实际 APK SHA-256：`a1ad4b90d942dd036a1919e8f374e2e8e70be6441d694d0e74bedb6c36a295de`。日志、公开签名输出、包信息和资源摘要位于 `build/release-lyrics-sources-2026-11-04/`，不含真实私钥／配置。

## 限制与后续

本次没有重新运行 Kotlin 回归或完整 `lintRelease`（构建自动执行的 `lintVitalRelease` 已通过）；与前轮最终相关源码摘要逐项比对后构建，前轮结果见[验证报告](../audit/2026-11-04/follow-up/VALIDATION.md)，不写成本次测试通过。未做手机安装、启动、音效点击或真实播放验收；没有声称原手机卡死已解决。Windows／Linux Release 未构建。没有公开发布或自动升版。

## 同步更新的文档

[文档索引](../README.md)、[开发说明第 12 节](../DEVELOPMENT.md)。
