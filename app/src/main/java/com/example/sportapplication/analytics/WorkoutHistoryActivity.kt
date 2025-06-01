package com.example.sportapplication.analytics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.R
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class WorkoutHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingTextView: TextView
    private lateinit var workoutsRef: com.google.firebase.database.DatabaseReference
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_history)

        recyclerView = findViewById(R.id.workoutRecyclerView)
        loadingTextView = findViewById(R.id.loadingTextView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Firebase reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Пожалуйста, войдите в аккаунт", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        workoutsRef = FirebaseDatabase.getInstance().getReference("users/$userId/workouts")

        loadWorkouts()
    }

    private fun loadWorkouts() {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Toast.makeText(this, "Пожалуйста, войдите в аккаунт", Toast.LENGTH_LONG).show()
//            finish()
//            return
//        }

        loadingTextView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

//        val workoutsRef = FirebaseDatabase.getInstance().getReference("users/$userId/workouts")
        workoutsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val workouts = mutableListOf<Pair<String, Workout>>()
                for (child in snapshot.children) {
                    try {
                        val workoutDTO = child.getValue(WorkoutDTO::class.java)
                        if (workoutDTO != null) {
                            val workout = workoutDTO.toWorkout()
                            val workoutId = child.key ?: continue
                            workouts.add(Pair(workoutId, workout))
                            Log.d("WorkoutHistory", "Loaded workoutDTO: $workoutDTO")
                            Log.d("WorkoutHistory", "Converted to workout: $workout")
                        } else {
                            Log.w("WorkoutHistory", "Failed to parse workoutDTO: ${child.key}")
                        }
                    } catch (e: Exception) {
                        Log.e("WorkoutHistory", "Error parsing workout ${child.key}: ${e.message}", e)
                    }
                }

                // Сортируем тренировки по времени
                workouts.sortByDescending { it.second.timestamp  }

                if (workouts.isEmpty()) {
                    loadingTextView.text = "У вас пока нет тренировок"
                    loadingTextView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    workoutAdapter = WorkoutAdapter(workouts) { workoutId ->
                        showDeleteConfirmationDialog(workoutId)
                    }
                    recyclerView.adapter = workoutAdapter
                    recyclerView.visibility = View.VISIBLE
                    loadingTextView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WorkoutHistory", "Failed to load workouts: ${error.message}", error.toException())
                Toast.makeText(this@WorkoutHistoryActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                loadingTextView.text = "Ошибка загрузки"
                loadingTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
        })
    }

    private fun showDeleteConfirmationDialog(workoutId: String) {
        AlertDialog.Builder(this)
            .setTitle("Удалить тренировку")
            .setMessage("Вы уверены, что хотите удалить эту тренировку? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteWorkout(workoutId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteWorkout(workoutId: String) {
        workoutsRef.child(workoutId).removeValue()
            .addOnSuccessListener {
                Log.d("WorkoutHistory", "Workout $workoutId deleted successfully")
                Toast.makeText(this, "Тренировка удалена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("WorkoutHistory", "Failed to delete workout $workoutId: ${e.message}", e)
                Toast.makeText(this, "Ошибка при удалении тренировки", Toast.LENGTH_SHORT).show()
            }
    }
}

class WorkoutAdapter(
    private val workouts: List<Pair<String, Workout>>,
    private val onLongClick: (String) -> Unit) :
    RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.workoutTitle)
        val detailsTextView: TextView = itemView.findViewById(R.id.workoutDetails)
        val iconImageView: ImageView = itemView.findViewById(R.id.workout_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val (workoutId, workout) = workouts.getOrNull(position) ?: run {
            holder.titleTextView.text = "Ошибка: данные отсутствуют"
            holder.detailsTextView.text = ""
            holder.iconImageView.setImageResource(R.drawable.ic_walk)
            return
        }
        val activityType = formatActivityType(workout.activityType)
        val date = formatDate(workout.timestamp)
        holder.titleTextView.text = "$activityType, $date"

        val distance = formatDistance(workout.distance)
        val duration = formatDuration(workout.duration)
        val pace = formatPace(workout.avgPace)
        holder.detailsTextView.text = "$distance, $duration, $pace"

        // Установка иконки в зависимости от типа активности
        holder.iconImageView.setImageResource(
            when (workout.activityType) {
                "running" -> R.drawable.ic_run
                "cycling" -> R.drawable.ic_bike
                else -> R.drawable.ic_walk
            }
        )

        // Добавляем обработчик долгого нажатия
        holder.itemView.setOnLongClickListener {
            onLongClick(workoutId)
            true
        }
    }

    override fun getItemCount(): Int = workouts.size

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

    private fun formatDuration(duration: Long): String {
        val hours = duration / 3600000
        val minutes = (duration % 3600000) / 60000
        val seconds = (duration % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatDistance(distance: Double): String {
        return String.format("%.2f км", distance)
    }

    private fun formatPace(pace: String?): String {
        if (pace.isNullOrEmpty() || pace == "—") return "—"
        return try {
            // Предполагаем, что pace в формате мм:сс
            val parts = pace.split(":")
            if (parts.size != 2) return "—"
            val minutes = parts[0]
            val seconds = parts[1]
            // Преобразуем в мм"сс'
            "$minutes'$seconds\""
        } catch (e: Exception) {
            Log.e("WorkoutHistory", "Error formatting pace: $pace", e)
            "—"
        }
    }
}