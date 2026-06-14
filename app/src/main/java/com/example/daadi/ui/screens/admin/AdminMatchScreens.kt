package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.data.supabase.SupabaseMatch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMatchArchiveScreen(
    supabaseManager: SupabaseManager,
    onMatchClick: (SupabaseMatch) -> Unit,
    onBack: () -> Unit
) {
    val matches by supabaseManager.matches.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Registry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { padding ->
        if (matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No match records found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(matches) { match ->
                    MatchArchiveItem(match, onClick = { onMatchClick(match) }, onDelete = { supabaseManager.deleteMatch(match.id) })
                }
            }
        }
    }
}

@Composable
fun MatchArchiveItem(match: SupabaseMatch, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Room: ${match.id.uppercase()}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF5C2D0A))
                Text(match.status.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (match.status == "finished") Color(0xFF2E7D32) else Color(0xFFE65100))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${match.hostName} VS ${match.opponentName.ifEmpty { "..." }}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Date: ${match.createdAt} • ${match.movesCount} Moves", fontSize = 11.sp, color = Color.Gray)
            
            if (match.winner != null) {
                Text("Winner: ${match.winner}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 4.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMatchDetailScreen(
    match: SupabaseMatch,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Audit Log", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MATCH SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5E3C))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ID: ${match.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Players: ${match.hostName} vs ${match.opponentName}", fontSize = 14.sp)
                    Text("Status: ${match.status.uppercase()}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (match.winner != null) Text("Result: ${match.winner} WON", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("MOVE TRANSCRIPT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5E3C))
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (match.movesJson.isNullOrBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray)
                            Text("No move-by-move data for this session.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        // In a real app, parse AND show move history list
                        Text(match.movesJson, modifier = Modifier.padding(16.dp), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
