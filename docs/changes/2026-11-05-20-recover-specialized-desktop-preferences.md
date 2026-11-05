# 2026-11-05 — 桌面专用偏好入口统一恢复备份

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：Android / Windows / Linux（按下文平台边界）
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q03。

## 需求或问题

坏主配置有有效.bak时，通用读取可恢复，主题/模式/上次歌单/歌词目录等专用入口却失败或丢其他键。

## 原因与依据

专用读写直接Properties.load，绕开AtomicPropertiesFile恢复。全部统一经同一读取器后再按原锁/顺序原子保存。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [platform/DesktopLibraryStore.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopLibraryStore.kt) | load/saveThemeMode、LastPlaylist、AppMode、DeepSeekConfig、CustomLyricsFolder | 统一AtomicPropertiesFile.read并保持其他偏好键。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q03所述输入/操作 | 专用入口抛非法Unicode异常 | 同一有效备份恢复；随后保存保留无关配置 |

## 平台与数据影响

Windows/Linux桌面偏好键和格式不变；不读取或输出真实凭据。Android不改这套文件读取实现。

## 验证

本次相关用例：LibraryPersistenceTest损坏配置/专用读写回归。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 执行入口 | 本次结果 |
| --- | --- |
| JUnitCore或Android instrumentation：LibraryPersistenceTest | 6通过、0失败、0条件跳过；逐用例见验收报告JSON |
| 完整桌面/Android源码与测试编译 | 通过；202/195份主源码，100/34份测试源码，版本不变 |

## 限制与后续

主文件和备份同时损坏仍按既有错误处理；不虚构无法恢复的数据。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
