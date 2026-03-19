package com.example.handtranslator.translator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun TranslationPanel(
    recognizedText: List<Letter>,
    onClearRecognizedText: (Boolean) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Перевод",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            if (recognizedText.isNotEmpty()) {

                IconButton(
                    onClick = { onClearRecognizedText(true) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Удалить символ"
                    )
                }

                IconButton(
                    onClick = { onClearRecognizedText(false) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Очистить всё"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        RecognizedTextLazyRowWithGradient(recognizedText)
    }
}

@Composable
fun LetterCard(letter: Letter) {
    Card(
        modifier = Modifier.padding(5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(letter.imageCard),
                contentDescription = letter.name,
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                letter.name,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun RecognizedTextLazyRowWithGradient(recognizedText: List<Letter>) {
    val listState = rememberLazyListState()

    LaunchedEffect(recognizedText.size) {
        if (recognizedText.isNotEmpty()) {
            listState.animateScrollToItem(recognizedText.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
        ) {
            items(recognizedText) { letter ->
                LetterCard(letter)
            }
        }

        val firstVisibleItem = listState.firstVisibleItemIndex
        if (firstVisibleItem > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.CenterStart)
            )
        }

        val layoutInfo = listState.layoutInfo
        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisibleItem < layoutInfo.totalItemsCount - 1) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .align(Alignment.CenterEnd)
            )
        }
    }
}