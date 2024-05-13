package com.volos.col.bluetoothServices

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.volos.col.R
import java.io.IOException
import java.util.UUID

val UUID_BT: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
class BluetoothActivity: AppCompatActivity() {
    //    Constants:
    private val toastLength: Int = 5
    private var isBound = false
    // End.

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var deviceList: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()
    private lateinit var availableDevicesToConnect: ListView
    private lateinit var connectedDevices: ListView
    private var availableBTDeviceNames: ArrayList<String> = ArrayList<String>()
    private var connectedBTDeviceNames: ArrayList<String> = ArrayList<String>()
    private var deviceMap: HashMap<String, String> = HashMap<String, String>()
    private var availableBTDevicesAdapter: ArrayAdapter<String>? = null
    private var connectedBTDevicesAdapter: ArrayAdapter<String>? = null
    private var bluetoothSocket: BluetoothSocket? = null

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission", "WrongConstant")
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bluetooth_main)

        availableDevicesToConnect = findViewById<ListView>(R.id.select_device_list)
        connectedDevices = findViewById<ListView>(R.id.connected_devices_list)

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        availableBTDevicesAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, availableBTDeviceNames)
        connectedBTDevicesAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, connectedBTDeviceNames)
        availableDevicesToConnect.adapter = availableBTDevicesAdapter
        connectedDevices.adapter = connectedBTDevicesAdapter

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
            startActivityForResult(enableBtIntent, 1)
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            ActivityCompat.requestPermissions(this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_PRIVILEGED
                    ), 1
            )
        }

        if(bluetoothAdapter.bondedDevices.size != 0) {
            bluetoothAdapter.bondedDevices.forEach {
                deviceMap[deviceName(it.name, it.address)] = it.address
                connectedBTDeviceNames.add(deviceName(it.name, it.address))
                connectedBTDevicesAdapter!!.notifyDataSetChanged()
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        val discoveryStarted: Boolean = bluetoothAdapter.startDiscovery()

        if (discoveryStarted){
            Toast.makeText(this, "Discovery Started...", toastLength).show()
        }

        availableDevicesToConnect.setOnItemClickListener { _, _, pos, _ ->
            val selectedDevice: String = availableBTDeviceNames[pos]
            val address: String? = deviceMap[selectedDevice]

            deviceList.forEach {
                if (it.address === address){
                    ConnectThread(it, this).start()
                }
            }
        }
        connectedDevices.setOnItemClickListener { _, _, pos, _ ->
            deviceMap[connectedBTDeviceNames[pos]]?.let { Log.d("CONSOLE Address: ", it) }
        }
    }

    private fun deviceName(name: String, address: String): String {
        return "Name: $name\n MAC: $address"
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
                    if (deviceName != null && deviceHardwareAddress != null && !connectedBTDeviceNames.contains(deviceName(deviceName, deviceHardwareAddress))) {
                        deviceMap[deviceName(deviceName, deviceHardwareAddress)] = deviceHardwareAddress
                        availableBTDeviceNames.add(deviceName(deviceName, deviceHardwareAddress))
                    }
                    availableBTDevicesAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the Bluetooth socket if connected
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
        super.onBackPressed()
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice, var context: Context) : Thread() {
        var deviceName: String = device.name
        var macAddress: String = device.address

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID_BT)
        }

        @SuppressLint("WrongConstant")
        public override fun start() {
            Toast.makeText(context, "Connecting to $deviceName...", toastLength).show()
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()
                bluetoothSocket = socket
                BTSocket = socket

                val serviceIntent = Intent(context, BluetoothService::class.java)
                startService(serviceIntent)

                Toast.makeText(context, "Successfully Connected to $deviceName!", toastLength).show()

                connectedBTDeviceNames.add(deviceName(deviceName, macAddress))
                connectedBTDevicesAdapter!!.notifyDataSetChanged()

                availableBTDeviceNames.remove(deviceName(deviceName, macAddress))
                availableBTDevicesAdapter?.notifyDataSetChanged()
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("CONSOLE IOException", "Could not close the client socket", e)
            }
        }
    }

    companion object {
        val UUID_MY: UUID = UUID_BT
        var BTSocket: BluetoothSocket? = null
    }
}