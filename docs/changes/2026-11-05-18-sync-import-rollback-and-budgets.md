# 2026-11-05 — 同步包完整校验、失败回退与资源预算

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q01、Q08。

## 需求或问题

损坏歌单在音频被覆盖之后才报错；导入回调失败留下新文件；极大引用计数没有预算。

## 原因与依据

原导入先commitTracks再readPlaylists，平台另行保存元数据。现先验证完整清单、SHA-256、ID、目标、引用与路径，再把落盘和平台元数据提交放在可回退的调用链。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [io/FileRollback.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/io/FileRollback.kt) | FileRollback.run | 在同一文件系统保存原文件；异常或取消时恢复旧文件、删除本次新文件。恢复失败保留副本与路径清单，并附带错误。 |
| [sync/PlainSyncPackage.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt) | import / readPlaylists / extractWhitelisted | onCommit提交回调、分块取消、引用与字节/空间预算，先验证后移动。 |
| [platform/DesktopLibraryStore.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopLibraryStore.kt) | mergeImported | 库/歌单主文件及.bak一起纳入元数据回退。 |
| [platform/DesktopMusicScanner.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopMusicScanner.kt) | scan | 普通选择/目录/拖放的.resonance同样走提交回调与ImportControl。 |
| [platform/AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | importSyncPackage | 暂存复制有空间/取消检查，管理索引与两个歌单偏好恢复，失败还原cachedTracks。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q01、Q08所述输入/操作 | 清单损坏可能先覆盖文件，平台保存失败没有统一撤回 | 验证失败不提交；运行期间提交失败撤回本次文件和受保护元数据；正常导入仍按原ID合并 |

## 平台与数据影响

共享JVM逻辑覆盖Android/Windows/Linux；包格式仍为resonance-sync-1。新增16MiB清单、每歌单10万/总100万引用、解压/备份至少保留16MiB空间；原单文件4GiB、总512GiB、200001条目限制保留。超限旧包需分批重新导出。

## 验证

本次相关用例：ArchiveRollbackTest、PlainSyncPackageTest、DesktopSyncPackageImportTest、DesktopMusicImportTest。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：ArchiveRollbackTest、PlainSyncPackageTest、DesktopSyncPackageImportTest、DesktopMusicImportTest | 23通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

这是运行期失败回退，不是多文件断电事务或崩溃恢复日志。实际磁盘损坏/断电、Windows真实文件锁、极低空间真机故障未制造；恢复失败会保留副本。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
