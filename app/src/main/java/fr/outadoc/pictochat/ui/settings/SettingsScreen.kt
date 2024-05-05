package fr.outadoc.pictochat.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.domain.ProfileColor
import fr.outadoc.pictochat.preferences.LocalPreferences
import fr.outadoc.pictochat.ui.theme.toColor
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
) {
    val viewModel = koinInject<SettingsViewModel>()
    val state by viewModel.state.collectAsState()

    BackHandler(onBack = onBackPressed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { insets ->
        when (val currentState = state) {
            is SettingsViewModel.State.Loading -> {
                CircularProgressIndicator()
            }

            is SettingsViewModel.State.Ready -> {
                SettingsContent(
                    modifier = modifier.padding(insets),
                    preferences = currentState.current,
                    onUpdatePreferences = viewModel::updatePreferences
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    preferences: LocalPreferences,
    onUpdatePreferences: (LocalPreferences) -> Unit,
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            SettingsHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text("Your profile")
            }
        }

        item {
            var openDialog by remember { mutableStateOf(false) }

            SettingsText(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = { Text("Display name") },
                subtitle = { Text(preferences.userProfile.displayName) },
                onClick = {
                    openDialog = true
                },
                onClickLabel = "Change display name"
            )

            if (openDialog) {
                var editValue by remember { mutableStateOf(preferences.userProfile.displayName) }

                AlertDialog(
                    title = { Text(text = "What's your name?") },
                    text = {
                        TextField(
                            value = editValue,
                            onValueChange = {
                                editValue = it
                            }
                        )
                    },
                    onDismissRequest = { openDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onUpdatePreferences(
                                    preferences.copy(
                                        userProfile = preferences.userProfile.copy(
                                            displayName = editValue
                                        )
                                    )
                                )
                                openDialog = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { openDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }

        item {
            var openDialog by remember { mutableStateOf(false) }

            SettingsText(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = { Text("Favorite color") },
                onClick = { openDialog = true },
                trailingIcon = {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        color = preferences.userProfile.displayColor.toColor()
                    ) {}
                },
            )

            if (openDialog) {
                var selectedColor by remember { mutableStateOf(preferences.userProfile.displayColor) }

                AlertDialog(
                    title = { Text(text = "What's your favorite color?") },
                    text = {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(ProfileColor.entries) { color ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        color = color.toColor()
                                    ) {
                                        IconButton(onClick = {
                                            selectedColor = color
                                        }) {
                                            if (selectedColor == color) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onDismissRequest = { openDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onUpdatePreferences(
                                    preferences.copy(
                                        userProfile = preferences.userProfile.copy(
                                            displayColor = selectedColor
                                        )
                                    )
                                )
                                openDialog = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { openDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }
    }
}