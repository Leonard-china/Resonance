# 2026-11-02 — 准备独立一起听中继守护与现网启动补丁

- 修改日期：2026-11-02（Asia/Shanghai）；类型：部署准备/测试；服务端 Linux，客户端版本仍为 0.6.5（39）。
- 关联：[公共 WSS 实现](2026-11-01-15-public-websocket-relay.md)、[现网运维](../../website/OPERATIONS.md)。
- 本记录阶段：**未部署**；已经请求用户确认具体生产变更，并等待 Cloudflare 登录。一般“继续开发”不替代用户原先要求的生产变更确认。

## 需求、原因与只读核对

用户要求所有安装用户默认使用其服务器一起听。客户端 WSS 与本地配对协议已存在；现网仍只有官网启动项，未安装 relay。临时启动后台进程不能保证随网站重启正确回收，重复中继进程还可能共同写预算文件。

本次通过已有运维客户端只读确认容器运行，内存约 837 MiB、磁盘约 2442 MiB；网站虚拟环境配置为 Python 3.12。`start.sh` 为既有邮件 worker 与 Gunicorn 50034，SHA-256 为 `4ea0e3ad8b18eb1f41b41093c2e4eeb32c74c4ce70b1012c22f775246b3e63b0`。没有输出认证配置或 Tunnel Token，也未读数据库。Cloudflare 浏览器会话目前需要用户登录。

## 实际修改

| 文件 | 符号/行为 |
| --- | --- |
| [supervise.py](../../services/relay/supervise.py) | 单独守护 relay；跟随启动脚本/Gunicorn 的父进程退出；仅终止自身子进程组；连续失败最多 5 次后停止，官网不随之重启 |
| [start-hook.sh](../../services/relay/start-hook.sh) | 仅增加守护启动项，保留 PATH、官网端口、邮件 worker 和 Gunicorn 命令；环境缺失时明确报告，官网照常继续 |
| [test_supervise.py](../../services/relay/test_supervise.py) | 实际 Python 子进程验证失败预算、父任务退出后子进程终止、无效可执行文件有界退出 |
| [部署方案](../../services/relay/DEPLOYMENT-leonard.md) | 说明文件锁、父进程生命周期、准确启动补丁及回滚边界 |

Linux 主入口以 flock 独占预算写入所有权；重启时最多等旧实例退出 15 秒，不通过删锁或预算文件绕过保护。监听仍为 127.0.0.1:45902，每日 2 GiB 应用负载限制保持。`supervise` 核心测试在本机 Windows 使用真实子进程，Linux flock/进程组行为仍需部署现场验收。

## 验证与可审阅产物

- `python -m unittest discover -s services/relay -p 'test_*.py' -q`：9 项全部通过（6 项原中继测试 + 3 项守护回归），最终日志 `build/transition-relay-20261102/relay-verified.log`。
- Git Bash 对 `run-public-relay.sh` 与 `start.sh.proposed` 的 `-n` 语法检查通过。首次直接调用 `bash` 因 PATH 无该命令失败；改用本机已存在的 Git Bash，不安装依赖。
- 白名单准备包 `build/transition-relay-20261102/resonance-relay-review.zip`；仅源码、固定依赖与方案，无凭据/数据。
- 对当前启动脚本生成精确补丁 `build/transition-relay-20261102/start.sh.patch` 和提案 `start.sh.proposed`。正式执行前再次核对原脚本哈希；若变化则重新审阅，不能覆盖他人现网修改。

## 平台、数据影响与未完成项

本地准备不更改网站业务、生产文件、数据库、用户记录、Cloudflare 或客户端默认值。批准后的实际动作：服务器私有备份、独立虚拟环境与服务目录、插入守护项、增加专用 Tunnel 路径，保留官网兜底。启用可能短暂重启官网及 Tunnel，需要用户确认；不购买服务或提高限额。

上线后才可验证公网 WebSocket、内层 TLS、Windows/Android 不同网络及多人会话；本地 9 项通过不代表这些已完成。回滚先撤新增路由、停止准确的守护/子进程、恢复启动备份，保留预算与官网数据。

同步更新 README 索引、DEVELOPMENT、OPTIMIZATION_REPORT 和 relay 文档。生产操作若继续执行，应另建上线记录并保存真实验收结果。
