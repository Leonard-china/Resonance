# 2026-11-06 — 批量重新定位音频时一次提交曲库索引

- 修改日期：2026-11-06（Asia/Shanghai）
- 类型：性能优化 / 并发边界修复
- 适用版本：0.6.8 / 42，不升版
- 影响平台：共享、Android、Windows、Linux
- 关联记录：[前置实现](2026-11-05-38-batch-relink-missing-audio.md)；用户批准实施本轮审查中的四项改进。

## 需求或问题

批量修复 M 首失效音频时，逐首重新加载整个 N 首曲库，并多次重写曲库和歌单索引，数据量随 M×N 增长。预览后的最新元数据和来源仍必须保留并复核。

## 原因与依据

`AudioRelinkSession.apply` 在循环内调用 `loadLibrary/saveReplacementAudio`；原单曲替换路径重新读取并写入整个库和歌单。实际上两端歌单持久快照均不保存 sourceUri，恢复时按歌曲 ID 读取库来源，来源单独变化无需重写歌单。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [AudioRelinkSession.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/AudioRelinkSession.kt)、[LibraryServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/LibraryServices.kt) | `apply / saveReplacementAudioBatch` | 会话只读一次库，逐项检查后调用一次批量提交；过期/不可读条目仍逐项报告。 |
| [AudioRelinkModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/AudioRelinkModels.kt) | `prepareAudioRelinkUpdates` | 平台持锁期间重新按 ID 合并最新元数据，复核旧来源仍失效、新来源可读；只修改 sourceUri。 |
| [DesktopLibraryStore.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopLibraryStore.kt)、[DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `replaceAudioBatch / saveReplacementAudioBatch` | Windows/Linux 共用持锁批量合并，原子写一次库，保留上一版备份，不写歌单。 |
| [AndroidMusicAccess.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidMusicAccess.kt)、[AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `replaceAudioBatch / saveReplacementAudioBatch` | 用 filesDir 锁合并托管索引、目录快照、removedIds 和最新收藏；一次原子写托管索引，保留 SAF content URI，不写歌单偏好。 |
| [AudioRelinkSessionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/AudioRelinkSessionTest.kt)、[AudioRelinkBatchTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/AudioRelinkBatchTest.kt)、[AudioRelinkDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AudioRelinkDeviceTest.kt) | 批量与持久化验证 | 1000 首仅一次库读取/一次提交；备份证明整批只产生一版、歌单文件字节不变；最新编辑/收藏、过期路径、消失文件、取消、重启关系及 Android 隔离测试。 |

沿用 `AtomicPropertiesFile` 写入和有效备份恢复。单曲重新定位保留原入口；没有批量写标签、转换或改 ID。提交前可取消；原子写开始后以完整索引为单位完成，不承诺进程断电时跨任意外部文件系统事务。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 修复多首 | 每首读写整库和歌单 | 读取/提交次数与条目数无关，一次原子库写 |
| 扫描后元数据或来源改变 | 会话读取与写入有时间窗口 | 提交锁内取最新元数据，过期来源拒绝 |
| 重启读取歌单 | 从库恢复新来源 | 保持同一 ID 和关系，无须重写歌单快照 |

本次验证操作次数与持久化结果，未以小样本测试宣称真实大型曲库耗时或固定倍数提升。

## 平台与数据影响

Android、Windows、Linux 共用相应共享逻辑，平台存储适配分别核对。版本保持 `version.properties` 的 0.6.8 / 42。保留原音频、收藏与歌曲 ID，不删除或改写原音频/标签；不改变 `.resonance` 包格式，不新增权限、外部服务或付费请求，不读取真实 Key 或签名文件。

## 验证

本次实际编译、定点回归、资源限制及未执行项见[本轮验证](../audit/2026-11-06/review-fixes/VALIDATION.md)。日志和本机脚本保留在忽略目录 `build/review-fixes-2026-11-06/`，不引用上轮审查或旧包结果作为本次通过依据。

## 限制与后续

仅交付源码、回归及文档；不打包、安装、升版或发布。Android 设备、Windows 原生操作、物理外置盘及长时间播放不由 JVM 替身测试代替；实际执行范围以上述验证记录为准。

## 同步更新的文档

`docs/README.md` 变更索引、`docs/DEVELOPMENT.md` 对应当前行为与回归入口，核对日期 2026-11-06。
