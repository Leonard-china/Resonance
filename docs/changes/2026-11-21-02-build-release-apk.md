# 2026-11-21 — 构建当前工作区 Android Release APK（未签名）

- 修改日期：2026-11-21（Asia/Shanghai）
- 类型：构建验证 / 交付产物
- 适用版本：`0.6.6 / 40`（`version.properties`，未升版）
- 影响平台：Android
- 关联记录或需求：[Atelier Acoustics 原生 UI 全面重塑](2026-11-21-01-atelier-acoustics-ui-redesign.md)

## 需求或问题

用户要求基于当前工作区源码构建 Android Release APK。

## 原因与依据

1. 当前工作区包含尚未提交的 Atelier Acoustics 原生 UI 重构源码，版本仍为 `0.6.6 / 40`。
2. 首次按默认 2 GiB cgroup 限额（`MemoryMax=2G`、`MemoryHigh=1536M`、`-Xmx896m`）启动 `:composeApp:assembleRelease` 时，进程内存达到 `1.6G` 触发 `MemoryHigh` 节流与频繁 GC，10 分钟内仍停留在 `:composeApp:compileReleaseKotlinAndroid`。
3. 用户明确授权加大构建内存占用（宿主机保留 2 GiB 即可）。停止旧任务并释放内存后，宿主机可用内存为 `7326 MiB`，随后将构建进程树调整为 `MemoryMax=5G`、`MemoryHigh=4600M`、`MemorySwapMax=0`，Daemon 堆 `-Xmx3072m`、`--max-workers=2`，顺利完成完整构建。
4. 仓库根目录不存在 `keystore.properties`，因此 [composeApp/build.gradle.kts](../../composeApp/build.gradle.kts#L150-L170) 中 `signingConfigs.findByName("release")` 为 `null`，产出的 APK 为未签名 Release 包。

## 具体修改

本次未修改业务源码、版本号或项目构建配置，仅执行受控 Gradle 构建并记录产物与校验结果：

| 产出或日志路径 | 说明 |
| --- | --- |
| `composeApp/build/outputs/apk/release/Resonance-0.6.6-release.apk` | 本次构建的 Android Release APK（`167458208` 字节，未签名） |
| `composeApp/build/outputs/apk/release/output-metadata.json` | AGP 产物元数据 |
| `build/release-apk-2026-11-21/assembleRelease-5g.log` | 本次构建完整日志（忽略目录） |

## 修改前后行为

| 场景 | 构建前 | 构建后 |
| --- | --- | --- |
| `composeApp/build/outputs/apk/release/` | 目录不存在 | 生成 `Resonance-0.6.6-release.apk`（SHA-256：`ba4927cdbc85b47c1799d9cb3744445f3f0fada01825d2c40584a8d8e4161656`） |

## 平台与数据影响

不修改任何业务源码、权限、偏好键、曲库格式、同步协议或音频文件；不读取或输出任何真实密钥。因缺少 `keystore.properties`，产物未签名，不能直接作为正式签名安装包分发或覆盖安装。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `systemd-run --user` (`MemoryMax=5G`, `MemoryHigh=4600M`, `MemorySwapMax=0`, `-Xmx3072m`, `--max-workers=2`, `--no-daemon`, `--offline`) 执行 `:composeApp:assembleRelease` | Linux 宿主机 + JDK 17 + Android SDK 36 | **通过**：耗时 `2m 9s`，53 个任务（26 执行、3 缓存、24 最新），`compileReleaseKotlinAndroid`、`dexBuilderRelease`、`mergeDexRelease`、`lintVitalRelease`、`packageRelease` 均通过；服务峰值内存 `4G`（Swap `0B`）。 |
| `zipalign -c -P 16 -v 4 Resonance-0.6.6-release.apk` | Android SDK build-tools `36.0.0` | **通过**：`Verification successful`（16 KiB 对齐通过）。 |
| `apksigner verify --verbose Resonance-0.6.6-release.apk` | Android SDK build-tools `36.0.0` | **未通过（预期）**：`DOES NOT VERIFY / ERROR: Missing META-INF/MANIFEST.MF`，确认无 `keystore.properties` 时产物未签名。 |
| 实机安装与设备运行 | Android 真机 | **未执行**：未提供签名配置且未连接/安装设备。 |

## 限制与后续

1. 本次生成的 `composeApp/build/outputs/apk/release/Resonance-0.6.6-release.apk` 为**未签名** Release APK；如需正式签名分发或覆盖安装，需先配置根目录 `keystore.properties` 后重新执行 `:composeApp:assembleRelease`（或对产物签名并校验 `apksigner verify`）。
2. 本次未修改代码，工作区原有未提交修改（Atelier Acoustics UI 重构）均完整保留。

## 同步更新的文档

- [文档入口](../README.md)（变更索引）
