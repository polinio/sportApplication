package com.example.sportapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Interval(
    val type: IntervalType,
    val targetDistance: Double, // Целевое расстояние в метрах
    val actualDistance: Double = 0.0, // Фактическое расстояние
    val duration: Long = 0L, // Длительность в миллисекундах
    val pace: String = "—" // Темп
) : Parcelable

enum class IntervalType {
    WARMUP, ACCELERATION, REST, COOLDOWN
}

data class IntervalDTO(
    val type: IntervalType = IntervalType.WARMUP, // Добавлено значение по умолчанию
    val targetDistance: Double = 0.0, // Добавлено значение по умолчанию
    val actualDistance: Double = 0.0, // Фактическое расстояние
    val duration: Long = 0L, // Длительность в миллисекундах
    val pace: String = "—" // Темп
)

// Методы преобразования
fun IntervalDTO.toInterval(): Interval = Interval(
    type = type,
    targetDistance = targetDistance,
    actualDistance = actualDistance,
    duration = duration,
    pace = pace
)

fun Interval.toDTO(): IntervalDTO = IntervalDTO(
    type = type,
    targetDistance = targetDistance,
    actualDistance = actualDistance,
    duration = duration,
    pace = pace
)