package com.example.sensortestapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.sensortestapplication.ActivityTransitionReceiver.Companion.TRANSITIONS_RECEIVER_ACTION
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity


class ForegroundService : Service() {

    private lateinit var activityTransitionReceiver: ActivityTransitionReceiver

    private val TAG: String? = "ForegroundService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ForegroundService onCreate: ")
        runAsForeground()
        activityTransitionReceiver = ActivityTransitionReceiver()
        val intentFilter = IntentFilter(TRANSITIONS_RECEIVER_ACTION)
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        registerReceiver(activityTransitionReceiver, intentFilter, RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ForegroundService onStartCommand: ")
        if(intent != null) {
            enableActivityTransitions()
        }
        return START_STICKY

    }

    private fun runAsForeground() {
        val notificationChannel = NotificationChannel(
            "TestChannelId", "NOTIFICATION_CHANNEL_NAME", NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)


        val notification = NotificationCompat.Builder(this, "TestChannelId")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentText("Service running")
            .setContentTitle("Test Application")
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFAULT).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1234, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1234, notification)
        }
    }

    private fun getActivityTransitionList(): List<ActivityTransition>{
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
            7658,
            Intent(TRANSITIONS_RECEIVER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        val request = ActivityTransitionRequest(getActivityTransitionList())

        Handler(Looper.getMainLooper()).postDelayed({
            ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, activityTransitionsPendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "enableActivityTransitions success")
                }
                .addOnFailureListener {
                    Log.d(TAG, "enableActivityTransitions failure")
                }
            // ...
        }, 1000)
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
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onDestroy() {
        Log.d(TAG, "ForegroundService onDestroy")
        disableActivityTransitions()
        unregisterReceiver(activityTransitionReceiver)
        super.onDestroy()
    }
    
}