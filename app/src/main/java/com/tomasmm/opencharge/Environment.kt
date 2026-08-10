package com.tomasmm.opencharge

import kotlin.math.roundToInt

/**
 * Estimador de temperatura ambiente y perfil térmico adaptativo para el Galaxy S25.
 *
 * La temperatura ambiente se estima cuando el móvil está en reposo (sin cargar y sin
 * consumo): la batería suele estar ~2 °C por encima del ambiente. Solo se usa si la
 * muestra es reciente (< 30 min).
 *
 * Referencias (verano en España, ambiente típico 30–33 °C):
 *  - Samsung corta/restringe la carga inalámbrica alrededor de 40–42 °C de batería
 *    (por eso el móvil se detuvo al 80 %: protección térmica). Por encima de 45 °C
 *    la carga entra en modo agresivo / se detiene.
 *  - La batería de Li-ion se degrada ~2× más rápido a 40 °C que a 25 °C; lo sano es
 *    mantener <40 °C y nunca rozar el corte de Samsung para que no nos detenga la carga.
 *
 * Valores elegidos (menos restrictivos que la versión anterior pero seguros):
 *  - Objetivo (setpoint): ambiente + 4 °C, cap 37–40 °C.
 *  - Seguridad (forzar lento): ambiente + 6 °C, cap 42–45 °C.
 *  - Apagado total (si hay clave maestra): seguridad + 3 °C.
 *
 * Sin muestra de ambiente (primer arranque o muestra vieja) se usa un ambiente de
 * verano de 30 °C: objetivo 38 °C, seguridad 42 °C.
 */
object AmbientEstimator {

    private const val FRESH_MS = 30 * 60_000L
    private const val DEFAULT_AMBIENT = 30f

    fun onIdleSample(prefs: Prefs, tempC: Float) {
        prefs.idleTempTenths = (tempC * 10).toInt()
        prefs.idleAt = System.currentTimeMillis()
    }

    fun hasFreshSample(prefs: Prefs): Boolean {
        val now = System.currentTimeMillis()
        return prefs.idleAt > 0 && now - prefs.idleAt < FRESH_MS
    }

    fun ambientTemp(prefs: Prefs): Float {
        if (prefs.ambientOverride > 0) return prefs.ambientOverride.toFloat()
        if (hasFreshSample(prefs)) {
            val t = prefs.idleTempTenths / 10f - 2f
            if (t in 10f..40f) return t
        }
        return DEFAULT_AMBIENT
    }

    /** Objetivo de temperatura efecto: ambiente + 4 °C, con cap 37–40 °C. */
    fun setpointFor(prefs: Prefs): Int =
        (ambientTemp(prefs) + 4f).roundToInt().coerceIn(37, 40)

    /** Seguridad (forzar lento): ambiente + 6 °C, con cap 42–45 °C. */
    fun safetyFor(prefs: Prefs): Int =
        (ambientTemp(prefs) + 6f).roundToInt().coerceIn(42, 45)
}
