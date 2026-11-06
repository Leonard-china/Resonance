# 2026-11-06 — 保留截图导入失败及中断后的剩余草稿

- 修改日期：2026-11-06（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.8 / 42，不升版
- 影响平台：共享、Android、Windows、Linux
- 关联记录：[前置实现](2026-11-05-37-persist-screenshot-import-drafts.md)；用户批准实施本轮审查中的四项改进。

## 需求或问题

确认截图歌单后，创建歌单失败或处理中断会丢失尚未开始的歌曲，已编辑的歌名和选择无法继续使用。用户批准修复前轮审查确认的问题。

## 原因与依据

`confirmScreenshotImport` 在任何导入前删除 `screenshotDraft`，而任务日志只在即将处理单首歌曲时才写入 payload。根任务没有完整导入计划，因而首次歌单写入失败后既无草稿，也无可重试歌曲。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `confirmScreenshotImport / checkpoint / importScreenshots` | 导入前原子保存已确认文字和选择，轮换草稿 ID；每首成功后移除已完成项，剩余失败/未开始项继续落盘；已有草稿先处理或丢弃，禁止新识别覆盖。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt)、[ScreenshotImportDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ScreenshotImportDialog.kt) | `MusicFeatureDialogs / ScreenshotImportDialog` | 任务运行期间隐藏编辑器，停止后显示剩余草稿；旧编辑器迟到保存不能覆盖新 ID，任务异常退出也重载已入库结果。 |
| [ScreenshotImportRecoveryTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/ScreenshotImportRecoveryTest.kt) | 四项恢复回归 | 歌单创建失败保留确认编辑、逐曲失败仅保留失败项、取消后恢复当前/未开始曲目；旧 ID 拒绝、新 runtime 恢复、本地去重及多歌单完成项剔除。 |

保持既有 `screenshotDraft` JSON 编码、锁、原子替换和备份，不迁移旧草稿。检查点写入失败立即停止剩余处理。进程在入库后、检查点前退出时，该首仍可能再次进入导入流程，但 `obtain` 复用可读本地曲目、`addToPlaylist` 按 ID 去重；不宣称跨文件事务的绝对恰好一次。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 首次创建歌单失败 | 草稿消失，无歌曲步骤可重试 | 完整已确认草稿保留，修复原因后可继续 |
| 部分成功后取消 | 未开始曲目无记录 | 成功项移出草稿，当前及未开始项保留 |
| 旧编辑器迟到保存 | 可能重写已消费计划 | 草稿 ID 已轮换，旧保存被拒绝 |

## 平台与数据影响

Android、Windows、Linux 共用相应共享逻辑，平台存储适配分别核对。版本保持 `version.properties` 的 0.6.8 / 42。保留原音频、收藏与歌曲 ID，不删除或改写原音频/标签；不改变 `.resonance` 包格式，不新增权限、外部服务或付费请求，不读取真实 Key 或签名文件。

## 验证

本次实际编译、定点回归、资源限制及未执行项见[本轮验证](../audit/2026-11-06/review-fixes/VALIDATION.md)。日志和本机脚本保留在忽略目录 `build/review-fixes-2026-11-06/`，不引用上轮审查或旧包结果作为本次通过依据。

## 限制与后续

仅交付源码、回归及文档；不打包、安装、升版或发布。Android 设备、Windows 原生操作、物理外置盘及长时间播放不由 JVM 替身测试代替；实际执行范围以上述验证记录为准。

## 同步更新的文档

`docs/README.md` 变更索引、`docs/DEVELOPMENT.md` 对应当前行为与回归入口，核对日期 2026-11-06。
