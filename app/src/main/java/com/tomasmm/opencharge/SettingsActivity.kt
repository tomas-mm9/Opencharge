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
        val edSetpoint = findViewById<EditText>(R.id.ed_setpoint)
        val edSafety = findViewById<EditText>(R.id.ed_safety)
        val edPeriod = findViewById<EditText>(R.id.ed_period)
        val swRestart = findViewById<MaterialSwitch>(R.id.sw_restart)
        val edHigh = findViewById<EditText>(R.id.ed_high)
        val edLow = findViewById<EditText>(R.id.ed_low)
        val edCooldown = findViewById<EditText>(R.id.ed_cooldown)

        swDynamic.isChecked = prefs.controllerMode == "DYNAMIC"
        edSetpoint.setText(prefs.setpoint.toString())
        edSafety.setText(prefs.safetyHigh.toString())
        edPeriod.setText(prefs.periodMin.toString())
        swRestart.isChecked = prefs.applyNeedsRestart
        edHigh.setText(prefs.tempHigh.toString())
        edLow.setText(prefs.tempLow.toString())
        edCooldown.setText(prefs.cooldownMin.toString())

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val setpoint = edSetpoint.text.toString().toIntOrNull()
            val safety = edSafety.text.toString().toIntOrNull()
            val period = edPeriod.text.toString().toIntOrNull()
            val high = edHigh.text.toString().toIntOrNull()
            val low = edLow.text.toString().toIntOrNull()
            val cooldown = edCooldown.text.toString().toIntOrNull()

            if (setpoint == null || safety == null || period == null ||
                high == null || low == null || cooldown == null ||
                safety <= setpoint || high <= low || period < 1 || cooldown < 1
            ) {
                Toast.makeText(this, getString(R.string.settings_invalid), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            prefs.controllerMode = if (swDynamic.isChecked) "DYNAMIC" else "SIMPLE"
            prefs.setpoint = setpoint
            prefs.safetyHigh = safety
            prefs.periodMin = period
            prefs.applyNeedsRestart = swRestart.isChecked
            prefs.tempHigh = high
            prefs.tempLow = low
            prefs.cooldownMin = cooldown

            if (prefs.masterEnabled || (prefs.autoEnable && BatteryReader.read(this).wireless)) {
                ServiceUtils.startService(this)
            }
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            prefs.controllerMode = "DYNAMIC"
            prefs.setpoint = 37
            prefs.safetyHigh = 42
            prefs.periodMin = 3
            prefs.applyNeedsRestart = true
            prefs.tempHigh = 40
            prefs.tempLow = 36
            prefs.cooldownMin = 5
            swDynamic.isChecked = true
            edSetpoint.setText("37")
            edSafety.setText("42")
            edPeriod.setText("3")
            swRestart.isChecked = true
            edHigh.setText("40")
            edLow.setText("36")
            edCooldown.setText("5")
            Toast.makeText(this, getString(R.string.settings_reset), Toast.LENGTH_SHORT).show()
        }
    }
}
