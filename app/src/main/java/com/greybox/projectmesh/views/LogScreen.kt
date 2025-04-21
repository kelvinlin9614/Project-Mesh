package com.greybox.projectmesh.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.MNetLoggerAndroid
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.viewModel.LogScreenModel
import com.greybox.projectmesh.viewModel.LogScreenViewModel
import com.ustadmobile.meshrabiya.log.MNetLogger
import org.kodein.di.compose.localDI
import org.kodein.di.instance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(
    viewModel: LogScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { di, savedStateHandle -> LogScreenViewModel(di, savedStateHandle) },
            defaultArgs = null,
        )
    )
){
    val uiState by viewModel.uiState.collectAsState(initial = LogScreenModel())
    ShowLogScreen(uiState)
}

@Composable
fun ShowLogScreen(
    uiState: LogScreenModel
){
    val context = LocalContext.current
    val localDi = localDI()
    val logger: MNetLogger by localDi.instance()
    val androidLogger = logger as MNetLoggerAndroid
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault())
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = uiState.logs,
            key = {it.lineId}
        ) { logLine ->
            val formattedTime = timeFormatter.format(Date(logLine.time))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                ListItem(
                    modifier = Modifier.padding(4.dp),
                    headlineContent = {
                        Text("[$formattedTime] ${logLine.line}")
                    }
                )
            }
        }
    }
}
