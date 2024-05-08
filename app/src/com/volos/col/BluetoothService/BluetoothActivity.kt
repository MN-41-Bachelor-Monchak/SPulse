package com.volos.col.BluetoothService

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.volos.col.R
import java.io.IOException
import java.util.UUID

class BluetoothActivity: AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    private val PERMISSIONS_STORAGE: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_PRIVILEGED
    )

    var DEVICE_MAC_ADDRESS: String = ""
    private val REQUEST_ENABLE_BT = 1
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var deviceList: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()
    private lateinit var availableDevicesToConnect: ListView
    private lateinit var connectedDevices: ListView
    private var devicesNamesToConnectArray: ArrayList<String> = ArrayList<String>()
    private var connectedDevicesNamesArray: ArrayList<String> = ArrayList<String>()
    private var deviceMap: HashMap<String, String> = HashMap<String, String>();
    private var devicesToConnectListViewAdapter: ArrayAdapter<String>? = null
    private var connectedDevicesListViewAdapter: ArrayAdapter<String>? = null
    private var MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private var bluetoothSocket: BluetoothSocket? = null
    private var pairedDevices: Set<BluetoothDevice>? = null

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission", "WrongConstant")
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bluetooth_main)

        availableDevicesToConnect = findViewById<ListView>(R.id.select_device_list)
        connectedDevices = findViewById<ListView>(R.id.connected_devices_list)
        val refreshButton = findViewById<Button>(R.id.select_device_refresh)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        devicesToConnectListViewAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            devicesNamesToConnectArray
        )
        connectedDevicesListViewAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            connectedDevicesNamesArray
        )
        availableDevicesToConnect.adapter = devicesToConnectListViewAdapter
        connectedDevices.adapter = connectedDevicesListViewAdapter

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            startActivityForResult(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                1
            )
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                1
            );
        }

        if(getPairedDevices()) {
            pairedDevices?.forEach {
                deviceMap.put(it.name, it.address)
                connectedDevicesNamesArray.add(it.name)
                connectedDevicesListViewAdapter!!.notifyDataSetChanged()
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        val discoveryStarted: Boolean = bluetoothAdapter.startDiscovery()

        if (discoveryStarted){
            Toast.makeText(this, "Discovery Started...", 10).show()
        }

        availableDevicesToConnect.setOnItemClickListener { _, _, pos, _ ->
            val selectedDevice: String = devicesNamesToConnectArray[pos]
            val address: String? = deviceMap[selectedDevice]

            deviceList.forEach {
                if (it.address === address){
                    ConnectThread(it, this).run()
                }
            }
        }

        refreshButton.setOnClickListener {

        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                    deviceList.add(device)

                    val deviceName = device.name // Name
                    val deviceHardwareAddress = device.address // MAC address
                    if (deviceName != null && deviceHardwareAddress != null && !connectedDevicesNamesArray.contains(deviceName)) {
                        deviceMap.put(deviceName, deviceHardwareAddress)
                        devicesNamesToConnectArray.add(deviceName)
                    }
                    devicesToConnectListViewAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getPairedDevices(): Boolean {
        pairedDevices = bluetoothAdapter.bondedDevices
        if(pairedDevices == null){
            return false
        } else {
            pairedDevices?.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
            }
            return true
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Close the Bluetooth socket if connected
        bluetoothSocket?.close()
        // Unregister the broadcast receiver
        unregisterReceiver(bluetoothReceiver)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onBackClick(v: View) {
        setContentView(R.layout.main)
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice, context: Context) : Thread() {
        var deviceName: String = device.name
        var macAddress: String = device.address
        var context: Context = context

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        public override fun run() {
            Toast.makeText(context, "Connecting to $deviceName...", 5).show()
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()
                BluetoothActivity.BLUETOOTH_SOCKET = socket
                DEVICE_MAC_ADDRESS = macAddress
                Toast.makeText(context, "Successfully Connected to $deviceName!", 5).show()

                connectedDevicesNamesArray.add(deviceName)
                connectedDevicesListViewAdapter!!.notifyDataSetChanged()

                devicesNamesToConnectArray.remove(deviceName)
                devicesToConnectListViewAdapter?.notifyDataSetChanged()

                // Socket connected successfully, get the input stream
                val DH = DataHandler(context)
                DH.readStream()
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("jerk IOException", "Could not close the client socket", e)
            }
        }
    }

    companion object {
        lateinit var BLUETOOTH_SOCKET: BluetoothSocket
    }
}