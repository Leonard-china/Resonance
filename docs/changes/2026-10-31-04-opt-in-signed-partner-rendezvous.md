# 2026-10-31 — 可选的签名伙伴寻址与独立自托管服务

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：功能 / 独立服务，集成验收进行中
- 适用版本：0.6.5 / 39，不升版
- 影响平台：Android、Windows 原生客户端；可选 Python 服务
- 关联：[伙伴直邀](2026-10-31-03-paired-partner-direct-invitations.md)、[已授权的实施边界](2026-10-30-07-companion-room-chat-and-partner-scope.md)

## 需求或问题

记住设备身份仍不能自动找到更换网络后的伙伴。用户已接受轻量会合路线，但保留音乐和聊天无中转的边界；本轮只做可审阅本地实现与自托管交付，不暗中启用公共服务。

## 原因与依据

TLS 固定伙伴身份与可变的 IP/端口属于不同信息。单纯永久保存邀请链接会过期；单纯记住旧 IP 也会失效。寻址服务只给已经相互配对的活跃设备提供短时地址，邀请仍由双方的认证 TLS 连接发送。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [独立服务](../../services/rendezvous/server.py) | `Registry`、`Server`、`Handler` | 设备 P-256/SHA-256 签名、60 秒单次挑战、双向伙伴授权查询、120 秒内存登记、限速/并发/大小与严格字段边界；无聊天、房间能力密钥或音频接口 |
| [原生客户端](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherRendezvousClient.kt) | `register`、`lookup`、`withdraw` | 使用系统保护的设备签名身份；正式只允许无凭据/查询/片段的 HTTPS，禁用重定向，限制响应 16KB 与请求超时；HTTP 回环仅显式测试构造参数允许 |
| [房间服务](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `configureRendezvous`、`rendezvousLoop`、`invitePartner` | 用户配置后每 45 秒更新短时地址，伙伴邀请时尝试新地址，失败仍保留缓存直连和链接；不把寻址返回当作身份认证，后续 TLS 继续锁定伙伴公钥 |
| [配置与面板](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 可选寻址设置 | 默认空地址/关闭；说明双方同服务、元数据可见性和可达性限制；配置仍在系统保护的本机独立存储 |
| [服务测试](../../services/rendezvous/test_server.py) / [JVM 集成](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherRendezvousTest.kt) | 严格协议与真实签名往返 | 授权、到期、撤销、挑战重放、私密字段拒绝、容量/HTTP；新增 Java 签名与 Python 校验互通及故意旧地址的直邀回归 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 伙伴地址改变 | 仅有本机旧缓存 | 双方显式配置同服务后可查询短时新地址，再走原生直邀 |
| 未配置服务 | 仅直连 | 保持直连，不请求任何默认公共端点 |
| 查询未配对设备 | 无服务 | 拒绝在线目录枚举，未授权/离线/过期同样返回不可查询 |
| 应用关闭或网络隔离 | 无法连接 | 仍无法唤醒或突破 NAT，不虚报送达 |

## 平台与数据影响

新增独立 `services/rendezvous/`，不改官网、网站数据库或部署。Python cryptography 50.0.2 仅服务端依赖，不装入 Android/Windows 播放器。登记数据只在服务内存；运营者仍可看到 IP、设备标识和登记关系，不宣称匿名。没有音乐、聊天和邀请密钥转发。关闭或断线后，撤销失败的登记最多 120 秒自然过期。默认无外部网络请求。

## 验证

Windows Python + cryptography 50.0.2：在 `services/rendezvous/` 执行 `python -m unittest -v`，**七项通过**（0.609s），含真实本地 HTTP。JVM/Android 构建与显式 Java→Python→伙伴直邀集成测试已启动，日志 `output/together-2026-10-30/night/rendezvous-build-1.log`，结果待补。未部署公网，未验证 Ubuntu、HTTPS 反向代理、移动网络或实际服务容量。

签名 API 对照 [cryptography 官方 ECDSA 文档](https://cryptography.io/en/latest/hazmat/primitives/asymmetric/ec/)，使用库提供的 DER 签名校验，不自写椭圆曲线算法。

后续 `rendezvous-build-1.log` **构建通过**（3m35s）。显式设置测试 Python 运行时后，`TogetherRendezvousTest` 两项均通过：生产 URL 限制，以及 Java 签名→Python 校验/查询→故意错误的缓存端点更新→原生伙伴直邀→接受后完成 mTLS 房间连接。不是跳过；没有公网/Android HTTPS 反向代理实测。随后 `full-isolated-1.log` 完整桌面回归及 Android Debug/测试 APK/Release lint 构建通过（10m24s），262 项中 257 通过、5 显式跳过；未把其它并行构建覆盖的共享报告计作本次结果。

## 限制与后续

寻址只改善地址变化，不能保证防火墙/NAT 允许直连；不开启音频中继。自托管部署入口和限制见 [服务说明](../../services/rendezvous/README.md)。真实外网服务需另行确定域名与部署配置；当前不要求用户在睡眠期间作决定。最终集成与 Release 验证尚未完成。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md` 第 7.1 节、`docs/THIRD_PARTY.md`。
