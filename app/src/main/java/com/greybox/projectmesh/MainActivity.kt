package com.greybox.projectmesh

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.greybox.projectmesh.networking.HttpServer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.asInetAddress
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.LocalNodeState
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import com.ustadmobile.meshrabiya.vnet.wifi.HotspotType
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.time.Duration


class MainActivity : ComponentActivity() {

    private val webServer = HttpServer()
    private val okHttpClient = OkHttpClient()

    private var selectedFileUri by mutableStateOf<Uri?>(null)
    private val selectFile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedFileUri = uri
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request nearby devices permission
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 2)
            }
        }

        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 2)
        }

        // Initialise Meshrabiya
        //initMesh();
        //thisNode = AndroidVirtualNode(
        //    appContext = applicationContext,
        //    dataStore = applicationContext.dataStore
        //)

        // Start HTTP
        webServer.start()

        // Load content
        setContent {
            PrototypePage()
        }

        // Allow networking on any port
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "project_mesh_libmeshrabiya")

    @Composable
    private fun PrototypePage()
    {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val thisNode by remember { mutableStateOf( AndroidVirtualNode(
                appContext = applicationContext,
                dataStore = applicationContext.dataStore
            ) ) }
            val nodes by thisNode.state.collectAsState(LocalNodeState())
            var connectLink by remember { mutableStateOf("")}

            val okHttp by remember { mutableStateOf(
                OkHttpClient().newBuilder()
                    .socketFactory(thisNode.socketFactory)
                    .connectTimeout(Duration.ofSeconds(30))
                    .readTimeout(Duration.ofSeconds(30))
                    .writeTimeout(Duration.ofSeconds(30))
                    .build()
            )}

            //var connectionState by remember { mutableStateOf<LocalNodeState?>(null) }

            Text(text = "Project Mesh", fontSize = TextUnit(48f, TextUnitType.Sp))
            Text(text = "This device IP: ${nodes.address.addressToDotNotation()}")
            if (!nodes.connectUri.isNullOrEmpty())
            {
                Text(text = "Connection state: ${nodes.wifiState}")

                if (nodes.wifiState.hotspotIsStarted)
                {
                    Text(text = "Join URI: ${nodes.connectUri}")

                    // Show QR Code
                    Image(
                        painter = rememberQrBitmapPainter(
                            content = nodes.connectUri!!,
                            size = 300.dp,
                            padding = 1.dp
                        ),
                        contentDescription = null
                    )
                }
                else
                {
                    Text("Start a hotspot to show Connect Link and QR code")
                }



            }
            val coroutineScope = rememberCoroutineScope()
            val qrScannerLauncher = rememberLauncherForActivityResult(contract = ScanContract()) {
                    result ->
                val link = result.contents
                if (link != null)
                {
                    val connectConfig = MeshrabiyaConnectLink.parseUri(link).hotspotConfig
                    if(connectConfig != null) {
                        val job = coroutineScope.launch {
                            //try {
                            thisNode.connectAsStation(connectConfig)
                            //} catch (e: Exception) {
                            //Log(Log.ERROR,"Failed to connect ",e)
                            //}
                        }
                    }
                }
            }

            Button(content = {Text("Scan QR code")}, onClick = {
                // Open QR code scanner
                //thisNodeForQr = thisNode
                //qrIntent.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
                //qrIntent.initiateScan()
                // Then gets called by intent reciever

                qrScannerLauncher.launch(ScanOptions().setOrientationLocked(false).setPrompt("Scan another device to join the Mesh").setBeepEnabled(false))

            }) //thisNode.meshrabiyaWifiManager.connectToHotspot()

            //this crashes
            Button(content = {Text("HTTP Test")}, onClick = {
                val payload = "test payload"

                val requestBody = payload.toRequestBody()
                val request = Request.Builder()
                    .post(requestBody)
                    .url("http://${nodes.originatorMessages.entries.first().value.lastHopAddr.addressToDotNotation()}:8080/hello")
                    .build()
                Log.d("DEBUG","Trying to request ${request.url}")
                okHttp.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Handle this
                        Log.d("DEBUG","DIDNT GOT WEB")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        // Handle this
                        Log.d("DEBUG","GOT WEB: ${response.body} ${response.code}")
                    }
                })
            })



            val hotspot: (type: HotspotType) -> Unit = {
                coroutineScope.launch {
                    // On start hotspot button click...
                    // Stop any hotspots
                    //thisNode.disconnectWifiStation()
                    //thisNode.meshrabiyaWifiManager.deactivateHotspot()


                    thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = ConnectBand.BAND_5GHZ, hotspotType = it)
                    // Report connect link
                    connectLink = thisNode.state.filter { it.connectUri != null }.firstOrNull().toString()
                }
            }

            Button(content = {Text("Start hotspot (Auto)")}, onClick = {hotspot(HotspotType.AUTO)})
            Button(content = {Text("Start hotspot (Wifi direct)")}, onClick = {hotspot(HotspotType.WIFIDIRECT_GROUP)})
            Button(content = {Text("Start hotspot (Local only)")}, onClick = {hotspot(HotspotType.LOCALONLY_HOTSPOT)})





            Text(text = "Other nodes:")
            //nodes.originatorMessages.entries
            //if (nodes.originatorMessages.isEmpty())
            //{
            //    Text(text = "N/A")
            //}
            //else

            nodes.originatorMessages.entries.forEach {
                Text(  it.value.lastHopAddr.addressToDotNotation() + it.value.originatorMessage + it.value)

            }
            var chatLog by remember { mutableStateOf("") }

            //new button for selecting a file
            Button(content = { Text("Select File") }, onClick = {
                selectFile.launch("image/*") //adjust the MIME type as needed
            })

            //new button for sending the file
            Button(
                content = { Text("Send File") },
                onClick = {
                    selectedFileUri?.let { fileUri ->
                        val lastHopAddr = nodes.originatorMessages.values.firstOrNull()?.lastHopAddr?.addressToDotNotation()
                        if (lastHopAddr != null) {
                            sendFile(thisNode, fileUri, lastHopAddr)
                            Log.d("DEBUG", "Sending file $fileUri to $lastHopAddr")
                            chatLog += "You: $fileUri\n"
                        }

                        else {
                            Log.e("DEBUG", "Could not find last hop address")
                        }
                    }
                },
                enabled = selectedFileUri != null
            )



            Row {
                var chatMessage by remember { mutableStateOf("") }


                TextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it},
                    label = { Text("Message") }
                )
                Button(content = {Text("Send")}, onClick = fun() {

                    chatLog += "You: $chatMessage\n"
                    // SEND TO NETWORK HERE
                    for (originatorMessage in nodes.originatorMessages) {
                        Log.d("DEBUG", "Sending '$chatMessage' to ${originatorMessage.value.lastHopAddr.addressToDotNotation()}")
                        val address: InetAddress = originatorMessage.value.lastHopAddr.asInetAddress()
                        val clientSocket = thisNode.socketFactory.createSocket(address,1337)
                        clientSocket.getOutputStream().write(("(${originatorMessage.value.lastHopAddr.addressToDotNotation()}) $chatMessage\n").toByteArray(
                            Charset.defaultCharset()) )
                        //clientSocket.getOutputStream().bufferedWriter().flush()
                        clientSocket.close()
                    }
                    chatMessage = ""
                })
            }

            Text(text = chatLog)
            // TCP Networking
            // Add to chat when recieve data
//            LaunchedEffect(Unit) {
//                Log.d("DEBUG","Launchedeffect?")
//                //Runs once (like useEffect null in React)
//                // Thread that listens for TCP data on port 1337, prints to chat
//                Thread(Runnable {
//                    Log.d("DEBUG","TCP thread started")
//                    val serverSocket = ServerSocket(1337)
//
//                    while (true) {
//                        val socket = serverSocket.accept()
//                        Log.d("DEBUG","Incoming chat...")
//
//                        val msg = socket.getInputStream().readBytes().toString(
//                            Charset.defaultCharset())
//                        Log.d("DEBUG", "Message info: ${msg}")
//                        chatLog += msg + '\n'
//
//                        socket.close()
//                        Log.d("DEBUG","Closed connection")
//                    }
//                }).start()
//            }

            LaunchedEffect(Unit) {
                Log.d("DEBUG","Launchedeffect?")
                //Runs once (like useEffect null in React)
                // Thread that listens for TCP data on port 1337, prints to chat
                Thread(Runnable {
                    Log.d("DEBUG","TCP thread started")
                    val serverSocket = ServerSocket(1337)

                    while (true) {
                        val socket = serverSocket.accept()
                        Log.d("DEBUG","Incoming file...")

                        val inputStream = socket.getInputStream()

                        val folderPath = "/downloads/mesh-app"
                        val folder = File(folderPath)
                        if (!folder.exists()) {
                            folder.mkdirs() // Create folders if they don't exist
                        }

                        val filePath = "$folderPath/filename.ext"
                        val file = File(filePath)
                        val outputStream = FileOutputStream(file)

                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }

                        outputStream.close()

                        // Update UI to display received file
                        // Note: This code will need to be updated to work correctly in Compose
                        // For now, just log the received file path
                        Log.d("DEBUG", "Received file saved at: $filePath")

                        socket.close()
                        Log.d("DEBUG","Closed connection")
                    }
                }).start()
            }


        }
    }

    //lateinit var thisNode: AndroidVirtualNode

    //testing***********************************



    private fun sendFile(thisNode: AndroidVirtualNode, fileUri: Uri, lastHopAddr: String) {
        val inputStream = contentResolver.openInputStream(fileUri)
        val fileName = contentResolver.getType(fileUri)

        val socket = Socket()
        try {
            val port = 1337

            socket.connect(InetSocketAddress(lastHopAddr, port))
            val outputStream = socket.getOutputStream()

            //send filename lol
            fileName?.let { outputStream.write(it.toByteArray()) }

            //send file
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream?.read(buffer).also { bytesRead = it!! } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }


            Log.d("MainActivity", "File sent successfully")
        }

        catch (e: IOException) {
            Log.e("MainActivity", "Failed to send file", e)
        }

        finally {
            socket.close()
            inputStream?.close()
        }
    }

}
