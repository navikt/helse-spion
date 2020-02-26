package no.nav.helse.spion.vedtaksmelding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.time.Duration

internal class ScheduledJobTest {

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    val delay = Duration.ofMillis(100)
    val job = DummyScheduledJob(CoroutineScope(testCoroutineDispatcher), delay)

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
    internal fun `When job fails and retry is on, retry forever without throwing`() {
        testCoroutineDispatcher.pauseDispatcher()

        job.failOnJob = true
        job.startAsync(retryOnFail = true)

        testCoroutineDispatcher.runCurrent()

        assertThat(job.getJobCompletedCounter()).isEqualTo(0)
    }

    @Test
    internal fun `When job fails and retry is off, throw given exception`() {
        testCoroutineDispatcher.pauseDispatcher()

        job.failOnJob = true
        job.startAsync(retryOnFail = false)

        assertThrows<IOException> { testCoroutineDispatcher.runCurrent() }

        assertThat(job.getJobCompletedCounter()).isEqualTo(0)
    }


    internal class DummyScheduledJob(coroutineScope: CoroutineScope, waitTimeBetweenRuns: Duration) : ScheduledJob(coroutineScope, waitTimeBetweenRuns) {
        public var failOnJob: Boolean = false
        private var jobCounter = 0
        fun getJobCompletedCounter(): Int {
            return jobCounter
        }

        override fun doJob() {
            if (failOnJob) throw IOException()

            jobCounter++
        }
    }
}