# Resonance UI 重构基线（2026）

## 参考素材

- [Material 3 Expressive](https://m3.material.io/)：参考其强调层级、形变反馈与 motion physics 的方向，不照搬组件外观。
- [Compose shared element transitions](https://developer.android.com/develop/ui/compose/animation/shared-elements)：参考空间连续性原则，让导航和内容切换有明确方向。
- [Compose animation modifiers](https://developer.android.com/develop/ui/compose/animation/composables-modifiers)：使用 Compose 原生状态动画，避免手工逐帧控制造成卡顿。
- [VibeMusic UI case study](https://dribbble.com/shots/26828908-Case-Study-VibeMusic-App-Design)：参考“封面优先、控制克制、深色沉浸”的信息层级。
- [Fankee music experience case study](https://www.uxstudioteam.com/fankee-case-study)：参考专辑封面聚焦与简化播放控制的体验取舍。

## 设计方向

- 氛围：深色本地音乐空间；用珊瑚色作为主要动作色，紫色与蓝色仅作为环境光。
- 层级：背景光晕 → 玻璃质感面板 → 悬浮卡片 → 当前状态高光，避免所有组件使用相同阴影。
- 动效：快速反馈约 160ms，页面与歌单切换约 280ms；进入使用 ease-out 观感，按压使用轻微弹簧回弹。
- 动态：导航保持稳定，只移动内容；歌单切换时标题和曲目内容保持空间连续性；播放中只让均衡器持续运动。
- 可用性：点击区域不低于 44dp；图标保留 `contentDescription`；文字与背景保持清晰对比；动画只承载状态变化。

## 产品约束

- 首页“你的歌单”只展示用户创建或导入的歌单，不再合成“本地音乐”歌单。
- 本机曲目仍参与实际播放队列与歌单匹配，但不作为首页歌单卡片出现。
- 最后选择的真实歌单单独持久化；启动时仅在该歌单仍存在时恢复，否则回退到第一个歌单。
