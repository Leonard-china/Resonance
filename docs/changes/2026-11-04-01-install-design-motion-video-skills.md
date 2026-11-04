# 2026-11-04 — 安装官网设计、动效检查与宣传视频技能

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：配置 / AI 协作工具 / 文档
- 适用版本：0.6.5 / 39；未更改版本号
- 影响平台：本项目本地 AI 协作环境；技能参考官网和原生 Android、Windows、Linux 开发
- 关联需求：用户要求安装讨论过的三套技能，并准备给 Antigravity 的交接提示词。

## 需求或问题

此前已安装 Emil 的 12 项技能，但上游新增的四项及官网设计 `design-taste-frontend`、宣传片 `onetake` 尚未安装。现有 Emil 仓库包含未提交改动，不能通过更新仓库覆盖它们。

## 原因与依据

使用 skill-installer 下载 GitHub 固定提交的副本，并安装到忽略的 `.agents/skills/` 中。Emil 现有目录保持不动，四项新增技能独立安装。来源和目录哈希保存在 [skills-lock.json](../../skills-lock.json)。

## 具体修改

| 文件 | 关键符号 / 配置 | 具体修改与目的 |
| --- | --- | --- |
| [技能锁文件](../../skills-lock.json) | `design-taste-frontend`、`onetake`、`remove-animations`、`animation-performance`、`mobile-native`、`break-ui` | 新增六项来源、提交、上游入口和完整目录哈希；保留原有条目 |
| `.agents/skills/design-taste-frontend/` | `design-taste-frontend` | 安装 taste-skill 官网设计技能，来源 `Leonxlnx/taste-skill`，提交 `12af471f627f866411b4bd785b19646d3672784c` |
| `.agents/skills/onetake/` | `onetake` | 安装产品宣传片技能及其配套文件，来源 `feitangyuan/onetake`，提交 `013a61ac8d98883c73733f59c06e49e9ba444f14` |
| `.agents/skills/{remove-animations,animation-performance,mobile-native,break-ui}/` | 四项 Emil 新技能 | 补充动效删减、网页动效性能、移动网页和 UI 极端数据检查，来源 `emilkowalski/skills`，提交 `f1090bec277140d02ed202440185ad9d02ac1490` |
| [开发说明](../DEVELOPMENT.md)、[文档入口](../README.md) | 本地 UI 设计技能、变更索引 | 记录适用范围、许可证、运行依赖和本次变更 |

目录哈希沿用已有锁文件算法：按相对路径 `localeCompare` 排序，将路径和文件字节依次输入 SHA-256，排除 `.git` 与 `node_modules`。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 使用上述六项技能 | 项目技能目录没有独立入口 | 本地存在技能入口与上游配套文件，可在后续会话按任务选择 |
| 原有 Emil 技能 | 目录包含已有未提交改动 | 原目录保持不动，未覆盖或更新原技能 |
| 官网、播放器和视频 | 不受影响 | 应用源码与现有成片保持不变；未制作视频或修改页面 |

## 平台与数据影响

未修改共享 Kotlin、Android、Windows 或 Linux 播放器实现、版本号、配置、同步包和用户音频。仅下载 GitHub 公共归档；没有读取用户密钥。`.agents/` 为忽略目录，其他机器需要根据锁文件安装。`onetake` 为 PolyForm Noncommercial 1.0.0，商业用途应先确认授权；`taste-skill` 和 Emil 项目为 MIT。素材的独立授权另行核对。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `git ls-remote` 三个 GitHub 仓库的 `refs/heads/main` | Linux / Git | 通过，确定上文列出的固定提交 |
| `install-skill-from-github.py --repo ... --ref ... --path ... --dest .agents/skills` | Linux / Python / skill-installer | 通过，六项目标目录安装完成；未运行上游安装脚本 |
| 入口与锁文件静态核对、目录哈希计算 | Linux / Node | 通过，六项均有 `SKILL.md`，并按目录内容生成哈希 |
| `node .agents/skills/design-taste-frontend/scripts/draw.mjs --list sectors` | Linux / Node | 通过，设计方向脚本列出可用领域；未运行浏览器检查 |
| `node .agents/skills/onetake/scripts/test_motion.js` | Linux / Node | 通过，上游动效函数测试输出 `ALL PASS`；不代表视频渲染通过 |
| `git diff --check`；`git -c core.whitespace=cr-at-eol diff --check` | Linux / Git | 默认检查失败：已有 `docs/DEVELOPMENT.md` 的 CRLF 差异被报告为行尾空白；指定 `cr-at-eol` 后通过，未重写已有行尾 |
| Node、FFmpeg 和 Python 模块检查 | Linux / 当前 Python 环境 | Node 24.22.2、FFmpeg 存在；Pillow 存在；`playwright`、`numpy`、`scipy`、`matplotlib`、`cv2` 未发现；未核对其他 Python 环境 |
| 实际官网制作、浏览器渲染、视频渲染及播放器回归 | 应用与素材环境 | 未执行；本次只安装技能，未安装视频运行依赖或改变应用 |

## 限制与后续

新技能需在后续会话重新发现；各客户端技能发现规则可能不同。`onetake` 首次执行前需按其文档配置依赖和 Chromium；当前 Python 环境未满足其完整依赖。`design-taste-frontend` 的浏览器检查不证明原生 Compose UI 正确。新增 Emil 技能中的 `animation-performance` 和 `mobile-native` 面向 Web。工具和视觉规则应服从项目要求和实际平台验证。

## 同步更新的文档

- [文档入口](../README.md)：增加本次记录的索引。
- [开发说明](../DEVELOPMENT.md)：第 15 节记录本次技能和使用边界。
- 协作规则没有变化，不修改 `AI-WORKFLOW.md`。
