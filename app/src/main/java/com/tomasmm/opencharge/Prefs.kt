package com.tomasmm.opencharge

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("opencharge", Context.MODE_PRIVATE)

    var masterEnabled: Boolean
        get() = sp.getBoolean("master_enabled", false)
        set(value) = sp.edit().putBoolean("master_enabled", value).apply()

    var autoEnable: Boolean
        get() = sp.getBoolean("auto_enable", false)
        set(value) = sp.edit().putBoolean("auto_enable", value).apply()

    var autoActive: Boolean
        get() = sp.getBoolean("auto_active", false)
        set(value) = sp.edit().putBoolean("auto_active", value).apply()

    var tempHigh: Int
        get() = sp.getInt("temp_high", 40)
        set(value) = sp.edit().putInt("temp_high", value).apply()

    var tempLow: Int
        get() = sp.getInt("temp_low", 36)
        set(value) = sp.edit().putInt("temp_low", value).apply()

    var cooldownMin: Int
        get() = sp.getInt("cooldown_min", 5)
        set(value) = sp.edit().putInt("cooldown_min", value).apply()

    var gentleMode: Boolean
        get() = sp.getBoolean("gentle_mode", false)
        set(value) = sp.edit().putBoolean("gentle_mode", value).apply()

    var gentleFastMin: Int
        get() = sp.getInt("gentle_fast_min", 20)
        set(value) = sp.edit().putInt("gentle_fast_min", value).apply()

    var gentleSlowMin: Int
        get() = sp.getInt("gentle_slow_min", 10)
        set(value) = sp.edit().putInt("gentle_slow_min", value).apply()

    // estado del controlador
    var speedMode: String
        get() = sp.getString("speed_mode", "FAST") ?: "FAST"
        set(value) = sp.edit().putString("speed_mode", value).apply()

    var slowReason: String
        get() = sp.getString("slow_reason", "") ?: ""
        set(value) = sp.edit().putString("slow_reason", value).apply()

    var lastModeChangeAt: Long
        get() = sp.getLong("mode_change_at", System.currentTimeMillis())
        set(value) = sp.edit().putLong("mode_change_at", value).apply()
}
