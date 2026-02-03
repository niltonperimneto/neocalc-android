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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Display(
    displayText: String,
    currentMode: CalculatorMode,
    onModeClick: () -> Unit,
    history: List<String> = emptyList(),
    onHistoryItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Display Container
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
            // History List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                contentPadding = PaddingValues(bottom = Spacing.sm)
            ) {
                 items(history) { item ->
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
                                     val clip = ClipData.newPlainText("History", item)
                                     clipboard.setPrimaryClip(clip)
                                     Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                 }
                             )
                     ) {
                         Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)
                         )
                     }
                 }
            }
            
            Text(
                text = NumberFormatter.format(displayText),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ), // M3 displayMedium for main result
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
