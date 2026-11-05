
package com.example.toolsappkotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

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

    private val readings = mutableListOf<Reading>()

    private val timeWindow = 10_000_000_000L // 10 seconds

    private val minValue = -20f
    private val maxValue = 20f

    private val leftPadding = 75f
    private val rightPadding = 25f
    private val topPadding = 35f
    private val bottomPadding = 55f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 28f
    }

    private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val yPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 150, 0)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val zPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
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

        postInvalidate()
    }

    @Synchronized
    fun clearGraph() {
        readings.clear()
        postInvalidate()
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

        // Horizontal grid lines and Y-axis labels
        for (i in 0..4) {
            val value = maxValue - i * 10f
            val y = graphTop + i * graphHeight / 4f

            canvas.drawLine(
                graphLeft, y, graphRight, y, gridPaint
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
                x, graphTop, x, graphBottom, gridPaint
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

        // Draw the three sensor axes
        synchronized(this) {
            if (readings.size < 2) return

            val latestTimestamp = readings.last().timestamp
            val earliestTimestamp = latestTimestamp - timeWindow

            fun drawAxis(
                selector: (Reading) -> Float,
                paint: Paint
            ) {
                val path = Path()

                readings.forEachIndexed { index, reading ->

                    val x = graphLeft +
                            ((reading.timestamp - earliestTimestamp).toFloat() /
                                    timeWindow) * graphWidth

                    val value = selector(reading).coerceIn(
                        minValue, maxValue
                    )

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
            drawAxis({ it.y }, yPaint)
            drawAxis({ it.z }, zPaint)

            // Current sensor values
            val latest = readings.last()

            canvas.drawText(
                "X: %.2f".format(latest.x),
                graphLeft,
                height - 5f,
                xPaint
            )

            canvas.drawText(
                "Y: %.2f".format(latest.y),
                graphLeft + 180f,
                height - 5f,
                yPaint
            )

            canvas.drawText(
                "Z: %.2f".format(latest.z),
                graphLeft + 360f,
                height - 5f,
                zPaint
            )
        }
    }
}
