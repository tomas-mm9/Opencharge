package com.tomasmm.opencharge

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object ServiceUtils {

    const val ACTION_STOP = "com.tomasmm.opencharge.action.STOP"
    const val ACTION_START = "com.tomasmm.opencharge.action.START"
    const val ACTION_STATUS = "com.tomasmm.opencharge.action.STATUS"

    fun startService(context: Context) {
        val i = Intent(context, ChargeControllerService::class.java).setAction(ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, i)
        } else {
            context.startService(i)
        }
    }

    fun stopService(context: Context) {
        val i = Intent(context, ChargeControllerService::class.java).setAction(ACTION_STOP)
        context.startService(i)
    }
}
