package com.volos.col

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity

class BluetoothActivity: AppCompatActivity() {
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bluetooth_main)

        //init bluetooth adapter
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        Log.d("jerk","Value: $bluetoothAdapter".toString());

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){

            } else {

            }
        }
    }
    
    fun onBackClick(v: View) {
        onBackClick(v)
    }
    private fun makeToast(thisC:Context, text:String, duration:Int) {
        val toast = makeText(this, text, duration) // in Activity
        toast.show()
    }
}