package com.example.ui

import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.LightBackground
import com.example.ui.theme.DebitRed
import com.example.ui.theme.CreditGreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Customer
import com.example.data.Product
import com.example.data.ScanLog
import com.example.data.Transaction
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScannerScreen(
    viewModel: LedgerViewModel,
    language: Language,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Scanner Unit, 1 = History & Analytics Logs
    val scanLogs by viewModel.scanLogs.collectAsStateWithLifecycle()
    
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (language == Language.HINDI) "स्मार्ट स्कैन केंद्र" else "Smart Scan Center",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (language == Language.HINDI) "UPI क्यूआर, बारकोड और बही प्रविष्टि" else "Decodes UPI QR codes, Invoices & Materials",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Tip: Scan customer card to open ledger. Scan invoice to load bill. Scan product to manage stock. UPI QR auto-adds transaction!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "info", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBackground)
        ) {
            // Beautiful Tab switcher
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.White,
                contentColor = PrimaryGreen,
                modifier = Modifier.fillMaxWidth().testTag("scanner_tabs")
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner Ready") },
                    text = { Text(if (language == Language.HINDI) "स्कैनर चालू" else "Scanner Unit") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Scan logs") },
                    text = { 
                        Text(
                            text = "${if (language == Language.HINDI) "इतिहास " else "History "}(${scanLogs.size})"
                        ) 
                    }
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (activeTab == 1) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (activeTab == 1) -width else width } + fadeOut()
                },
                label = "scanner_tabs_animation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ScannerUnitView(
                        viewModel = viewModel,
                        language = language,
                        customers = customers,
                        transactions = transactions,
                        products = products
                    )
                    1 -> ScanLogsHistoryView(
                        language = language,
                        scanLogs = scanLogs,
                        onClearAll = { viewModel.clearScanLogs() }
                    )
                }
            }
        }
    }
}

@Composable
fun ScannerUnitView(
    viewModel: LedgerViewModel,
    language: Language,
    customers: List<Customer>,
    transactions: List<Transaction>,
    products: List<Product>
) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted. Loading view...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission denied. Using Simulator Mode.", Toast.LENGTH_SHORT).show()
        }
    }

    var flashEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Physical Camera View card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraPermissionGranted) {
                        CameraScanPreview(
                            flashOn = flashEnabled,
                            onBarcodeDetected = { code ->
                                playBeep()
                                viewModel.handleScanPayload(code, "BARCODE") { toast ->
                                    Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    } else {
                        // Request Permission / Simulator Box
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera setup",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == Language.HINDI) "कैमरा स्कैनर सक्षम करें" else "Enable Hardware Camera Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == Language.HINDI)
                                    "भौतिक बही खाते या क्यूआर को तीव्रता से स्कैन करने के लिए अनुमति दें"
                                else
                                    "Permission needed to scan UPI QR reference and customer ledger cards in physical shop",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Text(if (language == Language.HINDI) "कैमरा सक्रिय करें" else "Activate Camera")
                            }
                        }
                    }

                    // Floating Scan Reticle Overlay
                    ScannerDecorativeReticle(modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Device Flashlight Toggle
        if (cameraPermissionGranted) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "flash",
                            tint = if (flashEnabled) Color.Yellow else Color.Gray
                        )
                    }
                }
            }
        }

        // Simulator Dashboard Panel (FOR EMULATORS & OFFLINE TESTING)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "simulation",
                            tint = PrimaryGreen
                        )
                        Column {
                            Text(
                                text = if (language == Language.HINDI) "स्मार्ट स्कैनर सिम्युलेटर डेस्क" else "High-Fidelity Scanner Emulator Desk",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = if (language == Language.HINDI) "बिना कैमरा भी संपूर्ण टेस्टिंग करें" else "Instantly tests OCR, barcode queries & payment linkages offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Mode 1: Customer Account Quick Scan
                    if (customers.isNotEmpty()) {
                        var selectedCustIdx by remember { mutableStateOf(0) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (language == Language.HINDI) "1. ग्राहक बही स्कैनर कोड चुनें" else "1. Select Customer ID Code to Scan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Simple Selector Wheel
                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(38.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedCustIdx = (selectedCustIdx + 1) % customers.size
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val target = customers.getOrNull(selectedCustIdx)
                                    Text(
                                        text = target?.let { "${it.name} (Code: SmartKhata_Customer_${it.id})" } ?: "No Customers",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val cust = customers.getOrNull(selectedCustIdx)
                                        if (cust != null) {
                                            playBeep()
                                            viewModel.handleScanPayload("SmartKhata_Customer_${cust.id}", "QR") { toast ->
                                                Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = "scan", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == Language.HINDI) "स्कैन करें" else "Sim Scan", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Mode 2: Invoice Receipt Scan
                    if (transactions.isNotEmpty()) {
                        var selectedTxIdx by remember { mutableStateOf(0) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (language == Language.HINDI) "2. बिल/रसीद बारकोड चुनें" else "2. Select Bill Invoice to Scan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(38.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedTxIdx = (selectedTxIdx + 1) % transactions.size
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val target = transactions.getOrNull(selectedTxIdx)
                                    val cname = customers.find { it.id == target?.customerId }?.name ?: "Unknown"
                                    Text(
                                        text = target?.let { "Inv #${it.id} - Rs ${it.amount.toInt()} for $cname" } ?: "No Bills",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val tx = transactions.getOrNull(selectedTxIdx)
                                        if (tx != null) {
                                            playBeep()
                                            viewModel.handleScanPayload("SmartKhata_Invoice_${tx.id}", "BARCODE") { toast ->
                                                Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "scan", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == Language.HINDI) "स्कैन करें" else "Sim Scan", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Mode 3: Product Material Scan
                    if (products.isNotEmpty()) {
                        var selectedProdIdx by remember { mutableStateOf(0) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (language == Language.HINDI) "3. निर्माण सामग्री बारकोड" else "3. Select Construction Material Barcode",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(38.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedProdIdx = (selectedProdIdx + 1) % products.size
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val target = products.getOrNull(selectedProdIdx)
                                    Text(
                                        text = target?.let { "${it.name} (Barcode: ${it.barcode})" } ?: "No Products",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val prod = products.getOrNull(selectedProdIdx)
                                        if (prod != null) {
                                            playBeep()
                                            viewModel.handleScanPayload(prod.barcode ?: "", "BARCODE") { toast ->
                                                Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = "scan", modifier = Modifier.size(14.dp), tint = Color.DarkGray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == Language.HINDI) "प्रबंधित करें" else "Sim Scan", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // Mode 4: UPI Payment Settlement Emulation
                    var paymentAmountStr by remember { mutableStateOf("500") }
                    var payCustomerIdx by remember { mutableStateOf(0) }
                    if (customers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (language == Language.HINDI) "4. यूपीआई क्यूआर भुगतान सिम्युलेटर" else "4. UPI QR Payment Settlement Emulator",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Choose Customer making payment
                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(38.dp)
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(8.dp))
                                        .clickable {
                                            payCustomerIdx = (payCustomerIdx + 1) % customers.size
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val target = customers.getOrNull(payCustomerIdx)
                                    Text(
                                        text = target?.name ?: "Select Customer",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }

                                // Interactive amount picker
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(38.dp)
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(8.dp))
                                        .clickable {
                                            paymentAmountStr = when (paymentAmountStr) {
                                                "500" -> "1000"
                                                "1000" -> "5000"
                                                "5000" -> "12000"
                                                else -> "500"
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "₹$paymentAmountStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val cust = customers.getOrNull(payCustomerIdx)
                                        if (cust != null) {
                                            playBeep()
                                            val simulatedUpiPayload = "upi://pay?pa=smartkhata@axis&pn=Smart+Khata+Verified&am=$paymentAmountStr&tn=cust_${cust.id}-payment"
                                            viewModel.handleScanPayload(simulatedUpiPayload, "QR") { toast ->
                                                Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "pay", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pay", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * CameraX scanning implementation with ZXing binding
 */
@Composable
fun CameraScanPreview(
    flashOn: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Flashlight control
    LaunchedEffect(flashOn) {
        camera?.cameraControl?.enableTorch(flashOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor, ZXingBarcodeAnalyzer { code ->
                    onBarcodeDetected(code)
                })

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    camera?.cameraControl?.enableTorch(flashOn)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * ZXing Image Analysis binding class
 */
class ZXingBarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128)
        setHints(hints)
    }

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val width = image.width
        val height = image.height

        val source = PlanarYUVLuminanceSource(
            data, width, height, 0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decodeWithState(bitmap)
            onBarcodeScanned(result.text)
        } catch (e: Exception) {
            // Ignored
        } finally {
            reader.reset()
            image.close()
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}

/**
 * Laser Line Animation reticle overlay
 */
@Composable
fun ScannerDecorativeReticle(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_offset"
    )

    Box(
        modifier = modifier
            .drawBehind {
                val boxWidth = size.width * 0.7f
                val boxHeight = size.height * 0.6f
                val left = (size.width - boxWidth) / 2f
                val top = (size.height - boxHeight) / 2f

                // Reticle Outline
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.8f),
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Laser horizontal line
                val laserY = top + (boxHeight * laserOffsetY)
                drawLine(
                    color = Color.Red,
                    start = Offset(left + 8.dp.toPx(), laserY),
                    end = Offset(left + boxWidth - 8.dp.toPx(), laserY),
                    strokeWidth = 3.dp.toPx()
                )
            }
    )
}

@Composable
fun ScanLogsHistoryView(
    language: Language,
    scanLogs: List<ScanLog>,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (language == Language.HINDI) "इतिहास बही स्कैन" else "Audit Log Registry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == Language.HINDI) "स्कैनिंग का संपूर्ण लेखा-जोखा" else "Records trace reports of all ledger checkins",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            if (scanLogs.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "clear")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == Language.HINDI) "साफ करें" else "Clear All")
                }
            }
        }

        if (scanLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "empty",
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == Language.HINDI) "कोई स्कैन इतिहास नहीं है" else "No scan activity registered",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scanLogs) { log ->
                    ScanLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun ScanLogItem(log: ScanLog) {
    val formatter = remember { SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Identifier icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (log.payload.startsWith("upi://pay")) Color(0xFFEFF6FF)
                        else if (log.payload.startsWith("SmartKhata_Customer_")) Color(0xFFECFDF5)
                        else if (log.payload.startsWith("SmartKhata_Invoice_")) Color(0xFFFEF3C7)
                        else Color(0xFFF1F5F9)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.payload.startsWith("upi://pay")) Icons.Default.CurrencyRupee
                    else if (log.payload.startsWith("SmartKhata_Customer_")) Icons.Default.Person
                    else if (log.payload.startsWith("SmartKhata_Invoice_")) Icons.Default.ReceiptLong
                    else Icons.Default.Layers,
                    contentDescription = "type",
                    tint = if (log.payload.startsWith("upi://pay")) Color(0xFF2563EB)
                    else if (log.payload.startsWith("SmartKhata_Customer_")) PrimaryGreen
                    else if (log.payload.startsWith("SmartKhata_Invoice_")) Color(0xFFD97706)
                    else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Text Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.resolvedEntity,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Action: ${log.actionTaken}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = "Payload: ${log.payload}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Play a standard scan confirm tone
 */
private fun playBeep() {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
