# Resonance 浅色 UI 方案

直接用浏览器打开 [index.html](index.html) 试用，或打开 [gallery.html](gallery.html) 看全部页面图片。素材已放在本目录，可离线打开；请保留目录结构。

- [方案说明](PLAN.md)
- [页面与源码覆盖表](COVERAGE.md)
- `screenshots/`：78个页面与关键状态 × 桌面/手机，共156张PNG。
- `contact-01.png` 至 `contact-08.png`：分页总览。
- `capture-report.json`、`interaction-report.json`、`motion-report.json`：截图、交互与切页验证结果。

这是 HTML 设计演示，不是原生运行截图。无实际声音、下载、账号登录或文件操作。

重新截图：安装 Playwright 与 Chromium 后，运行 `node capture.cjs`。
交互检查：`node verify.cjs`；切页检查：`node verify-motion.cjs`。可用环境变量 `PLAYWRIGHT_MODULE` 指向已有 Playwright 模块；不把本机工具路径提交到脚本。

Linux 本地后台预览：运行 `bash start-preview.sh`，再打开 http://127.0.0.1:8765/index.html 或 gallery.html。使用独立 systemd 用户服务，重复运行不会重复启动。停止：`systemctl --user stop resonance-light-ui-preview.service`。不配置开机自启动；退出登录或重启后如停止，请重跑脚本。离线直接打开 HTML 的方式继续有效。

切页体验（2026-11-04修订）：新旧内容同步交接，280ms / 12px；导航和播放条保留现有节点。快速往返从当前画面接续，尚未退场的页面保留输入与滚动位置，结束后回收旧页。这里只在转场期间保留旧页，不构成跨页面持久状态缓存。
