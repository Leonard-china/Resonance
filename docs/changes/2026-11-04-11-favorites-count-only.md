# 2026-11-04 — 我喜欢的音乐仅显示歌曲数量

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：UI 文案
- 适用版本：0.6.5 / 39；未升版
- 影响平台：Android、Windows、Linux 共享曲库卡片
- 关联需求：用户要求“我喜欢的音乐”旁不写其他文字，只显示歌曲数量。

## 需求或问题

收藏入口副标题原为“X 首 · 每一次心动都算数”，用户只要数量。

## 原因与依据

`PlaylistGrid.kt` 的固定收藏卡片将数量与宣传句拼接；不涉及收藏计数异常。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | 收藏固定入口副标题 | 直接显示 `$favoriteCount 首` |
| [PlayerRecoveryUiTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerRecoveryUiTest.kt) | `favoriteCardOnlyShowsSongCount` | 原生语义确认标题、7 首存在且旧宣传句消失 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 收藏 7 首 | 7 首 · 每一次心动都算数 | 7 首 |

## 平台与数据影响

三平台共享；收藏标志、歌曲计数、歌单和音频均不变。没有新增配置、权限、请求或数据迁移。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| Wrapper `:composeApp:desktopTest`（显式开启公网歌词验收） | Linux / JDK 17 | 通过：346 项，337 通过、9 项集成入口跳过；发生于最终歌词 UI 间距调整之前 |
| 最终 8 类针对性测试 + `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease` | 最后源码，Linux / Android SDK 36 | 通过：33 项全部通过、0 跳过；Debug／测试 APK 成功，Lint 0 Error／107 Warning |
| 收藏 7 首入口语义 | `PlayerRecoveryUiTest.favoriteCardOnlyShowsSongCount` | 通过：标题、7 首存在，旧宣传句消失 |

完整命令、跳过名单、原生截图、最后源码摘要、产物校验与早期失败的处理见[本轮验证报告](../audit/2026-11-04/follow-up/VALIDATION.md)。

## 限制与后续

Android/Windows 未做最终设备视觉验收；本次范围内无已知未完成实现项。

## 同步更新的文档

[文档索引](../README.md)、[开发说明第 11 节](../DEVELOPMENT.md#ui)。
