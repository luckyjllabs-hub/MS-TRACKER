package com.jllabs.moneylens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jllabs.moneylens.data.parser.SmsInboxScanner
import com.jllabs.moneylens.presentation.navigation.MainNavigation
import com.jllabs.moneylens.theme.MoneyLensTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            lifecycleScope.launch(Dispatchers.IO) {
                SmsInboxScanner.scanExistingInbox(applicationContext)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestSmsPermissions()

        enableEdgeToEdge()
        setContent {
            // Theme is applied in MainScreen from persisted dark-mode preference
            MoneyLensTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavigation()
                }
            }
        }
    }

    private fun checkAndRequestSmsPermissions() {
        val readSmsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val receiveSmsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

        if (!readSmsGranted || !receiveSmsGranted) {
            smsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                )
            )
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                SmsInboxScanner.scanExistingInbox(applicationContext)
            }
        }
    }
}
