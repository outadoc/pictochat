package fr.outadoc.pictochat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.ui.permissionlock.PermissionLocked
import fr.outadoc.pictochat.ui.permissionlock.REQUIRED_PERMISSIONS
import fr.outadoc.pictochat.ui.room.RoomScreen
import fr.outadoc.pictochat.ui.roomlist.RoomListScreen
import fr.outadoc.pictochat.ui.settings.SettingsScreen
import kotlinx.collections.immutable.toImmutableList
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
                    nearbyUserCount = state.nearbyUserCount,
                    roomStates = state.roomStates.values.toImmutableList(),
                    isOnline = state.isOnline,
                    onToggleOnline = { online ->
                        if (online) viewModel.onStart() else viewModel.onStop()
                    },
                    onRoomSelected = viewModel::onRoomSelected,
                    onSettingsSelected = viewModel::onSettingsSelected
                )
            }

            composable<Route.Room> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Room>()
                RoomScreen(
                    roomId = RoomId(route.roomId),
                    onBackPressed = viewModel::onLeaveRoom
                )
            }

            composable<Route.Settings> {
                SettingsScreen(
                    onBackPressed = viewModel::onCloseSettings
                )
            }
        }
    }
}
