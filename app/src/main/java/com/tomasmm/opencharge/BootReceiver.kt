package com.tomasmm.opencharge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val prefs = Prefs(context)
        if (prefs.masterEnabled) {
            ServiceUtils.startService(context)
            return
        }
        if (prefs.autoEnable) {
            val bs = BatteryReader.read(context)
            if (bs.wireless) {
                prefs.autoActive = true
                ServiceUtils.startService(context)
            }
        }
    }
}
