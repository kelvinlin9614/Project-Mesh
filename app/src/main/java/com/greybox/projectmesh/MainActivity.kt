package com.greybox.projectmesh

import android.content.ContentQueryMap
import android.content.Context
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.zxing.integration.android.IntentIntegrator
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.db.dao.MessageDao
import com.greybox.projectmesh.db.dao.UserDao
import com.greybox.projectmesh.db.entities.Message
import com.greybox.projectmesh.db.entities.User
import com.greybox.projectmesh.db.entities.UserMessage
import com.greybox.projectmesh.debug.CrashHandler
import com.greybox.projectmesh.debug.CrashScreenActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.ext.asInetAddress
import com.ustadmobile.meshrabiya.ext.requireAddressAsInt
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import com.ustadmobile.meshrabiya.vnet.LocalNodeState
import com.ustadmobile.meshrabiya.vnet.MeshrabiyaConnectLink
import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import com.ustadmobile.meshrabiya.vnet.wifi.HotspotType
import com.ustadmobile.meshrabiya.vnet.wifi.state.MeshrabiyaWifiState
import com.yveskalume.compose.qrpainter.rememberQrBitmapPainter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.PrintWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Scanner
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
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

        // crash screen
        CrashHandler.init(applicationContext,CrashScreenActivity::class.java)

        // Initialise Meshrabiya
        //initMesh();
        //thisNode = AndroidVirtualNode(
        //    appContext = applicationContext,
        //    dataStore = applicationContext.dataStore
        //)

        // Init db
        db = Room.databaseBuilder(applicationContext,MeshDatabase::class.java,"project-mesh-db").allowMainThreadQueries().build()
        messageDao = db.messageDao()
        userDao = db.userDao()


        // UUID
        val sharedPrefs = getSharedPreferences("project-mesh-uuid", Context.MODE_PRIVATE)

        // Read UUID, if not exists then generate one.
        if (!sharedPrefs.contains("UUID")) {
            // If it doesn't exist, add the string value
            val editor = sharedPrefs.edit()
            editor.putString("UUID", UUID.randomUUID().toString())
            editor.apply()
        }
        thisIDString = sharedPrefs.getString("UUID",null) ?: "ERROR"

        // Init self user
        if (!userDao.hasWithID(thisIDString))
        {
            userDao.initSelf( User(uuid=thisIDString,"Default name",0,0) )
        }

        // Load content
        setContent {
            PrototypePage()
        }

        // Allow networking on any port
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private lateinit var db: MeshDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var userDao: UserDao
    private lateinit var thisIDString: String

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "project_mesh_libmeshrabiya")

    @Composable
    private fun PrototypePage()
    {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            var thisNode by remember { mutableStateOf( AndroidVirtualNode(
                appContext = applicationContext,
                dataStore = applicationContext.dataStore
            ) ) }
            val nodes by thisNode.state.collectAsState(LocalNodeState())
            var connectLink by remember { mutableStateOf("")}

            //var connectionState by remember { mutableStateOf<LocalNodeState?>(null) }

            Text(text = "Project Mesh", fontSize = TextUnit(48f, TextUnitType.Sp))
            Text(text = "This device IP: ${nodes.address.addressToDotNotation()}")
            Text(text = "This device UUID: ${thisIDString}")
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

            // Crash button for testing the crash handler
            //Button(content = {Text("die")}, onClick = { throw Error("Crash and burn") } )

            Button(content = {Text("Scan QR code")}, onClick = {
                // Open QR code scanner
                //thisNodeForQr = thisNode
                //qrIntent.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
                //qrIntent.initiateScan()
                // Then gets called by intent reciever

                qrScannerLauncher.launch(ScanOptions().setOrientationLocked(false).setPrompt("Scan another device to join the Mesh").setBeepEnabled(false))

            }) //thisNode.meshrabiyaWifiManager.connectToHotspot()

            val hotspot: (type: HotspotType) -> Unit = {
                coroutineScope.launch {
                    // On start hotspot button click...
                    // Stop any hotspots
                    //thisNode.disconnectWifiStation()
                    //thisNode.meshrabiyaWifiManager.deactivateHotspot()

                    // Try 5GHz
                    try
                    {
                        thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = ConnectBand.BAND_5GHZ, hotspotType = it)
                    }
                    catch (e: Exception)
                    {
                        // Try 2.5GHz
                        try {
                            thisNode.setWifiHotspotEnabled(enabled=true, preferredBand = com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand.BAND_2GHZ, hotspotType = it)
                        }
                        catch (e: Exception)
                        {

                        }
                    }



                    // Report connect link
                    connectLink = thisNode.state.filter { it.connectUri != null }.firstOrNull().toString()
                }
            }

            val startHotspot: () -> Unit = {
                coroutineScope.launch {
                    thisNode.disconnectWifiStation()
                    thisNode = AndroidVirtualNode(
                        appContext = applicationContext,
                        dataStore = applicationContext.dataStore
                    )

                    // Try AUTO
                    hotspot(HotspotType.AUTO)

                    // Wait 5 sec...
                    delay(5000)

                    if (!nodes.wifiState.hotspotIsStarted)
                    {
                        // Recreate node
                        thisNode.disconnectWifiStation()
                        thisNode = AndroidVirtualNode(
                            appContext = applicationContext,
                            dataStore = applicationContext.dataStore
                        )
                        hotspot(HotspotType.LOCALONLY_HOTSPOT)
                    }
                }
            }

            LaunchedEffect(Unit) {
                startHotspot()
            }

            Button(content = {Text("Restart hotspot")}, onClick = {startHotspot()})
            //Button(content = {Text("Start hotspot (Auto)")}, onClick = {hotspot(HotspotType.AUTO)})
            //Button(content = {Text("Start hotspot (Wifi direct)")}, onClick = {hotspot(HotspotType.WIFIDIRECT_GROUP)})
            //Button(content = {Text("Start hotspot (Local only)")}, onClick = {hotspot(HotspotType.LOCALONLY_HOTSPOT)})

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



            //var chatLog by remember { mutableStateOf("") }
            Row {
                var newName by remember { mutableStateOf("") }
                TextField(
                    value = newName,
                    onValueChange = { newName = it},
                    label = { Text("Profile name") }
                )
                Button(content = {Text("Update")}, onClick = fun() {
                    coroutineScope.launch {
                        userDao.updateName(thisIDString,newName)
                    }
                })
            }

            Row {
                var chatMessage by remember { mutableStateOf("") }


                TextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it},
                    label = { Text("Message") }
                )
                Button(content = {Text("Send")}, onClick = fun() {
                    val newMessage = Message(content="You: $chatMessage\n", dateReceived = System.currentTimeMillis(), id=0, sender=thisIDString )
                    coroutineScope.launch {
                        messageDao.addMessage(newMessage)
                    }

                    // SEND TO NETWORK HERE
                    for (originatorMessage in nodes.originatorMessages) {
                        Log.d("DEBUG", "Sending '$chatMessage' to ${originatorMessage.value.lastHopAddr.addressToDotNotation()}")
                        val address: InetAddress = originatorMessage.value.lastHopAddr.asInetAddress()
                        val clientSocket = thisNode.socketFactory.createSocket(address,1337)
                        // Send the UUID string and the message together.
                        clientSocket.getOutputStream().write(("$thisIDString$chatMessage").toByteArray(
                            Charset.defaultCharset()) )
                        //clientSocket.getOutputStream().bufferedWriter().flush()
                        clientSocket.close()
                    }
                    chatMessage = ""
                })
            }

            //Text(text = chatLog)

            // Watch db for profiles
            val users by userDao.getAllFlow().collectAsState(ArrayList<User>())

            // Display
            Text("Users DB:", fontWeight = FontWeight.Bold)
            for (u in users)
            {
                Text("${u.uuid},${u.name},${u.address.addressToDotNotation()},${(System.currentTimeMillis() - u.lastSeen)/1000}s")
            }

            // Watch db for chat messages
            val messages by messageDao.getAllFlow().collectAsState(ArrayList<Message>())
            val messageUsers by userDao.messagesFromAllUsers().collectAsState(ArrayList<UserMessage>())

            // Display
            Text("Messages:", fontWeight = FontWeight.Bold)
            for (m in messageUsers)
            {
                // nicen the date
                val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm", Locale.getDefault())
                val date = Date(m.dateReceived)

                Text("[${dateFormat.format(date)}] ${m.name}: ${m.content}")
            }

            Button(content = {Text("Delete message history")}, onClick = fun() {
              coroutineScope.launch {
                  messageDao.deleteAll(messages)
              }
            })

            // Broadcast profile info every 10 seconds.
            // val jsonString = Json.encodeToString(myObject)

            // TCP Networking
            // Add to chat when recieve data
            LaunchedEffect(Unit) {
                Log.d("DEBUG","Launchedeffect?")
                //Runs once (like useEffect null in React)

                val mainHandler = Handler(Looper.getMainLooper())

                mainHandler.post(object : Runnable {
                    override fun run() {

                        coroutineScope.launch {
                            // Make sure our user object is up to date.
                            var thisUser = userDao.getID(thisIDString)
                            thisUser.lastSeen = System.currentTimeMillis()
                            thisUser.address = thisNode.addressAsInt
                            userDao.update(thisUser)

                            // Send to everyone
                            for (originatorMessage in nodes.originatorMessages) {
                                try {
                                    val address: InetAddress =
                                        originatorMessage.value.lastHopAddr.asInetAddress()
                                    val clientSocket =
                                        thisNode.socketFactory.createSocket(address, 1338)
                                    // Send the UUID string and the message together.
                                    clientSocket.getOutputStream().write(
                                        (Json.encodeToString(thisUser)).toByteArray(
                                            Charset.defaultCharset()
                                        )
                                    )
                                    //clientSocket.getOutputStream().bufferedWriter().flush()
                                    clientSocket.close()
                                } catch (_: Exception) {}

                            }
                        }

                        mainHandler.postDelayed(this, 1000 * 10)
                    }
                })

                // Thread for CHAT messages
                // Port: 1337
                // Receives: 36 char UUID then message content
                Thread(Runnable {
                    Log.d("DEBUG","TCP message thread started")
                    val serverSocket = ServerSocket(1337)

                    while (true) {
                        val socket = serverSocket.accept()
                        Log.d("DEBUG","Incoming chat...")

                        val msg = socket.getInputStream().readBytes().toString(
                            Charset.defaultCharset())

                        // The first 36 characters are the UUID - the rest are content.
                        val uuid = msg.substring(0, 36)
                        val content = msg.substring(36)

                        Log.d("DEBUG", "Message raw: $msg")
                        Log.d("DEBUG", "Message uuid: $uuid")
                        Log.d("DEBUG", "Message content: $content")

                        // Write into DB
                        val newMessage = Message(content=content, sender=uuid, dateReceived = System.currentTimeMillis(), id=0 )
                        coroutineScope.launch {
                            messageDao.addMessage(newMessage)
                        }

                        //chatLog += msg + '\n'

                        socket.close()
                        Log.d("DEBUG","Closed message connection")
                    }
                }).start()

                // Thread for USER messages
                // Port: 1338
                // Receives: 36 char UUID then message content
                Thread(Runnable {
                    Log.d("DEBUG","TCP profile thread started")
                    val serverSocket = ServerSocket(1338)

                    while (true) {
                        val socket = serverSocket.accept()
                        Log.d("DEBUG","Incoming user profile...")

                        val msg = socket.getInputStream().readBytes().toString(
                            Charset.defaultCharset())
                        Log.d("DEBUG","Profile JSON: $msg")
                        val user = Json.decodeFromString<User>(msg)

                        // Write into DB
                        coroutineScope.launch {
                            userDao.update(user)
                        }

                        //chatLog += msg + '\n'

                        socket.close()
                        Log.d("DEBUG","Closed profile connection")
                    }
                }).start()
            }

        }
    }

    //lateinit var thisNode: AndroidVirtualNode


    
}