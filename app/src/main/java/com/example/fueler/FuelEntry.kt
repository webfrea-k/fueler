package com.example.fueler

import com.soywiz.klock.DateTime
import com.soywiz.klock.days
import com.soywiz.klock.months
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class FuelEntry(
    val timestamp: Long = DateTime.nowUnixLong(),
    val odometer: Long,
    val amountOfLiters: Long,
    val priceOfLiter: Double
) {
    companion object{
        fun generateDummySequence(items: Int): MutableList<FuelEntry> {
            val generated = mutableListOf<FuelEntry>()
            var odometerInitial = 10000L
            for (i in 1..items) {
                val odometer = odometerInitial + Random.nextLong(800, 1000) //simulate average kilometers per tank
                generated.add(FuelEntry(
                    timestamp = DateTime.now().minus(10.months).plus((i + Random.nextInt(i, i + 2)).days).unixMillisLong, //generate date, so that last entries are right around now
                    odometer = odometer, //simulate average kilometers per tank
                    amountOfLiters = 70, //Always full tank
                    priceOfLiter = 1.40 + Random.nextDouble(i.toDouble()).div(100) //simulate price variation
                ))
                odometerInitial = odometer

            }
            return generated.sortedBy { it.timestamp }.reversed().toMutableList()
        }
    }
}

