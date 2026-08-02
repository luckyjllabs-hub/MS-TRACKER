package com.example.mstrackerapp.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.mstrackerapp.presentation.screens.MainScreen
import kotlinx.serialization.Serializable

enum class AppTab {
    OVERVIEW, TRANSACTIONS, GOALS, ACCOUNTS, BUDGET, SETTINGS, SMS_INBOX
}

@Serializable
data object MainKey : NavKey

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(MainKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MainKey> {
                MainScreen(
                    onItemClick = { navKey -> backStack.add(navKey) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
