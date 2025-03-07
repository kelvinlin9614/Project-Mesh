package com.greybox.projectmesh.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.db.entities.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kodein.di.DI
import com.greybox.projectmesh.server.AppServer
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.requireAddressAsInt
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.instance
import java.net.InetAddress

data class ChatScreenModel(
    val deviceName: String? = null,
    val virtualAddress: InetAddress = InetAddress.getByName("192.168.0.1"),
    val allChatMessages: List<Message> = emptyList()
)

class ChatScreenViewModel(
    di: DI,
    savedStateHandle: SavedStateHandle,
    virtualAddress: InetAddress
): ViewModel() {
    // _uiState will be updated whenever there is a change in the UI state
    private val deviceName = GlobalApp.DeviceInfoManager.getDeviceName(virtualAddress) ?: "Unknown"
    private val addressDotNotation = virtualAddress.requireAddressAsInt().addressToDotNotation()
    private val chatName: String = GlobalApp.DeviceInfoManager.getChatName(virtualAddress)
    private val _uiState = MutableStateFlow(
        ChatScreenModel(
            deviceName = deviceName,
            virtualAddress = virtualAddress
        )
    )
    // uiState is a read-only property that shows the current UI state
    val uiState: Flow<ChatScreenModel> = _uiState.asStateFlow()
    // di is used to get the AndroidVirtualNode instance
    private val db: MeshDatabase by di.instance()
    private val appServer: AppServer by di.instance()


    // launch a coroutine
    init {
        viewModelScope.launch {
            // collect the state flow of the AndroidVirtualNode
            db.messageDao().getChatMessagesFlow(chatName).collect{
                newChatMessages: List<Message> ->

                // update the UI state with the new state
                _uiState.update { prev ->
                    prev.copy(
                        allChatMessages = newChatMessages
                    )
                }
            }
        }
    }

    fun sendChatMessage(virtualAddress: InetAddress, message: String) {
        val sendTime: Long = System.currentTimeMillis()
        viewModelScope.launch {
            db.messageDao().addMessage(Message(0, sendTime, message, "Me", chatName))
        }
        appServer.sendChatMessage(virtualAddress, sendTime, message)
    }
}