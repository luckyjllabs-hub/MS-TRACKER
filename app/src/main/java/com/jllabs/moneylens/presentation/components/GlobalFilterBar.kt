package com.jllabs.moneylens.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jllabs.moneylens.domain.models.Category
import com.jllabs.moneylens.domain.models.Transaction
import com.jllabs.moneylens.utils.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val GlobalPeriodOptions = listOf(
    "All",
    "Today",
    "Yesterday",
    "This Week",
    "This Month",
    "3 Months",
    "6 Months",
    "1 Year",
    "Custom"
)

/**
 * Always-visible global search bar. Trailing search + filter icons use theme colors.
 */
@Composable
fun GlobalSearchBar(
    searchQuery: String,
    selectedFilter: String,
    isDarkMode: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    placeholder: String = "Search name, merchant, bank, amount…",
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val iconTint = scheme.onSurfaceVariant
    val showFilterBadge = !selectedFilter.equals("This Month", ignoreCase = true)

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .heightIn(min = 52.dp),
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = scheme.onSurface),
        shape = RoundedCornerShape(14.dp),
        placeholder = {
            Text(
                placeholder,
                fontSize = 13.sp,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = iconTint,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(22.dp)
                )
                BadgedBox(
                    badge = {
                        if (showFilterBadge) {
                            Badge(containerColor = scheme.error) {
                                Text(
                                    text = selectedFilter.take(1),
                                    color = scheme.onError,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                ) {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = "Filter period",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        colors = appTextFieldColors(isDarkMode)
    )
}

/** Match merchant / bank / category / note / date / amount / raw SMS. */
fun transactionMatchesSearch(
    tx: Transaction,
    query: String,
    categories: List<Category> = emptyList()
): Boolean {
    val q = query.trim().lowercase(Locale.US)
    if (q.isEmpty()) return true

    val categoryName = categories.find { it.id == tx.categoryId }?.name.orEmpty()
    val amountRupees = tx.amountMinor / 100.0
    val amountPlain = String.format(Locale.US, "%.2f", amountRupees)
    val amountInt = (tx.amountMinor / 100).toString()
    val amountFormatted = Money.format(tx.amountMinor)
        .lowercase(Locale.US)
        .replace("₹", "")
        .replace(",", "")
        .replace(" ", "")
    val qDigits = q.replace(",", "").replace("₹", "").replace("rs.", "").replace("rs", "").trim()

    return tx.merchant.lowercase(Locale.US).contains(q) ||
        tx.bankName.lowercase(Locale.US).contains(q) ||
        tx.note.lowercase(Locale.US).contains(q) ||
        tx.date.contains(q) ||
        tx.accountLast4.contains(q) ||
        tx.referenceNumber.lowercase(Locale.US).contains(q) ||
        tx.upiId.lowercase(Locale.US).contains(q) ||
        tx.smsSender.lowercase(Locale.US).contains(q) ||
        tx.rawSms.lowercase(Locale.US).contains(q) ||
        categoryName.lowercase(Locale.US).contains(q) ||
        tx.type.name.lowercase(Locale.US).contains(q) ||
        amountPlain.contains(qDigits) ||
        amountInt.contains(qDigits) ||
        amountFormatted.contains(qDigits) ||
        (qDigits.isNotEmpty() && qDigits.all { it.isDigit() || it == '.' } &&
            (amountPlain.startsWith(qDigits) || amountInt.startsWith(qDigits)))
}

/**
 * Period filter bottom sheet — few primary choices, compact more list, clear custom range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodFilterSheet(
    visible: Boolean,
    selectedFilter: String,
    customStartDate: String,
    customEndDate: String,
    onDismiss: () -> Unit,
    onFilterSelect: (String) -> Unit,
    onCustomRangeChange: (String, String) -> Unit
) {
    if (!visible) return

    val quickOptions = listOf("This Month", "This Week", "Today", "All")
    val moreOptions = listOf("Yesterday", "3 Months", "6 Months", "1 Year")
    val isCustom = selectedFilter.equals("Custom", ignoreCase = true)
    val moreSelected = moreOptions.any { it.equals(selectedFilter, ignoreCase = true) }

    var startIso by remember(customStartDate) { mutableStateOf(customStartDate) }
    var endIso by remember(customEndDate) { mutableStateOf(customEndDate) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var moreExpanded by remember(selectedFilter) { mutableStateOf(moreSelected) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    fun selectAndClose(option: String) {
        onFilterSelect(option)
        if (!option.equals("Custom", ignoreCase = true)) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Period",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = scheme.onSurface
                )
                Text(
                    selectedFilter.let { cur ->
                        if (cur.equals("Custom", true) && startIso.isNotBlank() && endIso.isNotBlank()) {
                            "Custom · $startIso → $endIso"
                        } else cur
                    },
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant
                )
            }

            // Primary: 2×2 large tiles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickOptions.take(2).forEach { option ->
                        PeriodQuickTile(
                            label = option,
                            selected = option.equals(selectedFilter, ignoreCase = true),
                            onClick = { selectAndClose(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickOptions.drop(2).forEach { option ->
                        PeriodQuickTile(
                            label = if (option == "All") "All time" else option,
                            selected = option.equals(selectedFilter, ignoreCase = true),
                            onClick = { selectAndClose(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))

            // More periods — collapsed by default
            Surface(
                onClick = { moreExpanded = !moreExpanded },
                shape = RoundedCornerShape(14.dp),
                color = scheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "More periods",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = scheme.onSurface
                        )
                        if (moreSelected) {
                            Text(
                                selectedFilter,
                                fontSize = 12.sp,
                                color = scheme.primary
                            )
                        }
                    }
                    Text(
                        if (moreExpanded) "Hide" else "Show",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = moreExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    moreOptions.forEach { option ->
                        PeriodRadioRow(
                            label = when (option) {
                                "3 Months" -> "Last 3 months"
                                "6 Months" -> "Last 6 months"
                                "1 Year" -> "Last year"
                                else -> option
                            },
                            selected = option.equals(selectedFilter, ignoreCase = true),
                            onClick = { selectAndClose(option) }
                        )
                    }
                }
            }

            // Custom range — one clear entry
            Surface(
                onClick = {
                    onFilterSelect("Custom")
                    if (startIso.isBlank()) pickingStart = true
                },
                shape = RoundedCornerShape(14.dp),
                color = if (isCustom) scheme.primary.copy(alpha = 0.12f) else scheme.surfaceVariant.copy(alpha = 0.45f),
                border = if (isCustom) BorderStroke(1.5.dp, scheme.primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Custom date range",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = scheme.onSurface
                    )
                    if (isCustom) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PeriodDateChip(
                                label = "From",
                                value = startIso.ifBlank { "Pick" },
                                onClick = { pickingStart = true },
                                modifier = Modifier.weight(1f)
                            )
                            PeriodDateChip(
                                label = "To",
                                value = endIso.ifBlank { "Pick" },
                                onClick = { pickingEnd = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (startIso.isNotBlank() && endIso.isNotBlank()) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Apply", color = scheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            "Pick start and end dates",
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (pickingStart) {
        IsoDatePickerDialog(
            title = "From date",
            initialIso = startIso,
            onDismiss = { pickingStart = false },
            onConfirm = { iso ->
                startIso = iso
                pickingStart = false
                if (endIso.isBlank()) endIso = iso
                onCustomRangeChange(iso, endIso.ifBlank { iso })
                onFilterSelect("Custom")
                pickingEnd = endIso.isBlank()
            }
        )
    }
    if (pickingEnd) {
        IsoDatePickerDialog(
            title = "To date",
            initialIso = endIso.ifBlank { startIso },
            onDismiss = { pickingEnd = false },
            onConfirm = { iso ->
                endIso = iso
                pickingEnd = false
                val start = startIso.ifBlank { iso }
                if (startIso.isBlank()) startIso = iso
                onCustomRangeChange(startIso.ifBlank { start }, iso)
                onFilterSelect("Custom")
            }
        )
    }
}

@Composable
private fun PeriodQuickTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.height(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (selected) scheme.onPrimary else scheme.onSurface
            )
        }
    }
}

@Composable
private fun PeriodRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) scheme.primary.copy(alpha = 0.10f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = scheme.primary,
                    unselectedColor = scheme.onSurfaceVariant
                )
            )
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = scheme.onSurface
            )
        }
    }
}

@Composable
private fun PeriodDateChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.4f)),
        modifier = modifier.height(52.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 10.sp, color = scheme.onSurfaceVariant)
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IsoDatePickerDialog(
    title: String,
    initialIso: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialMillis = remember(initialIso) { isoToUtcMillis(initialIso) ?: System.currentTimeMillis() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    onConfirm(utcMillisToIso(millis))
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state, title = { Text(title, modifier = Modifier.padding(16.dp)) })
    }
}

private fun isoToUtcMillis(iso: String): Long? {
    if (iso.isBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        sdf.parse(iso)?.time
    } catch (_: Exception) {
        null
    }
}

private fun utcMillisToIso(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(millis))
}
