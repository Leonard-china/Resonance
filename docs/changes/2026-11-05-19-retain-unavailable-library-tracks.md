# 2026-11-05 — 保留暂时不可用的曲目、收藏与歌单引用

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q02。

## 需求或问题

外置盘临时离线后编辑另一首歌曲，重写索引会永久丢掉离线曲目及关系。

## 原因与依据

桌面load按Files.exists过滤曲目，Android受管目录也过滤缺失file URI。读取现在保留索引，播放可用性由原有可用性检查决定。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [platform/DesktopLibraryStore.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopLibraryStore.kt) | load / replaceAudio | 当前格式沿用持久ID；旧记录无法读内容哈希时保留旧ID，必要时用路径摘要兜底。 |
| [features/AndroidMusicAccess.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidMusicAccess.kt) | readManaged | 暂不可用的受管文件不再被读取过滤。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q02所述输入/操作 | 离线记录读入就消失，后续保存损失关系 | 离线A仍在索引；编辑B后A收藏/关系不丢；原文件回来可继续读取 |

## 平台与数据影响

不迁移持久格式和版本；影响两端共享使用的受管索引。不会凭空恢复已经被旧版本保存删除的记录。

## 验证

本次相关用例：LibraryPersistenceTest离线/重连回归；OfflineCatalogDeviceTest手机受管文件离线编辑与重连；全套存储回归。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：LibraryPersistenceTest、OfflineCatalogDeviceTest | 7通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

未物理拔插Windows外置盘；用独立临时目录移动/恢复生成文件覆盖同一不可用状态。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
