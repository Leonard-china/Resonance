# 2026-10-16 — 关于页新增官网/充电/爱发电/GitHub，发布 v0.4.5 并同步双平台

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / 发布 / 生产运维
- 适用版本：0.4.4 / 27 → **0.4.5 / 28**（用户明确指示构建发布版并推到 GitHub 和 Leonard.asia）
- 影响平台：共享、Android、Windows、独立官网生产环境
- 关联需求：用户要求在关于页面添加官方网站、为作者充电（包含微信与支付宝收款码）、爱发电地址与 GitHub 地址，并构建发布版推送到 GitHub 和 Leonard.asia

## 需求或问题

1. 原“关于 Resonance”页面仅有应用信息、特色功能和使用技巧，缺少官方网站、充电支持渠道、爱发电链接及 GitHub 代码仓库入口。
2. 用户提供了微信支付与支付宝两张收款码图片，要求在软件内可直接为作者充电，并同步构建正式发布版推送到 GitHub 和官网下载。

## 原因与依据

1. 遵循本地优先与无云端依赖原则，将微信与支付宝赞助收款码无损内置于客户端资源中（Android `res/drawable-nodpi/` 与 Desktop `resources/`），由跨平台接口 [`loadSponsorQr`](file:///d:/Project/Resonance/composeApp/src/commonMain/kotlin/com/resonance/player/platform/DynamicArtwork.kt) 统一加载渲染。
2. 在 `AboutScreen` 中使用标准 `SettingRow` 布局呈现四大官方入口：官方网站 (`https://leonard.asia`)、为作者充电（唤起 `SponsorDialog` 弹窗）、爱发电主页 (`https://afdian.com/a/Leonucs`) 与 GitHub 仓库 (`https://github.com/Leonard-china/Resonance`)；通过 Compose Multiplatform 原生 `LocalUriHandler` 调用系统浏览器或唤起外部应用。
3. `SponsorDialog` 提供微信与支付宝无缝切换标签，展示原版赞助码，并支持直接唤起支付宝手机扫码付款链接及爱发电跳转。
4. 官网 `https://leonard.asia/support` 同步更新收款码 WebP 媒体及爱发电链接。
5. 按照语义化版本规范升至 `0.4.5 / 28`，完成两端签名打包、校验和计算及全网发布。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [DynamicArtwork.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/DynamicArtwork.kt) | `loadSponsorQr` | 声明跨平台收款码加载接口。 |
| [DynamicArtwork.android.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/DynamicArtwork.android.kt) | `actual fun loadSponsorQr` | 从 Android 本地 Drawable 资源动态解码 `sponsor_wechat` 与 `sponsor_alipay`。 |
| [DynamicArtwork.desktop.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DynamicArtwork.desktop.kt) | `actual fun loadSponsorQr` | 从桌面 ClassLoader 资源动态解码并转为 Skia Compose `ImageBitmap`。 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `AboutScreen`、`SponsorDialog` | 关于页新增“官方链接与支持”模块，支持官网、为作者充电弹窗、爱发电与 GitHub 链接跳转。 |
| [support.html](../../website/templates/support.html) | `support-note` | 官网充电页面增加爱发电主页链接。 |
| [version.properties](../../version.properties) | `versionName`、`versionCode` | 升级为 `versionName=0.4.5`、`versionCode=28`。 |
| [RELEASE_NOTES-0.4.5.md](../RELEASE_NOTES-0.4.5.md) | 版本说明与校验和 | 新增 0.4.5 版本说明，包含官方链接、双模式教程及真实产物 SHA-256 校验表。 |
| [release/0.4.5/](../../release/0.4.5/) | `Resonance-0.4.5-release.apk`、`Resonance-0.4.5.exe`、`SHA256SUMS.txt` | 官方归档本次全新构建的双端发布二进制产物。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 关于 Resonance 页面 | 仅有应用信息与特色说明 | 增加官方网站、为作者充电、爱发电主页与 GitHub 仓库入口 |
| 点击“为作者充电” | 无该功能 | 弹出赞助弹窗，可切换查看支付宝与微信高清收款码，支持直接拉起支付宝或打开爱发电 |
| 官网充电页 (leonard.asia/support) | 显示“待添加”占位符 | 展示真实微信与支付宝收款码及爱发电跳转链接 |
| 当前发布版本 | 0.4.4 / 27 | 升级为 0.4.5 / 28，双端签名包就绪 |

## 平台与数据影响

- **跨平台兼容**：Android 与 Windows 双端均可离线查看赞助收款码并正确调用系统浏览器/应用。
- **数据隔离**：本地配置文件与数据格式向后兼容，曲库、歌单与学习进度完整保留。
- **安全性**：赞助码为本地离线静态资源，不产生任何后台请求与隐私上报。

## 验证

| 操作或检查 | 环境 | 结果 |
| --- | --- | --- |
| Kotlin 双端多平台编译 | Gradle / JDK 17 | **通过**：`:composeApp:compileKotlinDesktop` 与 `:composeApp:compileDebugKotlinAndroid` 编译通过。 |
| 桌面单元与回归测试 | Gradle / JDK 17 | **通过**：`:composeApp:desktopTest` 121 项测试全部通过，0 失败。 |
| 正式发布包打包与构建 | Gradle / WiX / Android SDK 36 | **通过**：生成签名 Release APK 与 Windows 安装包 EXE，耗时 5 分 19 秒。 |
| Android APK 签名证书核验 | `apksigner.bat verify -v --print-certs` | **通过**：v2 签名有效，证书 SHA-256 为 `b2b4ad35...`，与官方发布证书完全一致。 |
| 产物清单与校验和生成 | `scripts/release-manifest.ps1 -Version 0.4.5` | **通过**：产物归档至 `release/0.4.5/`，SHA-256 写入 `RELEASE_NOTES-0.4.5.md`。 |
| 本地官网发布导入 | Python 虚拟环境 | **通过**：`manage.py import-release` 成功切至 0.4.5，校验和完全匹配。 |
| 生产官网 (leonard.asia) 部署 | PterodactylClient 容器发布 | **通过**：两端安装包、模板与收款码上传成功，远端平滑重启。 |
| 生产环境在线核验 (leonard.asia) | 公网 HTTPS | **通过**：下载页展示 v0.4.5 及新哈希；支持页展示微信与支付宝收款码及爱发电；两端 Range 下载 206 正常。 |

## 产物与实际校验值

- `release/0.4.5/Resonance-0.4.5-release.apk`
  - 大小：64,564,425 字节（61.57 MiB）
  - SHA-256：`e2f4f8cb0955c4a8a527cea4870109326c5a92fb32c89472880badb5fe525ca1`
- `release/0.4.5/Resonance-0.4.5.exe`
  - 大小：205,389,824 字节（195.88 MiB）
  - SHA-256：`fac33c754b61c7cdd98e8bd11d7ea69928a56545bd282ea78d11ef73d88fdb54`

## 同步更新的文档

- [docs/README.md](../README.md)：新增第 24 项变更记录索引，更新当前工作区版本为 0.4.5 / 28。
- [docs/DEVELOPMENT.md](../DEVELOPMENT.md)：同步当前版本基线。
- [docs/RELEASE_NOTES-0.4.5.md](../RELEASE_NOTES-0.4.5.md)：0.4.5 正式版发布说明。
