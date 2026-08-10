package com.tomasmm.opencharge

import kotlin.math.roundToInt

object StateHolder {
    @Volatile var tempC: Float = 0f
    @Volatile var level: Int = -1
    @Volatile var source: String = "—"
    @Volatile var mode: String = "—"
    @Volatile var currentMa: Int = 0
    @Volatile var armed: Boolean = false
    @Volatile var permissionOk: Boolean = false
    @Volatile var charging: Boolean = false
    @Volatile var wireless: Boolean = false
    @Volatile var lastNote: String = ""

    fun tempDisplay(): String = if (tempC > 0) "${tempC.roundToInt()}°C" else "—"
}
