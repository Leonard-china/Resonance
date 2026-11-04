# 2026-10-31 — 隔离原生验收、记录真实测试结果并重构建 Release

**07:20 最新入口：** 后续修复已在 [R5 本次实际交付产物记录](2026-10-31-13-current-release-artifacts-and-device-verification.md) 重建并重新校验。七项核心、五项音频及反向原生均通过，06:48 启动的两小时验收仍在运行。下方 01:26 的 R3 文件哈希与暂停/失败状态是历史排查记录，不能当作当前最终包或当前测试状态。

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：验证 / 构建，交付进行中
- 适用版本：0.6.5 / 39，不自动升版，不冒充新的公开版本发布
- 影响平台：Android、Windows；仅本机测试工具使用独立目录与应用 ID
- 关联：[聊天](2026-10-31-02-encrypted-local-chat-and-device-pairing.md)、[伙伴直邀](2026-10-31-03-paired-partner-direct-invitations.md)、[寻址](2026-10-31-04-opt-in-signed-partner-rendezvous.md)、[音频中断](2026-10-31-05-keep-room-during-local-audio-interruption.md)、[预取](2026-10-31-06-retire-cancelled-prefetch-and-opt-in-default.md)、[AI](2026-10-31-07-request-final-content-from-deepseek-flash.md)

## 需求或问题

维护者要求使用 MuMu 自主测试，完成软件后构建 Release 并发往自己的微信文件传输助手；宣传片可随后完成。需要长时间原生播放、聊天和中断恢复，不能只靠传输状态测试或历史安装包证明。

## 原因与依据

同一工作区另有 Gradle TLS 探测和同一 MuMu 的仪器测试，曾覆盖共享 XML 报告；反向原生测试在已发声约一分钟后被另一条 instrumentation 启动中止。Android ActivityManager 记录 `start instr` 强制停止原进程，crash buffer 无本次应用异常栈，不能当作已定位到产品崩溃。

Release 初次构建在打包测试编译缓存时无法读取一个生成 class 的模式；尚未确认根因，不擅自归因于路径长度。第二次尝试的 PowerShell 属性参数被拆成任务名，已加引号改正。后续使用短独立输出、独立 project-cache、独立 Gradle 进程和进程内 Kotlin 编译器，避免共享缓存状态干扰。

## 具体修改

| 文件 / 位置 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| 忽略的 `output/together-2026-10-30/night/isolated-build.gradle` | Gradle init | 初始独立 validation 输出，后改为短路径 `output/v31/`；不修改仓库构建默认值或版本 |
| 忽略的 `private-debug.gradle` | `androidComponents.finalizeDsl` | 仅验收 Debug 使用 `.codex` 后缀及 `output/n31/`，与另一组 `.debug` 测试隔离；正式 Release ID 不变 |
| 忽略的 `snapshot_runtime.py` | 原生测试运行时快照 | 复制可变项目 class/JAR/资源，保留不可变依赖缓存引用，独立 JUnit 运行，避免两小时占用 Gradle或后续构建替换测试字节码 |
| [Windows 原生桥接](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherNativeBridgeTest.kt) | opt-in 参数 | 支持两小时以上与三小时原创音频；DPAPI 身份、真实双向证书/聊天确认、周期消息、独立输出目录 |
| [反向桥接](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherReverseNativeBridgeTest.kt) | Windows Guest | 新客户端证书与已送达聊天、独立输出，检查原生暂停/seek/恢复 |
| [Android 原生测试](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherNativePlaybackDeviceTest.kt) | Host/Guest | 指定拥有的转发端口，后台原生播放与加密聊天，本机声音暂停 40–43 秒后恢复；不篡改其它测试映射 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 并行 Gradle 覆盖报告 | 难以确认本轮报告 | 独立输出和逐轮日志 |
| 另一组仪器测试重启 Debug 应用 | 中止本轮会话 | 验收采用独立应用 ID |
| 长时测试期间构建 | 可能替换运行时 class | 使用固定快照，构建可独立继续 |

## 平台与数据影响

仅测试工具及显式 opt-in 样本改变。没有自动升版、公开 GitHub/网站部署或改用户系统安全设置。测试音频为已有原创样本无损循环所得三小时 MP3，172800813 字节，小于 256MiB 单曲上限；不读取用户歌曲或真实签名密钥内容。测试记录、身份和本机工具路径保留在忽略目录。

## 已执行验证

| 本次命令 / 操作 | 结果与证据 |
| --- | --- |
| 完整 desktopTest + Debug/测试 APK + lintRelease | `full-isolated-1.log` 通过，10m24s，262 项中 257 通过、5 显式跳过；先于最终预取/收件人绑定/AI小改，不能混作最终全量结果 |
| together 全组 + 播放驱动 + 状态恢复单测；Debug/测试 APK/lint | `followup-isolated-2.log` 通过，3m40s；41 项中 36 通过、5 显式跳过；含实际 Java→Python 寻址互通、新预取中止、声音暂停及错误收件人拒绝 |
| MuMu 最新聊天/伙伴/入口七项 | `companion-mumu-3.log` 全部通过，12.222s；前两轮失败与修正分别保留日志 |
| MuMu Host→Windows 原生 Guest | `reverse-native-*-1.log` 被另一 instrumentation 中止；原生发声和聊天已发生，但完整测试失败，不写成通过 |
| 最终 Release 第一次 | `final-release-1.log` 编译缓存打包失败 |
| 最终 Release 第二次 | `final-release-2.log` PowerShell 属性参数错误，19s 失败 |
| 最终 Release 第三次 | `final-release-3.log` 正在执行 AI 回归、双 Android 单测、lint、签名 APK、Windows EXE；结果待补 |
| 独立 `.codex` 验收包 | `private-debug-1.log` 正在构建；之后重测反向和两小时会话 |

当前正式包尚未完成本次验签、哈希、启动和文件传输助手交付。旧 10 月 30 日产物不能冒充本次最终产物。

### 01:26 本次最终构建补充

`final-release-3.log` **BUILD SUCCESSFUL，12m42s，106 tasks**。本次 MusicAiWireTest 1 项及 MusicAssistantTest 34 项全部通过；Android Debug/Release 单元测试、Release lint、签名 APK 和 Windows EXE 构建均完成。实际产物如下，不复用旧值：

| 产物 | 字节数 | SHA-256 |
| --- | --- | --- |
| `output/v31/composeApp/outputs/apk/release/Resonance-0.6.5-release.apk` | 167186111 | `D4484481DB6122D3F5E6028BA0C9ADD7E6ADAB6EDB35BFC97DD8B2486101125B` |
| `output/v31/composeApp/compose/binaries/main-release/exe/Resonance-0.6.5.exe` | 322150912 | `9CE888B172D453A29E46D1822E96D21846B86CD486923DB77A2E2670DC36383D` |

`apksigner verify --verbose` 本次通过，v2 签名、一个签名者；Windows EXE 的 Authenticode 状态为 NotSigned。两端安装/启动、独立长时原生验收及微信传输仍待完成；构建通过不是已交付。

暂停前最后状态：专属 `.codex` 包串行重建通过（1m32s），监督流程已安装并进入反向原生步骤，但 Android Host 在端口就绪前退出，流程按设计停止；两小时测试尚未开始。实际原因需从最新 `native-isolated-*/reverse-mumu.log` 继续定位，不能把这个新失败归给前一次外部 instrumentation 干扰。额度到 92% 后按维护者要求保存进度，五小时后续做。

## 限制与后续

01:23 后续状态：独立验收包的首次并发构建发生 JVM 原生内存分配失败（约 306MB），已确认机器 16GB 内存、当时可用约 1.8GB。未改系统设置；保留失败日志，排队等待当前 Release 构建结束后，以 2GB 堆/两 worker 串行重试。忽略目录监督脚本已启动，等待专属 APK 成功后顺序执行七项核心、反向原生及两小时测试；失败即停并保存状态，当前尚不能声称两小时测试通过或已经开始。

补齐独立应用的双向原生与长时结果、实际最终产物验签哈希、Windows 启动和微信传输。没有实体手机，不能把 MuMu 当作电话/蓝牙/真实移动网络或物理音频同步精度证明。宣传片的真实 AI 配置/回答和拍摄仍待完成，软件不等影片。

## 同步更新的文档

`docs/README.md` 索引及各功能记录的本次验证状态；长期接续点在忽略目录 `CONTINUE.md`，没有写入用户密钥。
