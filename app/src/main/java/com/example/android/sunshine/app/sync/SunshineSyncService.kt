package com.example.android.sunshine.app.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class SunshineSyncService : Service() {

    companion object {
        private val sSyncAdapterLock = Any()
        private var sSunshineSyncAdapter: SunshineSyncAdapter? = null
    }

    override fun onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService")
        synchronized(sSyncAdapterLock) {
            if (sSunshineSyncAdapter == null) {
                sSunshineSyncAdapter = SunshineSyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = sSunshineSyncAdapter!!.syncAdapterBinder
}
