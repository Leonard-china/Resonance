# 2026-10-31 — 配对伙伴直接邀听、接受后进房与移除后撤销

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：功能，进行中的验证记录
- 适用版本：0.6.5 / 39，不升版
- 影响平台：Android、Windows
- 关联：[聊天与设备配对](2026-10-31-02-encrypted-local-chat-and-device-pairing.md)、[已确认需求](2026-10-30-07-companion-room-chat-and-partner-scope.md)

## 需求或问题

相同两人经常一起听，每次找外部应用发送链接很麻烦。需要配对后点伙伴发邀请，对方明确接受后再连接房间；不能把保存一个过期链接伪装成固定伙伴。

## 原因与依据

设备身份已持久化，但临时房间邀请仍随房间和十分钟期限更新。新增独立、应用存活期间的直接邀请监听，用 TLS 公钥身份验证伙伴；只在已配对设备间发送本次新邀请。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [TogetherPartnerHub.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherPartnerHub.kt) | `start`、`invite`、`close` | 默认 45874 端口，冲突可分配端口；强制 TLS 客户端认证、最多四条握手、8KB 有界邀请帧、超时、退出关闭；验证本机伙伴列表后才接收邀请 |
| [TogetherTransport.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt) | `partnerClient` | 出站锁定伙伴公钥身份，允许临时证书更新；不按对方 JSON 声称的 ID 授权 |
| [TogetherCompanionStore/Session](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | 配对 contact、`partnerContact` | 配对时保存有界地址和实际监听端口；之后在已认证房间更新，已移除关系不自动重建 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `invitePartner`、`respondPartnerInvitation`、`joinRoom` | 点击伙伴时使用新房间邀请；接收端只展示两分钟有效的邀请，未接受不建房间连接；接受时再次锁定邀请来源设备；房主的直接邀请只为该伙伴设备预先授权，不重复审批 |
| [原生面板](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) 与 [全局邀请弹窗](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherInvitationPrompt.kt) | 伙伴列表、`接受邀请` | 应用内其它页面也能确认伙伴直邀；显示未送达与网络边界，不伪造在线状态 |

邀请监听独立于退出当前房间，结束房间后仍可收到伙伴邀请；关闭服务或应用进程后不能收到。新监听失败时房间链接仍可使用。移除伙伴后再来的新邀请和地址更新会被拒绝，不自动删除双方历史。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 已配对且双方应用运行、已知地址可达 | 重新复制链接 | 点“邀请一起听”，对方接受后进入新房间 |
| 未配对设备发直邀 | 没有直邀监听 | TLS 后仍拒绝未授权设备，不显示邀请 |
| 对方未确认 | 无伙伴流程 | 仅提示邀请，不播放、不自动加入 |
| 对方更换网络或关闭应用 | 需要链接 | 明确提示无法送达，保留新链接兜底；不声称在线或保证跨网 |

## 平台与数据影响

伙伴加密记录新增地址列表和端口，旧记录缺失时按空地址处理。房间协议仍通过能力协商，原邀请格式兼容。新增应用存活期间直连 TCP 监听；不新增公网服务器、通知推送、音频或聊天中继；网站不受影响。伙伴端口若变化而不能先通过链接建立新会话，缓存地址暂不能自行恢复，待独立会合服务解决。

## 验证

已新增真实 TLS 的桌面直邀/确认/拒绝/撤销回归，及使用 Android Keystore 的 MuMu 聊天、配对、直邀集成测试。当前构建日志 `output/together-2026-10-30/night/partner-build-1.log`，结果待补。未执行项不能当作通过。

## 限制与后续

未提供公共会合服务，未保证运营商 NAT、防火墙或公网 IPv6 可达。关闭应用后的离线唤醒、系统推送不在本次直接监听能力内。原生界面、双端互通及长时测试待完成，再构建实际最终 Release。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 第 7.1 节。
