package com.example.handtranslator.test

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel

private const val PREFS = "asl_test_prefs"
private const val BEST_STREAK_KEY = "best_streak"

class AslTestViewModel(application: Application) : AndroidViewModel(application) {
    fun generateOptions(correctLetter: String): List<String> {
        val wrongLetter = deck.map { it.letter }.filter { it != correctLetter }.random()
        return listOf(correctLetter, wrongLetter).shuffled()
    }

    fun readBestStreak(): Int = getApplication<Application>().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(BEST_STREAK_KEY, 0)
    fun saveBestStreak(value: Int) {
        getApplication<Application>().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(BEST_STREAK_KEY, value).apply()
    }
}
