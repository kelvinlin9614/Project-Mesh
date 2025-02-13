package com.greybox.projectmesh.model

import com.ustadmobile.meshrabiya.vnet.wifi.ConnectBand
import com.ustadmobile.meshrabiya.vnet.wifi.HotspotType
import com.ustadmobile.meshrabiya.vnet.wifi.state.MeshrabiyaWifiState

data class HomeScreenModel(
    val wifiState: MeshrabiyaWifiState? = null,
    val connectUri: String? = null,
    val localAddress: Int = 0,
    val band: ConnectBand = ConnectBand.BAND_2GHZ,
    val hotspotTypeToCreate: HotspotType = HotspotType.AUTO,
    val hotspotStatus: Boolean = false,
    val isWifiConnected: Boolean = false,
    val nodesOnMesh: Set<Int> = emptySet(),
){
    val wifiConnectionEnabled: Boolean
        get() = wifiState?.connectConfig != null
}
