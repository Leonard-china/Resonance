# 2026-10-16 — 支持 B 站扫码登录与 Android 网页快捷登录自动获取 Cookie

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能
- 适用版本：0.4.4（versionCode 27）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：用户反馈手动配置 B 站 Cookie 流程过于繁琐，建议提供二维码或网页登录直接自动获取 Cookie。

## 需求或问题

用户在播放器中通过 B 站导入高码率音频时，需要在设置中配置自己的 B 站 Cookie。此前仅提供纯文本输入框，用户必须自行在外部抓包或从浏览器控制台中复制 Cookie 并粘贴，流程极繁琐且极易出错。

## 原因与依据

1. B 站官方提供标准的扫码授权网关：
   - 生成接口：`https://passport.bilibili.com/x/passport-login/web/qrcode/generate`，下发 `qrcode_key` 与授权链接 `url`。
   - 轮询接口：`https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=...`，返回状态码：86101（未扫码）、86090（已扫码未确认）、86038（二维码失效）、0（登录成功并在响应头下发 `Set-Cookie`）。
2. 在 Compose Multiplatform 中，二维码可以使用 ZXing 原生计算出 module bit matrix，通过 Compose `Canvas` 使用矢量点阵渲染，不需要任何平台私有 Bitmap 类，全平台（Windows 与 Android）完全统一复用。
3. 在 Android 手机上，通过内嵌 WebView 加载官方 H5 登录页（`https://passport.bilibili.com/h5-app/passport/login`），用户可以通过短信验证码、密码或客户端快捷授权登录；登录完成后由 `CookieManager.getInstance().getCookie("https://bilibili.com")` 自动提取 `SESSDATA`、`bili_jct`、`DedeUserID` 等凭据。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [composeApp/build.gradle.kts](../../composeApp/build.gradle.kts) | `jvmMain.dependencies` | 添加 `com.google.zxing:core:3.5.3` 依赖，用于解析生成二维码点阵。 |
| [MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt) | `BiliQrSession`, `BiliQrPollStatus`, `BiliQrPollResult` | 声明扫码会话模型、轮询状态枚举与轮询响应数据。 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | `PlatformServices.supportsWebLogin` | 新增平台能力属性（默认 `false`）。 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `override val supportsWebLogin` | Android 平台实现声明为 `true`。 |
| [MusicAssistantServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt) | `createBilibiliQrSession`, `pollBilibiliQrStatus` | 声明二维码生成与状态轮询的业务层接口。 |
| [BilibiliLoginService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/media/BilibiliLoginService.kt) | `BilibiliLoginService` | 封装 B 站登录 API 请求、ZXing 二维码矩阵点阵生成、轮询状态解析及 `Set-Cookie` 提取合并。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `JvmMusicAssistant` | 对接 `BilibiliLoginService`，实现二维码创建与轮询接口。 |
| [BilibiliLoginDialogs.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/BilibiliLoginDialogs.kt) | `BilibiliQrLoginDialog`, `expect BilibiliWebLoginDialog` | 共享 Compose 扫码弹窗（Canvas 绘制二维码、轮询协程、状态提示、刷新与成功回调）及 Web 弹窗 `expect` 声明。 |
| [BilibiliWebLogin.desktop.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/ui/BilibiliWebLogin.desktop.kt) | `actual BilibiliWebLoginDialog` | Desktop 端空实现占位。 |
| [BilibiliWebLogin.android.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/ui/BilibiliWebLogin.android.kt) | `actual BilibiliWebLoginDialog` | Android 端通过 `AndroidView` 内嵌官方 WebView，自动监听 `CookieManager` 捕获 `SESSDATA` 凭据。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `SettingsDialog` | 增加“扫码登录”与“网页快捷登录”按钮，支持自动填入与清除 Cookie。 |
| [BilibiliLoginServiceTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/media/BilibiliLoginServiceTest.kt) | `BilibiliLoginServiceTest` | 针对二维码生成、各种轮询状态及 Cookie 提取合并的单元测试。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 配置 B 站账号凭据 | 只能手动在外部抓包，复制长串字符串粘贴到输入框 | 支持点击“扫码登录”由手机 B 站 App 扫一扫确认登录，或在 Android 手机上点击“网页快捷登录”直接完成登录，凭据自动提取并保存 |
| 二维码失效或网络刷新 | 不支持 | 自动提示失效状态并提供“刷新二维码”按钮，重新拉取并重置轮询 |
| Cookie 清除与重置 | 需手动光标全选删除长字符串 | 提供快捷“清除 Cookie”按钮 |

## 平台与数据影响

- **共享逻辑**：二维码会话与轮询协议两端通用，ZXing 点阵以 `BooleanArray` 传递给 Compose `Canvas` 渲染，零平台图形差异。
- **Android**：支持扫码登录与基于系统 WebView 的网页快捷登录；`CookieManager` 仅在用户主动触发登录时与官方 `bilibili.com` 交互。
- **Windows**：支持扫码登录，不支持也不依赖 WebView。
- **数据与安全**：提取后的 Cookie 遵循既有加密管线安全保存在设备本地，不上传任何第三方。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest` | Windows 11 / JDK 17 | 通过，`BilibiliLoginServiceTest` 全场景用例通过 |
| `.\gradlew.bat :composeApp:compileReleaseKotlinAndroid :composeApp:lintRelease` | Windows 11 / Android SDK 36 | 通过，编译与 Lint 0 错误 |
| `.\gradlew.bat :composeApp:allTests` | Windows 11 / JDK 17 | 通过，全量回归套件执行通过 |

## 限制与后续

本次范围内无已知未完成项。

## 同步更新的文档

- `docs/README.md`：登记变更索引。
- `docs/DEVELOPMENT.md`：同步更新相关平台接口与架构说明。
