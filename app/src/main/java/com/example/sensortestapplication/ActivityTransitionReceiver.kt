package com.example.sensortestapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionReceiver: BroadcastReceiver() {
    
    private val TAG = "ActivityTransitionReceiver"

    companion object {
        const val TRANSITIONS_RECEIVER_ACTION = "com.example.demoocrapp.TRANSITIONS_RECEIVER_ACTION"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "ActivityTransitionReceiver onReceive called ${intent?.action} ${intent?.action == TRANSITIONS_RECEIVER_ACTION}")
        if (intent?.action == TRANSITIONS_RECEIVER_ACTION) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return
            Log.d(TAG, "ActivityTransitionResult: $result")
            var resultStr = ""
            for (event in result.transitionEvents) {
                DetectedActivity.STILL
                resultStr += "${getActivityType(event.activityType)} " +
                        "- ${getTransitionType(event.transitionType)}"
            }
            Toast.makeText(context, "Activity Transition Detected $resultStr", Toast.LENGTH_LONG).show()
        }
    }

    private fun getActivityType(int: Int): String {
        return when (int) {
            0 -> "IN_VEHICLE"
            1 -> "ON_BICYCLE"
            2 -> "ON_FOOT"
            3 -> "STILL"
            4 -> "UNKNOWN"
            5 -> "TILTING"
            7 -> "WALKING"
            8 -> "RUNNING"
            else -> "UNKNOWN"
        }
    }

    private fun getTransitionType(int: Int): String {
        return when (int) {
            0 -> "STARTED"
            1 -> "STOPPED"
            else -> ""
        }
    }
}