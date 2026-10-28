# 2026-10-27 — 安装 Media3 音频播放控制与播放统计技能集并规划听歌时长及音效切换

- 修改日期：2026-10-27（Asia/Shanghai）
- 类型：构建配置 / AI 协作工具 / 架构规划
- 适用版本：0.6.2 / 36；不改变版本号
- 影响平台：共享（AI 协作环境）/ Android / Windows
- 关联记录或需求：用户需求为“添加记录听歌时长功能与歌曲详情页切换音效功能，先去 GitHub 寻找相关 skills 再开工”

## 需求或问题

用户需要在当前工作区添加两大核心播放体验功能：
1. **记录听歌时长**（单曲累计听歌时长、播放次数与全库听歌统计）；
2. **歌曲详细页面的音效切换**（在全屏播放/歌曲详情页中支持即时切换原声、人声、低音增强、流行、摇滚、3D环绕、K歌伴奏等音效预设）。

在正式开工编码前，按用户要求先在 GitHub 上检索并安装专业规范的 Agent Skills，为双端音频 DSP 管线及播放时长度量建立标准实践依据。

## 原因与依据

1. **版本与架构对齐**：Resonance 核心音频框架采用 **AndroidX Media3 1.10.1**，并在桌面端通过 `DesktopPcmPlayer` 共享 `PcmGainProcessor`（PCM 16-bit 线性软处理）。
2. **甄选 GitHub 技能**：在 GitHub 开源社区中，[`sunnat629/android-media-pack`](https://github.com/sunnat629/android-media-pack)（Apache-2.0 许可证）专门为 AI 编码 Agent 提供与 Media3 1.10.1 深度绑定的技能体系。从中甄选出两项直接指导本次任务的核心技能：
   - **`media3-analytics-telemetry`**：规范 Media3 播放周期事件监听、`PlaybackStats` 会话度量与播放时长追踪，要求度量逻辑与核心播放容灾解耦，确保听歌时长统计不干扰播放性能。
   - **`media3-audio-playback`**：规范 Media3 音频属性、音频焦点、生命周期及跨平台（KMP）分层，要求平台专用 API 留在 Android 源集，将状态与控制上浮至共享层。
3. **安全与隔离**：使用 `npx skills add` 直接以 `--copy` 方式安全安装到本地 `.agents/skills/` 目录，通过 Socket / Snyk 安全评估（0 告警，Safe），不污染全局环境，保留 Git 忽略约定。

## 具体修改

| 文件 / 目录 | 关键符号 / 配置 | 具体修改与目的 |
| --- | --- | --- |
| [`.agents/skills/media3-analytics-telemetry/`](file:///d:/Project/Resonance/.agents/skills/media3-analytics-telemetry/) | `SKILL.md` | Media3 播放度量与时长统计技能，指导听歌时长采样与持久化。 |
| [`.agents/skills/media3-audio-playback/`](file:///d:/Project/Resonance/.agents/skills/media3-audio-playback/) | `SKILL.md` | Media3 音频属性、焦点控制与音频处理架构边界技能。 |
| [`skills-lock.json`](file:///d:/Project/Resonance/skills-lock.json) | `skills.media3-analytics-telemetry`, `skills.media3-audio-playback` | 记录来源仓库 `sunnat629/android-media-pack`、Git 路径与内容 SHA-256 哈希。 |
| [`docs/README.md`](file:///d:/Project/Resonance/docs/README.md) | 变更索引 | 新增 2026-10-27-07 技能安装与规划记录。 |
| [`docs/DEVELOPMENT.md`](file:///d:/Project/Resonance/docs/DEVELOPMENT.md) | 第 15 节“本地技能与 AI 工具” | 同步录入新增的两项 Media3 专项技能及其在时长与音效任务中的指引。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 播放统计与音频控制技能 | 仅有通用 `media3-cast-integration`，缺少音频管道与播放度量技能 | 引入 `media3-analytics-telemetry` 与 `media3-audio-playback`，具备权威的 Media3 1.10.1 播放度量与音频分层指引 |
| 听歌时长与音效技术方案 | 尚无系统性方案 | 基于两项技能确立了“KMP 统一 PCM 软音效算法（PcmGainProcessor）+ 原子持久化播放时长度量（ListeningStatsStore）”方案 |

## 平台与数据影响

- **代码与架构影响**：本次仅新增本地 Agent Skills 规则文件与锁文件，未修改 Kotlin 业务代码或 Gradle 脚本。
- **配置与数据兼容**：`.agents/` 目录受 `.gitignore` 保护；保留工作区内所有未提交的 UI 与单测改动。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `npx skills add sunnat629/android-media-pack --skill media3-audio-playback media3-analytics-telemetry -a antigravity --copy -y` | Node.js / CLI | 通过。成功下载并复制到 `.agents/skills/`，Socket 安全评估 0 告警，评级 Safe。 |
| `git status --short` | PowerShell | 通过。`skills-lock.json` 已更新，原有未提交修改完整保留。 |
| 技能文件完整性与 YAML frontmatter 校验 | Python 3.13 | 通过。两项技能的 `SKILL.md` 文件完整，属性与关键字解析正常。 |

## 限制与后续

- 下一步将依据这两套技能规范，正式开工实现“听歌时长记录”与“歌曲详情页音效预设切换”。

## 同步更新的文档

- [docs/README.md](file:///d:/Project/Resonance/docs/README.md)（变更索引新增本记录条目）
- [docs/DEVELOPMENT.md](file:///d:/Project/Resonance/docs/DEVELOPMENT.md)（第 15 节技能表同步更新）
