package com.example.toolsappkotlin

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class PhysicsToolsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize once per process, including when Android restores another activity.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
    }
}
