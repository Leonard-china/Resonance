package com.resonance.player

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*

/** Explicitly opt in on a separate validation application ID; no user music is opened. */
@RunWith(AndroidJUnit4::class)
class ApplicationLifecycleDeviceTest {
    @Test fun repeatedRecreationAndShutdownKeepTheMainLooperResponsive() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        assumeTrue("Requires the isolated refactor validation package",
            InstrumentationRegistry.getArguments().getString("refactorLifecycle") == "true" &&
                packageName.endsWith(".validation20261105refactor"))
        repeat(3) {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                scenario.onActivity { activity -> assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
                scenario.recreate()
                scenario.onActivity { activity -> assertSame(Looper.getMainLooper(), Looper.myLooper()); assertFalse(activity.isFinishing) }
            } finally { scenario.close() }
            Thread.sleep(250)
            instrumentation.waitForIdleSync()
            val heartbeat = CountDownLatch(1)
            Handler(Looper.getMainLooper()).post { heartbeat.countDown() }
            assertTrue("The main looper must survive native service cleanup", heartbeat.await(3, TimeUnit.SECONDS))
        }
    }
}
