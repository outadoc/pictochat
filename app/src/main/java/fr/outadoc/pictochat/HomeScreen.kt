package fr.outadoc.pictochat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

        val navController = rememberNavController()
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.joinedRoomId) {
            if (state.joinedRoomId != null) {
                navController.navigate("room/${state.joinedRoomId}")
            }
        }

        NavHost(
            navController = navController,
            startDestination = "home",
        ) {
            composable("home") {
                LaunchedEffect(Unit) {
                    viewModel.onLeaveRoom()
                }

                RoomListScreen(
                    rooms = state.rooms,
                    onRoomSelected = viewModel::onRoomSelected
                )
            }

            composable("room/{roomId}") {
                val room = remember(state.rooms) {
                    state.rooms.firstOrNull { it.id == state.joinedRoomId }
                }

                if (room != null) {
                    RoomScreen(
                        room = room,
                        onSendMessage = viewModel::onSendMessage
                    )
                }
            }
        }
    }
}
