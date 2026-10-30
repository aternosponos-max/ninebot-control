package com.ninebot.control

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ninebot.control.ui.theme.ThemePalette
import androidx.lifecycle.lifecycleScope
import com.ninebot.control.ble.NinebotBleManager
import com.ninebot.control.demo.DemoSimulator
import com.ninebot.control.ui.theme.NinebotControlTheme

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: NinebotBleManager
    private lateinit var demoSimulator: DemoSimulator

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* результаты разрешений обрабатываются неявно при следующем действии пользователя */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = NinebotBleManager(applicationContext)
        demoSimulator = DemoSimulator(lifecycleScope)
        demoSimulator.start()

        requestBlePermissions()

        setContent {
            var palette by remember { mutableStateOf(ThemePalette.GREEN) }
            NinebotControlTheme(palette = palette) {
                NinebotApp(
                    bleManager = bleManager,
                    demoSimulator = demoSimulator,
                    themePalette = palette,
                    onThemePaletteChange = { palette = it }
                )
            }
        }
    }

    private fun requestBlePermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        demoSimulator.stop()
        bleManager.disconnect()
    }
}
