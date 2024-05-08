package com.volos.col.BluetoothService

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.IOException

class DataHandler (var context: Context) {
    private val socket = BluetoothActivity.BLUETOOTH_SOCKET
    @SuppressLint("MissingPermission")
    public fun readStream() {
        Log.d("jerk", socket.remoteDevice.address + " : " + socket.remoteDevice.name)
        Log.d("jerkData", "inputStream")
        // Start a separate thread to read data from the input stream
        Thread {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = socket.inputStream.read(buffer) ?: -1
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

    public fun writeStream(data: String) {
        val bytes = data.toByteArray()
        Log.d("jerkData", "outPutStream")
        try {
            socket.outputStream.write(bytes)
            socket.outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun processReceivedData(data: String) {
        println("Received data: $data")
        Log.d("jerkData12", data)
    }
}