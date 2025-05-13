package com.example.sportapplication.analytics

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.R
import com.example.sportapplication.data.Workout
import com.example.sportapplication.data.WorkoutDTO
import com.example.sportapplication.data.toWorkout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
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

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var periodSpinner: Spinner
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var periodTextView: TextView
    private lateinit var durationBlock: LinearLayout
    private lateinit var countBlock: LinearLayout
    private lateinit var caloriesBlock: LinearLayout
    private lateinit var totalDurationTextView: TextView
    private lateinit var totalCountTextView: TextView
    private lateinit var totalCaloriesTextView: TextView
    private lateinit var activityBlocksContainer: LinearLayout

    private val workouts = mutableListOf<Workout>()
    private var currentPeriodType = "week" // week, month, year
    private var currentPeriodStart: Long = 0
    private var currentPeriodEnd: Long = 0
    private val calendar = Calendar.getInstance()
    private var selectedMetric = "duration" // duration, count, calories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        // Инициализация UI элементов
        barChart = findViewById(R.id.barChart)
        periodSpinner = findViewById(R.id.periodSpinner)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        periodTextView = findViewById(R.id.periodText)
        durationBlock = findViewById(R.id.durationBlock)
        countBlock = findViewById(R.id.countBlock)
        caloriesBlock = findViewById(R.id.caloriesBlock)
        totalDurationTextView = findViewById(R.id.totalDuration)
        totalCountTextView = findViewById(R.id.totalCount)
        totalCaloriesTextView = findViewById(R.id.totalCalories)
        activityBlocksContainer = findViewById(R.id.activityBlocksContainer)

        // Настройка выпадающего меню
        val periods = arrayOf("По дням недели", "По дням месяца", "По месяцам года")
        periodSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periods)
        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentPeriodType = when (position) {
                    0 -> "week"
                    1 -> "month"
                    2 -> "year"
                    else -> "week"
                }
                resetToCurrentPeriod()
                updateChartAndStats()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Настройка кнопок навигации
        prevButton.setOnClickListener { shiftPeriod(-1); updateChartAndStats() }
        nextButton.setOnClickListener { shiftPeriod(1); updateChartAndStats() }

        // Настройка кликов по блокам
        durationBlock.setOnClickListener { selectMetric("duration") }
        countBlock.setOnClickListener { selectMetric("count") }
        caloriesBlock.setOnClickListener { selectMetric("calories") }

        // Инициализация текущего периода (текущая неделя)
        resetToCurrentPeriod()

        // По умолчанию выбран "Длительность"
        selectMetric("duration")

        // Загрузка данных
        loadWorkouts()
    }

    private fun selectMetric(metric: String) {
        selectedMetric = metric
        updateSelectedBlock()
        updateChartAndStats()
    }

    private fun updateSelectedBlock() {
        durationBlock.setBackgroundResource(if (selectedMetric == "duration") R.drawable.rounded_background_selected else R.drawable.rounded_background)
        countBlock.setBackgroundResource(if (selectedMetric == "count") R.drawable.rounded_background_selected else R.drawable.rounded_background)
        caloriesBlock.setBackgroundResource(if (selectedMetric == "calories") R.drawable.rounded_background_selected else R.drawable.rounded_background)
    }

    private fun resetToCurrentPeriod() {
        calendar.time = Date()
        when (currentPeriodType) {
            "week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                currentPeriodEnd = calendar.timeInMillis
            }
            "month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                currentPeriodEnd = calendar.timeInMillis
            }
            "year" -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = calendar.timeInMillis
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                currentPeriodEnd = calendar.timeInMillis
            }
        }
        Log.d("AnalyticsActivity", "Reset period: $currentPeriodStart to $currentPeriodEnd")
        updatePeriodText()
    }

    private fun shiftPeriod(direction: Int) {
        calendar.timeInMillis = currentPeriodStart
        when (currentPeriodType) {
            "week" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, direction)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                currentPeriodEnd = calendar.timeInMillis
            }
            "month" -> {
                calendar.add(Calendar.MONTH, direction)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                currentPeriodEnd = calendar.timeInMillis
            }
            "year" -> {
                calendar.add(Calendar.YEAR, direction)
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = calendar.timeInMillis
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                currentPeriodEnd = calendar.timeInMillis
            }
        }
        Log.d("AnalyticsActivity", "Shifted period: $currentPeriodStart to $currentPeriodEnd")
        updatePeriodText()
    }

    private fun updatePeriodText() {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val startDate = sdf.format(Date(currentPeriodStart))
        val endDate = sdf.format(Date(currentPeriodEnd))
        periodTextView.text = when (currentPeriodType) {
            "week" -> "Неделя: $startDate - $endDate"
            "month" -> "Месяц: ${calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${calendar.get(Calendar.YEAR)}"
            "year" -> "Год: ${calendar.get(Calendar.YEAR)}"
            else -> ""
        }
    }

    private fun loadWorkouts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val workoutsRef = FirebaseDatabase.getInstance().getReference("users/$userId/workouts")
        workoutsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                workouts.clear()
                for (child in snapshot.children) {
                    try {
                        val workoutDTO = child.getValue(WorkoutDTO::class.java)
                        if (workoutDTO != null) {
                            val workout = workoutDTO.toWorkout()
                            workouts.add(workout)
                            Log.d("AnalyticsActivity", "Loaded workoutDTO: $workoutDTO")
                            Log.d("AnalyticsActivity", "Converted to workout: $workout")
                        } else {
                            Log.w("AnalyticsActivity", "Failed to parse workoutDTO: ${child.key}")
                        }
                    } catch (e: Exception) {
                        Log.e("AnalyticsActivity", "Error parsing workout ${child.key}: ${e.message}", e)
                    }
                }
                updateChartAndStats()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AnalyticsActivity", "Failed to load workouts: ${error.message}", error.toException())
            }
        })
    }

    private fun updateChartAndStats() {
        // Фильтрация тренировок за текущий период
        val filteredWorkouts = workouts.filter {
            it.timestamp in currentPeriodStart..currentPeriodEnd
        }

        // Обновление суммарных показателей
        val totalDuration = filteredWorkouts.sumOf { it.duration } / 3600000f // Часы
        val formattedDuration = ceil(totalDuration * 10) / 10 // Округление вверх до 1 знака
        val totalCount = filteredWorkouts.size
        val totalCalories = filteredWorkouts.sumOf { it.calories }
        totalDurationTextView.text = String.format("%.1f час", formattedDuration)
        totalCountTextView.text = "$totalCount раз"
        totalCaloriesTextView.text = "$totalCalories ккал"

        // Подготовка данных для графика
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        when (currentPeriodType) {
            "week" -> {
                val days = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                val dataByDay = Array(7) { mutableListOf<Workout>() }
                calendar.timeInMillis = currentPeriodStart
                for (i in 0..6) {
                    val dayStart = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val dayEnd = calendar.timeInMillis - 1
                    dataByDay[i] = filteredWorkouts.filter { it.timestamp in dayStart..dayEnd }.toMutableList()
                }

                for (i in dataByDay.indices) {
                    val value = when (selectedMetric) {
                        "duration" -> ceil((dataByDay[i].sumOf { it.duration } / 3600000f) * 10) / 10
                        "count" -> dataByDay[i].size.toFloat()
                        "calories" -> dataByDay[i].sumOf { it.calories }.toFloat()
                        else -> 0f
                    }
                    if (value > 0) {
                        entries.add(BarEntry(entries.size.toFloat(), value))
                        labels.add(days[i])
                    }
                }
            }
            "month" -> {
                calendar.timeInMillis = currentPeriodStart
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dataByDay = Array(daysInMonth) { mutableListOf<Workout>() }
                for (i in 0 until daysInMonth) {
                    val dayStart = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val dayEnd = calendar.timeInMillis - 1
                    dataByDay[i] = filteredWorkouts.filter { it.timestamp in dayStart..dayEnd }.toMutableList()
                }

                for (i in dataByDay.indices) {
                    val value = when (selectedMetric) {
                        "duration" -> ceil((dataByDay[i].sumOf { it.duration } / 3600000f) * 10) / 10
                        "count" -> dataByDay[i].size.toFloat()
                        "calories" -> dataByDay[i].sumOf { it.calories }.toFloat()
                        else -> 0f
                    }
                    if (value > 0) {
                        entries.add(BarEntry(entries.size.toFloat(), value))
                        labels.add((i + 1).toString())
                    }
                }
            }
            "year" -> {
                val months = arrayOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
                val dataByMonth = Array(12) { mutableListOf<Workout>() }
                calendar.timeInMillis = currentPeriodStart
                for (i in 0 until 12) {
                    val monthStart = calendar.timeInMillis
                    calendar.add(Calendar.MONTH, 1)
                    val monthEnd = calendar.timeInMillis - 1
                    dataByMonth[i] = filteredWorkouts.filter { it.timestamp in monthStart..monthEnd }.toMutableList()
                }

                for (i in dataByMonth.indices) {
                    val value = when (selectedMetric) {
                        "duration" -> ceil((dataByMonth[i].sumOf { it.duration } / 3600000f) * 10) / 10
                        "count" -> dataByMonth[i].size.toFloat()
                        "calories" -> dataByMonth[i].sumOf { it.calories }.toFloat()
                        else -> 0f
                    }
                    if (value > 0) {
                        entries.add(BarEntry(entries.size.toFloat(), value))
                        labels.add(months[i])
                    }
                }
            }
        }

        // Форматтер для значений столбцов
        val barValueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (selectedMetric) {
                    "duration" -> String.format("%.1f", ceil(value * 10) / 10)
                    "count" -> value.toInt().toString()
                    "calories" -> value.toInt().toString()
                    else -> value.toString()
                }
            }
        }

        // Настройка графика
        val dataSet = BarDataSet(entries, when (selectedMetric) {
            "duration" -> "Длительность (часы)"
            "count" -> "Количество тренировок"
            "calories" -> "Калории"
            else -> ""
        }).apply {
            color = resources.getColor(R.color.blue_500, null)
            valueTextSize = 12f
            valueFormatter = barValueFormatter
        }
        val barData = BarData(dataSet).apply {
            barWidth = 0.4f
        }
        barChart.apply {
            data = if (entries.isNotEmpty()) barData else null
            description.isEnabled = false
            setNoDataText("Нет данных за этот период")
            setFitBars(true)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }

        // Обновление блоков активностей
        updateActivityBlocks(filteredWorkouts)
    }

    private fun updateActivityBlocks(filteredWorkouts: List<Workout>) {
        activityBlocksContainer.removeAllViews()
        val totalDuration = filteredWorkouts.sumOf { it.duration }.toFloat()
        val durationByType = mutableMapOf(
            "running" to 0f,
            "walking" to 0f,
            "cycling" to 0f
        )

        filteredWorkouts.forEach { workout ->
            durationByType[workout.activityType] = durationByType[workout.activityType]!! + workout.duration / 3600000f
        }

        // Сортировка по длительности
        val sortedTypes = durationByType.entries.sortedByDescending { it.value }
        sortedTypes.forEach { (type, duration) ->
            if (duration > 0) {
                val percentage = if (totalDuration > 0) (duration / (totalDuration / 3600000f) * 100) else 0f
                val block = layoutInflater.inflate(R.layout.item_activity_block, activityBlocksContainer, false)
                val title = block.findViewById<TextView>(R.id.activityTitle)
                val durationText = block.findViewById<TextView>(R.id.activityDuration)
                val percentageText = block.findViewById<TextView>(R.id.activityPercentage)
                title.text = when (type) {
                    "running" -> "Бег"
                    "cycling" -> "Велосипед"
                    else -> "Ходьба"
                }
                val formattedDuration = ceil(duration * 10) / 10 // Округление вверх до 1 знака
                durationText.text = String.format("%.1f час", formattedDuration)
                percentageText.text = String.format("%.1f%%", percentage)
                activityBlocksContainer.addView(block)
            }
        }

        if (activityBlocksContainer.childCount == 0) {
            val emptyText = TextView(this).apply {
                text = "Нет активностей за этот период"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            activityBlocksContainer.addView(emptyText)
        }
    }
}