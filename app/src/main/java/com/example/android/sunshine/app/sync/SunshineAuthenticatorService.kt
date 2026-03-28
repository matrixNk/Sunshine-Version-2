package com.example.android.sunshine.app.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * The service which allows the sync adapter framework to access the authenticator.
 */
class SunshineAuthenticatorService : Service() {

    private lateinit var mAuthenticator: SunshineAuthenticator

    override fun onCreate() {
        mAuthenticator = SunshineAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder = mAuthenticator.iBinder
}
