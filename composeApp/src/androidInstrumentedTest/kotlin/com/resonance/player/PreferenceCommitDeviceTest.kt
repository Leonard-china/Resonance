package com.resonance.player

import android.content.SharedPreferences
import com.resonance.player.platform.commitChecked
import com.resonance.player.platform.withPreferenceRollback
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.lang.reflect.Proxy
import org.junit.Test
import org.junit.Assert.*

class PreferenceCommitDeviceTest {
    @Test fun failedImportRestoresEveryPreferenceStoreAndTypedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val names = listOf("qa-rollback-one", "qa-rollback-two")
        val prefs = names.map { context.getSharedPreferences(it, Context.MODE_PRIVATE) }
        try {
            prefs[0].edit().clear().putString("playlist", "original").putStringSet("ids", setOf("a", "b")).commitChecked("fixture")
            prefs[1].edit().clear().putLong("revision", 7).putBoolean("enabled", true).commitChecked("fixture")
            val before = prefs.map { it.all }
            val error = runCatching {
                withPreferenceRollback(prefs) {
                    prefs.forEach { it.edit().clear().putString("new", "replacement").commitChecked("fixture") }
                    error("simulated import failure")
                }
            }.exceptionOrNull()
            assertEquals("simulated import failure", error?.message)
            assertEquals(before, prefs.map { it.all })
        } finally { names.forEach(context::deleteSharedPreferences) }
    }
    @Test fun failedSynchronousCommitIsReportedRatherThanAccepted() {
        var attempts = 0
        val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { _, method, _ ->
            if (method.name == "commit") { attempts++; false } else null
        } as SharedPreferences.Editor
        val error = runCatching { editor.commitChecked("fixture save failure") }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertEquals("fixture save failure", error?.message)
        assertEquals(1, attempts)
    }
}
