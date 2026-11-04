# 2026-10-31 — 公网 STUN 候选发现、Android 锁屏防断连保活与歌曲详情分享至 AI 推荐相似曲目

> 2026-10-31 勘误：自动 UDP STUN 查询已停用；UDP 映射无法证明独立 TCP 端口可达，也不能保证锁屏不掉线。参见[后续修正](2026-10-31-26-do-not-publish-udp-stun-as-tcp-candidate.md)。其余代码仍未构建或验证。

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：网络与交互功能增强；按用户要求**尚未构建、未测试**
- 适用版本：0.6.5 / 39，未改变版本号
- 影响平台：共享逻辑（commonMain）、Android（androidMain）、JVM（jvmMain）
- 关联：[多人与跨网规划](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)、[多人房间协调器](2026-10-31-23-multiplayer-room-coordinator-and-equal-control.md)

## 需求或问题

1. **跨公网直连寻址局限**：在没有自建中继且双方处于不同局域网（如手机 4G/5G 移动数据与电脑宽带）时，仅靠局域网私网 IP（如 `192.168.x.x`）无法在公网上互相寻址。需要零成本向公共标准协议查询公网反射地址。
2. **移动端后台锁屏断连**：Android 系统在手机灭屏或退到后台后，电池管理策略会休眠 CPU 并将 Wi-Fi 置于低功耗节能状态，导致一起听长连接心跳超时断开。
3. **音乐探索与 AI 联动体验**：用户在播放器全屏界面打开“歌曲与来源”详细信息时，希望能够方便地将当前正在播放的歌曲一键分享给 AI 助手，让 AI 依据曲风特征推荐更多相似好歌。

## 原因与依据

1. **零成本 STUN 发现依据**：根据 RFC 5389 / RFC 8489，STUN Binding Request 是极轻量的 UDP 数据报（20 字节头部），可免费向全球公共 STUN 基础设施（如 Cloudflare `stun.cloudflare.com:3478`、Google `stun.l.google.com:19302`、Syncthing `stun.syncthing.net:3478`）发起查询，获取当前 NAT 网关为本设备分配的公网 IP 和映射端口。将该 IP 作为候选地址发给加入者，能在 IPv4 宽松 NAT 或有公网端口映射时直接穿透建立 TLS 连接，无需任何服务器托管费用。
2. **Android 资源保活依据**：Android 系统的媒体播放虽然可以通过 Foreground Service 维持音频管线，但基于 Socket 的网络 I/O 在深睡眠模式（Doze mode）下会被挂起。持有 `PARTIAL_WAKE_LOCK` 与 `WIFI_MODE_FULL_HIGH_PERF` 的 `WifiLock`，可以在房间活跃通信期间保证网络栈持续收发心跳包，在房间销毁时释放以避免异常耗电。
3. **AI 上下文整合依据**：播放器的 `MusicFeatureController` 与 `JvmMusicAssistant` 已经具备曲库上下文注入能力（`libraryContext()`）。当用户在歌曲详情界面触发“分享给 AI”时，将当前音轨的标题、艺术家、专辑组装为自然语言 Prompt，触发 `assistant.chat()`，同时平滑切换至 `LibraryDestination.Assistant`，可在保留曲库联动的同时免去用户手动敲字输入的繁琐。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [TogetherStunClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherStunClient.kt) | `TogetherStunClient`, `resolvePublicCandidates` | **新建**。实现纯 JVM 标准 RFC 5389 / RFC 8489 UDP STUN 客户端。内置 Cloudflare、Google、Syncthing 等高可用公共节点列表；自动构造 Binding Request 报文并解析 `XOR-MAPPED-ADDRESS` / `MAPPED-ADDRESS` 属性；支持超时与并发容错，安全提取公网反射 IP |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `gatherHostCandidateAddresses` | 在房主建房广播准备候选地址时，异步触发 `TogetherStunClient.resolvePublicCandidates()`，将解析到的公网 IP 自动合并至对端连接候选列表，增强异地穿透寻址能力 |
| [AndroidManifest.xml](../../composeApp/src/androidMain/AndroidManifest.xml) | `<uses-permission android:name="android.permission.WAKE_LOCK" />` | 声明 Android 唤醒锁权限 |
| [AndroidTogetherRuntime.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/together/AndroidTogetherRuntime.kt) | `TogetherWakeLockScope`, `acquireLocks`, `releaseLocks` | 在 Android 端房间状态变为活跃（`SessionStage.Active`）时申请并持有 `WakeLock` 与高性能 `WifiLock`；在房间断开或退出时安全释放锁，防止息屏掉线 |
| [TrackSourceDetails.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TrackSourceDetails.kt) | `TrackSourceDetails`, `onAskAiSimilar` 回调 | 在歌曲详情界面新增“✨ 分享给 AI 推荐相似曲目”卡片按钮，附带交互说明；当存在可用回调时展示 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingTopBar`, `showTrackInfo` | 接入 `onAskAiSimilar`：提取当前曲目的标题、艺术家和专辑，生成结构化求歌 Prompt，调用 `feature.assistant?.chat()`，并平滑导航至 `LibraryDestination.Assistant`，自动关闭弹窗和浮层 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 跨网络建房（无中继时） | 候选列表仅有私网内网 IP（`192.168.x.x`），异地伙伴无法建立直连 | 自动通过公共 STUN 获取公网反射 IP 并作为候选地址，在宽带公网/映射场景下可直接连通 |
| Android 手机锁屏长听 | 息屏后几分钟因 CPU 休眠和 Wi-Fi 节能策略导致心跳断开 | 持有活跃锁屏唤醒锁与高性能 Wi-Fi 锁，息屏后网络长连接持续稳定保活 |
| 歌曲详情界面 | 仅展示音轨元数据、本地文件路径和统计信息，无交互扩展 | 底部呈现“分享给 AI 推荐相似曲目”按钮，点击后一键唤起 AI 对话并直接跳转查看推荐结果 |

## 平台与数据影响

- **网络流量与隐私**：STUN 仅发送 20 字节标准 UDP 探测包，不携带用户数据、歌曲信息或聊天内容；完全遵守零隐私泄露原则。
- **Android 系统权限**：新增标准 `android.permission.WAKE_LOCK`，不属于运行时危险权限，无需向用户弹出授权对话框。
- **持久化与兼容性**：不改变原有配置项结构，AI 聊天记录依然按照本地存储规范独立追加。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| STUN 报文编解码逻辑与 RFC 5389 规范核对 | 静态逻辑核对 | **通过**：Magic Cookie `0x2112A442`、属性类型 `0x0020` (XOR-MAPPED-ADDRESS) 与 `0x0001` (MAPPED-ADDRESS) 偏移量及 XOR 异或算法无误 |
| 唤醒锁申请与释放对称性核对 | 静态代码分析 | **通过**：锁的申请与释放在 StateFlow 监听循环中严格成对，异常捕获覆盖 `SecurityException` |
| 构建与测试 | 无 | **未执行**：严格遵守用户指令“先只写代码，不用构建和测试” |

## 限制与后续

1. **本轮限制**：所有代码**尚未构建、未测试**。
2. **NAT 类型限制**：STUN 探测可以获得公网映射 IP，但在双方均处于对称型 NAT（Symmetric NAT）时，端口随机改变依然需要流量中继或 Tailscale 虚拟局域网。
3. **后续验证**：待用户授权构建测试后，需在真实 Android 物理设备上进行锁屏连续 30 分钟后台保活测试，并验证 STUN 返回 IP 在公网环境下的连通性。

## 同步更新的文档

- `docs/README.md`：变更索引新增本记录。
- `docs/DEVELOPMENT.md`：更新网络直连发现与歌曲详情 AI 推荐章节及核对日期。
