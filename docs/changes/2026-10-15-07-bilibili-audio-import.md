# 2026-10-15 — 新增 B 站音频导入及自定义歌曲信息

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：功能
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

用户希望从 B 站视频提取音轨并使用简洁歌名、正确歌手、专辑和封面，避免视频长标题直接作为歌曲信息。

## 具体修改

- 支持 BV、官方视频 URL、b23 短链接及分 P；读取 metadata 后独立编辑字段和本地封面/目标歌单。
- 正常 WBI 请求播放信息、选 DASH 高带宽音轨和备用地址；单段视频可作 fallback。
- 下载 staging 后两端原生 FFmpeg 转320k MP3，真实写入标签和封面，再 hash 入托管库；来源 bvid:cid 去重。
- 媒体主机/HTTPS/重定向/大小/超时受控；Cookie 可选、加密保存；401/403/412 明确提示官方验证/限制。

| 文件 | 关键修改 |
| --- | --- |
| [media/BilibiliClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/media/BilibiliClient.kt) | inspect/search/download/cover |
| [media/Mp3Import.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/media/Mp3Import.kt) | arguments/hash |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | download/obtain |
| [ui/MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | VideoImportDialog |

## 修改前后

以前无在线音频入口；现在填链接、改信息、下载入库并可加入歌单。

## 平台与数据影响

网络向 B 站发视频ID/搜索/可选Cookie；源阶段文件结束清理。两端写新 MP3 的标签，不宣称修改所有已有源音频。

## 验证

MusicFeaturesIntegrationTest 验真实标签/封面转码；MusicAssistantTest 注入音源验编排/去重。公共 metadata 探测 HTTP412，真实网络下载未验通。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

平台账号/区域/验证限制会失败，按原因提示，不绕过；原版打分不能保证音源真伪。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
