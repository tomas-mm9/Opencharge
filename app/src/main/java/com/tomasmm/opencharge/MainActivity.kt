package com.tomasmm.opencharge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var swMaster: MaterialSwitch
    private lateinit var swAuto: MaterialSwitch
    private lateinit var txtTemp: TextView
    private lateinit var txtLevel: TextView
    private lateinit var txtSource: TextView
    private lateinit var txtCurrent: TextView
    private lateinit var txtAvg: TextView
    private lateinit var txtMode: TextView
    private lateinit var txtPerm: TextView
    private lateinit var txtNote: TextView

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshStatus()
        }
    }

    private val uiRunnable = object : Runnable {
        override fun run() {
            refreshStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        swMaster = findViewById(R.id.sw_master)
        swAuto = findViewById(R.id.sw_auto)
        txtTemp = findViewById(R.id.txt_temp)
        txtLevel = findViewById(R.id.txt_level)
        txtSource = findViewById(R.id.txt_source)
        txtCurrent = findViewById(R.id.txt_current)
        txtAvg = findViewById(R.id.txt_avg)
        txtMode = findViewById(R.id.txt_mode)
        txtPerm = findViewById(R.id.txt_perm)
        txtNote = findViewById(R.id.txt_note)

        swMaster.isChecked = prefs.masterEnabled
        swAuto.isChecked = prefs.autoEnable

        swMaster.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                prefs.masterEnabled = true
                requestNotifPermissionIfNeeded()
                ServiceUtils.startService(this)
            } else {
                prefs.masterEnabled = false
                if (prefs.autoActive) prefs.autoActive = false
                val bs = BatteryReader.read(this)
                if (!(prefs.autoEnable && bs.wireless)) {
                    ServiceUtils.stopService(this)
                }
            }
        }

        swAuto.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                prefs.autoEnable = true
                val bs = BatteryReader.read(this)
                if (bs.wireless) {
                    prefs.autoActive = true
                    ServiceUtils.startService(this)
                }
            } else {
                prefs.autoEnable = false
                if (prefs.autoActive) prefs.autoActive = false
                if (!prefs.masterEnabled) {
                    ServiceUtils.stopService(this)
                }
            }
        }

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btn_diagnostico).setOnClickListener {
            startActivity(Intent(this, DiagnosticActivity::class.java))
        }
        findViewById<Button>(R.id.btn_battery).setOnClickListener {
            openBatteryOptimization()
        }
        findViewById<Button>(R.id.btn_copy).setOnClickListener {
            copyAdbCommand()
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(statusReceiver, IntentFilter(ServiceUtils.ACTION_STATUS))
        handler.post(uiRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(uiRunnable)
        runCatching { unregisterReceiver(statusReceiver) }
    }

    override fun onResume() {
        super.onResume()
        swMaster.isChecked = prefs.masterEnabled
        swAuto.isChecked = prefs.autoEnable
        refreshStatus()
    }

    private fun refreshStatus() {
        val bs = BatteryReader.read(this)
        StateHolder.tempC = bs.tempC
        StateHolder.level = bs.level
        StateHolder.charging = bs.charging
        StateHolder.wireless = bs.wireless
        StateHolder.source = when {
            !bs.charging && bs.plugged == 0 -> "No cargando"
            bs.wireless -> "Inalámbrica"
            else -> "Cable"
        }

        txtTemp.text = getString(R.string.status_temp_value, StateHolder.tempDisplay())
        txtLevel.text = getString(R.string.status_level_value, if (bs.level >= 0) "${bs.level}%" else "—")
        txtSource.text = getString(R.string.status_source_value, StateHolder.source)

        val current = BatteryReader.currentNow(this)
        txtCurrent.text = getString(
            R.string.status_current_value,
            if (current == Int.MIN_VALUE) "—" else "${current / 1000} mA"
        )

        txtAvg.text = getString(
            R.string.status_avg_value,
            if (StateHolder.avgWatts > 0) "%.1f W".format(StateHolder.avgWatts) else "—"
        )

        txtMode.text = getString(R.string.status_mode_value, StateHolder.mode)

        val permOk = StateHolder.permissionOk
        val permText = if (permOk) getString(R.string.perm_ok) else getString(R.string.perm_missing)
        txtPerm.text = getString(R.string.status_perm_value, permText)

        txtNote.text = getString(R.string.status_note_value, StateHolder.lastNote.ifBlank { getString(R.string.note_idle) })
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val has = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!has) notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.battery_opt_already_off),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        try {
            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName"))
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun copyAdbCommand() {
        val cmd = getString(R.string.adb_command)
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("adb", cmd))
        Toast.makeText(this, getString(R.string.adb_copied), Toast.LENGTH_SHORT).show()
    }
}
