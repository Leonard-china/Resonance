# 2026-11-04 — 恢复浅色 UI 预览并独立运行本地服务

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复 / 本地预览配置
- 适用版本：0.6.5 / 39，未升版
- 影响平台：本机 Linux 预览；不改变原生 Android / Windows / Linux 应用
- 关联：[浅色方案与原型](2026-11-04-02-light-ui-proposal-and-html-prototype.md)

## 需求与原因

用户截图显示 `127.0.0.1:8765` 返回 `ERR_CONNECTION_REFUSED`。检查 `ss -ltnp 'sport = :8765'`，无监听进程。此前预览通过工具命令会话启动，未配置独立的服务生命周期；本次检查确认服务已停止，但未取得原进程终止日志，不断言具体终止原因。

## 具体修改

新增 [start-preview.sh](../../design-system/resonance/proposals/2026-11-04-light-glass/start-preview.sh)，通过 `systemd-run --user` 启动 `resonance-light-ui-preview.service`。使用脚本所在目录、系统 Python、127.0.0.1:8765，仅提供方案静态文件；重复启动复用当前服务，异常退出可重启。更新提案 README 的启动、停止与生命周期说明。

## 前后行为

此前服务停止后两处预览链接均拒绝连接。现在由独立用户服务运行，原型、总览、脚本和图片均返回200；已在用户应用内浏览器重新打开总览，看到78个页面/状态的目录内容。

## 平台与数据影响

无公网发布、权限扩大、真实音频或凭据读取，无 Kotlin、版本、存储、配置或同步包变更。仅本机回环端口。服务不是开机自启动，用户退出登录或重启电脑后可能停止；届时重跑脚本即可。Windows及其他平台仍可解压后直接打开 HTML。

## 验证

- `bash -n start-preview.sh`：通过。
- 启动脚本及再次运行：通过；第二次提示预览已运行。
- `systemctl --user show ... -p ActiveState -p SubState -p Restart`：active / running / on-failure。
- Python urllib 请求 index.html、gallery.html、app.js、示例图片：全部200。
- 应用内浏览器新开 gallery.html：正常显示目录，页面已保留；旧错误页自动化绑定因其 data URL 被工具策略拒绝，改为直接打开已授权的本地HTTP地址，未改变浏览器安全设置。
- 未执行原生构建或回归；没有应用源码变更。

## 限制与文档

本次未验证机器重启后持续运行，也未配置开机启动。服务只在当前电脑可访问。更新 docs/README.md 变更索引、docs/DEVELOPMENT.md 预览启动说明及提案 README。
