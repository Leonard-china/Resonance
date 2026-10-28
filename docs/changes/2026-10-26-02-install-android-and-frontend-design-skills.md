# 2026-10-26 — 安装与配置 Google Android Skills 及 Anthropic Frontend Design 技能集

- 修改日期：2026-10-26（Asia/Shanghai）
- 类型：构建配置 / AI 协作工具
- 适用版本：0.6.0 / 34；不改变版本号
- 影响平台：共享（AI 协作环境）/ Android / Windows
- 关联记录或需求：用户需求为 Android 听歌软件配置 Google Android Skills 与 Anthropic Frontend Design

## 需求或问题

用户需要在当前工作区中为 Android 听歌播放器（Resonance）安装并配置以下两套 Agent Skills：
1. **Google Android Skills**（官方仓库：`https://github.com/android/skills`）
2. **Anthropic Frontend Design**（官方仓库：`https://github.com/anthropics/skills/tree/main/skills/frontend-design`）

安装与安全边界要求：
- 仅安装到当前项目的 `.agents/skills/` 目录，限制在工作区作用域，不污染全局配置或注册表环境变量。
- Google Android Skills 优先甄选与 Kotlin、Jetpack Compose、Android 原生开发强相关的技能集合，排除不相关的车载驻车视频、XR智能眼镜、Wear智能手表等技能。
- Frontend Design 仅安装单项目标技能，不安装无关的技能集合；且必须适配 Android 原生环境，明确将 Web 设计原则转换为 Jetpack Compose 实现，禁止生成 HTML、CSS 或 React 代码。
- 保留工作区现有目录（`.agents/skills/emilkowalski-skills`、`ui-ux-pro-max-skill`），不覆盖用户已有文件。
- 不修改 Gradle 脚本、Kotlin 源码或项目架构。

## 原因与依据

1. **Antigravity 技能发现机制**：根据 Antigravity 自定义系统规范，工作区级技能放置在 `.agents/skills/<skill-name>/SKILL.md`，每个技能具备以 YAML frontmatter（`name` 与 `description`）开头的 Markdown 入口文件，可包含 `references/` 等支持资料，由 Agent 渐进式加载（Progressive Disclosure）。
2. **安全与全局配置隔离**：官方 `dl.google.com` 提供的 `install.cmd` 会尝试向 Windows 注册表 `HKCU\Environment\Path` 写入全局路径，并下载全局可执行文件，属于高风险全局污染操作。通过 Agent Skills 生态标准 CLI `skills`（Vercel Labs，已原生适配 Antigravity 检测），可以以 `--agent antigravity --copy` 模式直接将 GitHub 官方仓库的目标技能安全提取并复制到本地工作区的 `.agents/skills/` 目录，免去提权和注册表变更。
3. **针对音乐软件的技能筛选**：Google Android Skills 仓库共有 33 项技能，其中针对本播放器架构（Kotlin Multiplatform + Jetpack Compose + Media3）甄选了 15 项最相关的核心原生开发技能，过滤掉了与本项目无关的硬件/平台技能。

## 具体修改

| 文件 / 目录 | 关键符号 / 结构 | 具体修改与目的 |
| --- | --- | --- |
| [.agents/skills/frontend-design/](file:///d:/Project/Resonance/.agents/skills/frontend-design/) | `frontend-design/SKILL.md` | 从 Anthropic 官方仓库安装前端设计技能，并在 `SKILL.md` 中增加 `Android & Jetpack Compose Native Mandate` 规范，指引 AI 将调色板、字体排印、布局线框等概念落地为 Compose 原生代码，严禁输出 HTML/CSS。 |
| [.agents/skills/jetpack-compose-performance/](file:///d:/Project/Resonance/.agents/skills/jetpack-compose-performance/) | `SKILL.md`, `references/` | Google 官方 Jetpack Compose 性能优化与重组审计技能。 |
| [.agents/skills/adaptive/](file:///d:/Project/Resonance/.agents/skills/adaptive/) | `SKILL.md`, `references/` | Google 官方自适应与大屏/折叠屏布局适配技能。 |
| [.agents/skills/styles/](file:///d:/Project/Resonance/.agents/skills/styles/) | `SKILL.md`, `references/` | Google 官方 Material 3 主题、样式与调色板规范技能。 |
| [.agents/skills/edge-to-edge/](file:///d:/Project/Resonance/.agents/skills/edge-to-edge/) | `SKILL.md`, `references/` | Google 官方沉浸式 Edge-to-Edge 边到边与软键盘/系统栏避让技能。 |
| [.agents/skills/navigation-3/](file:///d:/Project/Resonance/.agents/skills/navigation-3/) | `SKILL.md`, `references/` | Google 官方 Navigation 3 架构与路由流转技能。 |
| [.agents/skills/navigation-event/](file:///d:/Project/Resonance/.agents/skills/navigation-event/) | `SKILL.md`, `references/` | Google 官方 Compose 导航事件与状态处理技能。 |
| [.agents/skills/media3-cast-integration/](file:///d:/Project/Resonance/.agents/skills/media3-cast-integration/) | `SKILL.md`, `references/` | Google 官方 Media3 媒体播放与 Cast 投屏集成技能。 |
| [.agents/skills/glance-canonical-layouts/](file:///d:/Project/Resonance/.agents/skills/glance-canonical-layouts/) | `SKILL.md`, `references/` | Google 官方 Jetpack Glance 桌面小组件微件规范技能。 |
| [.agents/skills/anr-audit-and-fix/](file:///d:/Project/Resonance/.agents/skills/anr-audit-and-fix/) | `SKILL.md`, `references/` | Google 官方主线程卡顿排查与 ANR 审计修复技能。 |
| [.agents/skills/r8-analyzer/](file:///d:/Project/Resonance/.agents/skills/r8-analyzer/) | `SKILL.md`, `references/` | Google 官方 R8 混淆配置、Keep 规则与 APK 体积优化技能。 |
| [.agents/skills/android-profiler/](file:///d:/Project/Resonance/.agents/skills/android-profiler/) | `SKILL.md`, `references/` | Google 官方 Profiler 内存/CPU/渲染性能剖析技能。 |
| [.agents/skills/android-permissions-security/](file:///d:/Project/Resonance/.agents/skills/android-permissions-security/) | `SKILL.md`, `references/` | Google 官方 Android 运行时权限与 IPC 安全审计技能。 |
| [.agents/skills/android-intent-security/](file:///d:/Project/Resonance/.agents/skills/android-intent-security/) | `SKILL.md`, `references/` | Google 官方 Intent 与四大组件安全防护技能。 |
| [.agents/skills/testing-setup/](file:///d:/Project/Resonance/.agents/skills/testing-setup/) | `SKILL.md`, `references/` | Google 官方 Android 单元测试与 Compose UI 自动化测试技能。 |
| [.agents/skills/migrate-xml-views-to-jetpack-compose/](file:///d:/Project/Resonance/.agents/skills/migrate-xml-views-to-jetpack-compose/) | `SKILL.md`, `references/` | Google 官方 XML 视图转 Compose 迁移技能。 |
| [skills-lock.json](file:///d:/Project/Resonance/skills-lock.json) | 元数据记录 | 记录所有已安装技能的版本哈希、Git 来源与路径映射。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本地技能库配置 | `.agents/skills` 仅包含历史用户技能（`emilkowalski-skills`、`ui-ux-pro-max-skill`） | 新增 Anthropic `frontend-design` 以及 15 项 Google Android 官方核心技能，且完整保留已有技能 |
| Compose / UI 设计请求 | AI 容易产出平庸套路化 UI 或误用 Web 前端 CSS 代码 | `frontend-design` 强制要求在 Android 原生下采用 Jetpack Compose 实现，并结合 `styles`、`adaptive`、`edge-to-edge` 指引高质量设计 |
| Android 原生特性开发 | 缺少 Google 官方最佳实践参考，容易出现重组性能退化、系统栏遮挡、ANR 等问题 | AI 自动按需激活性能审计、边到边、Media3、权限管理等官方技能，提供权威指引 |

## 平台与数据影响

- **代码与架构影响**：0。没有修改任何 Gradle 构建脚本、Kotlin 源代码、依赖配置或平台接口。
- **配置与数据兼容**：安装仅局限在 `.agents/skills/` 和本地 `skills-lock.json`。由于 `.agents/` 已经被列入项目的 `.gitignore`，不会对版本控制基线造成任何意外改动。
- **现有文件安全**：保留原有 `emilkowalski-skills` 与 `ui-ux-pro-max-skill`，无任何文件覆盖。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 现有目录与 git 状态检查 | Windows PowerShell (`git status`) | 通过。工作区初始状态 clean，已有目录完整无损坏。 |
| Anthropic `frontend-design` 精确安装 | `npx skills add anthropics/skills --skill frontend-design -a antigravity --copy -y` | 通过。成功下载安装至 `.agents/skills/frontend-design`，Socket 评估 0 告警，安全评级 Safe。 |
| Google Android 核心技能筛选与安装 | `npx skills add android/skills --skill <15项核心技能> -a antigravity --copy -y` | 通过。15 项原生核心技能全部复制到本地目录，安全评估全通过。 |
| 元数据与 SKILL.md 格式校验脚本 | PowerShell 脚本遍历全部 16 个 SKILL.md | 通过。所有技能的 `SKILL.md` 均存在，YAML frontmatter `name` 与目录一致且 `description` 完整。 |
| `frontend-design` Compose 规范注入 | 文件内容核对 | 通过。成功在 `SKILL.md` 注入 Android & Compose Native Mandate，明确禁止 Web 代码。 |
| 临时构建物与下载清理 | PowerShell `Remove-Item` | 通过。克隆的临时文件已彻底清理，未遗留冗余垃圾。 |

## 限制与后续

- **IDE 识别生效说明**：Antigravity 采用热插拔机制扫描项目 `.agents/` 目录；如果当前会话中技能列表未立即刷新，建议在编辑器中按 `Ctrl+Shift+P` 执行 `Developer: Reload Window` 或重启 IDE，以确保最新安装的技能完整注入到 AI 的技能索引上下文。
- **后续使用**：进行 Compose UI 重构或新页面开发时，可通过提示词如“遵循 frontend-design 设计规范与 Compose performance 准则进行页面重构”显式或隐式激活对应技能。

## 同步更新的文档

- [docs/README.md](file:///d:/Project/Resonance/docs/README.md)（变更索引新增本记录条目）
