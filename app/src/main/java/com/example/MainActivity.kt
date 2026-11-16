package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.InvoiceViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PricingScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    LOGIN, DASHBOARD, SCAN, PRICING
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable true edge-to-edge drawing
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppNavigation()
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: InvoiceViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }

    // Coordinate screen state based on authentication sessions
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            currentScreen = AppScreen.LOGIN
        } else if (currentScreen == AppScreen.LOGIN) {
            currentScreen = AppScreen.DASHBOARD
        }
    }

    Crossfade(targetState = currentScreen, label = "navigation_transition") { screen ->
        when (screen) {
            AppScreen.LOGIN -> LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { currentScreen = AppScreen.DASHBOARD }
            )
            AppScreen.DASHBOARD -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToScan = { currentScreen = AppScreen.SCAN },
                onNavigateToPricing = { currentScreen = AppScreen.PRICING },
                onSignOut = { viewModel.signOut() }
            )
            AppScreen.SCAN -> ScanScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
            )
            AppScreen.PRICING -> PricingScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
            )
        }
    }
}
