package com.resonance.player.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/** Application work has one explicit owner, independent of any native window. */
class ApplicationTasks(context: CoroutineContext) {
    private val job = SupervisorJob(context[Job])
    val failures = kotlinx.coroutines.flow.MutableStateFlow<Throwable?>(null)
    val scope = CoroutineScope(context + job + CoroutineExceptionHandler { _, failure -> failures.value = failure })
    fun close() { job.cancel() }
    suspend fun join() { job.join() }
}

/** Like LaunchedEffect, but shutdown can cancel all application producers before draining writes. */
@Composable
internal fun ApplicationEffect(scope: CoroutineScope, vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    DisposableEffect(scope, *keys) {
        val job = scope.launch(block = block)
        onDispose { job.cancel() }
    }
}
