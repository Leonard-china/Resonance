# 2026-11-03 — 重排一起听房间、固定邀请入口并收纳长播放队列

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：UI 重构 / 交互修复 / 本地 APK 构建
- 适用版本：`0.6.5 / 39`，未升版
- 影响平台：Android／Windows 共享 UI；本次构建 Android Release
- 关联需求：用户反馈一起听界面简陋，创建房间后展开很长的播放列表，复制邀请必须滚到页面底部。
- 前置记录：[磨砂界面与 APK](2026-11-03-02-frosted-ui-and-release-apk.md)、[之前的一起听首页简化](2026-11-01-14-dual-theme-and-simpler-together-ui.md)。

## 需求或问题

用户需要创建房间后马上复制邀请，同时希望一起听拥有与播放器相符的毛玻璃质感和明确的信息层级。当前会话此前明确要求自行试用、不运行测试，本次延续该要求并重新提供 APK。

## 原因与依据

`TogetherScreen` 原先使用工具页的单个滚动 Column，将 `room.sharedQueue` 通过 `forEachIndexed` 全量组合在邀请区之前；邀请区又位于 AI 选歌之后。队列越长，复制入口距离越远。二维码、地址说明、刷新、分享和复制与正文混在同一滚动流，成员与队列也默认全部展开。仅修改容器底色没有解决页面结构。

## 具体修改

| 文件 | 关键类／函数 | 具体修改与目的 |
| --- | --- | --- |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | `TogetherScreen`、邀请与房间状态 | 改用独立房间布局；复制／分享及结束／退出放在滚动区之外。复制成功在按钮显示两秒反馈，错误在固定页头提示；过期时主按钮提供刷新。二维码、刷新、地址／端口编辑由邀请菜单进入。保留完整邀请的预览与显式加入确认 |
| [TogetherRoomComponents.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherRoomComponents.kt) | `TogetherRoomPage`、`TogetherRoomHero` | 固定磨砂页头与独立滚动正文，880dp 内容宽度上限；房间主区展示本机或远端实际曲目封面、标题、真实连接状态、人数和核验码，无封面使用原有回退 |
| 同上 | `TogetherSection`、`TogetherQueueSummary` | 分区留白 16dp，成员与 AI 选歌默认收起；展开高度使用既有阻尼 1 弹簧并尊重减少动效。队列摘要最多组合当前及后续两首，没有当前项时展示前三首；不再组合整个队列 |
| 同上 | `TogetherQueueDialog`、`TogetherQueueRow` | 完整队列使用 85% 可用高度、680dp 宽度上限的原生 Dialog；关闭和返回入口固定。`LazyColumn` 按稳定歌曲 ID 惰性组合，首次定位当前项；点歌直接可用，排序与移除放在逐曲菜单，保留连接／可播放／多人状态限制 |
| [DESIGN.md](../../DESIGN.md)、[PRODUCT.md](../../PRODUCT.md) | 一起听布局／产品事实 | 记录用户反馈、固定邀请与三首摘要、独立完整队列和默认折叠规则 |

创建／加入不同房间时重置房间内容滚动与临时弹层；普通队列更新和播放时钟更新不会重置正文位置。返回同一页面时保留既有保存的滚动位置。临时邀请、二维码与地址输入未新增 SavedState／磁盘保存。AI 选歌仍须手动展开、输入、生成及确认，不自动调用模型。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 创建带有长队列的房间 | 全量列表把复制入口推到下方 | 复制入口在固定页头，队列摘要最多三首 |
| 浏览聊天或列表下方内容 | 复制和结束入口随页面滚走 | 房间页头持续提供复制、分享及结束／退出 |
| 查看完整队列 | 与房间正文一起滚动、一次组合全部条目 | 手动打开独立惰性列表，关闭入口固定，保留原队列操作 |
| 邀请过期 | 复制按钮禁用，另找刷新 | 同一主按钮显示“刷新邀请”；分享／二维码保持不可用，刷新后再复制 |
| 想看成员或使用 AI 选歌 | 默认展开并增加页面长度 | 显示摘要及明确入口，按需展开 |
| 打开房间 | 图标与长段状态文字堆叠 | 实际歌曲封面、状态、成员和队列分层呈现 |

## 平台与数据影响

只调整共享原生 Compose UI；Android 优先，Windows 使用同一布局。没有新增依赖或更改平台接口、网络协议、服务器、播放同步、队列数据、存储格式、权限、文件删除或元数据写回。摘要不截断实际共享队列。聊天与伙伴实现保持原有行为。版本来源仍为 `version.properties`，没有修改签名配置或读取私钥。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 下方 Android Release 构建命令 | Linux，既有临时 JDK 17 / Android SDK 36 工具链 | 通过；首轮 43 秒，细化固定退出入口／动画／滚动状态后的最终轮 38 秒，49 tasks：6 executed、43 up-to-date |
| `apksigner.jar verify --verbose` 对本次交付 APK | Android Build Tools 36.0.0 | 通过，APK v2 签名有效、1 个签名者 |
| 本次文件差异、文档链接与空白检查 | 工作区静态核对 | 通过；保留此前所有未提交修改 |
| 单元／UI／设备测试、Lint、截图、跨网／多人验收 | 未执行 | 遵照当前会话“不用你测试”的要求，不引用历史测试结果 |
| Windows 编译、安装与设备试用 | 未执行 | 本次仅构建 Android APK |

实际构建命令（工具链位置仅为本机忽略目录，不作为提交资源）：

```bash
build/lyrics-motion-2026-11-03/tooling/jdk/bin/java \
  -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:assembleRelease \
  -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease \
  --no-configuration-cache --console=plain
```

本次构建日志：`build/together-room-ui-2026-11-03/release-build.log`、`release-final.log`；签名检查：`apk-signature.log`。只有既有弃用／可空性及实验 Lint 配置警告，无构建错误。

本次安装包：[Resonance-0.6.5-release.apk](../../release/0.6.5/2026-11-03-together-room/Resonance-0.6.5-release.apk)。大小 **167,349,964 字节（159.60 MiB）**；SHA-256：

```text
a370bea83791f5207533efec19dd97c9c2070966e899fdb94c64fdf58bd45525
```

同目录含 `SHA256SUMS.txt` 和试用说明；上一版磨砂 APK 保留，未覆盖。没有公开发布、推送或安装到设备。

## 限制与后续

实现和 Android Release 构建已完成；本版视觉、极小窗口／大字号、真实复制／分享、滚动／聊天及队列操作仍待用户设备试用，不能将构建成功当作功能或性能通过。邀请有效期和直连／中继能力沿用原有服务，未改变连接可靠性。后续定位入口为 `TogetherScreen` 的固定 header 与 `TogetherRoomComponents`。

## 同步更新的文档

- [文档入口与变更索引](../README.md)。
- [开发说明](../DEVELOPMENT.md)第 7.1 节当前一起听 UI、任务到代码定位表。
- [原生设计规范](../../DESIGN.md)与[产品事实](../../PRODUCT.md)。
