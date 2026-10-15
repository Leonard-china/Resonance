# 2026-10-15 — 修复列表封面回退并限制大图内存

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：Bug 修复 / 性能
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

Android MediaStore 缩略图 URI 抛异常时外层 runCatching 结束，没走音频内嵌封面回退；封面重复/全尺寸解码增加滚动开销。

## 具体修改

- 在缩略图分支内部捕获读取失败，继续用 audioArtwork 音频 URI 提取内嵌图片。
- 新增跨列表共享位图缓存，最多40项/约800万像素、3并发与 single-flight；失败不负缓存。
- 两端解码限制约768像素；Android 明确初始化采样率为1，修正设备测试发现的零采样值问题。
- 补全完成清缓存并重载，保持歌单/本地封面优先级。
- 缓存修订号触发现有列表图片重新加载；清空后旧解码任务不能把过期结果写回新缓存。

| 文件 | 关键修改 |
| --- | --- |
| [platform/DynamicArtwork.android.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/DynamicArtwork.android.kt) | decodeArtwork/decodeBounded/embeddedArtwork |
| [platform/DynamicArtwork.desktop.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DynamicArtwork.desktop.kt) | ImageReader 下采样 |
| [platform/ArtworkMemoryCache.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ArtworkMemoryCache.kt) | LRU/像素预算/请求合并 |
| [ui/LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | AlbumArtwork 异步缓存 |

## 修改前后

以前列表可能无封面而播放器能显示；现在缩略图异常也继续尝试同一音频内嵌图。

## 平台与数据影响

缓存只在内存、原封面不改写；源文件和权限真实失效仍回退占位。

## 验证

MusicFeaturesIntegrationTest 在 API32 生成带封面 MP3，以不存在缩略图 URI 检查内嵌回退；实际结果见验证记录。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

不保证所有损坏图片或私有 provider 可读取；解码失败不永久缓存。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
