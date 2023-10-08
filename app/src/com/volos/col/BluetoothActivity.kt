package com.volos.col

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity

class BluetoothActivity: AppCompatActivity() {
    // bluetooth Manager
//    private val bManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
//    private val bAdapter: BluetoothAdapter? = bManager.adapter
    private var REQUEST_ENABLE_BT:Int = 1
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bluetooth_main)

        //init bluetooth adapter
//        if (bAdapter == null) {
//            makeToast(this, "Device doesn't support Bluetooth", 5)
//        }
//
//        if (bAdapter?.isEnabled == false) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
////            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//        }
    }

    private fun makeToast(thisC:Context, text:String, duration:Int) {
        val toast = makeText(this, text, duration) // in Activity
        toast.show()
    }
}