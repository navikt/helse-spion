package no.nav.helse.slowtests.db

import no.nav.helse.spion.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.spion.bakgrunnsjobb.BakgrunnsjobbStatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class PostgresBakgrunnsjobbRepositoryTest {

    @Test
    fun `lagre og hente opp igjen bakgrunnsjobb`() {
        val bakgrunnsjobb = Bakgrunnsjobb(
                UUID.randomUUID(),
                "test",
                LocalDateTime.now(),
                LocalDateTime.now(),
                BakgrunnsjobbStatus.OPPRETTET,
                LocalDateTime.now(),
                0,
                3,
                ""
        )
    }
}
