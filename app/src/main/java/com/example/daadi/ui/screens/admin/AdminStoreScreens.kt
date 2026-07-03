package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun AdminStoreManagement(supabaseManager: SupabaseManager, onBack: () -> Unit) {
    val storeItems by supabaseManager.storeItems.collectAsStateWithLifecycle()
    val coupons by supabaseManager.coupons.collectAsStateWithLifecycle()
    val isSyncing by supabaseManager.isSyncing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Items & Packs", "Coupons")

    AdminFoundationScaffold(
        title = "Store Management", 
        supabaseManager = supabaseManager, 
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AdminDesign.Primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(targetState = selectedTab, label = "store_tabs") { tabIndex ->
                    when (tabIndex) {
                        0 -> StoreItemsList(storeItems, isSyncing)
                        1 -> CouponList(coupons, isSyncing)
                    }
                }
            }
        }
    }
}

@Composable
fun StoreItemsList(items: List<com.example.daadi.data.supabase.SupabaseStoreItem>, isSyncing: Boolean) {
    if (isSyncing && items.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else if (items.isEmpty()) {
        AdminEmptyState(title = "No Store Items", description = "The marketplace is currently empty. Add items to start selling.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
        ) {
            items(items) { item ->
                StoreItemCard(item)
            }
        }
    }
}

@Composable
fun StoreItemCard(item: com.example.daadi.data.supabase.SupabaseStoreItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(8.dp),
                color = AdminDesign.OnSurfaceVariant.copy(alpha = 0.05f)
            ) {
                AsyncImage(
                    model = item.imageUrl ?: "https://via.placeholder.com/150",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AdminDesign.OnSurface)
                    if (item.isFeatured) {
                        Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                        Badge(containerColor = AdminDesign.Secondary) { 
                            Text("FEATURED", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold) 
                        }
                    }
                }
                Text(item.description ?: "No description provided", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant, maxLines = 2)
                Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.priceUsd != null) {
                        Text("$${item.priceUsd}", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32), fontSize = 18.sp)
                    } else if (item.priceCoins != null) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(item.priceCoins.toString(), fontWeight = FontWeight.Black, color = AdminDesign.Primary, fontSize = 18.sp)
                    }
                    
                    if (item.discountPercentage > 0) {
                        Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                        Surface(color = AdminDesign.Error.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "-${item.discountPercentage}%",
                                color = AdminDesign.Error,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            IconButton(onClick = { /* Edit Item */ }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AdminDesign.Primary)
            }
        }
    }
}

@Composable
fun CouponList(coupons: List<com.example.daadi.data.supabase.SupabaseCoupon>, isSyncing: Boolean) {
    if (isSyncing && coupons.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
            items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
        }
    } else if (coupons.isEmpty()) {
        AdminEmptyState(title = "No Coupons Found", description = "Create promotional codes to drive sales and user engagement.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AdminDesign.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingMedium)
        ) {
            items(coupons) { coupon ->
                CouponCard(coupon)
            }
        }
    }
}

@Composable
fun CouponCard(coupon: com.example.daadi.data.supabase.SupabaseCoupon) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = AdminDesign.Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = AdminDesign.Primary)
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(coupon.code, fontWeight = FontWeight.Black, fontSize = 16.sp, color = AdminDesign.OnSurface)
                Text(
                    text = if (coupon.discountType == "fixed") "$${coupon.value} FLAT DISCOUNT" else "${coupon.value}% PERCENTAGE OFF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminDesign.Secondary
                )
                Text("Redemptions: ${coupon.usedCount} / ${coupon.maxUses ?: "Unlimited"}", fontSize = 10.sp, color = AdminDesign.OnSurfaceVariant)
            }
            Badge(
                containerColor = if (coupon.isActive) Color(0xFF2E7D32) else AdminDesign.OnSurfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(AdminDesign.SpacingSmall)
            ) {
                Text(
                    text = if (coupon.isActive) "ACTIVE" else "EXPIRED", 
                    color = if (coupon.isActive) Color.White else AdminDesign.OnSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
