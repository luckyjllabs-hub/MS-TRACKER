package com.jllabs.moneylens.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.TransactionType
import com.jllabs.moneylens.utils.Money

@Composable
fun BudgetScreen(uiState: MoneyLensUiState, viewModel: MoneyLensViewModel) {
    val totalMonthlyLimitMinor = uiState.categories.sumOf { it.monthlyLimitMinor }
    val totalSpentMinor = uiState.totalExpenseMinor
    val totalRemainingMinor = totalMonthlyLimitMinor - totalSpentMinor

    val monthlyProgress = if (totalMonthlyLimitMinor > 0) {
        (totalSpentMinor.toFloat() / totalMonthlyLimitMinor.toFloat()).coerceIn(0f, 1.5f)
    } else 0f

    // Exceeded & Warning Category Alerts
    val categoryBudgets = uiState.categories.map { category ->
        val categorySpentMinor = uiState.transactions
            .filter { it.categoryId == category.id && it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor }
        val categoryRemainingMinor = category.monthlyLimitMinor - categorySpentMinor
        val progress = if (category.monthlyLimitMinor > 0) {
            (categorySpentMinor.toFloat() / category.monthlyLimitMinor.toFloat()).coerceIn(0f, 1.5f)
        } else 0f

        CategoryBudgetStatus(
            category = category,
            spentMinor = categorySpentMinor,
            limitMinor = category.monthlyLimitMinor,
            remainingMinor = categoryRemainingMinor,
            progress = progress
        )
    }

    val exceededCategories = categoryBudgets.filter { it.limitMinor > 0 && it.spentMinor > it.limitMinor }
    val warningCategories = categoryBudgets.filter { it.limitMinor > 0 && it.progress in 0.80f..0.99f }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Budget & Spend Limits",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D332A)
        )
        Text(
            text = "Set maximum budget limits for each category to track monthly spending",
            fontSize = 12.sp,
            color = Color(0xFF7C8079)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Budget Alerts Section
            if (exceededCategories.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Budget Exceeded Alert!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD32F2F))
                                Text(
                                    text = "${exceededCategories.size} category budgets exceeded limit this month (${exceededCategories.joinToString { it.category.name }})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF5C1D1D)
                                )
                            }
                        }
                    }
                }
            } else if (warningCategories.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Budget Warning (80%+ Used)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFF57F17))
                                Text(
                                    text = "Approaching budget limits in ${warningCategories.joinToString { it.category.name }}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF5D4037)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Monthly Budget Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly Budget Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2D332A))
                            Surface(
                                color = if (totalRemainingMinor >= 0) Color(0xFFE4E8E3) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (totalRemainingMinor >= 0) "Remaining: ${Money.format(totalRemainingMinor)}" else "Exceeded by ${Money.format(-totalRemainingMinor)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalRemainingMinor >= 0) Color(0xFF3B7A57) else Color(0xFFD32F2F)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { monthlyProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = if (monthlyProgress >= 1.0f) Color(0xFFD32F2F) else if (monthlyProgress >= 0.8f) Color(0xFFF57F17) else Color(0xFF3B7A57),
                            trackColor = Color(0xFFE4E8E3),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Spent: ${Money.format(totalSpentMinor)}", fontSize = 12.sp, color = Color(0xFF7C8079))
                            Text("Total Budget: ${Money.format(totalMonthlyLimitMinor)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B7A57))
                        }
                    }
                }
            }

            // 3. Category Budget Breakdown Header
            item {
                Text("Category Spending Limits", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2D332A))
            }

            // 4. Interactive Category Budget Items
            items(categoryBudgets, key = { it.category.id }) { item ->
                CategoryBudgetAdjustableCard(
                    status = item,
                    onUpdateLimit = { newLimitRupees ->
                        viewModel.updateCategoryLimit(item.category.id, newLimitRupees)
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetAdjustableCard(
    status: CategoryBudgetStatus,
    onUpdateLimit: (Double) -> Unit
) {
    val currentLimitRupees = status.limitMinor / 100.0
    
    // Bidirectional state: Slider & Text Input
    var sliderValue by remember(status.limitMinor) { mutableFloatStateOf(currentLimitRupees.toFloat()) }
    var inputText by remember(status.limitMinor) { mutableStateOf(if (status.limitMinor > 0) currentLimitRupees.toInt().toString() else "0") }

    val isExceeded = status.remainingMinor < 0 && status.limitMinor > 0

    // Strict maximum slider track limit: ₹50,000
    val maxSliderRange = 50000f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header Row: Category Icon + Name (Left) | Status Badge (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(status.category.icon, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.category.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF2D332A)
                    )
                }

                Surface(
                    color = if (isExceeded) Color(0xFFFFEBEE) else if (status.limitMinor == 0L) Color(0xFFF4F6F3) else Color(0xFFE4E8E3),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (status.limitMinor == 0L) "No Limit" else if (isExceeded) "Exceeded: -${Money.format(-status.remainingMinor)}" else "Left: ${Money.format(status.remainingMinor)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExceeded) Color(0xFFD32F2F) else if (status.limitMinor == 0L) Color(0xFF7C8079) else Color(0xFF3B7A57)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Progress Bar
            LinearProgressIndicator(
                progress = { if (status.limitMinor > 0) status.progress.coerceIn(0f, 1f) else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (status.progress >= 1.0f && status.limitMinor > 0) Color(0xFFD32F2F) else if (status.progress >= 0.8f) Color(0xFFF57F17) else Color(0xFF3B7A57),
                trackColor = Color(0xFFE4E8E3),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Values & Limit Input Row (Perfect Alignment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ${Money.format(status.spentMinor)}",
                    fontSize = 12.sp,
                    color = Color(0xFF7C8079)
                )

                // Compact, perfectly aligned Max Limit Field
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Max Limit: ₹",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555A52)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF4F6F3),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D5CE)),
                        modifier = Modifier
                            .width(85.dp)
                            .height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = inputText,
                                onValueChange = { text ->
                                    val filtered = text.filter { it.isDigit() }
                                    inputText = filtered
                                    val valDouble = filtered.toDoubleOrNull() ?: 0.0
                                    sliderValue = valDouble.toFloat().coerceIn(0f, 50000f)
                                    onUpdateLimit(valDouble)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B7A57),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Interactive Slider track strictly capped at ₹50,000 maximum
            Slider(
                value = sliderValue.coerceIn(0f, 50000f),
                onValueChange = { newValue ->
                    val roundedValue = (newValue / 250).toInt() * 250f
                    val clamped = roundedValue.coerceIn(0f, 50000f)
                    sliderValue = clamped
                    inputText = clamped.toInt().toString()
                },
                onValueChangeFinished = {
                    onUpdateLimit(sliderValue.toDouble())
                },
                valueRange = 0f..50000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF3B7A57),
                    activeTrackColor = Color(0xFF3B7A57),
                    inactiveTrackColor = Color(0xFFE4E8E3)
                ),
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )
        }
    }
}

private data class CategoryBudgetStatus(
    val category: Category,
    val spentMinor: Long,
    val limitMinor: Long,
    val remainingMinor: Long,
    val progress: Float
)
