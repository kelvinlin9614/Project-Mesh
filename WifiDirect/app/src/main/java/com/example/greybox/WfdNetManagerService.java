package com.example.greybox;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Build;
import android.util.Log;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/// PE_AUTO_CONNECT
public class WfdNetManagerService {
    private static final String TAG = "WfdNetManagerService";

    private int creationRetriesLeft = 3;
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    public String encryptedSSID;    // TODO: encrypted SSID. Maybe change to private with getter/setter
    public String encryptedPass;    // TODO: encrypted PASS. Maybe change to private with getter/setter
    final String SSID = "DIRECT-Gb";  // TODO: modify and verify if it's the same or the system adds more info
    final String PASS = "12345678";   // TODO: modify.
    String groupOwnerMacAddress;
    public WfdDnsSdManager wfdDnsSdManager;
    // TODO: Unfortunately, we have to know the name assigned to the wireless interface. Not sure if
    //  this might vary.
    final String WIRELESS_INTERFACE_NAME = "wlan0";
    public String deviceMacAddress;

    /// TODO: remove later. This is just for testing/debugging
    public boolean _isGO = false;
    ///

    // TODO: shouldn't the port be requested to the OS by calling SocketServer(0)? But that means
    //  we need to start first the ServerSocket before registering the service. This would
    //  contradict the instructions to open the ServerSocket at the end of initialization
    final int SERVER_PORT = 8888;

    // https://www.notion.so/grey-box/Findings-journal-d3c584f59f3544e0bb02ebb91d3b3e59?pvs=4#d8559b89c0ac47849f6126258394c528
    // The name is subject to change based on conflicts with other services advertised on the same network.
    // NOTE: does this mean that we could have a problem if we expect a fixed name? Check if we ever
    //  use the service name other than for registration.
    final String SERVICE_NAME = "greybox_mesh";
    final String SERVICE_TYPE = "_nsdgreybox._tcp"; // The service type uniquely identifies the service your device is advertising
    WifiP2pDnsSdServiceInfo dnsSdServiceInfo;

    // TODO: is it secure to have the encryption key as an attribute?
    private static final String ENCRYPTION_KEY = "TODO";


    // TODO: think if this class should manage also the WifiP2pManager instance, opening of the channel,
    //  and so on


    WfdNetManagerService(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        this.manager = manager;
        this.channel = channel;

        wfdDnsSdManager = new WfdDnsSdManager(manager, channel);
    }


    // ---------------------------------------------------------------------------------------------
    //  Methods
    // ---------------------------------------------------------------------------------------------
    public void createSoftAP() {

        creationRetriesLeft--;
        /// Temporary
        // Information about the current build, extracted from system properties.
        // NOTE: do we set some of these properties when building the app?
        Log.d(TAG, "Build.BOARD:    " + Build.BOARD);
        Log.d(TAG, "Build.BRAND:    " + Build.BRAND);
        Log.d(TAG, "Build.DEVICE:   " + Build.DEVICE);
        Log.d(TAG, "Build.DISPLAY:  " + Build.DISPLAY);
        Log.d(TAG, "Build.HARDWARE: " + Build.HARDWARE);
        Log.d(TAG, "Build.HOST:     " + Build.HOST);
        Log.d(TAG, "Build.ID:       " + Build.ID);
        Log.d(TAG, "Build.MANUFACTURER: " + Build.MANUFACTURER);
        Log.d(TAG, "Build.MODEL:    " + Build.MODEL);
        Log.d(TAG, "Build.PRODUCT:  " + Build.PRODUCT);
//        Log.d(TAG, "Build.SKU:      " + Build.SKU); // requires API 31, current is API 29
        Log.d(TAG, "Build.USER:  " + Build.USER);
        ///

        // Step 1
        // Try to get the MAC address since the beginning
        if (deviceMacAddress == null) {
            deviceMacAddress = getDeviceMacAddress();
        }

        if (deviceMacAddress.isEmpty()) {
            Log.e(TAG, "Failed to obtain the MAC address. Auto-connect won't work. Aborting.");
            return;
        }

        // Builder used to build WifiP2pConfig objects for creating or joining a group.
        WifiP2pConfig.Builder configBuilder = new WifiP2pConfig.Builder();

        // WifiP2pConfig.GROUP_OWNER_BAND_AUTO is optional, since it's default
        // Also, setGroupOperatingBand() and setGroupOperatingFrequency() are mutually exclusive
        // Network name MUST start with the prefix DIRECT-<xy>, where <xy> are alphanumeric characters.
        WifiP2pConfig config = configBuilder
                .setNetworkName(SSID)
                .setPassphrase(PASS)
                .build();


        manager.createGroup(channel, config, createGroupListener);
        //
    }

    // TODO: implement the real thing
    private String encryptInformation(String data) {
//        javax.crypto.Cipher.getInstance();
        return data;
    }

    /*
     * NOTE: This method works only if the target SDK is 29. Even more, it might fail if the application
     *  runs on Android 13.
     */
    public String getDeviceMacAddress() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface netInterface : networkInterfaces) {
                if (!netInterface.getName().equalsIgnoreCase(WIRELESS_INTERFACE_NAME)) continue;

                byte[] macBytes = netInterface.getHardwareAddress();
                if ((macBytes == null) || (macBytes.length == 0)) {
                    Log.d(TAG, "Failed to get the MAC address.");
                    return "";
                }

                StringBuilder macAddress = new StringBuilder();
                for (byte b : macBytes) {
                    macAddress.append(String.format("%02X:", b));
                }
                // Remove the last ":"
                macAddress.deleteCharAt(macAddress.length() - 1);
                Log.d(TAG, "MAC address: " + macAddress);

                return macAddress.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get the MAC address.");
            e.printStackTrace();
        }

        return "";
    }


    // Network Discovery Service
    // TODO: pass the encripted MAC address as a parameter or we encrypt it here? Who has
    //  the key to encrypt? I guess that's who answers the question.
    // TODO: if we fail to get the MAC address I think we need to return some error and just abort
    //  the whole operation, nut just "return" as it's done right now
    public void makeNSDBroadcast(String enSSID, String enPass) {
        Log.d(TAG, "start makeNSDBroadcast");

        // TODO: this `if` might not be in the final version. For now I hope it helps with debugging
        //  (calling directly the `makeNSDBroadcast()` through `_startDNSBroadcast()` without creating
        //  a new service info)
//        if (dnsSdServiceInfo != null) {
//            Log.d(TAG, " local service already added.");
//            Log.d(TAG, "end makeNSDBroadcast");
//
//            return ;
//        }

        // Create a string map containing information about your service.
        // Map is an ADT whereas HashMap is an implementation of Map
        Map<String, String> record = new HashMap<>();

        // TODO: encrypt MAC address
        // Encryption of the MAC address
        String enMac = deviceMacAddress;
        //

        Log.d(TAG, " Creting the record.");

        // TODO: The article suggests to concatenate the whole info, I guess that would be more secure
        record.put("mac", enMac);
        record.put("ssid", enSSID);
        record.put("pass", enPass);
        record.put("port", String.valueOf(SERVER_PORT));
        record.put("name", "greybox" + (int) (Math.random() * 1000));   // TODO: I guess I need to remove the random number

        // Service information.  Pass it an instance name, service type `_protocol._transportlayer`,
        // and the map containing information other devices will want once they connect to this one
        dnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, SERVICE_TYPE, record);

        Log.d(TAG, "dnsSdServiceInfo: " + dnsSdServiceInfo.toString());

        wfdDnsSdManager.addLocalService(dnsSdServiceInfo);

        Log.d(TAG, "end makeNSDBroadcast");
    }

    /// TODO: remove this later. It's just for testing/debugging purposes. We can do this here as
    //   long as SSID and PASS are not encrypted
    public void _startDNSBroadcast() {
        makeNSDBroadcast(SSID, PASS);
    }
    ///

    // Does this makes sense? This is just a wrapper.
    public void discoverServices() {
        wfdDnsSdManager.discoverServices();
    }

    // TODO: still unclear if we are going to need this. For now I hope will help to have a better
    //  behavior of the prototype, since the T95 doesn't seem to have a way to manually delete the
    //  groups as happens with mobile devices. This will be called on onDestroy()
    public void tearDown() {

        wfdDnsSdManager.removeLocalService(dnsSdServiceInfo);

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Group removed successfully.");
            }

            @Override
            public void onFailure(int errorCode) {
                Log.e(TAG, "Failed to remove group. Error code: " + errorCode);
            }
        });
    }


    // ---------------------------------------------------------------------------------------------
    //  Listeners (as attributes)
    // ---------------------------------------------------------------------------------------------
    // TODO: determine if the listener should be an action in this class or in MainActivity. If so,
    //  we would need an instance of MainActivity.
    WifiP2pManager.ActionListener createGroupListener = new WifiP2pManager.ActionListener() {

        @Override
        public void onSuccess() {
            Log.d(TAG, "Succeed to create group.");
            // TODO: encrypt SSID and PASS
            encryptedSSID = SSID;
            encryptedPass = PASS;
            //
        }

        @Override
        public void onFailure(int errorCode) {
            Log.d(TAG, "Failed to create group. Error code: " + errorCode);
            WfdErrorInterpreter.logError(errorCode);

            // TODO: is there any way to retry creation of the group if the first attempt failed?
            if ((errorCode == WifiP2pManager.BUSY) && creationRetriesLeft > 0) {
                Log.d(TAG, "Retrying...");
                createSoftAP();
            }
        }
    };
}
///
