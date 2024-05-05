package fr.outadoc.pictochat.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            SettingsText(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = { Text("Display name") },
                subtitle = { Text(preferences.userProfile.displayName) }
            )
        }

        items(ProfileColor.entries) { item ->
            SettingsText(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = { Text("Favorite color") },
                trailingIcon = {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = item.toColor()
                    ) {}
                }
            )
        }
    }
}