package com.example.sportapplication.analytics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.R
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_history)

        recyclerView = findViewById(R.id.workoutRecyclerView)
        loadingTextView = findViewById(R.id.loadingTextView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadWorkouts()
    }

    private fun loadWorkouts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Пожалуйста, войдите в аккаунт", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadingTextView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val workoutsRef = FirebaseDatabase.getInstance().getReference("users/$userId/workouts")
        workoutsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val workouts = mutableListOf<Workout>()
                for (child in snapshot.children) {
                    try {
                        val workoutDTO = child.getValue(WorkoutDTO::class.java)
                        if (workoutDTO != null) {
                            val workout = workoutDTO.toWorkout()
                            workouts.add(workout)
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
                workouts.sortByDescending { it.timestamp }

                if (workouts.isEmpty()) {
                    loadingTextView.text = "Нет тренировок"
                    loadingTextView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    recyclerView.adapter = WorkoutAdapter(workouts)
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
}

class WorkoutAdapter(private val workouts: List<Workout>) :
    RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.workoutTitle)
        val detailsTextView: TextView = itemView.findViewById(R.id.workoutDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts.getOrNull(position) ?: run {
            holder.titleTextView.text = "Ошибка: данные отсутствуют"
            holder.detailsTextView.text = ""
            return
        }
        val activityType = formatActivityType(workout.activityType)
        val date = formatDate(workout.timestamp)
        holder.titleTextView.text = "$activityType, $date"

        val distance = formatDistance(workout.distance)
        val duration = formatDuration(workout.duration)
        val pace = workout.avgPace
        holder.detailsTextView.text = "$distance, $duration, $pace"
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
}