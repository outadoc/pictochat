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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val lobbyManager: LobbyManager,
) : ViewModel() {

    @Stable
    data class State(
        val isOnline: Boolean = false,
        val roomStates: PersistentMap<RoomId, RoomState> = persistentMapOf(),
        val nearbyUserCount: Int = 0,
        val currentDestination: Route = Route.Home,
    )

    val state = lobbyManager.state
        .map { state ->
            State(
                isOnline = state.isOnline,
                roomStates = state.rooms,
                nearbyUserCount = state.nearbyUserCount,
                currentDestination = if (state.joinedRoomId != null) {
                    Route.Room(state.joinedRoomId.value)
                } else {
                    Route.Home
                }
            )
        }
        .onEach { Log.d("MainViewModel", "State: $it")}
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

    fun onLeaveRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.leaveCurrentRoom()
        }
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
