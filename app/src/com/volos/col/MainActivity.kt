/*
 * Copyright (c) 2017-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.volos.col

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.browser.trusted.sharing.ShareTarget.METHOD_GET
import androidx.browser.trusted.sharing.ShareTarget.RequestMethod
import com.salesforce.androidsdk.app.SalesforceSDKManager
import com.salesforce.androidsdk.mobilesync.app.MobileSyncSDKManager
import com.salesforce.androidsdk.rest.RestClient
import com.salesforce.androidsdk.rest.RestRequest
import com.salesforce.androidsdk.rest.RestResponse
import com.salesforce.androidsdk.ui.SalesforceActivity
import com.volos.col.bluetoothServices.BluetoothActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Main activity
 */
class MainActivity : SalesforceActivity() {

    private var client: RestClient? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // To remove additional header
        supportActionBar?.setDisplayShowTitleEnabled(false)

        //Set Theme
        val idDarkTheme = MobileSyncSDKManager.getInstance().isDarkTheme
        setTheme(if (idDarkTheme) R.style.SalesforceSDK_Dark else R.style.SalesforceSDK)
        MobileSyncSDKManager.getInstance().setViewNavigationVisibility(this)

        // Register the BroadcastReceiver
        val filter = IntentFilter("com.volos.col.DATA_RECEIVED")
        registerReceiver(dataReceiver, filter)

        // Setup view
        setContentView(R.layout.main)
    }

    override fun onResume() {
        // Hide everything until we are logged in
        findViewById<ViewGroup>(R.id.root).visibility = View.INVISIBLE

        super.onResume()
    }

    override fun onResume(client: RestClient) {
        // Keeping reference to rest client
        this.client = client

        // Show everything
        findViewById<ViewGroup>(R.id.root).visibility = View.VISIBLE
    }

    private val dataReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            val receivedData = intent?.getStringExtra("DATA")
            // Handle the received data here
            receivedData?.let {
                val splitted: List<String> = it.split("//")
                val SpO2: TextView = findViewById<TextView>(R.id.spo2)
                val Temp: TextView = findViewById<TextView>(R.id.temperature)
                val BPM: TextView = findViewById<TextView>(R.id.bpm)
                SpO2.text = "SpO2: " + splitted[0] + "%"
                Temp.text = "Температура: " + splitted[1] + "C*"
                BPM.text = "BPM: " + splitted[2]

                sendToSalesforce(splitted[0], splitted[1], splitted[2])
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun refreshData(v: View) {
        val SpO2: TextView = findViewById<TextView>(R.id.spo2)
        val Temp: TextView = findViewById<TextView>(R.id.temperature)
        val BPM: TextView = findViewById<TextView>(R.id.bpm)
        SpO2.text = "SpO2: No Data"
        Temp.text = "Temperature: No Data"
        BPM.text = "BPM: No Data"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendToSalesforce(SpO2: String, bodyTemp: String, BPM: String) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val currentTime = LocalDateTime.now().format(formatter).toString()
            val formattedTime = currentTime.split(" ")[0] + "T" + currentTime.split(" ")[1] + ".100+0000"

            val jsonObject = JSONObject()
            jsonObject.put("Body_Temperature__c", bodyTemp)
            jsonObject.put("Date_Time_Recorded__c", formattedTime)
            jsonObject.put("Heart_Beats_Minute__c", BPM)
            jsonObject.put("SpO2__c", SpO2)
            jsonObject.put("userId", client?.clientInfo?.userId)
            val body = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val req: RestRequest = RestRequest(RestRequest.RestMethod.POST, "https://spulse-dev-ed.develop.my.salesforce.com/services/apexrest/SPulse", body)

            client?.sendAsync(req, object : RestClient.AsyncRequestCallback {
                override fun onSuccess(request: RestRequest, result: RestResponse) {
                    result.consumeQuietly()
                    runOnUiThread {
                        try {
                            //Log.d("CONSOLE Request: ", request.toString())
                            Log.d("CONSOLE Response: ", result.toString())
                        } catch (e: Exception) {
                            Log.d("CONSOLE Exception: ", e.toString() + "\n" + e.message.toString())
                        }
                    }
                }

                override fun onError(exception: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity,
                            this@MainActivity.getString(R.string.sf__generic_error, exception.toString()),
                            Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e : Exception){
            Log.d("CONSOLE Exception: ", e.toString() + "\n" + e.message.toString())
        }
    }
    /**
     * Called when "Logout" button is clicked.

     * @param v
     */
    fun onLogoutClick(v: View) {
        SalesforceSDKManager.getInstance().logout(this)
    }

    fun runBluetoothActivity(view: View) {
        val intent = Intent(this, BluetoothActivity::class.java)
        startActivity(intent)
    }
}