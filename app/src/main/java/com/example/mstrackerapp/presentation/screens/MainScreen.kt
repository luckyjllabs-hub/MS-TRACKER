package com.example.mstrackerapp.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.mstrackerapp.data.DefaultMSTrackerRepository
import com.example.mstrackerapp.presentation.components.*
import com.example.mstrackerapp.presentation.navigation.AppTab

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current.applicationContext,
    viewModel: MSTrackerViewModel = viewModel { MSTrackerViewModel(DefaultMSTrackerRepository(context)) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddTxDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBarHeader(
                activeTab = uiState.activeTab,
                smsCount = uiState.smsQueue.size,
                onSmsClick = { viewModel.selectTab(AppTab.SMS_INBOX) }
            )
        },
        bottomBar = {
            MSTrackerBottomNavigation(
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
                .background(Color(0xFFF4F3EF))
                .padding(paddingValues)
        ) {
            when (uiState.activeTab) {
                AppTab.OVERVIEW -> OverviewScreen(uiState = uiState, viewModel = viewModel)
                AppTab.TRANSACTIONS -> TransactionsLedgerScreen(uiState = uiState, viewModel = viewModel)
                AppTab.GOALS -> GoalsScreen(uiState = uiState, viewModel = viewModel)
                AppTab.ACCOUNTS -> AccountsScreen(uiState = uiState, viewModel = viewModel)
                AppTab.BUDGET -> BudgetScreen(uiState = uiState, viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
                AppTab.SMS_INBOX -> SmsInboxScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            onDismiss = { showAddTxDialog = false },
            onConfirm = { type, amount, accId, catId, merchant, note ->
                viewModel.addTransaction(type, amount, accId, catId, merchant, note)
                showAddTxDialog = false
            }
        )
    }
}
