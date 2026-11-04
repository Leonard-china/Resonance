# 2026-11-02 — 修正转场背景断言的动画结束同步

- 修改日期：2026-11-02（Asia/Shanghai）；类型：测试修复；Android 仪器测试。
- 版本：0.6.5（39），不升版；关联：[上一轮设备验证](2026-11-02-01-native-validation-and-local-candidates.md)。

## 现象、原因与依据

`LibraryDetailTransitionTest.openingCoversTheListAndClipsAtTheViewport` 在固定推进 400ms 后要求背景透明，实际仍为 Canvas。产品使用随位移变化的弹簧 `DetailSlideSpec`，不是固定 400ms 的 tween。此前同一测试的中点采样问题已经修正，本次仅定位最后的背景断言。

在保持原弹簧、背景逻辑及像素容差不变时，让 Compose 自动推进到空闲后，断言通过。测试日志记录普通动画结算推进 704–800ms 的虚拟时间，减少动态路径为 128ms；固定 400ms 并不能代表完成。实际结束截图显示黑色底色及蓝色内容，原浅色遮挡已移除，未确认产品存在永久背景残留。

## 修改

仅修改 [LibraryDetailTransitionTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryDetailTransitionTest.kt)：新增 `settleTransition()`，临时开启 `mainClock.autoAdvance`、调用 `waitForIdle()` 并恢复手动时钟；限制本次虚拟时间推进不得超过 2 秒。替换稳定状态的 400ms 假设，保留动画中途、层级、裁剪、快速返回、减少动态及最终透明断言。增加稳定帧截图，与中途帧分别存证。

没有更改生产动画、组件、主题或业务行为，没有删除断言、放宽颜色阈值或吞掉异常。实现依据为 [Compose 官方同步说明](https://developer.android.com/develop/ui/compose/testing/synchronization)：自动推进时 `waitForIdle` 会等待动画、布局与绘制空闲。

## 验证

- 构建：`gradlew.bat :composeApp:assembleDebugAndroidTest -I build/final-20261102/isolated-android.init.gradle --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain`，通过。
- MuMu Android 12，独立 `com.resonance.player.validation20261102` 包；`am instrument -w -r -e class com.resonance.player.LibraryDetailTransitionTest …/androidx.test.runner.AndroidJUnitRunner`：修改后 4 项全部通过；增加结束截图后再跑 4 项全部通过（3.059s）。
- 日志：`build/transition-relay-20261102/transition-final-build.log`、`transition-final-device.log`。时间输出只提取测试自己的 `Detail transition settled` 行，没有采集用户应用日志。
- 实际查看[结束帧](../audit/2026-11-02/detail-transition-settled.png)，[中途帧](../audit/2026-11-02/detail-transition-verified.png)也已保存。图中红/蓝/黑为专门检测遮挡和透明度的测试夹具，不是产品配色。
- 后续联合计时、控制器、音频与转场的 **10 项设备复查全部通过，0 失败（63.804s）**，结果见 [本次台账](../audit/2026-11-02/transition-relay-verification.json)；未执行的项目不复用旧结果。

## 数据影响、回滚与后续

只改测试，不访问用户音乐、配置或服务器。旧候选包的生产源码不受影响，不必因此重新发行。检查点路径见 `build/transition-relay-20261102/checkpoint.txt`，其中有本次修改前文件副本；保留原有未提交工作。

更新文档索引、开发说明和优化报告；上一轮失败证据保留，当前结果以本记录为准。完整加载/缓冲状态和公共一起听的部署验收仍是独立工作。
