package fr.outadoc.pictochat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val lobbyManager: LobbyManager,
) : ViewModel() {

    data class State(
        val rooms: List<Room>,
    )

    private val _state = MutableStateFlow(
        State(
            rooms = lobbyManager.rooms
        )
    )
    val state = _state.asStateFlow()

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
