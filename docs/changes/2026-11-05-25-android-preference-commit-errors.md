# 2026-11-05 — Android同步偏好写失败向调用方报告

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q10。

## 需求或问题

主题/外观/天气/音量等commit返回false被忽略，UI可能显示保存完成但重启丢值。

## 原因与依据

持久入口契约不一致。新增commitChecked检查同步写结果，保留原顺序与调用方错误反馈，不改为异步apply。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [platform/PreferenceCommit.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/PreferenceCommit.kt) | commitChecked / withPreferenceRollback | false抛明确错误；导入异常尝试按类型恢复多个偏好，恢复失败附带原异常。 |
| [platform/AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | 偏好保存入口 | 替换忽略结果的commit；保留原来已检查的写入。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q10所述输入/操作 | false仍正常返回 | false抛错交给现有任务/持久队列反馈；导入偏好失败尝试恢复 |

## 平台与数据影响

Android专用，无偏好键/格式变更；Windows/Linux不依赖SharedPreferences。

## 验证

本次相关用例：PreferenceCommitDeviceTest注入false、真实隔离偏好多存储/类型回退；Android完整编译。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：PreferenceCommitDeviceTest | 2通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

未填满真实手机存储或损坏正式设置；false为可控替身，回退测试使用测试包生成偏好。硬件持续写失败不能保证恢复成功。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
