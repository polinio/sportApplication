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
    private lateinit var paceEditText: EditText
    private lateinit var intervalSpinner: Spinner
    private lateinit var toleranceEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_tempo_workout)

        distanceEditText = findViewById(R.id.distanceEditText)
        paceEditText = findViewById(R.id.paceEditText)
        intervalSpinner = findViewById(R.id.intervalSpinner)
        toleranceEditText = findViewById(R.id.toleranceEditText)

        // Настройка Spinner для интервалов
        val intervals = arrayOf("1 км", "2 км", "5 км")
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
                paceEditText.setText(pace)
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
            val distance = distanceEditText.text.toString().toDoubleOrNull()
            val pace = paceEditText.text.toString()
            val interval = intervalSpinner.selectedItem.toString()
            val tolerance = toleranceEditText.text.toString().toIntOrNull()

            // Валидация ввода
            if (distance == null || distance <= 0) {
                Toast.makeText(this, "Введите корректную дистанцию (> 0 км)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!pace.matches(Regex("\\d+:\\d{2}"))) {
                Toast.makeText(this, "Введите темп в формате мин:сек (например, 5:30)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (tolerance == null || tolerance < 0) {
                Toast.makeText(this, "Введите корректную погрешность (>= 0 сек)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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