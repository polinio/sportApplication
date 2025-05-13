package com.example.sportapplication.account

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AchievementsActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private lateinit var totalStepsTextView: TextView
    private lateinit var totalCaloriesTextView: TextView
    private lateinit var totalDistanceTextView: TextView
    private lateinit var maxStepsTextView: TextView
    private lateinit var longestRunTextView: TextView
    private lateinit var bestPaceTextView: TextView
    private lateinit var longestRunTimeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        // Инициализация UI элементов
        totalStepsTextView = findViewById(R.id.totalSteps)
        totalCaloriesTextView = findViewById(R.id.totalCalories)
        totalDistanceTextView = findViewById(R.id.totalDistance)
        maxStepsTextView = findViewById(R.id.maxSteps)
        longestRunTextView = findViewById(R.id.longestRun)
        bestPaceTextView = findViewById(R.id.bestPace)
        longestRunTimeTextView = findViewById(R.id.longestRunTime)

        val userId = firebaseAuth.currentUser?.uid ?: return
        database.child("users").child(userId).child("workouts")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalSteps = 0
                    var totalCalories = 0
                    var totalDistance = 0.0
                    var maxSteps = 0
                    var maxStepsDate = ""
                    var longestRun = 0.0
                    var longestRunDate = ""
                    var bestPace = Int.MAX_VALUE
                    var bestPaceDate = ""
                    var longestRunTime = 0L
                    var longestRunTimeDate = ""

                    for (workoutSnapshot in snapshot.children) {
                        val activityType = workoutSnapshot.child("activityType").getValue(String::class.java) ?: ""
                        val steps = workoutSnapshot.child("steps").getValue(Int::class.java) ?: 0
                        val calories = workoutSnapshot.child("calories").getValue(Int::class.java) ?: 0
                        val distance = workoutSnapshot.child("distance").getValue(Double::class.java) ?: 0.0
                        val duration = workoutSnapshot.child("duration").getValue(Long::class.java) ?: 0L
                        val timestamp = workoutSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val avgPace = workoutSnapshot.child("avgPace").getValue(String::class.java) ?: "—"

                        // Общая статистика
                        totalSteps += steps
                        totalCalories += calories
                        totalDistance += distance

                        // Достижения в ходьбе
                        if (activityType == "walking") {
                            if (steps > maxSteps) {
                                maxSteps = steps
                                maxStepsDate = formatDate(timestamp)
                            }
                        }
                        // Достижения в беге
                        else if (activityType == "running") {
                            if (distance > longestRun) {
                                longestRun = distance
                                longestRunDate = formatDate(timestamp)
                            }
                            val paceInSeconds = parsePaceToSeconds(avgPace)
                            if (paceInSeconds < bestPace && paceInSeconds > 0) {
                                bestPace = paceInSeconds
                                bestPaceDate = formatDate(timestamp)
                            }
                            if (duration > longestRunTime) {
                                longestRunTime = duration
                                longestRunTimeDate = formatDate(timestamp)
                            }
                        }
                    }

                    // Отображение общей статистики
                    totalStepsTextView.text = "Всего шагов: $totalSteps"
                    totalCaloriesTextView.text = "Всего калорий: $totalCalories"
                    totalDistanceTextView.text = "Всего расстояние: ${totalDistance.toInt()} км"

                    // Отображение достижений в ходьбе
                    maxStepsTextView.text = if (maxSteps > 0) {
                        "Максимальное количество шагов: $maxSteps ($maxStepsDate)"
                    } else "Максимальное количество шагов: —"

                    // Отображение достижений в беге
                    longestRunTextView.text = if (longestRun > 0) {
                        "Самая длинная пробежка: ${String.format("%.2f", longestRun)} км ($longestRunDate)"
                    } else "Самая длинная пробежка: —"
                    bestPaceTextView.text = if (bestPace != Int.MAX_VALUE) {
                        "Самый лучший темп: ${formatPace(bestPace)} ($bestPaceDate)"
                    } else "Самый лучший темп: —"
                    longestRunTimeTextView.text = if (longestRunTime > 0) {
                        "Самая долгая пробежка: ${formatTime(longestRunTime)} ($longestRunTimeDate)"
                    } else "Самая долгая пробежка: —"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибки
                }
            })
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun parsePaceToSeconds(pace: String): Int {
        if (pace == "—" || pace.isEmpty()) return Int.MAX_VALUE
        val parts = pace.split(":")
        if (parts.size != 2) return Int.MAX_VALUE
        val minutes = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val seconds = parts[1].toIntOrNull() ?: return Int.MAX_VALUE
        return minutes * 60 + seconds
    }

    private fun formatPace(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    private fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}