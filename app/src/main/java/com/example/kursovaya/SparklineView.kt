package com.example.kursovaya

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<Double> = emptyList()
    private var trend: RateTrend = RateTrend.UNKNOWN

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 45
    }

    fun setData(values: List<Double>, trend: RateTrend) {
        this.data = values
        this.trend = trend
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Лёгкая “сеточка”
        gridPaint.color = ContextCompat.getColor(context, android.R.color.darker_gray)
        canvas.drawLine(0f, h / 2f, w, h / 2f, gridPaint)

        if (data.size < 2) return

        val colorRes = when (trend) {
            RateTrend.UP -> android.R.color.holo_green_light
            RateTrend.DOWN -> android.R.color.holo_red_light
            RateTrend.SAME -> android.R.color.darker_gray
            RateTrend.UNKNOWN -> android.R.color.darker_gray
        }
        linePaint.color = ContextCompat.getColor(context, colorRes)

        val leftPad = 6f
        val rightPad = 6f
        val topPad = 6f
        val bottomPad = 6f

        val minV = data.minOrNull() ?: return
        val maxV = data.maxOrNull() ?: return
        val range = max(1e-9, maxV - minV) // чтобы не делить на 0

        val usableW = max(1f, w - leftPad - rightPad)
        val usableH = max(1f, h - topPad - bottomPad)

        val stepX = usableW / (data.size - 1)

        fun yOf(v: Double): Float {
            val t = ((v - minV) / range).toFloat()
            val y = topPad + (1f - t) * usableH
            return min(h - bottomPad, max(topPad, y))
        }

        val path = Path()
        data.forEachIndexed { i, v ->
            val x = leftPad + i * stepX
            val y = yOf(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        canvas.drawPath(path, linePaint)
    }
}
