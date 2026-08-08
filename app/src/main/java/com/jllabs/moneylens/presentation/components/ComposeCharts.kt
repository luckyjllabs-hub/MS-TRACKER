package com.jllabs.moneylens.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class PieChartSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun CategoryPieChart(
    slices: List<PieChartSlice>,
    modifier: Modifier = Modifier,
    title: String = "Category spend",
    isPrivacyMasked: Boolean = false,
    isDarkMode: Boolean = false
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return

    val cardBg = if (isDarkMode) Color(0xFF1E241C) else MaterialTheme.colorScheme.surface
    val ink = if (isDarkMode) Color(0xFFE8EDE8) else MaterialTheme.colorScheme.onSurface
    val muted = if (isDarkMode) Color(0xFFA8B2A8) else MaterialTheme.colorScheme.onSurfaceVariant
    val track = if (isDarkMode) Color(0xFF2A322A) else Color(0xFFE4E8E3)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ink)

            Spacer(modifier = Modifier.height(8.dp))

            // Compact stacked share strip — replaces the large donut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(track)
            ) {
                slices.forEach { slice ->
                    val weight = (slice.value / total).coerceAtLeast(0.01f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(slice.color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slices.forEach { slice ->
                    val rawPct = (slice.value / total) * 100f
                    val pctLabel = when {
                        rawPct >= 1f -> "${rawPct.roundToInt()}%"
                        rawPct > 0f -> "<1%"
                        else -> "0%"
                    }
                    val barFraction = (slice.value / total).coerceIn(0.02f, 1f)
                    val amountText = if (isPrivacyMasked) {
                        "••••"
                    } else {
                        "₹${formatShortCurrency(slice.value)}"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(slice.color, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = slice.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ink,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = pctLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = muted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = amountText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ink
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(track, RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barFraction)
                                    .fillMaxHeight()
                                    .background(slice.color, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    incomeValues: List<Float>,
    expenseValues: List<Float>,
    monthLabels: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Monthly Cash Flow", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))
                    Text("Income (+) vs Expense (-) per month", fontSize = 11.sp, color = Color(0xFF7C8079))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF3B7A57), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Income", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B7A57))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFD87D56), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expense", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD87D56))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = (incomeValues + expenseValues).maxOrNull()?.takeIf { it > 0 } ?: 1f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                monthLabels.forEachIndexed { index, monthName ->
                    val inc = incomeValues.getOrElse(index) { 0f }
                    val exp = expenseValues.getOrElse(index) { 0f }

                    val incRatio = (inc / maxVal).coerceIn(if (inc > 0) 0.08f else 0.02f, 1f)
                    val expRatio = (exp / maxVal).coerceIn(if (exp > 0) 0.08f else 0.02f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Amount badges on top
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (inc > 0) {
                                Text(
                                    text = formatShortCurrency(inc),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B7A57)
                                )
                            }
                            if (inc > 0 && exp > 0) {
                                Text("/", fontSize = 9.sp, color = Color(0xFF7C8079))
                            }
                            if (exp > 0) {
                                Text(
                                    text = formatShortCurrency(exp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD87D56)
                                )
                            }
                            if (inc == 0f && exp == 0f) {
                                Text("₹0", fontSize = 9.sp, color = Color(0xFFB0B5AD))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bar pairs container (65.dp leaves plenty of room for month text below)
                        Box(
                            modifier = Modifier
                                .height(65.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Income bar
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(incRatio)
                                        .background(
                                            if (inc > 0) Color(0xFF3B7A57) else Color(0xFFE4E8E3),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                // Expense bar
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(expRatio)
                                        .background(
                                            if (exp > 0) Color(0xFFD87D56) else Color(0xFFE4E8E3),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = monthName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555A52)
                        )
                    }
                }
            }
        }
    }
}

private fun formatShortCurrency(amount: Float): String {
    return when {
        amount >= 100000 -> String.format("%.1fL", amount / 100000)
        amount >= 1000 -> String.format("%.1fk", amount / 1000)
        else -> String.format("%.0f", amount)
    }
}

@Composable
fun DailySpendingLineChart(
    spendingPoints: List<Float>,
    dayLabels: List<String>,
    modifier: Modifier = Modifier
) {
    if (spendingPoints.isEmpty()) return

    val totalSpent = spendingPoints.sum()
    val avgDaily = if (spendingPoints.isNotEmpty()) totalSpent / spendingPoints.size else 0f
    val maxSpent = spendingPoints.maxOrNull() ?: 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Spending Trend", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2D332A))
                    Text("Day-by-day expense breakdown for recent days", fontSize = 11.sp, color = Color(0xFF7C8079))
                }
                Surface(
                    color = Color(0xFFF4F6F3),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Avg: ", fontSize = 10.sp, color = Color(0xFF7C8079))
                        Text(
                            text = "₹${formatShortCurrency(avgDaily)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B7A57)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = spendingPoints.maxOrNull()?.takeIf { it > 0 } ?: 1f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                spendingPoints.forEachIndexed { index, amount ->
                    val dayLabel = dayLabels.getOrElse(index) { "" }
                    val ratio = (amount / maxVal).coerceIn(if (amount > 0) 0.1f else 0.03f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Amount Badge above bar/point
                        Text(
                            text = if (amount > 0) "₹${formatShortCurrency(amount)}" else "₹0",
                            fontSize = 9.sp,
                            fontWeight = if (amount == maxSpent && maxSpent > 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (amount == maxSpent && maxSpent > 0) Color(0xFFD87D56) else if (amount > 0) Color(0xFF3B7A57) else Color(0xFFB0B5AD)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Vertical Trend Column (65.dp leaves plenty of room for day text below)
                        Box(
                            modifier = Modifier
                                .height(65.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(ratio)
                                    .background(
                                        color = if (amount == maxSpent && maxSpent > 0) Color(0xFFD87D56) else if (amount > 0) Color(0xFF3B7A57) else Color(0xFFE4E8E3),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dayLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555A52)
                        )
                    }
                }
            }
        }
    }
}
