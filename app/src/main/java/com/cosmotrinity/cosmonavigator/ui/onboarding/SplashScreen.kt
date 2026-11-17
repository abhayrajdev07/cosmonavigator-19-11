
package com.cosmotrinity.cosmonavigator.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmotrinity.cosmonavigator.R
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicAccent
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicBackground
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicPrimary
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicTextPrimary
import com.cosmotrinity.cosmonavigator.ui.theme.CosmicTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {

    val logoScale = remember {
        Animatable(0.60f)
    }

    val logoAlpha = remember {
        Animatable(0f)
    }

    val textAlpha = remember {
        Animatable(0f)
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "SplashAnimation"
    )

    // Animated rotating gradient border
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            )
        ),
        label = "BorderRotation"
    )

    // Logo pulse animation
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoPulse"
    )

    LaunchedEffect(Unit) {

        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )

        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )

        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )

        delay(1500)

        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Animated logo container
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .scale(pulse),
                contentAlignment = Alignment.Center
            ) {

                // Rotating gradient border
                Box(
                    modifier = Modifier
                        .size(195.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                            shadowElevation = 30f
                        }
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFF8C00),
                                    Color(0xFFFFC107),
                                    Color(0xFF2563EB),
                                    Color(0xFF38BDF8),
                                    Color(0xFFFF8C00)
                                )
                            ),
                            shape = RoundedCornerShape(42.dp)
                        )
                )

                // Inner background
                Box(
                    modifier = Modifier
                        .size(185.dp)
                        .background(
                            color = CosmicBackground,
                            shape = RoundedCornerShape(38.dp)
                        )
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {

                    // CosmoNavigator logo
                    Image(
                        painter = painterResource(
                            id = R.drawable.cosmonavigator_logo
                        ),
                        contentDescription = "CosmoNavigator Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(logoScale.value)
                            .alpha(logoAlpha.value)
                            .clip(RoundedCornerShape(32.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "CosmoNavigator",
                color = CosmicTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NAVIGATE YOUR PREPARATION",
                color = CosmicAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Plan • Focus • Prepare • Succeed",
                color = CosmicTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }

        // Developer credit
        Text(
            text = "Made with focus by Abhayraj Patel",
            color = CosmicTextSecondary.copy(alpha = 0.65f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(textAlpha.value)
        )
    }
}
