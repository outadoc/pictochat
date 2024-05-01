package fr.outadoc.pictochat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(
    private val nearbyConnectionManager: NearbyConnectionManager,
) : ViewModel() {

    fun start() {
        viewModelScope.launch {
            nearbyConnectionManager.start()
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyConnectionManager.close()
    }
}