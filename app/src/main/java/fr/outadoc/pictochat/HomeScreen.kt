package fr.outadoc.pictochat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.koin.compose.koinInject

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen() {
    val viewModel = koinInject<MainViewModel>()
    val permissionsState = rememberMultiplePermissionsState(REQUIRED_PERMISSIONS)

    PermissionLocked(
        permissionsState
    ) {
        LaunchedEffect(Unit) {
            viewModel.start()
        }

        val state by viewModel.state.collectAsState()

        RoomListScreen(
            rooms = state.rooms,
            onRoomSelected = viewModel::onRoomSelected
        )
    }
}
