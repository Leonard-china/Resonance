# 2026-11-01 — 按操作顺序保存完整随机与循环偏好

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：Bug 修复；共享 / Android / Windows；版本保持 0.6.5（39）
- 关联：[审计第 6 项](../PRODUCT_AUDIT.md)

## 需求、原因与依据

快速连续切换随机/循环时，App 原来为每次保存分别启动协程；平台 `setPlaybackMode` 又隐式保存一次。桌面同步保存阻塞调用者，Android 隐式保存吞异常；独立协程完成顺序也无法保证最后一次操作最终落盘。

## 具体修改

| 文件 | 关键符号与变化 |
| --- | --- |
| [PlaybackModeWriter](../../composeApp/src/commonMain/kotlin/com/resonance/player/data/PlaybackModeWriter.kt) | 在点击时捕获完整 shuffle/repeat 对，交给已有 PersistenceQueue 的同键合并通道 |
| [App](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 三个模式保存入口统一；DeepSeek 配置也进入已有顺序队列；保存失败可见且不暂停播放 |
| [PlatformServices](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | 明确 setPlaybackMode 仅影响原生播放，savePlaybackMode 专管持久化 |
| [AndroidPlatformServices](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | 去掉实时 setter 的隐式 IO；显式 save 在 IO 上 commit 并检查失败 |
| [DesktopPlatformServices](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | 实时 setter 不再同步写磁盘；显式 save 沿用 Store 原子写 |

修改前可能出现旧操作晚完成覆盖新值；修改后实时控制仍立即响应，已开始的保存顺序完成，尚未执行的同键操作合并为最后一对。退出排空沿用原 PersistenceQueue，不新增写入机制。配置键与格式不变，用户仍可保存并在重启后恢复；未读取或输出真实凭据。

## 验证

- 新增 [PlaybackModeWriterTest](../../composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackModeWriterTest.kt)：阻塞第一笔保存，连续改变两项偏好，关闭队列后放行，验证无并发、完整快照、最后操作优先和排空。
- 修改 [PlaybackModePersistenceTest](../../composeApp/src/desktopTest/kotlin/com/resonance/player/PlaybackModePersistenceTest.kt)：保留显式保存/恢复断言，增加实时 setter 不绕过队列的断言，并正确关闭临时平台服务。
- 首轮 12 项中 1 项失败：旧测试断言 setter 必须隐式持久化，与这次有意明确的接口分工冲突。未删除保存验证，而是分别验证实时操作与显式保存；最终 36 项定向回归全部通过，包含原 PersistenceQueue 4 项、偏好存储 3 项和新增写入 1 项。两端编译通过，证据见 [稳定性台账](../audit/2026-11-01/stability-verification.json)。

## 平台影响、限制和文档

Android 和 Windows 接口一起调整；原子文件、备份、偏好键、配置加密均未更改。真实设备快速点击后重启验收留给收尾；不能由受控调度测试推断所有平台 IO 故障已覆盖。更新 README、DEVELOPMENT、优化报告和交接提示词。
