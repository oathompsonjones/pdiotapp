package com.specknet.pdiotapp

import com.specknet.pdiotapp.utils.RespeckAnalysis
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class RespeckAnalysisUnitTest {
    @Test
    fun readCSV() {
        val respeckAnalysis = RespeckAnalysis(null)
        val data = respeckAnalysis.readCSV("/Users/oathompsonjones/Documents/Edinburgh/Year-4/PDIoTS/pdiotapp/app/src/main/java/com/specknet/pdiotapp/trainingdata/PDIoT2324/Respeck/DailyActivities/ascending/s1_respeck_ascending_breathingNormal.csv")
        assert(data != null)
    }
}