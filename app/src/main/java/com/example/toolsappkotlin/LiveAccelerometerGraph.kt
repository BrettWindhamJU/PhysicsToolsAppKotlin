
package com.example.toolsappkotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.ceil

class LiveAccelerometerGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Reading(
        val timestamp: Long,
        val x: Float,
        val y: Float,
        val z: Float
    )

    var pressureMode = false
    private val readings = mutableListOf<Reading>()

    private val timeWindow = 10_000_000_000L // 10 seconds

    // Dynamic Y-axis scale
    private var minValue = -20f
    private var maxValue = 20f

    private val defaultScale = 20f
    private val scalePadding = 1.2f

    private val leftPadding = 75f
    private val rightPadding = 25f
    private val topPadding = 35f
    private val bottomPadding = 55f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ju_disabled)
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ju_secondary_text)
        textSize = 28f
    }

    private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ju_green)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val yPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ju_graph_gold)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val zPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ju_text)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    @Synchronized
    fun addReading(timestamp: Long, x: Float, y: Float, z: Float) {

        readings.add(Reading(timestamp, x, y, z))

        val cutoff = timestamp - timeWindow

        readings.removeAll {
            it.timestamp < cutoff
        }

        updateAxisScale()

        postInvalidate()
    }

    @Synchronized
    fun clearGraph() {
        readings.clear()

        minValue = -defaultScale
        maxValue = defaultScale

        postInvalidate()
    }

    // Automatically adjust the Y-axis scale
    private fun updateAxisScale() {

        val highestValue = readings.maxOfOrNull {
            maxOf(
                abs(it.x),
                abs(it.y),
                abs(it.z)
            )
        } ?: defaultScale

        // Add 20% extra space above the highest reading
        val requiredScale = highestValue * scalePadding

        // Round up to the next multiple of 10
        val newScale = maxOf(
            defaultScale,
            ceil(requiredScale / 10f).toInt() * 10f
        )

        maxValue = newScale
        minValue = -newScale
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val graphWidth = width - leftPadding - rightPadding
        val graphHeight = height - topPadding - bottomPadding

        if (graphWidth <= 0 || graphHeight <= 0) return

        val graphLeft = leftPadding
        val graphRight = width - rightPadding
        val graphTop = topPadding
        val graphBottom = height - bottomPadding

        // Background
        canvas.drawColor(Color.WHITE)

        // Horizontal grid lines and dynamic Y-axis labels
        for (i in 0..4) {

            val value = maxValue -
                    i * (maxValue - minValue) / 4f

            val y = graphTop + i * graphHeight / 4f

            canvas.drawLine(
                graphLeft,
                y,
                graphRight,
                y,
                gridPaint
            )

            canvas.drawText(
                value.toInt().toString(),
                15f,
                y + 9f,
                textPaint
            )
        }

        // Vertical grid lines
        for (i in 0..5) {

            val x = graphLeft + i * graphWidth / 5f

            canvas.drawLine(
                x,
                graphTop,
                x,
                graphBottom,
                gridPaint
            )

            canvas.drawText(
                "${i * 2}s",
                x - 15f,
                height - 15f,
                textPaint
            )
        }

        // Axis title
        canvas.drawText(
            "Acceleration (m/s²)",
            graphLeft,
            25f,
            textPaint
        )

        synchronized(this) {

            if (readings.size < 2) return

            val latestTimestamp = readings.last().timestamp
            val earliestTimestamp = latestTimestamp - timeWindow

            // Draw each sensor axis
            fun drawAxis(
                selector: (Reading) -> Float,
                paint: Paint
            ) {

                val path = Path()

                readings.forEachIndexed { index, reading ->

                    val x = graphLeft +
                            ((reading.timestamp - earliestTimestamp).toFloat() /
                                    timeWindow) * graphWidth

                    // Use dynamic scale without clipping
                    val value = selector(reading)

                    val y = graphBottom -
                            ((value - minValue) /
                                    (maxValue - minValue)) * graphHeight

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                canvas.drawPath(path, paint)
            }

            drawAxis({ it.x }, xPaint)
            if (!pressureMode) drawAxis({ it.y }, yPaint)
            if (!pressureMode) drawAxis({ it.z }, zPaint)

            // Current sensor values
            val latest = readings.last()

            canvas.drawText(
                (if (pressureMode) "hPa: %.2f" else "X: %.2f").format(latest.x),
                graphLeft,
                height - 5f,
                xPaint
            )

            canvas.drawText(
                if (pressureMode) "" else "Y: %.2f".format(latest.y),
                graphLeft + 180f,
                height - 5f,
                yPaint
            )

            canvas.drawText(
                if (pressureMode) "" else "Z: %.2f".format(latest.z),
                graphLeft + 360f,
                height - 5f,
                zPaint
            )
        }
    }
}


