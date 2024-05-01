package fr.outadoc.pictochat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
        LaunchedEffect(Any()) {
            viewModel.start()
        }

        RoomListScreen()
    }
}
