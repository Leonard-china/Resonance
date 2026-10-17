# 2026-10-17 — 修复歌源网络公网协议限制、接口跳转与多候选智能回退机制

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：Bug 修复 / 体验优化
- 适用版本：0.4.8 / 31（保持当前版本，不改变版本号）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：用户反馈截图与问题“似乎歌源很难使用”、“还有这怎么回事”

## 需求或问题

1. **大面积报“歌源只支持公网 HTTPS 地址”**：用户在音乐助手生成每日推荐或下载歌曲时，大部分歌曲（如《慢慢喜欢你》、《花之舞》、《千与千寻》、《起风了》等）报错中断，提示“歌源只支持公网 HTTPS 地址”。
2. **“下载时长与专辑目录不符，未加入推荐”**：推荐部分热门歌曲（如《夜曲》）时，因平台仅提供十秒试听片段导致时长校验失败，且单一源失败后未尝试其他歌源或 B 站备选，导致整批推荐失败并报错“本次没有可用的新推荐，旧推荐保留。夜曲：下载时长与专辑目录不符，未加入推荐；请在下载与空间中检查”。

## 原因与依据

1. **国内主流音乐平台音频直链多为 HTTP 协议**：
   - 网易云外链解析接口 `https://music.163.com/song/media/outer/url?id=...` 会 302 重定向到 CDN 实际音频地址（如 `http://m701.music.126.net/...`）。
   - 酷我音乐 `anti.s` 接口返回的 CDN 音频地址多为 `http://...sycdn.kuwo.cn/...` 或 `http://kw-bj.kuwo.cn/...`。
   - 原 `PublicSourceNetwork.publicUrl` 校验逻辑强制要求 `uri.scheme == "https"`，拦截了所有合法的公网 HTTP 音频流下载与重定向，抛出“歌源只支持公网 HTTPS 地址”。
2. **歌源接口重定向被阻断**：
   - 原 `PublicSourceNetwork.request` 包含 `require(!response.isRedirect)`，当歌源 API 存在 301/302 重定向（如 HTTP 跳转 HTTPS 或网关转发）时直接抛出“歌源接口发生跳转，请更新源地址”。
3. **试听片段与单源单次尝试缺乏回退机制**：
   - 酷我等平台针对版权或 VIP 歌曲仅提供试听切片（`listen_fragment === '1'`，约 10~15 秒），Resonance 原有的整曲时长与目录校验（`|expected - actual| > maxOf(8, expected / 25)`）准确识别出非完整版音频并报错。
   - 原 `obtain` 与 `recommendInternal` 逻辑仅取首个歌源的首项候选（`firstOrNull`），一旦该源因版权限制、试听切片或下载网络波动失败，直接终止该歌曲，既没有尝试已启用的其他主流歌源（如网易云、酷狗），也没有回退至启用的 B 站补充来源。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [SourceNetwork.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/SourceNetwork.kt) | `publicUrl` / `request` / `download` | 1. `publicUrl` 统一放行合法的公网 `http` 与 `https`（标准 80/443 及常用 8080/8443 端口），结合 DNS 解析继续严格阻断内网回环与私网 IP（防 SSRF）；<br>2. `request` 与 `download` 均支持最多 5 次安全重定向，每跳均通过 `publicUrl` 校验；<br>3. 适配 OkHttp `toHttpUrlOrNull` 保证特殊字符解析鲁棒性。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `obtain` / `recommendInternal` / `importSourceInternal` | 1. `obtain` 遍历所有已启用歌源的匹配项，当前歌源失败自动尝试下一个歌源，全部歌源均不可用时无缝回退至 B 站录音室单曲；<br>2. `recommendInternal` 对直接推荐候选增加兜底，候选源下载异常时转入 `obtain` 全源/B 站检索，极大提升推荐成功率；<br>3. 封面图与歌词链接支持 HTTP 协议。 |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | `lyrics` / `recommendations` | 1. `lyrics` 外部 lrc 链接支持 HTTP；<br>2. `recommendations` 在新安装或无历史种子时默认以热歌/流行等经典维度拉取首批候选，杜绝空白候选。 |
| [kuwo.js](../../composeApp/src/jvmMain/resources/music-source/builtin/kuwo.js) | `search` / `getMediaSource` | 1. `search` 解析 `item.payInfo.listen_fragment`，标明 `isTrial: true`，让试听切片在下载前即被过滤并自动转去其他歌源；<br>2. `getMediaSource` 去除 `MUSIC_` 前缀保护。 |
| [kugou.js](../../composeApp/src/jvmMain/resources/music-source/builtin/kugou.js) | `getMediaSource` | 移除将原音频地址强制替换为 `https://` 的代码，保留 CDN 返回的原始协议。 |
| [DynamicArtwork.desktop.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DynamicArtwork.desktop.kt) | `loadDesktopArtwork` | 封面图网络加载支持 `http://`。 |
| [DynamicArtwork.android.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/DynamicArtwork.android.kt) | `loadAndroidArtwork` | 封面图网络加载支持 `http://`。 |
| [MusicSourcesScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicSourcesScreen.kt) | `MusicSourcesScreen` | 提示文案与导入按钮输入框放开 HTTP 协议限制。 |
| [MusicSourceRegistryTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicSourceRegistryTest.kt) | 测试用例 | 验证 HTTP 与 HTTPS 均可通过 `publicUrl`，内网 IP 与凭据继续被拦截；新增空种子推荐默认热歌拉取回归测试。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 网易云 302 重定向至 126.net HTTP CDN | 报错：`歌源只支持公网 HTTPS 地址`，下载中断 | 正常跟随 302 重定向，成功下载完整 MP3 音频 |
| 酷我音频返回 HTTP 地址（如 `http://kw-bj.kuwo.cn`） | 报错：`歌源只支持公网 HTTPS 地址` | 正常下载并完成转码与标签写入 |
| 歌源 A 仅提供试听片段（如《夜曲》10秒） | 报错：`下载时长与专辑目录不符，未加入推荐`，整批推荐可能因此失败 | 歌源插件预判试听标为 `isTrial` 或时长不符后，自动尝试歌源 B、歌源 C 或 B 站完整音源 |
| 音乐助手通过歌源检索下载歌曲 | 仅试探首个源的首个候选项，失败即告吹 | 循环轮询所有已启用的匹配歌源候选，直至成功获取或回退 B 站 |
| 新安装或曲库全空时拉取每日推荐 | 候选集为空，直接保留旧推荐 | 自动采用热歌与流行经典候选种子，开箱即可生成推荐 |

## 平台与数据影响

- **跨平台共享逻辑**：`SourceNetwork`、`MusicSourceRegistry`、`JvmMusicAssistant` 两端共用，Desktop 与 Android 表现一致。
- **Android 端**：`AndroidManifest.xml` 原本已配置 `android:usesCleartextTraffic="true"`，修复后直接支持主流 CDN 的 HTTP 下载。
- **数据与安全兼容**：`PublicSourceNetwork.publicAddress` 的内网 IP、回环地址、云元数据地址与私有网段过滤保持严格生效，仅放行公网合法 HTTP/HTTPS 流量，完全满足沙箱安全隔离要求。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `.\gradlew.bat :composeApp:desktopTest --tests "com.resonance.player.features.MusicSourceRegistryTest"` | Windows 11, JDK 17 | **通过**：HTTP/HTTPS 协议放行、内网拦截、QuickJS 异步执行、三大内置歌源初始化与空种子默认热歌候选全量通过（耗时 27s） |
| `.\gradlew.bat :composeApp:desktopTest --tests "*RecommendationPolicyTest*" --tests "*MusicAssistantTest*" --tests "*MusicJsonStoreTest*"` | Windows 11, JDK 17 | **通过**：推荐评分、去重、历史冷却、多源回退与存储测试全量通过（耗时 28s） |
| `.\gradlew.bat :composeApp:allTests :composeApp:assembleDebug` | Windows 11, JDK 17, Android SDK 36 | **通过**：桌面 JVM 测试与 Android 单元测试全部通过，Debug APK 编译成功（耗时 3m 55s） |
| `website\.venv\Scripts\python.exe -m unittest discover -s website\tests` | Python 3.12 虚拟环境 | **通过**：全量 42 项网站和论坛测试通过（耗时 17.3s） |

## 限制与后续

1. **真实物理设备回归**：因宿主机当前无连接的 Android 硬件手机，真机网络连通性依赖单元与集成测试验证。
2. **歌源服务可用性**：第三方公共歌源服务稳定性取决于各平台公开接口，若平台彻底下线未认证公共接口，需通过多源回退或 B 站补充来源获取。

## 同步更新的文档

- [docs/README.md](../README.md)
- [docs/DEVELOPMENT.md](../DEVELOPMENT.md)
