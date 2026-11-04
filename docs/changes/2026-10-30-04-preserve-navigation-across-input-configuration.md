# 2026-10-30 — 保存导航以应对 MuMu 输入配置重建

- 修改日期：2026-10-30（Asia/Shanghai）
- 类型：Bug 修复，源码已修改，新包待复测
- 适用版本：version.properties 的 `0.6.5 / 39`，未升版
- 平台：共享导航，主要复现于 Android/MuMu
- 关联：[一起听改进](2026-10-30-02-listen-together-session-and-native-playback.md)

## 需求或问题

配置真实 AI 与拍摄正式 App 时，点击助手后很快回到曲库。MuMu ActivityTaskManager 显示约五秒一次的 finger/-touch 切换，Config changes=8。

## 原因与依据

MainActivity 由系统重建，而 App.destination 使用普通 remember，重建默认 Library。隔离拍摄宿主通过不能证明正式 App 也保持导航。按 adaptive 技能状态恢复章节处理，不增加 Manifest configChanges 来隐藏丢失。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | destination | 改为 rememberSaveable 保存目的地枚举 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 输入配置触发重建 | 回到曲库 | 从 SavedState 恢复当前页面；新包实测待执行 |

## 平台与数据影响

只保存目的地枚举，不保存邀请、Key 或模型配置。无新权限、Manifest、音频、偏好格式或版本变化。Windows 无此 Android 重建路径。

## 验证

| 操作 | 环境 | 结果 |
| --- | --- | --- |
| ADB 点击和 ActivityTaskManager 日志 | MuMu 正式 MainActivity | 已复现回首页，确认周期性配置变化和源码普通 remember |
| 新包构建与导航复测 | 当前工作区 | 未执行；正在结束的长构建使用此前源码，需重建 |

## 限制与后续

只修导航目的地，不宣称已审计全部 remember。未保存的编辑草稿需继续检查，敏感字段不得保存到 Bundle。用户已提供 DeepSeek 凭据供真实测试，尚未配置，不在仓库或日志复述。

## 同步更新的文档

文档索引及 DEVELOPMENT 本轮状态说明。
