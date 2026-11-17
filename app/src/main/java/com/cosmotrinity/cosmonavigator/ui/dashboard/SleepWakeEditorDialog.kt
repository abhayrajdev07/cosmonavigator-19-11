
package com.cosmotrinity.cosmonavigator.ui.dashboard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.data.DailySleepRecordRepository
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SleepWakeEditorDialog(
    repository: DailySleepRecordRepository,
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zoneId = remember { ZoneId.systemDefault() }

    val accent = Color(0xFF9277FF)
    val wakeColor = Color(0xFFE5A943)
    val bedColor = Color(0xFF8995EA)

    // Selected date always represents the wake-up date.
    var selectedDate by remember { mutableStateOf(initialDate) }

    var sleepTime by remember { mutableStateOf(LocalTime.of(22, 0)) }
    var wakeUpTime by remember { mutableStateOf(LocalTime.of(6, 0)) }

    var hasSavedBedtime by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    }

    val fullDateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault())
    }

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    }

    // Determine whether bedtime belongs to the selected date
    // or the previous date.
    val bedtimeDate = if (sleepTime.isBefore(wakeUpTime)) {
        selectedDate
    } else {
        selectedDate.minusDays(1)
    }

    val bedtimeDateTime = bedtimeDate.atTime(sleepTime)
    val wakeDateTime = selectedDate.atTime(wakeUpTime)

    val durationMinutes = Duration.between(
        bedtimeDateTime,
        wakeDateTime
    ).toMinutes()

    val validDuration = durationMinutes in 1..1440

    val durationText = if (hasSavedBedtime && validDuration) {
        "${durationMinutes / 60}h ${durationMinutes % 60}m"
    } else {
        ""
    }

    // Load the record for the selected wake-up date.
    LaunchedEffect(selectedDate) {
        isLoading = true
        errorMessage = null
        hasSavedBedtime = false

        try {
            val record = repository.getRecord(selectedDate)

            if (record != null) {
                record.actualSleepAt?.let { timestamp ->
                    val savedBedtime = Instant.ofEpochMilli(timestamp)
                        .atZone(zoneId)

                    val savedBedtimeDate = savedBedtime.toLocalDate()

                    // Accept bedtime on either the selected date
                    // or the previous calendar date.
                    if (
                        savedBedtimeDate == selectedDate ||
                        savedBedtimeDate == selectedDate.minusDays(1)
                    ) {
                        sleepTime = savedBedtime.toLocalTime()
                        hasSavedBedtime = true
                    }
                }

                record.actualWakeUpAt?.let { timestamp ->
                    val savedWakeUp = Instant.ofEpochMilli(timestamp)
                        .atZone(zoneId)

                    if (savedWakeUp.toLocalDate() == selectedDate) {
                        wakeUpTime = savedWakeUp.toLocalTime()
                    }
                }
            } else {
                sleepTime = LocalTime.of(22, 0)
                wakeUpTime = LocalTime.of(6, 0)
            }
        } catch (exception: Exception) {
            errorMessage = exception.message
                ?: "Unable to load this date."
        } finally {
            isLoading = false
        }
    }

    fun showTimePicker(
        currentTime: LocalTime,
        onTimeSelected: (LocalTime) -> Unit
    ) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onTimeSelected(LocalTime.of(hour, minute))
            },
            currentTime.hour,
            currentTime.minute,
            false
        ).show()
    }

    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),

        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Sleep & Wake",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sleep time → wake-up time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(
                        1.dp,
                        accent.copy(alpha = 0.55f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accent
                    )
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.size(8.dp))

                    Text(
                        "Wake-up date: ${selectedDate.format(fullDateFormatter)}",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeSelectionCard(
                        modifier = Modifier.weight(1f),
                        label = "Wake-up",
                        dateLabel = selectedDate.format(dateFormatter),
                        time = wakeUpTime.format(timeFormatter),
                        accent = wakeColor,
                        icon = {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = "Wake-up",
                                tint = wakeColor,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        onClick = {
                            showTimePicker(wakeUpTime) {
                                wakeUpTime = it
                            }
                        }
                    )

                    TimeSelectionCard(
                        modifier = Modifier.weight(1f),
                        label = "Bedtime",
                        dateLabel = bedtimeDate.format(dateFormatter),
                        time = sleepTime.format(timeFormatter),
                        accent = bedColor,
                        icon = {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = "Bedtime",
                                tint = bedColor,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        onClick = {
                            showTimePicker(sleepTime) {
                                sleepTime = it
                                hasSavedBedtime = true
                            }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accent.copy(alpha = 0.13f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 15.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = accent.copy(alpha = 0.18f)
                        ) {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "TOTAL SLEEP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = durationText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },

        confirmButton = {
            Button(
                enabled = !isLoading &&
                        !isSaving &&
                        hasSavedBedtime &&
                        validDuration,

                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null

                        try {
                            repository.saveRecordForDate(
                                date = selectedDate,
                                sleepTime = sleepTime,
                                wakeUpTime = wakeUpTime
                            )

                            onSaved()
                            onDismiss()
                        } catch (exception: Exception) {
                            errorMessage = exception.message
                                ?: "Unable to save the sleep record."
                        } finally {
                            isSaving = false
                        }
                    }
                },

                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        "Save",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },

        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeSelectionCard(
    modifier: Modifier = Modifier,
    label: String,
    dateLabel: String,
    time: String,
    accent: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                icon()

                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                time,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )

            Text(
                dateLabel,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
