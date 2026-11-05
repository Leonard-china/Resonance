package com.resonance.player.application

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One shutdown transaction: drain accepted writes before releasing native services. */
class ApplicationLifetime(private val releasePlatform: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val closing = MutableStateFlow<Job?>(null)
    private val stops = linkedMapOf<String, () -> Unit>()
    private val stoppingState = MutableStateFlow(false)
    val stopping: StateFlow<Boolean> get() = stoppingState
    private val completions = linkedMapOf<String, suspend () -> Unit>()
    private val drains = linkedMapOf<String, suspend () -> Unit>()
    private val failures = MutableStateFlow<List<Throwable>>(emptyList())
    val shutdownFailures: StateFlow<List<Throwable>> get() = failures
    val isClosing: Boolean get() = closing.value != null

    /** Register on the application dispatcher, before accepting window input. */
    fun register(name: String, drain: suspend () -> Unit) {
        check(!isClosing) { "Application is closing" }
        drains[name] = drain
    }
    fun registerStop(name: String, stop: () -> Unit) {
        check(!isClosing) { "Application is closing" }; stops[name] = stop
    }
    fun registerCompletion(name: String, completion: suspend () -> Unit) {
        check(!isClosing) { "Application is closing" }; completions[name] = completion
    }
    fun close(): Job {
        closing.value?.let { return it }
        val producers = stops.values.toList()
        val settled = completions.values.toList()
        val accepted = drains.values.toList()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                for (stop in producers) try { stop() }
                catch (failure: Exception) { failures.value += failure }
                for (completion in settled) try { completion() }
                catch (failure: Exception) { failures.value += failure }
                for (drain in accepted) try { drain() }
                catch (failure: Exception) { failures.value += failure }
                try { releasePlatform() }
                catch (failure: Exception) { failures.value += failure }
            } finally { scope.cancel() }
        }
        if (closing.compareAndSet(null, job)) { stoppingState.value = true; job.start(); return job }
        job.cancel()
        return checkNotNull(closing.value)
    }
}
