package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Invoice
import com.example.ui.InvoiceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FilterChipCustom(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF6750A4) else Color(0xFFF3EDFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("filter_chip_$label")
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF49454F),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InvoiceViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onSignOut: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val userStats by viewModel.userStats.collectAsState()

    // Search and filter states
    var searchQuery by remember { mutableStateOf<String>("") }
    var selectedCategory by remember { mutableStateOf<String>("Všetko") }

    // Detail Dialog state
    var selectedInvoiceForDetail by remember { mutableStateOf<Invoice?>(null) }

    // Categories list for Slovakia business
    val SlovakCategories = listOf("Všetko", "Kancelária", "Cestovné", "Služby", "Technika", "Strava", "Iné")

    // Filter invoices dynamically
    val filteredInvoices = invoices.filter { invoice ->
        val matchesSearch = invoice.supplierName.contains(searchQuery, ignoreCase = true) ||
                (invoice.description?.contains(searchQuery, ignoreCase = true) ?: false)
        val matchesCategory = selectedCategory == "Všetko" || invoice.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        containerColor = Color(0xFFFDF8FF), // Bento background
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Blesková Faktúra",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF21005D) // Deep purple
                        )
                        Text(
                            text = "${currentUser?.displayName ?: "Hosť"} • Evidencia 2026",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF49454F)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToPricing,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pricing_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Predplatné",
                            tint = Color(0xFF6750A4)
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Odhlásiť sa",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF8FF),
                    titleContentColor = Color(0xFF21005D)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDFF),
                modifier = Modifier.border(width = 0.5.dp, color = Color(0xFFCAC4D0))
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Domov */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Domov", tint = Color(0xFF21005D)) },
                    label = { Text("Domov", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFFEADDFF)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPricing,
                    icon = { Icon(Icons.Default.Receipt, contentDescription = "Doklady", tint = Color(0xFF49454F)) },
                    label = { Text("Doklady", color = Color(0xFF49454F)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPricing,
                    icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Prehľad", tint = Color(0xFF49454F)) },
                    label = { Text("Prehľad", color = Color(0xFF49454F)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPricing,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Nastavenia", tint = Color(0xFF49454F)) },
                    label = { Text("Nastavenia", color = Color(0xFF49454F)) }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.shouldAutoLaunchCamera = true
                    onNavigateToScan()
                },
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("fab_scan_invoice"),
                containerColor = Color(0xFF6750A4), // Bento primary button
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Skenovať"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bleskový Sken",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bento Cell 1: Hero Section (Revenue / total expenses)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_expenses_bento_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEADDFF) // Light lavender/purple
                    ),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VÝDAVKY S DPH",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF21005D)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF21005D))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+12.4%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFEADDFF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = String.format(Locale.GERMANY, "%.2f €", userStats.totalInvoiced),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 38.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = Color(0xFF21005D)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Počet zaevidovaných dokladov: ${userStats.invoiceCount} ks • Limit: Bez limitu",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF49454F)
                        )
                    }
                }
            }

            // Bento Cell 2 & 3 Row: Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action 1: Skenuj AI
                    Card(
                        onClick = onNavigateToScan,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .testTag("action_scan_bento_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD0BCFF) // Medium Purple Accent
                        ),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF21005D))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Skenovať",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Skenuj AI",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF21005D)
                                )
                                Text(
                                    text = "Pridať účtenku",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF21005D).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Action 2: Predplatné
                    Card(
                        onClick = onNavigateToPricing,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .testTag("action_pricing_bento_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFD8E4) // Pink Accent
                        ),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF31111D))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Predplatné",
                                    tint = Color(0xFFFFD8E4),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Predplatné",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF31111D)
                                )
                                Text(
                                    text = "AI Kredity & Balíky",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF31111D).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Bento Cell 4: Compliance / AI Status
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_bento_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STAV AI SKENERA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF1D1B20)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF3EDFF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "AI AKTIÍVNE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF6750A4)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Credits/Limit bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (userStats.isSubscribed) "Blesk Premium (Neobmedzene)" else "Kredity: ${userStats.credits} skenov zostáva",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = if (userStats.isSubscribed) "100%" else "${(userStats.credits * 100) / 15}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF6750A4)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { if (userStats.isSubscribed) 1.0f else (userStats.credits.toFloat() / 15f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF6750A4),
                            trackColor = Color(0xFFE6E1E5)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Compliance Checklist
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Overené",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Slovenská legislatíva 2026",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF49454F)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Overené",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Automatické rozpoznanie DPH (20%, 10%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }
            }

            // Bento Cell 5: Monthly Expenses Bar Chart
            item {
                MonthlyExpensesBarChart(invoices = invoices)
            }

            // Search Bar & Filter Section within bento aesthetic
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ZOZNAM DOKLADOV",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF1D1B20)
                    )

                    // PDF Export Button
                    val context = androidx.compose.ui.platform.LocalContext.current
                    TextButton(
                        onClick = {
                            com.example.ui.PdfExporter.exportInvoicesToPdf(context, filteredInvoices)
                        },
                        modifier = Modifier.testTag("btn_export_pdf"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF6750A4)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exportovať do PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Exportovať PDF",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_invoices_input"),
                    placeholder = { Text("Vyhľadať dodávateľa, položku...", color = Color(0xFF49454F)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Hľadať", tint = Color(0xFF6750A4)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Vymazať", tint = Color(0xFF6750A4))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    )
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(SlovakCategories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChipCustom(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = category
                        )
                    }
                }
            }

            // List of Invoices
            if (filteredInvoices.isEmpty()) {
                item {
                    // Beautiful empty state in Light theme
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Prázdny stav",
                                tint = Color(0xFFCAC4D0),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedCategory != "Všetko") {
                                    "Nenašli sa žiadne výsledky"
                                } else {
                                    "Žiadne zaevidované doklady"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1D1B20),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedCategory != "Všetko") {
                                    "Skúste upraviť vyhľadávací výraz alebo vybrať inú kategóriu."
                                } else {
                                    "Stlačte tlačidlo 'Bleskový Sken' alebo zvoľte skenovanie z menu."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF49454F),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    InvoiceItemRow(
                        invoice = invoice,
                        onClick = { selectedInvoiceForDetail = invoice },
                        onDelete = { viewModel.deleteInvoice(invoice.id) }
                    )
                }
            }
        }
    }

    // Detail Dialog for Viewing/Editing scanned invoice fields
    selectedInvoiceForDetail?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            onDismiss = { selectedInvoiceForDetail = null },
            onSave = { updated ->
                viewModel.saveInvoice(updated)
                selectedInvoiceForDetail = null
            }
        )
    }
}

// --- Interactive Row Item Composable ---
@Composable
fun InvoiceItemRow(
    invoice: Invoice,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("invoice_row_${invoice.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, Color(0xFFCAC4D0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Avatar indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when (invoice.category) {
                                "Kancelária" -> Color(0xFF6750A4).copy(alpha = 0.12f)
                                "Cestovné" -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                "Služby" -> Color(0xFF8B5CF6).copy(alpha = 0.12f)
                                "Technika" -> Color(0xFFEC4899).copy(alpha = 0.12f)
                                "Strava" -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                else -> Color(0xFF64748B).copy(alpha = 0.12f)
                            }
                        )
                ) {
                    Icon(
                        imageVector = when (invoice.category) {
                            "Kancelária" -> Icons.Default.Description
                            "Cestovné" -> Icons.Default.DirectionsCar
                            "Služby" -> Icons.Default.Build
                            "Technika" -> Icons.Default.Computer
                            "Strava" -> Icons.Default.Restaurant
                            else -> Icons.Default.Receipt
                        },
                        contentDescription = invoice.category,
                        tint = when (invoice.category) {
                            "Kancelária" -> Color(0xFF6750A4)
                            "Cestovné" -> Color(0xFFF59E0B)
                            "Služby" -> Color(0xFF8B5CF6)
                            "Technika" -> Color(0xFFEC4899)
                            "Strava" -> Color(0xFFEF4444)
                            else -> Color(0xFF64748B)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = invoice.supplierName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1D1B20),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = invoice.issueDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            color = Color(0xFFCAC4D0),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = invoice.category,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = when (invoice.category) {
                                "Kancelária" -> Color(0xFF6750A4)
                                "Cestovné" -> Color(0xFFD97706)
                                "Služby" -> Color(0xFF7C3AED)
                                "Technika" -> Color(0xFFDB2777)
                                "Strava" -> Color(0xFFDC2626)
                                else -> Color(0xFF4B5563)
                            }
                        )
                    }

                    // Display due-date warning badge if parsed successfully
                    val parsedDate = remember(invoice.issueDate) { parseInvoiceDate(invoice.issueDate) }
                    if (parsedDate != null) {
                        val cal = Calendar.getInstance().apply {
                            time = parsedDate
                            add(Calendar.DAY_OF_YEAR, 14)
                        }
                        val dueDate = cal.time
                        
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                        
                        val diffMillis = dueDate.time - today.time
                        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                        
                        if (diffDays < 0) {
                            val daysOverdue = -diffDays
                            val overdueText = when (daysOverdue) {
                                1 -> "Po splatnosti 1 deň"
                                in 2..4 -> "Po splatnosti $daysOverdue dni"
                                else -> "Po splatnosti $daysOverdue dní"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFDAD6))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .testTag("invoice_overdue_badge_${invoice.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFBA1A1A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = overdueText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF410002)
                                )
                            }
                        } else if (diffDays in 0..3) {
                            val warningText = when (diffDays) {
                                0 -> "Splatnosť dnes!"
                                1 -> "Splatnosť zajtra"
                                else -> "Splatnosť o $diffDays dni"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFE8D6))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .testTag("invoice_warning_badge_${invoice.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = warningText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.GERMANY, "%.2f €", invoice.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFF21005D)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_invoice_${invoice.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Zmazať",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- Stats Card Component (retained as helper for backwards compatibility) ---
@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF49454F)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = Color(0xFF1D1B20)
            )
        }
    }
}

// --- Detail Dialog to Edit Invoice Fields (Slovak) ---
@Composable
fun InvoiceDetailDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onSave: (Invoice) -> Unit
) {
    var supplierName by remember { mutableStateOf<String>(invoice.supplierName) }
    var ico by remember { mutableStateOf<String>(invoice.ico ?: "") }
    var dic by remember { mutableStateOf<String>(invoice.dic ?: "") }
    var totalAmountStr by remember { mutableStateOf<String>(invoice.totalAmount.toString()) }
    var vatRateStr by remember { mutableStateOf<String>(invoice.vatRate?.toString() ?: "20.0") }
    var vatAmountStr by remember { mutableStateOf<String>(invoice.vatAmount?.toString() ?: "") }
    var issueDate by remember { mutableStateOf<String>(invoice.issueDate) }
    var category by remember { mutableStateOf<String>(invoice.category) }
    var description by remember { mutableStateOf<String>(invoice.description ?: "") }

    var isMenuExpanded by remember { mutableStateOf<Boolean>(false) }
    val categories = listOf("Kancelária", "Cestovné", "Služby", "Technika", "Strava", "Iné")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("invoice_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Detail účtovného dokladu",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF21005D),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Dodávateľ") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_supplier_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        )
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ico,
                            onValueChange = { ico = it },
                            label = { Text("IČO") },
                            modifier = Modifier.weight(1f).testTag("edit_ico_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20)
                            )
                        )
                        OutlinedTextField(
                            value = dic,
                            onValueChange = { dic = it },
                            label = { Text("DIČ / IČ DPH") },
                            modifier = Modifier.weight(1f).testTag("edit_dic_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20)
                            )
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = totalAmountStr,
                            onValueChange = { totalAmountStr = it },
                            label = { Text("Suma s DPH (€)") },
                            modifier = Modifier.weight(1.2f).testTag("edit_amount_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20)
                            )
                        )
                        OutlinedTextField(
                            value = vatRateStr,
                            onValueChange = { vatRateStr = it },
                            label = { Text("DPH (%)") },
                            modifier = Modifier.weight(0.8f).testTag("edit_vat_rate_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20)
                            )
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = issueDate,
                            onValueChange = { issueDate = it },
                            label = { Text("Dátum vystavenia") },
                            modifier = Modifier.weight(1f).testTag("edit_date_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20)
                            )
                        )

                        // Highly compatible Custom Dropdown Menu (no ExposedDropdownMenuBox version errors)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategória") },
                                trailingIcon = {
                                    IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                                        Icon(
                                            imageVector = if (isMenuExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Menu",
                                            tint = Color(0xFF1D1B20)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMenuExpanded = !isMenuExpanded }
                                    .testTag("edit_category_selector"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0),
                                    focusedTextColor = Color(0xFF1D1B20),
                                    unfocusedTextColor = Color(0xFF1D1B20)
                                )
                            )
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = Color(0xFF1D1B20)) },
                                        onClick = {
                                            category = cat
                                            isMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Real-time calculated due date warning / info in Detail Dialog
                item {
                    val parsedDate = remember(issueDate) { parseInvoiceDate(issueDate) }
                    if (parsedDate != null) {
                        val cal = Calendar.getInstance().apply {
                            time = parsedDate
                            add(Calendar.DAY_OF_YEAR, 14)
                        }
                        val dueDate = cal.time
                        val formattedDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dueDate)
                        
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                        
                        val diffMillis = dueDate.time - today.time
                        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                        
                        val (bgColor, textColor, strokeColor, statusText, icon) = when {
                            diffDays < 0 -> {
                                val daysOverdue = -diffDays
                                val daysText = when (daysOverdue) {
                                    1 -> "1 deň"
                                    in 2..4 -> "$daysOverdue dni"
                                    else -> "$daysOverdue dní"
                                }
                                DueStatusInfo(
                                    bgColor = Color(0xFFFFDAD6),
                                    textColor = Color(0xFF410002),
                                    strokeColor = Color(0xFFFFB4AB),
                                    statusText = "Doklad je po splatnosti o $daysText (Dátum splatnosti: $formattedDueDate)",
                                    icon = Icons.Default.Warning
                                )
                            }
                            diffDays in 0..3 -> {
                                val daysText = when (diffDays) {
                                    0 -> "dnes!"
                                    1 -> "zajtra"
                                    else -> "o $diffDays dni"
                                }
                                DueStatusInfo(
                                    bgColor = Color(0xFFFFE8D6),
                                    textColor = Color(0xFF78350F),
                                    strokeColor = Color(0xFFFFB27A),
                                    statusText = "Splatnosť dokladu sa blíži - $daysText (Dátum splatnosti: $formattedDueDate)",
                                    icon = Icons.Default.Info
                                )
                            }
                            else -> {
                                DueStatusInfo(
                                    bgColor = Color(0xFFE8F5E9),
                                    textColor = Color(0xFF1B5E20),
                                    strokeColor = Color(0xFFA5D6A7),
                                    statusText = "Doklad je v splatnosti - ostáva $diffDays dní (Dátum splatnosti: $formattedDueDate)",
                                    icon = Icons.Default.CheckCircle
                                )
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("due_date_warning_banner"),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(1.dp, strokeColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Poznámka / Popis položky") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_description_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("cancel_edit_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1D1B20)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                        ) {
                            Text("Zrušiť")
                        }

                        Button(
                            onClick = {
                                val amount = totalAmountStr.toDoubleOrNull() ?: invoice.totalAmount
                                val vRate = vatRateStr.toDoubleOrNull() ?: 20.0
                                val vAmount = vatAmountStr.toDoubleOrNull() ?: (amount * (vRate / (100.0 + vRate)))

                                onSave(
                                    invoice.copy(
                                        supplierName = supplierName,
                                        ico = ico.ifBlank { null },
                                        dic = dic.ifBlank { null },
                                        totalAmount = amount,
                                        vatRate = vRate,
                                        vatAmount = vAmount,
                                        issueDate = issueDate,
                                        category = category,
                                        description = description.ifBlank { null }
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("save_edit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Uložiť", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyExpensesBarChart(
    invoices: List<Invoice>,
    modifier: Modifier = Modifier
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Máj", "Jún", "Júl", "Aug", "Sep", "Okt", "Nov", "Dec")
    val monthFullNames = listOf(
        "Január", "Február", "Marec", "Apríl", "Máj", "Jún",
        "Júl", "August", "September", "Október", "November", "December"
    )

    // Calculate monthly totals
    val monthlyTotals = remember(invoices) {
        val totals = DoubleArray(12) { 0.0 }
        invoices.forEach { invoice ->
            val dateParts = invoice.issueDate.split("-")
            if (dateParts.size >= 2) {
                val year = dateParts[0].toIntOrNull()
                val month = dateParts[1].toIntOrNull()
                if (year == currentYear && month != null && month in 1..12) {
                    totals[month - 1] += invoice.totalAmount
                }
            }
        }
        totals
    }

    val maxTotal = remember(monthlyTotals) {
        val max = monthlyTotals.maxOrNull() ?: 0.0
        if (max == 0.0) 100.0 else max
    }

    // Default selected month is the current month (0-indexed)
    val currentMonthIdx = remember { Calendar.getInstance().get(Calendar.MONTH) }
    var selectedMonthIdx by remember { mutableStateOf<Int?>(currentMonthIdx) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_expenses_chart_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Graf",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MESAČNÝ PREHĽAD VÝDAVKOV",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF1D1B20)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF3EDFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ROK $currentYear",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFF6750A4)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Display of Selected Month Total
            val dispIdx = selectedMonthIdx ?: currentMonthIdx
            val dispMonthName = monthFullNames[dispIdx]
            val dispAmount = monthlyTotals[dispIdx]

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = dispMonthName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF49454F)
                    )
                    Text(
                        text = String.format(Locale.GERMANY, "%.2f €", dispAmount),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFF21005D)
                    )
                }
                
                Text(
                    text = "Kliknutím vyberte mesiac",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF49454F).copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // The Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = Color(0xFFE6E1E5).copy(alpha = 0.5f)
                        )
                    }
                }

                // Bars Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyTotals.forEachIndexed { idx, total ->
                        val isSelected = selectedMonthIdx == idx
                        val barHeightFraction = (total / maxTotal).toFloat().coerceIn(0.02f, 1.0f)
                        
                        // Beautiful animation of bar scale when loading
                        val animatedFraction by animateFloatAsState(
                            targetValue = barHeightFraction,
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "BarHeightAnimation"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedMonthIdx = idx
                                }
                                .padding(horizontal = 2.dp)
                        ) {
                            // Bar column
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp * animatedFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = if (isSelected) {
                                                listOf(Color(0xFF6750A4), Color(0xFFD0BCFF))
                                            } else {
                                                if (total > 0.0) {
                                                    listOf(Color(0xFFEADDFF), Color(0xFFF3EDFF))
                                                } else {
                                                    listOf(Color(0xFFE6E1E5).copy(alpha = 0.3f), Color(0xFFE6E1E5).copy(alpha = 0.1f))
                                                }
                                            }
                                        )
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF6750A4) else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Month Label
                            Text(
                                text = monthNames[idx],
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                ),
                                color = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseInvoiceDate(dateStr: String): Date? {
    val formats = listOf("yyyy-MM-dd", "dd.MM.yyyy", "d.M.yyyy", "yyyy/MM/dd")
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.isLenient = false
            return sdf.parse(dateStr.trim())
        } catch (e: Exception) {
            // try next format
        }
    }
    return null
}

private data class DueStatusInfo(
    val bgColor: Color,
    val textColor: Color,
    val strokeColor: Color,
    val statusText: String,
    val icon: ImageVector
)
