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
}
