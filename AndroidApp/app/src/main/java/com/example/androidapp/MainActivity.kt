package com.example.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.androidapp.theme.AndroidAppTheme
import com.example.androidapp.ui.CalculatorScreen
import com.example.androidapp.vardiya.ui.VardiyaScreen

enum class AppDestination {
    VARDIYA,
    CALCULATOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AndroidAppTheme {
                var currentDestination by remember { mutableStateOf(AppDestination.VARDIYA) }

                BackHandler(enabled = currentDestination == AppDestination.CALCULATOR) {
                    currentDestination = AppDestination.VARDIYA
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (currentDestination) {
                        AppDestination.VARDIYA -> {
                            VardiyaScreen(
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
