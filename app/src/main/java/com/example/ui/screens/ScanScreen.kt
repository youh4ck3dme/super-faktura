package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Invoice
import com.example.data.ExtractedInvoiceJson
import com.example.ui.InvoiceViewModel
import com.example.ui.ScanUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsState()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var stagedUri by remember { mutableStateOf<Uri?>(null) }
    var stagedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var stagedFileName by remember { mutableStateOf<String>("") }
    var stagedFileSize by remember { mutableStateOf<String>("") }

    // Clear staged file when entering or when ScanState resets to Idle
    LaunchedEffect(scanState) {
        if (scanState is ScanUiState.Idle) {
            stagedUri = null
            stagedBitmap = null
            stagedFileName = ""
            stagedFileSize = ""
        }
    }

    // System Image Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                stagedUri = it
                stagedBitmap = bitmap
                val (name, size) = getFileInfo(context, it)
                stagedFileName = name
                stagedFileSize = size
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            stagedUri = null
            stagedBitmap = it
            stagedFileName = "fotoaparát_snímka.jpg"
            stagedFileSize = String.format(Locale.US, "%.1f KB", (it.byteCount / 1024).toDouble())
        }
    }

    // Auto-launch camera if requested from Dashboard
    LaunchedEffect(Unit) {
        if (viewModel.shouldAutoLaunchCamera) {
            viewModel.shouldAutoLaunchCamera = false
            cameraLauncher.launch()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scan_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Bleskový AI skener", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Späť",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (scanState is ScanUiState.Idle && stagedBitmap == null) {
                ExtendedFloatingActionButton(
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier
                        .padding(8.dp)
                        .testTag("fab_instant_camera_capture"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Odfotiť nový doklad"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Odfotiť nový doklad",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = scanState) {
                is ScanUiState.Idle -> {
                    if (stagedBitmap != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Info badge
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Doklad bol úspešne pripravený. Kliknite na tlačidlo nižšie pre spustenie AI analýzy.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Interactive file input detail box
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Image preview
                                    Image(
                                        bitmap = stagedBitmap!!.asImageBitmap(),
                                        contentDescription = "Náhľad vybraného dokladu",
                                        modifier = Modifier
                                            .size(160.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(
                                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // File info card
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = "Súbor",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stagedFileName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (stagedFileSize.isNotEmpty()) {
                                                    Text(
                                                        text = stagedFileSize,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                stagedUri = null
                                                stagedBitmap = null
                                                stagedFileName = ""
                                                stagedFileSize = ""
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("clear_staged_file_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Odstrániť súbor",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }

                            // Action buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedBitmap = stagedBitmap
                                        viewModel.scanInvoiceImage(stagedBitmap!!)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("analyze_with_mistral_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Mistral",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Extrahovať dáta s Mistral AI",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("change_staged_file_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Collections, contentDescription = "Galéria")
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Zvoliť iný súbor z galérie", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Explanatory card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Umelá inteligencia Blesk",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Odfoťte pokladničný blok alebo faktúru. AI prečíta text, identifikuje sumu, DPH, IČO aj kategóriu.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Dashed upload square area styled as a Bento tile (Now clickable file input!)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable { galleryLauncher.launch("image/*") }
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterCenterFocus,
                                        contentDescription = "Zameranie",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Zamerajte doklad alebo kliknite sem a vyberte súbor",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Uistite sa, že písmo je dobre čitateľné a svetlo je dostatočné.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }

                            // Scan Action buttons (Bento Grid aligned)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Camera Action
                                Button(
                                    onClick = { cameraLauncher.launch() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("scan_camera_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Fotoaparát")
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Odfotiť nový doklad", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Gallery Action
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("scan_gallery_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Collections, contentDescription = "Galéria")
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Vybrať fotku z galérie", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Sample simulation button
                                Button(
                                    onClick = {
                                        val bitmap = generateSampleReceiptBitmap()
                                        stagedUri = null
                                        stagedBitmap = bitmap
                                        stagedFileName = "vzorovy_tesco_doklad.png"
                                        stagedFileSize = "23.4 KB"
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("scan_mock_sample_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Receipt, contentDescription = "Vzor")
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Simulovať vzorový doklad (Tesco)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                is ScanUiState.Scanning -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Image preview if selected
                        selectedBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Nahrávaný doklad",
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "BLESKOVÁ ANALÝZA DOKLADU",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.progressMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                is ScanUiState.Success -> {
                    // Review form screen
                    ReviewScannedInvoiceForm(
                        extracted = state.extractedData,
                        bitmap = state.bitmap,
                        onCancel = { viewModel.startNewScan() },
                        onConfirm = { invoice ->
                            viewModel.saveInvoice(invoice)
                            onNavigateBack()
                        }
                    )
                }

                is ScanUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Chyba",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(36.dp)
                             )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Vyskytla sa chyba",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.startNewScan() },
                            modifier = Modifier
                                .width(200.dp)
                                .height(48.dp)
                                .testTag("retry_scan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Skúsiť znova", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Composable: Review and Edit scanned invoice fields before saving ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScannedInvoiceForm(
    extracted: ExtractedInvoiceJson,
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Invoice) -> Unit
) {
    var supplierName by remember { mutableStateOf<String>(extracted.supplierName) }
    var ico by remember { mutableStateOf<String>(extracted.ico ?: "") }
    var dic by remember { mutableStateOf<String>(extracted.dic ?: "") }
    var totalAmountStr by remember { mutableStateOf<String>(extracted.totalAmount.toString()) }
    var vatRateStr by remember { mutableStateOf<String>(extracted.vatRate?.toString() ?: "20.0") }
    var issueDate by remember { mutableStateOf<String>(extracted.issueDate) }
    var category by remember { mutableStateOf<String>(extracted.category) }
    var description by remember { mutableStateOf<String>(extracted.description ?: "") }

    var isMenuExpanded by remember { mutableStateOf<Boolean>(false) }
    val categories = listOf("Kancelária", "Cestovné", "Služby", "Technika", "Strava", "Iné")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("review_scanned_form"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Scanned",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AI extrakcia úspešná",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Skontrolujte a prípadne upravte zistené údaje.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = supplierName,
                onValueChange = { supplierName = it },
                label = { Text("Názov dodávateľa") },
                modifier = Modifier.fillMaxWidth().testTag("scanned_supplier_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ico,
                    onValueChange = { ico = it },
                    label = { Text("IČO") },
                    modifier = Modifier.weight(1f).testTag("scanned_ico_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = dic,
                    onValueChange = { dic = it },
                    label = { Text("DIČ / IČ DPH") },
                    modifier = Modifier.weight(1f).testTag("scanned_dic_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier.weight(1.2f).testTag("scanned_amount_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = vatRateStr,
                    onValueChange = { vatRateStr = it },
                    label = { Text("DPH (%)") },
                    modifier = Modifier.weight(0.8f).testTag("scanned_vat_rate_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier.weight(1.1f).testTag("scanned_date_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Category Selection Box (Highly compatible Custom Dropdown)
                Box(modifier = Modifier.weight(0.9f)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategória") },
                        trailingIcon = {
                            IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                                Icon(
                                    imageVector = if (isMenuExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Zoznam",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMenuExpanded = !isMenuExpanded }
                            .testTag("scanned_category_selector"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) },
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

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Popis výdavku") },
                modifier = Modifier.fillMaxWidth().testTag("scanned_description_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("cancel_scanned_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Vymazať")
                }

                Button(
                    onClick = {
                        val amount = totalAmountStr.toDoubleOrNull() ?: extracted.totalAmount
                        val vRate = vatRateStr.toDoubleOrNull() ?: 20.0
                        val vAmount = amount * (vRate / (100.0 + vRate))

                        onConfirm(
                            Invoice(
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
                    modifier = Modifier.weight(1.3f).height(52.dp).testTag("confirm_scanned_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Potvrdiť a uložiť", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Helper: Programmatically generate a realistic Slovak Tesco receipt on a Bitmap ---
fun generateSampleReceiptBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 560, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Background - crumpled off-white receipt paper
    canvas.drawColor(android.graphics.Color.parseColor("#FAF9F6"))
    
    val paint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.DKGRAY
        textSize = 14f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
    }
    
    val titlePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
        textSize = 22f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val subtitlePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.GRAY
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    // Top Receipt Margin
    var y = 40f
    canvas.drawText("TESCO STORES SR, a.s.", 200f, y, titlePaint)
    y += 24f
    canvas.drawText("Prevádzka: Kamenné Námestie, Bratislava", 200f, y, subtitlePaint)
    y += 18f
    canvas.drawText("IČO: 31321828  IČ DPH: SK2020300125", 200f, y, subtitlePaint)
    y += 25f
    
    paint.strokeWidth = 2f
    canvas.drawLine(30f, y, 370f, y, paint) // separator
    y += 25f
    
    // Receipt Items
    paint.textSize = 13f
    canvas.drawText("Kancelársky papier A4 (Reflex)  1 ks", 30f, y, paint)
    canvas.drawText("5.90 A", 300f, y, paint)
    y += 22f
    canvas.drawText("Blok poznámkový linajkový       5 ks", 30f, y, paint)
    canvas.drawText("12.50 A", 300f, y, paint)
    y += 22f
    canvas.drawText("USB Kľúč Kingston 64GB          1 ks", 30f, y, paint)
    canvas.drawText("18.00 A", 300f, y, paint)
    y += 22f
    canvas.drawText("Káva mletá Jacobs Krönung      1 ks", 30f, y, paint)
    canvas.drawText("9.50 B", 300f, y, paint)
    y += 30f

    canvas.drawLine(30f, y, 370f, y, paint)
    y += 30f

    // Total DPH Section
    val boldTotalPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
        textSize = 18f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    }
    
    canvas.drawText("CELKOM K ÚHRADE:", 30f, y, boldTotalPaint)
    canvas.drawText("45.90 EUR", 270f, y, boldTotalPaint)
    y += 26f
    
    // Tax breakdown
    paint.textSize = 12f
    canvas.drawText("Sadzba DPH       Bez DPH      DPH      S DPH", 30f, y, paint)
    y += 18f
    canvas.drawText("A (20.0%)        30.33     6.07     36.40", 30f, y, paint)
    y += 18f
    canvas.drawText("B (10.0%)         8.64     0.86      9.50", 30f, y, paint)
    y += 30f

    canvas.drawLine(30f, y, 370f, y, paint)
    y += 25f

    // Date & Time details
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    canvas.drawText("Dátum vystavenia: $date", 30f, y, paint)
    y += 18f
    canvas.drawText("DKP: 6012020300125010   Číslo dokladu: 4529", 30f, y, paint)
    y += 32f
    
    canvas.drawText("ĎAKUJEME ZA NÁKUP!", 200f, y, subtitlePaint)
    
    return bitmap
}

fun getFileInfo(context: android.content.Context, uri: Uri): Pair<String, String> {
    var name = "doklad_snímka.jpg"
    var sizeStr = ""
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: "doklad_snímka.jpg"
                }
                if (sizeIndex != -1) {
                    val size = cursor.getLong(sizeIndex)
                    sizeStr = if (size > 1024 * 1024) {
                        String.format(Locale.US, "%.2f MB", size.toDouble() / (1024 * 1024))
                    } else {
                        String.format(Locale.US, "%.1f KB", size.toDouble() / 1024)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return Pair(name, sizeStr)
}
