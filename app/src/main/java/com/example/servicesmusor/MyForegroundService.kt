package com.example.servicesmusor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.Intent as Intent

class MyForegroundService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    var myRef: DatabaseReference = database.getReference("message")
    private lateinit var controlRef: DatabaseReference
    private var shouldContinueWriting = true




    override fun onCreate() {
        super.onCreate()
        log("onCreate")
        controlRef = database.getReference("Control")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        controlRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val controlValue = dataSnapshot.getValue(String::class.java)
                if (controlValue == "Stop") {
                    shouldContinueWriting = false
                    // Notify MainActivity to stop recording
                    //val stopRecordingIntent = Intent(this@MyForegroundService, MainActivity::class.java)
                    //stopRecordingIntent.action = "stopRecording"
                    //applicationContext.sendBroadcast(stopRecordingIntent)
                    log("Stop")
                    val intent = Intent("isRecording").apply {
                        putExtra("Recording",shouldContinueWriting)
                    }
                    sendBroadcast(intent)

                } else {
                    shouldContinueWriting = true
                    // Notify MainActivity to start recording
                   // val startRecordingIntent = Intent(this@MyForegroundService, MainActivity::class.java)
                    //startRecordingIntent.action = "startRecording"
                    log("Start")
                    val intent = Intent("isRecording").apply {
                        putExtra("Recording",shouldContinueWriting)
                    }
                    sendBroadcast(intent)

                   // writeToFirebaseContinuously()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                log("onCancelled: ${databaseError.message}")
            }
        })
    }

    private fun writeToFirebaseContinuously() {
        coroutineScope.launch {
            while (shouldContinueWriting) {
                myRef.push().setValue(getCurrentDateTimeString())
                delay(1000)
                log(getCurrentDateTimeString())
            }
        }
    }
    fun getCurrentDateTimeString(): String {
        val currentDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return currentDateTime.format(formatter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand")
        // You can start any additional tasks or functionality here if required.
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        log("onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun log(message: String) {
        Log.d("SERVICE_TAG", "MyForegroundService: $message")
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Title")
        .setContentText("Text")
        .setSmallIcon(R.drawable.ic_launcher_background)
        .build()

    companion object {

        private const val CHANNEL_ID = "channel_id"
        private const val CHANNEL_NAME = "channel_name"
        private const val NOTIFICATION_ID = 1

        fun newIntent(context: Context): Intent {
            return Intent(context, MyForegroundService::class.java)
        }
    }
}