package com.example.handtranslator.translator

class PredictionStabilizer(
    private val windowSize: Int
) {
    private val window = ArrayDeque<String>()
    private var lastSampledTimeMs = 0L

    fun shouldSample(nowMs: Long, sampleIntervalMs: Long): Boolean {
        if (window.isEmpty()) return true
        return nowMs - lastSampledTimeMs >= sampleIntervalMs
    }

    fun add(letter: String, sampledAtMs: Long) {
        window.addLast(letter)
        lastSampledTimeMs = sampledAtMs
        while (window.size > windowSize) window.removeFirst()
    }

    fun resolve(requiredMatches: Int): String? {
        if (window.size < windowSize) return null
        val majority = window.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: return null
        if (majority.value < requiredMatches) return null
        clear()
        return majority.key
    }

    fun clear() {
        window.clear()
        lastSampledTimeMs = 0L
    }
}
