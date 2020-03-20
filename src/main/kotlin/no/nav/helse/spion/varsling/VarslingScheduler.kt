package no.nav.helse.spion.varsling

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.fixedRateTimer

class VarslingScheduler (
        val intervalSeconds: Long,
        val queue: VarslingQueue,
        val sender: VarslingSender
) {

    private val logger = LoggerFactory.getLogger(VarslingScheduler::class.java)
    var timer: Timer? = null
    private val lock = ReentrantLock()

    fun setup() {
        logger.info("Starter")
        timer = fixedRateTimer(
                name = "Automasjon",
                initialDelay = 0,
                period = intervalSeconds * 1000)
        {
            start()
        }
    }

    fun start() : Int {
        if (lock.isLocked) {
            return -1
        }
        lock.lock()
        queue.check(sender)
        lock.unlock()
        return 0
    }

    fun stop() {
        logger.info("Stopper...")
        timer?.cancel()
        logger.info("Stoppet!")
    }

}
