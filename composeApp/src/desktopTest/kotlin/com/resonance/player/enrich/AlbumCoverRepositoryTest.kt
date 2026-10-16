package com.resonance.player.enrich

import com.resonance.player.features.MusicJsonStore
import com.resonance.player.model.SongRequest
import java.nio.file.Files
import kotlin.test.*

class AlbumCoverRepositoryTest {
    @Test fun catalogRejectsCoversAndLiveVersionsAndPrefersRequestedAlbum() {
        val root = Files.createTempDirectory("resonance-catalog-")
        try {
            val source = AlbumCoverRepository(MusicJsonStore(root))
            val json = """{"code":0,"data":{"song":{"list":[
                {"songname":"晴天 (Live)","singer":[{"name":"周杰伦"}],"albumname":"演唱会","albummid":"0000000000000000"},
                {"songname":"晴天","singer":[{"name":"其他歌手"}],"albumname":"翻唱","albummid":"1111111111111111"},
                {"songname":"晴天","singer":[{"name":"周杰伦"}],"albumname":"叶惠美","albummid":"000MkMni19ClKG"}
            ]}}}"""
            val cover = assertNotNull(source.parseQqCatalog(json, SongRequest("晴天", "周杰伦", "叶惠美")))
            assertEquals("叶惠美", cover.album)
            assertTrue(cover.artworkUrl.orEmpty().endsWith("000MkMni19ClKG.jpg"))
            assertNull(source.parseQqCatalog(json, SongRequest("晴天", "不存在的歌手")))
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
}
