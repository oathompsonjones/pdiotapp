package com.specknet.pdiotapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogBookActivity : AppCompatActivity() {
    private lateinit var ascendView: TextView
    private lateinit var descendView: TextView
    private lateinit var backView: TextView
    private lateinit var leftView: TextView
    private lateinit var rightView: TextView
    private lateinit var frontView: TextView
    private lateinit var miscView: TextView
    private lateinit var walkView: TextView
    private lateinit var runView: TextView
    private lateinit var shuffleView: TextView
    private lateinit var sitStandView: TextView
    private lateinit var breatheView: TextView
    private lateinit var coughView: TextView
    private lateinit var hyperventilateView: TextView
    private lateinit var otherView: TextView
    private lateinit var dataPickerView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LogBookActivity", "onCreate: here")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logbook)

        ascendView = findViewById(R.id.ascend)
        descendView = findViewById(R.id.descend)
        backView = findViewById(R.id.back)
        leftView = findViewById(R.id.left)
        rightView = findViewById(R.id.right)
        frontView = findViewById(R.id.front)
        miscView = findViewById(R.id.misc)
        walkView = findViewById(R.id.walk)
        runView = findViewById(R.id.run)
        shuffleView = findViewById(R.id.shuffle)
        sitStandView = findViewById(R.id.sitStand)
        breatheView = findViewById(R.id.breathe)
        coughView = findViewById(R.id.cough)
        hyperventilateView = findViewById(R.id.hyperventilate)
        otherView = findViewById(R.id.other)
        dataPickerView = findViewById(R.id.datePicker)

        dataPickerView.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))

        dataPickerView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()
                if (count == 10) dataPickerView.clearFocus()
                if (input.endsWith("\n")) {
                    dataPickerView.setText(input.substring(0, input.length - 1))
                    dataPickerView.clearFocus()
                }
                if (!input.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return
                updateLogbook(input)
            }
        })
    }

    private fun updateLogbook(date: String) {
        // Find all files of the format "yyyy-MM-dd.csv" in the app's files directory
        val files = filesDir.listFiles() ?: return
        val file = files.find { it.name == "$date.csv" }?.readText() ?: "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"
        val data = file.split(",")
        ascendView.text = convertTime(data[0].toInt() * 2)
        descendView.text = convertTime(data[1].toInt() * 2)
        backView.text = convertTime(data[2].toInt() * 2)
        leftView.text = convertTime(data[3].toInt() * 2)
        rightView.text = convertTime(data[4].toInt() * 2)
        frontView.text = convertTime(data[5].toInt() * 2)
        miscView.text = convertTime(data[6].toInt() * 2)
        walkView.text = convertTime(data[7].toInt() * 2)
        runView.text = convertTime(data[8].toInt() * 2)
        shuffleView.text = convertTime(data[9].toInt() * 2)
        sitStandView.text = convertTime(data[10].toInt() * 2)
        breatheView.text = convertTime(data[11].toInt() * 2)
        coughView.text = convertTime(data[12].toInt() * 2)
        hyperventilateView.text = convertTime(data[13].toInt() * 2)
        otherView.text = convertTime(data[14].toInt() * 2)
    }

    private fun convertTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}