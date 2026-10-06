# Resonance v0.2.5

## 重点更新

- **播放模式记忆与持久化修复**：
  - 修复最后一次播放选择随机模式后，重新打开应用未保持随机播放的问题。
  - 新增 `PlaybackModePreference` 数据模型，支持随机播放（`shuffleEnabled`）与循环模式（`repeatMode`）的跨会话记忆。
  - Android 平台通过 `SharedPreferences` 持久化，并在 `MediaController` 连接建立与模式切换时自动同步至底层播放器；Windows 桌面端通过 `ui-state.properties` 持久化。
  - 歌单、专辑、歌手及收藏详情页点击「随机播放」按钮时，联动激活并持久化播放器的随机模式。
  - 修复退出试听预览或删除单曲重置状态时误重置播放模式的问题。
  - 新增 `PlaybackModePersistenceTest` 单元测试，保证播放模式持久化与状态恢复的可靠性。

## 构建产物与校验

| 文件 | 平台 | 大小 | SHA-256 |
| --- | --- | --- | --- |
| `Resonance-0.2.5-release.apk` | Android | 59.92 MiB | `D5DC0CA28BF6883E5367FC360442CF59DDE9FDDF3C87659AF39D117D5BB39CF6` |
