# 2026-11-01 — 为所有安装用户接入自有服务器的 WSS 中继

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.6.5（39），未升版
- 影响平台：共享 / Android / Windows；中继另为独立 Python 服务
- 关联：[产品审计](../PRODUCT_AUDIT.md)、[本轮结果](../OPTIMIZATION_REPORT.md)
- 路径缩写：C = composeApp/src/commonMain/kotlin/com/resonance/player；J = 对应 jvmMain。

## 需求或问题

用户指定所有安装用户默认使用其服务器。只读确认官网只有一个分配端口，既有 raw TCP/TLS relay 不能直接通过当前 HTTP Tunnel。需要保留官网与原房间加密协议，并减少连接配置负担。

## 原因与具体修改

- `C/platform/ListenTogetherService.kt::TogetherDefaults`、AndroidTogetherRuntime、DesktopPlatformServices：生产默认 WSS URL；TogetherCompanionStore 仅在无已保存配置时使用默认，显式关闭/自定义保留；测试默认不访问公网。
- `J/together/TogetherWebSocket.kt`：基于已存在 OkHttp 的有界二进制流 Socket，超时/反压/取消；`TogetherRelayTransport` 验证 WSS 路径并在其上运行原证书锁定房间 TLS；保留 HTTPS 原始 TLS 模式。
- `JvmListenTogetherService`：保留 socket/generation 释放语义；失败递增间隔、最多 5 次，等待槽正常到期仅续订有效邀请；停止后刷新邀请可重试。TogetherCompanionSession 识别无密钥 WSS 配置。
- `services/relay/websocket_server.py`、requirements、run-public-relay.sh：独立回环服务，限并发、单 IP、帧、时长、会话流量和持久每日预算；日志仅错误类型，不包含邀请/消息/地址。
- `test_websocket_server.py`、integration_fixture.py、TogetherWebSocketTest：Python 真 WebSocket 往返/上限/清理；JVM 端点校验、默认配置兼容、Python 回环上的真实内层 TLS 往返。
- 部署文件及回滚见 `services/relay/DEPLOYMENT-leonard.md`。

## 修改前后与平台、数据影响

**未部署、未发布，公网与 Android Conscrypt 未验收。** 新版邀请 WSS 地址需要双方使用支持版本；旧私有服务仍可用。服务器可见网络元数据，协调设备仍可读群消息，不宣称无条件匿名或协调者不可见加密。

拟新增回环 45902 及 Tunnel 专用路径，保留官网 50034 与数据。2 GiB/UTC 日是应用负载保护上限，不是计费硬限制；所有用户默认使用不代表承诺无限容量。生产进程/路由改动需用户批准并核实 Cloudflare 权限。

## 验证

完整 `:composeApp:desktopTest` 中 `TogetherWebSocketTest` 3/3 通过，真实 Python WebSocket + 内层双向证书 TLS；原有寻址 Python/JVM 联测也通过。`python -m unittest discover -s services/relay -p 'test_*.py' -q` 6/6 通过；`bash -n services/relay/run-public-relay.sh` 通过。Ubuntu 守护、公共 WSS 证书、跨网及 Android Conscrypt 未验收。

最终共同命令：`./gradlew.bat :composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain`，成功；桌面 276 通过 / 4 条件跳过 / 0 失败，Android Lint 0 errors / 110 warnings。

本次详细命令、数量、截图与限制记录在 [优化报告验证台账](../OPTIMIZATION_REPORT.md#验证台账)，不使用旧产物充当本轮通过。中间编译失败与修正也在台账保留。

## 限制与后续

上述未验收项保持待办；没有修改生产数据、签名配置、版本号或发布产物。检查点为 `.git/audit-checkpoints/20261101-203513`，只按本轮变更文件回退，不能丢弃此前脏工作区。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 当前状态、`docs/OPTIMIZATION_REPORT.md`；服务变更另更新 relay README 与部署说明。
