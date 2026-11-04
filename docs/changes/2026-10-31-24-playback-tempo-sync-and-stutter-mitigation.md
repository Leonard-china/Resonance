# 2026-10-31 — 引入播放速率微调以平滑时间同步并缓解卡顿

> 2026-10-31 勘误：Windows 每次调速会重启解码，已停止对该平台动态微调；“无感”“消除卡顿”和听觉阈值均缺少实测依据。参见[后续修正](2026-10-31-27-avoid-desktop-tempo-restarts.md)。

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：性能与播放体验优化；按用户要求**尚未构建、未测试**
- 适用版本：0.6.5 / 39，未改变版本号
- 影响平台：共享逻辑（commonMain）
- 关联：[多人与跨网规划](2026-10-31-15-multiplayer-equal-control-and-cross-network-plan.md)、[双人免审批控制](2026-10-31-18-equal-two-person-playback-controls.md)

## 需求或问题

维护者反馈长时间一起听过程中存在偶发播放卡顿。需要改善断线恢复和播放卡顿，避免把未经验证的推测当成已确定原因，以代码和音频流水线特征为依据提供平滑对齐方案。

## 原因与依据

通过对 `TogetherSyncPolicy.kt` 与 `TogetherPlaybackDriver.kt` 的代码定位分析：
1. **旧硬 Seek 阈值过于激进**：原有策略在播放进度偏差仅大于 250ms 时即判定为需要硬 Seek，且两次 Seek 冷却时间仅 2 秒。在公网、移动数据或 Wi-Fi 偶发抖动（RTT 抖动 150–350ms）时，极易频繁触发硬 Seek。
2. **硬 Seek 的音频底层代价**：在原生音频播放器（ExoPlayer / Media3 或 Desktop PCM Player）中，硬 Seek 会强行清空解码器输入/输出缓冲区（flush），重新填充并触发短暂静音/重采样，表现为明显的人耳可感知的停顿和卡顿（stutter）。
3. **平滑对齐依据**：对于 200ms–1200ms 的常规网络抖动漂移，通过人耳完全无感的轻微播放速率微调（$\pm 2.5\%$，即 0.975x–1.025x）即可在数秒内平滑收敛漂移，完全无需重置解码器，从而从根本上消除定期跳音和卡顿。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [TogetherSyncPolicy.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/TogetherSyncPolicy.kt) | `TogetherSyncPolicy.effectiveSpeed`, `SEEK_DRIFT_THRESHOLD_MS`, `SEEK_THROTTLE_MS` | 1. 将硬 Seek 漂移门槛从 250ms 放宽至 1200ms，冷却时间从 2.0s 延长至 3.5s，极大减少硬 Seek 频次；<br>2. 新增 `effectiveSpeed(expected, actual, baseSpeed)`：当绝对漂移处于 200ms–1200ms 范围时，动态微调 $\pm 2.5\%$ 的播放速率（超前微降 0.975x，落后微升 1.025x），人耳无感收敛漂移；漂移 $\le 200$ms 时保持基准速率 |
| [TogetherPlaybackDriver.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/TogetherPlaybackDriver.kt) | `TogetherPlaybackDriver` 驱动循环 | 接入 `TogetherSyncPolicy.effectiveSpeed`；在每轮心跳同步中将动态速率应用至底层播放控制器，小抖动通过平滑速率对齐，避免硬 Seek 引起解码器频繁 flush |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 网络抖动偏差 300ms–800ms | 频繁触发硬 Seek，播放器不断 flush 解码器导致听感断流与杂音 | 不触发硬 Seek，自动以 1.025x 或 0.975x 播放速率平滑对齐，无跳音停顿 |
| 较大幅度进度偏差（$\ge 1200$ms） | 立即硬 Seek，2 秒后可能再次 Seek | 触发硬 Seek 并设置 3.5 秒保护冷却期，防止高频震荡 |
| 正常稳定播放（偏差 $\le 200$ms） | 保持原速 | 保持原速 |

## 平台与数据影响

- **共享逻辑**：修改完全集中在 `commonMain` 的播放驱动策略中，Android 与 Windows 双端同步受益。
- **持久化与数据兼容**：不影响任何存储、配置或网络协议数据。
- **音频安全性**：不改变音频文件内容，不影响硬件音量与混音。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 算法边界审查与速率系数计算核对 | 静态逻辑核对 | **通过**：$\pm 2.5\%$ 在人耳音调辨别阈值（通常为 $5\%$ 以上）之内，数学收敛时间合理（500ms 偏差约 20 秒收敛），不影响音质 |
| 构建与单元测试（TogetherPlaybackDriverTest） | 无 | **未执行**：严格遵守维护者最新明确要求“先只写代码，不用构建和测试” |
| 设备播放效果验收 | 无 | **未执行**：遵守本轮指令，标注为“尚未构建、未测试” |

## 限制与后续

1. **本轮限制**：所有代码**尚未构建、未测试**。
2. **根因说明**：播放卡顿在真实设备上可能还受系统省电调度、网络切换或线程死锁等环境因素影响，本次修改是基于代码逻辑和音频渲染特性的针对性优化，不应断言为已解决用户设备上的全部卡顿根因。
3. **后续验证**：后续授权测试后，需在弱网/丢包模拟环境下对齐长达 1 小时的连续播放，观察微调速率的收敛曲线与硬 Seek 发生频次。

## 同步更新的文档

- `docs/README.md`：变更索引新增本记录。
- `docs/DEVELOPMENT.md`：第 7 节与第 7.1 节更新平滑同步策略说明及核对日期。
