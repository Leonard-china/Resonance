# 2026-10-17 — 区分离线音频状态并支持恢复和重新定位

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

释放下载、原文件丢失及没有匹配音频共用“待匹配”，点播放只提示重新扫描；已有来源记录也缺少直接恢复入口。

## 原因与依据

曲库行只检查 sourceUri 是否为空，未核实文件可读性；下载去重也可能把已经丢失的 URI 当作可复用音频。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `TrackRow` | 按可读性、释放台账、恢复错误显示已释放下载／文件不可用／尚未匹配／来源不可用。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `startTrack / trackPendingRestore` | 先核实文件可用；用可取消请求保留快速切歌顺序，打开恢复、重新定位和换来源查找入口。 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | `audioFileAvailable / chooseReplacementAudio / saveReplacementAudio` | 两端提供实际文件检查和原生选择器；替换 URI 时保留歌曲 ID、收藏和歌单。saveReplacementAudio 使用 AndroidMusicAccess.update 或 DesktopLibraryStore.replaceAudio 显式保存新来源，覆盖原先不在托管索引或已被 load 排除的旧条目。 |
| [MusicLibraryAccess.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicLibraryAccess.kt) | `audioAvailable` | 下载去重改为核实文件，Android 额外支持 content URI 授权。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `restoreState / importSourceAudio / downloadAudio / obtain` | 暴露已释放 ID，并在恢复时避免复用已消失的源文件。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 没有 sourceUri 都显示待匹配；失效 URI 可能仍被当作已有音频。 | 显示具体状态；点选不可用歌曲后可以重新下载并播放、定位本地音频或换来源。 |

## 平台与数据影响

不删除任何原文件、不写回标签、不改变歌曲 ID 或同步格式。重新定位只更新 URI／MIME 与曲库、歌单快照；Android 通过 SAF 申请持久读取权限。恢复沿用既有所有权与来源逻辑，失败时保留信息和关系；选择器取消不修改记录。

## 验证

MusicAssistantTest、StorageAndAssistantExtraTest 与 LibraryPersistenceTest 的下载释放／恢复、收藏歌单保护与落盘回归列入本轮检查；两端选择器实现完成编译检查；新增 LibraryPersistenceTest.relinkingMissingAudioPreservesIdentityFavoriteAndPlaylistAfterRestart 验证旧文件丢失后重定位、重启、旧 ID／收藏／歌单／MIME 保留及原文件字节不变。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

真实 Android SAF 授权撤销、Windows 文件移动后重定位、恢复失败后换来源的点击链路尚未实机操作。显示“来源不可用”来自本次恢复失败，不代表永久失效。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

