package com.greybox.projectmesh.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.viewModel.ChatScreenModel
import com.greybox.projectmesh.viewModel.ChatScreenViewModel
import com.greybox.projectmesh.viewModel.HomeScreenViewModel
import com.greybox.projectmesh.viewModel.PingScreenViewModel
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.mmcp.MmcpOriginatorMessage
import org.kodein.di.compose.localDI
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatScreen(
    virtualAddress: InetAddress,
    onClickButton: () -> Unit,
    viewModel: ChatScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { di, savedStateHandle -> ChatScreenViewModel(di, savedStateHandle, virtualAddress)},
            defaultArgs = null
        )
    )
) {
    // declare the UI state, we can use the uiState to access the current state of the viewModel
    val uiState: ChatScreenModel by viewModel.uiState.collectAsState(initial = ChatScreenModel())
    var textMessage by rememberSaveable { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp)) {
            DisplayAllMessages(uiState, onClickButton)
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(4.dp)) {
            TextField(modifier = Modifier.weight(3f),
                value = textMessage,
                onValueChange = {
                    textMessage = it
                })
            Button(modifier = Modifier.weight(1f), onClick = {
                val message = textMessage.trimEnd()
                if(message.isNotEmpty()) {
                    viewModel.sendChatMessage(virtualAddress, message)
                    // resets the text field
                    textMessage = ""
                }
            }) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun DisplayAllMessages(uiState: ChatScreenModel, onClickButton: () -> Unit) {
    val context = LocalContext.current
    // display all chat messages
    LazyColumn{
        item{
            Row(modifier = Modifier.fillMaxWidth()){
                Text(modifier = Modifier.weight(3f),
                    text = "Device name: ${uiState.deviceName}, IP address: ${uiState.virtualAddress.hostAddress}"
                )
                Button(modifier = Modifier.weight(1f),
                    onClick = onClickButton) {
                    Text(text = "Ping")
                }
            }
        }
        items( // todo
            items = uiState.allChatMessages
        ){ chatMessage ->
            Spacer(modifier = Modifier.width(4.dp))
            Row(modifier = Modifier.fillMaxWidth()){
                val sender: String = if(chatMessage.sender == "Me")
                    "Me"
                else
                        (GlobalApp.DeviceInfoManager.getDeviceName(uiState.virtualAddress) ?: "Loading...")
                LongPressCopyableText(
                    context = context,
                    text = "$sender [${SimpleDateFormat("HH:mm").format(Date(chatMessage.dateReceived))}]: ",
                    textCopyable = "${chatMessage.content}",
                    textSize = 15
                )
            }
        }
    }
}