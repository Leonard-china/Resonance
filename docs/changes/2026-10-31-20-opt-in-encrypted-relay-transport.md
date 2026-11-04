# 2026-10-31 — 编写自选中继与直连失败后的双层 TLS 回退

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：功能，代码待构建和安全/网络验收
- 适用版本：0.6.5 / 39，未升版
- 影响平台：Android、Windows JVM 共用连接及可选独立 Python 中继
- 关联：[维护者授权的跨网方向](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)

## 需求或问题

维护者反馈手机流量与家庭宽带互连失败，明确同意优先直连、必要时使用用户自行选择或部署的加密中继。原有寻址只登记地址，不能转发数据或保证穿越 NAT。

## 原因与依据

现有客户端只尝试邀请中的直连 TCP 地址。中继需要两端都能向公共服务建立出站连接；中继本身不是 ICE/STUN/TURN 的实现，是否可在具体运营商网络使用仍取决于服务器可达性、证书及网络策略。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [server.py](../../services/relay/server.py)、[运行说明](../../services/relay/README.md) | `Relay` | 默认仅监听回环、必须配置 TLS 证书和操作员密钥；按邀请能力的单向摘要配对两条出站连接，有限并发/等待/空闲/流量，不解析或保存音频与聊天 |
| [TogetherRelayTransport.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherRelayTransport.kt) | `connect`、`roomSocket` | 外层 TLS 校验中继证书与主机名，配对后在通道内建立原有证书指纹锁定的房间 TLS；仅发送摘要路由，不向中继发送原始邀请密钥 |
| [TogetherTransport.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt) | `TogetherInvitation` | 可选邀请参数携带中继地址；没有中继时保持原邀请格式。旧版严格解析器不认识新参数，无法加入带中继的新邀请，界面已说明需双方更新 |
| [TogetherCompanionStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionStore.kt)、[TogetherCompanionPanel.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 中继配置 | 用户明确填写服务地址和访问密钥；使用现有 Android Keystore / Windows DPAPI 存储密钥，输入不写入 SavedState，默认关闭，变更在下次建房生效 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | 房间监听、`connectGuest`、`connectRelayGuest` | 并行保留原直连监听及可选出站等待；客端先尝试直连，再对非 TLS 身份错误回退中继。关闭房间取消并关闭自身套接字，记录连接路径；轮换邀请时保留近期对端恢复路由 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | 连接状态与加入确认 | 告知服务方可见元数据/潜在流量费用，区分实际中继路径，不再声称永不经过中继；修正并行候选地址不是测速选优的描述 |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | 文件注释 | 明确聊天所用房间通道可以经过用户自选的透明字节中继，避免旧注释误称无服务器 |

## 修改前后行为

| 场景 | 修改前 | 修改后（代码设计，未验收） |
| --- | --- | --- |
| 同网或公网地址可直连 | 直连 | 仍优先直连 |
| 直连失败、创建方配置可用中继且双方支持 | 无法连接 | 尝试通过中继建立内层端到端房间 TLS |
| 未配置服务 | 不使用中继 | 保持不使用，不连接默认公共服务 |
| 直连与已配置中继均无法建立 | 没有中继可供诊断 | 提示检查所选中继服务与地址，不误报为只需开启 IPv6 |
| 房间人数 | 两人 | 仍为两人；此记录不代表多人已实现 |

## 平台与数据影响

新增受系统密钥保护的本机 `relay.properties`；服务密钥不会写入邀请或聊天。服务端必须由维护者自行部署/选择，操作员可见 IP、时长、流量及路由摘要，可中断连接但内层 TLS 负责内容保密和设备认证。外层 TLS 与服务运行本身不能证明端到端安全，嵌套 TLS 实现、身份绑定和服务抗滥用都需要独立审查。

## 验证

维护者要求本轮只写代码，不构建或测试；因此**未运行客户端编译、单元/集成测试、服务器、真实跨网、嵌套 TLS 兼容、安全审计或部署**。先前 R5 验收不覆盖新中继；不要把代码存在当成跨网可用或已部署。

## 限制与后续

服务协议当前是自有的双向字节隧道，不实现 ICE/STUN/TURN；有严格大小/时长上限但没有完整认证发券、每来源速率、计费或公网运营方案。必须补齐恶意输入、TLS 身份、握手/取消/轮换竞态、直连优先、中继故障、旧客户端、真实 Android/Windows 与家庭宽带/移动数据验收。多人房间、真正平等队列和协调节点迁移仍待实施，不能通过移除单连接锁伪装完成。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 7.1 节与 `services/relay/README.md`；不部署公共服务。
