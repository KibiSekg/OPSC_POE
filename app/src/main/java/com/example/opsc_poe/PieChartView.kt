package com.example.opsc_poe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rectF = RectF()
    private var dataPoints = listOf<Pair<String, Double>>()

    // Custom color palette matching your Spend Smart app theme accents
    private val sliceColors = listOf(
        "#ED4B00", "#15174D", "#216999", "#4CAF50",
        "#FFB300", "#9C27B0", "#00BCD4", "#795548"
    )

    fun setData(categories: Map<String, Double>) {
        val total = categories.values.sum()
        if (total == 0.0) {
            dataPoints = emptyList()
        } else {
            // Map categories to percentage sweeps
            dataPoints = categories.map { it.key to (it.value / total) }
        }
        invalidate() // Redraw canvas layout
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        // Setup chart boundaries inside the view square
        val size = minOf(width, height)
        val padding = 16f
        rectF.set(
            padding,
            padding,
            size.toFloat() - padding,
            size.toFloat() - padding
        )

        var startAngle = 0f
        dataPoints.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second * 360f).toFloat()

            // Cycle through theme colors sequentially
            val colorHex = sliceColors[index % sliceColors.size]
            paint.color = Color.parseColor(colorHex)

            // Draw the structural piece layout slice
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }
    }
}