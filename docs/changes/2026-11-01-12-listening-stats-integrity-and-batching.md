# 2026-11-01 — 保护听歌记录并把统计写盘移到 IO

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.6.5（39），未升版
- 影响平台：共享 / Android / Windows；中继另为独立 Python 服务
- 关联：[产品审计](../PRODUCT_AUDIT.md)、[本轮结果](../OPTIMIZATION_REPORT.md)
- 路径缩写：C = composeApp/src/commonMain/kotlin/com/resonance/player；J = 对应 jvmMain。

## 需求或问题

统计文件解析失败后原实现默认为空，后续写入覆盖损坏历史；写盘异常被吞掉，且每秒在 UI 调用链重写完整 JSON。审计用临时夹具复现，见 PRODUCT_AUDIT.md。

## 原因与具体修改

- `J/playback/ListeningStatsStore.kt`：校验旧 JSON 字段，保留有效 `.bak`；无可用备份时拒绝覆盖；恢复前另存损坏主文件；原子替换失败向上传播并保留待写增量。
- `C/platform/PlatformServices.kt`、Android/Windows PlatformServices：记录与 flush 改为 suspend，在 `Dispatchers.IO` 执行；生产使用 10 秒批量间隔。
- `C/App.kt`：暂停、切歌、退出组合时用 NonCancellable flush；失败写入界面消息状态（全屏下的非阻断反馈仍待完善），不把历史当零值。`NowPlayingOverlay`、`TasteRadarScreen`、`RecommendationScheduleStatus` 对不可用统计展示提示并保留推荐。
- `desktopTest/.../ListeningStatsStoreTest.kt`：损坏保护、备份恢复、损坏原文留存、批量刷盘及写失败回归。

## 修改前后与平台、数据影响

旧合法 JSON 格式不变；新增备份和损坏留存文件，不删除用户歌曲、播放列表或历史。异常进程终止最多损失约 10 秒尚未落盘的增量；恢复备份可回退一个刷盘周期，不声称零损失。现有后台统计生命周期和 near-end 完成计数问题未在本次重构。

性能夹具：10,000 条历史，之前同步每次更新中位 20 ms / P95 71 ms；本次缓冲更新中位 85 μs / P95 143 μs，一次显式安全刷盘 83 ms，持久化总数 10,031。后者是连续增量夹具、非实际播放秒间隔，且机器有并发构建；仅证明热路径减少序列化，不能宣称整机响应速度提高固定倍数。首次加载/统计读取仍可能有同步成本。

## 验证

完整 `:composeApp:desktopTest` 中 `ListeningStatsStoreTest` 5/5 通过，含损坏原文留存、备份恢复和写失败；两端 Kotlin 编译通过。性能夹具与真实限制见下述报告。

最终共同命令：`./gradlew.bat :composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain`，成功；桌面 276 通过 / 4 条件跳过 / 0 失败，Android Lint 0 errors / 110 warnings。

本次详细命令、数量、截图与限制记录在 [优化报告验证台账](../OPTIMIZATION_REPORT.md#验证台账)，不使用旧产物充当本轮通过。中间编译失败与修正也在台账保留。

## 限制与后续

上述未验收项保持待办；没有修改生产数据、签名配置、版本号或发布产物。检查点为 `.git/audit-checkpoints/20261101-203513`，只按本轮变更文件回退，不能丢弃此前脏工作区。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 当前状态、`docs/OPTIMIZATION_REPORT.md`；服务变更另更新 relay README 与部署说明。
