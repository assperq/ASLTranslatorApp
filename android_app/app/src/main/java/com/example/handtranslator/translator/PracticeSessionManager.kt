package com.example.handtranslator.translator

import kotlin.math.roundToInt

class PracticeSessionManager(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var letters: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var errorCount: Int = 0
    private var startedAtMs: Long = 0L
    private var finishedAtMs: Long? = null

    fun start(sourceText: String): PracticeSessionSnapshot {
        letters = sourceText.mapNotNull { ch ->
            val value = ch.uppercaseChar()
            if (value in 'A'..'Z') value.toString() else null
        }
        currentIndex = 0
        errorCount = 0
        startedAtMs = clock()
        finishedAtMs = if (letters.isEmpty()) startedAtMs else null
        return snapshot()
    }

    fun submitPrediction(predictedLetter: String): PracticeCheckResult {
        if (letters.isEmpty() || finishedAtMs != null) return PracticeCheckResult.AlreadyFinished(snapshot())

        val expected = letters[currentIndex]
        val normalizedPrediction = predictedLetter.uppercase()
        return if (normalizedPrediction == expected) {
            currentIndex += 1
            if (currentIndex >= letters.size) finishedAtMs = clock()
            PracticeCheckResult.Correct(snapshot())
        } else {
            errorCount += 1
            PracticeCheckResult.Wrong(expected = expected, actual = normalizedPrediction, snapshot = snapshot())
        }
    }

    fun cancel(): PracticeSessionSnapshot {
        finishedAtMs = clock()
        return snapshot()
    }

    fun snapshot(): PracticeSessionSnapshot {
        val total = letters.size
        val completed = currentIndex.coerceAtMost(total)
        val finished = total == 0 || finishedAtMs != null
        val elapsedEnd = finishedAtMs ?: clock()
        val attempts = completed + errorCount
        val successPercent = if (attempts == 0) 0 else ((completed.toFloat() / attempts) * 100).roundToInt()

        return PracticeSessionSnapshot(
            letters = letters,
            currentIndex = completed,
            currentLetter = if (!finished && completed in letters.indices) letters[completed] else null,
            completedCount = completed,
            totalCount = total,
            errorCount = errorCount,
            startedAtMs = startedAtMs,
            finishedAtMs = finishedAtMs,
            elapsedMs = (elapsedEnd - startedAtMs).coerceAtLeast(0L),
            progress = if (total == 0) 0f else completed.toFloat() / total,
            successPercent = successPercent,
            isFinished = finished
        )
    }
}

data class PracticeSessionSnapshot(
    val letters: List<String>,
    val currentIndex: Int,
    val currentLetter: String?,
    val completedCount: Int,
    val totalCount: Int,
    val errorCount: Int,
    val startedAtMs: Long,
    val finishedAtMs: Long?,
    val elapsedMs: Long,
    val progress: Float,
    val successPercent: Int,
    val isFinished: Boolean,
)

sealed class PracticeCheckResult(open val snapshot: PracticeSessionSnapshot) {
    data class Correct(override val snapshot: PracticeSessionSnapshot) : PracticeCheckResult(snapshot)
    data class Wrong(val expected: String, val actual: String, override val snapshot: PracticeSessionSnapshot) : PracticeCheckResult(snapshot)
    data class AlreadyFinished(override val snapshot: PracticeSessionSnapshot) : PracticeCheckResult(snapshot)
}
