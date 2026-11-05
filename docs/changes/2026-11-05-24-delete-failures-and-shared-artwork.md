# 2026-11-05 — 删除失败保留记录、批量报告实际结果并保留共享封面

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q09、Q11。

## 需求或问题

文件权限拒绝被吞掉，库记录却被移除并报成功；删除一首会删掉另一首共用的封面；部分批次UI整批移除。

## 原因与依据

之前将异常当作deleteIfExists=false，随后无条件改索引；封面无归属/引用判断；批量结果没有实际移除ID。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [model/LibraryModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | DeleteTrackReport.removedTrackIds | 在返回值附加本次实际移除ID，不改持久格式。 |
| [platform/DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | deleteLocalTrack / batchDeleteTracks | 物理删除异常即返回失败，元数据保留；逐项汇总实际成功与失败；不删封面。 |
| [platform/AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | deleteLocalTrack / batchDeleteTracks | 提供者拒绝/权限异常保留记录，逐项反馈；不删封面。 |
| [features/AndroidMusicAccess.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidMusicAccess.kt) | delete | 后台入口提供者拒绝时不删关系，成功后核对收藏保存。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 批量/单曲删除回调 | 队列只移除实际成功条目，部分成功仍刷新，失败保留可重试。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q09、Q11所述输入/操作 | 文件仍在却丢记录、报成功；B封面被A删除牵连 | 文件IO失败保留记录；成功项才移出队列/歌单；保留封面文件 |

## 平台与数据影响

三端文件删除语义变化；真实删除仍不可撤销。保留全部封面会留下暂未回收文件，避免删用户/共享图片；未增加后台GC。

## 验证

本次相关用例：DeletionSafetyTest真实平台入口、共享图片和Linux只读目录部分批次；全套存储/播放回归。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：DeletionSafetyTest | 2通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

Windows文件占用与Android用户提供者拒绝未制造真实故障；磁盘/偏好在物理删除后的写失败无法撤销已删除音频，返回失败而非保证事务删除。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
