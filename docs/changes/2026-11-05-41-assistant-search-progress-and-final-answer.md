# 2026-11-05 — 修复助手检索误判无进展、缺少最终回答与错误重复显示

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（`version.properties`），未升版
- 影响平台：共享 Compose / JVM，Android、Windows、Linux
- 关联记录：[防重复请求与下载恢复](2026-11-05-12-assistant-repeat-cost-and-download-recovery.md)

## 需求或问题

用户反馈连续三次助手任务失败。截图中的请求为“给我找一些英语播客 podcast 适用于 A2 到 B1”；助手只返回“连续 6 轮检索或失败均未完成新操作”，同一错误在聊天、控制器反馈和任务状态中显示三遍，没有推荐或可用结果。

## 原因与依据

`AssistantToolSession.execute` 仅把非只读工具成功计为进展，所有只读搜索即使返回新结果也不会重置计数；`record` 在第六轮直接抛异常，模型没有汇总已有信息或解释失败的机会。`chat` 将空白 content 替换为“已完成音乐管理操作”，还可能把没有回答伪装成成功。

系统提示要求“使用工具实际完成”，却没有区分文字推荐和下载／修改，也没有说明现有工具只有曲库、歌曲源和 B 站搜索，缺少通用网页和播客／RSS目录。此提示容易诱使模型将播客推荐反复转成歌曲检索。截图未附模型逐轮调用日志，无法确认该手机六轮具体调用、另外两次失败或供应商实际响应，不能断言所有历史失败均由同一原因导致。

UI 直接并列显示助手消息、`feature.message` 和 `task.message`；本地抛出的错误还被标注为模型回复。

## 具体修改

| 文件 | 关键类／函数 | 具体修改与目的 |
| --- | --- | --- |
| [AssistantToolSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/AssistantToolSession.kt) | `rememberObservation`、`record`、`needsFinalAnswer`、`finalMessages` | 对非空检索条目做规范化指纹去重，新数据算进展；同参数去重保持不变。保留有界检索摘要供回答使用。连续六轮无进展改为进入文字收束阶段，不直接抛异常。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `chat`、`SYSTEM`、`tools` | 普通推荐可直接回答；说明搜索能力。收束最多发送一次 `tools=null` 请求；继续返回工具调用时不执行并报错，空答复也报错。没有自动重发；本地错误标记“本地执行反馈”。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `AssistantScreen` | 末条助手消息已包含的尾部错误不重复显示；控制器／任务相同通知合并；独立反馈仍显示。 |
| [AssistantToolSessionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/AssistantToolSessionTest.kt) | 新增进展／空结果／同结果检索回归 | 覆盖 12 轮新数据、六轮空检索、换词同结果、失败工具只执行一次及有界上下文。 |
| [MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | 助手聊天回归 | 复现截图请求，验证空检索后单次文字回答、无下载或改库、九轮新检索可继续、收束调用不执行工具、空答复不报成功。 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | `assistantShowsReportedFailureOnceAndKeepsUnrelatedFeedbackVisible` | 实际 Compose 渲染核对错误仅显示一次，同时保留独立的设置保存失败。 |

## 修改前后行为

| 场景（Before） | 修改后（After） | 原因 |
| --- | --- | --- |
| 连续搜索新曲目，第六轮失败 | 新的非空条目可持续推进 | 读取新信息也是任务进展 |
| 连续空检索／相同结果／失败，直接抛内部轮数错误 | 停止工具，最多一次文字回答；仍无有效回答则明确失败 | 保留防循环保护，并给模型整理结果的机会 |
| 仅请求播客推荐也被要求操作曲库 | 可直接给一般建议并说明未经在线核实 | 现有歌曲搜索无法替代通用播客目录 |
| 空回复显示已完成音乐管理 | 明确报模型未返回有效回复 | 没有证据不能标成功 |
| 同一个错误显示三遍 | 聊天显示一次，相同底部通知去重 | 降低重复反馈，保留独立错误 |

## 平台与数据影响

Android、Windows、Linux 使用同一助手和 Compose 展示逻辑；无平台接口、存储字段、同步包或版本变化。检索指纹／摘要仅属于当前任务，不跨会话持久化。保留下载转换、取消、任务防竞争、持久化顺序、音频删除本机确认与路径边界；普通推荐不会自动授权下载或改库。

无新外部服务／权限。相比旧版第六轮直接失败，新增最多一次用于最终文字回答的模型请求；正常直接回答或成功工具任务不追加该请求。付费用量仍走现有 `ask` 记账，连接中断不自动重发。未读取真实 API Key、签名或用户音频，验证使用临时曲库和假模型。

本次对照 [DeepSeek Chat Completions 官方参数说明](https://api-docs.deepseek.com/api/create-chat-completion/)：不提供工具时默认 `tool_choice=none`。本地仍独立拒绝收束响应中的任何工具调用，不以服务商遵守该默认值作为执行保护。此文档核对不等于真实付费请求验收。

## 验证

日志保存在忽略目录 `build/assistant-recovery-2026-11-05/`；每项验证使用独立 `systemd-run --user` 服务运行 `when_build_idle.py` 后再运行下列脚本。整个进程树限制 `MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0`，确认 cgroup 限额后串行执行，共用构建锁；启动前至少 3.5 GiB 可用，运行时余量低于 1536 MiB 则停止自身验证。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python3 build/assistant-recovery-2026-11-05/compile_desktop.py` | 本机 Linux，直接 K2 编译当前共享／JVM／桌面源码 | **通过**，186 份源码，退出码 0；服务峰值约 1.1 GiB。不是打包或 ProGuard 验收。 |
| `python3 build/assistant-recovery-2026-11-05/test_desktop.py` | 临时曲库、假 AI／音源，独立 Xvfb 原生 Compose 渲染 | **通过**，62 项，0 失败／跳过：工具会话 6、接口请求 2、助手 48、存储与助手交互 4、原生 UI 2。测试源码首次编译因新增 `assertContains` 第三参数缺少 `message=` 失败，修正后 5 份测试源码编译及全部回归通过；服务峰值约 588 MiB。 |
| `python3 build/assistant-recovery-2026-11-05/compile_android.py` | 本机 Android classpath，直接 K2 编译 | **通过**，178 份应用源码、29 份仪器测试源码，均退出码 0；服务峰值约 1.1 GiB。仪器测试仅编译，未在手机运行；不是 APK／Release 构建。 |
| `python3 build/assistant-recovery-2026-11-05/test_native_final.py` 与两张截图目视复核 | 独立 Xvfb，浅色背景、临时失败消息 | **通过**，最终 UI 两项复测 0 失败／跳过（属于前述 62 项中的两项，不另计为独立新用例）；峰值约 570 MiB。夹具补齐根 Surface，单句错误与独立设置错误均可读；`composeApp/build/assistant-recovery/screenshots/` 保留两张原生图。 |
| 文档链接与 `git diff --check`（本次涉及文件） | 本机文件检查 | **通过**；保留原工作区未提交改动。 |
| 真实付费模型／Windows GUI／手机设备／打包安装 | 未连接真实模型或改变已安装客户端 | **未执行**。 |

最终实现与六份相关源码／测试文件的 SHA-256 在验证期间保持不变，快照位于 `build/assistant-recovery-2026-11-05/final-source-sha256.json`。常规弃用提示及一个 JSON 空值筛选的冗余条件警告未阻止编译；未执行全工程 Gradle 回归、设备测试、Release 或发布验收。

## 限制与后续

未调用用户付费 DeepSeek／其他真实模型；替身测试只能验证本地调用、收束和执行边界，不能证明供应商一定能给出合适的 A2–B1 播客推荐。本轮没有新增通用网页／RSS搜索能力，也不能验证历史三次失败的全部原因。未打包、安装或发布新客户端，当前手机仍使用旧行为；实际服务和设备端效果需在后续更新后验证。

## 同步更新的文档

`docs/README.md` 变更索引和最新入口；`docs/DEVELOPMENT.md` 第 13 节的工具进展、收束、推荐能力和错误展示基线。
