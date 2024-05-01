package fr.outadoc.pictochat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.Room
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val lobbyManager: LobbyManager,
) : ViewModel() {

    data class State(
        val rooms: List<Room> = emptyList(),
        val joinedRoom: Room? = null,
    )

    val state = lobbyManager.state
        .map { state ->
            State(
                rooms = state.availableRooms,
                joinedRoom = state.joinedRoom
            )
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = State()
        )

    fun start() {
        viewModelScope.launch {
            lobbyManager.connect()
        }
    }

    fun onRoomSelected(room: Room) {
        viewModelScope.launch {
            lobbyManager.join(room)
        }
    }

    override fun onCleared() {
        super.onCleared()
        lobbyManager.close()
    }
}
