package com.neocalc.app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neocalc.app.R
import com.neocalc.app.ui.style.Spacing

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tutorial_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TutorialSection(
                    title = stringResource(R.string.tutorial_section_basic),
                    content = stringResource(R.string.tutorial_basic_content)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                TutorialSection(
                    title = stringResource(R.string.tutorial_section_modes),
                    content = stringResource(R.string.tutorial_modes_content)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                TutorialSection(
                    title = stringResource(R.string.tutorial_section_gestures),
                    content = stringResource(R.string.tutorial_gestures_content)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                TutorialSection(
                    title = stringResource(R.string.tutorial_section_history),
                    content = stringResource(R.string.tutorial_history_content)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                TutorialSection(
                    title = stringResource(R.string.tutorial_section_sessions),
                    content = stringResource(R.string.tutorial_sessions_content)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                TutorialSection(
                    title = stringResource(R.string.tutorial_section_themes),
                    content = stringResource(R.string.tutorial_themes_content)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tutorial_confirm))
            }
        }
    )
}

@Composable
private fun TutorialSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.xs)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
