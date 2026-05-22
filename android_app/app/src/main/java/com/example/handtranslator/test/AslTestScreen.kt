package com.example.handtranslator.test

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.handtranslator.R
import kotlin.random.Random

private const val PREFS = "asl_test_prefs"
private const val BEST_STREAK_KEY = "best_streak"

private data class AslCard(val letter: String, @DrawableRes val drawableRes: Int)

private val deck = listOf(
    AslCard("A", R.drawable.asl_a), AslCard("B", R.drawable.asl_b), AslCard("C", R.drawable.asl_c),
    AslCard("D", R.drawable.asl_d), AslCard("E", R.drawable.asl_e), AslCard("F", R.drawable.asl_f),
    AslCard("G", R.drawable.asl_g), AslCard("H", R.drawable.asl_h), AslCard("I", R.drawable.asl_i),
    AslCard("J", R.drawable.asl_j), AslCard("K", R.drawable.asl_k), AslCard("L", R.drawable.asl_l),
    AslCard("M", R.drawable.asl_m), AslCard("N", R.drawable.asl_n), AslCard("O", R.drawable.asl_o),
    AslCard("P", R.drawable.asl_p), AslCard("Q", R.drawable.asl_q), AslCard("R", R.drawable.asl_r),
    AslCard("S", R.drawable.asl_s), AslCard("T", R.drawable.asl_t), AslCard("U", R.drawable.asl_u),
    AslCard("V", R.drawable.asl_v), AslCard("W", R.drawable.asl_w), AslCard("X", R.drawable.asl_x),
    AslCard("Y", R.drawable.asl_y), AslCard("Z", R.drawable.asl_z)
)

@Composable
fun AslTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentStreak by rememberSaveable { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(readBestStreak(context)) }

    var currentCard by remember { mutableStateOf(deck.random()) }
    var options by remember { mutableStateOf(generateOptions(currentCard.letter)) }
    var statusText by rememberSaveable { mutableStateOf("") }

    val shakeTransition = rememberInfiniteTransition(label = "shake")
    val rotation by shakeTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(260), repeatMode = RepeatMode.Reverse),
        label = "shake_anim"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = onBack) { Text(stringResource(R.string.back_to_main)) }
                Text(stringResource(R.string.test_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.streak_current, currentStreak), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.streak_best, bestStreak), style = MaterialTheme.typography.bodyLarge)

            Image(
                painter = painterResource(id = currentCard.drawableRes),
                contentDescription = stringResource(R.string.test_gesture_image),
                modifier = Modifier.size(220.dp).rotate(rotation)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    Button(onClick = {
                        val isCorrect = option == currentCard.letter
                        if (isCorrect) {
                            currentStreak += 1
                            statusText = context.getString(R.string.answer_correct)
                            if (currentStreak > bestStreak) {
                                bestStreak = currentStreak
                                saveBestStreak(context, bestStreak)
                            }
                        } else {
                            currentStreak = 0
                            statusText = context.getString(R.string.answer_incorrect, currentCard.letter)
                        }
                        currentCard = deck.random()
                        options = generateOptions(currentCard.letter)
                    }) { Text(option) }
                }
            }

            if (statusText.isNotBlank()) Text(statusText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun generateOptions(correctLetter: String): List<String> {
    val wrongLetter = deck.map { it.letter }.filter { it != correctLetter }.random(Random)
    return listOf(correctLetter, wrongLetter).shuffled(Random)
}

private fun readBestStreak(context: Context): Int =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(BEST_STREAK_KEY, 0)

private fun saveBestStreak(context: Context, value: Int) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(BEST_STREAK_KEY, value).apply()
}
