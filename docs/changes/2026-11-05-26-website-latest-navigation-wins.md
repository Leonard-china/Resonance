# 2026-11-05 — 官网导航乱序响应、缓存和历史统一按最后意图提交

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.6 / 40（version.properties；不升版）
- 影响平台：独立官网
- 关联需求：用户要求修复本次全面测试问题；[原始缺陷](../audit/2026-11-05/BUGS.md)、[原始手机结果](../audit/2026-11-05/ANDROID-DEVICE-REPORT.md)，编号Q12。

## 需求或问题

先点论坛后点下载，较慢的论坛响应最后覆盖DOM与history；延迟过渡回调和缓存前进后退也可能倒退。

## 原因与依据

异步响应没有统一请求代次。每次导航（含缓存/popstate）先递增代次，过期响应及过渡提交无权修改页面、历史、回退和进度。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [static/site.js](../../website/static/site.js) | navigate / navigationGeneration / popstate | 统一代次检查，跳过旧ViewTransition，pushState与applyPage放在同一有效提交。 |
| [tests/test_navigation.cjs](../../website/tests/test_navigation.cjs) | Node内置测试 | 逆序请求、缓存命中、延迟过渡、popstate、当前失败回退和点击当前页六项。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| Q12所述输入/操作 | 旧响应能覆盖最后点击 | 只有当前有效导航能提交页面/history或触发原生回退 |

## 平台与数据影响

独立官网JS，无播放器影响、数据库/凭据/依赖变更；公共GET读取缓存行为保留。不部署、不向论坛/邮件写入。

## 验证

本次相关用例：node --test website/tests/test_navigation.cjs；Python官网/服务回归。执行命令、独立数据边界、逐用例结果、失败后的定点复测及计数统一见[本次修复验收](../audit/2026-11-05/FIX-VALIDATION.md)。该报告保留初次失败记录，历史修复前通过数不作为本次通过依据。

| 命令 | 本次结果 |
| --- | --- |
| `node --test website/tests/test_navigation.cjs` | 6项通过，0失败 |
| `python -m unittest discover -s website/tests -p 'test_*.py' -v` | 45项通过，0失败 |

## 限制与后续

Node隔离DOM夹具执行真实site.js，未声称真实浏览器端到端、现网发布或SMTP验收。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：相应导入、持久化、AI、UI/测试或官网章节，核对日2026-11-05。
- [文档索引](../README.md)：独立记录与修复验收入口。
