package com.tomasmm.opencharge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DiagnosticActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostico)

        findViewById<TextView>(R.id.txt_device).text = getString(
            R.string.diag_device_value,
            "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )

        findViewById<TextView>(R.id.txt_samsung).text = getString(
            R.string.diag_samsung_value,
            if (WirelessChargeControl.isSamsung(this)) "Sí" else "No"
        )

        refreshKey()

        findViewById<Button>(R.id.btn_write1).setOnClickListener { testWrite(1) }
        findViewById<Button>(R.id.btn_write0).setOnClickListener { testWrite(0) }
        findViewById<Button>(R.id.btn_copy_diag).setOnClickListener { copyAdb() }
        findViewById<TextView>(R.id.txt_adb_diag).text = getString(R.string.adb_command)
    }

    override fun onResume() {
        super.onResume()
        refreshKey()
    }

    private fun refreshKey() {
        val (value, table) = WirelessChargeControl.readAnyTable(this, -2)
        val tv = findViewById<TextView>(R.id.txt_key_value)
        tv.text = if (table == null) {
            getString(R.string.diag_key_not_found)
        } else {
            getString(R.string.diag_key_value, WirelessChargeControl.KEY, value, table)
        }
    }

    private fun testWrite(value: Int) {
        val res = WirelessChargeControl.writeKey(this, value)
        val txt = findViewById<TextView>(R.id.txt_test_result)
        val msg = when (res) {
            WirelessChargeControl.WriteResult.OK ->
                getString(R.string.diag_write_ok, value)
            WirelessChargeControl.WriteResult.NO_PERMISSION ->
                getString(R.string.diag_write_noperm)
            WirelessChargeControl.WriteResult.MISMATCH ->
                getString(R.string.diag_write_mismatch)
            WirelessChargeControl.WriteResult.NOT_FOUND ->
                getString(R.string.diag_key_not_found)
        }
        txt.text = msg
        refreshKey()
    }

    private fun copyAdb() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("adb", getString(R.string.adb_command)))
        Toast.makeText(this, getString(R.string.adb_copied), Toast.LENGTH_SHORT).show()
    }
}
