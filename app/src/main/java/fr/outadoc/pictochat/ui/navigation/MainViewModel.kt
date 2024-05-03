package fr.outadoc.pictochat.ui.navigation

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.RoomState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val lobbyManager: LobbyManager,
) : ViewModel() {

    @Stable
    data class State(
        val roomStates: List<RoomState> = emptyList(),
        val nearbyUserCount: Int = 0,
        val currentDestination: Route = Route.Home,
    )

    val state = lobbyManager.state
        .map { state ->
            State(
                roomStates = state.rooms,
                nearbyUserCount = state.nearbyUserCount,
                currentDestination = if (state.joinedRoomId != null) {
                    Route.Room(state.joinedRoomId)
                } else {
                    Route.Home
                }
            )
        }
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

    fun onRoomSelected(roomId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.join(roomId)
        }
    }

    fun onLeaveRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.leaveCurrentRoom()
        }
    }

    fun onSendMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            lobbyManager.sendMessage(message)
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
