package com.neocalc.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.neocalc.app.R
import com.neocalc.app.ui.style.DisplayCorner
import com.neocalc.app.ui.style.Spacing
import com.neocalc.app.ui.util.NumberFormatter
import uniffi.neocalc_backend.HistoryItem

@Composable
fun Display(
    displayText: String,
    history: List<HistoryItem> = emptyList(),
    onHistoryRestore: (HistoryItem) -> Unit = {},
    onHistoryInsertResult: (HistoryItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val panelColor = MaterialTheme.colorScheme.surfaceContainerLow

    // The display reads as one large rounded panel above the keypad.
    // (The mode selector lives in the top app bar, so history owns the
    // full panel height with nothing floating above it.)
    Surface(
        color = panelColor,
        shape = RoundedCornerShape(bottomStart = DisplayCorner, bottomEnd = DisplayCorner),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd)
            ) {
                // History, fading out toward the top edge for depth
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to panelColor,
                                    0.15f to Color.Transparent
                                )
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    contentPadding = PaddingValues(bottom = Spacing.sm, top = Spacing.md)
                ) {
                    items(history) { item ->
                        HistoryRow(
                            item = item,
                            onRestore = { onHistoryRestore(item) },
                            onInsertResult = { onHistoryInsertResult(item) }
                        )
                    }
                }

                val formattedText = remember(displayText) {
                    NumberFormatter.format(displayText)
                }

                // Current value: auto-sizes to fit and slides in on change
                AnimatedContent(
                    targetState = formattedText,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) { it / 3 } + fadeIn(tween(220)))
                            .togetherWith(fadeOut(tween(90)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "displayValue"
                ) { value ->
                    BasicText(
                        text = value,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 28.sp,
                            maxFontSize = 64.sp,
                            stepSize = 2.sp
                        ),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End
                        ),
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * A single history entry. Tapping the row reverts the calculator to that
 * entry's expression; tapping the result inserts just the result into the
 * current expression; long-pressing copies the whole line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    item: HistoryItem,
    onRestore: () -> Unit,
    onInsertResult: () -> Unit
) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.history_copied)
    val displayString = "${item.expression} = ${item.result}"

    val resultColor = if (item.isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(Spacing.sm),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onRestore,
                onLongClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("History", displayString))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs, horizontal = Spacing.xs),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.expression,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = " = ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.result,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = resultColor,
                maxLines = 1,
                modifier = Modifier
                    .then(
                        if (item.isError) Modifier
                        else Modifier.clickable(onClick = onInsertResult)
                    )
            )
        }
    }
}
