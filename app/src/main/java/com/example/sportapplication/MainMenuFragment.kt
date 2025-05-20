package com.example.sportapplication

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.sportapplication.analytics.AnalyticsActivity
import com.example.sportapplication.analytics.WorkoutHistoryActivity
import android.util.Log
import com.example.sportapplication.data.Workout
import com.example.sportapplication.data.WorkoutDTO
import com.example.sportapplication.data.toWorkout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class MainMenuFragment : Fragment() {

    private lateinit var stepsTextView: TextView
    private lateinit var lastWorkoutTextView: TextView
    private lateinit var welcomeText: TextView
    private lateinit var dateTimeText: TextView
    private lateinit var totalTrainingTime: TextView
    private lateinit var trendsMessage: TextView
    private lateinit var trendsChart: BarChart
    private lateinit var goalsContainer: LinearLayout

    private val sharedPreferences by lazy { requireContext().getSharedPreferences("UserDataPrefs", MODE_PRIVATE) }
    private lateinit var stepsProgress: ProgressBar
    private lateinit var stepsValue: TextView
    private lateinit var stepsUnit: TextView
    private lateinit var caloriesProgress: ProgressBar
    private lateinit var caloriesValue: TextView
    private lateinit var caloriesUnit: TextView
    private lateinit var activityProgress: ProgressBar
    private lateinit var activityValue: TextView
    private lateinit var activityUnit: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stepsTextView = view.findViewById(R.id.steps)
        lastWorkoutTextView = view.findViewById(R.id.lastWorkout)
        welcomeText = view.findViewById(R.id.welcomeText)
        dateTimeText = view.findViewById(R.id.dateTimeText)
        totalTrainingTime = view.findViewById(R.id.totalTrainingTime)
        trendsMessage = view.findViewById(R.id.trendsMessage)
        trendsChart = view.findViewById(R.id.trendsChart)
        goalsContainer = view.findViewById(R.id.goalsContainer)

        val block1 = view.findViewById<LinearLayout>(R.id.block1)
        block1.setOnClickListener {
            val intent = Intent(requireContext(), AnalyticsActivity::class.java)
            startActivity(intent)
        }

        val block2 = view.findViewById<LinearLayout>(R.id.block2)
        block2.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutHistoryActivity::class.java)
            startActivity(intent)
        }

        // Add click listener for info icon
        val infoIcon = view.findViewById<ImageView>(R.id.infoIcon)
        infoIcon.setOnClickListener {
            showWhoGuidelinesDialog()
        }

        setupWelcomeMessage()
        setupActivityCalendar()
        loadLastWorkout()
    }

    private fun setupWelcomeMessage() {
        val name = sharedPreferences.getString("name", "Пользователь") ?: "Пользователь"
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..21 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
        welcomeText.text = "$greeting, $name!"
    }

    private fun setupActivityCalendar() {
        dateTimeText.text = SimpleDateFormat("HH:mm 'Посл. 7 дней'", Locale.getDefault()).format(Date())

        loadWorkoutsForLast7Days()

        val goals = listOf(
            Goal("Шаги", 8000, R.drawable.ic_walk),
            Goal("Калории", 300, R.drawable.ic_calories),
            Goal("Активность", 30, R.drawable.ic_clock)
        )
        goals.forEachIndexed { index, goal ->
            val goalView = layoutInflater.inflate(R.layout.item_goal_circle, goalsContainer, false)
            val icon = goalView.findViewById<ImageView>(R.id.goalIcon)
            val progress = goalView.findViewById<ProgressBar>(R.id.goalProgress)
            val valueText = goalView.findViewById<TextView>(R.id.goalValue)
            val unitText = goalView.findViewById<TextView>(R.id.goalUnit)

            icon.setImageResource(goal.icon)
            progress.max = goal.target

            when (index) {
                0 -> {
                    stepsProgress = progress
                    stepsValue = valueText
                    stepsUnit = unitText
                    stepsUnit.text = "шагов"
                    stepsValue.text = "0"
                }
                1 -> {
                    caloriesProgress = progress
                    caloriesValue = valueText
                    caloriesUnit = unitText
                    caloriesUnit.text = "калорий"
                    caloriesValue.text = "0"
                }
                2 -> {
                    activityProgress = progress
                    activityValue = valueText
                    activityUnit = unitText
                    activityUnit.text = "минут"
                    activityValue.text = "0"
                }
            }

            goalsContainer.addView(goalView)
        }

        loadTodayWorkoutsForGoals()
    }

    private fun loadTodayWorkoutsForGoals() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val workoutsRef = FirebaseDatabase.getInstance().getReference("users/$userId/workouts")

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        workoutsRef.orderByChild("timestamp").startAt(startOfDay.toDouble()).endAt(endOfDay.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val workouts = snapshot.children.mapNotNull { it.getValue(WorkoutDTO::class.java)?.toWorkout() }
                    val totalSteps = workouts.sumOf { it.steps }.toInt()
                    val totalCalories = workouts.sumOf { it.calories }.toInt()
                    val totalMinutes = (workouts.sumOf { it.duration } / 60000).toInt()

                    stepsProgress.progress = totalSteps
                    stepsValue.text = "$totalSteps"

                    caloriesProgress.progress = totalCalories
                    caloriesValue.text = "$totalCalories"

                    activityProgress.progress = totalMinutes
                    activityValue.text = "$totalMinutes"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainMenuFragment", "Failed to load today workouts: ${error.message}", error.toException())
                }
            })
    }

    private fun loadWorkoutsForLast7Days() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val workoutsRef = FirebaseDatabase.getInstance().getReference("users/$userId/workouts")

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.timeInMillis

        workoutsRef.orderByChild("timestamp").startAt(startDate.toDouble()).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val workouts = snapshot.children.mapNotNull { it.getValue(WorkoutDTO::class.java)?.toWorkout() }
                updateTrendsChart(workouts)
            }

            override fun onCancelled(error: DatabaseError) {
                trendsMessage.text = "Ошибка загрузки данных"
                Log.e("MainMenuFragment", "Failed to load workouts: ${error.message}", error.toException())
            }
        })
    }

    private fun updateTrendsChart(workouts: List<Workout>) {
        val dataByDay = mutableMapOf<String, Float>()
        val days = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val sdf = SimpleDateFormat("E", Locale("ru"))
        var totalHours = 0f
        for (i in 0..6) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis

            val dayWorkouts = workouts.filter { it.timestamp in dayStart..dayEnd }
            val hours = dayWorkouts.sumOf { it.duration } / 3600000f
            totalHours += hours

            val dayLabel = when (sdf.format(Date(dayStart)).lowercase()) {
                "пн" -> "Пн"
                "вт" -> "Вт"
                "ср" -> "Ср"
                "чт" -> "Чт"
                "пт" -> "Пт"
                "сб" -> "Сб"
                "вс" -> "Вс"
                else -> "?"
            }
            days.add(dayLabel)
            dataByDay[dayLabel] = hours
        }

        val formattedTotalHours = ceil(totalHours * 10) / 10
        totalTrainingTime.text = "$formattedTotalHours ч"

        val entries = dataByDay.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value)
        }
        val dataSet = BarDataSet(entries, "Часы тренировок").apply {
            color = resources.getColor(R.color.blue_500, null)
            setDrawValues(false)
        }
        val barData = BarData(dataSet).apply { barWidth = 0.4f }

        trendsChart.apply {
            data = if (entries.isNotEmpty()) barData else null
            description.isEnabled = false
            setNoDataText("Нет данных за этот период")
            legend.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(days)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun loadLastWorkout() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            lastWorkoutTextView.text = "Войдите, чтобы увидеть тренировки"
            return
        }

        val workoutsRef = FirebaseDatabase.getInstance().getReference("users/$userId/workouts")
        workoutsRef.orderByChild("timestamp").limitToLast(1).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val workoutDTO = snapshot.children.firstOrNull()?.getValue(WorkoutDTO::class.java)
                    if (workoutDTO != null) {
                        val workout = workoutDTO.toWorkout()
                        val activityType = formatActivityType(workout.activityType)
                        val date = formatDate(workout.timestamp)
                        val distance = formatDistance(workout.distance)
                        lastWorkoutTextView.text = "Последняя тренировка: $activityType, $date, $distance"
                        Log.d("MainMenuFragment", "Loaded workoutDTO: $workoutDTO")
                        Log.d("MainMenuFragment", "Converted to workout: $workout")
                    } else {
                        lastWorkoutTextView.text = "Начните первую тренировку!"
                        Log.w("MainMenuFragment", "No workout data found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainMenuFragment", "Failed to load last workout: ${error.message}", error.toException())
                    lastWorkoutTextView.text = "Ошибка загрузки тренировки"
                }
            })
    }

    private fun formatActivityType(activityType: String?): String {
        return when (activityType) {
            "running" -> "Бег"
            "cycling" -> "Велосипед"
            else -> "Ходьба"
        }
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMMM HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Неизвестная дата"
        }
    }

    private fun formatDistance(distance: Double): String {
        return String.format("%.2f км", distance)
    }

    private fun showWhoGuidelinesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Рекомендации ВОЗ по активности")
            .setMessage(
                "Всемирная организация здравоохранения (ВОЗ) рекомендует взрослым:\n\n" +
                        "• 150–300 минут умеренной аэробной активности в неделю (примерно 21–43 мин/день).\n" +
                        "• Или 75–150 минут интенсивной аэробной активности в неделю (примерно 11–21 мин/день).\n" +
                        "• Не менее 8,000–10,000 шагов в день для поддержания здоровья.\n" +
                        "• Упражнения для укрепления мышц 2 или более дней в неделю.\n" +
                        "• Сокращение времени сидячего образа жизни, замена его легкой активностью (расход энергии 300–500 ккал/день).\n\n" +
                        "Эти цели помогают снизить риск хронических заболеваний и улучшить общее самочувствие."
            )
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show()
    }

    data class Goal(val name: String, val target: Int, val icon: Int)
}