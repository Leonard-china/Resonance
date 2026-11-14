# 2026-11-14 — 配置 Ubuntu 本机 Android 16 APK 测试设备

- 修改日期：2026-11-14（Asia/Shanghai）
- 类型：本地测试环境与协作文档
- 适用版本：0.6.11 / 45
- 影响平台：Ubuntu 上的 Android 测试
- 关联记录：[ApkStart 官方仓库](https://github.com/RoutineMiku/apkstart)、[0.6.11 发布记录](2026-11-13-04-publish-0.6.11-release.md)

## 需求或问题

用户希望部署截图里的 ApkStart，以后开发 APK 时可以在电脑上安装和验证。项目已有通用 Android 设备测试说明，本机没有供 Resonance 默认使用的专用设备和启动参数。

## 原因与依据

ApkStart 官方发布的启动器面向 Windows，内置 Android Emulator 和 ADB。当前开发机是 Ubuntu x86_64，已具备这两个 SDK 工具、Android 16 x86_64 系统镜像和可用的 KVM 加速，因此可以直接用模拟器完成本机 APK 验证。

## 具体修改

| 文件或配置 | 入口 | 修改 |
| --- | --- | --- |
| 本机用户的 Android SDK 配置 | `resonance-api36` | 使用现有 Android 16 默认 x86_64 镜像和 Pixel 7 设备定义创建独立 AVD，验证时经 ADB 序列号 `emulator-5560` 访问。 |
| [开发说明](../DEVELOPMENT.md#本机-android-apk-测试) | 本机 Android APK 测试 | 记录 AVD 创建、资源检查、按需启动、APK 试装、截屏、崩溃日志和设备回归的命令。 |
| [仓库规则](../../AGENTS.md)与[协作流程](../AI-WORKFLOW.md) | 本机资源预算、实施与验证 | 将该 AVD 设为当前 Ubuntu 开发机后续 APK 安装、启动及可在模拟器覆盖的回归的默认目标。 |
| [文档索引](../README.md) | 变更记录 | 加入本次环境配置记录。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本机收到一份新 APK | 需自行选设备和启动方式 | 先完成构建，再按说明启动专用 AVD，使用显式 ADB 序列号安装本次 APK，按改动选择启动检查或设备回归。 |

## 平台与数据影响

新增的 AVD 使用独立虚拟设备数据，验收结束后退出，后续按需启动。Android 应用代码、版本号、签名、应用数据格式和同步协议均未修改。本轮试装只用已有正式 APK，未接入真实曲库或真机。当前宿主未安装 ApkStart 的 Windows 启动器。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `emulator -accel-check`、`avdmanager create avd` | Ubuntu x86_64 | 通过。KVM 可用，`resonance-api36` 创建成功，ADB 报告 API 36、ABI `x86_64`。 |
| `sha256sum` 和 `aapt dump badging` | 0.6.11 正式 APK | 通过。SHA-256 与 0.6.11 发布记录一致，包名为 `com.resonance.player`，包含 x86_64 原生库。 |
| SwiftShader 渲染启动及试装 | AVD，宿主总内存上限 3500 MiB | 前两次触及服务内存上限，其中第二次已完成安装和系统报告启动。第三次降低设备内存和分辨率后，安装超时、退出时模拟器中止，未计通过。 |
| 本机 GPU 渲染，ADB `install -r`、`am start -W`、`dumpsys activity`、`screencap`、`logcat -b crash -d` | 同一 AVD，设备 1280 MiB、540×1200、宿主总内存上限 3500 MiB、禁止服务使用 swap | 通过。安装返回 `Success`，冷启动返回 `Status: ok`，等待 7 秒后进程仍在且 `MainActivity` 仍处于前台。截图可见欢迎页，崩溃日志未记录 Resonance 崩溃。宿主服务内存峰值约 3.13 GiB，验证后已关闭模拟器。日志与截图在忽略目录 `build/android-apk-validation/`。 |
| Gradle 构建、instrumentation、真机及实际音频播放 | Android | 本次未执行。本次验收范围为已有 APK 的安装、启动和界面渲染。 |

## 限制与后续

所记录的内存与渲染参数在当前 Ubuntu 开发机核对。首次使用其他开发机时应核对 KVM、GPU 和系统镜像。遇到依赖真实音频硬件、后台系统行为或设备专有功能的改动，仍须按实际设备验证。

## 同步更新的文档

[仓库规则](../../AGENTS.md)、[协作流程](../AI-WORKFLOW.md)、[开发说明](../DEVELOPMENT.md#本机-android-apk-测试)、[文档索引](../README.md)。
