package com.example

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Clean launcher activity that clears any dangling tasks or stuck system screens
 * (like Quick Share / chooser) and brings the Door Billing app immediately to the front.
 */
class AppLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
        } catch (_: Exception) {}

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}
