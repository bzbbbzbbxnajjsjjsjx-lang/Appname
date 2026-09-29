package com.example.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.theme.AndroidAppTheme
import com.example.androidapp.ui.CalculatorScreen
import com.example.androidapp.vardiya.data.repository.LocalVardiyaRepository
import com.example.androidapp.vardiya.ui.VardiyaAppScaffold
import com.example.androidapp.vardiya.ui.VardiyaViewModel

enum class AppDestination {
    VARDIYA,
    CALCULATOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val vardiyaViewModel: VardiyaViewModel = viewModel {
                VardiyaViewModel(LocalVardiyaRepository(this@MainActivity))
            }
            val uiState by vardiyaViewModel.uiState.collectAsState()

            AndroidAppTheme(dynamicColor = uiState.isDynamicColorEnabled) {
                var isLaunched by remember { mutableStateOf(false) }
                var currentDestination by remember { mutableStateOf(AppDestination.VARDIYA) }

                BackHandler(enabled = currentDestination == AppDestination.CALCULATOR) {
                    currentDestination = AppDestination.VARDIYA
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!isLaunched) {
                        com.example.androidapp.ui.startup.VardiyaLaunchScreen(
                            onLaunchComplete = { isLaunched = true }
                        )
                    } else {
                        when (currentDestination) {
                            AppDestination.VARDIYA -> {
                                VardiyaAppScaffold(
                                    viewModel = vardiyaViewModel,
                                    onNavigateToCalculator = { currentDestination = AppDestination.CALCULATOR }
                                )
                            }
                            AppDestination.CALCULATOR -> {
                                CalculatorScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
