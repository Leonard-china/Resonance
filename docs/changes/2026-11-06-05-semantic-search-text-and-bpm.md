# 2026-11-06 — 修复 BPM 搜索忽略明确歌手和文字条件

- 修改日期：2026-11-06（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.8 / 42，不升版
- 影响平台：共享、Android、Windows、Linux
- 关联记录：[前置实现](2026-11-05-39-offline-bpm-analysis-and-filtering.md)；用户批准实施本轮审查中的四项改进。

## 需求或问题

搜索“周杰伦 120 BPM”时，节拍符合但歌手完全不符的曲目也返回，明确文字条件只影响排序。

## 原因与依据

`matchLocalSemanticTracks` 只在没有 BPM 范围时排除 `hits.isEmpty()`。一旦有节拍条件，所有满足 BPM 的曲目都能通过。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [SemanticSearchModels.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/SemanticSearchModels.kt) | `matchLocalSemanticTracks` | 显式 keywords、artists、genres 必须逐项匹配标题/歌手/专辑文字，并同时满足 BPM；场景/情绪仍可用于节拍推断和排序，不要求歌名包含“跑步”。 |
| [SemanticSearchConstraintsTest.kt](../../composeApp/src/commonTest/kotlin/com/resonance/player/SemanticSearchConstraintsTest.kt) | 四项新增回归 | 歌手/歌名与 BPM 交集、曲风、未知节拍、纯 BPM、跑步场景、无 BPM 文字搜索。 |

未新增“扩展推荐”混排；仍为本地文字与缓存 BPM 查找，不将文字匹配宣传为真实音频曲风分类，不改变 BPM 分析算法和缓存格式。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 周杰伦 120 BPM | 其他歌手也出现 | 明确文字与节拍同时满足 |
| 摇滚 120 BPM | 无摇滚文字也出现 | 曲风文字与节拍同时满足 |
| 跑步/仅 120 BPM | 按节拍推荐 | 保持，不要求标题含场景词 |

## 平台与数据影响

Android、Windows、Linux 共用相应共享逻辑，平台存储适配分别核对。版本保持 `version.properties` 的 0.6.8 / 42。保留原音频、收藏与歌曲 ID，不删除或改写原音频/标签；不改变 `.resonance` 包格式，不新增权限、外部服务或付费请求，不读取真实 Key 或签名文件。

## 验证

本次实际编译、定点回归、资源限制及未执行项见[本轮验证](../audit/2026-11-06/review-fixes/VALIDATION.md)。日志和本机脚本保留在忽略目录 `build/review-fixes-2026-11-06/`，不引用上轮审查或旧包结果作为本次通过依据。

## 限制与后续

仅交付源码、回归及文档；不打包、安装、升版或发布。Android 设备、Windows 原生操作、物理外置盘及长时间播放不由 JVM 替身测试代替；实际执行范围以上述验证记录为准。

## 同步更新的文档

`docs/README.md` 变更索引、`docs/DEVELOPMENT.md` 对应当前行为与回归入口，核对日期 2026-11-06。
