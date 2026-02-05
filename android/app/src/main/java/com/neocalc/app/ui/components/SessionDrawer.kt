package com.neocalc.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neocalc.app.R
import com.neocalc.app.core.SessionManager
import com.neocalc.app.ui.style.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Session drawer showing all calculator sessions with switch/close/add functionality.
 */
@Composable
fun SessionDrawer(
    drawerState: DrawerState,
    sessions: List<SessionManager.Session>,
    currentSession: SessionManager.Session?,
    onSessionClick: (SessionManager.Session) -> Unit,
    onSessionClose: (SessionManager.Session) -> Unit,
    onAddSession: () -> Unit,
    scope: CoroutineScope,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    stringResource(R.string.calculator_sessions), 
                    modifier = Modifier.padding(Spacing.md), 
                    style = MaterialTheme.typography.titleMedium
                )
                
                sessions.forEach { session ->
                    NavigationDrawerItem(
                        label = { Text(session.name) },
                        selected = session == currentSession,
                        onClick = {
                            onSessionClick(session)
                            scope.launch { drawerState.close() }
                        },
                        badge = {
                            if (sessions.size > 1) {
                                IconButton(
                                    onClick = { onSessionClose(session) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.calculator_close_session),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(Spacing.sm + Spacing.xs),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.calculator_new_calculator)) },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, stringResource(R.string.content_desc_add)) },
                    onClick = {
                        onAddSession()
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(Spacing.sm + Spacing.xs),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        },
        content = content
    )
}
