# 2026-11-05 — 移植 DeepSeek Harness 搜索抓取模块并使用成熟原生 Markdown 组件

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能／助手能力与界面接入
- 适用版本：`version.properties` 的 0.6.6 / 40，未升版
- 影响平台：共享 Compose／JVM，Android、Windows、Linux
- 关联需求：用户指定 [deepseek-ai/deepseek-harness](https://github.com/deepseek-ai/deepseek-harness.git)，要求复用成熟项目和 UI，而非仅做局部补丁；前轮[检索进展与收束修复](2026-11-05-41-assistant-search-progress-and-final-answer.md)。

## 需求或问题

“找一些适合 A2 到 B1 的英语播客”曾连续失败。前轮只修复了无进展误判与收束，播放器仍只有歌曲工具，无法搜索公开网页；助手回答用普通 `Text` 显示 Markdown，来源链接不能作为原生网页链接点击。用户要求以指定 Harness 的效果为参照，实际利用开源实现补齐能力。

## 原因与依据

本次核对官方 Harness 的 commit `fcc0ad462172522aaf5c93a7b2e01f6b9f0b0159`：`packages/web/web-deepseek-api/src/search.ts`、`fetch.ts`、`transport.ts` 和 `packages/web/tool-web`。上游为 MIT，采用独立 Web provider、直接 Search／Fetch API、凭据就绪后才暴露工具、来源字段映射、取消与失败结果等机制；这些可以移植到 JVM，而完整的 Cordis／Node／Web UI 不是当前 Android／Windows／Linux 原生播放器的可直接嵌入组件。

核对 [DeepSeek Search](https://api-docs.deepseek.com/api/search/)、[Fetch](https://api-docs.deepseek.com/api/fetch/) 和[计费说明](https://api-docs.deepseek.com/guides/search/)后接入同一官方协议。Search／Fetch 按成功请求独立计费，不应当宣传成聊天 Token 的免费附属能力。

界面实际采用 [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer/tree/v0.38.1) 的 Material 3 组件。核对 Maven 发布 POM 和实际 JVM class 头：0.38.1 使用 Kotlin 2.2.21／JVM major 61，可用于当前 Kotlin 2.3.20／JDK 17；0.40.0 虽与 Kotlin 版本相符，首次原生测试实际因 major 65（Java 21）失败，不能采用。0.42–0.46 还需要 Kotlin 2.4。因此固定 0.38.1，不为渲染回答升级整个工程工具链。

## 具体修改

| 文件 | 关键类／函数 | 修改与目的 |
| --- | --- | --- |
| [DeepSeekWebResearch.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ai/DeepSeekWebResearch.kt) | `MusicWebResearch`、`DeepSeekWebResearch.search/fetch/post/supports` | 将 Harness 直接 Search／Fetch 流程适配到 Kotlin：每次操作一个请求、来源字段规范化、正文及响应大小有界、取消断开、不重发、不跟随重定向。连接只允许 DeepSeek 官方 HTTPS 标准路径，不把代理／其他模型密钥转交官方。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `chat`、`tools(webEnabled)`、`SYSTEM` | 依据当前连接和开关注册 `web_search`／`web_fetch`；明确播客查找优先官方网页和实际来源引用。执行过程复用 `step` 子任务和真实失败状态；同一任务相同搜索仍复用已有结果。 |
| [AssistantToolSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/AssistantToolSession.kt) | `READ_ONLY` | 网页搜索／正文读取属于只读工具，新信息继续推进；不把网页检索当作完成了文件操作。保持六轮无进展后单次文字收束。 |
| [MusicFeatureModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/MusicFeatureModels.kt)、[MusicFeatureCodec.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicFeatureCodec.kt) | `assistantWebSearchEnabled`、`assistantTaskId` | 新增持久的联网开关（缺省 true）和只在内存中的本轮任务归属。新建／切换／删除／清空会话清除过程展示归属。 |
| [AssistantReply.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AssistantReply.kt) | `AssistantReply`、`AssistantResearchSteps` | 使用上游原生 Markdown 组件：异步解析、标题／列表／链接、选择复制、加载／解析失败保留原文字；默认不加载图片。已完成检索可展开，进行中显示真实步骤，错误保留具体原因。 |
| [AssistantMusicLinks.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/AssistantMusicLinks.kt)、[MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `isAssistantWebLink`、`AssistantScreen`、AI 设置 | 只对用户点击的 HTTP／HTTPS 链接调用原生 URI handler，拒绝任意文件／Intent／脚本 scheme；本地音乐链接先经现有曲库解析，播放与队列附件保留。新增联网开关和独立计费说明。 |
| [composeApp/build.gradle.kts](../../composeApp/build.gradle.kts) | commonMain dependency | 实际添加 `com.mikepenz:multiplatform-markdown-renderer-m3:0.38.1`，由 Gradle 解析三端依赖，无 WebView。 |
| [许可资源目录](../../composeApp/src/commonMain/composeResources/files/licenses/) | 两份上游许可 | 保留 DeepSeek MIT、Markdown Apache-2.0 及附带 MIT 声明，随正常 Compose 资源构建纳入应用；同步第三方说明。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 官方 DeepSeek 会话找播客／查在线资料 | 只能调用歌曲搜索或给未核实建议 | 可调用网页搜索、读取公开正文，再附工具实际返回的来源链接 |
| 使用代理、自定义地址或其他模型 | 没有 Web 能力声明 | 不暴露官方联网工具，明确未在线核实；不会发送这些连接的 Key |
| 同一任务重复相同搜索 | 无网页搜索工具 | 复用会话幂等结果，不重复发搜索请求 |
| Markdown 回答 | 星号、标题标记和 URL 当普通文字显示 | 原生标题、列表、可选文字和可点击来源 |
| 检索处理过程 | 只看到单个内部工具名／全局进度 | 当前聊天显示实际搜索／读取步骤、进行中／完成／失败及可展开历史 |
| 网页含下载／删除指令 | 无网页输入 | 一律视为外部证据数据，不改变原删除授权和音乐工具权限 |

## 平台与数据影响

三端共用 Kotlin provider 和 Compose 组件；无平台接口变更。Windows／Linux 的外链点击复用 Compose 原生 URI handler，手机复用 Android URI handler；未验证每个平台浏览器实际弹出。

新配置键 `assistantWebSearchEnabled` 缺省 true，旧设置可读取，关闭后持久保留；只有当前会话选中且配置有效的官方 DeepSeek 连接才真正开放工具。`assistantTaskId` 不落盘；原聊天消息和同步包格式不变。网页步骤进入已有 `tasks` 子任务，不增加失败自动收费重试入口。

联网只由聊天中的实际工具调用发起，没有后台网页爬取或辅助模型请求；查询及最多 128 个 Unicode 字符的本轮用户问题传给官方 Search API，URL 传给 Fetch API。每次搜索最多六份来源、每份模型摘要 800 字符；正文保留前 3000 字符并标注截断，长结果仍经过已有工具上下文压缩。搜索／抓取费用独立于当前聊天 Token 合计，设置明确提示；本轮没有实现检索账单估算。

仅取公开 HTTP／HTTPS 页面，拒绝带凭据、字面内网／回环 IP、本机域名等目标。抓取由 DeepSeek 服务完成；本地不解析网页脚本，也不直接访问网页目标。没有任意 shell／桌面控制、没有音频下载授权扩张，不改变转换、取消、播放防竞争、原子存储、文件删除确认或版本。

## 复用项目的取舍

| 上游 | 核对内容 | 本轮采用方式 |
| --- | --- | --- |
| DeepSeek Harness（MIT） | 搜索、抓取、凭据、工具边界与 provider 源码 | 实际 Kotlin 移植 Search／Fetch，保留来源及许可；没有声称嵌入完整运行时 |
| multiplatform-markdown-renderer（Apache-2.0，含 MIT 部分） | 0.38.1 源码 API、许可和发布 POM | 实际引入 Material 3 依赖并用于助手回答 |
| Auxio、RetroMusicPlayer（GPL-3.0） | 官方项目说明／许可 | 调研可用交互方向，本轮未复制其代码或资源 |
| 用户既定 AM++／Apple Music UI Demo 参考 | 当前仓库的视觉方向与既有来源记录 | 继续保留；本轮聚焦助手，未改成另一个播放器的全套页面 |

## 验证

验证日志：忽略目录 `build/assistant-harness-2026-11-05/`。每项 Kotlin／Gradle 验证均通过共享构建锁及独立 systemd 用户服务串行运行，限制整个进程树 `MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0`；启动前至少 3.5 GiB 可用，运行中保留 1536 MiB 余量。不读取真实 Key／签名文件／用户歌曲。

| 实际命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `python3 build/assistant-harness-2026-11-05/resolve.py`（Gradle `:composeApp:exportAssistantHarnessClasspaths`） | 当前工程／新依赖，单 worker、短生命周期 Gradle、进程内 Kotlin 配置 | **通过**；初选 0.40.0 解析通过后在原生测试发现 Java 21 不兼容，改为 0.38.1 再次解析通过。最终三端 classpath 使用 0.38.1；未打包。 |
| `python3 build/assistant-harness-2026-11-05/compile_desktop.py` | 当前完整共享／JVM／桌面源码，直接 K2 编译并使用本轮 Gradle 解析的新依赖 | **通过**，188 份源码；进程树峰值约 1.1 GiB。 |
| `python3 build/assistant-harness-2026-11-05/test_desktop.py` | 临时曲库、HTTP 替身、假模型及独立 Xvfb 原生 Compose 渲染 | **通过**，78 项／0 失败／0 跳过：音乐链接 5、工具会话 6、Search／Fetch 协议及取消／配置 8、聊天接口 2、助手 50、存储及控制 4、原生 UI 3。7 份测试源码编译通过；包含搜索→正文→引用、重复搜索只执行一次、关闭／代理不开放工具、没有意外音频下载、Markdown 可点击链接与可展开真实步骤。 |
| `python3 build/assistant-harness-2026-11-05/compile_android.py` | 本轮 Gradle 解析的 Android classpath，当前应用／仪器测试直接 K2 编译 | **通过**，180 份应用源码、29 份仪器测试源码，均退出码 0。仪器测试仅编译，未在设备运行，不代表 APK 或 Release 验收。 |
| `git diff --check`（本次文件）、文档链接核对和源码 SHA-256 守卫 | 本机工作区 | **通过**；最终串行验证期间全部 Kotlin 源码和测试哈希未变化，记录 `final-source-sha256.json`。保留其他未提交修改。 |

初次回归失败记录保留于 `first-validation/`：新网络测试的一项表达式测试方法返回异常对象而非 `Unit`，导致 JUnit 初始化失败，已显式声明 `Unit`；两个原生 UI 用例因 Markdown 0.40.0 的 Java 21 字节码失败，改为实际兼容 JDK 17 的 0.38.1 后上述最终 78 项均通过。没有更改 JDK 或放宽进程树内存限制。

已查看本轮原生图 `composeApp/build/assistant-harness/screenshots/research-and-sources.png`，白底文字、标题／加粗、来源下划线及步骤布局可读，测试来源域名和节目文字均为夹具，不是实际搜索结果。首次状态和最终结果分开保留，不以先前失败输出或旧产物冒充最终通过。

最终 `pipeline.py` 串行完成桌面全源码编译→78 项回归→Android 应用／仪器测试源码编译，systemd 服务 `Result=success`；整个过程峰值约 1.2 GiB，未达到 2 GiB 限额、没有开启 Swap 或关闭用户应用。常规弃用提示及既有 JSON 空值筛选冗余条件警告未阻断编译。没有执行全工程 Gradle 测试／Lint、ProGuard／Release 打包、设备安装、Windows 原生 GUI 或真实付费请求。

## 限制与后续

没有使用真实 DeepSeek Key 或执行付费联网请求；替身验证可以确认协议、调用边界和 UI，不能证明服务商实际可用、特定地区网页可读或真实模型能完成全部播客推荐。正文截断可能缺少页面后部信息，需要模型换一个相关页面；未新增 RSS 订阅或流媒体播客播放。

没有嵌入完整 DeepSeek Harness／Cordis／Node，没有通用文件系统或 shell agent。未进行整个播放器页面替换，未复制上述 GPL 播放器源文件。未打包、安装或发布本轮 Android／Windows／Linux 客户端；当前手机仍未具有本轮新增能力。保留这一交付边界，不把源码测试说成真机效果或 Harness 全功能等价。

## 同步更新的文档

`docs/README.md` 最新入口和变更索引；`docs/DEVELOPMENT.md` 第 13 节能力、网络、配置和原生 UI；`docs/THIRD_PARTY.md` 实际采用的上游模块、版本、许可与未复制的参考范围。
