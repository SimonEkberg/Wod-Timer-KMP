package com.simon.wodtimer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.simon.wodtimer.App
import com.simon.wodtimer.platform.AndroidPlatform

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidPlatform.appContext = applicationContext
        AndroidPlatform.currentActivity = this
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        if (AndroidPlatform.currentActivity === this) {
            AndroidPlatform.currentActivity = null
        }
        super.onDestroy()
    }
}
