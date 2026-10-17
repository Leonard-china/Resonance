# 2026-10-17 — 统一本地与在线搜索并增加候选试听和换来源

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

在线找歌混在歌源插件管理页里，新用户需要先理解歌源概念；已有空曲库引导组件没有接入主列表。候选只能核对文字再下载，难以先判断是否为想要的录音版本。

## 原因与依据

DiscoverScreen 原来只查本地索引，MusicSourcesScreen 自己维护另一套搜索和下载弹窗；多源结果直接取前 40 项，可能全部来自第一个源。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DiscoverScreen / EmptyLibrary` | 曲库搜索增加本地与在线切换，保留关键词；真正空曲库接入搜索和导入入口。 |
| [OnlineSongSearch.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/OnlineSongSearch.kt) | `OnlineSongSearch` | 统一在线候选、目标歌单确认、下载错误就地显示、同歌换来源和停止试听。 |
| [MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | `startPreview / stopPreview / searchSources` | 管理试听代次、取消、30 秒停止、离开页面清理以及跨搜索页的候选状态。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `prepareSourcePreview / clearSourcePreview` | 沿用公网校验网络准备最多 64 MiB 的临时音频，不导入曲库，不创建永久音频记录。 |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | `search` | 按来源交错排列候选，限制总数时仍保留其他源。 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `previewAudio / stopAudioPreview` | 使用独立 ExoPlayer；Windows 对应独立 DesktopPcmPlayer，暂停主播放器后试听。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 搜索只搜本地，在线搜索位于管理页下方；没有试听，首源可能占满结果。 | 曲库可切换在线找歌；核对前可试听最多 30 秒或换来源，确认后下载到曲库／目标歌单。 |

## 平台与数据影响

共享 UI 与 JVM 网络，两端均实现独立试听。最多下载 64 MiB 临时文件，沿用 DNS、公网和重定向边界，不绕过试听标记。试听会暂停当前音乐，结束后由用户主动恢复。离开页面、停止或取消后释放播放器和临时文件；不改源音频标签，不改变同步包。未增加模型请求。

## 验证

MusicAssistantTest 验证临时文件上限、不入库、不调用模型、清理与试听标记拦截；MusicSourceRegistryTest 验证多源交错。双端 Kotlin 编译已通过。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

试听需要先准备临时音频，超过 64 MiB 会失败；原始格式的真实解码、蓝牙和音频焦点还需双端设备实测。公开歌源可能失效，不能把夹具测试视为在线服务验证。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

