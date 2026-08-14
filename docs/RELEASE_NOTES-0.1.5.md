# Resonance v0.1.5

发布日期：2026-08-14

## 本版本重点

- 新增 Resonance 品牌图标：午夜蓝背景、珊瑚色波形与共振环，延续应用现有视觉语言。
- Android 增加普通、圆形与 Adaptive Icon 资源，兼容不同厂商启动器蒙版。
- Android 应用名称统一通过 `app_name` 资源显示为 `Resonance`。
- Windows 便携 EXE、Setup EXE、MSI 和运行窗口使用同一套品牌图标。
- Windows 产品名、可执行文件名与窗口标题统一为 `Resonance`。
- 版本升级到 `0.1.5`（Android versionCode 6），支持覆盖升级 `0.1.4`。
- 提交品牌母版和可复现的多尺寸图标生成脚本。

## 验证结果

- 15 个测试套件、30 个测试：全部通过。
- Android APK 资源表包含自适应图标前景、背景、普通图标和圆形图标。
- APK 应用名为 `Resonance`，versionCode 6、versionName 0.1.5，v2 签名验证通过。
- MuMu 模拟器覆盖安装成功，应用启动后保持在前台且没有闪退。
- Windows 便携 EXE 与安装器均可提取出新图标。
- Windows ProductName 和窗口标题均为 `Resonance`，Release 便携版实际启动成功。
- Portable ZIP 包含 `Resonance.exe`、`Resonance.ico`、`app` 与完整 `runtime`。

## 发布文件

| 文件 | SHA-256 |
| --- | --- |
| `Resonance-Android-v0.1.5.apk` | `4A3C04F3E6072A4B5DF96FFBDAACAF9C93DD04D54209EAAA649F62DC6551BE9D` |
| `Resonance-Windows-v0.1.5-Portable.zip` | `9E3B9FBBB275E87DF664668ABA3545947BEE45004275971702769218625F2A3D` |
| `Resonance-Windows-v0.1.5-Setup.exe` | `7AE4CF49E22EA8960A3A1032C818D9E9675CC8B5C7EA6B2B7B27422D6D4B9992` |
| `Resonance-Windows-v0.1.5.msi` | `3502BB1EB03ECEFC88E94DC6F673F893AA7A2868A7B3DB62AF35271ED4D49913` |

## 图标源文件

- `assets/branding/resonance-icon-master.png`：完整背景母版，用于 Windows 与传统 Android 图标。
- `assets/branding/resonance-icon-foreground.png`：透明标志母版，用于 Android 自适应前景。
- `scripts/generate_icons.py`：使用 Pillow 生成各 Android density、Adaptive Icon 前景及多尺寸 Windows ICO。

## 已知限制

- Windows 安装程序仍未使用商业代码签名证书，首次运行可能触发 SmartScreen。
- 应用内部功能和 KGMA 支持范围与 v0.1.4 相同；本版本主要是品牌与安装体验更新。
