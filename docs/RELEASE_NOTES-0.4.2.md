# 2026-10-16 — v0.4.2：聊天输入法智能贴合、底部导航栏避让与歌曲极速下载体验

版本：**0.4.2 / versionCode 25**。本次提供 Android 签名正式版 APK（Android 8.0 / API 26 及以上，包名 `com.resonance.player`）与 Windows 64位独立安装包（`Resonance-0.4.2.exe`）。

## 主要更新

1. **聊天输入法智能贴合与沉浸式交互**：
   - 配置系统级 `adjustResize` 窗口软键盘避让模式，彻底消除全屏 Edge-to-Edge 模式下输入法弹起时顶部标题栏被推飞出屏幕的问题。
   - 键盘弹起时，底部导航栏（BottomNavigationBar）与迷你播放器自动平滑收起隐藏，输入框紧密贴合在输入法软键盘正上方（仅保留 8dp 微距），垂直视窗全部留给聊天消息历史列表。
   - 键盘收起后，底部导航栏即刻平滑恢复，体验顺滑自然。

2. **歌曲极速下载与 B 站 CDN QoS 提速**：
   - 升级媒体流请求 User-Agent 为现代标准 Chrome 浏览器 UA，彻底解除 B 站存储节点对第三方客户端的恶劣速率限制（从几十 KB/s QoS 限速提升至数十 MB/s 高速直连通道）。
   - 文件下载流全面采用 256KB `BufferedInputStream` 与 `BufferedOutputStream` 双向缓冲，大幅提升数据写入吞吐。

3. **后置封面与歌词并发处理**：
   - 音频下载并转码入库后，专辑封面智能检索与歌词获取改为并发执行，并加入 8 秒超时保护，单曲下载全流程耗时缩减 50% 以上，绝不阻塞主任务完成。

4. **FFmpeg 多线程转码加速**：
   - 本地转码命令启用 `-threads 0`，充分调用多核 CPU 算力，加快音频格式转换与元数据写入速度。

## 使用与升级

- **Android**：使用官方正式签名密钥签名，可从历史版本直接覆盖安装升级，曲库、歌单、设置及历史对话数据完整保留。
- **Windows**：运行 `Resonance-0.4.2.exe` 安装，内置完整执行环境与 FFmpeg。
- **数据自治**：保持完全本地优先（Local-First），不设云端账户，所有音频、歌词、封面及会话历史均安全保存在用户设备本地。

## 下载与校验

[GitHub v0.4.2](https://github.com/Leonard-china/Resonance/releases/tag/v0.4.2) 提供 `Resonance-0.4.2-release.apk`、`Resonance-0.4.2.exe` 与 `SHA256SUMS.txt`。校验值由本次实际产物生成。

<!-- GENERATED-CHECKSUMS -->
| 文件 | 大小 | SHA-256 |
| --- | --- | --- |
| `Resonance-0.4.2-release.apk` | 60.87 MiB | `3881be75bae3f78732feac72ec9e2d38f25e524f993b95cee1a40a353ddb5b4f` |
| `Resonance-0.4.2.exe` | 194.74 MiB | `0ca6fdf6e6ab089ec71fd298764b85737368bb25b005d4500b676238017bfeab` |
<!-- END-GENERATED-CHECKSUMS -->
