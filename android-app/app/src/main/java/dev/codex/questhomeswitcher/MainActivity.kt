package dev.codex.questhomeswitcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.codex.questhomeswitcher.ui.HomeSwitcherApp
import dev.codex.questhomeswitcher.ui.HomeSwitcherViewModel

class MainActivity : ComponentActivity() {
    private val homeSwitcherViewModel: HomeSwitcherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermission()
        setContent {
            HomeSwitcherApp(viewModel = homeSwitcherViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        homeSwitcherViewModel.onAppResumed()
    }

    override fun onPause() {
        homeSwitcherViewModel.onAppPaused()
        super.onPause()
    }

    private fun requestStoragePermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 7)
        }
    }
}
