package com.example.handtranslator.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { settings ->
            settings[key] = value
        }
    }

    fun <T> read(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        context.dataStore.data.map { settings ->
            settings[key] ?: defaultValue
        }

    private val PREDICTION_COOLDOWN_KEY = longPreferencesKey("prediction_cooldown_key")

    suspend fun setPredictionCooldown(value: Long) = save(PREDICTION_COOLDOWN_KEY, value)
    fun getPredictionCooldown(): Flow<Long> = read(PREDICTION_COOLDOWN_KEY, 200L)

    private val REQUIRED_MATCHES_KEY = intPreferencesKey("required_matches_key")

    suspend fun setRequiredMatches(value: Int) = save(REQUIRED_MATCHES_KEY, value)
    fun getRequiredMatches(): Flow<Int> = read(REQUIRED_MATCHES_KEY, 2)

    private val FRAME_SAMPLE_INTERVAL_MS = longPreferencesKey("frame_sample_interval_key")

    suspend fun setFrameSampleInterval(value: Long) = save(FRAME_SAMPLE_INTERVAL_MS, value)
    fun getFrameSampleInterval(): Flow<Long> = read(FRAME_SAMPLE_INTERVAL_MS, 90L)

    private val LIVE_CONFIDENCE_THRESHOLD = floatPreferencesKey("live_confidence_threshold_key")

    suspend fun setLiveConfidenceThreshold(value: Float) = save(LIVE_CONFIDENCE_THRESHOLD, value)
    fun getLiveConfidenceThreshold(): Flow<Float> = read(LIVE_CONFIDENCE_THRESHOLD, 0.45f)

    private val PHOTO_CONFIDENCE_THRESHOLD = floatPreferencesKey("photo_confidence_threshold_key")

    suspend fun setPhotoConfidenceThreshold(value: Float) = save(PHOTO_CONFIDENCE_THRESHOLD, value)
    fun getPhotoConfidenceThreshold(): Flow<Float> = read(PHOTO_CONFIDENCE_THRESHOLD, 0.35f)

    private val VIDEO_CONFIDENCE_THRESHOLD = floatPreferencesKey("video_confidence_threshold_key")

    suspend fun setVideoConfidenceThreshold(value: Float) = save(VIDEO_CONFIDENCE_THRESHOLD, value)
    fun getVideoConfidenceThreshold(): Flow<Float> = read(VIDEO_CONFIDENCE_THRESHOLD, 0.2f)

    private val VIDEO_FRAME_SAMPLE_INTERVAL_MS = longPreferencesKey("video_frame_sample_interval_key")

    suspend fun setVideoFrameSampleInterval(value: Long) = save(VIDEO_FRAME_SAMPLE_INTERVAL_MS, value)
    fun getVideoFrameSampleInterval(): Flow<Long> = read(VIDEO_FRAME_SAMPLE_INTERVAL_MS, 1500L)

    private val VIDEO_PREVIEW_FILL_ENABLED = intPreferencesKey("video_preview_fill_enabled_key")

    suspend fun setVideoPreviewFillEnabled(value: Boolean) = save(VIDEO_PREVIEW_FILL_ENABLED, if (value) 1 else 0)
    fun getVideoPreviewFillEnabled(): Flow<Boolean> = read(VIDEO_PREVIEW_FILL_ENABLED, 0).map { it == 1 }

    private val SINGLE_FRAME_RECOGNITION_TIMEOUT_MS = longPreferencesKey("single_frame_recognition_timeout_ms_key")

    suspend fun setSingleFrameRecognitionTimeoutMs(value: Long) = save(SINGLE_FRAME_RECOGNITION_TIMEOUT_MS, value)
    fun getSingleFrameRecognitionTimeoutMs(): Flow<Long> = read(SINGLE_FRAME_RECOGNITION_TIMEOUT_MS, 2500L)
}
