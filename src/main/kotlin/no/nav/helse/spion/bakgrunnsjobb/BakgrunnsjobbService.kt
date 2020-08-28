package no.nav.helse.spion.bakgrunnsjobb

import java.time.LocalDateTime

class BakgrunnsjobbService(
        val bakgrunnsjobbRepository: BakgrunnsjobbRepository
) {

    fun sjekkOgProsseserVentendeBakgrunnsjobber() {
        finnVentende()
                .forEach(this::prosesser)
    }

    private fun prosesser(jobb: Bakgrunnsjobb){
        jobb.behandlet = LocalDateTime.now()
        jobb.forsoek++

        try{
            //bla bla
        } finally {
            bakgrunnsjobbRepository.update(jobb)
        }
    }


    fun finnVentende(): List<Bakgrunnsjobb> =
            bakgrunnsjobbRepository.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now(), setOf(BakgrunnsjobbStatus.OPPRETTET, BakgrunnsjobbStatus.FEILET))

}
