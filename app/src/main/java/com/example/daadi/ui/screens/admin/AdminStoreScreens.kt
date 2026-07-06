package com.example.daadi.ui.screens.admin

import com.example.daadi.data.supabase.SupabaseStoreItem
import com.example.daadi.data.supabase.SupabaseCoupon

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
fun AdminStoreManagement(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val storeItems by adminViewModel.economyRepository.storeItems.collectAsStateWithLifecycle()
    val coupons by adminViewModel.economyRepository.coupons.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Items & Packs", "Coupons")
    var showCreateItemDialog by remember { mutableStateOf(false) }
    var showCreateCouponDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.economyRepository.fetchStoreItems()
        adminViewModel.economyRepository.fetchCoupons()
    }

    AdminFoundationScaffold(
        title = "Store Management", 
        adminViewModel = adminViewModel, 
        onBack = onBack,
        actions = {
            if (selectedTab == 0) {
                IconButton(onClick = { showCreateItemDialog = true }) {
                    Icon(Icons.Default.AddBusiness, contentDescription = "Add Pack", tint = AdminDesign.Primary)
                }
            } else {
                IconButton(onClick = { showCreateCouponDialog = true }) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = "Add Coupon", tint = AdminDesign.Primary)
                }
            }
        }
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
                        0 -> StoreItemsList(
                            items = storeItems, 
                            isSyncing = isSyncing,
                            onDelete = { id -> adminViewModel.economyRepository.deleteStoreItem(id) }
                        )
                        1 -> CouponList(
                            coupons = coupons, 
                            isSyncing = isSyncing,
                            onDelete = { id -> adminViewModel.economyRepository.deleteCoupon(id) }
                        )
                    }
                }
            }
        }

        if (showCreateItemDialog) {
            CreateStoreItemDialog(
                onDismiss = { showCreateItemDialog = false },
                onConfirm = { name, desc, type, coinPrice, usdPrice, isFeatured, discount ->
                    adminViewModel.economyRepository.createStoreItem(name, desc, type, coinPrice, usdPrice, isFeatured, discount)
                    showCreateItemDialog = false
                }
            )
        }

        if (showCreateCouponDialog) {
            CreateCouponDialog(
                onDismiss = { showCreateCouponDialog = false },
                onConfirm = { code, dType, valDouble, maxU ->
                    adminViewModel.economyRepository.createCoupon(code, dType, valDouble, maxU)
                    showCreateCouponDialog = false
                }
            )
        }
    }
}

@Composable
fun StoreItemsList(
    items: List<SupabaseStoreItem>, 
    isSyncing: Boolean,
    onDelete: (String) -> Unit
) {
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
                StoreItemCard(item, onDelete)
            }
        }
    }
}

@Composable
fun StoreItemCard(item: SupabaseStoreItem, onDelete: (String) -> Unit) {
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
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Pack", tint = AdminDesign.Error)
            }
        }
    }
}

@Composable
fun CouponList(
    coupons: List<SupabaseCoupon>, 
    isSyncing: Boolean,
    onDelete: (String) -> Unit
) {
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
                CouponCard(coupon, onDelete)
            }
        }
    }
}

@Composable
fun CouponCard(coupon: SupabaseCoupon, onDelete: (String) -> Unit) {
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
            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
            IconButton(onClick = { onDelete(coupon.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete coupon", tint = AdminDesign.Error)
            }
        }
    }
}

@Composable
fun CreateStoreItemDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, Int?, Double?, Boolean, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("pack") }
    var currencyType by remember { mutableStateOf("USD") } // or "COINS"
    var coinPrice by remember { mutableStateOf("100") }
    var usdPrice by remember { mutableStateOf("4.99") }
    var isFeatured by remember { mutableStateOf(false) }
    var discountPercent by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deploy Store Item/Pack", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Product Description") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape, minLines = 2)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("TYPE:", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    FilterChip(selected = type == "pack", onClick = { type = "pack" }, label = { Text("PACK") })
                    FilterChip(selected = type == "bundle", onClick = { type = "bundle" }, label = { Text("BUNDLE") })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("PRICING:", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    FilterChip(selected = currencyType == "USD", onClick = { currencyType = "USD" }, label = { Text("FIAT (USD)") })
                    FilterChip(selected = currencyType == "COINS", onClick = { currencyType = "COINS" }, label = { Text("GOLD COINS") })
                }

                if (currencyType == "USD") {
                    OutlinedTextField(value = usdPrice, onValueChange = { usdPrice = it }, label = { Text("Price (USD)") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                } else {
                    OutlinedTextField(value = coinPrice, onValueChange = { coinPrice = it }, label = { Text("Price (Coins)") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                }

                OutlinedTextField(value = discountPercent, onValueChange = { discountPercent = it }, label = { Text("Discount Percentage") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                    Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                    Text("Pin to Featured Shelf", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCoinPrice = if (currencyType == "COINS") coinPrice.toIntOrNull() ?: 0 else null
                    val finalUsdPrice = if (currencyType == "USD") usdPrice.toDoubleOrNull() ?: 0.0 else null
                    onConfirm(
                        name,
                        desc,
                        type,
                        finalCoinPrice,
                        finalUsdPrice,
                        isFeatured,
                        discountPercent.toIntOrNull() ?: 0
                    )
                },
                shape = AdminDesign.ButtonShape
            ) {
                Text("PUBLISH PACK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
        }
    )
}

@Composable
fun CreateCouponDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double, Int?) -> Unit) {
    var code by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("percentage") } // or "fixed"
    var value by remember { mutableStateOf("15") }
    var maxUses by remember { mutableStateOf("500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Issue Promo Coupon", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("PROMO CODE (e.g. SUMMER50)") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("DISCOUNT TYPE:", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    FilterChip(selected = discountType == "percentage", onClick = { discountType = "percentage" }, label = { Text("PERCENTAGE") })
                    FilterChip(selected = discountType == "fixed", onClick = { discountType = "fixed" }, label = { Text("FLAT CASH") })
                }

                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(if (discountType == "percentage") "Percentage Discount (%)" else "Flat Discount Value ($)") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
                OutlinedTextField(value = maxUses, onValueChange = { maxUses = it }, label = { Text("Max Claims (Blank for infinite)") }, modifier = Modifier.fillMaxWidth(), shape = AdminDesign.InputShape)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        code.uppercase(),
                        discountType,
                        value.toDoubleOrNull() ?: 0.0,
                        maxUses.toIntOrNull()
                    )
                },
                shape = AdminDesign.ButtonShape
            ) {
                Text("ISSUE COUPON")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT") }
        }
    )
}
