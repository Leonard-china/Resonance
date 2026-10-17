# 2026-10-17 — 导入 MusicFree 核心歌源并优先下载录音室候选及记录来源

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 构建配置
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：优先使用歌源减少 Token，排除 MV/对白版本，区分专辑封面和视频缩略图。

## 需求或问题

原下载主要依赖 B 站搜索并转成 320k MP3，不能由输出码率证明源音质。用户希望下载时尽力找录音室版本、使用其他歌源，查看真实来源并优先获取专辑封面。

## 原因与依据

原 BilibiliClient.candidateScore 给 MV 加分，音频导入统一编码；曲库没有持久化音源、封面分类和实际编码参数。歌单卡片大图不代表其每首歌都有封面，不能据此认定歌曲列表漏读同一张图。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | install / search / media / lyrics / evaluate | 本地 JS、HTTPS JS/订阅导入，启停卸载，MusicFree 核心接口；QuickJS 隔离执行，基础 axios/fetch/qs/crypto-js 兼容层 |
| [SourceNetwork.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/SourceNetwork.kt) | PublicSourceNetwork | 仅公网 HTTPS、DNS 检查、跳转检查、跨主机去请求头、响应/下载上限、超时与取消 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | obtain / importSourceInternal / recordOrigin / coverStep | 本地→启用歌源→可选 B 站；源内详情/完整音频/同条目歌词/封面，时长异常阻止进入推荐；保存来源与实际音频参数 |
| [BilibiliClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/media/BilibiliClient.kt)、[Mp3Import.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/media/Mp3Import.kt) | disallowedVersion / originalExtension / arguments | 自动候选硬过滤 MV/现场/翻唱/混音/剪辑，支持 UID 范围；兼容格式直接保留音轨，关闭开关时转 MP3 |
| [MusicSourcesScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicSourcesScreen.kt)、[TrackSourceDetails.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TrackSourceDetails.kt) | MusicSourcesScreen / TrackSourceDetails | 歌源管理、搜索核对/目标歌单下载、来源详情；不把来源标注当成已鉴定原版 |
| [SyncArtwork.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/SyncArtwork.kt)、两端 DynamicArtwork | tracksWithPlaylistArtwork / AlbumArtwork | 每首歌优先已有图片，再读内嵌封面；复用无歧义且有目录证据的同专辑图，提供缺图批量匹配 |
| [build.gradle.kts](../../composeApp/build.gradle.kts)、[资源说明](../../composeApp/src/jvmMain/resources/music-source/README.md) | jvmMain / Android resources | QuickJS 1.0.10、CryptoJS 4.2.0 及许可资源，两端原生接入 |

## 修改前后行为

| 场景 | 修改前 | 修改后（代码行为，待运行验证） |
| --- | --- | --- |
| 自动找歌 | B 站搜索、MV 有额外分数 | 先查已启用歌源，排除非录音室关键词，按设置决定 B 站补充 |
| 保存音频 | 固定转 320k MP3 | 默认兼容时复制音轨，记录实际参数，不声称升码率改善音质 |
| 查看封面 | 无来源分类 | 显示目录专辑图/歌源标注图/视频缩略图/自选或未知 |

## 平台与数据影响

共享新增 SourceSong、MediaOrigin 与来源界面；JVM 运行 JS/网络，两端读取本机实际编码信息、支持原格式入库。music-assistant 新增 sources/ 脚本与 musicSources、sourceOrigins、sourceSongs、mediaOrigins 索引；设置新增 preserveOriginalAudio、bilibiliFallback、preferredBilibiliUids。旧字段缺省兼容，旧音频不重编码、不批量改标签。插件不获得 JVM 反射、任意文件、B 站 Cookie 或 AI Key。安装检查禁止网络，单次运行限制 32MB 内存/512KB 栈/5秒执行与25秒协程时限。请求上限2MB、音频512MiB、封面传输10MiB且校验图片头。歌源可自主访问公网，用户需自行选用来源。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| git -c core.safecrlf=false diff --check | Windows 工作区 | 通过（代码收尾时执行） |
| MusicSourceRegistryTest 新增协议/持久化/crypto/取消/网络边界用例 | 测试源码 | 已编写，未运行 |
| 最终双端编译、真实歌源下载、原格式播放与发布包加载原生库 | Android / Windows | 未执行；用户要求后续不再构建发布，交给 Antigravity |

用户指令前的编译尝试与尚待验证项见 [交接](../HANDOFF-2026-10-17.md)，不能作为最终通过依据。

## 限制与后续

仅支持 MusicFree 核心协议，不等于全生态兼容；未支持的 npm/Node 模块明确失败。订阅导入可部分成功，不做整体回滚。未经真实歌源端到端测试；URL 返回完整音频、授权要求、平台可播性均待核验。版本关键词、目录时长和账号无法保证录音室版本或母带音质。未内置声称“保音质”的 B 站白名单；UID 由用户填写。目录图匹配依赖歌名/歌手，具体再版/合辑封面仍可能有歧义。发布前检查 QuickJS 原生组件及许可随包情况。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/THIRD_PARTY.md、docs/HANDOFF-2026-10-17.md。
