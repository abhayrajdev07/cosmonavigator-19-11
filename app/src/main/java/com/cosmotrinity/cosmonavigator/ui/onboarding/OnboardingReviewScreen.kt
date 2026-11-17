
package com.cosmotrinity.cosmonavigator.ui.onboarding

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OnboardingReviewScreen(
    profile: AspirantProfileInput,
    goals: AspirantGoalsInput,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onEditGoals: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    var profileBitmap by remember {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(profile.profileImageUri) {
        val uri = profile.profileImageUri

        profileBitmap = if (uri != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver
                        .openInputStream(Uri.parse(uri))
                        ?.use {
                            BitmapFactory.decodeStream(it)
                        }
                }.getOrNull()
            }
        } else {
            null
        }
    }

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
                    text = "FINAL STEP",
                    color = CosmicAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .background(CosmicPrimary, CircleShape)
                    )
                }
            }
        }

        // REVIEW CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 18.dp, bottom = 28.dp)
        ) {

            Text(
                text = "Review your profile",
                color = CosmicTextPrimary,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Check your details before starting your UPSC preparation journey with CosmoNavigator.",
                color = CosmicTextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PROFILE CARD
            ReviewCard(
                title = "Personal Profile",
                icon = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = CosmicSecondary
                    )
                },
                onEdit = onEditProfile
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(
                                2.dp,
                                CosmicAccent,
                                CircleShape
                            )
                            .clip(CircleShape)
                            .background(CosmicSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap!!.asImageBitmap(),
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = CosmicTextSecondary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Column {
                        Text(
                            text = profile.fullName,
                            color = CosmicTextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = profile.language,
                            color = CosmicSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                ReviewItem(
                    "Preparation Stage",
                    profile.preparationStage
                )

                ReviewItem(
                    "UPSC Attempt",
                    profile.attemptNumber?.toString() ?: "Not specified"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // UPSC GOALS CARD
            ReviewCard(
                title = "UPSC Preparation Goals",
                icon = {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = CosmicSecondary
                    )
                },
                onEdit = onEditGoals
            ) {
                ReviewItem(
                    "Target Examination",
                    "UPSC CSE ${goals.targetExamYear}"
                )

                ReviewItem(
                    "Prelims Date",
                    goals.prelimsDate.ifBlank { "Not announced / Not specified" }
                )

                ReviewItem(
                    "Optional Subject",
                    goals.optionalSubject
                )

                ReviewItem(
                    "Mains Study Medium",
                    goals.studyMedium
                )

                ReviewItem(
                    "Daily Study Target",
                    "${goals.dailyStudyHours} hours"
                )

                ReviewItem(
                    "Preparation Start Date",
                    goals.preparationStartDate.ifBlank { "Not specified" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // COMPLETION MESSAGE
            Surface(
                color = CosmicSurface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = CosmicAccent,
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Your journey starts here!",
                        color = CosmicTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Stay consistent, track your progress and work towards your UPSC goal every day.",
                        color = CosmicTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // COMPLETE SETUP
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary
                )
            ) {
                Text(
                    text = "Complete Setup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(10.dp))

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You can edit these details later from your profile.",
                color = CosmicTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ReviewCard(
    title: String,
    icon: @Composable () -> Unit,
    onEdit: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        color = CosmicSurface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = title,
                    color = CosmicTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = CosmicSecondary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.size(4.dp))

                    Text(
                        text = "Edit",
                        color = CosmicSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            content()
        }
    }
}

@Composable
private fun ReviewItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Text(
            text = label,
            color = CosmicTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = CosmicTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
