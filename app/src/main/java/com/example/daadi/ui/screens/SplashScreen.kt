package com.example.daadi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateHome: () -> Unit) {
    // Elegant pulsing exit animation of splash
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            )
        )
        onNavigateHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF3E3)), // Warm off-white sandal background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scale.value)
        ) {
            // Elegant Sandstone Mandala ornament
            Text(
                "ॐ",
                fontSize = 58.sp,
                color = Color(0xFFD4A55A),
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                "DAADI",
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFF5C2D0A), // Rich rosewood text
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Navakankari • Nine Men's Morris",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF8B5E3C), // Sand sandstone brown
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFFD4A55A),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Classic Strategy Restored",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                letterSpacing = 2.sp
            )
        }
    }
}
