# 2026-10-17 — 管理应用下载空间并保留歌单关系释放本地音频

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.6 / 29 工作区，未升版、未发布
- 影响平台：共享 / Android / Windows
- 关联需求：避免无用 MP3 占满手机，区分临时与已保存下载。

## 需求或问题

此前主要依赖换批删除临时推荐，缺少下载占用、重复检查和保留元数据释放音频入口。不能直接把扫描到的外部 MP3 当作缓存清理。

## 原因与依据

曲库没有独立的下载所有权台账；Track 和歌单都保存歌曲快照，释放音频必须同步 sourceUri 并支持旧 ID 恢复。文件删除不可用单纯移除曲库条目替代。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [ManagedMusicStorage.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/ManagedMusicStorage.kt) | register / report / cleanup / checkBudget / path | 正向登记应用下载、旧来源迁移；已保存/临时/占用/空闲、SHA精确重复检查、预览清理、路径边界与播放保护、下载预算 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | storageReport / cleanupDownloads / restoreDownload | 串行服务入口、从保存来源恢复；推荐旧批清理增加托管路径与共享引用检查 |
| [MusicSourcesScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicSourcesScreen.kt) | MusicStorageScreen | 查看占用、设置临时上限、清理预览、显式释放已保存音频、重新下载 |
| [MusicLibraryAccess.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/MusicLibraryAccess.kt)、两端 MusicAccess/曲库存储 | adoptRestoredAudio / update | 离线曲目保留信息，恢复沿用旧ID/收藏；同步歌单 sourceUri |
| [MusicAssistantTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt) | cleanup / restore 回归 | 仅托管文件、外部文件拒绝、已保存/播放保护、释放再恢复 |

## 修改前后行为

| 场景 | 修改前 | 修改后（待运行验证） |
| --- | --- | --- |
| 空间管理 | 无统一占用/临时上限页面 | 工具→下载与空间，可查看占用/上限/重复项并预览释放 |
| 已保存歌曲仅想省空间 | 物理删除会丢曲目关系 | 显式释放本地文件，sourceUri置空，保留曲目/收藏/歌单，可按来源恢复 |
| 外部导入文件 | 没有清理所有权模型 | 本入口仅操作已登记且位于应用音乐目录的真实文件，不跟随逃逸符号链接 |

## 平台与数据影响

ownedAudio 保存路径、快照、永久保存意图、登记时间及 evicted；手动下载默认永久保存，推荐下载默认临时。旧 videoOrigins 仅在安全目录中迁入。temporaryStorageMb 默认512，范围64–8192；下载阶段保守估算原/输出空间，超限停止，不自动删除已保存歌曲。释放前更新曲库和歌单，删除失败尝试回滚；共享文件有其他曲目引用或正在播放时保留。释放实际删除音频，不改标签；应用外手动删除原有行为不变。桌面空 source 需已有有效ID才加载。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| git -c core.safecrlf=false diff --check | Windows | 通过 |
| 新增释放/恢复/播放保护/外部路径回归 | desktopTest 源码，临时夹具目录 | 已编写，未运行 |
| 删除失败、崩溃点、双端重启/恢复、容量实测 | Android / Windows | 未执行，按用户要求交给 Antigravity；严禁用真实音乐做破坏测试 |

## 限制与后续

不是全盘 MP3 扫描器，也不自动接管未知历史孤立文件。SHA只检测文件完全相同，不判断不同编码的同一录音。下载预算目前在源文件落盘后检查，512MiB网络上限仍可能先消耗临时空间。曲库、歌单和物理删除不构成跨文件事务，需重点验证删除前后崩溃/写失败恢复；该验证完成前不能声称存储安全已通过。重新下载依赖原源可用性和内容稳定性。详见 [交接](../HANDOFF-2026-10-17.md)。

## 同步更新的文档

docs/README.md、docs/DEVELOPMENT.md、docs/HANDOFF-2026-10-17.md。
