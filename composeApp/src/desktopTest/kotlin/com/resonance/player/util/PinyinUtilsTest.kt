package com.resonance.player.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinyinUtilsTest {

    @Test
    fun testGetCharInitial() {
        assertEquals('z', PinyinUtils.getCharInitial('周'))
        assertEquals('j', PinyinUtils.getCharInitial('杰'))
        assertEquals('l', PinyinUtils.getCharInitial('伦'))
        assertEquals('a', PinyinUtils.getCharInitial('A'))
        assertEquals('b', PinyinUtils.getCharInitial('b'))
        assertEquals('1', PinyinUtils.getCharInitial('1'))
    }

    @Test
    fun testToPinyinInitials() {
        assertEquals("zjl", PinyinUtils.toPinyinInitials("周杰伦"))
        assertEquals("qlx", PinyinUtils.toPinyinInitials("七里香"))
        assertEquals("ts", PinyinUtils.toPinyinInitials("Taylor Swift"))
    }

    @Test
    fun testMatches() {
        // 原文包含匹配
        assertTrue(PinyinUtils.matches("周杰伦 - 晴天", "晴天"))
        assertTrue(PinyinUtils.matches("周杰伦 - 晴天", "周杰伦"))

        // 拼音首字母缩写匹配
        assertTrue(PinyinUtils.matches("周杰伦", "zjl"))
        assertTrue(PinyinUtils.matches("七里香", "qlx"))

        // 忽略大小写
        assertTrue(PinyinUtils.matches("Taylor Swift", "ts"))
        assertTrue(PinyinUtils.matches("Taylor Swift", "taylor"))

        // 空搜索词匹配任意
        assertTrue(PinyinUtils.matches("任何歌曲", ""))

        // 不匹配
        assertFalse(PinyinUtils.matches("晴天", "阴天"))
        assertFalse(PinyinUtils.matches("晴天", "xyz"))
    }
}
