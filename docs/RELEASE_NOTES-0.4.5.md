# 2026-10-16 — v0.4.5：关于页新增官方网站/为作者充电/爱发电/GitHub，集成双模式新手教程

版本：**0.4.5 / versionCode 28**。本次提供 Android 签名正式版 APK（Android 8.0 / API 26 及以上，包名 `com.resonance.player`）与 Windows 64位独立安装包（`Resonance-0.4.5.exe`）。

## 主要更新

1. **“关于 Resonance”新增官方生态与充电赞助支持**：
   - **官方网站**：直达 [leonard.asia](https://leonard.asia)，随时浏览独立官网、参与匿名论坛讨论并获取最新发布资讯。
   - **为作者充电**：集成微信支付与支付宝双通道收款码弹窗，原生渲染高清赞助码，支持一键直接拉起支付宝扫码支付。
   - **爱发电主页**：直达 [afdian.com/a/Leonucs](https://afdian.com/a/Leonucs)，支持广大乐迷通过爱发电赞助独立作者持续演进。
   - **Github地址**：直达 [github.com/Leonard-china/Resonance](https://github.com/Leonard-china/Resonance)，查阅完整开源代码、提交 Issue 与参与 pair programming 协作。

2. **首次启动双模式使用教程（快速了解 / 跟着操作）**：
   - 首次打开应用时提示选择“快速了解”（四页图文速览曲库、助手、B 站导入与歌词）或“跟着操作”（交互式真实控件教学），亦可随时跳过。
   - 快速图文模式专注核心理念与操作直觉，手机与桌面端均适配清晰的原生示意图。

3. **全流程真实界面交互教学与引导**：
   - 提供 7 大核心章节交互操作：曲库与播放、音量平衡、歌词与双语翻译、歌单管理、AI 音乐助手、B 站导入与扫码登录、推荐生命周期。
   - 教学遮罩高亮真实控件，支持穿透点击与滚动定位；真实检测操作完成状态并自动推进；支持随时收起卡片、跳过步骤或暂停教学，保护真实音频与付费模型调用安全。

4. **设置页教程目录与学习进度持久化**：
   - 设置页顶部常驻“使用教程”入口，跳过或完成后可随时回访各章节离线图文与常见问题解答。
   - 本地独立保存学习进度（已完成章节与阅读位置），支持随时继续未完章节或重新练习。

5. **B 站账号全端扫码与 Android 网页快捷登录**：
   - 原生矢量二维码支持全端扫码自动捕获并持久化 Cookie；Android 独享原生 WebView 安全弹窗快捷登录。

6. **下载与多项界面细节体验优化**：
   - 优化软键盘弹起与输入框贴合、修复导出歌单偶现的封面丢失异常、提升歌曲并发下载效率。

## 使用与升级

- **Android**：使用官方正式签名密钥签名，可从历史版本直接覆盖安装升级，曲库、歌单、设置及历史对话数据完整保留。
- **Windows**：运行 `Resonance-0.4.5.exe` 安装，内置完整执行环境与 FFmpeg。
- **数据自治**：保持完全本地优先（Local-First），不设云端账户，所有音频、歌词、封面及会话历史均安全保存在用户设备本地。

## 下载与校验

[GitHub v0.4.5](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.5) 与官网 [leonard.asia/downloads](https://leonard.asia/downloads) 提供 `Resonance-0.4.5-release.apk`、`Resonance-0.4.5.exe` 与 `SHA256SUMS.txt`。校验值由本次实际产物生成。

<!-- GENERATED-CHECKSUMS -->
| 文件 | 大小 | SHA-256 |
| --- | --- | --- |
| `Resonance-0.4.5-release.apk` | 61.57 MiB | `e2f4f8cb0955c4a8a527cea4870109326c5a92fb32c89472880badb5fe525ca1` |
| `Resonance-0.4.5.exe` | 195.88 MiB | `fac33c754b61c7cdd98e8bd11d7ea69928a56545bd282ea78d11ef73d88fdb54` |
<!-- END-GENERATED-CHECKSUMS -->
