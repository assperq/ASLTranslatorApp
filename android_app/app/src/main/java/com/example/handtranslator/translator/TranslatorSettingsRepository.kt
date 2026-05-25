package com.example.handtranslator.translator

import com.example.handtranslator.data.preferences.DataStoreManager

class TranslatorSettingsRepository(
    private val dataStoreManager: DataStoreManager
) {
    fun predictionCooldown() = dataStoreManager.getPredictionCooldown()
    suspend fun setPredictionCooldown(value: Long) = dataStoreManager.setPredictionCooldown(value)

    fun requiredMatches() = dataStoreManager.getRequiredMatches()
    suspend fun setRequiredMatches(value: Int) = dataStoreManager.setRequiredMatches(value)

    fun frameSampleIntervalMs() = dataStoreManager.getFrameSampleInterval()
    suspend fun setFrameSampleIntervalMs(value: Long) = dataStoreManager.setFrameSampleInterval(value)

    fun liveConfidenceThreshold() = dataStoreManager.getLiveConfidenceThreshold()
    suspend fun setLiveConfidenceThreshold(value: Float) = dataStoreManager.setLiveConfidenceThreshold(value)

    fun photoConfidenceThreshold() = dataStoreManager.getPhotoConfidenceThreshold()
    suspend fun setPhotoConfidenceThreshold(value: Float) = dataStoreManager.setPhotoConfidenceThreshold(value)

    fun videoConfidenceThreshold() = dataStoreManager.getVideoConfidenceThreshold()
    suspend fun setVideoConfidenceThreshold(value: Float) = dataStoreManager.setVideoConfidenceThreshold(value)

    fun videoFrameSampleIntervalMs() = dataStoreManager.getVideoFrameSampleInterval()
    suspend fun setVideoFrameSampleIntervalMs(value: Long) = dataStoreManager.setVideoFrameSampleInterval(value)

    fun videoPreviewFillEnabled() = dataStoreManager.getVideoPreviewFillEnabled()
    suspend fun setVideoPreviewFillEnabled(value: Boolean) = dataStoreManager.setVideoPreviewFillEnabled(value)

    fun singleFrameRecognitionTimeoutMs() = dataStoreManager.getSingleFrameRecognitionTimeoutMs()
    suspend fun setSingleFrameRecognitionTimeoutMs(value: Long) = dataStoreManager.setSingleFrameRecognitionTimeoutMs(value)
}
