# 2026-10-31 — 避免 Windows 跟听微调速度反复重启解码

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：播放同步修复；版本 0.6.5 / 39，不升版
- 影响平台：共享播放驱动、Android、Windows
- 关联：[速率微调初始实现](2026-10-31-24-playback-tempo-sync-and-stutter-mitigation.md)

## 需求或问题

用户反馈跟听偶发卡顿。未经验证的 ±2.5% 微调被两端共用，但两端更改速度的代价不同。

## 原因与依据

`DesktopPcmPlayer.setPlaybackSpeed` 对每次变化重新调用 `play`，会重启原生解码播放会话，不能假设速率变化平滑或没有缓冲中断；Android ExoPlayer 通过 `setPlaybackSpeed` 更新参数。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [TogetherPlaybackDriver.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/TogetherPlaybackDriver.kt) | `smoothSpeedChanges` | 新增默认关闭的能力标志，未声明平滑变速的平台只跟随远端基准速度，不执行动态微调 |
| [AndroidTogetherRuntime.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/together/AndroidTogetherRuntime.kt) | `TogetherPlaybackDriver` | Android 显式启用现有微调；不保证听感或续航 |

## 修改前后行为

Windows 不再因跟听误差在基准速率与微调速率之间频繁切换；用户主动改变基准速度仍按现有播放器实现处理。Android 保留未验收的微调逻辑。两端仍使用当前硬跳转门槛，不能由此断言原有卡顿已解决。

## 平台与数据影响

不修改音频文件、持久化格式或权限。默认关闭保护没有声明能力的其他调用者。

## 验证

静态阅读两端播放调用链；按用户要求未构建、未运行测试或实际听感验收。原记录声称双端“人耳无感”“消除卡顿”缺乏实测依据，应以本记录为准。

## 限制与后续

需真实设备测量漂移、寻址、弱网与平台变速成本；Android 微调效果与音质未经验证。

## 同步更新的文档

`docs/README.md` 与 `docs/DEVELOPMENT.md`。
