package com.example.sportapplication.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.util.Locale
import kotlin.math.min

object AvatarGenerator {

    private val colors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
        "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
        "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
        "#FF5722", "#795548", "#9E9E9E", "#607D8B"
    )

    fun generateAvatar(context: Context, initials: String, sizeDp: Int): Bitmap {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(colors[initials.hashCode() % colors.size])
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        // Text
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = sizePx * 0.4f
        }
        val text = initials.take(2).uppercase(Locale.getDefault())
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(
            text,
            sizePx / 2f,
            sizePx / 2f - textBounds.exactCenterY(),
            textPaint
        )

        return bitmap
    }
}