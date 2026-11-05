# 2026-11-05 — 抑制助手重复请求并修复跨搜索下载和音频超时

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复；版本 0.6.6 / 40，不升版
- 平台：共享 JVM，Android、Windows、Linux
- 需求：账单 1,004 次请求／97,002,259 tokens，聊天 connection closed、下载失败；Key 仅用于播放器。

## 需求或问题

简单推荐可能重复调用模型，下载全失败的自动推荐下一分钟又付费；已搜索歌曲仍可能报 ID 无效，较慢歌曲会被总超时截断。截图不能重建历史各次输入／缓存／输出比例，不把全部账单归因到一个缺陷。

## 原因与依据

推荐只检查成功批次时间；工具循环无无进展退出、历史无限累积、硬截 JSON；每次源搜索清空前次 ID；元数据和音频共用 40 秒总期限。官方旧 DeepSeek 模型已退役，依据[官方公告](https://api-docs.deepseek.com/news/news260424/)。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `recommendInternal`、`recommendationAttemptDue` | 请求前持久记录尝试时间，失败按配置间隔冷却，手动刷新立即重试。旧启用配置升级从现在进入冷却。 |
| [AssistantToolSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/AssistantToolSession.kt) | `execute`、`record`、`compact` | 同任务规范化参数去重；最近四轮完整调用组约 24K 字符及完成记录；六轮无新成功操作本地暂停；结果保持完整 JSON。持续成功任务无总轮数上限。 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `chat`、`execute`、`ask` | 历史／库摘要有界；歌单元数据和成员分页；搜索 ID 合并至任务结束；具体源错误回传；新增按操作的尝试及已返回用量诊断。 |
| [MusicAiClient.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/ai/MusicAiClient.kt) | `complete`、`effectiveConnection` | 定长体禁止连接故障时隐式 POST 重放；提示未重发／用量未知；官方旧别名按原思考语义映射 Flash，第三方地址不变，回复标签显示实际模型。 |
| [SourceNetwork.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/SourceNetwork.kt) | `PublicSourceNetwork.download` | 音频总期限十分钟、读取停滞 20 秒；元数据总期限仍 40 秒；保留取消、公网 DNS／重定向、大小与完整性校验。 |

## 修改前后行为

自动推荐全失败从“每分钟再次请求”改为“按设置间隔冷却”；重复失败工具从“无限累积”改为“复用结果并在无进展时停”；搜索 A、B 后下载 A 保留有效 ID；持续超过 40 秒的下载可继续完成。

## 平台与数据影响

新增根字段 `recommendationLastAttemptAt`、月度 `usage.<month>.byOperation`（操作尝试、未返回用量的失败、输入／缓存／输出及最大消息字符数）。字符数不是 token 估算；旧月度合计兼容。既有取消预算上限／16 步上限需求保留。不改同步包、音频转换、原子持久化、文件删除或权限；未读取真实 Key，未调用付费模型。

## 验证

| 操作 | 环境及结果 |
| --- | --- |
| 完整桌面源码编译 | 200 份源码通过。第一次因另一对话仍修改 PlayerExpansion 而被 SHA 守卫拦下，未安装；稳定后重新完整编译通过。 |
| JSON 平台兼容修正 | Android 不公开 JSONObject.valueToString，首轮 Android 编译明确失败；改用公开 JSONArray 序列化。桌面只重编译该纯类源文件的四个类，保留完整模块映射并校验其余 199 源哈希未变；没有混入旧接口。 |
| 完整 Android 源码编译 | 修正后 191 份 Release 源码通过；不是 APK 构建／安装通过。 |
| 最终定向回归 | `test_changes.py --compile-only` 九份测试源码通过；`run_isolated_tests.py` 69 项通过、0 失败／跳过，覆盖自动失败冷却与重启、旧配置迁移、重复工具／长任务／有效 JSON、跨搜索下载 ID、两条来源失败汇总、超时与公网地址边界、歌词高亮与窗口生命周期。全部假 AI／假媒体／回环服务。 |
| 原生补测 | 隔离 X11 软件渲染会话，两倍缩放三项全通过；上一安装实现的新增拖动／解锁用例预期失败，新实现通过。 |
| 打包与安装 | 用户要求保护内存后停止重型验证。768 MiB 桌面打包尝试被“上限＋1.5 GiB 余量”门槛拦下，60 秒内没有启动 Gradle。没有生成本轮 APK，也没有覆盖任何现有安装。 |

源码编译总进程树上限 1.5 GiB、Swap0，峰值约 1.1 GiB；用户提出内存余量要求后，后续轻量原生补测使用 512 MiB 上限，峰值 285.2 MiB。日志位于本轮忽略目录 `build/assistant-cost-and-lyrics-drag-2026-11-05/`。

## 限制与后续

历史没有逐请求遥测，无法精确分摊 9700 万 tokens。未做付费模型或真实公网音频验收；服务拒绝、网络和区域限制仍会真实失败。当前改动尚未安装。手机和电脑保留上一轮已验收的歌词修复版。重型 APK 进程树上限 1.5 GiB，须至少 3 GiB 可用内存；当前不足，按用户要求受阻，不自动提高限额或关闭应用。后续入口：`package_all.py`、`verify_ready_apk.py`、`verify_install_phone.py`、`install_desktop.py`；每次先复核源码哈希和内存门槛，安装前保留原包。

## 同步更新的文档

`docs/README.md` 索引；`docs/DEVELOPMENT.md` 第 13 节及官方接口说明。

后续 Android 安装：内存余量满足门槛、手机重新连接后，已将包含本轮助手/下载修复及播放页动效的完整正式包保留数据覆盖手机，实际安装字节与新包一致，见[手机安装记录](2026-11-05-15-player-animation-phone-install.md)。本记录中“未安装/打包受阻”保留为当轮历史；桌面本轮仍未覆盖，不把手机结果当成付费接口或真实下载验收。
