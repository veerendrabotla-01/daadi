package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daadi.engine.ai.AIPersonality
import com.example.daadi.engine.ai.AiConfig

@Composable
fun AdminAIEngineScreen(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val systemSettings by adminViewModel.remoteConfigRepository.systemSettings.collectAsStateWithLifecycle()
    
    val currentMaxDepth = systemSettings.find { it.key == "ai_max_depth" }?.value?.toIntOrNull() ?: 4
    val currentMillWeight = systemSettings.find { it.key == "ai_mill_weight" }?.value?.toIntOrNull() ?: 150
    val currentPersonalityStr = systemSettings.find { it.key == "ai_personality" }?.value ?: AIPersonality.BALANCED.name
    val currentPersonality = try { AIPersonality.valueOf(currentPersonalityStr) } catch(e: Exception) { AIPersonality.BALANCED }

    var config by remember(currentMaxDepth, currentMillWeight, currentPersonality) { 
        mutableStateOf(AiConfig(maxDepth = currentMaxDepth, millWeight = currentMillWeight, personality = currentPersonality)) 
    }
    var isSaving by remember { mutableStateOf(false) }

    AdminFoundationScaffold("AI Engine Core", adminViewModel = adminViewModel, onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            item {
                Text("STRATEGIC HEURISTICS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            }

            item {
                ConfigSliderCard(
                    title = "Search Depth",
                    value = config.maxDepth.toFloat(),
                    range = 1f..6f,
                    steps = 5,
                    onValueChange = { config = config.copy(maxDepth = it.toInt()) },
                    description = "Number of look-ahead turns. Higher is smarter but slower."
                )
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            }

            item {
                ConfigSliderCard(
                    title = "Mill Weight",
                    value = config.millWeight.toFloat(),
                    range = 50f..500f,
                    onValueChange = { config = config.copy(millWeight = it.toInt()) },
                    description = "Priority given to forming mills."
                )
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            }

            item {
                Text("PERSONALITY CORE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = AdminDesign.Primary)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
                    colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
                ) {
                    Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
                        AIPersonality.entries.forEach { personality ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                RadioButton(
                                    selected = config.personality == personality,
                                    onClick = { config = config.copy(personality = personality) }
                                )
                                Text(
                                    text = personality.name,
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = AdminDesign.OnSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AdminDesign.SpacingLarge))
            }

            item {
                Button(
                    onClick = { 
                        isSaving = true
                        adminViewModel.remoteConfigRepository.updateSystemSetting("ai_max_depth", config.maxDepth.toString())
                        adminViewModel.remoteConfigRepository.updateSystemSetting("ai_mill_weight", config.millWeight.toString())
                        adminViewModel.remoteConfigRepository.updateSystemSetting("ai_personality", config.personality.name)
                        isSaving = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AdminDesign.ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("APPLY GLOBAL AI PARAMETERS", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigSliderCard(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontWeight = FontWeight.Bold, color = AdminDesign.OnSurface)
                Text(value.toInt().toString(), fontWeight = FontWeight.Black, color = AdminDesign.Primary)
            }
            Text(description, fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(thumbColor = AdminDesign.Primary, activeTrackColor = AdminDesign.Primary)
            )
        }
    }
}
