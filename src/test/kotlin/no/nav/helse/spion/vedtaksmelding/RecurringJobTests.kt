package no.nav.helse.spion.vedtaksmelding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import no.nav.helse.utils.RecurringJob
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration

internal class RecurringJobTests {

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    val delay = Duration.ofMillis(100)
    val job = TestRecurringJob(CoroutineScope(testCoroutineDispatcher), delay)

    @Test
    internal fun `StartAsync does job in coroutine and then waits`() {
        testCoroutineDispatcher.pauseDispatcher()

        job.startAsync()
        assertThat(job.getJobCompletedCounter()).isEqualTo(0)

        testCoroutineDispatcher.runCurrent()

        assertThat(job.getJobCompletedCounter()).isEqualTo(1)

        testCoroutineDispatcher.advanceTimeBy(delay.toMillis())
        testCoroutineDispatcher.runCurrent()

        assertThat(job.getJobCompletedCounter()).isEqualTo(2)
    }

    @Test
    internal fun `When job fails and retry is on, ignore errors and run job again`() {
        testCoroutineDispatcher.pauseDispatcher()

        job.failOnJob = true
        job.startAsync(retryOnFail = true)

        testCoroutineDispatcher.runCurrent()

        assertThat(job.getCallCounter()).isEqualTo(1)
        assertThat(job.getJobCompletedCounter()).isEqualTo(0)

        testCoroutineDispatcher.advanceTimeBy(delay.toMillis())
        testCoroutineDispatcher.runCurrent()

        assertThat(job.getCallCounter()).isEqualTo(2)
        assertThat(job.getJobCompletedCounter()).isEqualTo(0)
    }

    @Test
    internal fun `When job fails and retry is off, stop processing`() {
        testCoroutineDispatcher.pauseDispatcher()

        job.failOnJob = true
        job.startAsync(retryOnFail = false)

        testCoroutineDispatcher.runCurrent()

        assertThat(job.getCallCounter()).isEqualTo(1)
        assertThat(job.getJobCompletedCounter()).isEqualTo(0)

        testCoroutineDispatcher.advanceTimeBy(delay.toMillis())
        testCoroutineDispatcher.runCurrent()

        assertThat(job.getCallCounter()).isEqualTo(1)
    }

    @Test
    internal fun `Stopping the job prevents new execution`() {
        testCoroutineDispatcher.pauseDispatcher()

        job.startAsync()
        testCoroutineDispatcher.runCurrent()
        job.stop()

        assertThat(job.getJobCompletedCounter()).isEqualTo(1)

        testCoroutineDispatcher.advanceTimeBy(delay.toMillis())
        testCoroutineDispatcher.runCurrent()

        assertThat(job.getJobCompletedCounter()).isEqualTo(1)
    }

    internal class TestRecurringJob(coroutineScope: CoroutineScope, waitTimeBetweenRuns: Duration) : RecurringJob(coroutineScope, waitTimeBetweenRuns) {
        public var failOnJob: Boolean = false
        private var jobCompletedCounter = 0
        fun getJobCompletedCounter(): Int {
            return jobCompletedCounter
        }

        private var callCounter = 0
        fun getCallCounter(): Int {
            return callCounter
        }

        override fun doJob() {
            callCounter++
            if (failOnJob) throw IOException()
            jobCompletedCounter++
        }
    }
}