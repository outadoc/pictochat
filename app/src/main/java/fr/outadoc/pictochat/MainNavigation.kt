package fr.outadoc.pictochat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.koin.compose.koinInject

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainNavigation() {
    val viewModel = koinInject<MainViewModel>()
    val permissionsState = rememberMultiplePermissionsState(REQUIRED_PERMISSIONS)

    PermissionLocked(
        permissionsState
    ) {
        LifecycleStartEffect(viewModel) {
            viewModel.onStart()
            onStopOrDispose {
                viewModel.onStop()
            }
        }

        val navController = rememberNavController()
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.currentDestination) {
            when (state.currentDestination) {
                is Route.Home -> {
                    navController.popBackStack(Route.Home, inclusive = false)
                }

                else -> {
                    navController.navigate(state.currentDestination) {
                        launchSingleTop = true
                    }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = Route.Home,
        ) {
            composable<Route.Home> {
                RoomListScreen(
                    rooms = state.rooms,
                    onRoomSelected = viewModel::onRoomSelected
                )
            }

            composable<Route.Room> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Room>()
                val room = remember(state.rooms) {
                    state.rooms.firstOrNull { it.id == route.roomId }
                }

                if (room != null) {
                    RoomScreen(
                        room = room,
                        onSendMessage = viewModel::onSendMessage,
                        onBackPressed = {
                            viewModel.onLeaveRoom()
                        }
                    )
                }
            }
        }
    }
}
