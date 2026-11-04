# 2026-11-04 — 补齐浅色原型的页面切换动画

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复 / 交互原型
- 适用版本：0.6.5 / 39，不升版
- 平台：桌面与手机浏览器原型；原生 Android / Windows / Linux 未改
- 关联：[原型方案](2026-11-04-02-light-ui-proposal-and-html-prototype.md)

## 需求与原因

用户反馈“切换页面时怎么没有动画了”。`app.js.render` 直接替换内容 HTML，只有播放器和歌词具备动画；缺少目的地与曲库分栏的转场执行逻辑，并非用户设置导致。

## 具体修改

| 文件 | 关键符号 | 内容 |
| --- | --- | --- |
| [app.js](../../design-system/resonance/proposals/2026-11-04-light-glass/app.js) | transitionPage / stopPageTransition / render | 在内容容器执行单个 WAAPI 位移与透明度动画；同级导航按索引确定方向，返回使用反方向 |
| [style.css](../../design-system/resonance/proposals/2026-11-04-light-glass/style.css) | --page-duration / --page-travel | 220ms、18px，复用 --ease-out 强减速曲线 |
| [index.html](../../design-system/resonance/proposals/2026-11-04-light-glass/index.html) | CSS/JS版本参数 | 刷新后加载本次脚本与样式 |
| [verify-motion.cjs](../../design-system/resonance/proposals/2026-11-04-light-glass/verify-motion.cjs) | 动画检查 | 桌面/手机、快速中断、反向、固定底座、键盘与减少动态 |

开始切页时立即更新目标内容，不等待退出动画；连续操作采样当前容器位移/透明度并取消旧动画，只有一个活动动画。初始加载、同页、播放器专用转场不重复播放。键盘、截图模式、手动减少动态及系统偏好立即取消内容动画。底部播放条与侧栏不参与移动。

## 前后行为

| 操作 | 修改前 | 修改后 |
| --- | --- | --- |
| 主导航 / 曲库标签 / 详情 | 瞬间替换 | 220ms轻位移与淡入 |
| 连续切换 | 无动画 | 最新目的地立即生效，中断旧动画 |
| 减少动态或键盘 | 即时 | 继续即时，运行中的动画也取消 |

## 平台与数据影响

仅HTML原型修改；静态效果图终态布局不变，无需重出156张图。无音频、存储、网络、权限或版本变化。离线包同步更新。原生实现保持现状。

## 验证

- `node --check app.js`：通过。
- `node verify-motion.cjs`：4组通过，覆盖1440px / 390px、220ms与方向、中断后最终页、底座位置、手动/系统减少动态、键盘即时。
- `node verify.cjs`：既有10组本轮重跑通过，页面脚本错误0，含78页360px横溢出检查。
- 应用内浏览器刷新原型并点击“专辑”：页面切换成功，更新后的标签页已保留。
- 首次动效测试把WAAPI规范化的字符串透明度与数字严格比较导致失败；改为数值解析后通过，未为测试改变生产动画。
- 未执行Kotlin构建、原生真机或120Hz性能测量；浏览器自动检查不等于原生流畅度验收。

## 限制与文档

这是短距离内容入场，不是旧页和新页同时在场的共享元素转场。需要用户继续体验手感。更新方案动效表、token记录、开发说明和变更索引；当前静态图仍有效。
