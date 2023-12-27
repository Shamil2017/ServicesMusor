package com.example.servicesmusor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyReceiver: BroadcastReceiver() {
    private var shouldContinueWriting = false
    override fun onReceive(p0: Context?, intent: Intent?) {
        when (intent?.action)
        {
            "isRecording" -> {
                shouldContinueWriting = intent.getBooleanExtra("Recording", false)
                log("$shouldContinueWriting")
            }
        }
    }
}

private fun log(message: String) {
    Log.d("SERVICE_TAG", "MyReceiver: $message")
}