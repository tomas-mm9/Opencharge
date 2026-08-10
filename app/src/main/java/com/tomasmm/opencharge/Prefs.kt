package com.tomasmm.opencharge

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("opencharge", Context.MODE_PRIVATE)

    // ---- toggles ----
    var masterEnabled: Boolean
        get() = sp.getBoolean("master_enabled", false)
        set(value) = sp.edit().putBoolean("master_enabled", value).apply()

    var autoEnable: Boolean
        get() = sp.getBoolean("auto_enable", false)
        set(value) = sp.edit().putBoolean("auto_enable", value).apply()

    var autoActive: Boolean
        get() = sp.getBoolean("auto_active", false)
        set(value) = sp.edit().putBoolean("auto_active", value).apply()

    // ---- modo del controlador ----
    /** "SIMPLE" (histéresis) o "DYNAMIC" (PWM/PI con potencia media). */
    var controllerMode: String
        get() = sp.getString("controller_mode", "DYNAMIC") ?: "DYNAMIC"
        set(value) = sp.edit().putString("controller_mode", value).apply()

    // ---- parámetros modo SIMPLE ----
    var tempHigh: Int
        get() = sp.getInt("temp_high", 40)
        set(value) = sp.edit().putInt("temp_high", value).apply()

    var tempLow: Int
        get() = sp.getInt("temp_low", 36)
        set(value) = sp.edit().putInt("temp_low", value).apply()

    var cooldownMin: Int
        get() = sp.getInt("cooldown_min", 5)
        set(value) = sp.edit().putInt("cooldown_min", value).apply()

    // ---- parámetros modo DYNAMIC ----
    /** Temperatura objetivo de la batería en modo dinámico (°C). */
    var setpoint: Int
        get() = sp.getInt("setpoint", 37)
        set(value) = sp.edit().putInt("setpoint", value).apply()

    /** Por encima de esta temperatura se fuerza carga lenta (o apagado si es posible). */
    var safetyHigh: Int
        get() = sp.getInt("safety_high", 42)
        set(value) = sp.edit().putInt("safety_high", value).apply()

    /** Período del ciclo PWM en minutos. */
    var periodMin: Int
        get() = sp.getInt("period_min", 3)
        set(value) = sp.edit().putInt("period_min", value).apply()

    // ---- temperatura ambiente (para umbrales adaptativos) ----
    var idleTempTenths: Int
        get() = sp.getInt("idle_temp_tenths", 0)
        set(value) = sp.edit().putInt("idle_temp_tenths", value).apply()

    var idleAt: Long
        get() = sp.getLong("idle_at", 0)
        set(value) = sp.edit().putLong("idle_at", value).apply()

    /** 0 = automático; >0 = temperatura ambiente fijada manualmente (°C). */
    var ambientOverride: Int
        get() = sp.getInt("ambient_override", 0)
        set(value) = sp.edit().putInt("ambient_override", value).apply()

    /** Si en tu One UI el cambio de velocidad no aplica en caliente, actívalo para
     *  reiniciar la sesión de carga (apagar/enciender la carga) al cambiar de modo. */
    var applyNeedsRestart: Boolean
        get() = sp.getBoolean("apply_needs_restart", true)
        set(value) = sp.edit().putBoolean("apply_needs_restart", value).apply()

    /** Clave maestra detectada para poder apagar la carga por completo ("" si no hay). */
    var masterKey: String
        get() = sp.getString("master_key", "") ?: ""
        set(value) = sp.edit().putString("master_key", value).apply()

    // ---- estado interno del controlador ----
    var speedMode: String
        get() = sp.getString("speed_mode", "FAST") ?: "FAST"
        set(value) = sp.edit().putString("speed_mode", value).apply()

    var slowReason: String
        get() = sp.getString("slow_reason", "") ?: ""
        set(value) = sp.edit().putString("slow_reason", value).apply()

    var lastModeChangeAt: Long
        get() = sp.getLong("mode_change_at", System.currentTimeMillis())
        set(value) = sp.edit().putLong("mode_change_at", value).apply()

    /** Potencia media objetivo en vatios (décimas) para el modo dinámico. */
    var basePowerTenths: Int
        get() = sp.getInt("base_power_tenths", 100) // 10.0 W
        set(value) = sp.edit().putInt("base_power_tenths", value).apply()

    var lastControlAt: Long
        get() = sp.getLong("last_control_at", System.currentTimeMillis())
        set(value) = sp.edit().putLong("last_control_at", value).apply()

    var periodStartAt: Long
        get() = sp.getLong("period_start_at", System.currentTimeMillis())
        set(value) = sp.edit().putLong("period_start_at", value).apply()

    var restartInProgress: Boolean
        get() = sp.getBoolean("restart_in_progress", false)
        set(value) = sp.edit().putBoolean("restart_in_progress", value).apply()

    var lastRestartAt: Long
        get() = sp.getLong("last_restart_at", 0)
        set(value) = sp.edit().putLong("last_restart_at", value).apply()
}
