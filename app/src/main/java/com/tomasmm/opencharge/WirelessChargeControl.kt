package com.tomasmm.opencharge

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log

object WirelessChargeControl {

    private const val TAG = "OpenCharge"
    const val KEY = "wireless_fast_charging"

    /** Claves candidatas para apagar/enciender la carga inalámbrica completa (toggle maestro). */
    val MASTER_KEY_CANDIDATES = listOf(
        "wireless_charging",
        "wireless_charge",
        "wireless_charging_enabled"
    )

    enum class WriteResult { OK, NO_PERMISSION, MISMATCH, NOT_FOUND }

    private val tables = listOf("system" to 0, "global" to 1, "secure" to 2)
    private const val MISSING = -999

    private fun getInt(cr: ContentResolver, table: Int, key: String, def: Int): Int = when (table) {
        1 -> Settings.Global.getInt(cr, key, def)
        2 -> Settings.Secure.getInt(cr, key, def)
        else -> Settings.System.getInt(cr, key, def)
    }

    private fun putInt(cr: ContentResolver, table: Int, key: String, value: Int): Boolean = when (table) {
        1 -> Settings.Global.putInt(cr, key, value)
        2 -> Settings.Secure.putInt(cr, key, value)
        else -> Settings.System.putInt(cr, key, value)
    }

    /** Devuelve (valor, tabla) del primer sitio donde exista la clave, o (def, null). */
    fun readAnyTable(context: Context, key: String, def: Int): Pair<Int, String?> {
        for ((name, table) in tables) {
            val v = getInt(context.contentResolver, table, key, MISSING)
            if (v != MISSING) return v to name
        }
        return def to null
    }

    /** Intenta escribir la clave en todas las tablas y verifica releendo. */
    fun writeKey(context: Context, key: String, value: Int): WriteResult {
        val cr = context.contentResolver
        for ((name, table) in tables) {
            try {
                val ok = putInt(cr, table, key, value)
                if (!ok) continue
                val back = getInt(cr, table, key, MISSING)
                if (back == value) {
                    Log.d(TAG, "Escrito $key=$value en tabla '$name'")
                    return WriteResult.OK
                }
            } catch (e: SecurityException) {
                Log.d(TAG, "Sin permiso para escribir $key en '$name'", e)
            } catch (e: Exception) {
                Log.d(TAG, "Error escribiendo $key en '$name'", e)
            }
        }
        val hasAny = readAnyTable(context, key, MISSING).second != null
        return if (hasAny) WriteResult.NO_PERMISSION else WriteResult.NOT_FOUND
    }

    /** Detecta si hay una clave maestra (toggle de carga inalámbrica completa) en el dispositivo. */
    fun findMasterKey(context: Context): String? {
        for (k in MASTER_KEY_CANDIDATES) {
            val (v, table) = readAnyTable(context, k, MISSING)
            if (table != null && (v == 0 || v == 1)) return k
        }
        return null
    }

    fun isSamsung(context: Context): Boolean {
        val m = android.os.Build.MANUFACTURER?.lowercase() ?: ""
        return m.contains("samsung")
    }
}
