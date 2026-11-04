# 2026-11-04 — 增加 QQ 与独立音乐歌源并兼容 VPN Fake-IP 解析

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：功能 / 网络兼容修复
- 适用版本：0.6.5 / 39；未升版
- 影响平台：Android、Windows、Linux 共享 JVM 实现
- 关联需求：软件内置更多歌源；歌词公网联测失败。

## 需求或问题

原内置网易云、酷我、酷狗三源；用户要求增加。联测发现本机 VPN 将 `music.163.com`、`c.y.qq.com`、`archive.org` 解析为 198.18/15 Fake-IP，原公网检查正确拒绝此保留地址，导致脚本请求在 HTTP 前失败。用户截图含 VPN 标志，但未确认手机使用相同 Fake-IP 模式。

## 原因与依据

歌源网络始终检查实际解析结果。不能直接放行 198.18/15 或移除内网限制。本轮增加仅对该明确 VPN 形态触发的独立 HTTPS DNS 核验，仍连接核验后的公网地址。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [qq.js](../../composeApp/src/jvmMain/resources/music-source/builtin/qq.js) | `search/getMusicInfo/getMediaSource/getLyric` | QQ 公开目录与同步歌词；匿名 vkey 仅使用实际返回的公开完整音频地址，付费/无地址明确失败 |
| [archive.js](../../composeApp/src/jvmMain/resources/music-source/builtin/archive.js) | `search/getMediaSource` | Internet Archive Netlabels 独立音乐目录；检索后读取单曲文件元数据，过滤受限/私有/过大文件并去重不同编码 |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | `BUILTIN_SCRIPTS`、`ensureBuiltinSources`、`remove`、`search` | 3→5；一次性增量预置，保留现有脚本、启停与卸载选择；搜索最多并发 3 源，结果顺序稳定、保留失败来源提示 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `builtinStartup` | 初始化后再供歌词使用，并刷新来源状态 |
| [PublicSourceDns.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/PublicSourceDns.kt)、[SourceNetwork.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/SourceNetwork.kt) | `PublicSourceDns.lookup` | 只有系统结果全为 Fake-IP 且为域名时调用固定 Cloudflare HTTPS DNS A 查询；响应≤64KB、期限≤8 秒、缓存≤128 域名/60 秒；最终所有 IP 仍须通过原公网检查 |
| [MusicSourcesScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicSourcesScreen.kt) | 歌源说明 | 列出五源，说明授权/地区/网络影响，不承诺曲目全可播 |
| [BuiltinMusicSourcesTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/BuiltinMusicSourcesTest.kt)、[PublicSourceDnsTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/PublicSourceDnsTest.kt)、[MusicSourceRegistryTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicSourceRegistryTest.kt) | 原始脚本/迁移/DNS 回归 | QQ 免费解析/付费拒绝、歌词；Archive 单曲过滤；新增源不覆盖用户选择；Fake-IP 缓存、公网正常解析不额外联网、私网/混合/字面 IP 拒绝 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 新用户内置来源 | 网易云、酷我、酷狗 | 原三源 + QQ 音乐 + Internet Archive Netlabels |
| 旧配置升级 | 不会自动加入新脚本 | 仅增补两个新源一次，已有配置与脚本不覆盖 |
| 用户卸载后重启 | 没有增量预置版本语义 | 记录卸载 tombstone，增量过程不重新安装；主动恢复仍可恢复全部五源 |
| 系统 DNS 返回 Fake-IP | 公网保护拒绝，无法查询 | 独立 HTTPS DNS 核验，连接核验后的公网地址 |
| 普通 DNS、内网或直接 IP | 公网允许／内网拒绝 | 规则保留，普通公网解析不额外调用 DNS 服务 |

## 平台与数据影响

`music-assistant/music-assistant.json` 新增 `builtinSourcesVersion=2`、`removedBuiltinSources`（ID 数组），旧配置兼容；安装仍有最多 20 源限制，满额需手动调整，不覆盖现有源；不迁移音频、歌单或模型凭据。内置脚本为本项目实现，未复制第三方插件。QQ 接收搜索词和曲目 ID；Archive 接收搜索词、条目/文件 ID；仅在 Fake-IP 时 Cloudflare 接收待解析域名，不发送 URL 路径、搜索词、API Key 或音频。既有请求体/下载上限、跨主机跳转去头与私网拒绝保留。无登录、Cookie 共享或付费绕过。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| Wrapper `:composeApp:desktopTest`（显式开启公网歌词验收） | Linux / JDK 17 | 通过：346 项，337 通过、9 项集成入口跳过；发生于最终歌词 UI 间距调整之前 |
| 最终 8 类针对性测试 + `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease` | 最后源码，Linux / Android SDK 36 | 通过：33 项全部通过、0 跳过；Debug／测试 APK 成功，Lint 0 Error／107 Warning |
| QQ 媒体／歌词、Archive 单曲过滤、5 源增量及 DNS 安全规则 | 最终针对性回归 | 通过；付费媒体不请求、受限文件不出现，私网与直接 Fake-IP 继续拒绝 |
| QQ 所报歌曲目录与歌词 | 原生 QuickJS＋真实网络 | 通过：找到温岚原版 240s，46 行同步歌词；匿名音频无 purl，不算该曲可播 |
| 新 Archive JS 检索及公开媒体 Range 请求 | Node 执行真实脚本／公网 | 通过：27 条，HTTP 206、audio/mpeg，读取 1024 字节后取消；不是整曲原生播放 |
| Android／Windows 最终原生来源验收 | 本轮 | 未执行；Android 断开，Windows 不在当前环境 |

完整命令、跳过名单、原生截图、最后源码摘要、产物校验与早期失败的处理见[本轮验证报告](../audit/2026-11-04/follow-up/VALIDATION.md)。

## 限制与后续

QQ 搜索和歌词可用不代表版权曲目可下载；本轮《夏天的风》匿名媒体接口没有返回可用 purl，按预期视为不可用。Archive 公开文件联测仅验证小段响应，未宣称整曲播放验收。所有来源可能因授权、地区、接口变化或网络失效；没有运行 Windows 或最终 Android 真机歌源验收。新增 DNS 兼容只覆盖全为 198.18/15 的系统结果，不把其他不明地址或解析失败自动放行。

## 同步更新的文档

[文档索引](../README.md)、[开发说明第 8、10、17 节](../DEVELOPMENT.md#music-source-update)、[第三方请求与边界](../THIRD_PARTY.md)。
