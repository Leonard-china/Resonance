# 2026-11-05 — 音频设备失败后保留进度重试与后台连续验收

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：功能／Bug修复／性能与验证
- 适用版本：`0.6.6 / 40`，不修改`version.properties`，不升版
- 影响平台：Android／Windows／Linux，共享原生Kotlin／Compose
- 关联需求：用户要求全部实施五项优化，并修复歌词校准被遮挡和无法滚动；前置[一起听移除](2026-11-05-33-remove-listen-together.md)。

## 需求或问题

补齐日常独立播放中的暂停／恢复、音频输出失败和后台持续播放验证。

## 原因与依据

DesktopPcmPlayer.setPlaying在旧会话已经关闭时直接play(track)，使音频设备异常后的用户重试从零开始；暂停等待仍定时唤醒。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [DesktopPcmPlayer.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) | `setPlaying` / `awaitPlaying` | 设备失败后按原进度准备解码；暂停等待显式命令通知。 |


desktopMain/audio/DesktopPcmPlayer.kt：setPlaying、awaitPlaying；desktopTest的DesktopPcmPlayerSessionTest、DesktopAudioOutputIntegrationTest。

## 修改前后行为

桌面用户重试失败会话时从保留的progress重新准备解码；已到99%及以上的结束曲目仍从头播放。不自动抢回音频或创建无界重试。Android继续使用Media3原有音频焦点与耳机拔出处理。

## 平台与数据影响

保持曲库、收藏、歌单、统计ID、播放任务代次、顺序偏好写入及`.resonance`包格式。原音频不删除、不改标签；旧数据保留。本项不迁移数据格式。两端平台源码均检查并完整编译，Linux本机验证不代表Windows系统验收。无新服务器依赖，无新增付费请求，不读取真实Key或签名私钥。

## 验证

新增DesktopPcmPlayerSessionTest重试进度回归；既有暂停、部分写入、无进展退让、旧任务隔离继续运行。新增DesktopAudioOutputIntegrationTest可选60—7200秒后台soak，本轮显式300秒，静音原创合成音频，无Compose，循环暂停／seek／恢复／重开并校验统计；结果见统一验收。没有连接手机，Windows实机、蓝牙物理拔插、来电与系统休眠唤醒及两小时验收未执行。

本次实际命令、最终计数、首轮失败与修正、资源限制和未验证范围集中见[本轮验收](../audit/2026-11-05/player-improvements/VALIDATION.md)。验证入口为本轮忽略目录`build/player-improvements-2026-11-05/final_validation.py`、`test_desktop.py`、`visual_tests.py`与`soak.py`，不是历史测试或旧安装包结果。

## 限制与后续

本轮只交付当前源码与本地验证，未替换用户已安装应用或公开发布；未升版。手机ADB未连接，Android测试仅编译，物理蓝牙／来电／锁屏／省电及Windows设备验收未完成。实际结果以统一验收记录为准。

## 同步更新的文档

`docs/README.md`索引、`docs/DEVELOPMENT.md`相应能力／UI／数据／测试章节、根README；歌词布局同时更新DESIGN.md。历史记录保留原验收事实。
