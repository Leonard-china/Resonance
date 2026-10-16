package com.resonance.player.media

import com.resonance.player.model.BiliQrPollStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class BilibiliLoginServiceTest {

    @Test
    fun testCreateQrSessionSuccess() = runBlocking {
        val mockService = BilibiliLoginService(
            httpGet = { url, _ ->
                assertTrue(url.contains("qrcode/generate"))
                BilibiliLoginService.HttpResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = """{"code":0,"data":{"url":"https://passport.bilibili.com/h5-app/passport/login/scan?qrcode_key=key123","qrcode_key":"key123"}}"""
                )
            }
        )
        val session = mockService.createQrSession()
        assertNotNull(session)
        assertEquals("key123", session.qrcodeKey)
        assertTrue(session.matrixSize > 0)
        assertEquals(session.matrixSize * session.matrixSize, session.bits.size)
        // Ensure at least some bits are true (black pixels)
        assertTrue(session.bits.any { it })
    }

    @Test
    fun testCreateQrSessionFailure() = runBlocking {
        val mockService = BilibiliLoginService(
            httpGet = { _, _ ->
                BilibiliLoginService.HttpResponse(
                    statusCode = 500,
                    headers = emptyMap(),
                    body = """{"code":-500,"message":"server error"}"""
                )
            }
        )
        val session = mockService.createQrSession()
        assertNull(session)
    }

    @Test
    fun testPollQrStatusWaiting() = runBlocking {
        val mockService = BilibiliLoginService(
            httpGet = { url, _ ->
                assertTrue(url.contains("qrcode_key=key123"))
                BilibiliLoginService.HttpResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = """{"code":0,"data":{"code":86101,"message":"未扫码"}}"""
                )
            }
        )
        val result = mockService.pollQrStatus("key123")
        assertEquals(BiliQrPollStatus.WAITING, result.status)
        assertTrue(result.message.contains("扫码"))
    }

    @Test
    fun testPollQrStatusScanned() = runBlocking {
        val mockService = BilibiliLoginService(
            httpGet = { _, _ ->
                BilibiliLoginService.HttpResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = """{"code":0,"data":{"code":86090,"message":"已扫码未确认"}}"""
                )
            }
        )
        val result = mockService.pollQrStatus("key123")
        assertEquals(BiliQrPollStatus.SCANNED, result.status)
        assertTrue(result.message.contains("确认"))
    }

    @Test
    fun testPollQrStatusExpired() = runBlocking {
        val mockService = BilibiliLoginService(
            httpGet = { _, _ ->
                BilibiliLoginService.HttpResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = """{"code":0,"data":{"code":86038,"message":"二维码已失效"}}"""
                )
            }
        )
        val result = mockService.pollQrStatus("key123")
        assertEquals(BiliQrPollStatus.EXPIRED, result.status)
        assertTrue(result.message.contains("失效"))
    }

    @Test
    fun testPollQrStatusSuccessWithCookies() = runBlocking {
        val mockService = BilibiliLoginService(
            httpGet = { _, _ ->
                BilibiliLoginService.HttpResponse(
                    statusCode = 200,
                    headers = mapOf(
                        "Set-Cookie" to listOf(
                            "SESSDATA=sess_mock_value; Path=/; Domain=.bilibili.com; HttpOnly",
                            "bili_jct=jct_mock_value; Path=/; Domain=.bilibili.com",
                            "DedeUserID=1000888; Path=/; Domain=.bilibili.com"
                        )
                    ),
                    body = """{"code":0,"data":{"code":0,"message":"0","url":""}}"""
                )
            }
        )
        val result = mockService.pollQrStatus("key123")
        assertEquals(BiliQrPollStatus.SUCCESS, result.status)
        assertNotNull(result.cookie)
        assertTrue(result.cookie!!.contains("SESSDATA=sess_mock_value"))
        assertTrue(result.cookie!!.contains("bili_jct=jct_mock_value"))
        assertTrue(result.cookie!!.contains("DedeUserID=1000888"))
    }

    @Test
    fun testExtractAndMergeCookies() {
        val headers = mapOf(
            "Set-Cookie" to listOf(
                "SESSDATA=abc; Path=/",
                "bili_jct=def; Domain=.bilibili.com"
            )
        )
        val extracted = BilibiliLoginService.extractCookies(headers)
        assertEquals("SESSDATA=abc; bili_jct=def", extracted)

        val merged = BilibiliLoginService.mergeCookies(extracted, "sid=xyz; SESSDATA=new_abc")
        assertTrue(merged.contains("bili_jct=def"))
        assertTrue(merged.contains("sid=xyz"))
        assertTrue(merged.contains("SESSDATA=new_abc"))
    }
}
