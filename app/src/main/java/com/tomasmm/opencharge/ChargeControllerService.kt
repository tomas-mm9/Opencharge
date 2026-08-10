package com.tomasmm.opencharge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class ChargeControllerService : Service() {

    companion object {
        private const val TAG = "OpenCharge"
        private const val CHANNEL_ID = "opencharge_status"
        private const val NOTIF_ID = 1001
        private const val POLL_MS = 15_000L
    }

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            try {
                tickOnce()
            } catch (e: Exception) {
                Log.e(TAG, "Error en ciclo", e)
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServiceUtils.ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        running = true
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
    }

    private fun tickOnce() {
        val bs = BatteryReader.read(this)
        val current = BatteryReader.currentNow(this)
        val manual = prefs.masterEnabled
        val auto = prefs.autoEnable

        // Sesión autoactivada: se arma mientras haya carga inalámbrica
        if (bs.wireless && auto) {
            if (!prefs.autoActive) prefs.autoActive = true
        } else if (!bs.wireless && prefs.autoActive) {
            prefs.autoActive = false
        }

        val effective = manual || (auto && bs.wireless)

        if (!effective) {
            shutdown()
            return
        }

        StateHolder.armed = effective && bs.wireless
        StateHolder.charging = bs.charging
        StateHolder.wireless = bs.wireless
        StateHolder.tempC = bs.tempC
        StateHolder.level = bs.level
        StateHolder.currentMa = if (current == Int.MIN_VALUE) 0 else current
        StateHolder.source = sourceLabel(bs)

        if (bs.wireless) {
            applyControl(bs.tempC)
        } else {
            StateHolder.mode = "Espera (carga por cable)"
            StateHolder.permissionOk = true
            StateHolder.lastNote = "Carga por cable: sin restricciones. Solo se ajusta la inalámbrica."
        }

        updateNotification()
        sendStatusBroadcast()
    }

    private fun applyControl(tempC: Float) {
        val high = prefs.tempHigh
        val low = prefs.tempLow
        val now = System.currentTimeMillis()
        val mode = prefs.speedMode
        val reason = prefs.slowReason
        val gentle = prefs.gentleMode
        val elapsedMin = (now - prefs.lastModeChangeAt) / 60_000.0

        if (mode == "FAST") {
            if (tempC >= high) {
                setMode("SLOW", "HIGH", now)
            } else if (gentle && elapsedMin >= prefs.gentleFastMin) {
                setMode("SLOW", "GENTLE", now)
            }
        } else {
            val allowFast = when (reason) {
                "HIGH" -> tempC <= low && elapsedMin >= prefs.cooldownMin
                "GENTLE" -> tempC < high && elapsedMin >= prefs.gentleSlowMin
                else -> tempC <= low && elapsedMin >= prefs.cooldownMin
            }
            if (allowFast) setMode("FAST", "", now)
        }

        val wantFast = prefs.speedMode == "FAST"
        val res = WirelessChargeControl.writeKey(this, if (wantFast) 1 else 0)
        StateHolder.permissionOk = res == WirelessChargeControl.WriteResult.OK
        StateHolder.mode = if (wantFast) "Rápido (15W)" else "Lento (5W)"

        StateHolder.lastNote = when (res) {
            WirelessChargeControl.WriteResult.OK ->
                if (wantFast) "Velocidad rápida activada (temperatura aceptable)."
                else "Velocidad lenta activada para evitar sobrecalentamiento."
            WirelessChargeControl.WriteResult.NO_PERMISSION ->
                "Falta el permiso adb WRITE_SECURE_SETTINGS (ver Diagnóstico)."
            WirelessChargeControl.WriteResult.MISMATCH ->
                "El ajuste no se aplicó; comprueba Diagnóstico."
            WirelessChargeControl.WriteResult.NOT_FOUND ->
                "Clave wireless_fast_charging no disponible en este One UI."
        }
    }

    private fun setMode(mode: String, reason: String, at: Long) {
        prefs.speedMode = mode
        prefs.slowReason = reason
        prefs.lastModeChangeAt = at
        Log.d(TAG, "Modo → $mode ($reason)")
    }

    private fun sourceLabel(bs: BatteryReader.BatteryState): String = when {
        !bs.charging && bs.plugged == 0 -> "No cargando"
        bs.wireless -> "Inalámbrica"
        (bs.plugged and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> "Cable (USB)"
        else -> "Cable (AC)"
    }

    private fun shutdown() {
        running = false
        handler.removeCallbacks(tick)
        prefs.autoActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendStatusBroadcast() {
        val i = Intent(ServiceUtils.ACTION_STATUS)
        i.putExtra("temp", StateHolder.tempC)
        i.putExtra("level", StateHolder.level)
        i.putExtra("source", StateHolder.source)
        i.putExtra("mode", StateHolder.mode)
        i.putExtra("current", StateHolder.currentMa)
        i.putExtra("armed", StateHolder.armed)
        i.putExtra("permissionOk", StateHolder.permissionOk)
        i.putExtra("note", StateHolder.lastNote)
        sendBroadcast(i)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Estado de carga inalámbrica",
                NotificationManager.IMPORTANCE_LOW
            )
            ch.description = "Notificación persistente con el estado de la carga optimizada."
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, ChargeControllerService::class.java).setAction(ServiceUtils.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Batería ${StateHolder.level}% · ${StateHolder.tempDisplay()} · ${StateHolder.source} · ${StateHolder.currentMa}mA"
        val big = "$text\n${StateHolder.lastNote}"

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_opencharge)
            .setContentTitle("OpenCharge — ${StateHolder.mode}")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(big))
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_stop),
                    "Detener",
                    stopPi
                ).build()
            )
        }
        return builder.build()
    }
}
