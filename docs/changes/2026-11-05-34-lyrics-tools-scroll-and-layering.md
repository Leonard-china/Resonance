# 2026-11-05 — 歌词工具独立滚动与校准面板层级

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能／Bug修复／性能与验证
- 适用版本：`0.6.6 / 40`，不修改`version.properties`，不升版
- 影响平台：Android／Windows／Linux，共享原生Kotlin／Compose
- 关联需求：用户要求全部实施五项优化，并修复歌词校准被遮挡和无法滚动；前置[一起听移除](2026-11-05-33-remove-listen-together.md)。

## 需求或问题

用户反馈歌词不同步时展开校准，被当前歌词挡住且无法滑动。

## 原因与依据

LyricsContent 把翻译和校准控件直接放入歌词上方的非滚动 Column；小窗口／大字会挤压歌词视口，且歌词容器没有明确裁剪边界。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricsContent / LyricsToolsDialog` | 歌词工具改为独立原生 Dialog，固定标题和关闭入口，85%高度、560dp最大宽度；正文纵向滚动，提前／落后／重置分行显示，保留当前歌词预览；歌词视口 clipToBounds 防止越界绘制。 |



## 修改前后行为

工具由展开在正文中改为独立、有界、可滚动面板。校准仍按±500ms提交原有偏移回调，不改音频与歌词时间轴存储格式。

## 平台与数据影响

保持曲库、收藏、歌单、统计ID、播放任务代次、顺序偏好写入及`.resonance`包格式。原音频不删除、不改标签；旧数据保留。本项不迁移数据格式。两端平台源码均检查并完整编译，Linux本机验证不代表Windows系统验收。无新服务器依赖，无新增付费请求，不读取真实Key或签名私钥。

## 验证

新增 UiVisualContractTest.lyricToolsStayScrollableAndClickableInShortLargeTextWindow；390×360、1.6倍字体实际滚动到校准按钮并点击验证500ms回调，再关闭。新增 LyricsToolsDeviceTest，当前只编译，手机未接入。实际原生面板见[本次截图](../audit/2026-11-05/player-improvements/screenshots/lyrics-tools-short-large.png)。

本次实际命令、最终计数、首轮失败与修正、资源限制和未验证范围集中见[本轮验收](../audit/2026-11-05/player-improvements/VALIDATION.md)。验证入口为本轮忽略目录`build/player-improvements-2026-11-05/final_validation.py`、`test_desktop.py`、`visual_tests.py`与`soak.py`，不是历史测试或旧安装包结果。

## 限制与后续

本轮只交付当前源码与本地验证，未替换用户已安装应用或公开发布；未升版。手机ADB未连接，Android测试仅编译，物理蓝牙／来电／锁屏／省电及Windows设备验收未完成。实际结果以统一验收记录为准。

## 同步更新的文档

`docs/README.md`索引、`docs/DEVELOPMENT.md`相应能力／UI／数据／测试章节、根README；歌词布局同时更新DESIGN.md。历史记录保留原验收事实。
