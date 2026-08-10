package com.tomasmm.opencharge

import android.content.Context
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Controlador de carga inalámbrica.
 *
 * Dos modos:
 *  - SIMPLE: histéresis por temperatura (rápido/lento).
 *  - DYNAMIC: controlador PI + PWM de 3 estados (rápido 15 W / lento 5 W / apagado 0 W),
 *             produce una potencia media continua entre 0 y 15 W. El apagado solo está
 *             disponible si se detecta una clave maestra (toggle completo de carga inalámbrica).
 *
 * Si `applyNeedsRestart` está activado y se detecta una clave maestra, al cambiar de estado
 * se interrumpe la sesión de carga ~1,5 s (apagado → encendido) para que el nuevo ajuste
 * se re-negocie en caliente.
 */
class ChargeController(private val ctx: Context, private val prefs: Prefs) {

    companion object {
        private const val TAG = "OpenCharge"
        const val FAST_W = 15.0
        const val SLOW_W = 5.0
        private const val KP = 1.5          // W por °C de error
        private const val RAMP_UP_PER_MIN = 0.4   // W/min (sube con suavidad)
        private const val RAMP_DOWN_PER_MIN = 1.5 // W/min (baja rápido)
        private const val RESTART_MS = 1500L
        private const val MIN_BETWEEN_RESTARTS_MS = 60_000L
    }

    data class Result(
        val mode: String,
        val note: String,
        val avgWatts: Float,
        val permissionOk: Boolean
    )

    private val mainKey: String get() = WirelessChargeControl.KEY

    fun tick(tempC: Float, wireless: Boolean): Result {
        if (!wireless) {
            prefs.restartInProgress = false
            return Result(
                mode = "Espera (carga por cable o sin carga)",
                note = "Carga por cable: sin restricciones. Solo se ajusta la inalámbrica.",
                avgWatts = 0f,
                permissionOk = true
            )
        }

        if (prefs.masterKey.isEmpty()) {
            WirelessChargeControl.findMasterKey(ctx)?.let { prefs.masterKey = it }
        }
        val offAvailable = prefs.masterKey.isNotEmpty()

        val setpointEff = AmbientEstimator.setpointFor(prefs)
        val safetyEff = AmbientEstimator.safetyFor(prefs)

        return if (prefs.controllerMode == "SIMPLE") simpleTick(tempC, offAvailable, setpointEff, safetyEff)
        else dynamicTick(tempC, offAvailable, setpointEff, safetyEff)
    }

    // ------------------------------------------------------------------ SIMPLE
    private fun simpleTick(
        tempC: Float,
        offAvailable: Boolean,
        setpointEff: Int,
        safetyEff: Int
    ): Result {
        val now = System.currentTimeMillis()
        val mode = prefs.speedMode
        val elapsedMin = (now - prefs.lastModeChangeAt) / 60_000.0

        if (mode == "FAST") {
            if (tempC >= safetyEff) setMode("SLOW", "HIGH", now)
        } else {
            if (tempC <= setpointEff && elapsedMin >= prefs.cooldownMin) setMode("FAST", "", now)
        }

        val fast = prefs.speedMode == "FAST"
        val res = WirelessChargeControl.writeKey(ctx, mainKey, if (fast) 1 else 0)
        maybeRestart(if (fast) "FAST" else "SLOW")

        val modeLabel = if (fast) "Rápido (15W)" else "Lento (5W)"
        val note = when (res) {
            WirelessChargeControl.WriteResult.OK ->
                if (fast) "Rápido: ${tempC.round()}°C aceptable (objetivo $setpointEff)."
                else "Lento: ${tempC.round()}°C ≥ ${safetyEff}°C, evitando sobrecalentamiento."
            WirelessChargeControl.WriteResult.NO_PERMISSION ->
                "Falta el permiso adb WRITE_SECURE_SETTINGS (ver Diagnóstico)."
            WirelessChargeControl.WriteResult.MISMATCH ->
                "El ajuste no se aplicó; comprueba Diagnóstico."
            WirelessChargeControl.WriteResult.NOT_FOUND ->
                "Clave $mainKey no disponible en este One UI."
        }
        return Result(modeLabel, note, if (fast) FAST_W.toFloat() else SLOW_W.toFloat(), res == WirelessChargeControl.WriteResult.OK)
    }

    // ----------------------------------------------------------------- DYNAMIC
    private fun dynamicTick(
        tempC: Float,
        offAvailable: Boolean,
        setpointEff: Int,
        safetyEff: Int
    ): Result {
        val now = System.currentTimeMillis()
        val periodMs = (prefs.periodMin * 60_000L).coerceAtLeast(60_000L)
        val minW = if (offAvailable) 0.0 else SLOW_W

        // ---- PI sobre temperatura con slew limit ----
        var base = prefs.basePowerTenths / 10.0
        val error = setpointEff - tempC // >0 => frío => subir potencia
        var target = (FAST_W + KP * error).coerceIn(minW, FAST_W)

        val dtMin = max((now - prefs.lastControlAt) / 60_000.0, 0.0)
        val delta = target - base
        base = if (delta >= 0) {
            base + min(delta, RAMP_UP_PER_MIN * dtMin)
        } else {
            base - min(-delta, RAMP_DOWN_PER_MIN * dtMin)
        }
        base = base.coerceIn(minW, FAST_W)
        prefs.basePowerTenths = (base * 10).toInt()
        prefs.lastControlAt = now

        // ---- PWM: reparte la potencia media en el período ----
        val phase = (now - prefs.periodStartAt).mod(periodMs)
        val p = base
        val fastTime: Long
        val slowTime: Long
        val offTime: Long
        if (p >= SLOW_W) {
            fastTime = (((p - SLOW_W) / (FAST_W - SLOW_W)) * periodMs).toLong()
            slowTime = periodMs - fastTime
            offTime = 0
        } else {
            fastTime = 0
            slowTime = ((p / SLOW_W) * periodMs).toLong()
            offTime = periodMs - slowTime
        }

        var state = when {
            phase < fastTime -> "FAST"
            phase < fastTime + slowTime -> "SLOW"
            else -> "OFF"
        }

        // ---- sobreeseguridad ----
        if (tempC >= safetyEff + 3 && offAvailable) state = "OFF"
        else if (tempC >= safetyEff) state = "SLOW"

        val res = applyDynamicState(state, offAvailable)

        val modeLabel = when (state) {
            "FAST" -> "Rápido (15W)"
            "SLOW" -> "Lento (5W)"
            else -> "Apagado (pausa)"
        }
        val note = if (offAvailable) {
            "Dinámico · media ${"%.1f".format(p)} W · objetivo $setpointEff°C / seg $safetyEff°C ($modeLabel)"
        } else {
            "Dinámico · media ${"%.1f".format(max(p, SLOW_W))} W · objetivo $setpointEff°C / seg $safetyEff°C ($modeLabel) · sin apagado"
        } + permissionSuffix(res)
        return Result(modeLabel, note, if (offAvailable) p.toFloat() else max(p, SLOW_W).toFloat(), res == WirelessChargeControl.WriteResult.OK)
    }

    private fun applyDynamicState(state: String, offAvailable: Boolean): WirelessChargeControl.WriteResult {
        val now = System.currentTimeMillis()
        val current = prefs.speedMode
        val res = when (state) {
            "FAST" -> {
                setMode("FAST", "PWM", now)
                WirelessChargeControl.writeKey(ctx, mainKey, 1)
            }
            "SLOW" -> {
                setMode("SLOW", "PWM", now)
                WirelessChargeControl.writeKey(ctx, mainKey, 0)
            }
            else -> { // OFF
                setMode("OFF", "PWM", now)
                if (offAvailable) WirelessChargeControl.writeKey(ctx, prefs.masterKey, 0)
                else WirelessChargeControl.writeKey(ctx, mainKey, 0)
            }
        }

        // Reinicio de sesión solo en transiciones a FAST/SLOW (apaga/enciende para re-negociar)
        if ((state == "FAST" || state == "SLOW") && current != state) maybeRestart(state)
        return res
    }

    private fun permissionSuffix(res: WirelessChargeControl.WriteResult): String = when (res) {
        WirelessChargeControl.WriteResult.OK -> ""
        WirelessChargeControl.WriteResult.NO_PERMISSION -> " · ¡Falta permiso adb!"
        WirelessChargeControl.WriteResult.MISMATCH -> " · ajuste no aplicado"
        WirelessChargeControl.WriteResult.NOT_FOUND -> " · clave no disponible"
    }

    private fun maybeRestart(newState: String) {
        val mk = prefs.masterKey
        if (mk.isEmpty() || !prefs.applyNeedsRestart || prefs.restartInProgress) return
        val now = System.currentTimeMillis()
        if (now - prefs.lastRestartAt < MIN_BETWEEN_RESTARTS_MS) return

        prefs.restartInProgress = true
        prefs.lastRestartAt = now
        Log.d(TAG, "Reiniciando sesión de carga ($newState) usando clave '$mk'")
        Thread {
            WirelessChargeControl.writeKey(ctx, mk, 0)
            Thread.sleep(RESTART_MS)
            WirelessChargeControl.writeKey(ctx, mk, 1)
            prefs.restartInProgress = false
        }.start()
    }

    private fun setMode(mode: String, reason: String, at: Long) {
        prefs.speedMode = mode
        prefs.slowReason = reason
        prefs.lastModeChangeAt = at
        Log.d(TAG, "Modo → $mode ($reason)")
    }

    private fun Float.round(): Int = roundToInt()
}
