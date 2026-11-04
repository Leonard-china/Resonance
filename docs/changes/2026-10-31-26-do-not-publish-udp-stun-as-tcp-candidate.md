# 2026-10-31 — 停止把 UDP STUN 结果自动作为房间 TCP 地址

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：跨网与网络隐私修正；版本 0.6.5 / 39，不升版
- 影响平台：Android、Windows 共用 JVM 房间服务
- 关联：[STUN 初始实现](2026-10-31-25-public-stun-discovery-and-ai-similar-tracks.md)

## 需求或问题

用户希望家庭宽带与手机移动网络更容易连接，且此前批准自选中继。建房时自动查询外部 STUN 服务会向该服务暴露公网来源，并不等于用户配置了中继。

## 原因与依据

原实现使用独立临时 UDP socket 查询公网地址，丢弃了 UDP 映射端口，再把 IP 配合房间 TCP 监听端口发送。UDP 的 NAT 映射不能证明另一个 TCP 监听端口可从公网进入；实际可达性取决于 TCP 映射及网络策略。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `host` | 移除默认自动外部 STUN 查询及错误候选追加；现存 STUN 客户端文件保留但不再由此调用 |

## 修改前后行为

建房不再自动联系代码内列出的公共 STUN 服务，也不再将 UDP 映射 IP 当作已确认的 TCP 可达地址；既有本地地址、手工选择和用户自选中继代码保留。单纯公网 IP 并不保证连通。

## 平台与数据影响

不修改用户数据和音频，不部署服务；可能减少某些本来恰好有正确 TCP 映射的公网 IP 候选，真实跨网能力仍需正确的 TCP 地址发现、打洞或经用户配置中继。

## 验证

静态阅读调用链；依用户要求未构建、未测试、未部署或做真实跨网验证。原记录中“自动发现可增强 TCP 穿透”的描述由本记录更正。

## 限制与后续

后续若引入外部发现服务，需明确用户选择与隐私说明，验证 TCP 的 IP 和端口，不能以 UDP 查询代替实际端到端可达性判断。

## 同步更新的文档

`docs/README.md` 与 `docs/DEVELOPMENT.md`。
