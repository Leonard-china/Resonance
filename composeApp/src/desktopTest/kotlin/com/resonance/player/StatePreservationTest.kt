package com.resonance.player

import com.resonance.player.model.AiConnectionDraft
import com.resonance.player.model.AiDraftStore
import com.resonance.player.model.AiProvider
import kotlin.test.*

class StatePreservationTest {
    @BeforeTest fun clean() {
        AiDraftStore.clear(AiProvider.DeepSeek)
        AiDraftStore.clear(AiProvider.Qwen)
    }

    @Test fun draftStorePreservesUnsavedInputAcrossRecreationWithoutBundleSerialization() {
        assertNull(AiDraftStore.get(AiProvider.DeepSeek))
        val draft = AiConnectionDraft("https://api.deepseek.com", "deepseek-chat", "sk-test-secret-key", false)
        AiDraftStore.save(AiProvider.DeepSeek, draft)

        val retrieved = AiDraftStore.get(AiProvider.DeepSeek)
        assertNotNull(retrieved)
        assertEquals("https://api.deepseek.com", retrieved.baseUrl)
        assertEquals("deepseek-chat", retrieved.model)
        assertEquals("sk-test-secret-key", retrieved.apiKey)
        assertFalse(retrieved.vision)

        AiDraftStore.clear(AiProvider.DeepSeek)
        assertNull(AiDraftStore.get(AiProvider.DeepSeek))
    }

    @Test fun draftStoreKeepsIndependentDraftsPerProvider() {
        AiDraftStore.save(AiProvider.DeepSeek, AiConnectionDraft("https://api.deepseek.com", "deepseek-chat", "key1", false))
        AiDraftStore.save(AiProvider.Qwen, AiConnectionDraft("https://dashscope.aliyuncs.com", "qwen-max", "key2", true))

        assertEquals("key1", AiDraftStore.get(AiProvider.DeepSeek)?.apiKey)
        assertEquals("key2", AiDraftStore.get(AiProvider.Qwen)?.apiKey)
        assertTrue(AiDraftStore.get(AiProvider.Qwen)?.vision == true)

        AiDraftStore.clear(AiProvider.DeepSeek)
        assertNull(AiDraftStore.get(AiProvider.DeepSeek))
        assertNotNull(AiDraftStore.get(AiProvider.Qwen))
        AiDraftStore.clear(AiProvider.Qwen)
    }
}
