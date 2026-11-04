# 2026-11-01 — 修复一起听群聊与单聊路由、客户端消息丢包并完成双端全量测试与 Release 交付

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：Bug 修复 / 构建验证
- 适用版本：0.6.5 / 39（保持不变，不升版）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：[2026-11-01-08-antigravity-validation-handoff.md](2026-11-01-08-antigravity-validation-handoff.md)

## 需求或问题

在接手并对此前未构建运行的多人一起听、群聊逐成员回执、原生协调端控制及 AI 歌词等变更执行全量构建与测试时，发现并解决了以下关键测试阻塞与运行时问题：
1. `UiVisualContractTest` 在反射代理 `ListenTogetherService.getTogetherNotificationsBlocked()` 时发生 `ClassCastException`，原因是代理中错误返回了布尔值 `false` 而非期望的 `StateFlow<Boolean>`。
2. `App.kt` 的 `resolvePlaybackQueue` 在面对空队列或非可播放自定义队列项时未能平滑回退至完整曲库，导致 `PlaybackQueueResolutionTest` 断言失败。
3. `TogetherSessionTest` 中存在无用且重复的 `when` 分支 `"mediaError"`，且有重连测试等待条件未适配带序号的状态推进。
4. `ListenTogetherTest` 的容量拒绝测试用例断言未匹配当前 8 人（1 协调者 + 7 访客）上限。
5. `TogetherCompanionTest.realTlsChatPairingLocalClearAndStableDeviceIdentityAcrossNewRooms` 发生超时失败：
   - 访客在 1 对 1 房间发送的聊天消息未能成功标记为送达（`delivered = false`），协调端也未收到该消息。
   - 分析发现：`TogetherCompanionSession` 的发送队列通道循环在检查待发消息是否存在时，使用了 `state.value.conversationId.ifEmpty { state.value.peerId }`（即房间临时 UUID `roomId`），而 1 对 1 聊天在 `sendText` 和 `flush` 时均存放在 `chatTarget(state.value)`（即持久伙伴设备 ID `peerId`）中。二者不一致导致发送循环误判消息已失效并丢弃，网络层未实际发出该包。

## 原因与依据

1. **测试反射代理类型不匹配**：在 `UiVisualContractTest.kt` 中，`getTogetherNotificationsBlocked` 的返回类型已重构成 `StateFlow<Boolean>`，但代理仍返回原生 `false`。
2. **播放队列安全回退**：当自定义队列为空或没有任何一项在曲库中可播放时，必须安全降级到曲库完整列表，避免播放器驱动因空队列停滞。
3. **单聊 vs 房间群聊存储键隔离与发包一致性**：
   - 为使 1 对 1 伙伴的聊天记录在不同房间会话间保持连续且支持单端独立清空，1 对 1（成员数 $\le 2$）的消息存放在 `peerId`（设备公钥指纹）下；三人及以上的群聊（成员数 $> 2$）消息存放在 `conversationId`（房间临时 UUID）下。
   - `TogetherCompanionSession` 内部的 `packets` 发送循环必须统一调用 `chatTarget(state.value)` 进行消息检索，否则在 1 对 1 房间中因检索键不匹配而返回 `sender = null`，导致消息在本地静默丢弃。
   - `recordIncoming` 和 `markRead` 亦同步调整，使单聊和房间维度的未读提醒都能正确匹配并即时归零。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `visualContract_togetherStateWiring` | 将 `getTogetherNotificationsBlocked` 的代理返回值修正为 `MutableStateFlow(false)`，消除类型转换异常。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `resolvePlaybackQueue` | 强化空自定义队列及非可播放自定义队列的回退逻辑，确保有效降级至 `fullLibraryQueue`。 |
| [TogetherSessionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherSessionTest.kt) | `TogetherSessionTest` | 移除重复的 `"mediaError"` 分支；更新重连断言等待状态为 `Connected` 且序列号递增。 |
| [ListenTogetherTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/ListenTogetherTest.kt) | `roomCapacityRejection...` | 修正断言逻辑，与当前系统 8 人容量（7 访客成功加入，第 8 位被拒绝）保持一致。 |
| [TogetherTransport.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt) | `publicKeyId` | 将 `publicKeyId` 访问级别开放，以便各模块统一复用设备公钥 ID 提取逻辑。 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `sendChat`, `acceptPeer`, `reportGroupDelivery` | 1 对 1 聊天在 `attachedPeer` 与 `roomId` 间安全路由；重连访客基于恢复令牌恢复原本成员 ID；对已发送音频去重；多处状态重置加入同步保护。 |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `chatTarget`, `packets`, `recordIncoming`, `markRead` | 统一通过 `chatTarget` 获取当前聊天检索键；修复发送队列循环丢包问题；支持清空与未读计数对单聊和房间的双向同步。 |
| [release-manifest.ps1](../../scripts/release-manifest.ps1) | `release-manifest` | 执行产物复制、生成真实 SHA256SUMS.txt，并更新 RELEASE_NOTES-0.6.5.md。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 1 对 1 一起听房间发送聊天消息 | 访客调用 `sendChat` 后消息仅入库，因发送循环检查键不匹配被丢弃，房主收不到消息，访客端一直显示待送达 | 访客发送的消息正常经 TLS 链路到达房主并收到回执，双端消息均标记为 `delivered = true` |
| `desktopTest` 运行 | 存在多处类型、断言与超时错误，全量测试无法通过 | 265 项测试全部通过（260 通过，5 忽略，0 失败），耗时约 4 分 53 秒 |
| Android Lint 与编译 | 源码未执行完整验证 | `lintRelease`、`assembleDebug`、`assembleDebugAndroidTest` 均编译通过 |
| Release 产物与校验 | 无当前源码的 Release 产物或仅有旧构建 | 成功构建 Android Release APK 与 Windows Release EXE，签名及 SHA-256 校验真实有效 |

## 平台与数据影响

- **共享逻辑**：仅调整会话消息调度与回退安全逻辑，完全兼容现有数据结构与离线偏好设置。
- **Android**：无额外权限要求，Release APK 构建与 V2 签名正常通过。
- **Windows**：桌面测试全绿，打包 Release Distributable 及安装器 EXE 成功。EXE 维持未签名（NotSigned）状态。
- **网络与凭据**：不读取、不打印任何真实 keystore 密码或 API 密钥。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --no-configuration-cache --max-workers=2` | Windows 10 x64, OpenJDK 17 | **通过**。265 项测试全部完成（260 成功，0 失败，5 跳过）。 |
| `.\gradlew.bat :composeApp:lintRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache --max-workers=2` | Android SDK 36.0.0 / AGP | **通过**。Lint 及 Debug APK、测试 APK 构建成功。 |
| `.\gradlew.bat :composeApp:assembleRelease --no-configuration-cache --max-workers=2` | Windows 10 x64 | **通过**。生成 `Resonance-0.6.5-release.apk`。 |
| `.\gradlew.bat :composeApp:createReleaseDistributable :composeApp:packageReleaseExe --no-configuration-cache --max-workers=2` | Windows 10 x64, WiX 3.11 | **通过**。生成 `Resonance-0.6.5.exe` 与绿色运行时目录。 |
| `apksigner.bat verify --verbose release\0.6.5\Resonance-0.6.5-release.apk` | Android build-tools 36.0.0 | **通过**。Verified using v2 scheme (APK Signature Scheme v2): true。 |
| `Get-AuthenticodeSignature release\0.6.5\Resonance-0.6.5.exe` | Windows PowerShell | **通过**。状态确认为 `NotSigned`（据实记录，未签名）。 |
| `powershell -File scripts/release-manifest.ps1` | Windows PowerShell | **通过**。校验清单已更新到 `release/0.6.5/SHA256SUMS.txt` 与 `docs/RELEASE_NOTES-0.6.5.md`。 |

## 限制与后续

1. **群聊信任模型与中继限制**：
   - 当前多人房间采用“每成员到协调设备的 TLS”拓扑，协调设备是合法参与者且在转发过程中能够读取群消息，尚未接入点对点群签名与防篡改验证。
   - 协调设备退出即意味着房间解散，不支持协调设备自动平滑移交给其他访客成员。
2. **跨网 NAT 穿透边界**：
   - 现阶段直连基于局域网及带公网 IPv6/IPv4 的直连候选；在双端均处于对称型 NAT（Symmetric NAT）的真实跨网环境下，直连将不可达。
   - 跨网成功依赖用户配置的可信加密字节中继（`relayUrl`），无公共中央中继部署。
3. **真实设备运行验证限制**：
   - 本机 MuMu 模拟器当前未在后台运行或监听（16384 拒绝连接），真机无连接；因此 Android 端的 Native UI 仪器测试本次列为“因环境受阻未执行”，未虚构通过结论。

## 同步更新的文档

- `docs/README.md`：变更索引追加本文档链接。
- `docs/RELEASE_NOTES-0.6.5.md`：更新本次真实生成的 APK/EXE 文件大小与 SHA-256 校验和。
