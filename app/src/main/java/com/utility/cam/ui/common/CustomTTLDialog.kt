package com.utility.cam.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.utility.cam.R

enum class TimeUnit(val hoursMultiplier: Int) {
    HOURS(1),
    DAYS(24),
    WEEKS(168)
}

/**
 * Dialog for Pro users to set custom TTL (Time To Live) in hours, days, or weeks
 */
@Composable
fun CustomTTLDialog(
    onDismiss: () -> Unit,
    onConfirm: (hours: Int) -> Unit,
    initialValue: String = "24",
    initialUnit: TimeUnit = TimeUnit.HOURS
) {
    var customValue by remember { mutableStateOf(initialValue) }
    var selectedUnit by remember { mutableStateOf(initialUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.capture_review_custom_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.capture_review_custom_dialog_message))
                Spacer(modifier = Modifier.height(16.dp))

                // Unit selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedUnit == TimeUnit.HOURS,
                        onClick = { selectedUnit = TimeUnit.HOURS },
                        label = { Text(stringResource(R.string.capture_review_unit_hours)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedUnit == TimeUnit.DAYS,
                        onClick = { selectedUnit = TimeUnit.DAYS },
                        label = { Text(stringResource(R.string.capture_review_unit_days)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedUnit == TimeUnit.WEEKS,
                        onClick = { selectedUnit = TimeUnit.WEEKS },
                        label = { Text(stringResource(R.string.capture_review_unit_weeks)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Value input
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { value ->
                        // Only allow digits
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            customValue = value
                        }
                    },
                    label = {
                        Text(
                            when (selectedUnit) {
                                TimeUnit.HOURS -> stringResource(R.string.capture_review_custom_hours_label)
                                TimeUnit.DAYS -> stringResource(R.string.capture_review_custom_days_label)
                                TimeUnit.WEEKS -> stringResource(R.string.capture_review_custom_weeks_label)
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = customValue.toIntOrNull()
                    if (value != null && value > 0) {
                        val totalHours = value * selectedUnit.hoursMultiplier
                        onConfirm(totalHours)
                    }
                }
            ) {
                Text(stringResource(R.string.capture_review_custom_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.capture_review_custom_dialog_cancel))
            }
        }
    )
}
