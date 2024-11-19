package com.specknet.pdiotapp

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LogBookActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LogBookActivity", "onCreate: here")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logbook)
        val layout = findViewById<LinearLayout>(R.id.logbook_list)

        // Find all files of the format "yyyy-MM-dd_test.csv" in the app's files directory
        val files = filesDir.listFiles() ?: return
        for (file in files) {
            if (!file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}.csv"))) continue
            val dateView = TextView(this)
            dateView.text = file.name.slice(0..9)
            layout.addView(dateView)

            // upStair,downStair,lieBack,lieLeft,lieRight,lieFront,miscMove,walk,run,shuffle,sitStand,breathe,cough,hyperventilate,other
            val logbookView = TextView(this)
            val data = file.readText().split(",")
            logbookView.text = buildString {
                append("Time spent ascending stairs: ${data[0].toInt() * 2} seconds\n")
                append("Time spent descending stairs: ${data[1].toInt() * 2} seconds\n")
                append("Time spent lying on your back: ${data[2].toInt() * 2} seconds\n")
                append("Time spent lying on your left side: ${data[3].toInt() * 2} seconds\n")
                append("Time spent lying on your right side: ${data[4].toInt() * 2} seconds\n")
                append("Time spent lying on your front: ${data[5].toInt() * 2} seconds\n")
                append("Time spent in miscellaneous movements: ${data[6].toInt() * 2} seconds\n")
                append("Time spent walking: ${data[7].toInt() * 2} seconds\n")
                append("Time spent running: ${data[8].toInt() * 2} seconds\n")
                append("Time spent shuffle walking: ${data[9].toInt() * 2} seconds\n")
                append("Time spent sitting down or standing up: ${data[10].toInt() * 2} seconds\n")
                append("Time spent breathing normally: ${data[11].toInt() * 2} seconds\n")
                append("Time spent coughing: ${data[12].toInt() * 2} seconds\n")
                append("Time spent hyperventilating: ${data[13].toInt() * 2} seconds\n")
                append("Time spent talking, singing, laughing or eating: ${data[14].toInt() * 2} seconds\n")
            }
            layout.addView(logbookView)
        }
    }
}