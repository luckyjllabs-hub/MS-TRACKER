package com.jllabs.moneylens.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FilterInk = Color(0xFF1B241C)
private val FilterSurface = Color(0xFFFFFFFF)
private val FilterSelected = Color(0xFF2E6244)
private val FilterBorder = Color(0xFF8A9488)
private val FilterMuted = Color(0xFF4A5248)

/**
 * High-contrast single-select period control (replaces hard-to-see chip rows).
 */
@Composable
fun PeriodDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val ink = scheme.onSurface
    val surface = scheme.surface
    val muted = scheme.onSurfaceVariant
    val selectedColor = Color(0xFF3B7A57)
    val border = scheme.outline.copy(alpha = 0.6f)

    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = muted)
        Spacer(modifier = Modifier.size(4.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(14.dp),
                color = surface,
                border = BorderStroke(1.5.dp, border),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = selectedColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selected, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ink)
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ink)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = surface
            ) {
                options.forEach { option ->
                    val isOn = option.equals(selected, ignoreCase = true)
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOn) selectedColor else ink
                            )
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        trailingIcon = {
                            if (isOn) Icon(Icons.Default.Check, contentDescription = null, tint = selectedColor)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Multi-select filter dropdown with clear visual checkmarks.
 * Empty [selected] means "All" (no filter applied).
 */
@Composable
fun MultiSelectDropdown(
    label: String,
    options: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    allLabel: String = "All",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val summary = when {
        selected.isEmpty() -> allLabel
        selected.size == 1 -> selected.first()
        else -> "${selected.size} selected"
    }

    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FilterMuted)
        Spacer(modifier = Modifier.size(4.dp))
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = if (selected.isEmpty()) FilterSurface else Color(0xFFE8F5E9),
            border = BorderStroke(1.5.dp, if (selected.isEmpty()) FilterBorder else FilterSelected),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = FilterSelected, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(summary, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FilterInk, maxLines = 1)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FilterInk)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = FilterSurface,
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        allLabel,
                        fontWeight = if (selected.isEmpty()) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected.isEmpty()) FilterSelected else FilterInk
                    )
                },
                onClick = { onSelectedChange(emptySet()) },
                trailingIcon = {
                    if (selected.isEmpty()) Icon(Icons.Default.Check, null, tint = FilterSelected)
                }
            )
            HorizontalDivider()
            Column(modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    val isOn = option in selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Medium,
                                color = FilterInk
                            )
                        },
                        onClick = {
                            onSelectedChange(
                                if (isOn) selected - option else selected + option
                            )
                        },
                        trailingIcon = {
                            if (isOn) Icon(Icons.Default.Check, null, tint = FilterSelected)
                        }
                    )
                }
            }
            if (selected.isNotEmpty()) {
                HorizontalDivider()
                TextButton(
                    onClick = { onSelectedChange(emptySet()) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Clear selection", color = FilterSelected, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Compact high-contrast segmented control for All / Credit / Debit.
 */
@Composable
fun TypeSegmentedControl(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val track = scheme.surfaceVariant
    val selectedBg = Color(0xFF3B7A57)
    val unselectedText = scheme.onSurfaceVariant
    val border = scheme.outline.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = track,
        border = BorderStroke(1.dp, border),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val isOn = option.equals(selected, ignoreCase = true)
                Surface(
                    onClick = { onSelected(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOn) selectedBg else Color.Transparent,
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isOn) Color.White else unselectedText
                    )
                }
            }
        }
    }
}
