package com.example.sensortestapplication

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sensortestapplication.databinding.ActivityMainBinding
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class MainActivity : AppCompatActivity() {
    private val TAG: String? = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )

        checkBatteryDataUsageRestrictions()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(
                    Manifest.permission.FOREGROUND_SERVICE_HEALTH,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ), 101
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ), 101
            )
        }


        binding.startService.setOnClickListener {
            Log.d(TAG, "onCreate: startService.setOnClickListener")
            val intent = Intent(this, ForegroundService::class.java)
            startService(intent)
        }

        binding.stopService.setOnClickListener {
            Log.d(TAG, "onCreate: stopService.setOnClickListener")
            val intent = Intent(this, ForegroundService::class.java)
            stopService(intent)
        }
//        registerActivityTransitions()
    }

    override fun onStart() {
        super.onStart()
       // enableActivityTransitions()
    }

    override fun onDestroy() {
        super.onDestroy()
       // unRegisterActivityTransitions()
    }

    private fun checkBatteryDataUsageRestrictions() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBatteryOptimizations =
            powerManager.isIgnoringBatteryOptimizations(packageName)
        Log.d(
            TAG,
            "checkBatteryDataUsageRestrictions: isIgnoringBatteryOptimizations : $isIgnoringBatteryOptimizations"
        )

        if (isIgnoringBatteryOptimizations.not()) {
            // Prompt the user to disable battery optimization for your app
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isEnableBackgroundDataNeeded =
            connectivityManager.isActiveNetworkMetered.not() || connectivityManager.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        Log.d(
            TAG,
            "checkBatteryDataUsageRestrictions: isEnableBackgroundDataNeeded : $isEnableBackgroundDataNeeded"
        )

        if (isEnableBackgroundDataNeeded.not()) {
            // Prompt the user to enable background data for your app
            val intent = Intent(
                Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                // Request notification permission
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            }
        }
    }

    /*private fun getActivityTransitionList(): List<ActivityTransition>{
        val activityTransitionList = mutableListOf<ActivityTransition>()
        activityTransitionList.apply {
            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_FOOT)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_FOOT)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }
        return activityTransitionList
    }

    private val activityTransitionsPendingIntent by lazy {
        PendingIntent.getBroadcast(
            this,
            ForegroundService.CUSTOM_REQUEST_CODE_USER_ACTION,
            Intent(ActivityTransitionReceiver.TRANSITIONS_RECEIVER_ACTION),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun registerActivityTransitions(){
        registerReceiver(ActivityTransitionReceiver(),
            IntentFilter(ActivityTransitionReceiver.TRANSITIONS_RECEIVER_ACTION),
            RECEIVER_EXPORTED
        )
    }

    private fun unRegisterActivityTransitions(){
        unregisterReceiver(receiver)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "receiver onReceive action: ${intent?.action} extras: ${intent?.extras}")
            if(intent?.action?.equals(ActivityTransitionReceiver.TRANSITIONS_RECEIVER_ACTION) == false) return

            val result = intent?.let { ActivityTransitionResult.extractResult(it) } ?: return
            Log.d(TAG, "receiver result: ${result.transitionEvents}")
        }
    }

    private fun enableActivityTransitions() {
        Log.d(TAG, "enableActivityTransitions")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ActivityRecognition.getClient(this)
            .requestActivityUpdates(2000, activityTransitionsPendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "enableActivityTransitions success")
            }
            .addOnFailureListener {
                Log.d(TAG, "enableActivityTransitions failure")
            }
    }

    private fun disableActivityTransitions() {
        Log.d(TAG, "disableActivityTransitions")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityRecognition.getClient(this)
            .removeActivityTransitionUpdates(activityTransitionsPendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "disableActivityTransitions success")
            }.addOnFailureListener {
                Log.d(TAG, "disableActivityTransitions failure")
            }

     */
}



