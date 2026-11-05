package com.example.toolsappkotlin

import com.example.toolsappkotlin.data.AccelerometerReading
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

object RecordingAnalysis {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-3.5-flash-lite")
    }

    suspend fun analyze(prompt: String): String? = model.generateContent(prompt).text

    internal fun buildPrompt(readings: List<AccelerometerReading>): String {
        require(readings.isNotEmpty())
        val firstTime = readings.first().sensorTimestampNanos
        return buildString {
            appendLine("Explain this complete accelerometer recording for a physics class in <=150 words: trends and limitations. No exact activity, velocity or distance claims.")
            appendLine("Reply in plain text: two short paragraphs labeled Trends: and Limitations:. No Markdown, LaTeX, asterisks, dollar signs or backslashes. Write units as m/s² and equations in words.")
            appendLine("Time is milliseconds since the first reading. X/Y/Z are in m/s² and include gravity (~9.81 at rest). Sampling intervals may vary; gaps are not zero motion.")
            appendLine("Reading count: ${readings.size}")
            appendLine("elapsed_ms,x,y,z")
            // Include every stored reading in order, without sampling or rounding the axes.
            readings.forEach { row ->
                val elapsedMillis = (row.sensorTimestampNanos - firstTime) / 1_000_000.0
                appendLine("$elapsedMillis,${row.x},${row.y},${row.z}")
            }
        }
    }
}
