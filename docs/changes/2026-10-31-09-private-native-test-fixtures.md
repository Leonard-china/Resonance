# 2026-10-31 — 原生跨端验收素材改为应用私有目录

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：测试修复
- 适用版本：0.6.5 / 39，正式功能与版本不变
- 平台：Android 仪器测试及 Windows 本机监督脚本
- 关联：[隔离原生验收](2026-10-31-08-isolated-native-validation-and-release-build.md)

## 需求或问题

独立 `.codex` 验收包反向原生测试在读取 `host.mp3` 时失败。ADB 能列出外部文件，但 `run-as com.resonance.player.codex` 访问同一目录得到 Permission denied。此前不能归因为真实播放器崩溃或传输失败。

## 原因与依据

音频由 shell 写到外部应用目录，受 MuMu/Android 分区存储与所有权约束。测试首先检查文件存在，尚未创建房间便失败；正式功能没有进入该链路。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [TogetherNativePlaybackDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherNativePlaybackDeviceTest.kt) | Host/Guest 的 native-bridge 目录 | 改用 `context.filesDir`，音频、临时邀请、端口与追踪文件只在测试应用私有目录 |
| 忽略的 `run_isolated_native.py` | `put_private`、`get_private` | 最终使用 ADB push 随机暂存文件后由 `run-as` 复制并核对字节数，exec-out 读取；仅传递本轮测试文件，不读取凭据，不打印邀请内容 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| shell 创建的外部素材 | 应用可能无权读取 | 以应用 UID 写入自己的私有测试目录 |
| 取回临时邀请与追踪 | 依赖外部存储访问 | 只读写测试私有文件，避免外部目录权限差异 |

## 平台与数据影响

只修改仪器测试，不改变正式 APK/EXE 代码或已生成产物。使用独立 `.codex` 应用，不清理用户音频、伙伴或 AI 配置；原始原创音频只复制到本轮测试目录。不添加存储权限或修改系统安全设置。

## 验证

ADB 外部目录可见文件 19200813 字节，应用 UID 访问外部目录 Permission denied；改用私有 dd 写入后返回码与字节数见本次运行输出。测试 APK 重建日志 `output/together-2026-10-30/night/private-test-fixture-2.log`；双向原生和两小时结果待补，不将素材可读等同于播放通过。

本次再发现 Windows 下 `adb shell -T` 直接读磁盘 stdin 只写入 107 字节，虽返回 0，不能当作成功。改用 `exec-in` 和二进制 PIPE，并逐次核对远端文件字节数；65536 字节、包含全部 0–255 字节的探针往返完全一致。测试 APK 重建通过（57s），新监督运行日志 `native-supervisor-2.log`。

大文件进一步验证后，exec-in 的 64KB 探针不能代表完整素材：19200813 字节文件仍只见 19136512 字节，`native-supervisor-2` 因逐次字节校验而正确中止。最终使用 ADB 文件传输到随机命名的 shell 临时文件，再由应用 UID 复制进私有目录，立即删除暂存文件；核对私有文件为完整 19200813 字节。临时内容仅为本轮原创测试素材/临时测试邀请，不放用户模型凭据。`native-supervisor-3.log` 已进入真实反向原生播放，完整结果仍待补。

## 限制与后续

07:20 续作：本次 `native-isolated-20261031-064404` 的反向原生两端均通过，独立核心七项与音频五项也通过；两小时测试仍在运行，见[当前验收记录](2026-10-31-13-current-release-artifacts-and-device-verification.md)。此前素材权限与截断失败保留为排查事实。

继续跑真实 MediaSessionService/ExoPlayer 与 Windows PCM 双向桥接，再跑两小时测试。正式包仍需安装/启动和微信交付验收。

## 同步更新

`docs/README.md` 变更索引；正式存储架构未改变，无需改写产品数据章节。
