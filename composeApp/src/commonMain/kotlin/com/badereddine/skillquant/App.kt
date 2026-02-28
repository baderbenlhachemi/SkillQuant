package com.badereddine.skillquant

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.badereddine.skillquant.ui.dashboard.DashboardScreen
import com.badereddine.skillquant.ui.theme.SkillQuantTheme
import com.badereddine.skillquant.util.ThemeManager
import org.koin.compose.koinInject

@Composable
fun App() {
    val themeManager = koinInject<ThemeManager>()
    val themeMode by themeManager.themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    SkillQuantTheme(darkTheme = darkTheme) {
        Navigator(DashboardScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}