package com.jllabs.moneylens.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.jllabs.moneylens.data.DefaultMoneyLensRepository
import com.jllabs.moneylens.presentation.components.*
import com.jllabs.moneylens.presentation.navigation.AppTab
import com.jllabs.moneylens.theme.MoneyLensTheme

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current.applicationContext,
    viewModel: MoneyLensViewModel = viewModel { MoneyLensViewModel(DefaultMoneyLensRepository(context)) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddTxDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600
    val pageBg = if (uiState.isDarkMode) Color(0xFF121612) else Color(0xFFF4F3EF)
    val chromeBg = if (uiState.isDarkMode) Color(0xFF1E241C) else Color.White

    MoneyLensTheme(darkTheme = uiState.isDarkMode) {
    if (isTabletOrLandscape) {
        // Adaptive Layout for Medium/Expanded Screens (Tablets, Foldables, Landscape)
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(pageBg)
                .safeDrawingPadding()
        ) {
            NavigationRail(
                containerColor = chromeBg,
                header = {
                    FloatingActionButton(
                        onClick = { showAddTxDialog = true },
                        containerColor = Color(0xFF3B7A57),
                        contentColor = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                }
            ) {
                NavigationRailItem(
                    selected = uiState.activeTab == AppTab.OVERVIEW,
                    onClick = { viewModel.selectTab(AppTab.OVERVIEW) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Overview") },
                    label = { Text("Overview", fontSize = 10.sp) }
                )
                NavigationRailItem(
                    selected = uiState.activeTab == AppTab.REMINDERS,
                    onClick = { viewModel.selectTab(AppTab.REMINDERS) },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                    label = { Text("Reminders", fontSize = 10.sp) }
                )
                NavigationRailItem(
                    selected = uiState.activeTab == AppTab.ACCOUNTS,
                    onClick = { viewModel.selectTab(AppTab.ACCOUNTS) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Accounts") },
                    label = { Text("Accounts", fontSize = 10.sp) }
                )
                NavigationRailItem(
                    selected = uiState.activeTab == AppTab.SETTINGS,
                    onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) }
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBarHeader(
                    activeTab = uiState.activeTab,
                    smsCount = uiState.smsQueue.size,
                    onSmsClick = { viewModel.selectTab(AppTab.SMS_INBOX) }
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.fillMaxSize().widthIn(max = 1000.dp)) {
                        RenderMainTabContent(uiState, viewModel)
                    }
                }
            }
        }
    } else {
        // Standard Compact Layout for Phones (Portrait)
        Scaffold(
            modifier = modifier,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBarHeader(
                    activeTab = uiState.activeTab,
                    smsCount = uiState.smsQueue.size,
                    onSmsClick = { viewModel.selectTab(AppTab.SMS_INBOX) }
                )
            },
            bottomBar = {
                MoneyLensBottomNavigation(
                    activeTab = uiState.activeTab,
                    smsCount = uiState.smsQueue.size,
                    onTabSelect = viewModel::selectTab
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddTxDialog = true },
                    containerColor = Color(0xFF3B7A57),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBg)
                    .padding(paddingValues)
            ) {
                RenderMainTabContent(uiState, viewModel)
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            isDarkMode = uiState.isDarkMode,
            onDismiss = { showAddTxDialog = false },
            onConfirm = { type, amount, accId, catId, merchant, note ->
                viewModel.addTransaction(type, amount, accId, catId, merchant, note)
                showAddTxDialog = false
            }
        )
    }
    } // MoneyLensTheme
}

@Composable
private fun RenderMainTabContent(uiState: MoneyLensUiState, viewModel: MoneyLensViewModel) {
    when (uiState.activeTab) {
        AppTab.OVERVIEW -> OverviewScreen(uiState = uiState, viewModel = viewModel)
        AppTab.TRANSACTIONS -> TransactionsLedgerScreen(uiState = uiState, viewModel = viewModel)
        AppTab.REMINDERS -> RemindersScreen(uiState = uiState, viewModel = viewModel)
        AppTab.ACCOUNTS -> AccountsScreen(uiState = uiState, viewModel = viewModel)
        AppTab.BUDGET -> SettingsScreen(uiState = uiState, viewModel = viewModel)
        AppTab.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
        AppTab.SMS_INBOX -> SmsInboxScreen(uiState = uiState, viewModel = viewModel)
        AppTab.CLASSIFICATION_REVIEW -> SettingsScreen(uiState = uiState, viewModel = viewModel)
    }
}
