package com.neocalc.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neocalc.app.core.CalculatorMode
import com.neocalc.app.ui.style.Spacing
import com.neocalc.app.ui.util.NumberFormatter
import com.neocalc.app.R
import androidx.compose.ui.res.stringResource
import uniffi.neocalc_backend.HistoryItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Display(
    displayText: String,
    currentMode: CalculatorMode,
    onModeClick: () -> Unit,
    history: List<HistoryItem> = emptyList(),
    onHistoryItemClick: (HistoryItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.history_copied)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.md),
    ) {
        // Mode Indicator (Top Center)
        Surface(
            onClick = onModeClick,
            shape = RoundedCornerShape(Spacing.sm + Spacing.xs),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                     text = currentMode.title.uppercase(),
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.md),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .padding(top = Spacing.xl)
        ) {
            // History List with HistoryItem support
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                contentPadding = PaddingValues(bottom = Spacing.sm)
            ) {
                 items(history) { item ->
                     val displayString = "${item.expression} = ${item.result}"
                     // Use error color for error items, high contrast for normal items
                     val textColor = if (item.isError) {
                         MaterialTheme.colorScheme.error
                     } else {
                         MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                     }
                     
                     Surface(
                         shape = RoundedCornerShape(Spacing.xs),
                         color = MaterialTheme.colorScheme.surface,
                         modifier = Modifier
                             .fillMaxWidth()
                             .combinedClickable(
                                 onClick = { onHistoryItemClick(item) },
                                 onLongClick = {
                                     // Copy to clipboard
                                     val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                     val clip = ClipData.newPlainText("History", displayString)
                                     clipboard.setPrimaryClip(clip)
                                     Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                                 }
                             )
                     ) {
                         Text(
                            text = displayString,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)
                         )
                     }
                 }
            }
            
            val formattedText = androidx.compose.runtime.remember(displayText) {
                NumberFormatter.format(displayText)
            }
            
            Text(
                text = formattedText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Legacy Display overload for backward compatibility.
 * Converts string history to HistoryItem format.
 */
@JvmName("DisplayWithStringHistory")
@Composable
fun Display(
    displayText: String,
    currentMode: CalculatorMode,
    onModeClick: () -> Unit,
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Convert strings to HistoryItems for backward compatibility
    val historyItems = history.map { item ->
        val parts = item.split(" = ")
        HistoryItem(
            expression = parts.getOrElse(0) { item },
            result = parts.getOrElse(1) { "" },
            timestamp = 0u,
            isError = false
        )
    }
    
    Display(
        displayText = displayText,
        currentMode = currentMode,
        onModeClick = onModeClick,
        history = historyItems,
        onHistoryItemClick = { historyItem -> 
            onHistoryItemClick("${historyItem.expression} = ${historyItem.result}")
        },
        modifier = modifier
    )
}
