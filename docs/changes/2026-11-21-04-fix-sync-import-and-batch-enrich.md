# 2026-11-21 — 修复 Android 歌单导入失败与多选批量补全无生效，优化歌单传输交互

- 修改日期：2026-11-21（Asia/Shanghai）
- 类型：Bug 修复 / 功能体验优化
- 适用版本：`0.7.0 / 70`（`version.properties`，未改变版本号）
- 影响平台：共享 / JVM / Android / Windows / Linux
- 关联记录或需求：[同步包校验、回退与预算](2026-11-05-18-sync-import-rollback-and-budgets.md)、[升版 0.7.0 构建记录](2026-11-21-03-bump-to-0.7.0-and-build-release-apk.md)

## 需求或问题

用户在真机体验 0.7.0 后反馈两项真实故障及交互痛点：

1. **歌单导出后再导入失败，且传输方式偏繁琐**：在 Android 端导出 `.resonance` 歌单文件后立即导入，导入流程报错失败；同时缺少歌单详情页一键导出、导出后一键系统分享及本机已导出文件列表快速导入/分享入口。
2. **多选批量补全显示成功但实际未补全**：用户多选曲库歌曲执行「AI 智能补全」后，弹窗提示“补全完成”，但歌曲封面与标签实际上未发生变化；而在单曲播放页右上角菜单点击「匹配专辑封面」却能成功补全封面。

## 原因与依据

### 问题一：Android 歌单导出→导入失败

1. **核心根因 — Android SELinux/FUSE 下的 `Files.getFileStore(...).usableSpace` 异常**：[`PlainSyncPackage.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt)（原 87、268、294 行）、[`ManagedMusicStorage.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/ManagedMusicStorage.kt)（原 56 行）与 [`AndroidPlatformServices.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt)（原 1100 行）在空间预算检查时直接调用 `Files.getFileStore(path).usableSpace`。在 Android 11–16 FUSE / Scoped Storage 与 SELinux 环境下，Java NIO 的 `UnixFileStore.findMountEntry()` 需读取 `/proc/mounts` 匹配挂载点，容易抛出 `IOException: Mount point not found` 或 `AccessDeniedException`。导出阶段未调用 `getFileStore`，因此导出成功而导入必然失败。
2. **次要根因与兼容问题**：
   - 导入后音频路径与歌单成员 `sourceUri` 不一致（一方为原始文件路径、一方为 `file://` URI）。
   - Android SAF 选中文件的显示名称直接取 `uri.lastPathSegment`（常为 `msf:12345`），且暂存文件固定同名不利于并发隔离。
   - 导出阶段若单曲封面流损坏或不可读，未隔离异常可能中断整首歌的音频打包；此外未校验实际打包曲目数是否为 0。

### 问题二：多选批量补全“假成功”与未落盘

定位到四条叠加根因：

1. **Android MediaStore 曲目更新被静默丢弃（决定性根因）**：[`AndroidPlatformServices.updateTracks`](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) 原先只对已存在于 `synced-library.properties` 的记录做 `map` 替换，对通过系统 `MediaStore` 扫描进曲库的本机歌曲直接忽略不写盘。批处理结束后 `App.kt` 调用 `loadLibrary()` 重新扫描 `MediaStore`，刚补全的封面和标签立即被原 MediaStore 记录冲掉。而单曲右上角「匹配专辑封面」走的是 `AndroidMusicAccess.update`（会 upsert 到 `synced-library.properties`），因此单曲生效而多选无效。
2. **Android MediaStore 假封面占位 URI 导致封面补全被跳过**：`AndroidPlatformServices.scanMediaStore()` 为每首 MediaStore 歌曲默认写入 `artworkPath = "content://media/external/audio/albumart/<albumId>"`，即便专辑实际无封面该字符串也非空，使 `Track.isMissingCover` 返回 `false`，[`MetadataEnricher`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/MetadataEnricher.kt) 直接跳过封面搜索。
3. **公共封面源缺口**：单曲「匹配专辑封面」（`AlbumCoverRepository`）优先检索 **QQ 音乐** 公共目录（800×800 高清封面，对中文及流行曲库命中率高），而 `MetadataEnricher` 原先仅查询 iTunes 与酷狗。
4. **统计与完成文案误报**：原 `BatchEnrichReport` 仅判断最终字段非空而非“本轮是否真实新增/变更”，并在 0 项实际更新时仍提示“补全完成”，且完成视图残留已废弃的跨窗 `.resonanceGlass`。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`composeApp/src/jvmMain/kotlin/com/resonance/player/io/StorageSpace.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/io/StorageSpace.kt) | `usableSpaceBytes(path: Path)` | 新增安全磁盘剩余空间查询：沿父目录定位最近已存在目录，优先调用直通 libc `statvfs` 的 `File.usableSpace`，仅在结果 `<= 0L` 时回退 `Files.getFileStore`，彻底绕开 Android `/proc/mounts` 权限限制。 |
| [`composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sync/PlainSyncPackage.kt) | `export`、`import`、`Exported`、`usableSpaceProvider` | 全部空间检查切换至可注入的 `usableSpaceProvider = ::usableSpaceBytes`；规范化 ZIP 条目路径（`\` → `/`、剥离前导 `./`）同时保持原有目录白名单与越界拒绝；导出前在 `runCatching` 内有界缓冲封面（≤ 16 MiB）避免坏封面中断音频写入；`export` 返回 `Exported(tracks, playlists, skippedTrackCount)`；扩展音频扩展名回退并去除 `#fragment`。 |
| [`composeApp/src/jvmMain/kotlin/com/resonance/player/features/ManagedMusicStorage.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/ManagedMusicStorage.kt) | `ensureAvailableSpace` | 替换 `Files.getFileStore` 为 `usableSpaceBytes(directory)`。 |
| [`composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/MetadataEnricher.kt`](../../composeApp/src/jvmMain/kotlin/com/resonance/player/enrich/MetadataEnricher.kt) | `MetadataEnricher`、`hasReadableCover`、`searchQq`、`parseQqSearch`、`enrichTrack`、`batchEnrich` | 新增 `hasReadableCover: (Track) -> Boolean` 注入探针以识别无效占位封面；将 QQ 音乐目录检索（`searchQq` / `parseQqSearch`）作为第一公共目录源接入，与单曲封面匹配对齐；改为按字段真实变化统计 `coversEnriched` / `metadataEnriched` / `lyricsEnriched`；0 项更新时返回诚实提示文案；净化封面落盘文件名。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/LibraryModels.kt) | `ExportedPackage`、`LocalSyncPackage` | `ExportedPackage` 增加可选内存字段 `shareUri: String? = null`；新增瞬态模型 `LocalSyncPackage` 描述本机可导入/分享的 `.resonance` 文件。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/platform/LibraryServices.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/LibraryServices.kt) | `shareExportedPackage`、`listLocalSyncPackages`、`importSyncPackageFrom` | 扩展默认兼容平台接口，支持导出文件一键分享、列出本机已导出歌单包与直连导入。 |
| [`composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt`](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `updateTracks`、`hasReadableCoverImage`、`exportSyncPackage`、`importSyncPackage`、`importSyncPackageUri`、`shareExportedPackage`、`listLocalSyncPackages`、`openTrackSource` | `updateTracks` 改为 upsert 写入 `synced-library.properties` 并同步 `cachedTracks`，使 MediaStore 曲目批补结果持久保留；为 `MetadataEnricher` 注入 `hasReadableCoverImage` 实流探针（探测 ≥ 12 字节）；导入改为 `usableSpaceBytes` + 唯一临时文件 + 真实 `DISPLAY_NAME` + 统一 `file://` URI；导出校验实际打包数量并记录 `shareUri`；实现 `Intent.ACTION_SEND` 系统分享与 `Download/Resonance/*.resonance` 扫描；修正 `openTrackSource` 对 `file://` 的显式处理。 |
| [`composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt`](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `exportSyncPackage`、`listLocalSyncPackages`、`importSyncPackageFrom`、`metadataEnricher` | 同步校验导出曲目数、`file://` URI 读取、封面读取异常隔离、`hasReadableCover` 文件有效性检测与本机 `.resonance` 列出/导入。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncPackageFeedback.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncPackageFeedback.kt) | `SyncPackageResultDialog` | 增加打包/合并曲目与歌单数量摘要条及「分享文件」按钮，完整保留既有测试语义标签。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/BatchEnrichDialog.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/BatchEnrichDialog.kt) | `EnrichCompletedView` | 移除残留 `.resonanceGlass` 改用静态 `.atelierCard()`；当封面/歌词/标签更新总数为 0 时明确展示「未匹配到新内容」而非误报“补全完成”。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt)、[`LibraryShell.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt)、[`App.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `SyncScreen`、`LibraryDetailView`、`LibraryShell`、`App` | 歌单详情顶栏新增一键「导出」按钮；「歌单导入与导出」页新增「一键快速导出」横向歌单胶囊与「本机已导出的歌单文件」列表（支持一键导入与一键分享）；批补完成后同步更新当前 `customQueue`。 |
| [`composeApp/src/desktopTest/kotlin/com/resonance/player/sync/PlainSyncPackageTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/sync/PlainSyncPackageTest.kt)、[`MetadataEnricherTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/enrich/MetadataEnricherTest.kt) | 新增回归用例 | 新增 `usableSpaceHelperAndNormalizedZipEntriesRoundTripCleanly` 与 `parsesQqCatalogAndTreatsUnreadableMediaStoreCoverAsMissing`。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Android 导入刚导出的 `.resonance` 歌单 | 在空间检查阶段因 `Files.getFileStore` 无法读取 `/proc/mounts` 抛出 `IOException` 导致导入失败；路径 URI 格式不统一 | 使用 `File.usableSpace`（libc `statvfs`）完成空间检查与完整性校验，导入后曲目与歌单 `file://` URI 一致，正常合并进本机曲库并可播放 |
| 歌单导出与分享流转 | 需进入工具页 → 弹窗 → 选择歌单 → 手动找文件分享 | 可在歌单详情顶栏或「歌单导入与导出」页一键快捷导出；导出完成弹窗支持一键「分享文件」；工具页直接展示本机 `.resonance` 文件列表支持一键导入/分享 |
| Android 多选歌曲执行「AI 智能补全」 | MediaStore 占位封面 URI 使封面补全被跳过；缺少 QQ 音乐目录；更新后的 MediaStore 曲目被 `updateTracks` 丢弃；0 变更仍报“补全完成” | 实流探测无效占位封面，优先查询 QQ 音乐 → iTunes → 酷狗目录，批补结果 upsert 持久化到 `synced-library.properties` 并刷新缓存；真实统计每类更新数，0 更新时诚实提示「未匹配到新内容」 |

## 平台与数据影响

- **同步包格式与兼容性**：`.resonance` 明文 ZIP 格式（`resonance-sync-1`）、SHA-256 校验、路径遍历防护、`FileRollback` 失败回退与体积/条目预算均保持完全兼容，新旧包可互导。`ExportedPackage.shareUri` 与 `LocalSyncPackage` 均为内存瞬态模型，不落盘、不修改持久化 Schema。
- **Android 持久化**：`updateTracks` 向既有 `filesDir/synced-library.properties` upsert 曲目元数据与封面路径，沿用现有原子属性文件与 `.bak` 恢复机制；不修改用户原始音频文件，也不涉及文件删除或签名密钥。
- **外部网络请求**：`MetadataEnricher` 新增对 QQ 音乐公开搜索接口（`c.y.qq.com`，与既有 `AlbumCoverRepository` 相同端点）的只读元数据/封面检索，不上传音频或用户凭据。
- **权限**：系统分享使用标准 `Intent.ACTION_SEND` + `FLAG_GRANT_READ_URI_PERMISSION`，不新增任何 Android 清单权限。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `compile-and-test.sh compile`（Desktop `common=120 + jvm=59 + desktop=28 + commonTest=23 + desktopTest=77`） | Linux + JDK 17，cgroup `MemoryMax=2G / MemoryHigh=1536M / MemorySwapMax=0` | **通过**：全部 307 个源文件零错误编译 |
| Android CLI 编译（`common=120 + jvm=59 + android=20` + Android SDK 36 + `R.jar`） | Linux + JDK 17，cgroup `MemoryMax=2G / MemoryHigh=1536M / MemorySwapMax=0` | **通过**：全部 199 个源文件零错误编译 |
| 10 个同步/回滚/补全/弹窗测试类（`SyncPackageNamingTest` 4、`PlainSyncPackageTest` 5、`ArchiveRollbackTest` 5、`CrossDeviceSyncFixtureTest` 1、`PlaylistArchiveFormatTest` 1、`DesktopSyncPackageImportTest` 8、`MetadataEnricherTest` 5、`AlbumCoverRepositoryTest` 1、`SyncPackageDialogTest` 3、`SyncPackageFeedbackTest` 3） | Linux + JDK 17，cgroup `MemoryMax=2G / MemoryHigh=1536M / MemorySwapMax=0` | **通过**：36 / 36 全部通过 |
| Android 真机实测导包→导入、系统分享及多选公网补全 | Android 实机 | **未执行**：当前主机未连接真机，待用户安装新 APK 后实机复核 |

## 限制与后续

1. 本轮在 Linux 宿主完成了完整 JVM 单元回归、桌面与 Android 全源码编译，但尚未在连接 USB 的 Android 真机上执行真实 MediaStore 曲目批补与 `Download/Resonance` 导入联调。
2. 若公网音乐目录（QQ 音乐 / iTunes / 酷狗）临时限流或曲目命名偏冷门，公共检索仍可能部分未命中，此时弹窗会诚实提示「未匹配到新内容」，用户可按需启用 AI 兜底或手动编辑/自选封面。

## 同步更新的文档

- [文档入口](../README.md)（变更索引）
- [详细开发说明](../DEVELOPMENT.md)（第 8 节元数据补全、第 9 节歌单文件导出/导入）
