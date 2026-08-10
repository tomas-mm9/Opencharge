package com.tomasmm.opencharge

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log

object WirelessChargeControl {

    private const val TAG = "OpenCharge"
    const val KEY = "wireless_fast_charging"

    enum class WriteResult { OK, NO_PERMISSION, MISMATCH, NOT_FOUND }

    private data class TableRef(val name: String, val table: Int)

    private val tables = listOf(
        TableRef("system", 0),
        TableRef("global", 1),
        TableRef("secure", 2)
    )

    private fun getInt(cr: ContentResolver, table: Int, def: Int): Int = when (table) {
        1 -> Settings.Global.getInt(cr, KEY, def)
        2 -> Settings.Secure.getInt(cr, KEY, def)
        else -> Settings.System.getInt(cr, KEY, def)
    }

    private fun putInt(cr: ContentResolver, table: Int, value: Int): Boolean = when (table) {
        1 -> Settings.Global.putInt(cr, KEY, value)
        2 -> Settings.Secure.putInt(cr, KEY, value)
        else -> Settings.System.putInt(cr, KEY, value)
    }

    fun readAnyTable(context: Context, def: Int): Pair<Int, String?> {
        for (t in tables) {
            val v = getInt(context.contentResolver, t.table, -2)
            if (v != -2) return v to t.name
        }
        return def to null
    }

    fun writeKey(context: Context, value: Int): WriteResult {
        val cr = context.contentResolver
        for (t in tables) {
            try {
                val ok = putInt(cr, t.table, value)
                if (!ok) continue
                val back = getInt(cr, t.table, -2)
                if (back == value) {
                    Log.d(TAG, "Escrito ${KEY}=$value en tabla '${t.name}'")
                    return WriteResult.OK
                }
            } catch (e: SecurityException) {
                Log.d(TAG, "Sin permiso para escribir ${KEY} en '${t.name}'", e)
            } catch (e: Exception) {
                Log.d(TAG, "Error escribiendo ${KEY} en '${t.name}'", e)
            }
        }
        val hasAny = readAnyTable(context, -2).second != null
        return if (hasAny) WriteResult.NO_PERMISSION else WriteResult.NOT_FOUND
    }

    fun isSamsung(context: Context): Boolean {
        val m = android.os.Build.MANUFACTURER?.lowercase() ?: ""
        return m.contains("samsung")
    }
}
