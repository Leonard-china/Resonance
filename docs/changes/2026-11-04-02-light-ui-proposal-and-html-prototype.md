# 2026-11-04 — 提供全页面双端浅色 UI 方案与 HTML 动效原型

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：设计提案 / 文档 / 交互原型
- 适用版本：0.6.5 / 39，未改变版本号
- 影响平台：Android、Windows、Linux 的未来共享 UI；本次产物为浏览器设计演示
- 关联需求：用户要求参考磨砂音乐播放器图片，先给每页浅色图和可试的 HTML 小动效；确认桌面与手机都要

## 需求或问题

用户希望在重构原生 UI 之前审阅完整方案，而非只看首页。现有历史效果图没有覆盖全部目的地、工具、设置与关键弹窗，也没有本轮可交互预览。

## 原因与依据

依据 `LibraryModels.LibraryDestination` 的19个目的地，`LibraryShell` 的4种曲库标签与4种详情，及播放器、Together、Tools、MusicFeature、Tutorial等组件定点梳理。设置原本包含长表单，本方案提出按任务拆分子页，不把提议写成已经生效的原生导航。

用户指定浅色、桌面和手机，要求先看方案。HTML 是本次明确要求的设计验证产物，不移植为原生运行容器。原生与官网代码均未修改。

## 具体修改

| 文件 | 关键符号 / 内容 | 修改与目的 |
| --- | --- | --- |
| [原型入口](../../design-system/resonance/proposals/2026-11-04-light-glass/index.html) | reviewbar / 页面选择 | 本地打开，切换桌面与手机、减少动态、进入图片总览 |
| [页面脚本](../../design-system/resonance/proposals/2026-11-04-light-glass/app.js) | register / render / drawPlayer / setLyric / handle | 78个页面与关键状态；本地搜索、示例播放/队列、聊天、邀请、表单反馈及歌词演示 |
| [样式](../../design-system/resonance/proposals/2026-11-04-light-glass/style.css) | container queries / player / dock | 雾白+莓红浅色体系、桌面侧栏、手机底座、280ms可反向播放器过渡、稳定行高歌词 |
| [图片总览](../../design-system/resonance/proposals/2026-11-04-light-glass/gallery.html) | 分类筛选 / 原图链接 | 156张双端首屏效果图，可打开原图和对应原型页面 |
| [方案](../../design-system/resonance/proposals/2026-11-04-light-glass/PLAN.md)、[覆盖表](../../design-system/resonance/proposals/2026-11-04-light-glass/COVERAGE.md) | token / 页面源码映射 / 后续步骤 | 明确提案与现状、验证与限制 |
| [截图脚本](../../design-system/resonance/proposals/2026-11-04-light-glass/capture.cjs)、[交互检查](../../design-system/resonance/proposals/2026-11-04-light-glass/verify.cjs) | Chromium 截图 / 行为检查 | 可复现双端图与10组关键交互 |

歌曲、歌词、聊天、统计与房间为虚构数据。素材来源与Lucide许可记录在 `assets/SOURCES.md` 与 `assets/lucide-LICENSE`。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 审阅完整浅色方向 | 历史部分页面图 | 78个页面/关键状态，每项均有桌面与手机首屏图 |
| 体验动画 | 原生历史实现、静态方案图 | HTML 可试播放器展开/收起、歌词连续定位与按压反馈 |
| 找对应页面 | 分散源文件 | 覆盖表、分类图片总览与顶部页面切换 |
| 原生应用 | 当前0.6.5实现 | 不变；未将本轮方案擅自写入生产 UI |

## 平台与数据影响

无 Kotlin、Gradle、版本、权限、真实音乐文件、存储、同步包、网络路由或密钥变更。HTML不连接模型、房间或音源服务，不删除/写回真实音频，不持久化表单凭据。所有资源本地引用，可离线打开。不读取用户曲库、真实签名或 API Key。`website/` 未修改。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `node --check app.js` | Node | 通过 |
| `node capture.cjs` | Chromium，1440×960、390×844 | 156张图；脚本错误0、图片缺失0、内容横向溢出0 |
| `node verify.cjs` | Chromium，含360px窄屏、系统减少动态 | 10组通过；覆盖全部78页面窄屏溢出检查 |
| `impeccable detect --json index.html style.css app.js` | 项目技能静态扫描 | 通过，报告 `[]` |
| 8张联系表与关键全尺寸图审阅 | 截图与源码 | 修复助手附件窄屏排版；独立审阅要求移除详情重复标签和共听/教程的不相关歌曲操作，已修并重截 |
| Kotlin/Gradle、Android真机、Windows/Linux原生窗口 | 未执行 | 本轮只交付方案，无原生修改；不能把浏览器图当原生验收 |

首次交互检查末项把浏览器的 `0s` 规范化结果误写为 `0s, 0s`，导致断言失败；改为逐项检查零时长后10组通过，未为测试改变产品动画。

## 未完成项与限制

- 方案尚待用户视觉反馈；未实施原生重构、打包、发布或提交。
- 78指页面与关键状态数量；通用确认弹窗与相似状态复用，不代表穷举所有业务组合。
- 图片展示固定首屏，长页面在原型中滚动；系统文件选择器、真实支付/登录二维码不伪造。
- 无真实音频输出。网络、付费、删除等按钮明确给出演示反馈。
- 需后续原生验证大字、48dp触摸、系统栏、IME、鼠标键盘、性能与平台差异。

## 同步文档

更新 `docs/README.md` 变更索引、`docs/DEVELOPMENT.md` UI章节，标明本方案是候选而非现行原生规范。保留根 `DESIGN.md` 的已实施基线。

独立审阅收尾：两项修正均为 resolved，disposition 为 ship；详细范围见 [REVIEW.md](../../design-system/resonance/proposals/2026-11-04-light-glass/REVIEW.md)。此结论仅针对原型与所列修正。
