# 2026-10-31 — 限制邀请候选地址并及时取消全部直连握手

- 修改日期：2026-10-31（Asia/Shanghai，10 月 30 日深夜开始实现）
- 类型：Bug 修复
- 适用版本：0.6.5 / 39，不升版
- 影响平台：Android、Windows 共用 JVM 传输
- 关联：[多地址探测](2026-10-30-05-multi-candidate-ipv6-listen-together.md)

## 需求或问题

用户要求提升连接成功率和连接简便性。多候选 TLS 握手期间主动取消，原实现未统一登记尚未认证的 socket；延迟启动的候选退出后也可能无人唤醒结果等待。

## 原因与依据

`connectRacedGuest` 的局部 socket 集合不能由 `leave` 立即关闭；房主接受的未认证 socket 也未纳入离开清理。邀请候选原先没有总数上限，格式错误会被静默丢弃。原始异常日志可能包含邀请能力 URL。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [TogetherTransport.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt) | `TogetherInvitation` | 主地址加候选最多八个，纯本地验证地址、端口及候选格式；保留旧单地址和自然语言邀请提取 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `retainSocket`、`retireSocket`、`epochSignal`、`leave` | 登记全部入站和出站握手；离开关闭当前代次 socket 并通知竞争等待；子任务未启动也释放资源；移除原始异常及地址回显 |
| [TogetherConnectionBoundsTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherConnectionBoundsTest.kt) | 两项回归 | 伪服务同时卡住两条 TLS 握手，验证取消两秒内结束、双方 socket 断开及状态回 Idle；覆盖候选边界与旧链接 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 朋友取消卡住的多候选握手 | 可能等待 socket 超时或一直等待结果 | 取消并关闭本次所有连接 |
| 邀请包含过多或错误候选 | 未统一限制或静默忽略 | 连接前本地拒绝 |

## 平台与数据影响

两端共用实现；未改变版本、持久数据、权限或音频文件。最多八个地址不保证公网可达，不新增中继或外部服务。

## 验证

Windows JDK 17：`./gradlew.bat :composeApp:desktopTest --tests com.resonance.player.together.* :composeApp:compileDebugKotlinAndroid`，本次第二次执行 **通过**（1m27s）；日志 `output/together-2026-10-30/night/connection-bounds-2.log`。第一次两项测试失败是测试错误地要求取消正常返回，以及把自然语言链接后空格当非法地址；已修正后重跑。可选原生桥接测试未开启，不能以此证明跨网或长时间发声。

## 限制与后续

后续整组回归发现 `leave` 复制 socket 集合时与 `retireSocket` 的无锁移除竞态：Kotlin `toList` 在集合恰好由一变零时可能直接调用空迭代器。已把移除也纳入同一锁，`partner-build-3.log` 整组构建通过。随后独立输出目录的完整桌面回归通过（262 项，257 通过、5 跳过）；日志 `full-isolated-1.log`，与新版预取/收件人绑定等后续改动的验证分开记录。

域名解析仍依赖系统；本回归覆盖数值 IP 的连接与 TLS 取消，不声称能中断系统 DNS 查询。公网 IPv6、防火墙、运营商 NAT 和真实锁屏仍需设备测试。

## 同步更新的文档

`docs/README.md` 索引及 `docs/DEVELOPMENT.md` 第 7.1 节。
