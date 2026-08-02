package com.example.mstrackerapp.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mstrackerapp.domain.models.Goal
import com.example.mstrackerapp.utils.Money

@Composable
fun GoalsScreen(uiState: MSTrackerUiState, viewModel: MSTrackerViewModel) {
    var showAddGoalDialog by remember { mutableStateOf(false) }

    val totalTargetMinor = uiState.goals.sumOf { it.targetAmountMinor }
    val totalSavedMinor = uiState.goals.sumOf { it.currentSavedMinor }
    val overallProgress = if (totalTargetMinor > 0) {
        (totalSavedMinor.toFloat() / totalTargetMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Financial Goal Tracker",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D332A)
                )
                Text(
                    text = "Track savings progress, monthly contributions & estimated completion",
                    fontSize = 12.sp,
                    color = Color(0xFF7C8079)
                )
            }

            IconButton(
                onClick = { showAddGoalDialog = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF3B7A57), contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Goals Overview Progress Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B7A57)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Total Savings Progress", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    Text("${(overallProgress * 100).toInt()}% Saved", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFFA8E6CF),
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved: ${if (uiState.isPrivacyMasked) "••••" else Money.format(totalSavedMinor)}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Target: ${if (uiState.isPrivacyMasked) "••••" else Money.format(totalTargetMinor)}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Goals List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.goals, key = { it.id }) { goal ->
                GoalTrackerCardItem(goal = goal, isPrivacyMasked = uiState.isPrivacyMasked)
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, target, icon, deadline ->
                viewModel.addGoal(name, target, icon, deadline)
                showAddGoalDialog = false
            }
        )
    }
}

@Composable
fun GoalTrackerCardItem(goal: Goal, isPrivacyMasked: Boolean) {
    val progress = if (goal.targetAmountMinor > 0) {
        (goal.currentSavedMinor.toFloat() / goal.targetAmountMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remainingMinor = goal.targetAmountMinor - goal.currentSavedMinor

    // Monthly contribution assumption: 10% of target or ₹10,000/month
    val estimatedMonthlyContributionMinor = (goal.targetAmountMinor * 0.10).toLong().coerceAtLeast(500000L)
    val monthsRemaining = if (estimatedMonthlyContributionMinor > 0 && remainingMinor > 0) {
        (remainingMinor.toDouble() / estimatedMonthlyContributionMinor.toDouble())
    } else 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Row 1: Icon, Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFF4F3EF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(goal.icon, fontSize = 22.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF2D332A)
                        )
                        Text(
                            text = "Target Date: ${goal.deadline}",
                            fontSize = 11.sp,
                            color = Color(0xFF7C8079)
                        )
                    }
                }

                Surface(
                    color = if (progress >= 1f) Color(0xFFE4E8E3) else Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (progress >= 1f) "Completed 🎉" else "${(progress * 100).toInt()}% Done",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progress >= 1f) Color(0xFF3B7A57) else Color(0xFF856404)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Target, Current Saved, Monthly Contribution
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Target", fontSize = 10.sp, color = Color(0xFF7C8079))
                    Text(
                        text = if (isPrivacyMasked) "••••" else Money.format(goal.targetAmountMinor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2D332A)
                    )
                }

                Column {
                    Text("Current Saved", fontSize = 10.sp, color = Color(0xFF7C8079))
                    Text(
                        text = if (isPrivacyMasked) "••••" else Money.format(goal.currentSavedMinor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF3B7A57)
                    )
                }

                Column {
                    Text("Monthly Contrib.", fontSize = 10.sp, color = Color(0xFF7C8079))
                    Text(
                        text = if (isPrivacyMasked) "••••" else Money.format(estimatedMonthlyContributionMinor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF45B7D1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF3B7A57),
                trackColor = Color(0xFFE4E8E3),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Estimated Completion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF3B7A57), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (progress >= 1f) "Goal Achieved!" else "Est. Completion: ~${"%.1f".format(monthsRemaining)} months remaining",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF3B7A57)
                    )
                }

                Text(
                    text = "Remaining: ${Money.format(remainingMinor.coerceAtLeast(0L))}",
                    fontSize = 10.sp,
                    color = Color(0xFF7C8079)
                )
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetRupees: Double, icon: String, deadline: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎯") }
    var deadline by remember { mutableStateOf("2026-12-31") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color(0xFF2D332A),
        textContentColor = Color(0xFF2D332A),
        modifier = Modifier
            .widthIn(max = 520.dp)
            .imePadding(),
        title = { Text("Create Savings Goal", fontWeight = FontWeight.Bold, color = Color(0xFF2D332A)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. New Phone, Vacation)") },
                    colors = com.example.mstrackerapp.presentation.components.appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Amount (₹)") },
                    colors = com.example.mstrackerapp.presentation.components.appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Target Deadline (yyyy-MM-dd)") },
                    colors = com.example.mstrackerapp.presentation.components.appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    onConfirm(name, target, icon, deadline)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7A57), contentColor = Color.White),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Create Goal", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF3B7A57)),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Cancel", color = Color(0xFF3B7A57), fontWeight = FontWeight.Bold)
            }
        }
    )
}
