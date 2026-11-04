# 2026-11-01 — Resonance 最终构建与验收交接提示词

以下内容可直接交给 Antigravity。源码阶段遵守维护者“不构建、不测试”要求，本文件中的命令和场景尚未执行；它们是下一阶段工作，不是通过证明。

```text
你接手 D:/Project/Resonance 的最终构建、测试、问题修复和本地产物交付。维护者指定由你完成这一步，请直接推进，发现失败就定位修复并验证，不要只出计划或把未运行写成通过。

先读 AGENTS.md、docs/README.md、docs/DEVELOPMENT.md、docs/AI-WORKFLOW.md、output/together-2026-10-30/CONTINUE.md 最新段落，再读 docs/changes/2026-11-01-01 到 07 的记录。重点为 02 直接 AI 歌词、04 群聊回执、05 原生协调端控制与队列、06 协议校验/恢复、07 未读提醒与剩余聊天通道。旧 R5 的构建和设备结果不能作为当前源码通过的依据。

工作区有维护者、Antigravity、Codex 的大量未提交和未跟踪源码、测试、文档；先记录 git status，完整保留。不要 reset/clean、覆盖或只挑几个文件打包。版本仍是 version.properties 的 0.6.5/39；不升版、不手写 BuildVersion.kt、不推送、不部署公共服务。播放器为 Kotlin/Compose 原生 Android + Windows，website 独立，本任务不碰网站或真实数据库。

用户功能要求：多于两人的长时一起听；所有成员平等播放/点歌/队列操作；加入和常听伙伴需要明确同意；各设备独立可清空聊天；消息提醒；优先直连，失败可用用户自选/自建加密字节中继。默认没有公共寻址或中继，不偷偷启用公共 STUN。AI 歌词应直接调用当前模型，不搜索，不上传音频；音频识别同步歌词是单独入口和费用/上传流程。

重要现状：本轮源码从未构建或运行，先处理真实编译错误，再验证功能，不要接受源码作者自述为证据。
1. 新增 TogetherHostQueue、hostQueue/hostPlayback StateFlow 和 TogetherHostPlaybackDriver。Android 在 AndroidTogetherRuntime 的原生作用域接控制/增量时间线，Windows 在原生作用域接控制/结束换曲；App 原生模式不再重复消费动作/结束事件，而跟随队列/原生曲目。验证没有重复播放、重复下一首、页面消失就失控或静默暂停。
2. 协调器和播放器的可播放混合队列通过会话/修订投影同步；最多 50 首，未完成贡献留在群队列但不可点歌，完整大小/SHA-256 校验后才入原生投影。核对 awaitingQueueProjection 反馈与同时本地换歌是否丢失修改或锁住队列。删除当前曲应选既定替代曲，暂停时仍暂停；移动当前曲应保留播放位置。曲库刷新保留房间临时曲，明确空队列不回退整个曲库，退出房间不把临时曲写入恢复快照或删除用户音频。已移除贡献不应占着 50 项可播放目录阻止新歌；磁盘缓存仍按 512 MiB 账目/离房清理，不是删除队列即释放磁盘。另测客户端进程重启后命令计数重置和旧 Windows 结束回调，不得卡住操作或多推进一首。
3. 实验多人协议 groupVersion=2；聊天另外协商 chatReceipts=1，只有具备本机加密聊天存储的客户端才声明此能力。无 companionStore 的协议/音频测试夹具可以参加群音频，但不能用于证明群聊。旧单对端独立发 queue 列表，不混入群；早期实验群客户端需更新。旧私聊没有冒充新群聊的迁移保证，必须实测边界和明确提示。
4. 群聊在本机加密记录内保存 pendingRecipients/recipientCount；协调设备也保存入站转发名单。排序补发任务在发消息、成员加入和 5 秒周期触发。接收者落盘后 chatReceipt，协调设备保存回执后向原发送者 chatAccepted；状态合并只减少名单。普通 chatAck 不能把新式逐成员消息冒充全员收到。已接收不代表已读，后来加入的人不能收到以前内容。清空本机记录也移除本机转发内容；协调设备清空后/房间关闭/进程重启的可靠投递不保证，不能写成不会丢消息。
5. 群内目前是每成员到协调设备的 TLS，协调设备是合法参与者且能读取/转发内容；没有完整接入发送者签名或协调设备移交。不得宣传协调设备无法读取、无法伪造群消息，或协调设备离开后仍无缝继续。TogetherGroupEnvelope 是基础代码，不是已接通证明。
6. 新快照检查房间 ID、递增 revision、8 人/50 曲目上限、唯一身份/协调设备、文本控制字符及当前曲一致性。缓存完成推进 revision，不把 .part 当已完成；媒体上传对照已批准贡献身份与元数据。旧 ready 不应标记新曲已就绪，上传失败要清理待传输状态。核对异常关闭后锁、缓存账目、连接令牌和队列均正确释放。
7. 未读计数是本进程当前会话最多 999，不是持久化未读/系统推送。新消息落盘后增加、重复不加、旧房间迟到事件不能污染新房间。末条确实在聊天列表可见时本机归零；浏览旧消息保留位置并有跳到最新。播放器标题提示未读。当前附着成员离开而仍有验证成员时重新附着，不能把剩余聊天室判成断开。
8. 直接 AI 文本生成调用链为 requestAiLyrics -> DeepSeekClient.generateLyrics -> completeText，不传搜索工具；模型时间戳一律去掉，按未经核验文本保存。验证结构化/纯文本/LRC/拒绝/空答/损坏结果、重复歌词不丢、快速切歌 A→B→A、旧译文和缓存迟到不会覆盖。原音频识别的上传/用量同意与普通免费歌词缓存流程要保留。

验证顺序：
- 先做共享/桌面编译与有意义的针对性测试，再 Android 编译/lint，再原生设备验收，最后双端 Release。根据缺口补回归测试，不能为绿灯禁用断言、跳过失败或删除旧用例。
- 查 docs/DEVELOPMENT.md 当前构建命令。可从以下现有任务开始（本交接没有运行它们）：
  .\gradlew.bat :composeApp:desktopTest --no-configuration-cache --max-workers=2
  .\gradlew.bat :composeApp:lintRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest --no-configuration-cache --max-workers=2
  按失败情况定点重跑；如需全量用文档中的 allTests。不要在 16 GB 机器同时跑多个重 JVM、Release 和摄影作业；控制 worker/堆，避免相互覆盖报告或旧产物混入。
- 阅读既有 TogetherPlaybackDriverTest、PlaybackStateStoreTest、TogetherCompanionTest、ListenTogetherTest、TogetherConnectionBoundsTest、TogetherPrefetchCancellationTest、两方向原生桥接及 Android 仪器测试。新增协调端驱动、群回执和多人必须补真正覆盖其行为的用例；原双人测试通过不等于多人通过。
- 至少三个独立成员验证共同播放/暂停/上下首/seek/点歌/贡献/删当前曲/删最后一首/上下移动，多个成员并发操作后以实际原生曲目和最终队列核对。测试页面关闭/切页/Activity 重建、锁屏和原生播放服务生命周期；Windows 自动结束、随机/循环与睡眠定时不能重复推进。
- 聊天至少覆盖：两个发送者各自序号1，协调端自己发送，连续多条顺序，发送中断线、重连、重复包、回执丢失/乱序、某接收者慢/离开/返回、后来加入者看不到历史、100条待发与100条转发上限、1000条保留滚动、独立清空后不重现，以及当前附着成员退出后剩余成员继续聊。磁盘失败不得先发成功回执，普通 chatAck 不能伪造全员收到。
- 安全/网络：无效邀请、过期/刷新、并发审批、恢复令牌与路由撤销，证书/设备指纹不匹配，旧世代动作、恶意快照、任意媒体ID、部分文件/超额缓存。TLS 身份失败不得降级到不验证的连接。中继用临时本地证书/随机测试凭据跑真实隧道，确认嵌套 TLS 可在目标 JVM/Android 运行、服务不接触音频/聊天明文；可参考 services/relay 和 services/rendezvous 的临时本地测试。不得公开部署。MuMu/回环通过不能冒充真实手机流量↔家中宽带/NAT通过；没有相应网络就准确列未测。
- 用户没有手机可借用，使用 MuMu。先只读检查当前模拟器、包和正在运行的测试。另有人的 .debug 仪器任务，不得 force-stop/覆盖安装/改数据打断。需要独立验收包与测试ID时沿用检查点的 .codex 隔离思路或独立工作副本，不复用旧APK当新产物，不抢占别人的声音任务，不自动恢复摄影。
- 系统通知验证允许/拒绝两种情况、聊天页前后台、重复包无重复提醒、未读/跳转/本机清空，以及 Windows 托盘不可用时的界面提示。不替用户选择防火墙或安全权限弹窗。
- 不读取/打印/导出真实 keystore.properties、用户 API Key、签名密钥/私钥，不从加密配置或后台内存导出凭据，不把真实密钥写进命令、日志、代码、测试夹具或提示词。真实模型验证仅通过正常应用已配置的授权接口且不展示凭据；其余优先用原创/公版夹具和模拟响应。

通过后再构建当前源码 Release：
  .\gradlew.bat :composeApp:assembleRelease --no-configuration-cache --max-workers=2
  .\gradlew.bat :composeApp:createReleaseDistributable :composeApp:packageReleaseExe --no-configuration-cache --max-workers=2
按项目发布脚本读取当前版本、生成/修订对应 RELEASE_NOTES-0.6.5.md。用本次真正产物验签/计算 SHA256，验证本次正式 Android 包和 Windows app image/EXE 启动；Windows EXE 若仍未签名据实说明，不拿历史哈希或旧安装包充数。保存到新 output 目录并列明准确产物路径与验证边界。旧 R5 已发微信，不重复发旧包；此提示词不要求发送消息、公开发布、提交或推送。

每个独立修复都新增当天独立 docs/changes 文档，更新索引和 DEVELOPMENT，记录真实命令/结果/跳过理由。最后给维护者简明报告：实际通过项、失败或未测项、本次APK/EXE/校验清单路径、相关变更文档及仍影响发布的阻碍。不要把协调迁移、完整群签名或任意NAT可达伪装成已经完成；若它们超出本轮协议，明确为设计限制。
```

当前源码接线阶段交接后应避免另一代理同时继续改同一工作区；后续修复由验收代理依据实测推进。宣传片仍未完成，不包含在这份构建验收提示词中。
