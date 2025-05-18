package com.example.sportapplication.workout

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sportapplication.R
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView
import com.google.android.gms.location.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Vibrator
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.sportapplication.data.Interval
import com.example.sportapplication.data.IntervalDTO
import com.example.sportapplication.data.IntervalType
import com.example.sportapplication.data.WorkoutDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.runtime.image.ImageProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutTrackingActivity : AppCompatActivity() {

    // Для сохранения в Firebase
    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance()
        .getReference("users")
        .child(auth.currentUser?.uid ?: "anonymous")
        .child("workouts")

    // Карты
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var polyline: PolylineMapObject
    private val routePoints = mutableListOf<Point>()
    private var startMarker: PlacemarkMapObject? = null
    private var currentLocationMarker: PlacemarkMapObject? = null

    // Объект для получения местоположения
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                updateMap(location.latitude, location.longitude)  // Центрируем камеру
                updateRoute(location.latitude, location.longitude)  // Добавляем точку маршрута
            }
        }
    }

    // Элементы интерфейса для тренировки
    private lateinit var durationTextView: TextView // TextView для отображения длительности тренировки
    private lateinit var caloriesTextView: TextView // TextView для отображения калорий
    private lateinit var distanceTextView: TextView  // TextView для отображения дистанции
    private lateinit var paceTextView: TextView // TextView для отображения темпа

    // Элементы интерфейса для отображения цели
    private lateinit var goalTextView: TextView
    private lateinit var goalProgressBar: ProgressBar

    // Элементы интерфейса для отображения интервалов
    private lateinit var intervalLayout: View
    private lateinit var intervalProgressText: TextView
    private lateinit var intervalProgressBar: ProgressBar
    private lateinit var intervalCounter: TextView

    // Кнопки управления
    private lateinit var pauseResumeButton: Button
    private lateinit var stopButton: Button

    // обработки паузы и остановки
    private var isPaused = false  // Состояние тренировки (на паузе или нет)
    private var lastLocationWhenPaused: Location? = null  // Последнее местоположение при паузе
    private var isLocationUpdatesActive = false  // Для управления активностью обновлений

    // Таймер
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    // Время начала тренировки
    private var trainingStartTime: Long = 0L

    // Расчёта калорий в зависимости от активности
    private var caloriesPerMilli: Double = 0.0
    private var totalCalories = 0.0
    private var totalDistance = 0.0
    private var lastLocation: Location? = null

    // Шагомер
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialStepCount: Int = -1
    private var totalSteps: Int = 0
    private var activityType: String = "walking"

    // Цели (обычный режим)
    private var goalType: String? = null
    private var goalValue: Float? = null
    private var goalAchieved = false

    // Интервальный режим
    private var intervals: MutableList<Interval>? = null
    private var currentIntervalIndex = 0
    private var intervalStartDistance = 0.0
    private var intervalStartTime: Long = 0L


    private var targetPace: String? = null
    private var targetDistance: Double? = null
    private var notificationInterval: Double? = null // в км
    private var paceTolerance: Int? = null // в секундах
    private var lastNotificationDistance = 0.0 // Последняя дистанция уведомления

    private lateinit var paceStatusTextView: TextView

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = event.values[0].toInt()
                if (initialStepCount == -1) {
                    initialStepCount = currentSteps
                }
                totalSteps = currentSteps - initialStepCount
                Log.d("WorkoutTracking", "Steps: $totalSteps")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            // Общее время в миллисекундах
            val currentTime = SystemClock.elapsedRealtime()
            val time = currentTime - startTime + elapsedTime

            // Обновляем таймер
            durationTextView.text = formatTime(time)

            // Расчитываем калории по фиксированной ставке
            totalCalories = time * caloriesPerMilli
            caloriesTextView.text = "${totalCalories.toInt()}"

            // Расчёт среднего темпа
            if (totalDistance > 0.0) {
                val paceInMinPerKm = (time.toDouble() / 60000) / (totalDistance / 1000)
                val paceMinutes = paceInMinPerKm.toInt()
                val paceSeconds = ((paceInMinPerKm - paceMinutes) * 60).toInt()
                paceTextView.text = String.format("%d:%02d мин/км", paceMinutes, paceSeconds)
            } else {
                paceTextView.text = "— мин/км"
            }

            // Обновляем прогресс бар
            updateGoalProgress(time / 60000f, totalDistance / 1000, totalCalories)

            updateIntervalProgress(time, totalDistance / 1000)

            // Обновление статуса темпа
            if (targetPace != null && notificationInterval != null && paceTolerance != null) {
                updatePaceStatus()
            }

            // Обновляем каждую секунду
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(this) // Инициализируем Яндекс.Карты ДО установки разметки
        enableEdgeToEdge()
        setContentView(R.layout.activity_workout_tracking)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SensorManager::class.java)
        checkLocationPermission()
        checkActivityRecognitionPermission()

        // Инициализация интерфейсных элементов
        mapView = findViewById(R.id.mapView)
        durationTextView = findViewById(R.id.duration)
        caloriesTextView = findViewById(R.id.calories)
        distanceTextView = findViewById(R.id.distance)
        paceTextView = findViewById(R.id.pace_or_speed)
        goalTextView = findViewById(R.id.goalText)
        goalProgressBar = findViewById(R.id.goalProgressBar)
        intervalLayout = findViewById(R.id.intervalLayout)
        intervalProgressText = findViewById(R.id.intervalProgressText)
        intervalProgressBar = findViewById(R.id.intervalProgressBar)
        intervalCounter = findViewById(R.id.intervalCounter)
        pauseResumeButton = findViewById(R.id.pauseResumeButton)
        stopButton = findViewById(R.id.stopButton)
        paceStatusTextView = findViewById(R.id.paceStatusTextView)

        // Получаем тип активности из Intent и устанавливаем коэффициент
        activityType = intent.getStringExtra("ACTIVITY_TYPE") ?: "walking"
        Log.d("WorkoutTrackingActivity", "Activity type: $activityType")
        caloriesPerMilli = when (activityType) {
            "running" -> 6.0 / 60000  // 6 кал/мин
            "cycling" -> 12.0 / 60000 // 12 кал/мин
            else -> 3.0 / 60000       // walking – 3 кал/мин
        }

        goalType = intent.getStringExtra("GOAL_TYPE")
        goalValue = intent.getFloatExtra("GOAL_VALUE", -1f).takeIf { it != -1f }
        intervals = intent.getParcelableArrayListExtra<Interval>("intervals")?.toMutableList()

        // Установка начального значения для интервалов
        if (goalType != null && goalValue != null) {
            findViewById<LinearLayout>(R.id.goalLayout).visibility = View.VISIBLE
            goalTextView.text = when (goalType) {
                "distance" -> "Цель: $goalValue км"
                "time" -> "Цель: ${goalValue!!.toInt()} мин"
                "calories" -> "Цель: ${goalValue!!.toInt()} ккал"
                else -> "Цель: 0"
            }
        } else if (intervals != null && intervals!!.isNotEmpty()) {
            intervalLayout.visibility = View.VISIBLE
            updateIntervalUI()
        }

        // Получение настроек темпа из Intent
        targetPace = intent.getStringExtra("pace")
        targetDistance = intent.getDoubleExtra("distance", 0.0).takeIf { it > 0 }
        val intervalStr = intent.getStringExtra("interval")
        paceTolerance = intent.getIntExtra("tolerance", 0)
        notificationInterval = intervalStr?.removeSuffix(" км")?.toDoubleOrNull()

        if (targetPace != null && notificationInterval != null && paceTolerance != null) {
            paceStatusTextView.visibility = View.GONE
        }

        // Инициализация шагомера
        if (activityType == "running" || activityType == "walking") {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepSensor == null) {
                Log.w("WorkoutTracking", "Step counter sensor not available")
                Toast.makeText(this, "Шагомер не поддерживается на этом устройстве",
                    Toast.LENGTH_LONG).show()
            }
        }

        pauseResumeButton.setOnClickListener {
            if (isRunning) pauseWorkout() else resumeWorkout()
        }

        stopButton.setOnClickListener {
            stopWorkout()
        }

        // Запуск тренировки
        startWorkout()
    }

    // Обновление прогресса
    private fun updateGoalProgress(timeInMinutes: Float, distanceInKm: Double, calories: Double) {
        if (goalType == null || goalValue == null || goalAchieved) return

        val progress = when (goalType) {
            "distance" -> (distanceInKm / goalValue!!) * 100
            "time" -> (timeInMinutes / goalValue!!) * 100
            "calories" -> (calories / goalValue!!) * 100
            else -> 0f
        }.toInt().coerceAtMost(100)

        goalProgressBar.progress = progress
        if (progress >= 100 && !goalAchieved) {
            goalAchieved = true
            goalTextView.text = "Вы достигли цели!"
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(500)
        }
    }

    // Обновление прогресса интервалов
    private fun updateIntervalProgress(totalTime: Long, distanceInKm: Double) {
        if (intervals.isNullOrEmpty() || currentIntervalIndex >= intervals!!.size) return

        val currentInterval = intervals!![currentIntervalIndex]
        val distanceInMeters = distanceInKm * 1000
        val intervalDistance = distanceInMeters - intervalStartDistance
        val progress = ((intervalDistance / currentInterval.targetDistance) * 100).toInt().coerceAtMost(100)
        intervalProgressBar.progress = progress

        val typeName = when (currentInterval.type) {
            IntervalType.WARMUP -> "Разминка"
            IntervalType.ACCELERATION -> "Ускорение"
            IntervalType.REST -> "Отдых"
            IntervalType.COOLDOWN -> "Заминка"
        }
        intervalProgressText.text = "$typeName: ${intervalDistance.toInt()}/${currentInterval.targetDistance.toInt()} м"
        intervalCounter.text = "Интервал ${currentIntervalIndex + 1} из ${intervals!!.size}"

        // Проверка завершения интервала
        if (intervalDistance >= currentInterval.targetDistance) {
            // Сохраняем данные текущего интервала
            val intervalDuration = totalTime - intervalStartTime
            val intervalPace = if (intervalDistance > 0) {
                val paceInMinPerKm = (intervalDuration.toDouble() / 60000) / (intervalDistance / 1000)
                val paceMinutes = paceInMinPerKm.toInt()
                val paceSeconds = ((paceInMinPerKm - paceMinutes) * 60).toInt()
                String.format("%d:%02d", paceMinutes, paceSeconds)
            } else "—"
            intervals!![currentIntervalIndex] = currentInterval.copy(
                actualDistance = intervalDistance,
                duration = intervalDuration,
                pace = intervalPace
            )

            // Переход к следующему интервалу
            currentIntervalIndex++
            if (currentIntervalIndex < intervals!!.size) {
                intervalStartDistance = distanceInMeters
                intervalStartTime = totalTime
                updateIntervalUI()
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1) // Двойная вибрация
            } else {
                intervalProgressText.text = "Все интервалы завершены!"
                intervalProgressBar.progress = 100
            }
        }
    }

    // Обновление UI для интервалов
    private fun updateIntervalUI() {
        if (intervals.isNullOrEmpty() || currentIntervalIndex >= intervals!!.size) return

        val currentInterval = intervals!![currentIntervalIndex]
        val typeName = when (currentInterval.type) {
            IntervalType.WARMUP -> "Разминка"
            IntervalType.ACCELERATION -> "Ускорение"
            IntervalType.REST -> "Отдых"
            IntervalType.COOLDOWN -> "Заминка"
        }
        intervalProgressText.text = "$typeName: 0/${currentInterval.targetDistance.toInt()} м"
        intervalProgressBar.progress = 0
        intervalCounter.text = "Интервал ${currentIntervalIndex + 1} из ${intervals!!.size}"
    }

    private fun updatePaceStatus() {
        val distanceInKm = totalDistance / 1000
        if (distanceInKm >= lastNotificationDistance + notificationInterval!!) {
            paceStatusTextView.visibility = View.VISIBLE // Делаем видимым при первом уведомлении
            val currentPace = calculateCurrentPace()
            val targetPaceInSeconds = parsePaceToSeconds(targetPace!!)
            val currentPaceInSeconds = parsePaceToSeconds(currentPace)

            val difference = currentPaceInSeconds - targetPaceInSeconds

            val message = when {
                difference > paceTolerance!! -> "Стоит немного ускориться, текущее запоздание по темпу ${formatDifference(difference)}"
                difference < -paceTolerance!! -> "Есть возможность немного сбавить темп, текущая спешка по темпу ${formatDifference(-difference)}"
                else -> "Хорошая поддержка темпа, так держать!"
            }

            paceStatusTextView.text = message

            // Вибрация
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(500)

            lastNotificationDistance += notificationInterval!!
        }

        // Проверка завершения дистанции
        if (targetDistance != null && distanceInKm >= targetDistance!!) {
            paceStatusTextView.text = "Дистанция завершена!"
            stopWorkout()
        }
    }

    private fun calculateCurrentPace(): String {
        val timeInMinutes = elapsedTime / 60000.0
        val distanceInKm = totalDistance / 1000
        if (distanceInKm > 0) {
            val paceInMinPerKm = timeInMinutes / distanceInKm
            val paceMinutes = paceInMinPerKm.toInt()
            val paceSeconds = ((paceInMinPerKm - paceMinutes) * 60).toInt()
            return String.format("%d:%02d", paceMinutes, paceSeconds)
        }
        return "—"
    }

    private fun parsePaceToSeconds(pace: String): Int {
        val parts = pace.split(":")
        val minutes = parts[0].toInt()
        val seconds = parts[1].toInt()
        return minutes * 60 + seconds
    }

    private fun formatDifference(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d'%02d\"", minutes, remainingSeconds)
    }

    // Проверка разрешений на доступ к геолокации
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission
                .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission
                .ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLastKnownLocation()
        }
    }

    // Проверка разрешений на доступ к шагомеру
    private fun checkActivityRecognitionPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission
                    .ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission
                    .ACTIVITY_RECOGNITION), ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE)
            } else {
                startStepCounter()
            }
        } else {
            startStepCounter()
        }
    }

    // Обработка результатов запроса разрешений
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager
                        .PERMISSION_GRANTED) {
                    getLastKnownLocation()
                } else {
                    Toast.makeText(this, "Need location permission",
                        Toast.LENGTH_SHORT).show()
                }
            }
            ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager
                        .PERMISSION_GRANTED) {
                    startStepCounter()
                } else {
                    Toast.makeText(this, "Need activity recognition permission " +
                            "for step counting", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Запуск и остановка шагомера
    private fun startStepCounter() {
        if (stepSensor != null && (activityType == "running" || activityType == "walking")) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("WorkoutTracking", "Step counter started")
        }
    }

    // Остановка шагомера
    private fun stopStepCounter() {
        if (stepSensor != null) {
            sensorManager.unregisterListener(stepListener)
            Log.d("WorkoutTracking", "Step counter stopped")
        }
    }

    // Получаем последнее известное местоположение
    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("WorkoutTracking", "Текущие координаты: ${location.latitude}, ${location.longitude}")
                    updateMap(location.latitude, location.longitude)  // Обновляем карту
                    updateRoute(location.latitude, location.longitude)  // Добавляем точку маршрута
                } else {
                    Log.d("WorkoutTracking", "Не удалось получить местоположение")
                }
            }
        } else {
            Log.e("WorkoutTracking", "Нет разрешения на доступ к геолокации")
        }
    }

    // Функция для обновления карты
    private fun updateMap(latitude: Double, longitude: Double) {
        // Создаём точку на карте
        val userLocation = Point(latitude, longitude)

        // Обновляем или создаем маркер текущей локации
        if (currentLocationMarker == null) {
            currentLocationMarker = mapView.mapWindow.map.mapObjects.addPlacemark(
                userLocation,
                ImageProvider.fromResource(this, R.drawable.ic_location_dot_vector)
            ).apply {
                setIconStyle(IconStyle().apply {
                    anchor = PointF(0.5f, 0.5f)
                    scale = 0.04f
                })
                zIndex = 10f // Выше маршрута и начальной точки
            }
            Log.d("WorkoutTracking", "Current location marker created at $latitude, $longitude")
        } else {
            currentLocationMarker?.geometry = userLocation
            Log.d("WorkoutTracking", "Current location marker updated to $latitude, $longitude")
        }
        // Центрируем камеру на текущем местоположении
        val cameraPosition = CameraPosition(userLocation, 16.0f, 0.0f, 0.0f)  // Масштаб
        mapView.mapWindow.map.move(cameraPosition)

        // Показываем маркер
        currentLocationMarker!!.isVisible = true
    }

    // Функция для обновления маршрута
    private fun updateRoute(latitude: Double, longitude: Double) {
        val newPoint = Point(latitude, longitude)
        routePoints.add(newPoint)  // Добавляем новую точку в список маршрута

        // Рассчитываем прирост дистанции
        val newLocation = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }

        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(newLocation)  // Получаем расстояние в метрах
            totalDistance += distance  // Прибавляем к общему расстоянию
            distanceTextView.text = String.format("%.2f км", totalDistance / 1000)  // Обновляем UI
        }

        lastLocation = newLocation  // Обновляем последнюю точку

        // Если маршрут уже есть, обновляем его
        if (::polyline.isInitialized) {
            polyline.geometry = Polyline(routePoints)
        } else {
            // Иначе создаём новый маршрут
            polyline = mapView.mapWindow.map.mapObjects.addPolyline(Polyline(routePoints)).apply {
                strokeWidth = 4f  // Толщина линии
                setStrokeColor(Color.BLUE)  // Цвет маршрута
            }
        }
    }

    // Запуск обновлений местоположения
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000) // 1 секунда
            .setMinUpdateIntervalMillis(2000)  // Минимальный интервал обновления 2 секунды
            .setMinUpdateDistanceMeters(5f) // Обновление только при перемещении на 5 метра
            .setWaitForAccurateLocation(false) // Сразу отдавать данные, не дожидаясь высокой точности
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                Looper.getMainLooper())
        }
    }

    private fun startWorkout() {
        Log.d("WorkoutTrackingActivity", "Starting workout...")
        trainingStartTime = System.currentTimeMillis()  // сохраняем дату и время начала тренировки
        startTime = SystemClock.elapsedRealtime()
        intervalStartTime = 0L
        handler.post(timerRunnable)
        isRunning = true
        pauseResumeButton.text = "Пауза"
        startLocationUpdates()
        startStepCounter()
    }

    private fun pauseWorkout() {
        elapsedTime += SystemClock.elapsedRealtime() - startTime
        handler.removeCallbacks(timerRunnable)
        isRunning = false
        pauseResumeButton.text = "Продолжить"
        stopStepCounter()
    }

    private fun resumeWorkout() {
        startTime = SystemClock.elapsedRealtime()
        handler.post(timerRunnable)
        isRunning = true
        pauseResumeButton.text = "Пауза"
        startStepCounter()
    }

    private fun stopWorkout() {
        handler.removeCallbacks(timerRunnable)
        if (isRunning) {
            elapsedTime += SystemClock.elapsedRealtime() - startTime
            isRunning = false
        }
        stopStepCounter()

        val totalTime = elapsedTime
        val activityType = intent.getStringExtra("ACTIVITY_TYPE") ?: "walking"  // передаём тот же тип, что получили ранее

        val avgPaceStr: String = if (totalDistance > 0) {
            val km = totalDistance / 1000
            val min = (totalTime / 60000.0 / km).toInt()
            val sec = (((totalTime / 60000.0 / km) - min) * 60).toInt()
            String.format("%d:%02d", min, sec)
        } else "—"

        // Конвертируем метры в километры для отображения
        val distanceInKm = totalDistance / 1000

        // Сериализуем точки маршрута
        val routePointsArray = ArrayList<DoubleArray>().apply {
            routePoints.forEach { point -> add(doubleArrayOf(point.latitude, point.longitude)) }
        }

        val timestampMillis = System.currentTimeMillis()

        // Сохраняем в Firebase
        saveWorkoutToFirebase(
            activityType = activityType,
            distance = totalDistance,
            duration = totalTime,
            avgPace = avgPaceStr,
            calories = totalCalories.toInt(),
            steps = totalSteps,
            timestamp = timestampMillis,
            intervals = intervals
        )

        val intent = Intent(this, WorkoutReportActivity::class.java).apply {
            putExtra("TOTAL_TIME", totalTime)
            putExtra("TOTAL_CALORIES", totalCalories.toInt())
            putExtra("ACTIVITY_TYPE", activityType)
            putExtra("TRAINING_START_TIME", trainingStartTime)
            putExtra("TOTAL_DISTANCE", totalDistance)
            putExtra("AVG_PACE", avgPaceStr)
            putExtra("TOTAL_STEPS", totalSteps)
            putExtra("ROUTE_POINTS", routePointsArray)
            putParcelableArrayListExtra("INTERVALS", intervals?.let { ArrayList(it) })
        }
        startActivity(intent)
        finish()
    }

    private fun saveWorkoutToFirebase(
        activityType: String,
        distance: Double,
        duration: Long,
        avgPace: String,
        calories: Int,
        steps: Int,
        timestamp: Long,
        intervals: List<Interval>?
    ) {
        val workoutId = dbRef.push().key ?: return

        // Конвертируем метры в километры и округляем до 2 знаков
        val distanceInKm = String.format(Locale.US, "%.2f", distance / 1000).toDouble()

        // Преобразуем Interval в IntervalDTO
        val intervalDTOs = intervals?.map { interval ->
            IntervalDTO(
                type = interval.type,
                targetDistance = interval.targetDistance,
                actualDistance = interval.actualDistance,
                duration = interval.duration,
                pace = interval.pace
            )
        }

        // Создаем WorkoutDTO
        val workoutDTO = WorkoutDTO(
            activityType = activityType,
            distance = distanceInKm,
            duration = duration,
            avgPace = avgPace,
            calories = calories,
            steps = steps,
            timestamp = timestamp,
            intervals = intervalDTOs
        )

        dbRef.child(workoutId).setValue(workoutDTO)
            .addOnSuccessListener {
                Log.d("WorkoutTracking", "Workout saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("WorkoutTracking", "Failed to save workout: ${e.message}", e)
            }
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatTimestamp(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timeInMillis))
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        if (isRunning) handler.post(timerRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val ACTIVITY_RECOGNITION_PERMISSION_REQUEST_CODE = 1002
    }
}