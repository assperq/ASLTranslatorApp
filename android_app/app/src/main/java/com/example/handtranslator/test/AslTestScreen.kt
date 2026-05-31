package com.example.handtranslator.test

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.handtranslator.R
import kotlinx.coroutines.delay

@Composable
fun AslTestScreen(
    onBack: () -> Unit,
    onOpenGestureQuiz: () -> Unit,
    viewModel: AslTestViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var gameState by rememberSaveable { mutableStateOf(GameState.READY) }
    var currentStreak by rememberSaveable { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(viewModel.readBestStreak()) }
    var currentCard by remember { mutableStateOf(deck.first()) }
    var options by remember { mutableStateOf(listOf("A", "B")) }
    var answerState by rememberSaveable { mutableStateOf(AnswerState.IDLE) }
    var statusText by rememberSaveable { mutableStateOf("") }
    var secondsLeft by rememberSaveable { mutableLongStateOf(ROUND_DURATION) }
    val remainingCards = remember { mutableStateListOf<AslCard>() }

    fun ensureDeck() {
        if (remainingCards.isEmpty()) {
            remainingCards.addAll(deck.shuffled())
        }
    }

    fun nextRound() {
        ensureDeck()
        val next = remainingCards.removeAt(0)
        currentCard = next
        options = viewModel.generateOptions(next.letter)
        secondsLeft = ROUND_DURATION
        answerState = AnswerState.IDLE
    }

    fun resetToReady(message: String) {
        gameState = GameState.READY
        currentStreak = 0
        statusText = message
        answerState = AnswerState.IDLE
    }

    LaunchedEffect(gameState, currentCard.letter) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect
        secondsLeft = ROUND_DURATION
        while (secondsLeft > 0 && gameState == GameState.PLAYING && answerState == AnswerState.IDLE) {
            delay(1000)
            secondsLeft -= 1
        }
        if (gameState == GameState.PLAYING && answerState == AnswerState.IDLE && secondsLeft == 0L) {
            answerState = AnswerState.TIMEOUT
            statusText = context.getString(R.string.answer_timeout)
            delay(800)
            resetToReady(context.getString(R.string.game_ready_after_reset))
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
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount -> dragOffset += amount },
                    onDragEnd = { if (dragOffset > 160f) onBack(); dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f }
                )
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_main)
                    )
                }

                Text(
                    text = stringResource(R.string.test_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            AppStyledBestScore(bestStreak = bestStreak)
            Button(onClick = onOpenGestureQuiz, modifier = Modifier.fillMaxWidth(0.9f)) {
                Text(stringResource(R.string.open_gesture_quiz))
            }
            ScorePill(stringResource(R.string.streak_current, currentStreak), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            TimerBar(secondsLeft = secondsLeft)

            if (gameState == GameState.READY) {
                Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 5.dp, modifier = Modifier.fillMaxWidth(0.9f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.game_ready_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(statusText.ifBlank { stringResource(R.string.game_ready_subtitle) }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        Button(onClick = {
                            statusText = ""
                            gameState = GameState.PLAYING
                            nextRound()
                        }) { Text(stringResource(R.string.game_start_button)) }
                    }
                }
            } else {
                Image(painter = painterResource(currentCard.drawableRes), contentDescription = stringResource(R.string.test_gesture_image), modifier = Modifier.size(210.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    options.forEach { letter ->
                        AnimatedOptionChip(letter = letter, secondsLeft = secondsLeft) {
                            val isCorrect = letter == currentCard.letter
                            answerState = if (isCorrect) AnswerState.CORRECT else AnswerState.WRONG
                            statusText = if (isCorrect) context.getString(R.string.answer_correct) else context.getString(R.string.answer_incorrect, currentCard.letter)
                            if (isCorrect) {
                                currentStreak += 1
                                if (currentStreak > bestStreak) {
                                    bestStreak = currentStreak
                                    viewModel.saveBestStreak(bestStreak)
                                }
                            } else {
                                currentStreak = 0
                            }
                        }
                    }
                }
            }

            Text(statusText, color = statusColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }

    LaunchedEffect(answerState) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect
        if (answerState == AnswerState.CORRECT) {
            delay(650)
            nextRound()
        } else if (answerState == AnswerState.WRONG) {
            delay(700)
            resetToReady(context.getString(R.string.game_ready_after_reset))
        }
    }
}

@Composable
private fun AppStyledBestScore(bestStreak: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.best_series_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(bestStreak.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ScorePill(text: String, background: Color, content: Color) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth(0.9f), color = background) {
        Text(text = text, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 14.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content)
    }
}

@Composable
private fun TimerBar(secondsLeft: Long) {
    val progress = secondsLeft / ROUND_DURATION.toFloat()
    val color = if (secondsLeft <= 1) Color(0xFFD32F2F) else Color(0xFF00897B)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.timer_left, secondsLeft), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        Box(modifier = Modifier.width(240.dp).background(Color.LightGray.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).padding(2.dp)) {
            Box(modifier = Modifier.fillMaxWidth(progress).height(10.dp).background(color, RoundedCornerShape(14.dp)))
        }
    }
}

@Composable
private fun AnimatedOptionChip(letter: String, secondsLeft: Long, onPick: () -> Unit) {
    val intensity = (ROUND_DURATION - secondsLeft).coerceAtLeast(0)
    val amplitude = 2f + intensity * 1.2f
    val duration = (560L - intensity * 75L).coerceAtLeast(170L).toInt()
    val transition = rememberInfiniteTransition(label = "opt")
    val angle by transition.animateFloat(
        -amplitude,
        amplitude,
        animationSpec = infiniteRepeatable(animation = tween(duration, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "a"
    )
    Surface(
        onClick = onPick,
        shape = CircleShape,
        tonalElevation = 7.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.rotate(angle).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    ) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Text(letter, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}

