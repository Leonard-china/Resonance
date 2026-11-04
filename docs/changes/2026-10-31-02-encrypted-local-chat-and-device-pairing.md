# 2026-10-31 — 房间内加密聊天、本机清空及双方确认的设备配对

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：功能
- 适用版本：0.6.5 / 39，不升版
- 影响平台：Android、Windows 共享协议及原生 Compose 界面
- 关联：[用户确认的陪伴房间需求](2026-10-30-07-companion-room-chat-and-partner-scope.md)

## 需求或问题

两人长时间开着房间，需要边听边聊，历史只在各自设备保存、各自清空，并记住经常一起听的朋友。此前只有临时房间身份，无文字通道及伙伴列表。

## 原因与依据

临时 TLS 证书每次创建房间更新，不能直接用证书指纹充当跨房间好友身份。新增受系统凭据保护的持久 P-256 设备密钥，通过 TLS 证明密钥持有，仍保留房间证书锁定、邀请密钥和房主审批。

## 具体修改

| 文件 | 符号 | 修改与目的 |
| --- | --- | --- |
| [共享契约](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `TogetherCompanionState`、聊天及配对方法 | 单独暴露本机聊天和伙伴状态，不依赖音频是否准备好 |
| [本机存储](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionStore.kt) | `identity`、`enqueue`、`receive`、`clear` | 加密设备密钥、最多 32 位伙伴及每位最多 1000 条记录；最多 100 条待发送，不静默淘汰待发；明文每条最多 2000 字/8KB |
| [会话](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `attach`、`receive`、`flush` | 协商 `companion:1` 后在原 TLS 通道传输；持久接收后确认、序号去重、重连补发、双向配对确认；独立有界写队列避免双向发送阻塞读循环 |
| [TLS](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt) | `identity(keyPair)`、`peerId` | 新客户端可提供设备证书；旧客户端仍可匿名加入；身份取自实际 TLS 公钥而非对方声称的 JSON ID |
| [服务](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | 房主/朋友握手和读循环 | 仅双方支持且客户端身份验证成功时启用聊天；旧增强协议不接收新增消息 |
| [原生界面](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 聊天面板、配对和清空对话框 | 显示送达/待发状态，手动浏览不强制滚底；本机清空及移除伙伴需明确操作；草稿不进入 SavedState |
| Android/Windows 平台服务 | `TogetherCompanionStore` 接线 | Android Keystore AES-GCM / Windows 当前用户 DPAPI；Android 凭据密钥初始化加同步避免并发生成 |

清空首先原子替换独立、无回滚备份的加密清空标记，保存收发序号底线，再写空历史、删除旧历史备份。标记损坏时拒绝加载，防止从旧备份恢复已清空内容。清空只影响本机；不发送远程删除命令。

后续审查补上界面动作的收件人绑定：发送在点击时捕获 peerId，存储前再次核对，防止异步等待期间换房间将消息送给新朋友；配对/接受直邀也校验显示时的对象，本机清空弹窗保留最初选定的对象。保存失败保留输入草稿。对应新增断言及界面复测在后续独立构建中运行。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 房间尚无歌曲或正在传输 | 没有文字沟通 | 完成认证后即可聊天 |
| 消息重发或同一设备换新房间 | 无持久聊天身份 | 保持设备身份，已收序号去重 |
| 清空自己的记录 | 无此功能 | 本机清空，朋友记录保留；旧消息重放不复活 |
| 记住朋友 | 每次只有临时邀请 | 双方明确确认后保存设备伙伴；一键邀请的连接入口另行实施 |

## 平台与数据影响

新增应用私有 `together-companions/`，与曲库、同步包、AI 设置和音频缓存隔离。密钥不进入日志或 SavedState；不上传聊天、伙伴列表、音乐库或身份私钥。删除伙伴保留本机历史，暂不自动结束已授权的房间。进程存活范围及后台限制不因此改变；没有公共服务部署。

## 验证

Windows JDK 17 执行 `./gradlew.bat :composeApp:desktopTest --tests com.resonance.player.together.* :composeApp:compileDebugKotlinAndroid`，**通过**（3m13s），日志 `output/together-2026-10-30/night/chat-compile-1.log`。新增 `TogetherCompanionTest` 四项全通过：真实 TLS 双向聊天和送达确认；双方同意前不保存伙伴；关闭服务后新房间保持设备身份和历史；两种旧版/新版方向兼容；本机加密、独立清空、旧备份/旧序号不复活；待发上限。该次编译在新界面文件写入前开始，不能代替界面和设备验收。

## 限制与后续

MuMu 第二次设备执行 `companion-mumu-2.log` **七项全部通过**（12.6s）：真实 Android Keystore/mTLS/聊天/配对/直邀、保存失败保留草稿、清空需确认、直邀未接受不连接及原房间入口。第一次五项通过、两项失败，分别为 UI 测试替身未实现 equals 与旧按钮文字断言；已修正再跑。该轮先于后续收件人绑定改动，最终新包仍需复测。

新界面构建和 MuMu 设备验收正在进行。当前记录只交代已实现的聊天与配对基础；固定伙伴直邀、动态地址会合、长期后台运行、跨 Windows/Android 的新双向证书验证还需后续实施和实测。没有把旧的 15 分钟原生音频结果作为新聊天版本通过证据。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 第 7.1 节。
