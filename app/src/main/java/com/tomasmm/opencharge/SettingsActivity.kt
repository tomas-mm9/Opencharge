package com.tomasmm.opencharge

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        val edHigh = findViewById<EditText>(R.id.ed_high)
        val edLow = findViewById<EditText>(R.id.ed_low)
        val edCooldown = findViewById<EditText>(R.id.ed_cooldown)
        val edFast = findViewById<EditText>(R.id.ed_fast)
        val edSlow = findViewById<EditText>(R.id.ed_slow)
        val swGentle = findViewById<MaterialSwitch>(R.id.sw_gentle)

        edHigh.setText(prefs.tempHigh.toString())
        edLow.setText(prefs.tempLow.toString())
        edCooldown.setText(prefs.cooldownMin.toString())
        edFast.setText(prefs.gentleFastMin.toString())
        edSlow.setText(prefs.gentleSlowMin.toString())
        swGentle.isChecked = prefs.gentleMode

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val high = edHigh.text.toString().toIntOrNull()
            val low = edLow.text.toString().toIntOrNull()
            val cooldown = edCooldown.text.toString().toIntOrNull()
            val fast = edFast.text.toString().toIntOrNull()
            val slow = edSlow.text.toString().toIntOrNull()

            if (high == null || low == null || cooldown == null || fast == null || slow == null ||
                high <= low || fast <= 0 || slow <= 0 || cooldown <= 0
            ) {
                Toast.makeText(this, getString(R.string.settings_invalid), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            prefs.tempHigh = high
            prefs.tempLow = low
            prefs.cooldownMin = cooldown
            prefs.gentleFastMin = fast
            prefs.gentleSlowMin = slow
            prefs.gentleMode = swGentle.isChecked

            if (prefs.masterEnabled || (prefs.autoEnable && BatteryReader.read(this).wireless)) {
                ServiceUtils.startService(this)
            }
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            prefs.tempHigh = 40
            prefs.tempLow = 36
            prefs.cooldownMin = 5
            prefs.gentleFastMin = 20
            prefs.gentleSlowMin = 10
            prefs.gentleMode = false
            edHigh.setText("40")
            edLow.setText("36")
            edCooldown.setText("5")
            edFast.setText("20")
            edSlow.setText("10")
            swGentle.isChecked = false
            Toast.makeText(this, getString(R.string.settings_reset), Toast.LENGTH_SHORT).show()
        }
    }
}
