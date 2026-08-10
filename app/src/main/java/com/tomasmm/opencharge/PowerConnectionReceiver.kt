package com.tomasmm.opencharge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                // No exigimos bs.wireless aquí: en Samsung el battery sticky puede tardar
                // unos segundos en reportar wireless. El servicio se queda en espera
                // hasta detectar la carga inalámbrica.
                if (prefs.autoEnable || prefs.masterEnabled) {
                    ServiceUtils.startService(context)
                }
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                if (prefs.autoActive) prefs.autoActive = false
                if (!prefs.autoEnable && !prefs.masterEnabled) {
                    ServiceUtils.stopService(context)
                }
            }
        }
    }
}
