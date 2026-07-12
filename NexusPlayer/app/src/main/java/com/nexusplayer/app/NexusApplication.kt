package com.nexusplayer.app

import android.app.Application
import com.nexusplayer.app.data.local.NexusDatabase

class NexusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Room DB instance early
        NexusDatabase.getInstance(this)
    }
}
