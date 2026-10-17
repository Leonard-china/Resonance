# 2026-10-16 — 第三方组件、模型服务、B 站和本地凭据

精确依赖见 [Gradle](../composeApp/build.gradle.kts) 与 [版本目录](../gradle/libs.versions.toml)；本文说明实际调用和发送内容，不代表所有外部服务已在线验通。

## 组件

| 组件 | 用途 |
| --- | --- |
| Kotlin / Compose / Material / Coroutines | 两端共享 UI/状态/任务 |
| Haze / haze-blur | 原生界面材质 |
| AndroidX Activity / DocumentFile / Media3 | 权限/文件选择/音频与媒体会话 |
| WorkManager 2.10.1 | Android 后台推荐及 dataSync 下载通知 |
| FFmpegKit audio 8.1.7 / Smart Exception 0.2.1 | Android 音频转换/整曲响度；显式补齐 Java facade 依赖 |
| 外部 ffmpeg.exe | Windows 播放解码/转换/响度 |
| jaudiotagger 3.0.1 | Windows 标签/封面/歌词读写 |
| JNA platform 5.17.0 | Windows DPAPI 与歌词窗口点击穿透 |
| sqlite-jdbc 3.47.1.0 | 本机酷狗数据库密钥，不是曲库数据库 |
| org.json 20250517 | 两端 JSON 编解码 |
| OkHttp 4.12.0 / Okio | 两端 WSS 语音识别连接，遵循组件 Apache 2.0 许可 |

ZXing 和 jdk.httpserver 随 LAN/二维码功能删除。KGG 实现注释记录 FlyMouseFormat 移植来源，发布应核对上游声明/许可；仓库无项目级 LICENSE。Windows FFmpeg 非固定 Gradle 二进制，需对实际 ffmpeg -version/-buildconf、许可证与对应源码分发条件核验；本文不声称已完成新 Release 许可证审计。Android 依赖为 audio 包，发布仍按实际内容核验。

## 外部请求

| 服务 | 地址/调用 | 发送或接收 |
| --- | --- | --- |
| LRCLIB | https://lrclib.net/docs | 歌名/歌手/专辑/时长，返回歌词 |
| iTunes Search | https://itunes.apple.com/search | 搜索词、曲目信息/封面 |
| 酷狗元数据 | complexsearch.kugou.com | 曲目搜索/封面；不是歌单分享链接导入 |
| B 站 | api.bilibili.com、b23.tv、bilibili.com、允许的媒体 CDN | 视频ID/搜索/WBI，可选自有 Cookie；公开可访问音轨/封面 |
| 通义千问 | dashscope.aliyuncs.com/compatible-mode/v1 | 文本、工具、选中的截图和歌词，Bearer Key |
| DeepSeek | api.deepseek.com/v1 | 文本/工具/歌词，Bearer Key；图片使用另选的视觉提供商 |
| 豆包/火山方舟 | ark.cn-beijing.volces.com/api/v3 | 用户实际模型/接入点支持的文本、工具、图片 |
| 自定义兼容接口 | 用户指定 HTTPS /chat/completions | 由用户选择服务，需支持相关工具/视觉消息 |
| 天气/定位 | Open-Meteo 等现有 WeatherClient/平台定位来源 | 城市、粗坐标/IP 信息 |
| 内置网易云歌源 | music.163.com、*.126.net | 歌曲/歌词公开查询、外链公开播放与下载 |
| 内置酷我歌源 | search.kuwo.cn、antiserver.kuwo.cn、m.kuwo.cn、*.kuwo.cn | 歌曲/歌词公开查询、MP3音频直链与专辑图 |
| 内置酷狗歌源 | songsearch.kugou.com、m.kugou.com、imge.kugou.com | 歌曲/歌词公开查询、免费音频直链与专辑图 |

AI 请求发稳定曲库摘要、歌单、部分元数据/消息/拒绝原因，不上传整库音频；OCR 发送所选截图分片，翻译发送歌词。OCR/译文本地缓存；服务商前缀缓存有其有效期，不保证跨日命中。Token 预算是本地预估与计数，不是人民币账单限额。

## 凭据和权限

新连接 Key/B站 Cookie 在 Windows 用用户 DPAPI、Android 用 AndroidKeyStore AES-GCM 加密；JSON 原子文件有上一版备份。旧 DeepSeek 配置兼容保留，可能仍有历史明文；不得宣称迁入新设置就清除了旧文件。不要把用户配置、音频、Cookie、Key 或签名文件提交仓库。

HTTPS 模型请求禁止重定向携带 Key，错误回显隐藏实际 Key。B 站 Cookie 只发给其页面/API 主机，媒体下载限制允许的 CDN；认证/验证/HTTP412 直接提示，不绕过访问限制。用户手选本地封面允许读取；AI 封面参数只允许公开 HTTPS B 站图片。

Android 悬浮权限 SYSTEM_ALERT_WINDOW 由系统设置授予；后台推荐使用 WorkManager/网络/存储约束及 dataSync 前台服务，不申请管理所有文件权限。备份关闭。Windows 关闭主窗进入托盘，主动退出停止后台任务。

## 实现参考

- [千问视觉](https://help.aliyun.com/zh/model-studio/vision-model/)、[工具调用](https://help.aliyun.com/zh/model-studio/qwen-function-calling)、[上下文缓存](https://help.aliyun.com/zh/model-studio/context-cache)
- [DeepSeek API](https://api-docs.deepseek.com/zh-cn/)、[缓存机制](https://api-docs.deepseek.com/zh-cn/guides/kv_cache/)
- [火山方舟文档](https://docs.volcengine.com/docs/ark?lang=zh)
- [WorkManager 2.10.1](https://developer.android.com/jetpack/androidx/releases/work#2.10.1)
- [JNA Crypt32Util 源码](https://github.com/java-native-access/jna/blob/master/contrib/platform/src/com/sun/jna/platform/win32/Crypt32Util.java)
- [yt-dlp Bilibili extractor](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/bilibili.py)：正常 WBI/音轨请求的实现参考；项目没有引入 yt-dlp 运行依赖。

来源仅供实现/后续排查，不构成外部接口长期可用承诺。本轮真实服务验证限制见 [验证记录](changes/2026-10-15-12-verification-and-handoff.md)。

## 音频同步歌词服务（2026-10-16）

新增百炼 Paraformer 实时 WSS 音频识别：模型 paraformer-realtime-v2，默认北京接口；也可填写相同协议的 WSS 地址。仅主动点击生成或确认收费重试时，将整首16 kHz单声道PCM上传，最多30分钟；无需麦克风权限。不自动调用付费识别。Key独立配置或复用千问Key，沿用DPAPI/AndroidKeyStore加密。任务参数不写入Key。

音频按时长收费，与聊天Token预算不同；本机按整曲时长保守预留月秒数，失败/取消可能仍计费。此计数不是供应商人民币账单。生成结果与原文备份保存在本机，之后按指纹缓存复用。

真实专辑封面使用QQ音乐、iTunes/酷狗公开元数据来源，不上传音频；无匹配继续显示视频封面。服务覆盖和在线可用性无法保证。

- [WSS协议](https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service)
- [客户端事件](https://help.aliyun.com/zh/model-studio/paraformer-client-events)
- [服务端时间戳字段](https://help.aliyun.com/zh/model-studio/paraformer-server-events)

## 独立官网（2026-10-16）

`website/requirements.txt` 单独管理 Flask 3.1.3、Pillow 12.3.0、Ubuntu Gunicorn 26.2.0，不改变播放器 Gradle 依赖。SQLite、SMTP、TLS 和密码/会话处理分别来自 Python 标准库与 Flask/Werkzeug。没有第三方统计、CDN 字体或浏览器侧邮件 SDK。

官网只保存访客主动提交的帖子/回复/截图、邮箱订阅偏好与版本信息，不读取播放器曲库。截图经重编码移除 EXIF；匿名限速使用带站点密钥的 IP HMAC，不在 SQLite 存原始 IP。邮箱不出现在公共页面，版本/推广分别确认、独立发送，TLS/STARTTLS 邮件仅交给作者配置的 SMTP 服务，真实 SMTP 尚未验证。

现网 leonard.asia 经 Cloudflare Tunnel 提供 HTTPS，运行于用户配置的 Pterodactyl 容器；本机独立运维客户端使用面板 API。Cloudflare 处理网站流量，面板控制服务器文件/进程，均不是播放器云端账户。面板配置、隧道 token 和作者凭据不进入本仓库。实际路径及验证范围见 [现网运维](../website/OPERATIONS.md)。

当前应用图标来自 `composeApp/src/desktopMain/resources/resonance-icon.png`；官网截图复制工程已有测试素材，不含用户音频。GitHub 按钮与历史版本跳转 `Leonard-china/Resonance`；最新版二进制来自作者在网站发布的本地存储。没有把未经提供的收款码或真实凭据作为静态资源提交。

## 本地截图 OCR 与截断名称核对（2026-10-17）

Android 和 Windows 随包内置 PP-OCRv5 mobile 检测/识别模型与字典，ONNX Runtime（Android 1.23.2 / Windows 1.20.0） 在 CPU 推理；两者为 Apache-2.0，模型原始来源、SHA-256、许可和复现脚本见 [模型说明](../composeApp/src/ocrModels/README.md)。本地不加载 Python、不向模型服务上传截图、不调用收费识图 API；设置可手动选择原云端视觉 API。会话按识别任务创建并关闭。

截断名称核对只向 QQ音乐/iTunes/B站发送可见歌名与歌手等文本，用匹配结果补全新导入信息；没有把缺失文字当作 OCR 已识别内容。旧 Key/视觉连接配置保留，识图模式作为独立设置存储。


## MusicFree 核心歌源运行时（2026-10-17，待运行验证）

- [MusicFree插件协议](https://musicfree.catcat.work/plugin/protocol.html)与[基础类型](https://musicfree.catcat.work/plugin/basic-type.html)为接口参考。没有内置第三方下载插件或可承诺授权/音质的歌源库，兼容核心方法不等于完整生态兼容。
- [QuickJS Kotlin](https://github.com/dokar3/quickjs-kt/tree/v1.0.10)依赖 `io.github.dokar3:quickjs-kt:1.0.10`，包装层Apache-2.0，底层QuickJS等原生组件按各自上游许可；发布时核对Android/Windows原生库与随包许可。未向脚本暴露JVM/文件/AI Key/B站Cookie。JS宿主仅受限公网HTTPS、Base64及安全随机数，执行有内存/栈/时间与取消限制。
- CryptoJS4.2.0，MIT。原始npm包 https://registry.npmjs.org/crypto-js/-/crypto-js-4.2.0.tgz ，未修改的JS与MIT许可置于 `composeApp/src/jvmMain/resources/music-source/`，按需在受限JS上下文加载。JS SHA-256为 `ee02257ffbaf0a9b481c7039b0f3bb20c360c9674fe4be8b38ae709b2ea59bbe`；详见[资源说明](../composeApp/src/jvmMain/resources/music-source/README.md)。axios/fetch/qs为本项目有限兼容实现，未引入对应完整npm运行环境。

歌源网络只允许公网HTTPS443，拒绝本地/内网DNS结果与URL用户信息，接口响应2MB、音频512MiB、封面10MiB上限；请求与重定向遵循SourceNetwork.kt。启用插件可向其配置的公网服务发送用户搜索词及请求头，卸载保留下载音频。无自动登录、Cookie共享或自动绕过收费/访问限制。来源的音质/版本/封面声明未通过母带鉴定；实际文件参数只描述编码。真实插件、双端原生库加载、发布许可检查尚未执行，见[交接](HANDOFF-2026-10-17.md)。


### 推荐评价数据（2026-10-17）

个人点赞、踩、500字符评论保存在本机music-assistant JSON。保存反馈不发模型；普通歌源推荐仅将加权歌手/明确风格查询词发送用户启用的歌源。用户使用AI精选或模型助手时，最多最近24条评价摘要（每条评论160字符）可进入所选模型上下文，受原预算限制。没有新增社交评论服务器、账号或第三方服务；关于页更新摘要从构建时对应版本发布文档内置，打开页面不联网。
