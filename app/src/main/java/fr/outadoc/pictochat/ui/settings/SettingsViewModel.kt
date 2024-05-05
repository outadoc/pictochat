package fr.outadoc.pictochat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.outadoc.pictochat.preferences.LocalPreferences
import fr.outadoc.pictochat.preferences.LocalPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val localPreferencesRepository: LocalPreferencesRepository,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Ready(
            val current: LocalPreferences,
        ) : State
    }

    val state: StateFlow<State> =
        localPreferencesRepository.preferences
            .map { State.Ready(it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                State.Loading
            )

    fun updatePreferences(updated: LocalPreferences) {
        viewModelScope.launch(Dispatchers.IO) {
            localPreferencesRepository.updatePreferences(updated)
        }
    }
}
