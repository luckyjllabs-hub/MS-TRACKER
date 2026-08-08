package com.jllabs.moneylens.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.jllabs.moneylens.presentation.screens.MainScreen
import kotlinx.serialization.Serializable

enum class AppTab {
    OVERVIEW, TRANSACTIONS, REMINDERS, ACCOUNTS, BUDGET, SETTINGS, SMS_INBOX, CLASSIFICATION_REVIEW
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
