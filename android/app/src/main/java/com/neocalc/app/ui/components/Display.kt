package com.neocalc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement

@Composable
fun Display(
    displayText: String,
    history: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Minimalist container: No hard background, just padding and alignment.
    // The background is handled by the scaffold/theme surface.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp), // Increased padding
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            // History List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true, // Show newest at bottom (visually above current input)
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp)
            ) {
                 // History comes as oldest->newest. We want to see them just above the current display.
                 // With reverseLayout=true, item 0 is at the bottom. 
                 // So we should reverse the list if we want standard "chat/log" behavior where we scroll up to see old.
                 // Actually, standard logs: line 1, line 2... line N (bottom).
                 // If we use reverseLayout=true, we should pass reversed list or just rely on natural stacking?
                 // Let's just use standard LazyColumn with Arrangement.Bottom so it stacks from bottom up, 
                 // but we need to verify scroll behavior.
                 // Simpler: Just standard Column/LazyColumn filling space.
                 items(history.asReversed()) { item ->
                     Text(
                        text = item,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                     )
                 }
            }
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.displayLarge, // Use M3 Hero Typography
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
