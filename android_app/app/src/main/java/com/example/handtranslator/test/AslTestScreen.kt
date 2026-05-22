package com.example.handtranslator.test

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.handtranslator.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val PREFS = "asl_test_prefs"
private const val BEST_STREAK_KEY = "best_streak"
private const val ROUND_DURATION = 5L

private data class AslCard(val letter: String, @DrawableRes val drawableRes: Int)
private enum class AnswerState { IDLE, CORRECT, WRONG, TIMEOUT }

private val deck = listOf(
    AslCard("A", R.drawable.asl_a), AslCard("B", R.drawable.asl_b), AslCard("C", R.drawable.asl_c), AslCard("D", R.drawable.asl_d), AslCard("E", R.drawable.asl_e),
    AslCard("F", R.drawable.asl_f), AslCard("G", R.drawable.asl_g), AslCard("H", R.drawable.asl_h), AslCard("I", R.drawable.asl_i), AslCard("J", R.drawable.asl_j),
    AslCard("K", R.drawable.asl_k), AslCard("L", R.drawable.asl_l), AslCard("M", R.drawable.asl_m), AslCard("N", R.drawable.asl_n), AslCard("O", R.drawable.asl_o),
    AslCard("P", R.drawable.asl_p), AslCard("Q", R.drawable.asl_q), AslCard("R", R.drawable.asl_r), AslCard("S", R.drawable.asl_s), AslCard("T", R.drawable.asl_t),
    AslCard("U", R.drawable.asl_u), AslCard("V", R.drawable.asl_v), AslCard("W", R.drawable.asl_w), AslCard("X", R.drawable.asl_x), AslCard("Y", R.drawable.asl_y), AslCard("Z", R.drawable.asl_z)
)

@Composable
fun AslTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var currentStreak by rememberSaveable { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(readBestStreak(context)) }
    var currentCard by remember { mutableStateOf(deck.random()) }
    var options by remember { mutableStateOf(generateOptions(currentCard.letter)) }
    var answerState by rememberSaveable { mutableStateOf(AnswerState.IDLE) }
    var statusText by rememberSaveable { mutableStateOf("") }
    var secondsLeft by rememberSaveable { mutableLongStateOf(ROUND_DURATION) }

    LaunchedEffect(currentCard.letter) {
        secondsLeft = ROUND_DURATION
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        if (answerState == AnswerState.IDLE) {
            answerState = AnswerState.TIMEOUT
            currentStreak = 0
            statusText = context.getString(R.string.answer_timeout)
            delay(900)
            currentCard = deck.random()
            options = generateOptions(currentCard.letter)
            answerState = AnswerState.IDLE
        }
    }

    val statusColor by animateColorAsState(
        when (answerState) {
            AnswerState.CORRECT -> Color(0xFF1E7F41)
            AnswerState.WRONG, AnswerState.TIMEOUT -> Color(0xFF9D1D1D)
            AnswerState.IDLE -> MaterialTheme.colorScheme.secondary
        }, label = "status"
    )

    Surface(
        modifier = Modifier.fillMaxSize().padding(12.dp).clip(RoundedCornerShape(24.dp)).pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, amount -> dragOffset += amount },
                onDragEnd = { if (dragOffset > 160f) onBack(); dragOffset = 0f },
                onDragCancel = { dragOffset = 0f }
            )
        }, color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_main))
                }
                Text(stringResource(R.string.test_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("⟵", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            }

            ScorePill(stringResource(R.string.streak_best, bestStreak), Color(0xFF6A1B9A))
            ScorePill(stringResource(R.string.streak_current, currentStreak), Color(0xFF1565C0))

            TimerBar(secondsLeft = secondsLeft)

            Image(painter = painterResource(currentCard.drawableRes), contentDescription = stringResource(R.string.test_gesture_image), modifier = Modifier.size(210.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                options.forEachIndexed { idx, letter ->
                    AnimatedOptionChip(letter = letter, delayMs = idx * 120L) {
                        val isCorrect = letter == currentCard.letter
                        answerState = if (isCorrect) AnswerState.CORRECT else AnswerState.WRONG
                        statusText = if (isCorrect) context.getString(R.string.answer_correct) else context.getString(R.string.answer_incorrect, currentCard.letter)
                        if (isCorrect) {
                            currentStreak += 1
                            if (currentStreak > bestStreak) {
                                bestStreak = currentStreak
                                saveBestStreak(context, bestStreak)
                            }
                        } else {
                            currentStreak = 0
                        }
                    }
                }
            }

            Text(statusText, color = statusColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }

    LaunchedEffect(answerState) {
        if (answerState == AnswerState.CORRECT || answerState == AnswerState.WRONG) {
            delay(850)
            currentCard = deck.random()
            options = generateOptions(currentCard.letter)
            answerState = AnswerState.IDLE
        }
    }
}

@Composable
private fun ScorePill(text: String, accent: Color) {
    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 5.dp, modifier = Modifier.fillMaxWidth(0.9f)) {
        Text(text = text, modifier = Modifier.fillMaxWidth().background(accent.copy(alpha = 0.12f)).padding(vertical = 10.dp, horizontal = 14.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = accent)
    }
}

@Composable
private fun TimerBar(secondsLeft: Long) {
    val progress = secondsLeft / ROUND_DURATION.toFloat()
    val color = if (secondsLeft <= 1) Color(0xFFD32F2F) else Color(0xFF00897B)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.timer_left, secondsLeft), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        Box(modifier = Modifier.width(240.dp).clip(RoundedCornerShape(14.dp)).background(Color.LightGray.copy(alpha = 0.35f)).padding(2.dp)) {
            Box(modifier = Modifier.fillMaxWidth(progress) .height(10.dp).clip(RoundedCornerShape(14.dp)).background(color))
        }
    }
}

@Composable
private fun AnimatedOptionChip(letter: String, delayMs: Long, onPick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "opt")
    val angle by transition.animateFloat(-3f, 3f, animationSpec = infiniteRepeatable(tween(550, delayMillis = delayMs.toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    Surface(onClick = onPick, shape = CircleShape, tonalElevation = 7.dp, modifier = Modifier.rotate(angle).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Text(letter, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun generateOptions(correctLetter: String): List<String> {
    val wrongLetter = deck.map { it.letter }.filter { it != correctLetter }.random(Random)
    return listOf(correctLetter, wrongLetter).shuffled(Random)
}

private fun readBestStreak(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(BEST_STREAK_KEY, 0)
private fun saveBestStreak(context: Context, value: Int) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(BEST_STREAK_KEY, value).apply() }
