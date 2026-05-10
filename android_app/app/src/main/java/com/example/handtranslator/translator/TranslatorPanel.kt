package com.example.handtranslator.translator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.handtranslator.R

@Composable
fun TranslationPanel(
    recognizedText: List<Letter>,
    onClearRecognizedText: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    compactCards: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .heightIn(min = 140.dp, max = 260.dp)
            .padding(16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.translation_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            if (recognizedText.isNotEmpty()) {
                IconButton(onClick = { onClearRecognizedText(true) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.delete_last_symbol)
                    )
                }
                IconButton(onClick = { onClearRecognizedText(false) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_all)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (recognizedText.isEmpty()) {
            Text(
                text = stringResource(R.string.translation_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            RecognizedTextScrollableRow(
                recognizedText = recognizedText,
                compactCards = compactCards,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp)
            )
        }
    }
}

@Composable
fun LetterCard(letter: Letter, compact: Boolean) {
    val imageSize = if (compact) 72.dp else 96.dp
    val horizontalPadding = if (compact) 10.dp else 12.dp
    val verticalPadding = if (compact) 8.dp else 12.dp

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(letter.imageCard),
                contentDescription = letter.name,
                modifier = Modifier.size(imageSize)
            )
            Spacer(Modifier.height(6.dp))
            Text(letter.name, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun RecognizedTextScrollableRow(
    recognizedText: List<Letter>,
    compactCards: Boolean,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(recognizedText.size) {
        if (recognizedText.isNotEmpty()) {
            horizontalScrollState.animateScrollTo(horizontalScrollState.maxValue)
        }
    }

    Column(
        modifier = modifier.verticalScroll(verticalScrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recognizedText.forEach { letter ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LetterCard(letter = letter, compact = compactCards)
                }
            }
        }
    }
}
