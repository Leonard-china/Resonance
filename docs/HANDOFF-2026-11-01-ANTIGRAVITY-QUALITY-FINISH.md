# 2026-11-01 — Antigravity 质量优化收尾提示词

用途：供 Gemini / Antigravity 接手已完成两批核心优化的原生项目，补齐设备验收和本地候选包。不是重新执行全项目审计，也不代表生产部署已获批准。本文件限定本次接手范围；旧 `HANDOFF-2026-11-01-ANTIGRAVITY-VALIDATION.md` 只作历史定位参考，不自动执行其中所有任务。

## 可直接交给 Antigravity 的指令

你接手 `D:\Project\Resonance`。项目是 Kotlin / Compose Multiplatform 原生 Android + Windows 音乐播放器，`website/` 为独立官网。请完成以下收尾工作，优先本地工具，控制上下文和迭代次数。

### 1. 先确认状态并保护工作区

- 先 `git status --short`，保留全部已有修改和未跟踪文件，不 reset、clean、stash、整体覆盖或一键提交。当前工作区混有多轮未提交成果，分支指针不能恢复未提交文件；修改前为本次涉及文件另存检查点。
- 按顺序读 `AGENTS.md`、`docs/README.md`、`docs/AI-WORKFLOW.md`、`docs/DEVELOPMENT.md` 的任务定位/构建/测试章节，再读 `docs/OPTIMIZATION_REPORT.md`、`docs/audit/2026-11-01/quality-verification.json` 、`docs/audit/2026-11-01/stability-verification.json` 和变更 17–19；第一批变更 12–15 按需定位。
- 定点阅读相关代码及测试，禁止从头反复扫描整个项目。源码是最终依据；文件变更后历史测试不能当作当前通过。
- 保留音乐、收藏、歌单、历史、API 配置、转换流程、播放防竞争机制、原子存储和同步边界。不得读取/输出真实 `keystore.properties`、API Key、Token、私钥或解密用户凭据。正常构建可使用已有安全配置，不导出其内容。

### 2. 准确理解已有结果

上一轮已实现：浅/深主题统一、一起听界面简化、统计损坏保护与 IO 批量写入、进度释放一次提交、全屏播放错误弹窗，以及默认自有服务器 WSS 通道。

上一轮结果：桌面 280 项（276 通过、4 跳过）；Android 编译/Debug 构建成功；Android Lint 0 errors / 110 warnings；公共 relay Python 6 项通过；JVM 经真实本地 WebSocket 的内层双向证书 TLS 往返通过。生成 70 张页面与 2 张错误截图，精选在 `docs/audit/2026-11-01/after/`。这些是历史证据，不能写成你本次执行的结果。

公共地址已写入客户端：`wss://leonard.asia/resonance-relay/v1`，**服务尚未部署**。本地 TLS 通过不能证明 Android Conscrypt 或公网通过；模拟器、同机与局域网不能冒充真实跨网。

第二批最新结果：36 项定向桌面测试全部通过（新增 10 项），双端编译通过；原生统计计时/完播去重、偏好顺序、错误请求归属与试听隔离已实现。新增安全检查点 `.git/audit-checkpoints/stability-20261101-215043/files`。统计/配置失败在全屏弹窗提示，但不暂停主播放。不要重新实现这些改动，也不要将第一批全量通过与第二批定向通过混算。

### 3. 先完成有价值的缺失验证

1. 核对当前源码与上一轮记录。未变更部分可引用已存证据并标注日期，不盲目反复全量跑；存在新修改时按影响范围回归，最终交付必须对应实际源码。
2. 使用隔离目录设置 `RESONANCE_DATA` 和 `RESONANCE_LIBRARY`。回环联测将 `RESONANCE_RELAY_TEST_PYTHON`、`RESONANCE_RENDEZVOUS_PYTHON` 指向已验证的本机 Python，不提交路径。不要自动安装大量依赖。
3. 共享/桌面、Android Lint/构建的已验证命令如下；先确认本机工具可用，失败记录具体原因，勿为了绿灯删断言或跳过失败。

```powershell
.\gradlew.bat :composeApp:desktopTest :composeApp:lintRelease :composeApp:assembleDebug --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain
python -m unittest discover -s services/relay -p 'test_*.py' -q
```

补齐 Android 单元测试（现有任务 `:composeApp:testReleaseUnitTest`，以项目实际可用任务为准）。`--offline` 缺缓存时先明确具体依赖，再决定是否联网获取，不把环境失败记成产品缺陷。不要并行启动多个重 Gradle/渲染任务覆盖报告或争抢内存。

4. 查看现有 Android 设备/模拟器及正在运行的任务。历史记录使用过 MuMu，先核实当前可用性；不能打断其他人的仪器测试、force-stop 正在使用的包、清数据或卸载。优先独立测试包和临时原创/公版音频，不替用户通过系统权限或防火墙弹窗。
5. 真正运行 Windows 和可用 Android 环境，验证：播放/暂停、快速连续切歌、上一首/下一首、随机/循环、拖动进度、音量/静音、坏文件和网络失败、切页/后台/恢复。观察原生实际曲目和声音，不只看 UI 状态。第二批重点：95% 后多次暂停恢复只计一次完播；循环另计一轮；2 倍速/seek/缓冲无虚增；Android Activity 重建/退后台服务继续计时；快速切换随机和循环后重启恢复最后一对；A→B→A 旧错误不影响新 A；试听/统计/配置失败不暂停主播放。不可写场景只用临时目录夹具，禁止破坏真实历史。
6. 验证浅色、深色、跟随系统；窄/宽/短窗口、大字体、键盘操作、减少动画。截图对照现有证据，不新增无目的视觉设计；真实数据之外的夹具须标明。
7. 一起听使用至少 Windows + Android 验证原生 TLS、加入/退出/断线重连、播放与队列；多人能力需要至少 3 个独立成员，双人通过不算多人通过。以临时本地 relay 做可执行的协议检查；仅当生产服务另行获批并部署后，才能测真实 WSS 默认路径与不同网络。

默认禁止付费 AI 探针，保持 `RESONANCE_PROMO_AI=0`。`RESONANCE_DEVICE_BRIDGE`、`RESONANCE_NATIVE_BRIDGE`、`RESONANCE_REVERSE_BRIDGE` 只在准备好对应设备和隔离夹具后按既有测试说明开启；其他时候设为 `0` 并如实记录跳过。没有设备时继续不依赖设备的工作，不伪造设备通过。

### 4. 对遗留问题限定范围

第二批已修复统计生命周期/完播去重、偏好顺序与错误归属，缺少的是设备最终验收。**仍有独立 P1：完整 Preparing/Buffering 状态未实现；本轮仅修正桌面过早发出播放成功与失败归属。** 不得宣称所有审计项完成，也不要在收尾时擅自重构状态机。P2 大库、图片/内存、首次统计读取和既有 Lint 警告先留证据与建议。

验收中发现可复现的小范围缺陷，可以定位、最小修复并加必要回归。涉及播放器状态机、协议、持久化迁移或较大架构改动，先给证据和独立方案，不为完成交接擅自扩大范围。不得宣称这些审计待办全部已修复，也不要求本轮清零全部 110 个警告。

### 5. 本地候选包与生产上线分开验收

完成适用检查后，可按现有配置构建当前版本的本地候选包；版本仅取 `version.properties`，不自动升版或修改生成版本文件：

```powershell
.\gradlew.bat :composeApp:assembleRelease --no-configuration-cache --max-workers=2
.\gradlew.bat :composeApp:createReleaseDistributable :composeApp:packageReleaseExe --no-configuration-cache --max-workers=2
```

仅用本次产物验签/计算 SHA-256，在安全隔离条件下验证启动，记录构建时间、准确路径、版本、签名状态及源码对应关系。缺少 JDK/FFmpeg/WiX/签名配置时报告阻碍，不能拿旧 APK/EXE 冒充新包。候选包保存在新输出目录；未完成公网/设备验收时标注“待验收”，不向用户承诺默认服务器可用，不公开发布、发送消息、推送或提交。

服务器上线是另一块工作，读取 `services/relay/DEPLOYMENT-leonard.md` 即可了解方案：新增独立回环 45902 服务与 Tunnel 专用路径，保留官网 50034/根路由/数据；初始每日 2 GiB 应用转发数据上限不是账单硬限额。实际守护进程补丁、Cloudflare 权限和路由仍需现场确认，**生产配置变更尚未批准**。不得因为这份交接提示词就部署、改路由、放宽限额、压测生产或购买服务。以后若用户明确批准，按具体批准范围执行，不重复请求已给出的同一授权。

### 6. 交付与停止条件

- 输出 `docs/ANTIGRAVITY-QUALITY-VALIDATION-<实际日期>.md`：本次真实通过/失败/未执行/环境受阻、复现与修复、设备与网络、截图、候选包及哈希、发布阻碍。历史结果另列，不能混算。
- 每个独立修复按 `AGENTS.md` 新增当天递增编号的变更记录，更新 `docs/README.md`，架构/行为变化再更新 DEVELOPMENT；不覆盖以前的记录。纯验收/文档交付也留独立记录。
- 一次有界验收；失败先定位，改动后定点复测。不重复改同一个方案无限试错，不换技术框架，不调用额外付费模型做宣传/图片。
- 原总预算为 10 美元，用户后来重置账户额度并授权继续，但这不提供美元计费数据，也不代表新的 10 美元。无法获知费用时停止高成本操作并报告，优先本地工具。
- 最终说明：本批验收是否完成、是否可生成本地候选包、是否满足正式上线条件。没有生产/跨网验收，应明确上线仍受阻。

## 给下一位模型的停止条件

先完成本批已实现代码的检查、适用设备验收、源码对应的本地候选包和报告。若只剩缺设备/未批准生产部署，不无限重试；明确列出阻碍。完整加载/缓冲状态属于仍未完成的产品工作，给定点方案，不把它计入“已全部完成”。一次验收后交付结果即可，不开展第三轮全项目审计。
