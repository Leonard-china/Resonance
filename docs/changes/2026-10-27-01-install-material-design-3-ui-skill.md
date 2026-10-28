# 2026-10-27 — 为当前项目安装 Material Design 3 UI 设计技能

- 修改日期：2026-10-27（Asia/Shanghai）
- 类型：配置 / AI 协作工具 / 文档
- 适用版本：0.6.0 / 34；不改变版本号
- 影响平台：本地 AI 协作环境；为 Android 与 Windows 原生 UI 开发提供设计参考
- 关联记录或需求：用户在候选技能比对后要求“新增第一个”；此前安装见 [Google Android Skills 与 Frontend Design](2026-10-26-02-install-android-and-frontend-design-skills.md)

## 需求或问题

为 Resonance 新增社区项目 `skydashnet/material-design-3-ui-skill`，补充 Material Design 3 的语义配色、排版、信息层级、组件选择、交互状态、自适应及 M3 Expressive 设计指引。安装前当前工作区不存在 `.agents/skills/material-design-3-ui/`。

本次范围是安装技能和记录配置，不包含播放器界面重构。

## 原因与依据

- [上游仓库](https://github.com/skydashnet/material-design-3-ui-skill) 提供 `material-design-3-ui` 技能；入口元数据版本为 **1.1.1**，许可证为 MIT。这是社区维护项目，并非 Google 官方技能。
- `git ls-remote` 本次解析到 `main` 提交 `82783f76de4a5b08a60421cae935cf8de7ca1ee8`；下载使用该提交，避免安装过程中分支更新造成来源不确定。
- 上游 `SKILL.md` 将设计拆为任务、层级、布局、语义主题、组件、状态、动效和无障碍流程，并按需引用 13 份资料。上游根目录提供包清单及静态校验脚本。
- 使用 `skill-installer` 自带的 `install-skill-from-github.py` 下载并复制文件，显式指定项目目标目录；未执行上游会写入全局技能目录的安装脚本。

## 具体修改

| 文件 | 关键符号 / 配置 | 具体修改与目的 |
| --- | --- | --- |
| [技能入口](../../.agents/skills/material-design-3-ui/SKILL.md) | `name: material-design-3-ui`、`metadata.version: 1.1.1` | 新增完整上游副本，保留入口、13 份参考资料、包清单、许可证及静态验证材料；不改写上游规则 |
| [技能来源记录](../../skills-lock.json) | `skills.material-design-3-ui` | 在原有 16 项之外新增来源、`sourceType`、固定 `ref`、`skillPath` 与 `computedHash`；保留所有旧条目 |
| [开发说明](../DEVELOPMENT.md) | 第 15 节“本地 UI 设计技能” | 说明当前安装、调用方式、原生双端边界及忽略目录的复现限制 |
| [文档入口](../README.md) | 变更索引 | 新增本记录，保留原有未提交的索引内容 |

`computedHash` 使用本机已有 `skills` CLI 的目录哈希算法：递归收集文件，排除 `.git` / `node_modules` 目录，按相对路径 `localeCompare` 排序，依次向 SHA-256 输入正斜杠路径和原始文件字节。计算覆盖实际安装的整个目录，而不仅是 `SKILL.md`。

本次目录哈希为 `34f1e4fa16efeacd5176dd3425090e0702876b2754ff44a083f356dc7603362f`。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 显式请求 `material-design-3-ui` | 当前项目未安装该技能 | 下一轮请求可通过 `$material-design-3-ui` 使用本地入口及参考资料 |
| 查询安装来源 | 锁文件中无该条目 | 可核对上游仓库、具体提交和安装内容哈希 |
| 播放器现有界面 | 现有 0.6.0 UI | 本次没有修改页面、运行逻辑或依赖 |

## 平台与数据影响

- 安装只落在当前项目 `.agents/skills/`，并更新项目锁文件和文档；没有改动全局技能配置、注册表或环境变量。
- `.agents/` 继续被 Git 忽略；没有强制将第三方技能源码加入版本控制。
- 未修改共享 Kotlin、Android / Windows 平台接口、Gradle、应用版本、偏好、曲库、同步包、音频文件或标签。
- 网络访问仅用于 GitHub 公共技能仓库；不需要用户 API Key 或签名凭据。
- 保留开始时已有的桌面测试改动、三份历史未跟踪记录及旧技能内容。

## 验证

下表中的安装命令以脚本名和项目相对目标表示，省略本机工具的绝对路径。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `git status --short`、`Test-Path .agents/skills/material-design-3-ui` | Windows PowerShell | 通过。识别已有未提交改动；安装前目标不存在 |
| `git ls-remote https://github.com/skydashnet/material-design-3-ui-skill.git refs/heads/main` | Git / GitHub | 通过。解析到上述固定提交 |
| `python install-skill-from-github.py --repo skydashnet/material-design-3-ui-skill --ref 82783f76de4a5b08a60421cae935cf8de7ca1ee8 --path . --name material-design-3-ui --dest .agents/skills --method download` | `skill-installer` / Python | 通过。安装到本项目 `.agents/skills/material-design-3-ui/` |
| `python .agents/skills/material-design-3-ui/tests/validate_skill.py` | Python 3.13 / Windows | 通过。入口 241 行、13 份参考资料、14 个核心包文件、6 份行为评估案例存在；此命令是静态检查，不执行案例的模型评估 |
| 固定提交归档与安装目录逐文件比对、锁文件哈希与旧条目检查 | Python / Node.js | 通过。37 个安装文件与固定提交原始字节一致；目录哈希匹配锁文件；原有 16 项条目保留 |
| `python build/material-design-3-ui-install-2026-10-27/verify-install.py`、`git diff --check` 及新增文件空白检查 | Python / Git | 通过。908 个既有文件哈希不变；新记录的本地链接、索引和开发说明入口有效，原有索引内容保留；无空白错误。审计脚本与结果仅位于忽略的 `build/` |
| Android / Windows 构建、Kotlin 回归、实际 UI 效果评估 | 应用环境 | 未执行。本次没有应用代码变更，也未进行界面设计 |

## 限制与后续

- 技能目录是本机忽略文件；其他机器或新检出仓库需要按固定提交安装。锁文件不自动下载技能。
- 静态验证确认包结构和核心规则完整，不代表界面美化效果已验证；后续 UI 任务仍需进行截图、交互与平台验证。
- 本次范围内无已知未完成项；当前页面外观保持原样。

## 同步更新的文档

- [文档入口](../README.md)：新增 2026-10-27 变更索引。
- [开发说明](../DEVELOPMENT.md)：第 15 节新增本地 UI 设计技能状态及核对日期。
- AI 协作流程未改变，无需调整 `AI-WORKFLOW.md`。
