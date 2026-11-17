
package com.cosmotrinity.cosmonavigator.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AspirantGoalsInput(
    val targetExamYear: Int,
    val prelimsDate: String,
    val optionalSubject: String,
    val studyMedium: String,
    val dailyStudyHours: Int,
    val preparationStartDate: String
)

private enum class GoalDateField {
    PRELIMS,
    PREPARATION_START
}

private val displayDateFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UPSCGoalsScreen(
    initialData: AspirantGoalsInput? = null,
    onBack: () -> Unit,
    onFinish: (AspirantGoalsInput) -> Unit
) {
    var targetYear by remember(initialData) {
        mutableStateOf(
            (initialData?.targetExamYear ?: 2027).toString()
        )
    }

    var optionalSubject by remember(initialData) {
        mutableStateOf(initialData?.optionalSubject ?: "Sociology")
    }

    var studyMedium by remember(initialData) {
        mutableStateOf(initialData?.studyMedium ?: "English")
    }

    var dailyStudyHours by remember(initialData) {
        mutableStateOf("${initialData?.dailyStudyHours ?: 8} hours")
    }

    var prelimsDate by remember(initialData) {
        mutableStateOf(initialData?.prelimsDate ?: "23-05-2027")
    }

    var preparationStartDate by remember(initialData) {
        mutableStateOf(initialData?.preparationStartDate ?: "08-06-2026")
    }

    var yearExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var mediumExpanded by remember { mutableStateOf(false) }
    var hoursExpanded by remember { mutableStateOf(false) }

    var activeDatePicker by remember {
        mutableStateOf<GoalDateField?>(null)
    }

    val years = (2027..2035).map { it.toString() }

    val subjects = listOf(
        "Not decided yet",
        "Agriculture",
        "Animal Husbandry and Veterinary Science",
        "Anthropology",
        "Botany",
        "Chemistry",
        "Civil Engineering",
        "Commerce and Accountancy",
        "Economics",
        "Electrical Engineering",
        "Geography",
        "Geology",
        "History",
        "Law",
        "Management",
        "Mathematics",
        "Mechanical Engineering",
        "Medical Science",
        "Philosophy",
        "Physics",
        "Political Science and International Relations",
        "Psychology",
        "Public Administration",
        "Sociology",
        "Statistics",
        "Zoology",
        "Literature"
    )

    val mediums = listOf("English", "Hindi", "Both")

    val studyHours = listOf(
        "4 hours",
        "6 hours",
        "8 hours",
        "10 hours",
        "12 hours",
        "14 hours"
    )

    val yearValid = targetYear.toIntOrNull() != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .systemBarsPadding()
    ) {

        // FIXED HEADER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = CosmicSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = CosmicTextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "STEP 2 OF 2",
                    color = CosmicAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .background(CosmicPrimary, CircleShape)
                    )
                }
            }
        }

        // SCROLLABLE FORM
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 18.dp, bottom = 28.dp)
        ) {
            Text(
                text = "Define your UPSC goals",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tell us about your examination target and study preferences. You can update these details later.",
                fontSize = 14.sp,
                color = CosmicTextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // TARGET EXAM YEAR
            GoalLabel("Target UPSC examination year *")

            GoalDropdown(
                value = targetYear,
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it },
                options = years,
                onSelected = { targetYear = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PRELIMS DATE
            GoalLabel("Prelims examination date (Optional)")

            GoalDateSelector(
                value = prelimsDate,
                placeholder = "DD-MM-YYYY",
                onClick = {
                    activeDatePicker = GoalDateField.PRELIMS
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Leave blank if the official date is not announced.",
                color = CosmicTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // OPTIONAL SUBJECT
            GoalLabel("UPSC optional subject")

            GoalDropdown(
                value = optionalSubject,
                expanded = subjectExpanded,
                onExpandedChange = { subjectExpanded = it },
                options = subjects,
                onSelected = { optionalSubject = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // STUDY MEDIUM
            GoalLabel("Mains study medium")

            GoalDropdown(
                value = studyMedium,
                expanded = mediumExpanded,
                onExpandedChange = { mediumExpanded = it },
                options = mediums,
                onSelected = { studyMedium = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // DAILY STUDY TARGET
            GoalLabel("Daily study target")

            GoalDropdown(
                value = dailyStudyHours,
                expanded = hoursExpanded,
                onExpandedChange = { hoursExpanded = it },
                options = studyHours,
                onSelected = { dailyStudyHours = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PREPARATION START DATE
            GoalLabel("Preparation start date (Optional)")

            GoalDateSelector(
                value = preparationStartDate,
                placeholder = "DD-MM-YYYY",
                onClick = {
                    activeDatePicker = GoalDateField.PREPARATION_START
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This helps calculate your preparation duration.",
                color = CosmicTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // CONTINUE BUTTON
            Button(
                onClick = {
                    val year = targetYear.toIntOrNull()

                    if (year != null) {
                        onFinish(
                            AspirantGoalsInput(
                                targetExamYear = year,
                                prelimsDate = prelimsDate,
                                optionalSubject = optionalSubject,
                                studyMedium = studyMedium,
                                dailyStudyHours = dailyStudyHours
                                    .substringBefore(" ")
                                    .toInt(),
                                preparationStartDate = preparationStartDate
                            )
                        )
                    }
                },
                enabled = yearValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary,
                    disabledContainerColor = CosmicSurfaceVariant
                )
            ) {
                Text(
                    text = "Continue to Review",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(10.dp))

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    // CALENDAR DIALOG
    if (activeDatePicker != null) {

        val selectedDateText = when (activeDatePicker) {
            GoalDateField.PRELIMS -> prelimsDate
            GoalDateField.PREPARATION_START -> preparationStartDate
            null -> ""
        }

        val initialMillis = remember(selectedDateText) {
            selectedDateText.toDateMillis()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = {
                activeDatePicker = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis =
                            datePickerState.selectedDateMillis

                        if (selectedMillis != null) {
                            val selectedDate = Instant
                                .ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .format(displayDateFormatter)

                            when (activeDatePicker) {
                                GoalDateField.PRELIMS ->
                                    prelimsDate = selectedDate

                                GoalDateField.PREPARATION_START ->
                                    preparationStartDate = selectedDate

                                null -> Unit
                            }
                        }

                        activeDatePicker = null
                    }
                ) {
                    Text(
                        text = "Select",
                        color = CosmicPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        activeDatePicker = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = CosmicTextSecondary
                    )
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// DATE SELECTOR FIELD
@Composable
private fun GoalDateSelector(
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            readOnly = true,
            enabled = false,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    color = CosmicTextSecondary
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = "Choose date",
                    tint = CosmicSecondary
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = CosmicTextPrimary,
                disabledPlaceholderColor = CosmicTextSecondary,
                disabledBorderColor = CosmicSurfaceVariant,
                disabledContainerColor = CosmicSurface,
                disabledTrailingIconColor = CosmicSecondary
            )
        )
    }
}

// FORM LABEL
@Composable
private fun GoalLabel(text: String) {
    Text(
        text = text,
        color = CosmicTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// DROPDOWN
@Composable
private fun GoalDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onExpandedChange(true)
                },
            readOnly = true,
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            trailingIcon = {
                Text(
                    text = if (expanded) "⌃" else "⌄",
                    color = CosmicTextSecondary,
                    fontSize = 20.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = CosmicTextPrimary,
                disabledBorderColor = CosmicSurfaceVariant,
                disabledContainerColor = CosmicSurface,
                disabledTrailingIconColor = CosmicTextSecondary
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
            },
            modifier = Modifier.background(CosmicSurface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = CosmicTextPrimary
                        )
                    },
                    onClick = {
                        onSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// CONVERT DD-MM-YYYY TO CALENDAR TIMESTAMP
private fun String.toDateMillis(): Long? {
    if (isBlank()) return null

    return runCatching {
        LocalDate.parse(this, displayDateFormatter)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

// FIELD COLORS
@Composable
private fun goalFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = CosmicTextPrimary,
        unfocusedTextColor = CosmicTextPrimary,
        focusedBorderColor = CosmicPrimary,
        unfocusedBorderColor = CosmicSurfaceVariant,
        focusedPlaceholderColor = CosmicTextSecondary,
        unfocusedPlaceholderColor = CosmicTextSecondary,
        cursorColor = CosmicAccent,
        focusedContainerColor = CosmicSurface,
        unfocusedContainerColor = CosmicSurface
    )
