# 2026-11-06 — 审查四项修复验证

本轮完成截图导入恢复、暂停准备后恢复选中曲目、明确文字与 BPM 联合筛选、批量音频路径一次提交。版本保持 0.6.8 / 42。保留工作区既有未提交改动，没有打包、安装、提交、推送或发布。

## 本次执行

| 实际命令/检查 | 结果 |
| --- | --- |
| `python3 build/review-fixes-2026-11-06/run_desktop.py` | 通过：Kotlin/Compose 2.3.20 CLI 编译当前 188 份共享/JVM/桌面生产源码、11 份测试源码，JUnit 执行 110 项全部通过。当前编译类置于运行类路径首位，不使用旧应用 JAR 代替当前生产类。 |
| `python3 build/review-fixes-2026-11-06/run_android.py`（调用 `compile_android.py`） | 通过：当前 180 份共享/JVM/Android 生产源码和 30 份设备测试源码编译成功，含新 `AudioRelinkDeviceTest`。这是源码编译，不是 APK/设备测试。 |
| 源码 SHA-256 前后守卫及最终复核 | 通过；两次最终编译期间及文档收尾后源码未变。便携摘要见 [source-sha256.json](source-sha256.json)。 |
| `git -c core.whitespace=cr-at-eol diff --check` | 通过。 |
| 本轮新增/修改 Markdown 链接目标存在性核对 | 通过；只核对本轮新增链接，不把历史链接状态冒充本次全仓文档审计。 |
| `adb devices -l` | 无设备连接，设备测试未执行。 |

首轮 108 项通过后补充多歌单恢复与实际原子写失败边界，再完整编译并运行最终 110 项；首轮日志另留 `first-validation/`，最终结果不是重复引用首轮或前次审查的 35 项结果。

## 测试覆盖

| 测试类 | 本次项数 | 关键范围 |
| --- | ---: | --- |
| PlaybackStateStoreTest | 25 | 快速切歌、音量修订、原生附着、缓冲及响度失败 |
| PausedPreparationTest | 5 | 准备前/后恢复、只启动一次、换曲/刷新、清空/退出、准备失败 |
| PlaybackSessionTest | 4 | 共享播放队列与命令回归 |
| OfflineRecoveryTest | 4 | 既有草稿/空选择、BPM缓存与备份 |
| ScreenshotImportRecoveryTest | 4 | 创建歌单失败保留编辑、部分失败、取消与重启、多歌单剩余项 |
| OfflineLibraryImprovementsTest | 3 | 文件名匹配、真实BPM范围、歌词索引 |
| SemanticSearchConstraintsTest | 4 | 明确歌手/歌名/曲风与BPM交集；纯节拍/场景/未知BPM |
| AudioRelinkSessionTest | 2 | 最新数据与来源保护、1000首一次读库/一次提交 |
| AudioRelinkBatchTest | 3 | 单版备份、歌单字节不变、失效/过期候选、取消及实际原子写失败 |
| LibraryPersistenceTest | 6 | 旧库恢复、并发收藏/合并、路径恢复与歌单关系 |
| MusicAssistantTest | 50 | 截图/推荐/任务/付费接口替身等原有助手回归 |
| 合计 | 110 | 全部通过 |

## 资源与隔离

每项重量级验证串行持有 `build/resonance-heavy-validation.lock`，独立 systemd 用户服务限制整个进程树为 `MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0`，启动后通过 `systemctl --user show` 核对生效。启动前至少 3.5 GiB 可用，运行中不足 1536 MiB 则停止自身验证。直接 K2 编译无常驻 Gradle/Kotlin daemon；未修改 Gradle 默认值、swap 或关闭用户应用。

最终桌面服务日志峰值约 1.1 GiB，Android 约 1.2 GiB，均成功。日志位于忽略目录 `build/review-fixes-2026-11-06/` 的 `desktop-systemd.log`、`android-systemd.log`、编译和测试日志。测试使用临时库、模拟播放器、原创小文件和既有服务替身，不访问用户真实音频、签名私钥或真实 API Key。

## 未执行与解释边界

- 未运行全工程 Gradle 测试、Lint、ProGuard/Release 打包；本轮使用仓库现有已解析依赖和独立 Kotlin CLI 编译当前源码。
- Android 无设备连接，新增设备测试仅编译；未验证 SAF 选择器、系统通知/锁屏按钮或真实解码/暂停恢复。
- Windows 未做原生 GUI/物理外置盘验证；共享桌面存储逻辑在 Linux 隔离文件系统执行，不能等同于 Windows 设备验收。
- 批量性能验证的是操作次数和原子持久化结果，未给出大型真实曲库的耗时或固定提速倍数。
- 截图导入以逐曲检查点恢复；入库后、检查点前中断的最后一首允许再次核对本地库，通过复用和关系去重恢复，不宣称跨文件恰好一次。

机器摘要见 [verification.json](verification.json)。独立变更：[截图恢复](../../../changes/2026-11-06-03-screenshot-import-checkpoints.md)、[暂停恢复](../../../changes/2026-11-06-04-resume-selected-track-after-preparation.md)、[搜索条件](../../../changes/2026-11-06-05-semantic-search-text-and-bpm.md)、[批量路径](../../../changes/2026-11-06-06-batch-audio-relink-commit.md)。
