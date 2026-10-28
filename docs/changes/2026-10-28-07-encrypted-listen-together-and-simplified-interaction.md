# 2026-10-28 — 引入双人加密一起听并极简交互体验

- 修改日期：2026-10-28（Asia/Shanghai）
- 类型：功能 / 交互优化
- 适用版本：`0.6.3 / 37`（尚未升版）
- 影响平台：共享（Compose Multiplatform）/ Android / Windows
- 关联记录或需求：
  - 用户反馈：“和朋友一起听的操作方式过于复杂，很多人不会用”
  - [2026-10-28-06 完成回弹版真机回归](2026-10-28-06-device-motion-diagnostics-and-regression.md) 中关于“直接加密一起听需求”的后续

## 需求或问题

1. 用户希望在 Resonance 中能够与朋友实时同步听同一首歌，但坚持隐私第一原则：不依赖中心化服务器账号，不中转或泄露音频数据，采用点对点端到端加密传输。
2. 初始实现中，操作流程偏极客化，普通用户容易感到困惑：
   - 房主界面直接暴露“房间端口（45873）”、原始 IP 列表与外部端口映射配置，门槛较高；
   - 朋友加入时必须手动将纯 `resonance-listen://...` 邀请粘贴至多行输入框，若包含聊天前后缀或标点符号会直接引发 URI 解析异常；
   - 缺少剪贴板自动侦测、零配置建房和通俗易懂的连接指南。

## 原因与依据

1. **去中心化与端到端加密架构**：
   - 采用纯 JVM / Android 原生 TLS 1.3 / 1.2 握手协议，房主端生成临时自签名证书与 SHA-256 证书指纹（PIN）及一次性随机密钥，朋友端使用该指纹锁定握手目标，彻底杜绝中间人攻击；
   - 音频数据分块流式传输并校验 SHA-256，仅存放于临时缓存目录，断开或退出后自动清理，不污染本地曲库。
2. **极简用户体验与容错设计**：
   - **智能剪贴板一键提取**：进入“与朋友一起听”时自动探测剪贴板，若存在一起听邀请，以高亮卡片提示“检测到朋友发来的一起听邀请”，提供【一键加入】；同时提供【从剪贴板粘贴并加入】快捷按钮。
   - **自然语言消息容错**：优化 `TogetherInvitation.parse`，使用精准正则 `Regex("""resonance-listen://[^\s"'<>\u4e00-\u9fa5，。！？、]+""")` 从任何微信、QQ 聊天分享文本中提取核心直连链接，即使朋友连同说明文字一起复制也能秒解析。
   - **房主零配置一键开房**：默认隐藏本地端口设置，自动匹配可用局域网 IP；一键复制包含亲切问候与直连链接的完整分享文案；高级端口与端口映射收敛至折叠项。
   - **状态可视化与常见问题**：展示大字号 4 位房间核验码、同步播放状态卡片及折叠式图文连接 FAQ。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `ListenTogetherService`, `TogetherState` | 定义一起听服务契约、角色、连接状态及只读事件流。 |
| [TogetherTransport.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherTransport.kt) | `TogetherInvitation`, `TogetherTls` | 临时 TLS 证书管理与邀请编码/解码；增加自然语言与标点符号包容正则解析。 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `JvmListenTogetherService` | 跨平台 JVM 双向 TLS 握手、点对点流式音频分块传输及播放进度秒级同步。 |
| [TogetherPlaybackBinding.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherPlaybackBinding.kt) | `TogetherPlaybackBinding` | 将远端播放帧绑定到本地播放器状态，防并发启动与未授权本地播放拦截。 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | `TogetherScreen`, `TogetherFaqItem` | 重塑极简交互界面：剪贴板智能提取卡片、一键建房、友好文案复制、折叠高级设置与常见问题指南。 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `ProfileScreen` | 在工具箱界面新增“与朋友一起听（双人直连 · 端到端加密 · 无中转）”入口。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `LibraryDestination.Together` | 增加导航目标，并在桌面侧栏与移动端手势返回中妥善支持。 |
| [ListenTogetherTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/ListenTogetherTest.kt) | `ListenTogetherTest` | 增加包裹自然语言与中文标点符号的邀请解析回归测试。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 房主创建房间 | 必须面对 45873 端口输入框与复杂网络概念 | 直观展示房主模式，点击【创建双人房间】一键开启，默认选中最优局域网 IP |
| 邀请朋友 | 仅复制生硬的链接，手动选择 IP 困难 | 一键复制带友好文案的消息；提供局域网 IP 标签切换，端口映射收纳至高级设置 |
| 朋友加入房间 | 需手动长按粘贴纯链接，误带中文或标点即报错 | 打开页面自动识别剪贴板邀请并提供【一键加入】；支持粘贴整段微信/QQ消息自动提取 |
| 连接指引 | 折叠的生硬技术长文 | 结构化的常见问题卡片，解答局域网直连、异地公网 IPv6/端口映射与隐私安全原理 |

## 平台与数据影响

- **跨平台共享**：Android 与 Windows 均通过共享原生 Compose 界面与 JVM TLS 引擎实现，协议完全互通。
- **数据与音频安全**：跟听音频仅临时存储在 `cache/together-audio/`，退出或切歌时安全清理，不写入本地曲库，不修改数据库。
- **系统权限与网络**：需局域网或公网直接通信能力，不发起任何向外部云服务器的中转请求。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --tests "com.resonance.player.together.*"` | Windows 11, JDK 17 | **通过**。包含 JSON 协议校验、自然语言邀请提取、IPv6 往返及临时缓存清理全部通过。 |
| `.\gradlew.bat :composeApp:desktopTest --tests "com.resonance.player.ui.UiVisualContractTest"` | Windows 11, JDK 17 | **通过**。包含 TogetherScreen 在桌面端宽屏、手机端多字号及深浅主题下的视觉渲染契约测试。 |
| `.\gradlew.bat :composeApp:compileDebugAndroidTestKotlin` | Windows 11, JDK 17 | **通过**。Android 端自动化测试代码全部编译成功。 |
| `adb shell am instrument -w -r -e class com.resonance.player.TogetherUiDeviceTest ...` | 真机 VKJ-AN00 (Android 16) | **通过**。真机上 2 项 UI 自动化交互与状态断言全部通过（6.9s）。 |
| `adb shell am instrument -w -r -e class ... 27 项核心测试` | 真机 VKJ-AN00 (Android 16) | **通过**。真机上 27 项交互、导航、音频会话及设备直连测试全部通过（60.5s）。 |
| `.\gradlew.bat :composeApp:desktopTest` | Windows 11, JDK 17 | **通过**。全量桌面端单元测试全部执行通过（4m 46s）。 |

## 限制与后续

- 本次优化已彻底解决“与朋友一起听”的操作复杂度问题。
- 后续可直接配合 Mineradio 音乐舞台 UI 进行版本发布（v0.6.4）。

## 同步更新的文档

- [文档入口](../README.md)：更新变更索引。
- [开发说明](../DEVELOPMENT.md)：记录一起听直连架构与交互更新。
