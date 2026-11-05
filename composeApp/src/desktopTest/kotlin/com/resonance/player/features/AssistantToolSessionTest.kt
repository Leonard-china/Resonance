package com.resonance.player.features

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.*

class AssistantToolSessionTest {
    private fun prefix() = JSONArray().put(JSONObject().put("role", "system").put("content", "fixture"))
    private fun record(session: AssistantToolSession, id: String, text: String) {
        session.record(JSONObject().put("role", "assistant").put("content", JSONObject.NULL)
            .put("tool_calls", JSONArray().put(JSONObject().put("id", id).put("type", "function")
                .put("function", JSONObject().put("name", "fixture").put("arguments", "{}")))),
            listOf(JSONObject().put("role", "tool").put("tool_call_id", id).put("content", text)))
    }

    @Test fun identicalMutationsRunOnceEvenAfterManySuccessfulRoundsAndArgumentReordering() = runBlocking {
        val session = AssistantToolSession(prefix())
        var writes = 0
        repeat(32) { index ->
            val result = session.execute("add_to_playlist", JSONObject().put("track", index).put("playlist", "fixture")) {
                writes++; "{\"saved\":true}"
            }
            record(session, "call-$index", result.text)
        }
        assertEquals(32, writes)
        val duplicate = session.execute("add_to_playlist", JSONObject().put("playlist", "fixture").put("track", 0)) {
            writes++; error("must not run")
        }
        assertTrue(JSONObject(duplicate.text).getBoolean("already_executed"))
        assertEquals(32, writes)
        assertTrue(session.messages().toString().length < 32_000)
        val messages = session.messages()
        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            if (message.optString("role") == "tool") {
                assertEquals(message.getString("tool_call_id"), messages.getJSONObject(i - 1)
                    .getJSONArray("tool_calls").getJSONObject(0).getString("id"))
            }
        }
    }

    @Test fun failedDuplicateRunsOnceAndStopsWithoutAnotherRequestAfterSixIdleRounds() = runBlocking {
        val session = AssistantToolSession(prefix())
        var attempts = 0
        repeat(5) { index ->
            val result = session.execute("download_source", JSONObject().put("id", "fixture")) {
                attempts++; error("fixture unavailable")
            }
            assertTrue(result.failed)
            record(session, "failed-$index", result.text)
        }
        assertEquals(1, attempts)
        val stopped = assertFailsWith<IllegalStateException> { record(session, "failed-6", "{}") }
        assertContains(stopped.message.orEmpty(), "避免重复消耗")
        assertContains(stopped.message.orEmpty(), "fixture unavailable")
    }

    @Test fun compactionKeepsJsonAndWholeIdsInsteadOfCuttingArrays() {
        val data = JSONArray()
        repeat(100) { data.put(JSONObject().put("id", "complete-id-$it").put("title", "长标题".repeat(30))) }
        val compact = JSONObject(AssistantToolSession.compact(data.toString(), 1000))
        assertEquals(100, compact.getInt("total"))
        val items = compact.getJSONArray("items")
        assertTrue(items.length() in 1..99)
        repeat(items.length()) { assertEquals("complete-id-$it", items.getJSONObject(it).getString("id")) }
        assertTrue(JSONObject(AssistantToolSession.compact(JSONObject().put("tracks", data).toString(), 1000)).getBoolean("truncated"))
    }
}
