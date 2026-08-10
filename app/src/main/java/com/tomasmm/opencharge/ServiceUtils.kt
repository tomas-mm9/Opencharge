package com.tomasmm.opencharge

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

    /** Añade un margen superior equivalente a la barra de estado (más un poco extra)
     *  para que los títulos no queden pegados a los iconos de notificaciones. */
    fun applyStatusBarInsetPadding(container: View) {
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraDp = 10
            val extraPx = (extraDp * v.resources.displayMetrics.density).toInt()
            v.setPadding(v.paddingLeft, bars.top + extraPx, v.paddingRight, v.paddingBottom)
            insets
        }
    }
}
