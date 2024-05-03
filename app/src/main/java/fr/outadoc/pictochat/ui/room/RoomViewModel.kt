package fr.outadoc.pictochat.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.UserProfile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoomViewModel(
    private val lobbyManager: LobbyManager,
) : ViewModel() {

    sealed class State {
        data object Idle : State()
        data class InRoom(
            val title: String,
            val currentMessage: String,
            val eventHistory: ImmutableList<ChatEvent>,
            val knownProfiles: ImmutableMap<DeviceId, UserProfile>,
        ) : State()
    }

    private data class InternalState(
        val roomId: RoomId? = null,
        val message: String = "",
    )

    private val _internalState = MutableStateFlow(InternalState())

    val state: StateFlow<State> = _internalState
        .flatMapMerge { internalState ->
            lobbyManager.state.map { lobbyState ->
                val currentRoom = lobbyState.rooms[internalState.roomId]
                if (currentRoom == null) {
                    State.Idle
                } else {
                    State.InRoom(
                        title = currentRoom.displayName,
                        currentMessage = internalState.message,
                        eventHistory = currentRoom.eventHistory,
                        knownProfiles = lobbyState.knownProfiles
                    )
                }
            }
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = State.Idle
        )

    fun onRoomSelected(roomId: RoomId) {
        _internalState.update { state ->
            state.copy(roomId = roomId)
        }
    }

    fun onSendMessage() {
        _internalState.update { state ->
            viewModelScope.launch(Dispatchers.IO) {
                lobbyManager.sendMessage(state.message)
            }

            state.copy(message = "")
        }
    }

    fun onMessageChanged(message: String) {
        _internalState.update { state ->
            state.copy(message = message)
        }
    }
}