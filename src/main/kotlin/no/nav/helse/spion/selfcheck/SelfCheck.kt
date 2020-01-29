package no.nav.helse.spion.selfcheck

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.system.measureTimeMillis

enum class SelfCheckState {OK, ERROR}

data class SelfCheckResult(
        val componentName : String,
        val state : SelfCheckState,
        val runTime : Long,
        val error : Error? = null)

interface SelfCheck {
    /**
     * Kjører en selvdiagnose.
     * Skal gi en exception hvis noe feiler.
     * En kjøring uten exception tolkes som OK.
     */
    fun doSelfCheck()
}

suspend fun runSelfChecks(checkableComponents: Collection<SelfCheck>) : List<SelfCheckResult> {
    return checkableComponents.pmap {
        var runTime = 0L
        try {
            runTime = measureTimeMillis {
                it.doSelfCheck()
            }
            SelfCheckResult(it.javaClass.canonicalName, SelfCheckState.OK, runTime)
        } catch (ex: Error) {
            SelfCheckResult(it.javaClass.canonicalName, SelfCheckState.ERROR, runTime, ex)
        }
    }
}

private suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}