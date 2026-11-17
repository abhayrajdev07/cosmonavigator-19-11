
package com.cosmotrinity.cosmonavigator.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.R
import com.cosmotrinity.cosmonavigator.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Logo and branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.cosmonavigator_logo
                    ),
                    contentDescription = "CosmoNavigator Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = CosmicAccent,
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CosmoNavigator",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )

                Text(
                    text = "NAVIGATE YOUR PREPARATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = CosmicAccent
                )
            }

            // Introduction
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your UPSC journey starts here.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "One place to plan, focus and track your preparation.",
                    fontSize = 13.sp,
                    color = CosmicTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }

            // Features
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WelcomeFeature(
                    icon = {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = CosmicSecondary
                        )
                    },
                    title = "Plan your preparation",
                    description = "Organise subjects and daily targets."
                )

                WelcomeFeature(
                    icon = {
                        Icon(
                            Icons.Filled.TrackChanges,
                            contentDescription = null,
                            tint = CosmicAccent
                        )
                    },
                    title = "Stay consistent",
                    description = "Track lectures, revision and study streaks."
                )

                WelcomeFeature(
                    icon = {
                        Icon(
                            Icons.Filled.Insights,
                            contentDescription = null,
                            tint = CosmicPrimary
                        )
                    },
                    title = "Measure your progress",
                    description = "Understand your performance and improve."
                )
            }

            // Bottom action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary,
                        contentColor = CosmicTextPrimary
                    )
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "→",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(9.dp))

                Text(
                    text = "Your preparation. Your discipline. Your journey.",
                    fontSize = 10.sp,
                    color = CosmicTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeFeature(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = CosmicSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        CosmicSurface,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicTextPrimary
                )

                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = CosmicTextSecondary
                )
            }
        }
    }
}
