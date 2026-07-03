package com.example.daadi.ui.screens.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseManager

object AdminDesign {
    // We will compute these dynamically in a theme wrapper or just rely on MaterialTheme colors mostly
    // But to keep backward compatibility with screens that use AdminDesign.Primary directly, we define them as vars
    var Primary = Color(0xFF6366F1) // Indigo-500 (Stripe/Linear vibe)
    var Secondary = Color(0xFF10B981) // Emerald-500
    var Tertiary = Color(0xFFF59E0B) // Amber-500
    var Background = Color(0xFFF9FAFB) // Gray-50
    var Surface = Color(0xFFFFFFFF)
    var OnSurface = Color(0xFF111827) // Gray-900
    var OnSurfaceVariant = Color(0xFF6B7280) // Gray-500
    var Error = Color(0xFFEF4444) // Red-500
    var Warning = Color(0xFFF59E0B)
    var Success = Color(0xFF10B981)
    
    var CardElevation = 0.dp // Modern flat design uses borders or very subtle shadows
    val CardShape = RoundedCornerShape(12.dp)
    val ButtonShape = RoundedCornerShape(8.dp)
    val InputShape = RoundedCornerShape(8.dp)
    
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingExtraLarge = 32.dp
}

@Composable
fun AdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    if (darkTheme) {
        AdminDesign.Primary = Color(0xFF818CF8) // Indigo-400
        AdminDesign.Secondary = Color(0xFF34D399) // Emerald-400
        AdminDesign.Tertiary = Color(0xFFFBBF24) // Amber-400
        AdminDesign.Background = Color(0xFF111827) // Gray-900
        AdminDesign.Surface = Color(0xFF1F2937) // Gray-800
        AdminDesign.OnSurface = Color(0xFFF9FAFB) // Gray-50
        AdminDesign.OnSurfaceVariant = Color(0xFF9CA3AF) // Gray-400
        AdminDesign.Error = Color(0xFFF87171) // Red-400
    } else {
        AdminDesign.Primary = Color(0xFF6366F1)
        AdminDesign.Secondary = Color(0xFF10B981)
        AdminDesign.Tertiary = Color(0xFFF59E0B)
        AdminDesign.Background = Color(0xFFF9FAFB)
        AdminDesign.Surface = Color(0xFFFFFFFF)
        AdminDesign.OnSurface = Color(0xFF111827)
        AdminDesign.OnSurfaceVariant = Color(0xFF6B7280)
        AdminDesign.Error = Color(0xFFEF4444)
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme(
            primary = AdminDesign.Primary,
            secondary = AdminDesign.Secondary,
            background = AdminDesign.Background,
            surface = AdminDesign.Surface,
            onSurface = AdminDesign.OnSurface,
            onSurfaceVariant = AdminDesign.OnSurfaceVariant,
            error = AdminDesign.Error
        ) else lightColorScheme(
            primary = AdminDesign.Primary,
            secondary = AdminDesign.Secondary,
            background = AdminDesign.Background,
            surface = AdminDesign.Surface,
            onSurface = AdminDesign.OnSurface,
            onSurfaceVariant = AdminDesign.OnSurfaceVariant,
            error = AdminDesign.Error
        ),
        content = content
    )
}

@Composable
fun StatusBadge(text: String, color: Color = AdminDesign.Primary) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ShimmerBrush(): Brush {
    val shimmerColors = listOf(
        AdminDesign.OnSurface.copy(alpha = 0.05f),
        AdminDesign.OnSurface.copy(alpha = 0.15f),
        AdminDesign.OnSurface.copy(alpha = 0.05f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun ShimmerItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(AdminDesign.CardShape)
            .background(ShimmerBrush())
    )
}

@Composable
fun AdminEmptyState(
    title: String = "No Data Found",
    description: String = "There are no records to display at this time.",
    icon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AdminDesign.SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon?.invoke()
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AdminDesign.OnSurface
        )
        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = AdminDesign.OnSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )
    }
}

@Composable
fun AdminErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AdminDesign.SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AdminDesign.Error
        )
        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AdminDesign.OnSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )
        Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
        Button(
            onClick = onRetry,
            shape = AdminDesign.ButtonShape,
            colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
        ) {
            Text("Try Again", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AdminLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize().background(AdminDesign.Background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AdminDesign.Primary, strokeWidth = 3.dp)
    }
}
