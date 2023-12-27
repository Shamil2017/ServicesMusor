package com.example.servicesmusor

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.servicesmusor.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var id = 0
    private val db: FirebaseFirestore? = null
    private val RECORD_AUDIO_PERMISSION_CODE = 101 // Permission request code

    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private lateinit var outputFile: File // Declare outputFile as a File


    private var receiver = object: BroadcastReceiver()
    {
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action)
            {
                "isRecording" -> {
                    isRecording = intent.getBooleanExtra("Recording", false)
                    log("$isRecording")


                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val recordButton: Button = findViewById(R.id.intent_service)
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                recordButton.text = "Start Recording"
            } else {
                if (checkPermissions()) {
                    startRecording()
                    recordButton.text = "Stop Recording"
                } else {
                    requestPermissions()
                }
            }
        }


        val intentFilter = IntentFilter().apply {
            addAction("isRecording")
        }
        registerReceiver(receiver, intentFilter)


        binding.simpleService.setOnClickListener {
            startService(MusorService.newIntent(this))
        }
        binding.foregroundService.setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                MyForegroundService.newIntent(this)
            )
        }


    }

    private fun checkPermissions(): Boolean {
        val permissionRecordAudio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )
        val permissionWriteStorage = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return permissionRecordAudio == PackageManager.PERMISSION_GRANTED &&
                permissionWriteStorage == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PERMISSIONS_REQUEST_CODE
        )
    }

    private fun startRecording() {
        // Inside startRecording():
        outputFile = File(externalCacheDir, "recording.3gp") // Assign File object using externalCacheDir


        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Change setOutputFile to take absolute path
                setOutputFile(outputFile?.absolutePath)
            }

            try {
                prepare()
                start()
                isRecording = true
                Toast.makeText(this@MainActivity, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to start recording: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }






    private fun log(message: String) {
        Log.d("SERVICE_TAG", "MainActivity: $message")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun stopRecording() {
        try {
            mediaRecorder.stop()
            mediaRecorder.release()
            isRecording = false
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
        } catch (e: RuntimeException) {
            Toast.makeText(
                this@MainActivity,
                "Failed to stop recording: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
    }


    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private const val SAMPLE_RATE = 44100
        private const val MIN_BUFFER_SIZE = 2048
    }
}