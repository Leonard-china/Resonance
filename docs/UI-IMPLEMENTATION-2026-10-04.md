# Resonance UI 实施与实际检查报告

日期：2026-10-04。交付版本：**0.2.4 / versionCode 12**。

## 交付与工作区保护

- 已按照 [UI 审阅](UI-OPTIMIZATION-AUDIT-2026.md)、[确认方向](UI-DIRECTION-2026-10-04.md) 和 [原生设计规范](DESIGN.md) 完成代码修改。保留 Kotlin Multiplatform / Compose，沿用现有业务状态与平台服务。
- 开始时生产源码没有未提交修改；已有未跟踪的审阅、方向文档与概念图已备份到 `build/ui-workspace-backup-20261004`，保留原文件。没有重置工作区，也没有创建提交。
- Windows 检查使用 `RESONANCE_DATA` / `RESONANCE_LIBRARY` 指向 `build/ui-check/desktop-profile`，隔离用户真实曲库。Android 使用模拟器；检查音频、封面、歌词都是本次生成的样本。
- 最终 APK：`composeApp/build/outputs/apk/release/Resonance-0.2.4-release.apk`，**62,817,725 字节（约 59.91 MiB）**。已安装到 Android 模拟器并运行。
- 通过真实微信桌面界面的文件选择器发送到用户的**文件传输助手**。16:57 先发送布局版；歌词可读性收尾后，17:21 重新发送 **`Resonance-0.2.4-release-final.apk`**（与上述构建产物字节一致，请使用这条最新文件）。已确认最终上传完成，没有把私人聊天截图保存到项目。

SHA-256：

```text
CA3D59B208ECC372CBBDB7CD22EF5B778602F3FD3EDCFF02A250191A3B8DBFFC
```

## 修改内容

### 视觉与导航

- 统一浅灰绿 / 冰蓝、乳白磨砂与深色灰绿黑的语义颜色，补全 Material 颜色容器，移除 UI 的珊瑚、橘黄装饰色；真实封面颜色保留。
- 新安装默认浅色；已有用户的深色、浅色、跟随系统设置继续生效。强调色与深浅主题独立。
- 结构表面采用局部 Haze 背景模糊、轻染色与克制阴影，回退使用高不透明度表面。正文和封面保持清晰。
- 导航收敛为音乐库、搜索、工具；播放器从持久迷你播放器进入。
- 默认展示用户实际创建 / 导入的歌单：自适应封面网格、名称两行、曲目数、创建入口与空状态。保留歌曲、专辑、歌手视图和多选业务。
- 搜索拥有独立查询区、初始引导、结果与无结果状态，复用现有本地搜索和拼音匹配逻辑。

### 播放器与响应式

- 紧凑屏底部导航，中等宽度导航 rail，宽屏侧栏；工具正文限宽且可滚动。
- 竖屏播放器依次展示封面、曲目、进度、主控制；宽屏使用封面 / 操作双栏，仅保留一份封面。
- 手机横屏优先显示进度和播放控制，歌词 / 队列在滚动内容中继续访问。修复实际检查中发现的重复封面与主控制被挤出首屏问题。
- 大屏主要内容居中并限制至 1280dp × 900dp；修复平板纵向留白过大、主控制离曲目信息过远的问题。
- 收藏、更多菜单、睡眠定时、浮动歌词、删除、进度、音量、倍速、歌词、队列的现有操作继续调用原回调。

### 动效、对比度和可访问性

- 使用临界阻尼与不小于 0.96 的按压缩放；持续环境漂移只在应用前台且播放时运行。
- 接入系统减少动画信号并提供应用内开关，减少动画时停止环境漂移、移除弹性位移与歌词自动滚动动画。
- 交互目标至少 48dp，图标按钮增加单一语义名称与操作，选中状态有语义反馈；播放、进度和音量具备可访问性标签。
- 浅色次要文字改为实色；主按钮统一使用 `onPrimary`。冰蓝主色加深到 `#315FD0`。
- 补查歌词后进一步移除非当前歌词和预览上下行的低透明度，保持可读的实色；歌词点击行最小高度 48dp。Android 全屏播放器 Dialog 独立设置系统栏的深浅图标与透明背景。
- Token 对比度计算：冰蓝文字 / 浅蓝选中背景约 4.85:1，白色 / 冰蓝主按钮约 5.71:1；绿色 / 浅绿选中背景约 4.68:1，次要文字 / 浅绿背景约 4.69:1。已查看实际玻璃画面，但没有逐像素审计全部组合。

### 天气与设置

- 提供浅灰绿、冰蓝银白、随天气三种模式。晴朗 / 局部多云映射绿色，雨雪 / 雷暴映射蓝色；阴天、雾、未知与请求失败保留上次配色。
- Android 仅申请前台大致位置；Windows 尝试系统可用定位；无位置时使用用户搜索并选择的城市。固定配色不请求天气。
- Open-Meteo 天气与城市搜索通过独立服务访问，前台按约 30 分钟缓存更新；粗略坐标、上次配色与天气状态持久化，不增加后台定位服务。
- 移除作者机器的歌词目录默认值。初始为空，可自动查找或选择文件夹；Android 使用 SAF 授权目录，Windows 使用原生目录选择器。

## 构建与自动检查

使用 JDK 17、Gradle wrapper 8.11.1、AGP 8.9.2、Kotlin 2.3.20、Compose 1.11.0。升级 wrapper / AGP 是为满足现有 AndroidX 依赖要求。

```powershell
.\gradlew.bat :composeApp:assembleRelease :composeApp:desktopTest --no-configuration-cache --console=plain
.\gradlew.bat :composeApp:run --no-configuration-cache --console=plain
```

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 最终 Android release 构建 | BUILD SUCCESSFUL，36 秒 | `build/ui-check/release-verification.log` |
| 桌面测试 | 18 个 suite，43 项，0 失败 / 错误 / 跳过 | `composeApp/build/test-results/desktopTest` |
| 天气映射与位置校验 | 4 项新测试通过，包含晴 / 雨雪 / 保留 / 无效位置 | `WeatherPaletteTest.kt` |
| Windows 编译及启动 | BUILD SUCCESSFUL，实际窗口运行后正常关闭 | `build/ui-check/windows-final-run.log` |
| APK 签名 | apksigner 验证通过，v2 签名、1 个 signer | 现有 release 签名配置；minSdk 26 |
| Android 安装 | adb install 成功，最终 APK 实际运行 | API 37 模拟器 |

构建成功不代表 Lint 完整干净：AGP 8.9 的 Lint 分析器对 Kotlin 2.3 metadata 报版本不兼容，`lintVitalAnalyzeRelease` 有诊断，release 任务仍完成。没有隐藏此诊断，也没有宣称 Lint 零问题。另有 Tooltip API、Android 单次定位与既有桌面 API 的弃用警告、既有 JavaFX unnamed-module 警告及 FFmpeg 库无法 strip 的提示。打包曾因 Windows 占用 `classes.dex` 失败，重试后成功；该失败日志保留在 `build/ui-check/release-file-lock.log`。

## 实际运行矩阵

实际检查是以下具体组合，**没有完成所有页面 × 所有尺寸 × 所有主题的笛卡尔积**。截图是原生应用真实运行画面，样本数据只用于检查，不是产品内伪造推荐歌单。

| 平台 / 尺寸 | 实际检查的主题与状态 | 结果 / 边界 |
| --- | --- | --- |
| Android API 37 模拟器，1080×2400、420dpi，约 411×914dp | 绿色浅色初始空曲库；本地媒体权限与扫描；创建 Morning samples 歌单并添加歌曲；搜索初始、rain 命中、zzzz 无结果与键盘；浅蓝设置和播放器；深蓝歌单和播放器 | 样本 3 首实际扫描导入；搜索结果可打开播放；最终无结果画面和键盘下迷你播放器可见 |
| 同一模拟器，字体 130% | 深蓝歌单、竖屏播放器 | 名称、主控制与底部安全区域已实际查看 |
| Android 横屏 2400×1080、420dpi，约 914×411dp | 浅蓝播放器；最终深蓝播放器、字体 130% | 主进度与传输控制首屏可见，歌词 / 队列可滚动访问 |
| Android 平板视口约 1066×1706dp | 1600×2560、240dpi 的浅蓝曲库 / 播放器 / 队列；最终 2800×4480、420dpi 的深蓝播放器、字体 130% | 最终居中有界布局已查看；这是模拟器尺寸调整，未使用实体平板 |
| Android 工具与设置 | 导入页与导入链接对话框；同步页与二维码生成提示；深浅主题、固定配色、减少动态效果；SAF 歌词文件夹选择、保存和匹配 LRC 实际读取；天气大致定位授权、上海城市搜索 / 选择、失败提示 | 页面及相关交互实际操作；浅蓝主题下看到 LRC 时间轴、中文当前行与进度变化；没有生产外部歌单导入、真实双设备同步或成功实时天气切色 |
| Windows 原生 Compose，最小 AWT 窗口 920×640（客户区截图约 906×634） | 歌单、播放器；浅绿与最终深蓝；样本 LRC 歌词 / 播放进度；空歌词目录 | 使用隔离资料目录，原用户曲库未修改 |
| Windows 宽窗口，截图 1920×1032 | 浅绿歌单网格与长名称、rain 搜索；浅蓝 / 深蓝设置；深色导入、同步；最终深蓝宽屏播放器 | 最终播放器内容限宽 / 限高实际查看；不是完整 Windows EXE 打包 |

Android 字体最终恢复 100%、分辨率 / 密度恢复原值并转回竖屏。Windows 检查窗口已正常关闭。

## 实际截图

截图存于 `docs/ui-checks/2026-10-04/`。这些都是实施过程中的真实检查图；图名的 final 表示当时完成布局修正后的检查。最后补查又提高了非当前歌词对比度、修正 Android 播放器系统栏，收尾截图另列于下方，布局断点保持一致。

| 画面 | 链接 |
| --- | --- |
| 手机浅色初始空歌单 | [实际截图](ui-checks/2026-10-04/android-light-empty.png) |
| 手机搜索无结果与键盘 | [最终截图](ui-checks/2026-10-04/android-search-empty-final.png) |
| 手机冰蓝浅色设置与表单 | [最终截图](ui-checks/2026-10-04/android-settings-blue-light-final.png) |
| 手机冰蓝浅色歌词时间轴、实际 LRC 与进度 | [最终截图](ui-checks/2026-10-04/android-lyrics-blue-light-final.png) |
| 手机竖屏播放器，字体 130% | [最终截图](ui-checks/2026-10-04/android-player-phone-font130-final.png) |
| 手机横屏播放器，字体 130% | [最终截图](ui-checks/2026-10-04/android-player-landscape-font130-final.png) |
| 平板视口有界播放器，字体 130% | [最终截图](ui-checks/2026-10-04/android-player-tablet-bounded-final.png) |
| Windows 浅绿宽屏歌单 | [实际截图](ui-checks/2026-10-04/windows-library-wide.png) |
| Windows 浅绿搜索结果 | [实际截图](ui-checks/2026-10-04/windows-search.png) |
| Windows 深蓝设置 | [实际截图](ui-checks/2026-10-04/windows-settings-blue-dark.png) |
| Windows 导入 | [实际截图](ui-checks/2026-10-04/windows-import-dark.png) |
| Windows 同步 | [实际截图](ui-checks/2026-10-04/windows-sync-dark.png) |
| Windows 小窗口播放器 | [最终截图](ui-checks/2026-10-04/windows-player-920-dark-final.png) |
| Windows 宽屏播放器 | [最终截图](ui-checks/2026-10-04/windows-player-wide-dark-final.png) |
| Windows 收尾后的歌词对比度与时间轴 | [收尾截图](ui-checks/2026-10-04/windows-lyrics-dark-release.png) |
| 最新 APK 的手机浅蓝歌单首页 | [收尾截图](ui-checks/2026-10-04/android-library-blue-light-release.png) |
| 最新 APK 的手机浅蓝歌词与控制 | [收尾截图](ui-checks/2026-10-04/android-lyrics-blue-light-release.png) |
| 最新 APK 的手机浅蓝横屏主控制 | [收尾截图](ui-checks/2026-10-04/android-landscape-blue-light-release.png) |

## 明确未验证或受限的部分

1. **天气完整联网切色未通过端到端检查**：上海城市搜索成功；当前天气接口返回 HTTP 429，应用显示失败 / 重试并保留原配色。晴雨雪映射测试通过，但不能代替真实成功响应检查。
2. **物理定位未验证**：模拟器授权了大致位置，注入 GPS 后大致位置 provider 仍未返回可用位置，实际使用城市兜底；Windows 成功定位和拒绝定位后的完整界面流程尚未实测。
3. **歌词检查范围**：最初 Android 样本 sidecar 名称与曲目元数据不对应；补上匹配曲名的 LRC 后重启、重新播放，SAF 授权目录读取成功，实际显示时间轴与中文当前行。Windows 也已实际加载 LRC 并观察到高亮 / 进度。Android KRC、嵌入歌词、时间轴编辑保存及系统浮动歌词权限流程仍未完整实测。
4. **导入 / 同步**：本地扫描成功；外部酷狗歌单生产链接、完整格式转换、实体 Android ↔ Windows 二维码 / 局域网传输没有完成端到端运行。现有导入、转换、歌词、同步相关桌面测试继续通过。
5. **设备与可访问性**：没有连接实体 Android 手机 / 平板，没有运行 Android API 26 的无模糊回退，没有实际使用 TalkBack / Narrator，也未检查 Windows 125% / 150% DPI、第二显示器或性能基准。
6. **玻璃依赖**：Haze 1.7.2 在当前 Compose 1.11.0 的 Windows 运行时出现 ShaderBrush ABI 不兼容，因此改用与本项目运行时兼容的 `haze` / `haze-blur 2.0.0-alpha02`。实际 Android / Windows 可运行，但这是预发布依赖，未来升级应重新运行两端检查。
7. **播放器系统栏**：独立 Dialog 已加入系统栏外观配置，API 37 实际截图仍显示灰色系统对比度栏和白色图标，可读但没有呈现完全透明的浅色系统栏；该系统版本的覆盖行为仍待进一步适配。

## 后续复查提示词

```text
请先阅读 docs/DESIGN.md、docs/UI-DIRECTION-2026-10-04.md 和 docs/UI-IMPLEMENTATION-2026-10-04.md，检查并保护当前未提交改动。沿用 Kotlin Multiplatform / Compose、浅色乳白磨砂、真实歌单优先以及绿色 / 冰蓝天气配色，保留现有业务行为。
优先补齐实施报告中未验证的真实流程：实体 Android 大致定位与城市兜底、天气成功响应及晴雨雪自动切色、Android KRC / 嵌入歌词 / 时间轴保存、外部歌单导入、Android / Windows 实际同步、Windows 高 DPI、TalkBack / Narrator 和旧 Android 模糊回退。解决发现的实际问题，再用真实应用截图与构建日志汇报；不要把模拟图、映射单元测试或页面打开成功当作端到端验证。不要升级 Kotlin / Compose 或改动业务协议来绕开 UI 问题。
```
