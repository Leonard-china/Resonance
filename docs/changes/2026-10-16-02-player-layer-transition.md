# 2026-10-16 — 合并完整播放器过渡并降低动态背景重组开销

- 修改日期：2026-10-16（Asia/Shanghai）
- 类型：功能 / Bug 修复
- 适用版本：0.4.0 / 23，本次按构建规则不自动升版
- 影响平台：共享、Android、Windows

## 需求、原因与依据

展开底部迷你播放器时存在卡顿和撕裂感。代码同时运行背景缩放/动态圆角和独立 AnimatedVisibility 滑入；环境光动画每帧在组合阶段读取角度，扩大重组范围。这些是确定的开销来源，不能仅凭源码断言所有设备掉帧的唯一原因。

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [ui/LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | LibraryShell / overlayMotion | 统一为一个可取消并从当前值继续的 Animatable；完整播放器固定尺寸，仅移动 graphicsLayer；挂载后让出两帧再进入 280 ms 过渡；背景与遮罩不逐帧重建布局。 |
| [ui/common/FluidAmbientCanvas.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/FluidAmbientCanvas.kt) | FluidAmbientCanvas | 保存动画 State，移到 Canvas 绘制阶段读取；播放器遮挡曲库时停用底层动态背景，播放器自身背景在过渡完成后再启用。 |
| [design/ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | ResonanceMotionTokens | 复用统一缓动定义；播放器 drawer 曲线 (0.32,0.72,0,1)，减少动态效果时 120 ms 渐显。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 展开/关闭 | 两套弹簧和整个背景变化 | 单一固定尺寸图层过渡，支持中途反向 |
| 环境光每帧更新 | 组合读取相位 | 绘制读取相位 |

## 平台与数据影响

共享 UI，两端生效；音频线程、队列与后台播放不因展开/关闭而重新启动。

## 验证

本次实际构建、测试、签名及产物结果统一见 [构建验证记录](2026-10-16-08-release-build-verification.md)。相关测试已随本次修改加入源码；不将旧版结果用作本次通过证据。

## 限制与后续

LibraryNavigationTest 增加反复展开/关闭回归；不将模拟器截图或测试通过表述为所有真机稳定 60/120 FPS。

## 同步更新的文档

已更新 [开发说明](../DEVELOPMENT.md)、[文档索引](../README.md)、[本次发布版构建说明](../RELEASE_NOTES-0.4.0-2026-10-16.md)。
