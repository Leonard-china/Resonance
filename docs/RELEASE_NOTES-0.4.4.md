# 2026-10-16 — v0.4.4：支持 B 站扫码登录与手机网页快捷登录

版本：**0.4.4 / versionCode 27**。本次提供 Android 签名正式版 APK（Android 8.0 / API 26 及以上，包名 `com.resonance.player`）与 Windows 64位独立安装包（`Resonance-0.4.4.exe`）。

## 主要更新

1. **B 站账号二维码扫码登录（全端通用，Desktop & Android）**：
   - 在“音乐设置 - B 站账号”卡片中新增“扫码登录”能力。
   - 调用官方登录网关自动获取授权二维码，采用 ZXing 点阵结合 Compose Canvas 原生矢量渲染，零平台 Bitmap 依赖，清晰锐利。
   - 支持实时轮询扫码状态（等待扫码、已扫码待确认、超时失效与一键刷新）。
   - 确认登录后自动提取并合并 `Set-Cookie` 票据，自动填入并持久化保存，彻底免除手动抓包复制 Cookie 的繁琐操作。

2. **手机端专属网页快捷登录（Android）**：
   - 针对 Android 用户新增“网页快捷登录”功能，通过全屏弹窗内嵌官方 H5 安全登录界面。
   - 支持短信验证码登录、账号密码登录或客户端授权。
   - 登录成功时自动由系统 `CookieManager` 捕获 `SESSDATA`、`bili_jct`、`DedeUserID` 等关键凭据并安全写回配置，弹窗自动完成并关闭。

3. **设置体验与凭据管理优化**：
   - 保留手动 Cookie 输入与粘贴作为灵活兜底。
   - 增加“清除 Cookie”快捷按钮与状态清晰提示。

## 使用与升级

- **Android**：使用官方正式签名密钥签名，可从历史版本直接覆盖安装升级，曲库、歌单、设置及历史对话数据完整保留。
- **Windows**：运行 `Resonance-0.4.4.exe` 安装，内置完整执行环境与 FFmpeg。
- **数据自治**：保持完全本地优先（Local-First），不设云端账户，所有音频、歌词、封面及会话历史均安全保存在用户设备本地。

## 下载与校验

[GitHub v0.4.4](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.4) 提供 `Resonance-0.4.4-release.apk`、`Resonance-0.4.4.exe` 与 `SHA256SUMS.txt`。校验值由本次实际产物生成。

<!-- GENERATED-CHECKSUMS -->
| 文件 | 大小 | SHA-256 |
| --- | --- | --- |
| `Resonance-0.4.4-release.apk` | 61.09 MiB | `1553d59557ac71bdcbba2f2bec024221ff1ebbe9a5061f4387fe3c1c4c3bb59e` |
| `Resonance-0.4.4.exe` | 195.32 MiB | `b918276c6107e3be07ca44072c99ab250397d2b178df108b86989a736b1e1323` |
<!-- END-GENERATED-CHECKSUMS -->
