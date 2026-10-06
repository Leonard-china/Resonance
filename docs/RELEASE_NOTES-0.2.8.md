# Resonance v0.2.8

## 重点修复与体验优化

- **歌曲详细播放页面丝滑滑动进出效果 (Silky Slide Transitions for Now Playing)**：
  - **移除阻塞动画的平台级 Dialog 弹窗**：原先「正在播放歌曲详情页」（NowPlayingOverlay）采用 Android/Desktop 平台原生 `Dialog` 组件包装，导致 Compose 动画系统无法介入，展开与关闭时均为 0ms 瞬间生硬闪现。本次重构将其彻底转为应用视觉树内的顶层图层（Overlay），接入完整的 Compose 动画流水线。
  - **纵向丝滑推入与滑出**：
    - **进入详情页**：从屏幕底部完全离屏位置丝滑向上滑入（`slideInVertically(DetailSlideSpec) { it }` + `fadeIn`），配合流体贝塞尔曲线（340ms），带来沉浸且极速响应的原生手感。
    - **退出详情页**：点击顶部「收起/关闭」箭头、系统返回键或手势返回时，详情页顺畅向下滑出屏幕底部（`slideOutVertically(DetailSlideSpec) { it }` + `fadeOut`），平滑露出主界面。
  - **手势下滑收起交互 (Swipe Down to Dismiss)**：
    - 顶部导航栏新增沉浸式胶囊拖拽条（Drag Handle Pill）。
    - 顶部栏与歌曲封面（ArtworkPane）均支持跟随手势快速下滑收起，自然触发丝滑退场动效。
  - **退出动画过渡状态保持**：即使在关闭或切歌边缘时刻，退出过程中严格保持最后活跃音轨状态与界面元素渲染，杜绝动画中途闪白或内容跳变。

## 构建产物与校验

| 文件 | 平台 | 大小 | SHA-256 |
| --- | --- | --- | --- |
| `Resonance-0.2.8-release.apk` | Android | 59.92 MiB | `8E7C9C1CA9CF1259F13348988441DCED9755C48F782559618C3F3F4F3F3E5B37` |
