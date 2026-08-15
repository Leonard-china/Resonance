# Resonance v0.1.6

发布日期：2026-08-15

## 本版本重点

- 启动 UI 重构（见 [`docs/UI-REFACTOR-2026.md`](UI-REFACTOR-2026.md)）：深色沉浸式本地音乐空间，珊瑚色作为主要动作色，紫色与蓝色仅作环境光。
- 重构 LibraryShell 导航与歌单切换：导航保持稳定、只移动内容，歌单切换保持空间连续性，动效遵循 160ms 反馈 / 280ms 页面切换基线。
- 首页"你的歌单"只展示用户创建或导入的歌单；本机曲目仍参与播放队列与歌单匹配，但不再作为首页歌单卡片出现。
- 最后选择的歌单单独持久化，启动时仅在该歌单仍存在时恢复，否则回退到第一个歌单。
- 新增 `SelectedPlaylistStateTest` 覆盖歌单选择状态逻辑。
- Android 主题与平台服务随重构对齐（`ResonanceTheme`、`PlatformServices` 等）。
- 构建产物改为带版本号命名：`Resonance-<versionName>-release.apk`，输出固定到 `build/outputs/apk/release/`。
- 版本升级到 `0.1.6`（Android versionCode 7），支持覆盖升级 `0.1.5`。

## 验证结果

- 测试套件全部通过（含新增的 SelectedPlaylistStateTest）。
- APK 应用名为 `Resonance`，versionCode 7、versionName 0.1.6，签名验证通过（证书 SHA-256 `B2B4AD35B8C7136569D219103448EAEE75D4BBD30F3872C601373DF43B303FE5`）。
- 构建产物命名与版本号自动同步，无需手动改名。

## 发布文件

| 文件 | SHA-256 |
| --- | --- |
| `Resonance-0.1.6-release.apk` | `485F5A600C9668E4A84E554077590C750143A93D93206E6CAD94D018616D6F64` |

## 已知限制

- 本版本仅发布 Android APK；Windows 便携版 / 安装程序 / MSI 沿用 v0.1.5（可在 GitHub Releases 历史版本中下载）。
- Windows 安装程序仍未使用商业代码签名证书，首次运行可能触发 SmartScreen。
- 应用内部功能与 KGMA 支持范围与 v0.1.5 相同；本版本主要是 UI 与发布流程更新。
