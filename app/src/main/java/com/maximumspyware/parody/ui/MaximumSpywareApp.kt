package com.maximumspyware.parody.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.maximumspyware.parody.ui.screens.DashboardScreen
import com.maximumspyware.parody.ui.screens.DisclaimerScreen

enum class AppScreen {
    Disclaimer,
    Dashboard,
}

@Composable
fun MaximumSpywareApp() {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Disclaimer) }

    when (currentScreen) {
        AppScreen.Disclaimer -> DisclaimerScreen(
            onAccept = { currentScreen = AppScreen.Dashboard },
        )
        AppScreen.Dashboard -> DashboardScreen()
    }
}
