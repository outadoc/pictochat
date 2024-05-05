package fr.outadoc.pictochat.ui.navigation

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.domain.RoomState
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val lobbyManager: LobbyManager,
) : ViewModel() {

    private data class InternalState(
        val navFlow: NavFlow = NavFlow.Main,
    )

    private sealed interface NavFlow {
        data object Main : NavFlow
        data object Settings : NavFlow
    }

    private val _internalState = MutableStateFlow(InternalState())

    @Stable
    data class State(
        val isOnline: Boolean = false,
        val roomStates: PersistentMap<RoomId, RoomState> = persistentMapOf(),
        val nearbyUserCount: Int = 0,
        val currentDestination: Route = Route.Home,
    )

    val state: StateFlow<State> =
        combine(
            lobbyManager.state,
            _internalState
        ) { lobbyState, internalState ->
            State(
                isOnline = lobbyState.isOnline,
                roomStates = lobbyState.rooms,
                nearbyUserCount = lobbyState.nearbyUserCount,
                currentDestination = when (internalState.navFlow) {
                    NavFlow.Main -> {
                        if (lobbyState.joinedRoomId != null) {
                            Route.Room(lobbyState.joinedRoomId.value)
                        } else {
                            Route.Home
                        }
                    }

                    NavFlow.Settings -> {
                        Route.Settings
                    }
                }
            )
        }
            .onEach { Log.d("MainViewModel", "State: $it") }
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = State()
            )

    fun onStart() {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.connect()
        }
    }

    fun onRoomSelected(roomId: RoomId) {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.join(roomId)
        }
    }

    fun onSettingsSelected() {
        _internalState.value = InternalState(navFlow = NavFlow.Settings)
    }

    fun onLeaveRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.leaveCurrentRoom()
        }
    }

    fun onCloseSettings() {
        _internalState.value = InternalState(navFlow = NavFlow.Main)
    }

    fun onStop() {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.close()
        }
    }

    override fun onCleared() {
        super.onCleared()
        lobbyManager.close()
    }
}
