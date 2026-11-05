# 2026-11-05 — 重复项保留录音版本并按完整批次确认

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q05、Q06。

## 需求或问题

去括号后把Live/Remix与原版归一组并默认选中清理；多首清理循环覆盖单曲待确认状态，仅留下最后一首。

## 原因与依据

元信息相同不能证明录音相同，容器格式不能证明音质。保留版本与时长，未知歌手不分组，全部取消默认删除选择；整批通过onBatchDelete。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [model/SmartLibraryAuditEngine.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/SmartLibraryAuditEngine.kt) | audit / normalizeTrackKey | 只剥离已知音质前缀，保留[Live]/【伴奏】等版本；歌手+完整歌名+时长为疑似组，不自动选择删除；保留建议仅作提示。 |
| [ui/SmartManagerScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SmartManagerScreen.kt) | DuplicateCleaningView | 疑似重复说明、手动选择和换行提示；删除动作不再称安全释放。 |
| [ui/LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | duplicateDeletion / onCleanDuplicates | 一份去重后的候选快照；确认层在页面之后绘制，避免页面拦截触摸；取消清空，确认调用一次批量入口。 |
| [ui/PlaylistDialogs.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistDialogs.kt) | BatchDeleteTracksDialog | 可传入整批歌曲，滚动显示具体明细。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q05、Q06所述输入/操作 | Live被预选；选择多首最终只确认一首 | 录音版本/时长差异不并组；默认零首；取消零次删除，确认完整批次一次 |

## 平台与数据影响

共享Android/Windows/Linux原生UI；不改音频、不写回标签，不引入基于格式的音质承诺。

## 验证

首次实机新增用例发现确认层绘制在主页面之前，文本虽存在但触摸被后绘制页面拦截，回调次数仍为0。现移动到同一Box内最后绘制，保持完整批次/取消/真实点击断言，不改成直接调用回调绕过遮挡。最终结果见下方本轮验收。

本次相关用例：SmartLibraryAuditEngineTest；SmartCleanupConfirmationDeviceTest两组选择/取消/一次完整批次。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：SmartLibraryAuditEngineTest、SmartCleanupConfirmationDeviceTest | 4通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

尚未接入音频指纹/逐字节重复认证；疑似重复仍需用户核对，不自动清理。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
