package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.SyllabusTrackerTheme
import com.example.ui.viewmodel.SyllabusViewModel

class MainActivity : ComponentActivity() {

  private val syllabusViewModel: SyllabusViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val state by syllabusViewModel.uiState.collectAsState()

      // Prompt for notifications permission on Android 13+
      RequestNotificationPermission()

      SyllabusTrackerTheme(themeMode = state.themeMode) {
        MainAppScreen(viewModel = syllabusViewModel)
      }
    }
  }
}

@Composable
private fun RequestNotificationPermission() {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
      // Handled
    }

    LaunchedEffect(Unit) {
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}

