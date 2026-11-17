
package com.cosmotrinity.cosmonavigator.ui.onboarding

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicAccent
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicBackground
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicPrimary
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicSurface
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicSurfaceVariant
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicTextPrimary
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AspirantProfileInput(
    val fullName: String,
    val language: String,
    val preparationStage: String,
    val attemptNumber: Int?,
    val profileImageUri: String? = null
)

@Composable
fun ProfileSetupScreen(
    initialData: AspirantProfileInput? = null,
    onBack: () -> Unit,
    onContinue: (AspirantProfileInput) -> Unit
) {
    var fullName by remember(initialData) {
        mutableStateOf(initialData?.fullName ?: "")
    }

    var language by remember(initialData) {
        mutableStateOf(initialData?.language ?: "English")
    }

    var preparationStage by remember(initialData) {
        mutableStateOf(
            initialData?.preparationStage ?: "Foundation Building"
        )
    }

    var attemptNumber by remember(initialData) {
        mutableStateOf(initialData?.attemptNumber?.toString() ?: "")
    }

    var profileImageUri by remember(initialData) {
        mutableStateOf(
            initialData?.profileImageUri?.let { Uri.parse(it) }
        )
    }

    var languageExpanded by remember { mutableStateOf(false) }
    var stageExpanded by remember { mutableStateOf(false) }

    val languages = listOf("English", "Hindi", "Both")

    val stages = listOf(
        "Beginner",
        "Foundation Building",
        "Syllabus Completion",
        "Revision",
        "Test Series / Exam Practice"
    )

    val context = LocalContext.current

    var profileBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        profileImageUri = uri
    }

    LaunchedEffect(profileImageUri) {
        val uri = profileImageUri

        profileBitmap = if (uri != null) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } else {
            null
        }
    }

    val nameValid = fullName.trim().isNotEmpty()

    val attemptValid = attemptNumber.isBlank() ||
            (attemptNumber.toIntOrNull() != null &&
                    attemptNumber.toInt() in 1..20)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .systemBarsPadding()
            .imePadding()
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
                    text = "STEP 1 OF 2",
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(CosmicPrimary, CircleShape)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(CosmicSurfaceVariant, CircleShape)
                )
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
                text = "Let's get to know you",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set up your aspirant profile to make CosmoNavigator your personal UPSC preparation companion.",
                fontSize = 14.sp,
                color = CosmicTextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // CIRCULAR PROFILE PHOTO
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .border(
                                    width = 2.dp,
                                    color = CosmicAccent,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .background(CosmicSurfaceVariant)
                                .clickable {
                                    imagePicker.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileBitmap != null) {
                                Image(
                                    bitmap = profileBitmap!!.asImageBitmap(),
                                    contentDescription = "Selected profile photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile placeholder",
                                    tint = CosmicTextSecondary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        // CAMERA BUTTON
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(38.dp)
                                .border(
                                    width = 3.dp,
                                    color = CosmicBackground,
                                    shape = CircleShape
                                )
                                .background(CosmicPrimary, CircleShape)
                                .clickable {
                                    imagePicker.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Change profile photo",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (profileBitmap != null) {
                            "Change profile photo"
                        } else {
                            "Add profile photo (Optional)"
                        },
                        color = CosmicTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // FULL NAME
            FormLabel("Full name *")

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Enter your full name")
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = profileFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // LANGUAGE
            FormLabel("Preferred language")

            ProfileDropdown(
                value = language,
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = it },
                options = languages,
                onSelected = { language = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PREPARATION STAGE
            FormLabel("Current preparation stage")

            ProfileDropdown(
                value = preparationStage,
                expanded = stageExpanded,
                onExpandedChange = { stageExpanded = it },
                options = stages,
                onSelected = { preparationStage = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ATTEMPT NUMBER
            FormLabel("UPSC attempt number (Optional)")

            OutlinedTextField(
                value = attemptNumber,
                onValueChange = {
                    if (it.length <= 2 && it.all(Char::isDigit)) {
                        attemptNumber = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("e.g. 1")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(14.dp),
                colors = profileFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You can leave this blank and update it later.",
                color = CosmicTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // CONTINUE
            Button(
                onClick = {
                    if (nameValid && attemptValid) {
                        onContinue(
                            AspirantProfileInput(
                                fullName = fullName.trim(),
                                language = language,
                                preparationStage = preparationStage,
                                attemptNumber = attemptNumber.toIntOrNull(),
                                profileImageUri = profileImageUri?.toString()
                            )
                        )
                    }
                },
                enabled = nameValid && attemptValid,
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
                    text = "Continue",
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
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        color = CosmicTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ProfileDropdown(
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

@Composable
private fun profileFieldColors() =
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
