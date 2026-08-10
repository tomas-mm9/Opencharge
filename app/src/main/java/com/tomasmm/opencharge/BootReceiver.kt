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
        if (prefs.masterEnabled || prefs.autoEnable) {
            // El servicio se queda en espera hasta detectar la carga inalámbrica,
            // así no dependemos del sticky de batería justo tras el boot.
            ServiceUtils.startService(context)
        }
    }
}
