package com.volos.col.bluetoothServices

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import com.volos.col.MainActivity
import com.volos.col.R
import java.io.IOException
import kotlin.reflect.typeOf

class BluetoothService() : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var device: BluetoothDevice? = null
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        bluetoothAdapter.bondedDevices.forEach {
            if (it.name == "ESP Pulse"){
                device = it
            }
        }

//        var socket = device?.createInsecureRfcommSocketToServiceRecord(BluetoothActivity.UUID_MY)
        val socket = BluetoothActivity.BTSocket

        if (socket != null) {
            connectToDevice(socket)
        } else {
            Toast.makeText(this, "An error occured...", 5).show()
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(socket: BluetoothSocket) {
        // Connect to the device and get the input stream
        // Start the data read thread
        val inputStream = socket.inputStream

        Thread {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream?.read(buffer) ?: -1
                    if (bytes != -1) {
                        val data = String(buffer, 0, bytes)

                        // Process the received data
                        processReceivedData(data)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }.start()
    }

    private fun processReceivedData(data: String) {
        if (data.length > 2) {
            val intent = Intent("com.volos.col.DATA_RECEIVED")
            intent.putExtra("DATA", data)
            sendBroadcast(intent)
        }
    }
}
