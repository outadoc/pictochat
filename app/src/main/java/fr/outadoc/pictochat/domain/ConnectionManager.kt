package fr.outadoc.pictochat.domain

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface ConnectionManager : Closeable {

    val state: StateFlow<State>

    suspend fun startDiscovery()
    suspend fun startAdvertising()

    fun stopDiscovery()
    fun stopAdvertising()

    sealed interface State {
        data object Idle : State
    }
}
