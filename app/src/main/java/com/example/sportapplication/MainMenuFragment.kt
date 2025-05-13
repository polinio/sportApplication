package com.example.sportapplication

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.sportapplication.analytics.AnalyticsActivity
import com.example.sportapplication.analytics.WorkoutHistoryActivity

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.sportapplication.data.Workout
import com.example.sportapplication.data.WorkoutDTO
import com.example.sportapplication.data.toWorkout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainMenuFragment : Fragment() {

    private lateinit var sensorManager: SensorManager // Менеджер сенсоров
    private lateinit var stepsTextView: TextView // Текстовое поле для отображения шагов
    private lateinit var lastWorkoutTextView: TextView


    // Запрос разрешения на распознавание активности
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("MainMenuFragment", "Activity recognition permission granted")
        } else {
            Toast.makeText(requireContext(), "Разрешение не получено!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_menu, container, false) // Загружаем разметку
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stepsTextView = view.findViewById(R.id.steps)  // Находим элемент для вывода шагов
        lastWorkoutTextView = view.findViewById(R.id.lastWorkout) // Находим элемент для вывода последней тренировки
        sensorManager = requireContext().getSystemService(SensorManager::class.java) // Получаем менеджер сенсоров

        requestActivityRecognitionPermission()  // Проверяем разрешение и ...

        // Обработка нажатия на блок 1 (шаги, расстояние, калории)
        val block1 = view.findViewById<LinearLayout>(R.id.block1)
        block1.setOnClickListener {
            val intent = Intent(requireContext(), AnalyticsActivity::class.java)
            startActivity(intent)
        }

        // Обработка нажатия на блок 2 (история тренировок)
        val block2 = view.findViewById<LinearLayout>(R.id.block2)
        block2.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutHistoryActivity::class.java)
            startActivity(intent)
        }

        loadLastWorkout()
    }

    // Запрос разрешения на доступ к данным о физической активности
    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Проверяем версию Android
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) // Запрашиваем разрешение
            }
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
            object :
            ValueEventListener {
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

    // Повторная регистрация слушателя при возврате на экран
    override fun onResume() {
        super.onResume()
    }
    // Отмена регистрации слушателя при уходе с экрана
    override fun onPause() {
        super.onPause()
    }
}