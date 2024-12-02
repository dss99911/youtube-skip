package com.example.youtubeskip

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val accessibilityManager = getSystemService<AccessibilityManager>()
        if (!accessibilityManager.isAccessibilityOn()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            showDozeDialog()
        }
    }

    fun AccessibilityManager?.isAccessibilityOn(): Boolean {
        if (this == null) {
            return false

        }
        return isAccessibilitySettingOn()
    }


    fun AccessibilityManager.isAccessibilitySettingOn(): Boolean =
        Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(packageName, true)?:false ||
                getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                    .filter { it.id.contains(packageName) }
                    .any()

    @TargetApi(Build.VERSION_CODES.M)
    fun batteryOptimizing(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun showDozeDialog() {
        val batteryOptimizing = batteryOptimizing(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !batteryOptimizing) {
            return
        }

        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (packageManager.resolveActivity(intent, 0) == null) {
            return
        }

        AlertDialog
            .Builder(this)
            .setTitle("disable battern optimization")
            .setMessage("for accessibility service not turned off, disable battery optimization")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}

