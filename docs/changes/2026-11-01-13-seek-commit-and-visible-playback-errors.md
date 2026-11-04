# 2026-11-01 — 拖动结束再跳转并在播放器上显示失败

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.6.5（39），未升版
- 影响平台：共享 / Android / Windows；中继另为独立 Python 服务
- 关联：[产品审计](../PRODUCT_AUDIT.md)、[本轮结果](../OPTIMIZATION_REPORT.md)
- 路径缩写：C = composeApp/src/commonMain/kotlin/com/resonance/player；J = 对应 jvmMain。

## 需求或问题

拖动进度每次变化都会调用 seek；Windows PCM 路径会重新启动解码任务。播放异常仅写入底层 importMessage，全屏播放器可能遮挡错误。

## 原因与具体修改

- `C/ui/common/SoftNeumorphicButtons.kt::WavySlider`：移动仅更新本地预览，释放时提交一次；音量保持实时调整。
- `C/ui/NowPlayingOverlay.kt::PlaybackProgress`：以 track.id 为 key，切歌丢弃旧拖动状态。
- `C/App.kt` 与新增 `C/ui/PlaybackErrorDialog.kt`：全局播放错误弹窗；本机播放提供重试，一起听不绕过房间控制发起重试。
- `UiVisualContractTest`：真实原生指针 Press/Move/Release 检查释放前 0 次、释放后 1 次 seek；双主题错误弹窗按钮语义与截图。

## 修改前后与平台、数据影响

共享 Android/Windows UI；不改解码、队列、持久化格式或自动播放策略。原生截图与语义不能替代扬声器实听、Android 触摸/音频焦点测试。保留原有防竞争播放任务机制。

## 验证

完整 `:composeApp:desktopTest` 中 `UiVisualContractTest` 16/16 通过，含真实指针拖动仅释放一次 seek、双主题错误弹窗和重试语义；本机扬声器/Android 实机播放未执行。

最终共同命令：`./gradlew.bat :composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain`，成功；桌面 276 通过 / 4 条件跳过 / 0 失败，Android Lint 0 errors / 110 warnings。

本次详细命令、数量、截图与限制记录在 [优化报告验证台账](../OPTIMIZATION_REPORT.md#验证台账)，不使用旧产物充当本轮通过。中间编译失败与修正也在台账保留。

## 限制与后续

上述未验收项保持待办；没有修改生产数据、签名配置、版本号或发布产物。检查点为 `.git/audit-checkpoints/20261101-203513`，只按本轮变更文件回退，不能丢弃此前脏工作区。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 当前状态、`docs/OPTIMIZATION_REPORT.md`；服务变更另更新 relay README 与部署说明。
