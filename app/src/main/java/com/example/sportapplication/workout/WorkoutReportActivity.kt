package com.example.sportapplication.workout

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sportapplication.R
import com.example.sportapplication.data.Interval
import com.example.sportapplication.data.IntervalType
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutReportActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var routePolyline: PolylineMapObject
    private lateinit var trainingTimeTextView: TextView
    private lateinit var trainingCaloriesTextView: TextView
    private lateinit var trainingDateTextView: TextView
    private lateinit var trainingActivityTypeTextView: TextView
    private lateinit var trainingDistanceTextView: TextView
    private lateinit var trainingStepsTextView: TextView
    private lateinit var trainingAvgPaceTextView: TextView
    private lateinit var trainingAverageStepLengthTextView: TextView
    private lateinit var intervalsTitle: TextView
    private lateinit var intervalsRecyclerView: RecyclerView
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_workout_report)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация UI
        trainingTimeTextView = findViewById(R.id.trainingTime)
        trainingCaloriesTextView = findViewById(R.id.trainingCalories)
        trainingDateTextView = findViewById(R.id.trainingDate)
        trainingActivityTypeTextView = findViewById(R.id.trainingActivityType)
        trainingDistanceTextView = findViewById(R.id.trainingDistance)
        trainingStepsTextView = findViewById(R.id.trainingSteps)
        trainingAvgPaceTextView = findViewById(R.id.trainingPace)
        trainingAverageStepLengthTextView = findViewById(R.id.trainingAverageStepLength)
        intervalsTitle = findViewById(R.id.intervalsTitle)
        intervalsRecyclerView = findViewById(R.id.intervalsRecyclerView)
        backButton = findViewById(R.id.backButton)

        // Получение данных из Intent
        val totalTime = intent.getLongExtra("TOTAL_TIME", 0L)
        val totalCalories = intent.getIntExtra("TOTAL_CALORIES", 0)
        val trainingStartTime = intent.getLongExtra("TRAINING_START_TIME", 0L)
        val activityType = intent.getStringExtra("ACTIVITY_TYPE") ?: "walking"
        val totalDistance = intent.getDoubleExtra("TOTAL_DISTANCE", 0.0)
        val avgPace = intent.getStringExtra("AVG_PACE") ?: "—"
        val totalSteps = intent.getIntExtra("TOTAL_STEPS", 0)
        val intervals = intent.getParcelableArrayListExtra<Interval>("INTERVALS")

        // Заполнение основной статистики
        trainingTimeTextView.text = formatTime(totalTime)
        trainingCaloriesTextView.text = "$totalCalories"
        trainingDateTextView.text = formatDateTime(trainingStartTime)
        trainingDistanceTextView.text = String.format("%.2f", totalDistance / 1000)
        trainingAvgPaceTextView.text = avgPace
        trainingStepsTextView.text = if (activityType == "cycling" || totalSteps == 0)
            "—" else "$totalSteps"

        // Средняя длина шага
        val averageStepLength = if (activityType != "cycling" && totalSteps > 0 && totalDistance > 0) {
            val stepLengthCm = (totalDistance / totalSteps) * 100 // Метры в см
            String.format("%.0f см", stepLengthCm)
        } else {
            "—"
        }
        trainingAverageStepLengthTextView.text = averageStepLength

        // Тип активности
        val activityText = when (activityType) {
            "running" -> "Бег"
            "cycling" -> "Велосипед"
            else -> "Ходьба"
        }
        trainingActivityTypeTextView.text = "Активность: $activityText"

        // Настройка интервалов
        if (!intervals.isNullOrEmpty()) {
            val completedIntervals = intervals.filter { it.actualDistance > 0 }
            if (completedIntervals.isNotEmpty()) {
                intervalsTitle.visibility = View.VISIBLE
                intervalsRecyclerView.visibility = View.VISIBLE
                intervalsRecyclerView.layoutManager = LinearLayoutManager(this)
                intervalsRecyclerView.adapter = IntervalReportAdapter(completedIntervals)
            }
        }

        // Инициализация карты
        val routePointsArray = intent.getSerializableExtra("ROUTE_POINTS") as? ArrayList<DoubleArray>
        val routePoints = routePointsArray?.map { Point(it[0], it[1]) } ?: emptyList()
        initMap(routePoints)

        // Обработчик кнопки возврата
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun initMap(points: List<Point>) {
        val mapContainer = findViewById<FrameLayout>(R.id.trackMapContainer)
        mapView = MapView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        mapContainer.addView(mapView)

        if (points.isNotEmpty()) {
            val mapObjects = mapView.mapWindow.map.mapObjects
            mapObjects.addPlacemark(points.first()).apply {
                setIcon(ImageProvider.fromResource(this@WorkoutReportActivity, R.drawable.ic_start))
                setIconStyle(IconStyle().apply {
                    anchor = PointF(0.5f, 1.0f)
                    scale = 1.5f
                })
            }
            mapObjects.addPlacemark(points.last()).apply {
                setIcon(ImageProvider.fromResource(this@WorkoutReportActivity, R.drawable.ic_finish))
                setIconStyle(IconStyle().apply {
                    anchor = PointF(0.5f, 1.0f)
                    scale = 1.5f
                })
            }
            if (points.size > 1) {
                routePolyline = mapObjects.addPolyline(Polyline(points)).apply {
                    strokeWidth = 5f
                    setStrokeColor(Color.BLUE)
                }
            }
            val boundingBox = calculateBoundingBox(points)
            val cameraPosition = mapView.mapWindow.map.cameraPosition(
                Geometry.fromBoundingBox(boundingBox)
            )
            mapView.mapWindow.map.move(cameraPosition)
        }
    }

    private fun calculateBoundingBox(points: List<Point>): BoundingBox {
        var minLat = 90.0
        var maxLat = -90.0
        var minLon = 180.0
        var maxLon = -180.0
        for (point in points) {
            if (point.latitude < minLat) minLat = point.latitude
            if (point.latitude > maxLat) maxLat = point.latitude
            if (point.longitude < minLon) minLon = point.longitude
            if (point.longitude > maxLon) maxLon = point.longitude
        }
        val padding = 0.01
        return BoundingBox(
            Point(minLat - padding, minLon - padding),
            Point(maxLat + padding, maxLon + padding)
        )
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatDateTime(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timeInMillis))
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}

class IntervalReportAdapter(private val intervals: List<Interval>) :
    RecyclerView.Adapter<IntervalReportAdapter.IntervalViewHolder>() {

    class IntervalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val intervalType: TextView = itemView.findViewById(R.id.intervalType)
        val intervalDistance: TextView = itemView.findViewById(R.id.intervalDistance)
        val intervalDuration: TextView = itemView.findViewById(R.id.intervalDuration)
        val intervalPace: TextView = itemView.findViewById(R.id.intervalPace)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntervalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interval_report, parent, false)
        return IntervalViewHolder(view)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: IntervalViewHolder, position: Int) {
        val interval = intervals[position]
        holder.intervalType.text = when (interval.type) {
            IntervalType.WARMUP -> "Разминка"
            IntervalType.ACCELERATION -> "Ускорение"
            IntervalType.REST -> "Отдых"
            IntervalType.COOLDOWN -> "Заминка"
        }
        holder.intervalDistance.text = "Пройдено: ${interval.actualDistance.toInt()} м"
        holder.intervalDuration.text = "Время: ${formatTime(interval.duration)}"
        holder.intervalPace.text = "Темп: ${interval.pace}"
    }

    override fun getItemCount(): Int = intervals.size

    private fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}