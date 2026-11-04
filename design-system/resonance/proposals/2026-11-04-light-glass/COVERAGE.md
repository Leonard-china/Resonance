# 页面覆盖清单

本轮是浅色设计提案与 HTML 演示，非原生运行截图。覆盖 19 个 LibraryDestination（Playing 合并至播放器/空态，Discover 为搜索，Profile 为工具）、4 个曲库标签、4 种详情及主要操作/状态。重复的删除、确认、进度弹窗复用对应样式，不声称枚举所有业务组合。设置原长页拆为分组页是本方案提议，尚未改动原生导航。

| 页面 / 状态 | 现有实现入口 | 桌面 | 手机 |
| --- | --- | --- | --- |
| 音乐库 · 歌曲 | `LibraryShell.LibraryScreen` | [图片](screenshots/library-desktop.png) | [图片](screenshots/library-phone.png) |
| 音乐库 · 歌单 | `LibraryShell.LibraryTab.Playlists` | [图片](screenshots/playlists-desktop.png) | [图片](screenshots/playlists-phone.png) |
| 音乐库 · 专辑 | `LibraryShell.LibraryTab.Albums` | [图片](screenshots/albums-desktop.png) | [图片](screenshots/albums-phone.png) |
| 音乐库 · 歌手 | `LibraryShell.LibraryTab.Artists` | [图片](screenshots/artists-desktop.png) | [图片](screenshots/artists-phone.png) |
| 歌单详情 | `LibraryDetail.PlaylistDetail` | [图片](screenshots/playlist-desktop.png) | [图片](screenshots/playlist-phone.png) |
| 专辑详情 | `LibraryDetail.AlbumDetail` | [图片](screenshots/album-desktop.png) | [图片](screenshots/album-phone.png) |
| 歌手详情 | `LibraryDetail.ArtistDetail` | [图片](screenshots/artist-desktop.png) | [图片](screenshots/artist-phone.png) |
| 我喜欢的音乐 | `LibraryDetail.Favorites` | [图片](screenshots/favorites-desktop.png) | [图片](screenshots/favorites-phone.png) |
| 本地搜索 | `LibraryDestination.Discover / LibrarySearchIndex` | [图片](screenshots/search-desktop.png) | [图片](screenshots/search-phone.png) |
| 在线搜索 | `OnlineSongSearch` | [图片](screenshots/online-desktop.png) | [图片](screenshots/online-phone.png) |
| 描述找歌 | `SemanticSongSearch` | [图片](screenshots/semantic-desktop.png) | [图片](screenshots/semantic-phone.png) |
| 每日推荐 | `RecommendationsScreen` | [图片](screenshots/recommendations-desktop.png) | [图片](screenshots/recommendations-phone.png) |
| 推荐历史 | `RecommendationHistoryScreen` | [图片](screenshots/recommendation-history-desktop.png) | [图片](screenshots/recommendation-history-phone.png) |
| 播放器 · 封面 | `NowPlayingOverlay` | [图片](screenshots/player-cover-desktop.png) | [图片](screenshots/player-cover-phone.png) |
| 播放器 · 歌词 | `NowPlayingOverlay` | [图片](screenshots/player-lyrics-desktop.png) | [图片](screenshots/player-lyrics-phone.png) |
| 播放器 · 队列 | `NowPlayingOverlay` | [图片](screenshots/player-queue-desktop.png) | [图片](screenshots/player-queue-phone.png) |
| 音乐助手 · 对话 | `AssistantScreen / AssistantMusicAttachment` | [图片](screenshots/assistant-desktop.png) | [图片](screenshots/assistant-phone.png) |
| 音乐助手 · 新会话 | `AssistantWelcome` | [图片](screenshots/assistant-welcome-desktop.png) | [图片](screenshots/assistant-welcome-phone.png) |
| 助手会话历史 | `AssistantScreen history dialog` | [图片](screenshots/assistant-history-desktop.png) | [图片](screenshots/assistant-history-phone.png) |
| 一起听 · 创建房间 | `TogetherScreen disconnected` | [图片](screenshots/together-desktop.png) | [图片](screenshots/together-phone.png) |
| 一起听 · 加入朋友 | `TogetherScreen joining` | [图片](screenshots/join-desktop.png) | [图片](screenshots/join-phone.png) |
| 一起听 · 房间 | `TogetherScreen connected / TogetherRoomComponents` | [图片](screenshots/room-desktop.png) | [图片](screenshots/room-phone.png) |
| 房间成员 | `TogetherRoomComponents / host controls` | [图片](screenshots/members-desktop.png) | [图片](screenshots/members-phone.png) |
| 共享播放队列 | `TogetherRoomComponents expanded queue` | [图片](screenshots/room-queue-desktop.png) | [图片](screenshots/room-queue-phone.png) |
| 房间 AI 选歌 | `TogetherAiQueue` | [图片](screenshots/room-ai-desktop.png) | [图片](screenshots/room-ai-phone.png) |
| 常听伙伴 | `TogetherCompanionPanel` | [图片](screenshots/partners-desktop.png) | [图片](screenshots/partners-phone.png) |
| 共听连接指南 | `TogetherScreen connection help / details` | [图片](screenshots/room-help-desktop.png) | [图片](screenshots/room-help-phone.png) |
| 工具 | `ProfileScreen` | [图片](screenshots/tools-desktop.png) | [图片](screenshots/tools-phone.png) |
| 导入音乐 | `ImportScreen` | [图片](screenshots/import-desktop.png) | [图片](screenshots/import-phone.png) |
| 导入进度 | `MusicTaskProgress / ImportProgress` | [图片](screenshots/import-progress-desktop.png) | [图片](screenshots/import-progress-phone.png) |
| 音频转换 | `ImportScreen KGMA/KGG flow` | [图片](screenshots/conversion-desktop.png) | [图片](screenshots/conversion-phone.png) |
| 歌单导入与导出 | `SyncScreen / SyncPackageDialog` | [图片](screenshots/archive-desktop.png) | [图片](screenshots/archive-phone.png) |
| 歌单包内容预览 | `SyncPackageDialog` | [图片](screenshots/archive-preview-desktop.png) | [图片](screenshots/archive-preview-phone.png) |
| 音乐任务 | `MusicTasksScreen` | [图片](screenshots/tasks-desktop.png) | [图片](screenshots/tasks-phone.png) |
| 歌源管理 | `MusicSourcesScreen` | [图片](screenshots/sources-desktop.png) | [图片](screenshots/sources-phone.png) |
| 来源偏好 | `MusicSourcesScreen source preferences` | [图片](screenshots/source-settings-desktop.png) | [图片](screenshots/source-settings-phone.png) |
| 下载与空间 | `MusicStorageScreen` | [图片](screenshots/storage-desktop.png) | [图片](screenshots/storage-phone.png) |
| 空间清理预览 | `MusicStorageScreen pending cleanup` | [图片](screenshots/cleanup-desktop.png) | [图片](screenshots/cleanup-phone.png) |
| 曲库智能管家 | `SmartManagerScreen` | [图片](screenshots/manager-desktop.png) | [图片](screenshots/manager-phone.png) |
| 重复版本检查 | `SmartManagerScreen duplicate review` | [图片](screenshots/duplicates-desktop.png) | [图片](screenshots/duplicates-phone.png) |
| 音乐品味画像 | `TasteRadarScreen` | [图片](screenshots/taste-desktop.png) | [图片](screenshots/taste-phone.png) |
| 设置 | `SettingsScreen / MusicFeatureSettingsSection` | [图片](screenshots/settings-desktop.png) | [图片](screenshots/settings-phone.png) |
| 外观与动态效果 | `AppearanceSettings` | [图片](screenshots/appearance-desktop.png) | [图片](screenshots/appearance-phone.png) |
| 播放与音效 | `MusicFeatureSettingsSection playback` | [图片](screenshots/playback-desktop.png) | [图片](screenshots/playback-phone.png) |
| 歌词与翻译 | `TranslationControls / floating lyrics preferences` | [图片](screenshots/lyrics-settings-desktop.png) | [图片](screenshots/lyrics-settings-phone.png) |
| 模型与 API | `AiConnectionEditor` | [图片](screenshots/ai-settings-desktop.png) | [图片](screenshots/ai-settings-phone.png) |
| 每日推荐计划 | `MusicFeatureSettingsSection recommendations` | [图片](screenshots/schedule-desktop.png) | [图片](screenshots/schedule-phone.png) |
| 同步歌词生成 | `MusicFeatureSettingsSection speech` | [图片](screenshots/speech-desktop.png) | [图片](screenshots/speech-phone.png) |
| B 站账号 | `BilibiliLoginDialogs` | [图片](screenshots/account-desktop.png) | [图片](screenshots/account-phone.png) |
| 扫码登录状态 | `BilibiliQrLoginDialog` | [图片](screenshots/login-qr-desktop.png) | [图片](screenshots/login-qr-phone.png) |
| 本地音乐与歌词目录 | `SettingsScreen local folders` | [图片](screenshots/folders-desktop.png) | [图片](screenshots/folders-phone.png) |
| 音量平衡说明 | `AudioBalanceHelpScreen` | [图片](screenshots/audio-help-desktop.png) | [图片](screenshots/audio-help-phone.png) |
| 使用教程 | `TutorialScreen / TutorialCatalog` | [图片](screenshots/tutorial-desktop.png) | [图片](screenshots/tutorial-phone.png) |
| 教程章节 | `TutorialScreens quick / chapter` | [图片](screenshots/tutorial-chapter-desktop.png) | [图片](screenshots/tutorial-chapter-phone.png) |
| 关于 Resonance | `AboutScreen / SponsorDialog` | [图片](screenshots/about-desktop.png) | [图片](screenshots/about-phone.png) |
| 版本更新说明 | `AboutScreen release notes` | [图片](screenshots/release-notes-desktop.png) | [图片](screenshots/release-notes-phone.png) |
| 支持作者 | `SponsorDialog` | [图片](screenshots/sponsor-desktop.png) | [图片](screenshots/sponsor-phone.png) |
| 新建歌单 | `CreatePlaylistDialog / RenamePlaylistDialog` | [图片](screenshots/create-playlist-desktop.png) | [图片](screenshots/create-playlist-phone.png) |
| 编辑歌曲信息 | `EditTrackDialog` | [图片](screenshots/edit-track-desktop.png) | [图片](screenshots/edit-track-phone.png) |
| 多选与歌单管理 | `BatchAddToPlaylistDialog / selection toolbar` | [图片](screenshots/batch-desktop.png) | [图片](screenshots/batch-phone.png) |
| 添加到歌单 | `BatchAddToPlaylistDialog` | [图片](screenshots/add-to-playlist-desktop.png) | [图片](screenshots/add-to-playlist-phone.png) |
| 批量补全歌曲信息 | `BatchEnrichDialog` | [图片](screenshots/enrich-desktop.png) | [图片](screenshots/enrich-phone.png) |
| 补全结果报告 | `BatchEnrichDialog report` | [图片](screenshots/enrich-result-desktop.png) | [图片](screenshots/enrich-result-phone.png) |
| 歌单截图导入 | `ScreenshotImportDialog` | [图片](screenshots/screenshot-desktop.png) | [图片](screenshots/screenshot-phone.png) |
| 截图识别结果 | `ScreenshotImportDialog recognized songs` | [图片](screenshots/screenshot-result-desktop.png) | [图片](screenshots/screenshot-result-phone.png) |
| B 站音频导入 | `VideoImportDialog` | [图片](screenshots/video-desktop.png) | [图片](screenshots/video-phone.png) |
| 睡眠定时 | `SleepTimerDialog` | [图片](screenshots/sleep-desktop.png) | [图片](screenshots/sleep-phone.png) |
| 音效预设 | `AudioEffectDialog` | [图片](screenshots/effects-desktop.png) | [图片](screenshots/effects-phone.png) |
| 音乐分享卡 | `ShareCardDialog` | [图片](screenshots/share-desktop.png) | [图片](screenshots/share-phone.png) |
| 歌曲来源详情 | `TrackSourceDetails` | [图片](screenshots/track-source-desktop.png) | [图片](screenshots/track-source-phone.png) |
| 删除确认 | `DeletePlaylistDialog / BatchDeleteTracksDialog` | [图片](screenshots/delete-confirm-desktop.png) | [图片](screenshots/delete-confirm-phone.png) |
| 播放失败反馈 | `PlaybackErrorDialog` | [图片](screenshots/playback-error-desktop.png) | [图片](screenshots/playback-error-phone.png) |
| 空曲库 | `LibraryScreen empty / PlayingPlaceholderScreen` | [图片](screenshots/empty-library-desktop.png) | [图片](screenshots/empty-library-phone.png) |
| 加载与缓冲 | `PlaybackState feedback / MusicTaskProgress` | [图片](screenshots/loading-desktop.png) | [图片](screenshots/loading-phone.png) |
| 品味雷达与维度 | `TasteRadarScreen dimension details` | [图片](screenshots/taste-dimensions-desktop.png) | [图片](screenshots/taste-dimensions-phone.png) |
| 天气配色与城市 | `AppearanceSettings weather` | [图片](screenshots/weather-desktop.png) | [图片](screenshots/weather-phone.png) |
| 推荐反馈 | `Recommendation feedback reason/comment dialog` | [图片](screenshots/feedback-desktop.png) | [图片](screenshots/feedback-phone.png) |
| 悬浮歌词 | `FloatingLyricsHost / DesktopFloatingLyrics` | [图片](screenshots/floating-lyrics-desktop.png) | [图片](screenshots/floating-lyrics-phone.png) |
