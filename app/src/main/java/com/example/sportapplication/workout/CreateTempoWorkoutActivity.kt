package com.example.sportapplication.workout

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.R

class CreateTempoWorkoutActivity : AppCompatActivity() {
    private lateinit var distanceEditText: EditText
    private lateinit var paceMinutesEditText: EditText
    private lateinit var paceSecondsEditText: EditText
    private lateinit var intervalSpinner: Spinner
    private lateinit var toleranceEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_tempo_workout)

        distanceEditText = findViewById(R.id.distanceEditText)
        paceMinutesEditText = findViewById(R.id.paceMinutesEditText)
        paceSecondsEditText = findViewById(R.id.paceSecondsEditText)
        intervalSpinner = findViewById(R.id.intervalSpinner)
        toleranceEditText = findViewById(R.id.toleranceEditText)

        // Настройка Spinner для интервалов
        val intervals = arrayOf("0,1 км","0,5 км", "1 км", "2 км", "5 км")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        intervalSpinner.adapter = adapter

        // Заполнение полей сохраненными данными
        intent.extras?.let { extras ->
            val distance = extras.getDouble("distance", 0.0)
            val pace = extras.getString("pace", "")
            val interval = extras.getString("interval", "")
            val tolerance = extras.getInt("tolerance", 0)

            if (distance > 0) {
                distanceEditText.setText(distance.toString())
            }
            if (pace.isNotEmpty()) {
                val paceParts = pace.split(":")
                if (paceParts.size == 2) {
                    paceMinutesEditText.setText(paceParts[0])
                    paceSecondsEditText.setText(paceParts[1])
                }
            }
            if (interval.isNotEmpty()) {
                val intervalIndex = intervals.indexOf(interval)
                if (intervalIndex >= 0) {
                    intervalSpinner.setSelection(intervalIndex)
                }
            }
            if (tolerance > 0) {
                toleranceEditText.setText(tolerance.toString())
            }
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            var valid = true

            val distanceStr = distanceEditText.text.toString().trim()
            val distance = distanceStr.toDoubleOrNull()
            if (distance == null || distance <= 0 || distance > 100.0) {
                distanceEditText.error = "Введите дистанцию (0–100 км)"
                valid = false
            } else {
                distanceEditText.error = null
            }

            val minutesStr = paceMinutesEditText.text.toString().trim()
            val secondsStr = paceSecondsEditText.text.toString().trim()
            val minutes = minutesStr.toIntOrNull()
            val seconds = secondsStr.toIntOrNull()

            val totalPaceSeconds = if (minutes != null && seconds != null) (minutes * 60 + seconds) else 0

            if (minutes == null || minutes < 0) {
                paceMinutesEditText.error = "Минуты ≥ 0"
                valid = false
            } else {
                paceMinutesEditText.error = null
            }

            if (seconds == null || seconds !in 0..59) {
                paceSecondsEditText.error = "Секунды: 0–59"
                valid = false
            } else {
                paceSecondsEditText.error = null
            }

            if (totalPaceSeconds == 0) {
                paceMinutesEditText.error = "Темп не может быть 0:00"
                paceSecondsEditText.error = "Темп не может быть 0:00"
                valid = false
            }

            val toleranceStr = toleranceEditText.text.toString().trim()
            val tolerance = toleranceStr.toIntOrNull()
            if (tolerance == null || tolerance <= 0) {
                toleranceEditText.error = "Погрешность должна быть > 0"
                valid = false
            } else {
                toleranceEditText.error = null
            }

            if (!valid) return@setOnClickListener

            val pace = String.format("%d:%02d", minutes!!, seconds!!)
            val interval = intervalSpinner.selectedItem.toString()

            val intent = Intent().apply {
                putExtra("distance", distance)
                putExtra("pace", pace)
                putExtra("interval", interval)
                putExtra("tolerance", tolerance)
            }
            setResult(RESULT_OK, intent)
            finish()
        }

    }
}