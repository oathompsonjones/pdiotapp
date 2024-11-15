package com.specknet.pdiotapp.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel


class LiveDataActivity : AppCompatActivity() {

    // global graph variables
    private lateinit var datasetResAccelX: LineDataSet
    private lateinit var datasetResAccelY: LineDataSet
    private lateinit var datasetResAccelZ: LineDataSet

    private lateinit var datasetThingyAccelX: LineDataSet
    private lateinit var datasetThingyAccelY: LineDataSet
    private lateinit var datasetThingyAccelZ: LineDataSet

    var time = 0f
    private lateinit var allRespeckData: LineData

    private lateinit var allThingyData: LineData

    private lateinit var respeckChart: LineChart
    private lateinit var thingyChart: LineChart

    // global broadcast receiver so we can unregister it
    private lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    private lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    private lateinit var looperRespeck: Looper
    private lateinit var looperThingy: Looper

    // classification variables
    private lateinit var outputView: TextView
    private lateinit var tflite: Interpreter

    private val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    private val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    private val respeckFrames: ArrayList<FloatArray> = ArrayList()
    private val thingyFrames: ArrayList<FloatArray> = ArrayList()

    private val predicationThingy: FloatArray = FloatArray(26)
    private val predicationRespeck: FloatArray = FloatArray(26)

    private var respeckOutputNumber: Int? = null
    private var thingyOutputNumber: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        setupCharts()

        setupClassification()

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {
                    val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("respeck", x, y, z, "Ascending")

                    respeckFrames.add(floatArrayOf(liveData.accelX, liveData.accelY, liveData.accelZ, liveData.gyro.x, liveData.gyro.y, liveData.gyro.z))
                    if (respeckFrames.size > 50) {
                        respeckFrames.removeAt(0)

                        val predicationRespeck = classify(respeckFrames.toTypedArray(), "Respeck")
//                        if (predication != null) {
//                            if (predication < 15) {
//                                runOnUiThread {
//                                    updateClassificationOutput(predication)
//                                }
//                            }
//                        }
                    }
                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)

        // set up the broadcast receiver
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent): Unit {
                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {
                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("thingy", x, y, z, "Ascending")

                    thingyFrames.add(
                        floatArrayOf(
                            liveData.accelX,
                            liveData.accelY,
                            liveData.accelZ,
                            liveData.gyro.x,
                            liveData.gyro.y,
                            liveData.gyro.z
                        )
                    )
                    if (thingyFrames.size > 50) {
                        thingyFrames.removeAt(0)

                        val predicationThingy = classify(thingyFrames.toTypedArray(), "Thingy")

//                        runOnUiThread {
//                            updateClassificationOutput(predicationThingy)
//                        }
                    }
                }

            }
        }

        compareModels(predicationThingy, predicationRespeck)


        // register receiver on another thread
        val handlerThreadThingy = HandlerThread("bgThreadThingyLive")
        handlerThreadThingy.start()
        looperThingy = handlerThreadThingy.looper
        val handlerThingy = Handler(looperThingy)
        this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)
    }

    private fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)
        thingyChart = findViewById(R.id.thingy_chart)

        // Respeck

        time = 0f
        val entriesResAccelX = ArrayList<Entry>()
        val entriesResAccelY = ArrayList<Entry>()
        val entriesResAccelZ = ArrayList<Entry>()

        datasetResAccelX = LineDataSet(entriesResAccelX, "Accel X")
        datasetResAccelY = LineDataSet(entriesResAccelY, "Accel Y")
        datasetResAccelZ = LineDataSet(entriesResAccelZ, "Accel Z")

        datasetResAccelX.setDrawCircles(false)
        datasetResAccelY.setDrawCircles(false)
        datasetResAccelZ.setDrawCircles(false)

        datasetResAccelX.setColor(ContextCompat.getColor(this, R.color.red))
        datasetResAccelY.setColor(ContextCompat.getColor(this, R.color.green))
        datasetResAccelZ.setColor(ContextCompat.getColor(this, R.color.blue))

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(datasetResAccelX)
        dataSetsRes.add(datasetResAccelY)
        dataSetsRes.add(datasetResAccelZ)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()

        // Thingy

        time = 0f
        val entriesThingyAccelX = ArrayList<Entry>()
        val entriesThingyAccelY = ArrayList<Entry>()
        val entriesThingyAccelZ = ArrayList<Entry>()

        datasetThingyAccelX = LineDataSet(entriesThingyAccelX, "Accel X")
        datasetThingyAccelY = LineDataSet(entriesThingyAccelY, "Accel Y")
        datasetThingyAccelZ = LineDataSet(entriesThingyAccelZ, "Accel Z")

        datasetThingyAccelX.setDrawCircles(false)
        datasetThingyAccelY.setDrawCircles(false)
        datasetThingyAccelZ.setDrawCircles(false)

        datasetThingyAccelX.setColor(ContextCompat.getColor(this, R.color.red))
        datasetThingyAccelY.setColor(ContextCompat.getColor(this, R.color.green))
        datasetThingyAccelZ.setColor(ContextCompat.getColor(this, R.color.blue))

        val dataSetsThingy = ArrayList<ILineDataSet>()
        dataSetsThingy.add(datasetThingyAccelX)
        dataSetsThingy.add(datasetThingyAccelY)
        dataSetsThingy.add(datasetThingyAccelZ)

        allThingyData = LineData(dataSetsThingy)
        thingyChart.data = allThingyData
        thingyChart.invalidate()
    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float, classifier: String) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            datasetResAccelX.addEntry(Entry(time, x))
            datasetResAccelY.addEntry(Entry(time, y))
            datasetResAccelZ.addEntry(Entry(time, z))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
            }
        } else if (graph == "thingy") {
            datasetThingyAccelX.addEntry(Entry(time, x))
            datasetThingyAccelY.addEntry(Entry(time, y))
            datasetThingyAccelZ.addEntry(Entry(time, z))

            runOnUiThread {
                allThingyData.notifyDataChanged()
                thingyChart.notifyDataSetChanged()
                thingyChart.invalidate()
                thingyChart.setVisibleXRangeMaximum(150f)
                thingyChart.moveViewToX(thingyChart.lowestVisibleX + 40)
            }
        }


    }

    private fun setupClassification() {
        outputView = findViewById(R.id.ActivityClassification)

        val file = assets.openFd("model.tflite")
        tflite = Interpreter(FileInputStream(file.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, file.startOffset, file.declaredLength))
    }

    private val respiratoryIndices = listOf(11, 12, 13, 14)

    private fun classify(data: Array<FloatArray>, respeckOrThingy: String): Float? {
        val inputArray = arrayOf(data)
        val outputArray = Array(1) { FloatArray(26) }

        tflite.run(inputArray, outputArray)

        if (respeckOrThingy == "Respeck") {
            respeckOutputNumber = outputArray.indices.maxByOrNull { it }
        } else {
            thingyOutputNumber = outputArray.indices.maxByOrNull { it }
        }

        Log.d("Classification" , "classify: ${outputArray[0].maxByOrNull { it } }")
        Log.d("Classification" , "classify: ${outputArray.indices.maxByOrNull { it }}")

        return outputArray[0].maxByOrNull { it }
    }


    private fun updateClassificationOutput(predication: Int?) {
        outputView.text = buildString {
            append("Activity Classification: ")
//            append(predication.toString())
//            append(" - ")
            append(listOf(
                "ascending",
                "descending",
                "lyingBack",
                "lyingLeft",
                "lyingRight",
                "lyingStomach",
                "miscMovement",
                "normalWalking",
                "running",
                "shuffleWalking",
                "sittingStanding",
                "breathingNormally",
                "oughing",
                "hyperventilation",
                "other",
                "ascending",
                "descending",
                "lyingBack",
                "lyingLeft",
                "lyingRight",
                "lyingStomach",
                "miscMovement",
                "normalWalking",
                "running",
                "shuffleWalking",
                "sittingStanding",
                "FAIL"
            )[predication ?: 26])
        }
    }


    private fun compareModels(predicationThingy: FloatArray, predicationRespeck: FloatArray) {
        if ((predicationThingy.maxOrNull() ?: 0f) >= (predicationRespeck.maxOrNull() ?: 0f)) {
            runOnUiThread {
                updateClassificationOutput(thingyOutputNumber)
            }
        } else {
            runOnUiThread {
                updateClassificationOutput(respeckOutputNumber)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
        looperThingy.quit()
        tflite.close()
    }
}
