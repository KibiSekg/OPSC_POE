package com.example.opsc_poe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log // Imported Android Log utility
import android.view.View

/**
 * Custom View layer designed to calculate, slice, and draw data percentages onto a 2D Canvas.
 * Interprets Map key-value definitions directly into proportional angular sweep slices.
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "PieChartView"
    }

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

    /**
     * Consumes categorization metrics maps, computes aggregate financial volumes,
     * and maps items to decimal ratio values used to slice the view.
     */
    fun setData(categories: Map<String, Double>) {
        val total = categories.values.sum()
        Log.d(TAG, "setData: Received ${categories.size} categories. Total aggregate weight calculated: R $total")

        if (total == 0.0) {
            Log.w(TAG, "setData: Total categorical values sum to 0. Emptying active data array maps.")
            dataPoints = emptyList()
        } else {
            // Map categories to percentage sweeps
            dataPoints = categories.map { it.key to (it.value / total) }
            Log.d(TAG, "setData: Dataset normalization mapping completed successfully. Invalidating view layout.")
        }

        invalidate() // Requests Android layout pipeline to redraw view canvas
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) {
            Log.d(TAG, "onDraw: Data points map is empty. Skipping active rendering pass.")
            return
        }

        // Setup chart boundaries inside the view square layout constraints
        val size = minOf(width, height)
        val padding = 16f
        rectF.set(
            padding,
            padding,
            size.toFloat() - padding,
            size.toFloat() - padding
        )

        Log.d(TAG, "onDraw: Starting Canvas sweep operations. Bound limits size: ${size}px, Padding: ${padding}f")

        var startAngle = 0f
        dataPoints.forEachIndexed { index, pair ->
            // Convert normalized decimals into geometric radial arc limits (360 degrees)
            val sweepAngle = (pair.second * 360f).toFloat()

            // Cycle through hex color definitions sequentially to prevent indexing out of bounds
            val colorHex = sliceColors[index % sliceColors.size]
            paint.color = Color.parseColor(colorHex)

            Log.d(TAG, "Rendering Arc Slice -> Index: $index, Label: '${pair.first}', " +
                    "Ratio: ${(pair.second * 100).toInt()}%, Start: ${startAngle}°, Sweep: ${sweepAngle}°")

            // Draw the structural piece layout slice directly on the 2D canvas boundary grid
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)

            // Advance the cursor tracking point forward by the size of the drawn slice
            startAngle += sweepAngle
        }

        Log.d(TAG, "onDraw: Canvas rendering pass completed successfully.")
    }
}