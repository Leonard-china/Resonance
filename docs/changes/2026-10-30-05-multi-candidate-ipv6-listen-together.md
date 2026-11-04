# 2026-10-30 — 一起听支持多候选通道自动探测、公网 IPv6 跨网直连与入口界面极简化

- 修改日期：2026-10-30（Asia/Shanghai）
- 类型：功能 / 交互打磨 / 跨网直连优化
- 适用版本：`0.6.5 / 39`（暂未升版）
- 影响平台：共享（commonMain / jvmMain）、Android、Windows
- 关联记录或需求：用户反馈一起听“老是连不上”、“为什么有三个网络连接入口”，并确认推进“第一步：多候选地址并发探测 + 优先 IPv6 跨网直连，零配置”落地。关联先前记录 [2026-10-30-01](2026-10-30-01-product-positioning-and-listen-together-plan.md)、[2026-10-30-02](2026-10-30-02-listen-together-session-and-native-playback.md)。

## 需求或问题

1. **“为什么有三个网络连接入口？”用户困惑且极易点错**：
   在多网卡设备（如 Windows 电脑装有 WSL/Docker/虚拟机，或手机开启热点/双栈网络）上，原有房主界面使用生硬的 `FilterChip` 将所有检测到的网卡 IP 以“选择网络连接入口”直接暴露。用户既看不懂各个 IP 代表的含义，又极易选中对外部完全不可达的虚拟机虚拟网卡，导致生成的邀请链接对方根本连不上。
2. **跨网络异地连接频频失败**：
   原有邀请格式只支持单个写死的 `host`。如果房主和朋友不在同一个 Wi-Fi（例如异地、移动流量或跨路由器），局域网内网 IPv4 在公网天然不可路由；即使双方网络具备公网 IPv6，用户也常常不会手动挑选，或因为单一地址探测超时而直接连接中断。
3. **连接失败诊断过于笼统**：
   连接超时或被拒绝时，原有界面只抛出一句泛化的“操作未完成，请检查网络连接、地址或邀请”，缺乏针对异地跨网、路由器 AP 隔离、防火墙拦截等典型场景的可操作建议。

## 原因与依据

1. `localAddresses()` 原始实现未过滤虚拟机/容器虚拟网卡（如 WSL、vEthernet、VMware、VirtualBox、Docker 等），且未识别全球单播公网 IPv6（`2000::/3`）与真实物理 Wi-Fi 的优先级，导致无效或次优地址排在前列。
2. `TogetherInvitation` 仅包含单个 `host`，且解析时硬编码 `parts.size == 2`，无法向后扩展候选地址。
3. `connectGuest` 采用单一地址串行连接，单次超时 8 秒。在具有多通道（局域网 IPv4、公网 IPv6、热点 IP）的环境下，未能实施类似 RFC 8305（Happy Eyeballs）的并发错峰竞速连接。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `TogetherInvitationPreview`, `invitation` | `TogetherInvitationPreview` 增加 `candidates: List<String>` 字段；接口重载带 `candidates` 的 `invitation` 并保持向后兼容 |
| [TogetherTransport.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt) | `TogetherInvitation` | 增加 `candidates` 与 `allHosts` 属性；`encode()` 支持在 query 中追加 `&cands=...`；`parse()` 放宽参数数量限制并解析候选通道列表，严格防止未知/重复键注入 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `localAddresses`, `connectGuest`, `connectRacedGuest`, `invitation` | 1. 过滤 WSL/VMware/VirtualBox/Docker 虚拟网卡与 IPv6 ULA 私网地址，将公网全球单播 IPv6（2000::/3）与真实 Wi-Fi 物理网卡优先分类排序；<br>2. 实现 Happy Eyeballs 并发错峰竞速探测，优先尝试公网 IPv6 与局域网 IPv4，率先完成 TLS 握手者胜出并关闭冗余 socket；<br>3. 重连阶段连接超时优化为 2.5s，避免无意义等待；<br>4. 细化连接超时、连接被拒、凭据失效的友好错误诊断与场景建议 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | `TogetherScreen`, `TogetherFaqItem` | 1. 彻底移除主界面晦涩的单选“网络连接入口”，替换为“多通道直连已就绪”智能状态卡片；<br>2. 复制邀请一键打包所有候选地址；<br>3. 详细网卡指定收拢至折叠的高级网络设置中；<br>4. 加入确认弹窗与 FAQ 中清晰展示多通道自动测速选优机制及公网 IPv6 跨网支持说明 |
| [ListenTogetherTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/ListenTogetherTest.kt) | `ListenTogetherTest` | 新增多候选地址编解码 Round-trip 测试与并发竞速自动选通活地址的端到端测试 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 房主创建房间界面 | 界面直接展示“选择网络连接入口：”及裸 IP 标签，用户需手动猜测并做单选，常误选虚拟网卡 | 主屏展示清晰的“已开启多通道直连（局域网 + 异地 IPv6）”就绪卡片，系统自动打包可用物理/公网通道，无需手动配置 |
| 异地跨网（双方有 IPv6） | 邀请仅含单一内网 IPv4，异地直连 100% 失败超时 | 邀请同时包含公网 IPv6 与局域网地址，加入端自动并发探测，在公网直接秒通，音质免服务器中转压缩 |
| 多网卡/虚拟机环境 | 包含 WSL、Docker 等内部网卡，好友连入必定失败 | 智能识别并过滤虚拟机网卡，仅保留真实 Wi-Fi 与公网 IPv6 |
| 朋友加入确认与测速 | 仅显示单一 host | 提示“已包含 N 个候选直连通道，加入时将自动测速选优” |
| 连接失败报错 | 仅提示“操作未完成，请检查网络连接、地址或邀请” | 精准区分超时（提示 IPv6 / 同网热点排查）、连接拒绝（提示房主房间与防火墙）、凭据失效等具体建议 |

## 平台与数据影响

- **共享逻辑（commonMain / jvmMain）**：纯 Kotlin 实现，完全跨平台支持 Desktop（Windows）与 Android。
- **协议兼容性**：新版完全向后兼容旧版邀请（单 host 依然走单连接通道）；新版之间自动启用多候选地址竞速。
- **数据与安全**：端到端加密（TLS 1.3 / 1.2）及随机临时凭据边界未变，不向任何第三方服务泄露歌曲数据。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --tests "com.resonance.player.together.ListenTogetherTest"` | Windows 11 / JDK 17 | **通过**（10 项测试全部通过，覆盖多候选地址 Round-trip 与竞速连接自动探测） |
| `.\gradlew.bat :composeApp:desktopTest --tests "com.resonance.player.together.*"` | Windows 11 / JDK 17 | **通过**（together 测试套件全部 100% 通过） |
| `.\gradlew.bat :composeApp:compileDebugKotlinAndroid` | Windows 11 / Android SDK | **通过**（Android 端 Kotlin 编译通过无错误） |

## 限制与后续

- 本轮属于“第一步：多候选地址并发探测 + 优先 IPv6 跨网直连”。对于双方网络均无公网 IPv6 且处于对称型 NAT 隔离的极端网络环境，后续按规划第二步引入轻量房间信令与打洞穿透/可选密文中继。
- 手机端移动数据网络与 Wi-Fi 切换时的动态重评保持现有有界重连机制。

## 同步更新的文档

- `docs/README.md`（新增 2026-10-30-05 索引与最新核对日期）
- `docs/DEVELOPMENT.md`（更新 7.1 节网络直连能力与多候选并发探测说明）
