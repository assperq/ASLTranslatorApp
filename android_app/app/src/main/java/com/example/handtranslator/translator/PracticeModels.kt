package com.example.handtranslator.translator

data class PracticeUiState(
    val mode: PracticeMode = PracticeMode.IDLE,
    val currentLetter: Letter? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val errorCount: Int = 0,
    val progress: Float = 0f,
    val successPercent: Int = 0,
    val elapsedMs: Long = 0L,
    val message: PracticeMessage? = null,
) {
    val canStart: Boolean get() = totalCount > 0 || mode == PracticeMode.IDLE
}

enum class PracticeMode { IDLE, RUNNING, FINISHED }

data class PracticeMessage(
    val type: PracticeMessageType,
    val expected: String? = null,
    val actual: String? = null,
)

enum class PracticeMessageType { CORRECT, WRONG, FINISHED }
