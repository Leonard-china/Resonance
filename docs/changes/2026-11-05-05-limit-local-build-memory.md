# 2026-11-05 — 约束本机构建总内存并串行验证

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：协作规则 / 验证方式调整
- 适用版本：`0.6.6 / 40`，不改变版本号
- 影响平台：当前共享 Linux 桌面的开发验证
- 关联需求：用户指出 AI 多次占满内存导致崩溃；发生于[歌单包导入修复](2026-11-05-04-linux-resonance-package-import.md)验证期间。

## 需求或问题

首次桌面编译、后续 Android 编译与 Linux Release 打包过程中均遭中断。内核日志确认 Java 被 OOM killer 终止，另有 ChatGPT 被内存耗尽终止的记录。用户桌面受到实际影响，不能继续按原内存配置重跑。

## 原因与依据

本机约 16 GiB RAM，约 512 MiB swap 已满。仓库默认 Gradle `-Xmx4g`、Kotlin daemon `-Xmx3g` 只是各 JVM 的堆上限，另有元空间、原生内存、ProGuard 与打包进程。将 Android 编译与 Linux Release 打包放在同一轮、没有限制整个进程树，允许聚合内存超出当前桌面预算。日志证明「构建期间全机 OOM」，不能将各 Xmx 相加当作测得峰值，也不声称被杀进程都是唯一原因。

## 具体修改

| 文件或执行入口 | 关键规则/参数 | 具体修改与目的 |
| --- | --- | --- |
| [仓库规则](../../AGENTS.md) | 本机构建资源 | 增加先检查内存、重量级验证串行、限制整个进程树和禁止自动放宽限额。 |
| [协作流程](../AI-WORKFLOW.md#本机资源预算) | 本机资源预算 | 记录 systemd 总限额、单 worker/短生命周期/进程内编译、核验方式及失败处理，供后续 AI 接手。 |
| 本次 Linux 构建 | `MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0` | 独立 systemd 用户服务限制父子进程；单 worker、`--no-daemon`、Kotlin `in-process`，命令覆盖堆 768 MiB/元空间 384 MiB。 |
| 本次验证范围 | `:composeApp:createDistributable` | 使用本次已通过回归的桌面源码生成普通完整 app image，不重复 Android/ProGuard 重任务。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 构建资源预算 | 各 JVM 独立大堆，聚合无上限 | 当前本机整个构建进程树默认最多 2 GiB，不使用 swap |
| 多目标验证 | Android 与 Linux Release 放在同一轮 | 重量级任务串行，先做本需求相关验证 |
| 超预算或中断 | 重跑可再次触发全机 OOM | 留日志、记录受阻并缩小范围，不自动提高预算 |

## 平台与数据影响

只约束开发验证，不更改产品接口、曲库、音乐、权限、版本号、包格式或凭据。不改变 `gradle.properties`、用户全局 Gradle、系统内存/swap 配置，不关闭用户其他应用。不在仓库提交本机 JDK/SDK 路径；实际命令与诊断保存在忽略的构建目录。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 内核 OOM 日志核对 | 当前 Linux | **确认失败原因**：Java 与 ChatGPT 被 OOM killer 终止；`build/resonance-package-import-2026-11-05/oom-evidence.log`。 |
| 限额用户服务运行 `/bin/true` | 当前 systemd 用户会话 | **通过**，支持建立服务及设置内存预算。 |
| `systemctl --user show … -p MemoryMax -p MemoryHigh -p MemorySwapMax` | 实际 Linux 构建服务 | **通过**，2147483648 / 1610612736 / 0 字节。 |
| 限额服务内 `:composeApp:createDistributable --max-workers=1 --no-daemon --offline` | JDK 17 | **通过**，31 秒，服务总峰值 **1.5 GiB**；`safe-linux-build.log`、`resource-limited-verification.log` 见同目录。 |
| 隔离完整 app image 的歌单包原生事件导入 | 独立 1 GiB 限额服务 / JVM 384 MiB | **通过**，总峰值 **280.6 MiB**；详情见歌单包修复记录。 |
| 规则与文档链接、CRLF 兼容 diff 检查 | 当前仓库 | **通过**；未为纯协作规则重复执行 Kotlin 回归。 |

## 限制与后续

2 GiB 是当前本机默认预算，不保证所有完整编译或 ProGuard 都能完成；超出时按环境受阻处理。原 Android 编译和 Release 便携包没有通过，不能用普通 app image 结果替代。用户明确调整预算或换用资源充足环境后，才安排相应重任务。

## 同步更新的文档

`AGENTS.md`、`docs/AI-WORKFLOW.md`、`docs/README.md` 索引及 `docs/DEVELOPMENT.md` 构建说明。
