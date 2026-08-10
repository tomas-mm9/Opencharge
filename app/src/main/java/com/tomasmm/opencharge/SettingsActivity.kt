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

        val swDynamic = findViewById<MaterialSwitch>(R.id.sw_dynamic)
        val edPeriod = findViewById<EditText>(R.id.ed_period)
        val swRestart = findViewById<MaterialSwitch>(R.id.sw_restart)
        val edCooldown = findViewById<EditText>(R.id.ed_cooldown)

        swDynamic.isChecked = prefs.controllerMode == "DYNAMIC"
        edPeriod.setText(prefs.periodMin.toString())
        swRestart.isChecked = prefs.applyNeedsRestart
        edCooldown.setText(prefs.cooldownMin.toString())

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val period = edPeriod.text.toString().toIntOrNull()
            val cooldown = edCooldown.text.toString().toIntOrNull()

            if (period == null || cooldown == null || period < 1 || cooldown < 1) {
                Toast.makeText(this, getString(R.string.settings_invalid), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            prefs.controllerMode = if (swDynamic.isChecked) "DYNAMIC" else "SIMPLE"
            prefs.periodMin = period
            prefs.applyNeedsRestart = swRestart.isChecked
            prefs.cooldownMin = cooldown

            if (prefs.masterEnabled || (prefs.autoEnable && BatteryReader.read(this).wireless)) {
                ServiceUtils.startService(this)
            }
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            prefs.controllerMode = "DYNAMIC"
            prefs.periodMin = 3
            prefs.applyNeedsRestart = true
            prefs.cooldownMin = 5
            swDynamic.isChecked = true
            edPeriod.setText("3")
            swRestart.isChecked = true
            edCooldown.setText("5")
            Toast.makeText(this, getString(R.string.settings_reset), Toast.LENGTH_SHORT).show()
        }
    }
}
