package no.nav.helse

import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.litote.kmongo.*
import kotlin.system.measureTimeMillis


class MongoTests {
    // For å kjøre denne må du ha en mongodb kjørende:
    // docker run -p 27017:27017 mongo:3.4-xenial


    @Test
    @Ignore
    internal fun insertDocuments() {
        val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
        val database = client.getDatabase("test") //normal java driver usage
        val col = database.getCollection<Ytelsesperiode>() //KMongo extension method

        // insert i bolker av 100 dokumenter 10000 ganger
        val bulkSize = 100
        val numBulks = 10000
        val generator = YtelsesperiodeGenerator(500, 50000)
        println("Starter inserts")
        var totalInsertTime = 0L

        for (i in 0..numBulks) {
            val ytelsesperioder = generator.take(bulkSize)

            val time = measureTimeMillis { col.insertMany(ytelsesperioder) }

            totalInsertTime += time
            println("Spent $time ms inserting $bulkSize docs. Time: ")
        }

        println("Done inserting " + numBulks * bulkSize + " items in $totalInsertTime ms")
    }

}
