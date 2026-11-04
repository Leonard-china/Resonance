# 2026-11-02 — 补入原生设计 skill 并重定 UI 重构与毛玻璃材质方向

- 修改日期：2026-11-02（Asia/Shanghai）
- 类型：文档 / 协作配置 / 图片方案，未实施产品 UI
- 适用版本：0.6.5（39），未升版
- 影响平台：Android 优先，Windows 兼容边界保留
- 关联：[前一版 UI 规划](2026-11-02-09-android-first-ui-plan.md)

## 需求或问题

用户否定厚封面、发光与凸起按钮堆叠的效果稿，授权放开原页面结构重新设计，并要求先给图片方案。随后明确希望采用 macOS 的毛玻璃质感。

## 原因与依据

上一张效果稿把立体感集中在物体厚度和控件装饰上，未满足用户对自然设计感的要求。旧布局、颜色与四项导航是候选，不应继续作为新设计的硬约束。音乐内容、功能、Android 原生规范与真实数据边界继续有效。

已检索 GitHub 上的 [Impeccable](https://github.com/pbakaus/impeccable)、[Emil skills](https://github.com/emilkowalski/skills) 和 [Taste Skill](https://github.com/Leonxlnx/taste-skill)。选择补入包含 Android 原生指引的 Impeccable；现有 Emil skills 保留，Taste Skill 仅调研，不自动安装全部工具或改用 Web 技术栈。

## 具体修改

| 文件 | 关键内容 | 本次实际修改 |
| --- | --- | --- |
| [PRODUCT.md](../../PRODUCT.md) | 产品事实、平台、品牌约束 | 汇总已有用户要求和仓库事实；记录允许重构、拒绝厚重拟物以及最新毛玻璃偏好；未把任何候选图记录为已获选 |
| [skills-lock.json](../../skills-lock.json) | `skills.impeccable` | 固定来源提交、上游路径与本次安装文件哈希 |
| 忽略目录 `.agents/skills/impeccable/` | `SKILL.md`、原生平台资料 | 经系统 skill-installer 安装 Impeccable 4.8.0，来源提交 `e89e3ada3e875a89ac2c5d33d11e65ebef6fe285`；未启用自动 hook |
| [DEVELOPMENT.md](../DEVELOPMENT.md) | 本地 UI 设计技能 | 补充 skill 与适用边界，核对日期更新为 2026-11-02 |
| [README.md](../README.md) | 当前方案入口与变更索引 | 增加本记录，前一方案保留作历史候选 |

本轮制作的会话图片包含：A 共听空间、B 唱片翻阅、C 文字舞台，每组两页；D 同一播放器的浅/深毛玻璃材质对照。图片由内置 ImageGen 生成，使用虚构音乐与聊天内容，已逐张查看。原图及提示词保存在本聊天的可视化工作区，未把生成图片或本机路径纳入播放器提交资源。

## 修改前后行为

| 场景 | 修改前 | 本次修改后 |
| --- | --- | --- |
| 设计约束 | 沿用原壳结构与中性色，立体化尝试偏厚重拟物 | 允许结构重构，明确 macOS 磨砂玻璃材质偏好；页面方案尚待选择 |
| 协作依据 | 原生设计与动效 skills 已有，缺少本轮的集中产品约束 | 新增产品事实文档与固定版本 Impeccable |
| 应用运行 | 当前 0.6.5 实现 | 未修改；图片不代表代码已实现 |

## 平台与数据影响

仅文档、skill 锁定信息及方案图片。未改变 Kotlin 代码、音频文件、播放协议、权限、存储格式、凭据或依赖版本；官网独立。已有未提交改动保留。毛玻璃需要后续在 Compose 中验证可读性、性能和降低透明度/动态效果时的处理。

## 验证

| 命令或操作 | 结果 |
| --- | --- |
| `install-skill-from-github.py --repo pbakaus/impeccable --ref e89e3ada3e875a89ac2c5d33d11e65ebef6fe285 --path .agents/skills/impeccable --dest .agents/skills`（实际使用绝对目标路径） | 通过，skill 已安装；后续轮次可发现，本轮已手动读取 |
| Impeccable `context --target composeApp/src/commonMain/kotlin/com/resonance/player/ui` | 命令通过；未能识别缺少 PRODUCT 的原生上下文，随后按已知事实新增产品记录并读取 Android 指引 |
| Impeccable `concept-seed --scope direction --mode operate` | 首次因缺少 PRODUCT 被拒绝；新增记录后通过。候选未选择，不视为实现或视觉验收 |
| 外部风格参考图读取 | 两个 Impeccable 参考图片链接经网页工具读取失败；未声称看过 |
| Python 图片解码、skill SHA-256 对照、本地文档链接检查 | 通过；实际图片已生成并查看，哈希与锁文件一致，5 个本地链接存在、索引唯一 |
| `git diff --check` | 默认检查因 DEVELOPMENT 的既有 CRLF 行末报错；保留原行尾，使用 `core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol` 重查通过 |
| Kotlin 构建、Android 设备交互、TalkBack、模糊性能与功耗 | 未执行；本次只提供图片方案 |

## 限制与后续

本轮没有实现动画或实际背景采样模糊。设计图中的中文、触控尺寸、不同封面对比度和大字号布局仍需原生实现验证。下一轮以用户选定图片或修改意见继续，不把三种探索或最新材质稿自动提升为生产设计系统。

## 同步更新的文档

`PRODUCT.md`、`docs/README.md`、`docs/DEVELOPMENT.md`；`docs/AI-WORKFLOW.md` 的既有规则未改变。
