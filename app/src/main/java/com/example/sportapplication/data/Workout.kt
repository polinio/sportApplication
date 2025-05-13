package com.example.sportapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Workout(
    val activityType: String = "walking",
    val distance: Double = 0.0,     // в километрах
    val duration: Long = 0L,        // в миллисекундах
    val avgPace: String = "—",      // строка формата "X:XX"
    val calories: Int = 0,          // количество сожженных калорий
    val timestamp: Long = 0L,       // временная метка в миллисекундах от эпохи Unix
    val steps: Int = 0,             // количество шагов
    val intervals: List<Interval>? = null
) : Parcelable

data class WorkoutDTO(
    val activityType: String = "walking",
    val distance: Double = 0.0,     // в километрах
    val duration: Long = 0L,        // в миллисекундах
    val avgPace: String = "—",      // строка формата "X:XX"
    val calories: Int = 0,          // количество сожженных калорий
    val timestamp: Long = 0L,       // временная метка в миллисекундах от эпохи Unix
    val steps: Int = 0,             // количество шагов
    val intervals: List<IntervalDTO>? = null
)

// Методы преобразования
fun WorkoutDTO.toWorkout(): Workout = Workout(
    activityType = activityType,
    distance = distance,
    duration = duration,
    avgPace = avgPace,
    calories = calories,
    timestamp = timestamp,
    steps = steps,
    intervals = intervals?.map { it.toInterval() }
)

fun Workout.toDTO(): WorkoutDTO = WorkoutDTO(
    activityType = activityType,
    distance = distance,
    duration = duration,
    avgPace = avgPace,
    calories = calories,
    timestamp = timestamp,
    steps = steps,
    intervals = intervals?.map { it.toDTO() }
)