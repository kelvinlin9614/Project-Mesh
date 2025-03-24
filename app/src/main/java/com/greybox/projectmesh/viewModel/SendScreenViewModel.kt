package com.greybox.projectmesh.viewModel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.server.AppServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

data class SendScreenModel(
    val outgoingTransfers: List<AppServer.OutgoingTransferInfo> = emptyList()
)

class SendScreenViewModel(
    di: DI,
    savedStateHandle: SavedStateHandle,
    private val onSwitchToSelectDestNode: (List<Uri>) -> Unit
): ViewModel() {
    // _uiState will be updated whenever there is a change in the UI state
    private val _uiState = MutableStateFlow(SendScreenModel())
    // uiState is a read-only property that shows the current UI state
    val uiState: Flow<SendScreenModel> = _uiState.asStateFlow()
    private val appServer: AppServer by di.instance()

    init {
        viewModelScope.launch{
            appServer.outgoingTransfers.collect{
                _uiState.update { prev ->
                    prev.copy(
                        outgoingTransfers = it
                    )
                }
            }
        }
    }

    fun onFileChosen(uris: List<Uri>){
        onSwitchToSelectDestNode(uris)
    }

    fun onDelete(transfer: AppServer.OutgoingTransferInfo){
        viewModelScope.launch {
            appServer.removeOutgoingTransfer(transfer.id)
        }
    }
}