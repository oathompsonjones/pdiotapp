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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
    private lateinit var avtivityClassificationView: TextView
    private lateinit var breathingClassificationView: TextView
    private lateinit var tfliteRespeckBreathing: Interpreter
    private lateinit var tfliteRespeckActivities: Interpreter
    private lateinit var tfliteThingyActivities: Interpreter

    private val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    private val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    private val respeckFrames: ArrayList<FloatArray> = ArrayList()
    private val thingyFrames: ArrayList<FloatArray> = ArrayList()

    private var predicationThingy: Float? = null
    private var predicationRespeck: Float? = null

    private var respeckBreathingOutputIndex: Int? = null
    private var respeckActivitiesOutputIndex: Int? = null
    private var thingyActivitiesOutputIndex: Int? = null

    private val activities = listOf(
        "Ascending stairs",
        "Descending stairs",
        "Lying on your back",
        "Lying on your left",
        "Lying on your right",
        "Lying on your front",
        "Miscellaneous movement",
        "Walking normally",
        "Running",
        "Shuffle walking",
        "Sitting/standing",
        "Error 404: Movement not found"
    )
    private val breathing = listOf(
        "Breathing normally",
        "Coughing",
        "Hyperventilating",
        "Other (e.g. talking, singing, laughing, eating)",
        "Error 404: Breathing not found"
    )

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
                    Log.d("Live", "onReceive: liveData = $liveData")

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("respeck", x, y, z, "Ascending")

                    respeckFrames.add(floatArrayOf(liveData.accelX, liveData.accelY, liveData.accelZ, liveData.gyro.x, liveData.gyro.y, liveData.gyro.z))
                    if (respeckFrames.size > 50) {
                        respeckFrames.removeAt(0)

                        if (time % 50 == 0f) {
                            classify(respeckFrames.toTypedArray(), Model.RESPECK_BREATHING)
                            predicationRespeck = classify(respeckFrames.toTypedArray(), Model.RESPECK_ACTIVITIES)
                            runOnUiThread {
                                updateBreathingClassificationOutput(respeckBreathingOutputIndex)
                                Log.d("Classification", "Frame: $time")
                            }
                            compareActivityModels(predicationThingy, predicationRespeck)
                            saveClassification(respeckBreathingOutputIndex, true)
                        }
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
                    val liveData = intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = $liveData")

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("thingy", x, y, z, "Ascending")

                    thingyFrames.add(floatArrayOf(liveData.accelX, liveData.accelY, liveData.accelZ, liveData.gyro.x, liveData.gyro.y, liveData.gyro.z))
                    if (thingyFrames.size > 50) {
                        thingyFrames.removeAt(0)
                        if (time % 50 == 0f)
                            predicationThingy = classify(thingyFrames.toTypedArray(), Model.THINGY_ACTIVITIES)
                    }
                }

            }
        }

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
        avtivityClassificationView = findViewById(R.id.ActivityClassification)
        breathingClassificationView = findViewById(R.id.BreathingClassification)

        var file = assets.openFd("respeck-breathing_128-16.tflite")
        tfliteRespeckBreathing = Interpreter(FileInputStream(file.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, file.startOffset, file.declaredLength))

        file = assets.openFd("respeck-activities_128-16.tflite")
        tfliteRespeckActivities = Interpreter(FileInputStream(file.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, file.startOffset, file.declaredLength))

        file = assets.openFd("thingy_128-16.tflite")
        tfliteThingyActivities = Interpreter(FileInputStream(file.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, file.startOffset, file.declaredLength))
    }

    private fun classify(data: Array<FloatArray>, model: Model): Float? {
        val inputArray = arrayOf(data)
        val outputArray: Array<FloatArray>

        when (model) {
            Model.RESPECK_BREATHING -> {
                outputArray = Array(1) { FloatArray(4) }
                tfliteRespeckBreathing.run(inputArray, outputArray)
                respeckBreathingOutputIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] }
            }
            Model.RESPECK_ACTIVITIES -> {
                outputArray = Array(1) { FloatArray(11) }
                tfliteRespeckActivities.run(inputArray, outputArray)
                respeckActivitiesOutputIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] }
            }
            Model.THINGY_ACTIVITIES -> {
                outputArray = Array(1) { FloatArray(11) }
                tfliteThingyActivities.run(inputArray, outputArray)
                thingyActivitiesOutputIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] }
            }
        }

        Log.d("Classification", model.toString())
        Log.d("Classification", "classify: ${outputArray[0].contentToString()}")
        Log.d("Classification" , "classify: ${outputArray[0].maxByOrNull { it } }")
        Log.d("Classification" , "classify: ${outputArray.indices.maxByOrNull { outputArray[0][it] }}")

        return outputArray[0].maxByOrNull { it }
    }

    private fun saveClassification(prediction: Int?, isBreathingData: Boolean) {
        if (prediction == null) return

        // Get the current date
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Check if a file exists for today
        val files = filesDir.listFiles()
        var file = files?.find { it.name == "${date}.csv" }
        if (file == null) {
            file = filesDir.resolve("${date}.csv")
            file.createNewFile()
            file.appendText("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0")
        }

        // Update the file with the new classification
        // FILE FORMAT: upStair,downStair,lieBack,lieLeft,lieRight,lieFront,miscMove,walk,run,shuffle,sitStand,breathe,cough,hyperventilate,other
        val currentData = file.readText().split(",")
        val index = if (isBreathingData) prediction + 11 else prediction
        var data = ""
        for (i in currentData.indices) {
            data += if (i == index) (currentData[i].toInt() + 1).toString() else currentData[i]
            data += if (i == currentData.size - 1) "" else ","
        }
        file.writeText(data)
    }

    private fun updateActivityClassificationOutput(predication: Int?) {
        avtivityClassificationView.text = activities[predication ?: 11]
    }

    private fun updateBreathingClassificationOutput(predication: Int?) {
        breathingClassificationView.text = breathing[predication ?: 4]
    }

    private fun compareActivityModels(predicationThingy: Float?, predicationRespeck: Float?) {
        if ((predicationThingy ?: 0f) >= (predicationRespeck ?: 0f)) {
            runOnUiThread {
                updateActivityClassificationOutput(thingyActivitiesOutputIndex)
            }
            saveClassification(thingyActivitiesOutputIndex, false)
        } else {
            runOnUiThread {
                updateActivityClassificationOutput(respeckActivitiesOutputIndex)
            }
            saveClassification(respeckActivitiesOutputIndex, false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
        looperThingy.quit()
        tfliteRespeckBreathing.close()
        tfliteRespeckActivities.close()
        tfliteThingyActivities.close()
    }

    private enum class Model {
        RESPECK_BREATHING,
        RESPECK_ACTIVITIES,
        THINGY_ACTIVITIES
    }
}
