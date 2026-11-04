# 2026-11-02 — 补齐双端原生播放验证与本地候选构建

- 日期：2026-11-02（Asia/Shanghai）；类型：测试/文档；Android/Windows；版本保持 0.6.5（39）。
- 关联：前一日变更 17–19；本次未改变产品业务逻辑、数据格式或生产配置。

## 需求与实际修改

补齐 UI 之外的真实播放验证。新增 desktopTest 的 `playback/DesktopAudioOutputIntegrationTest.kt`（2 项），使用生成音频、静音的真实 FFmpeg/Java Sound、临时数据，覆盖暂停、seek、变速、快速替换及无 Compose 的计时。需 `RESONANCE_DESKTOP_AUDIO_TEST=1`。统计检查先等待原生就绪，失败保留位置及保存警告。

新增 androidInstrumentedTest 的 `ListeningSessionDeviceTest.kt`：独立 validation applicationId、私有生成音频，覆盖 Activity 重建、后台计时、暂停、seek、95% 去重与原生循环。需 runner 参数 `isolatedNative=1`；拒绝普通用户包。

修正 `LibraryDetailTransitionTest.kt` 的采样坐标：原先在 80ms 固定取中点，实际新面板左缘为 243px、中点为 210px，采到了尚未覆盖的旧红色列表。真实截图证实右侧面板正确遮挡。现在按可见面板边界采样，保留遮挡、裁剪、恢复和减少动态断言；增加失败截图。没有修改产品动画或放宽颜色阈值。

## 验证结果及失败记录

完整命令、日志和临时构建脚本保存在 `build/final-20261102/`；机器台账见 [verification.json](../audit/2026-11-02/verification.json)。

- `desktopTest testReleaseUnitTest lintRelease assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2`：桌面 290 项，286 通过/4 条件跳过；Android 单元 55 项通过；Lint 0 错误/107 警告。
- 公共中继 Python unittest：6 项通过。
- 新 Windows 原生测试：首轮统计等待超时；追加诊断后两次通过，首轮原因未确定，不能宣称已修复某个产品故障。最终与 Tracker/Repository 合计 8 项通过。
- MuMu Android 12 初轮 10 项中 2 项失败：上述坐标假设及一次未发现 Compose 层级；独立复查只复现坐标问题。最终 10 项中 9 通过、1 失败：修正采样后面板遮挡和裁剪已通过，但第 79 行在推进 400ms 后仍读到 Canvas 而非透明后的黑色；动画未完全结束还是背景残留尚未定位，不能把视觉验收记为通过。日志 `android-final-device.log`，未覆盖历史失败记录。
- 重新生成原生页面截图，实际查看本目录下浅色播放器、深色一起听的大字体小窗口截图和 Android 转场失败截图。
- Android Release APK 构建及 apksigner 校验通过。Windows 首次输出目录被既有进程占用；改用 `composeApp/build/quality-candidates-20261102` 后运行目录与 EXE 构建成功，未关闭既有应用。Windows EXE 未签名；隔离启动只确认进程存活且 Responding，未确认应用窗口或交互，不算完整启动验收。

## 产物、数据与回滚

本地候选位于 `build/final-20261102/candidates/`，校验值来自本次文件，未发布或上传。Android 仪器包使用临时 finalizeDsl 将 debug applicationIdSuffix 改为 `.validation20261102`，未覆盖原应用。构建脚本在同目录，生产配置未变。

修改前文件检查点：`.git/audit-checkpoints/validation-20261102-033014/files`，对应 codex/validation 分支；分支不包含未提交快照。未提交、重置或清理原有改动。

## 限制与接续

完整加载/缓冲状态仍未实现；生产中继未部署、跨网/多人/长时及商业容量未验收。Windows 首次统计超时和一次 Android 层级缺失保留为间歇性验收风险。账户窗口最后查询已用 97%，不是美元费用；停止扩大改动，后续先读本记录和台账。同步更新 README 索引、DEVELOPMENT 与 OPTIMIZATION_REPORT。

## 最终接续定位

额度窗口末次查询 97%。优先定位 LibraryDetailTransition 的 moving/transition.isRunning 与 Android 测试时钟；保留遮挡、裁剪和最终透明断言，不用直接删断言通过。只定点复测该项，修复产品代码后再补必要编译/回归并重新生成对应候选。当前 APK/EXE 对应未修改的产品源码，尚未正式验收。一次性恢复自动化已删除，不再自行周期执行。
