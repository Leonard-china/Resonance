# 2026-11-05 — 将原生播放页动效与助手修复正式包更新到手机

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：正式包构建 / 设备覆盖安装 / 验证
- 适用版本：0.6.6 / 40；不升版
- 影响平台：当前 Android 手机
- 关联需求：用户询问是否完成，并明确手机已经连接，继续完成实际更新。
- 前置记录：[播放页动效](2026-11-05-11-apple-music-player-expansion.md)、[助手与下载修复](2026-11-05-12-assistant-repeat-cost-and-download-recovery.md)、[内存余量保护](2026-11-05-14-reserve-host-memory-before-builds.md)。

## 需求或问题

播放页展开/收起、封面衔接与助手修复已完成源码验证，但手机仍运行前轮歌词修复正式包。用户重新连接手机后，本轮需要生成与当前完整源码一致的正式 APK，保留应用数据覆盖安装，并核对实际安装文件和启动/交互状态。

## 原因与依据

ADB 已识别原手机为授权设备。完整 Android Release 编译结果的 191 份源码 SHA-256 与本轮工作区一致，并包含 `PlayerExpansionKt`、`PlayerExpansionState` 和 `AssistantToolSession`，不采用仅局部替换 UI class 的旧包。内存现已满足构建硬上限加 1536 MiB 的余量门槛；共用验证锁并持续监测余量。

## 具体修改

| 文件或入口 | 关键动作 | 修改与目的 |
| --- | --- | --- |
| 忽略目录 `build/apple-player-phone-install-2026-11-05/` | 源码快照、完整 Release class、Gradle/D8 打包与校验 | 在完整源码哈希守卫下生成本轮 APK，核对全部应用类、依赖资源类和 DEX 字节。 |
| 手机正式应用 `com.resonance.player` | 原包备份、公开证书比对、`adb install -r` | 保留当前应用数据，拒绝签名不一致的覆盖，不卸载或清除数据。 |
| 设备实际安装 APK / `MainActivity` | 拉取核对、持续进程及可见 Activity 检查 | 不凭相同版本号判断更新成功，复核本轮实际安装的字节及启动状态。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 手机应用 | 上一轮歌词修复包，尚无本轮播放页动效及助手修复 | 已同签名覆盖为本轮正式包；实际 APK 字节、稳定启动及原生开合手势通过检查 |
| 数据及回退 | 用户原有歌曲、歌单和设置 | 覆盖安装保留，另存安装前 APK 作为回退副本 |

## 平台与数据影响

不新增业务源码改动，不改版本、权限、曲库格式、同步协议或音频文件。包含已验证的当前 Android 共享 UI 和 JVM 助手修复；桌面专属歌词拖动不属于 Android 二进制。只更新手机，不操作电脑安装、服务器、官网或公开发布。不读取或输出真实 API Key、签名私钥或签名配置内容。

## 验证

本轮在独立 systemd 用户服务中串行打包和安装，共用 `build/resonance-heavy-validation.lock`。打包整个进程树限制为 `MemoryMax=1536M`、`MemoryHigh=1152M`、`MemorySwapMax=0`；启动时可用约 6.5 GiB，满足 3 GiB 门槛，峰值 1208705024 字节（约 1152.71 MiB）。安装/校验服务限制为 896 / 640 MiB、Swap0，峰值 559259648 字节（约 533.35 MiB）。两项均持续检查宿主余量，未提高预算或关闭用户应用，也未修改系统 swap。

| 实际命令或操作 | 环境与结果 |
| --- | --- |
| `adb devices -l` / `getprop ro.build.version.sdk` | 原 Honor 手机，授权 `device`，Android API 36。 |
| 当前源码与完整 Release 编译快照逐份 SHA-256 核对 | **通过**：191 份源码全部一致，1522 个应用 class 含本轮播放页动效和助手修复。复用的是前置完整编译结果；本轮没有再全量编译 Kotlin，不把历史编译改称本轮新执行。打包前后再次核对源码及完整 class 快照。 |
| 原设备 APK 拉取与校验 | **通过**：本轮独立保存 `artifacts/installed-before.apk`；SHA-256 为 `6923cb29e1b7495558b2e236eca0dd3532c7945c82dd393a35af7b429ab7af70`，没有覆盖此前备份。 |
| `gradle_task.py dex-prepare` | **通过**：执行 `dexBuilderRelease`、`mergeExtDexRelease`、`mergeReleaseStartupProfile` 与输入导出；源哈希守卫后接入完整 Release class。单 worker、短生命周期 Gradle、进程内 Kotlin策略，离线依赖。 |
| `merge_dex_complete.py` | **通过**：使用 AGP 配套 D8、Release/min API 26、单线程合并全部输入；4 份 DEX 的校验、1522 个应用类、38876 个全部输入类及 183 个资源类均覆盖。含 `androidx.startup.R$string`，没有沿用缺资源类的中间候选。 |
| `gradle_task.py release` / `:composeApp:assembleRelease` | **通过**：本轮实际生成新的正式 APK；消费本轮完整 D8 及原始 metadata。Release/Vital Lint 本轮显式跳过，未把签名与安装校验代称 Lint 通过。 |
| `verify_ready_apk.py` / apksigner / zipalign | **通过**：与原包公开签名证书相同；V2 及显式最低验证 API 23 的 V1 校验通过；16 KiB 对齐通过。ZIP 完整，APK 内 4 份 DEX 与本轮完整合并输出逐份一致。Manifest minSdk 仍为 26，targetSdk 36。 |
| `adb install -r` / 安装后拉取 APK 比对 | **通过**：`com.resonance.player` 正式包，0.6.6 / 40，保留数据覆盖；设备实际 APK 与下表产物 SHA-256 一致。设备更新时间 **2026-11-05 15:06:31**。未卸载或清除数据。 |
| `am start -W` / 连续 PID / Activity / 当前进程崩溃检查 | **通过**：主 Activity 恢复，同一进程连续运行 15 秒，当前进程没有 `FATAL EXCEPTION`。不是只核对启动命令返回码。 |
| `device_interaction.py open / short / long` | **通过**：从恢复的真实迷你播放器展开；177 px 慢速短拉后回到原展开位置（关闭按钮坐标误差不超过 3 px）；长拉收起并重新出现迷你播放器；再次展开和点击关闭均通过，最后停留音乐库。前后均为暂停态，未切歌、seek 或触发播放。 |
| 实际手机截图查看 | **通过**：封面、原歌词当前句淡底高亮可见；拖动中间帧确实移动并露出主壳，短拉结束恢复展开。没有把这一静态当前句检查当成跨句/跨曲跟随回归或帧率测量。 |
| 文档、链接、源码哈希、实际安装包复核及 `git -c core.whitespace=cr-at-eol diff --check` | **通过**；保留工作区全部未提交改动。 |

初次 UIAutomator 采集没有生成 XML，解析失败，当时截图显示系统通知栏覆盖。收起通知栏并回到正式应用后，压缩 UI 树、截图及上述实际指针交互全部重新执行成功；未将初次采集失败算作通过，也未因此回退已验签的应用。

证据保存在忽略目录 `build/apple-player-phone-install-2026-11-05/`：`ready-apk-verification.json`、`phone-install-verification.json`、`phone-interaction-verification.json`、`validated-source-sha256.json`、打包日志及 `device/` 截图。用户界面/曲目截图仅留本地，不作为可提交或公开发布资源。

| 本轮正式产物 | 字节 | SHA-256 |
| --- | --- | --- |
| `build/apple-player-phone-install-2026-11-05/artifacts/Resonance-0.6.6-player-animation-and-assistant-release.apk` | 167451490 | `dedb73d083d5fd111e121257ea44e341a82dcca73b7adfef308553a0b892a457` |

## 限制与后续

本次手机构建、覆盖安装与所列实机交互范围内无已知未完成项。版本保持 0.6.6 / 40，因此新旧包应通过实际文件和更新时间区分。本次未更新 Windows/Linux 安装，未发布，也未做付费模型、真实下载、跨曲连续跟随、音频听感或长时播放验收。此前桌面应用仍使用其前轮安装，不把手机成功扩大为桌面已更新。

## 同步更新的文档

文档入口、当前开发说明与本轮安装状态。
