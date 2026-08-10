package com.tomasmm.opencharge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val bs = BatteryReader.read(context)
                if (bs.wireless && prefs.autoEnable) {
                    prefs.autoActive = true
                    ServiceUtils.startService(context)
                } else if (prefs.masterEnabled) {
                    ServiceUtils.startService(context)
                }
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                if (prefs.autoActive) prefs.autoActive = false
                if (!prefs.masterEnabled) {
                    ServiceUtils.stopService(context)
                }
            }
        }
    }
}
