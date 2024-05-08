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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.salesforce.androidsdk.app.SalesforceSDKManager
import com.salesforce.androidsdk.mobilesync.app.MobileSyncSDKManager
import com.salesforce.androidsdk.rest.RestClient
import com.salesforce.androidsdk.ui.SalesforceActivity
import com.volos.col.BluetoothService.BluetoothActivity
import com.volos.col.BluetoothService.DataHandler


/**
 * Main activity
 */
class MainActivity : SalesforceActivity() {

    private var client: RestClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // To remove additional header
        supportActionBar?.setDisplayShowTitleEnabled(false)

        //Set Theme
        val idDarkTheme = MobileSyncSDKManager.getInstance().isDarkTheme
        setTheme(if (idDarkTheme) R.style.SalesforceSDK_Dark else R.style.SalesforceSDK)
        MobileSyncSDKManager.getInstance().setViewNavigationVisibility(this)

//        Log.d("jerk", "hello"); //(debug)

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