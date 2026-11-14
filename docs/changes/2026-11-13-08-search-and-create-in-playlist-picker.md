# 2026-11-13 — 在加入歌单窗口搜索或新建歌单

- 修改日期：2026-11-13（Asia/Shanghai）
- 类型：交互改进
- 适用版本：0.6.11 / 45，版本号保持原值
- 影响平台：Android、Windows、Linux
- 关联：[合并歌曲菜单中的歌单操作](2026-11-13-06-collapse-track-playlist-menu.md)

## 需求与原因

歌曲更多菜单合并后，歌单很多时仍要滚动查找。没有歌单时需要退出选择窗口，回到音乐库创建后再次打开。

## 修改与行为

| 文件或符号 | 修改 |
| --- | --- |
| [TrackPlaylistDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TrackPlaylistDialog.kt) | 至少 8 个歌单时显示名称搜索。搜索仅改变显示的列表，切换搜索词保留各歌单的勾选草稿。可在窗口内填写新歌单名称，完成时创建并加入当前歌曲。取消丢弃输入和勾选。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | “更多操作 → 加入歌单…”打开选择窗口。新建复用 `MusicAssistantServices.saveToPlaylist` 和原有曲库刷新，保存前等待已排队的歌单修改。 |
| [TrackPlaylistDialogTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/TrackPlaylistDialogTest.kt) | 验证搜索保留隐藏勾选、创建前取消、空名称和大字号短窗口操作。 |

## 平台与数据

使用共享 Compose 原生窗口。新建走已有歌单数据模型和保存路径。既有歌单的加入和移出继续通过原成员关系回调。无新权限或网络请求。

## 验证

使用 Gradle `:composeApp:desktopTest` 的编译产物与依赖，在 Xvfb 中直接运行 JUnit。`TrackPlaylistDialogTest` 三项通过，覆盖隐藏勾选保留、取消新建、空名称和大字号短窗口。构建与回归同[封面和歌词记录](2026-11-13-07-video-catalog-artwork-and-lyrics.md)。

## 限制与后续

手机未连接，系统键盘和真机点击待复测。当前选择窗口的新建入口使用应用已有的音乐服务。

## 同步文档

更新 [文档索引](../README.md) 和 [开发说明](../DEVELOPMENT.md) 第 11 节。
