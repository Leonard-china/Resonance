# 2026-10-16 — 为下载歌曲匹配真实专辑封面并缓存及保留视频兜底

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.0 / 23，本次按构建规则不自动升版
- 影响平台：共享、Android、Windows

## 需求、原因与依据

AI 通过 B 站下载时默认保存视频缩略图，歌曲封面观感差。按歌曲、歌手和版本找真实目录封面，比随意给歌手套一张照片更准确。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [enrich/AlbumCoverRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/AlbumCoverRepository.kt) | AlbumCoverRepository / MusicArtworkSource | 查询QQ音乐目录，并回退现有iTunes与酷狗元数据源；歌曲标题与歌手严格复核；缓存按歌曲查询和歌手/专辑索引到持久图片，同专辑复用；未命中缓存 24 小时，显式强制匹配可绕过。 |
| [features/JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | downloadAudio / coverStep / matchVideoCovers / setCover / match_album_cover | 新下载入库后独立匹配真实封面；未找到/失败保留视频图并记录可重试任务。任务页可批量匹配 videoOrigins 中既有歌曲；手选封面标记 customCovers，不被批量覆盖。播放菜单可单曲强制匹配或手选。AI 也有真实封面匹配工具。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 新下载 | 直接使用视频封面 | 自动尝试目录专辑封面，未命中保留视频图 |
| 已有下载 | 缺少批量替换入口 | 任务列表提供已下载歌曲专辑封面匹配 |
| 封面失败后重试 | 需要重新导入 | 只重试封面步骤，不重新下载音频 |

## 平台与数据影响

两端使用相同匹配/磁盘缓存；更新曲库覆盖值和歌单读取结果，不重编码既有音频。导入 MP3 的初始内嵌封面仍可能是视频图，应用以已匹配曲库封面为准。查询只发送歌曲元数据，不发送音频或模型 Key。

## 验证

本次实际构建、测试、签名及产物结果统一见 [构建验证记录](2026-10-16-08-release-build-verification.md)。相关测试已随本次修改加入源码；不将旧版结果用作本次通过证据。

## 限制与后续

AlbumCoverRepositoryTest验证目录中现场/翻唱排除及指定专辑选择；MusicAssistantTest验证封面单独重试、音频下载次数不增加、记录重启恢复。公共来源不保证覆盖所有歌手/所有版本；封面网络故障不阻断音频入库。

## 同步更新的文档

已更新 [开发说明](../DEVELOPMENT.md)、[文档索引](../README.md)、[本次发布版构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md)。

在线公开目录检查：QQ音乐查询《晴天 / 周杰伦》命中《叶惠美》，800像素JPEG返回HTTP200、183730字节，并人工核对图片；iTunes命中《Hello / Adele》的《25》。iTunes/酷狗对本次《晴天》查询未命中，故加入QQ目录。此检查不代表所有歌曲均能匹配。
