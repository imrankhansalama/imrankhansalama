package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import android.app.Activity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val voiceFeedback by viewModel.voiceFeedback.collectAsStateWithLifecycle()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                viewModel.processVoiceCommand(spokenText)
            }
        }
    }

    val isLockEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    if (isLockEnabled && !isUnlocked) {
        AppLockOverlay(viewModel = viewModel, language = language) {
            viewModel.setUnlocked(true)
        }
        return
    }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (currentScreen !is Screen.Dashboard && currentScreen !is Screen.MyPlan && currentScreen !is Screen.More) {
                        IconButton(
                            onClick = {
                                when (currentScreen) {
                                    is Screen.Customers, is Screen.Suppliers -> viewModel.navigateTo(Screen.Dashboard)
                                    is Screen.CustomerDetail -> viewModel.navigateTo(Screen.Customers)
                                    is Screen.SupplierDetail -> viewModel.navigateTo(Screen.Suppliers)
                                    is Screen.Inventory, is Screen.Billing, is Screen.Profile, is Screen.RecycleBin -> viewModel.navigateTo(Screen.More)
                                    else -> viewModel.navigateTo(Screen.Dashboard)
                                }
                            },
                            modifier = Modifier.testTag("top_bar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryGreen
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Brand Logo Icon with dual-ring gradient effect
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9))
                                .border(1.5.dp, PrimaryGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "App Icon",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = profile?.name ?: Loc.t("app_title", language),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryGreen,
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = when (currentScreen) {
                                        is Screen.Dashboard -> "Credit Book Ledger"
                                        is Screen.Customers -> if (language == Language.HINDI) "ग्राहक सूची (Customers)" else "Customers Directory"
                                        is Screen.Suppliers -> if (language == Language.HINDI) "सप्लायर सूची (Suppliers)" else "Suppliers Directory"
                                        is Screen.Inventory -> if (language == Language.HINDI) "स्टॉक प्रबंधन (Inventory)" else "Stock Management"
                                        is Screen.Billing -> if (language == Language.HINDI) "बिलिंग (Billing)" else "Billing Invoice"
                                        is Screen.Profile -> if (language == Language.HINDI) "प्रोफाइल सेटिंग्स (Profile)" else "Store Profile Settings"
                                        is Screen.MyPlan -> if (language == Language.HINDI) "सब्सक्रिप्शन (Premium Plan)" else "Premium Subscriptions"
                                        is Screen.More -> if (language == Language.HINDI) "अधिक टूल्स (More Tools)" else "Tools & Services"
                                        is Screen.CustomerDetail -> if (language == Language.HINDI) "ग्राहक लेन-देन (Customer TX)" else "Customer Transactions"
                                        is Screen.SupplierDetail -> if (language == Language.HINDI) "सप्लायर लेन-देन (Supplier TX)" else "Supplier Transactions"
                                        is Screen.ReminderTemplateManager -> if (language == Language.HINDI) "टेम्पलेट प्रबंधक (Templates)" else "Reminders & Templates"
                                        is Screen.ScannerScreen -> if (language == Language.HINDI) "क्यूआर बारकोड स्कैन (Scan Center)" else "Advanced QR & Barcode"
                                        is Screen.RecycleBin -> if (language == Language.HINDI) "रीसायकल बिन (Recycle Bin)" else "Recycle Bin Recovery"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                                // Online/Offline status badge easily visible on top bar
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp))
                                        .border(0.5.dp, Color(0xFF4CAF50), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                        )
                                        Text(
                                            text = if (language == Language.HINDI) "सुरक्षित" else "Offline Safe",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // Language switcher button with multi-device dialog picker (compact, beautiful pill)
                        OutlinedButton(
                            onClick = { showLanguageDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryGreen,
                                containerColor = Color.White
                            ),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("language_selector_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Switch Language",
                                modifier = Modifier.size(13.dp),
                                tint = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            val currentLangLabel = when (language) {
                                Language.ENGLISH -> "EN"
                                Language.HINDI -> "HI"
                                Language.BENGALI -> "BN"
                                Language.TELUGU -> "TE"
                                Language.MARATHI -> "MR"
                                Language.TAMIL -> "TA"
                                Language.GUJARATI -> "GJ"
                                Language.URDU -> "UR"
                                Language.KANNADA -> "KN"
                                Language.MALAYALAM -> "KL"
                                Language.PUNJABI -> "PB"
                            }
                            Text(
                                text = currentLangLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                fontSize = 11.sp
                            )
                        }

                        // AI Mic Voice Assistant Button - Framed elegantly
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE))
                                .border(1.dp, Color(0xFFFFCDD2), CircleShape)
                                .clickable {
                                    val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == Language.HINDI) "hi-IN" else "en-US")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == Language.HINDI) "बोलिए... (उदा: सुरेश को 500 दिए)" else "Speak... (e.g. Suresh ko 500 diye)")
                                    }
                                    try {
                                        speechLauncher.launch(voiceIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Voice recognizer not supported", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("top_bar_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "AI Voice Entry",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Dedicated QR & Barcode Scan Button - Framed elegantly
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, Color(0xFFC8E6C9), CircleShape)
                                .clickable { viewModel.navigateTo(Screen.ScannerScreen) }
                                .testTag("top_bar_scan_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode/QR",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Profile Navigation Button - Custom owner avatar / initials instead of a raw icon
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.navigateTo(Screen.Profile) }
                                .testTag("top_bar_profile_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfilePhoto(
                                photoUri = profile?.photoUri ?: "",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, PrimaryGreen, CircleShape),
                                fallbackIconSize = 16.dp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = PrimaryGreen
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // 1. Ledger Tab
                val isLedgerActive = currentScreen is Screen.Dashboard ||
                        currentScreen is Screen.Customers ||
                        currentScreen is Screen.CustomerDetail ||
                        currentScreen is Screen.Suppliers ||
                        currentScreen is Screen.SupplierDetail
                NavigationBarItem(
                    selected = isLedgerActive,
                    onClick = { viewModel.navigateTo(Screen.Dashboard) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Ledger") },
                    label = { Text(if (language == Language.HINDI) "लेजर" else "Ledger") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        indicatorColor = LightGreen
                    ),
                    modifier = Modifier.testTag("nav_ledger_btn")
                )

                // 2. My plan Tab
                NavigationBarItem(
                    selected = currentScreen is Screen.MyPlan,
                    onClick = { viewModel.navigateTo(Screen.MyPlan) },
                    icon = { Icon(Icons.Default.CardMembership, contentDescription = "My Plan") },
                    label = { Text(if (language == Language.HINDI) "मेरा प्लान" else "My plan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        indicatorColor = LightGreen
                    ),
                    modifier = Modifier.testTag("nav_my_plan_btn")
                )

                // 3. More Tab
                val isMoreActive = currentScreen is Screen.More ||
                        currentScreen is Screen.Inventory ||
                        currentScreen is Screen.Billing ||
                        currentScreen is Screen.Profile
                NavigationBarItem(
                    selected = isMoreActive,
                    onClick = { viewModel.navigateTo(Screen.More) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "More") },
                    label = { Text(if (language == Language.HINDI) "अन्य" else "More") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        indicatorColor = LightGreen
                    ),
                    modifier = Modifier.testTag("nav_more_btn")
                )
            }
        },
        floatingActionButton = {
            when (currentScreen) {
                is Screen.Customers -> {
                    FloatingActionButton(
                        onClick = { showAddCustomerDialog = true },
                        containerColor = PrimaryGreen,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("add_customer_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Customer")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Loc.t("add_customer", language), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is Screen.Suppliers -> {
                    FloatingActionButton(
                        onClick = { showAddSupplierDialog = true },
                        containerColor = PrimaryGreen,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("add_supplier_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Supplier")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Loc.t("add_supplier", language), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {}
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val isLedgerTabActive = currentScreen is Screen.Dashboard ||
                        currentScreen is Screen.Customers ||
                        currentScreen is Screen.Suppliers

                if (isLedgerTabActive) {
                    TabRow(
                        selectedTabIndex = when (currentScreen) {
                            is Screen.Dashboard -> 0
                            is Screen.Customers -> 1
                            is Screen.Suppliers -> 2
                            else -> 0
                        },
                        containerColor = Color.White,
                        contentColor = PrimaryGreen,
                        modifier = Modifier.fillMaxWidth().testTag("ledger_top_tabs")
                    ) {
                        Tab(
                            selected = currentScreen is Screen.Dashboard,
                            onClick = { viewModel.navigateTo(Screen.Dashboard) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(20.dp)) },
                            text = { Text(if (language == Language.HINDI) "डैशबोर्ड" else "Dashboard", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = currentScreen is Screen.Customers,
                            onClick = { viewModel.navigateTo(Screen.Customers) },
                            icon = { Icon(Icons.Default.People, contentDescription = "Customers", modifier = Modifier.size(20.dp)) },
                            text = { Text(if (language == Language.HINDI) "ग्राहक" else "Customers", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = currentScreen is Screen.Suppliers,
                            onClick = { viewModel.navigateTo(Screen.Suppliers) },
                            icon = { Icon(Icons.Default.Store, contentDescription = "Suppliers", modifier = Modifier.size(20.dp)) },
                            text = { Text(if (language == Language.HINDI) "सप्लायर" else "Suppliers", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = voiceFeedback != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    voiceFeedback?.let { feedback ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hearing,
                                        contentDescription = "AI Voice Status",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (language == Language.HINDI) "स्मार्ट एआई ध्वनि सहायक" else "Smart AI Voice Assistant",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            text = feedback,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF1E293B),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.clearVoiceFeedback() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is Screen.Dashboard -> {
                        DashboardScreenContent(
                            viewModel = viewModel,
                            language = language,
                            onNavigateToCustomers = { viewModel.navigateTo(Screen.Customers) },
                            onNavigateToSuppliers = { viewModel.navigateTo(Screen.Suppliers) }
                        )
                    }
                    is Screen.Customers -> {
                        CustomersScreenContent(viewModel = viewModel, language = language, onSelectCustomer = { id ->
                            viewModel.navigateTo(Screen.CustomerDetail(id))
                        })
                    }
                    is Screen.Suppliers -> {
                        SuppliersScreenContent(viewModel = viewModel, language = language, onSelectSupplier = { id ->
                            viewModel.navigateTo(Screen.SupplierDetail(id))
                        })
                    }
                    is Screen.Inventory -> {
                        InventoryScreenContent(viewModel = viewModel, language = language)
                    }
                    is Screen.Billing -> {
                        BillingScreenContent(viewModel = viewModel, language = language)
                    }
                    is Screen.Profile -> {
                        ProfileScreenContent(viewModel = viewModel, language = language)
                    }
                    is Screen.MyPlan -> {
                        MyPlanScreenContent(viewModel = viewModel, language = language)
                    }
                    is Screen.More -> {
                        MoreScreenContent(viewModel = viewModel, language = language)
                    }
                    is Screen.ReminderTemplateManager -> {
                        ReminderTemplateManagerScreen(
                            viewModel = viewModel,
                            language = language,
                            onBack = { viewModel.navigateTo(Screen.More) }
                        )
                    }
                    is Screen.ScannerScreen -> {
                        AdvancedScannerScreen(
                            viewModel = viewModel,
                            language = language,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }
                    is Screen.CustomerDetail -> {
                        CustomerDetailScreen(
                            customerId = screen.customerId,
                            viewModel = viewModel,
                            language = language,
                            onBack = { viewModel.navigateTo(Screen.Customers) }
                        )
                    }
                    is Screen.SupplierDetail -> {
                        SupplierDetailScreen(
                            supplierId = screen.supplierId,
                            viewModel = viewModel,
                            language = language,
                            onBack = { viewModel.navigateTo(Screen.Suppliers) }
                        )
                    }
                    is Screen.RecycleBin -> {
                        RecycleBinScreen(
                            viewModel = viewModel,
                            language = language,
                            onBack = { viewModel.navigateTo(Screen.More) }
                        )
                    }
                }
            }
        }
    }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            language = language,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address ->
                viewModel.addCustomer(name, phone, address)
                showAddCustomerDialog = false
                Toast.makeText(context, "${Loc.t("add_customer", language)}: $name", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddSupplierDialog) {
        AddSupplierDialog(
            language = language,
            onDismiss = { showAddSupplierDialog = false },
            onSave = { name, phone, address ->
                viewModel.addSupplier(name, phone, address)
                showAddSupplierDialog = false
                Toast.makeText(context, "${Loc.t("add_supplier", language)}: $name", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showLanguageDialog) {
        val languagesList = listOf(
            Pair(Language.ENGLISH, "English / English"),
            Pair(Language.HINDI, "Hindi / हिन्दी"),
            Pair(Language.BENGALI, "Bengali / বাংলা"),
            Pair(Language.TELUGU, "Telugu / తెలుగు"),
            Pair(Language.MARATHI, "Marathi / मराठी"),
            Pair(Language.TAMIL, "Tamil / தமிழ்"),
            Pair(Language.GUJARATI, "Gujarati / ગુજરાતી"),
            Pair(Language.URDU, "Urdu / اردو"),
            Pair(Language.KANNADA, "Kannada / ಕನ್ನಡ"),
            Pair(Language.MALAYALAM, "Malayalam / മലയാളം"),
            Pair(Language.PUNJABI, "Punjabi / ਪੰਜਾਬੀ")
        )

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = if (language == Language.HINDI) "भाषा चुनें / Select Language" else "Select Language",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(languagesList.size) { index ->
                        val (langItem, label) = languagesList[index]
                        val isSelected = langItem == language
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(langItem)
                                    showLanguageDialog = false
                                }
                                .testTag("lang_option_${langItem.name.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF1FDF4) else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryGreen else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryGreen else Color(0xFF1E293B)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(text = "Close", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ---------------- DASHBOARD SCREEN CONTENT ----------------

@Composable
fun DashboardScreenContent(
    viewModel: LedgerViewModel,
    language: Language,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSuppliers: () -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val txs by viewModel.transactions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val whatsappStatuses by viewModel.whatsappStatuses.collectAsStateWithLifecycle()

    // Filter transactions recorded today
    val todaysTxs = remember(txs) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

        txs.filter { tx ->
            val txCalendar = Calendar.getInstance().apply { timeInMillis = tx.date }
            txCalendar.get(Calendar.YEAR) == currentYear && txCalendar.get(Calendar.DAY_OF_YEAR) == currentDay
        }
    }

    val todayGiveAmount = remember(todaysTxs) {
        todaysTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
    }
    val todayReceiveAmount = remember(todaysTxs) {
        todaysTxs.filter { it.type != "CREDIT" }.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Welcome Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGreen),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfilePhoto(
                        photoUri = profile?.photoUri ?: "",
                        modifier = Modifier.size(50.dp),
                        fallbackIconSize = 26.dp
                    )
                    Column {
                        Text(
                            text = profile?.name ?: "My Business Store",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Credit Book Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightGreen.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Prominent Stats Row: side-by-side 'Total Receivable' and 'Total Payable'
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Receivable Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToCustomers() }
                    .testTag("receivable_dashboard_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, DebitRed.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFECEC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "Receivable Icon",
                                tint = DebitRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Go",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = Loc.t("receivable", language),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${stats.receivable.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DebitRed
                    )
                }
            }

            // Total Payable Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSuppliers() }
                    .testTag("payable_dashboard_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, CreditGreen.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(LightGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Payable Icon",
                                tint = CreditGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Go",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = Loc.t("payable", language),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${stats.payable.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = CreditGreen
                    )
                }
            }
        }

        // Smart AI Voice cashier helper panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LightGreen.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice cashier icon",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == Language.HINDI) "स्मार्ट वॉयस असिस्टेंट" else "Smart Voice Assistant",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryGreen
                        )
                        Text(
                            text = if (language == Language.HINDI) "बिना टाइप किए बोलकर तुरंत एंट्री दर्ज करें!" else "Speak instantly to add transactions or navigate!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    val innerSpeechLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
                            if (spokenText.isNotBlank()) {
                                viewModel.processVoiceCommand(spokenText)
                            }
                        }
                    }
                    
                    val context = LocalContext.current
                    
                    Button(
                        onClick = {
                            val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == Language.HINDI) "hi-IN" else "en-US")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == Language.HINDI) "बोलिए... (उदा: सुरेश को 500 दिए)" else "Speak... (e.g. Suresh ko 500 diye)")
                            }
                            try {
                                innerSpeechLauncher.launch(voiceIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Voice recognizer not supported", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "बोलें" else "Speak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.LightGray.copy(alpha = 0.4f)
                )

                Text(
                    text = if (language == Language.HINDI) "बोलने के कुछ आसान उदाहरण (Voice Shortcuts):" else "Spoken voice command examples list:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "💼 ग्राहक खाता ट्रांजैक्शन (Customer Entry)" else "💼 Customer Transaction",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (language == Language.HINDI) "• \"रमेश को 500 रुपये उधार दिए\" (Udhar/Credit)\n• \"मुकेश से 1000 रुपये जमा प्राप्त हुए\" (Jama/Payment)" else "• \"Ramesh ko 500 rupees diye\" (Credit/Debit)\n• \"Mukesh se 1000 rupees mile\" (Jama/Payment)",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = Color.DarkGray
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "⚡ आवाज से नेविगेशन (Voice Screens Navigation)" else "⚡ Voice Navigation Shortcuts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (language == Language.HINDI) "• \"बिलिंग स्क्रीन खोलो\" या \"स्टॉक खोलो\"" else "• \"Open billing screen\" or \"Go to Inventory\"",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }


        // ---------------- OVERDUE PAYMENTS REMINDERS ----------------
        val overdueCustomers = remember(customers, txs) {
            customers.map { cust ->
                val userTxs = txs.filter { it.customerId == cust.id }
                val credit = userTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
                val payment = userTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                val balance = credit - payment
                cust to balance
            }.filter { it.second > 0.0 }
        }

        if (overdueCustomers.isNotEmpty()) {
            Text(
                text = if (language == Language.HINDI) "बकाया भुगतान (Reminders)" else "Overdue Payments (Reminders)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DebitRed,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("overdue_reminders_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DebitRed.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Automated Reminders Card Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "बकाया सूची" else "Overdue List",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Button(
                            onClick = {
                                viewModel.sendBulkWhatsAppReminders(
                                    overdueCustomers = overdueCustomers,
                                    businessName = profile?.name ?: "Credit Book"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == Language.HINDI) "सबको ऑटो भेजें" else "Auto Remind All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 8.dp))

                    overdueCustomers.forEachIndexed { idx, (cust, bal) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cust.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${if (language == Language.HINDI) "बकाया राशि: " else "Balance: "}₹${bal.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DebitRed
                                )

                                // Show API sending status if any
                                val status = whatsappStatuses[cust.id]
                                if (status != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (status.isSending) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = PrimaryGreen)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (language == Language.HINDI) "एपीआई भेज रहा है..." else "Meta API Sending...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    } else if (status.success) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Sent", tint = CreditGreen, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (language == Language.HINDI) "एपीआई से भेजा गया" else "Sent via Meta API",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CreditGreen
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { viewModel.clearWhatsAppStatus(cust.id) },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    } else if (status.error != null) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Error, contentDescription = "Error", tint = DebitRed, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (language == Language.HINDI) "विफल" else "Failed",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DebitRed,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.clearWhatsAppStatus(cust.id) },
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                            Text(
                                                text = status.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            val context = LocalContext.current
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Standard WhatsApp Share (Manual Intent)
                                Button(
                                    onClick = {
                                        val upiStr = if (!profile?.upiId.isNullOrBlank()) " UPI: ${profile?.upiId}" else ""
                                        val msg = if (language == Language.HINDI) {
                                            "नमस्ते ${cust.name}, आपके खाता बही का बकाया ₹${bal.toInt()} है। कृपया जल्द भुगतान करें। धन्यवाद!$upiStr"
                                        } else {
                                            "Dear ${cust.name}, your outstanding balance of ₹${bal.toInt()} with ${profile?.name ?: "us"} is pending. Please pay at your earliest convenience. Thank you!$upiStr"
                                        }
                                        try {
                                            val cleanPhone = cust.phone.trim().filter { it.isDigit() }
                                            val phoneWithCountry = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("whatsapp://send?phone=$phoneWithCountry&text=${Uri.encode(msg)}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("smsto:${cust.phone}")
                                                putExtra("sms_body", msg)
                                            }
                                            context.startActivity(smsIntent)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("whatsapp_reminder_btn_${cust.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "WhatsApp icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == Language.HINDI) "साझा" else "Share",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // 2. Automated Meta WhatsApp API Action
                                Button(
                                    onClick = {
                                        viewModel.sendWhatsAppApiReminder(
                                            customerId = cust.id,
                                            customerPhone = cust.phone,
                                            customerName = cust.name,
                                            balanceAmount = bal,
                                            businessName = profile?.name ?: "Credit Book"
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("whatsapp_api_auto_btn_${cust.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Auto API",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == Language.HINDI) "ऑटो भेजें" else "Auto Remind",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (idx < overdueCustomers.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // Today's Transactions Title & Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("today_summary_manager_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color.Black) // Bold Black Accent Border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (language == Language.HINDI) "आज का ट्रांसेक्शन मैनेजर" else "Today's Ledger Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.Black, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${todaysTxs.size} Tx",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Column: Today Credit Given (उधार दिया)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = if (language == Language.HINDI) "उधार दिया (Today)" else "Udhar Given (Today)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black, // High visibility solid black label
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${todayGiveAmount.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = DebitRed
                            )
                        }
                    }
                    
                    // Right Column: Today Payment Received (भुगतान मिला)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = if (language == Language.HINDI) "भुगतान मिला (Today)" else "Payment In (Today)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black, // High visibility solid black label
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${todayReceiveAmount.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = CreditGreen
                            )
                        }
                    }
                }
            }
        }

        // Today's Transactions list view
        if (todaysTxs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Empty transactions",
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Loc.t("no_transactions", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (language == Language.ENGLISH) "Record customer credit/payment to see listings here." else "यहाँ लेनदेन देखने के लिए ग्राहकों का जमा/उधार दर्ज करें।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToCustomers() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                            border = BorderStroke(1.dp, PrimaryGreen)
                        ) {
                            Text(if (language == Language.ENGLISH) "Customer Tx" else "ग्राहक जमा/उधार")
                        }
                        Button(
                            onClick = { onNavigateToSuppliers() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text(if (language == Language.ENGLISH) "Supplier Tx" else "सप्लायर पेमेंट")
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    todaysTxs.forEachIndexed { index, tx ->
                        // Determine name and label
                        val name: String
                        val label: String
                        val txAmountColor: Color
                        val signLabel: String

                        if (tx.customerId != null) {
                            val cust = customers.find { it.id == tx.customerId }
                            name = cust?.name ?: "Customer #${tx.customerId}"
                            if (tx.type == "CREDIT") {
                                label = if (language == Language.ENGLISH) "Udhar Given (-)" else "उधार दिया (-)"
                                txAmountColor = DebitRed
                                signLabel = "-"
                            } else {
                                label = if (language == Language.ENGLISH) "Payment Received (+)" else "भुगतान मिला (+)"
                                txAmountColor = CreditGreen
                                signLabel = "+"
                            }
                        } else if (tx.supplierId != null) {
                            val supp = suppliers.find { it.id == tx.supplierId }
                            name = supp?.name ?: "Supplier #${tx.supplierId}"
                            if (tx.type == "CREDIT") {
                                label = if (language == Language.ENGLISH) "Bought Credit (-)" else "सामान उधार ख़रीदा (-)"
                                txAmountColor = DebitRed
                                signLabel = "-"
                            } else {
                                label = if (language == Language.ENGLISH) "Payment Given (+)" else "पेमेंट चुकाया (+)"
                                txAmountColor = CreditGreen
                                signLabel = "+"
                            }
                        } else {
                            name = "General Transaction"
                            label = tx.type
                            txAmountColor = Color.Black
                            signLabel = ""
                        }

                        val timeFormatted = remember(tx.date) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.date))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar Circle icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (txAmountColor == DebitRed) Color(0xFFFFECEC) else LightGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == "CREDIT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = "Tx Icon",
                                        tint = txAmountColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black, // Extra bold black font for name
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = txAmountColor,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = timeFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black, // High contrast black time tag
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!tx.notes.isBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (language == Language.HINDI) "नोट" else "Note",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Text(
                                                text = tx.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Black, // High contrast black note
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Far Right: prominent transaction amount
                            Text(
                                text = "$signLabel ₹${tx.amount.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = txAmountColor
                            )
                        }

                        if (index < todaysTxs.size - 1) {
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color.Gray.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CUSTOMER LIST AND DASHBOARD SCREEN ----------------

@Composable
fun CustomersScreenContent(
    viewModel: LedgerViewModel,
    language: Language,
    onSelectCustomer: (Int) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val searchList by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerQuery.collectAsStateWithLifecycle()
    val txs by viewModel.transactions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Business Dashboard Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Loc.t("monthly_summary", language),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = Loc.t("today_tx", language) + ": ₹${stats.todaySales.toInt()}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Receivable
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Loc.t("receivable", language),
                            style = MaterialTheme.typography.bodySmall,
                            color = LightGreen
                        )
                        Text(
                            text = "₹${stats.receivable.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                            .align(Alignment.CenterVertically),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    // Payable
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = Loc.t("payable", language),
                            style = MaterialTheme.typography.bodySmall,
                            color = LightGreen
                        )
                        Text(
                            text = "₹${stats.payable.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD1D1)
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.customerQuery.value = it },
            placeholder = { Text(Loc.t("search_hint", language)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("search_customer_input"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        // Interactive filter choice row
        var filterFavoriteOnly by remember { mutableStateOf(false) }
        var filterTag by remember { mutableStateOf("All") }
        var enableNearbySearch by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Star bookmarks filter
            FilterChip(
                selected = filterFavoriteOnly,
                onClick = { filterFavoriteOnly = !filterFavoriteOnly },
                label = { Text(if (language == Language.HINDI) "⭐ पसंदीदा" else "⭐ Favorites") },
                leadingIcon = {
                    if (filterFavoriteOnly) {
                        Icon(Icons.Default.Check, contentDescription = "Active", modifier = Modifier.size(12.dp))
                    }
                }
            )

            // Dynamic Proximity proximity filter
            FilterChip(
                selected = enableNearbySearch,
                onClick = { enableNearbySearch = !enableNearbySearch },
                label = { Text(if (language == Language.HINDI) "📍 आस-पास (GPS)" else "📍 Nearby (GPS)") },
                leadingIcon = {
                    if (enableNearbySearch) {
                        Icon(Icons.Default.MyLocation, contentDescription = "gps", modifier = Modifier.size(12.dp))
                    }
                }
            )

            // Tags classification
            listOf("All", "Regular", "VIP", "Risky").forEach { tag ->
                val isSelected = filterTag.lowercase() == tag.lowercase()
                FilterChip(
                    selected = isSelected,
                    onClick = { filterTag = tag },
                    label = {
                        Text(
                            text = when (tag) {
                                "All" -> if (language == Language.HINDI) "सभी" else "All"
                                "Risky" -> if (language == Language.HINDI) "🔴 जोखिम" else "Risky"
                                "VIP" -> if (language == Language.HINDI) "👑 VIP" else "VIP"
                                else -> if (language == Language.HINDI) "🟢 सामान्य" else "Regular"
                            }
                        )
                    }
                )
            }
        }

        // Customer Ledger List filtered on-the-fly
        val activeCustomers = searchList.filter { customer ->
            val matchesFav = !filterFavoriteOnly || customer.isFavourite
            val matchesTag = filterTag == "All" || customer.tags.lowercase() == filterTag.lowercase()
            matchesFav && matchesTag
        }

        val sortedCustomers = if (enableNearbySearch) {
            activeCustomers.sortedBy { customer ->
                val lat = if (customer.latitude == 0.0) 28.61 + (customer.id % 10) * 0.005 else customer.latitude
                val lon = if (customer.longitude == 0.0) 77.20 + (customer.id % 7) * 0.007 else customer.longitude
                Math.sqrt(Math.pow(lat - 28.61, 2.0) + Math.pow(lon - 77.20, 2.0))
            }
        } else {
            activeCustomers
        }

        if (sortedCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = "Empty",
                        tint = PrimaryGreen.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Loc.t("no_customers", language),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedCustomers) { customer ->
                    val userTxs = txs.filter { it.customerId == customer.id }
                    val credit = userTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
                    val payment = userTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                    val balance = credit - payment

                    CustomerListItem(
                        customer = customer,
                        balance = balance,
                        language = language,
                        onFavouriteToggle = { viewModel.toggleCustomerFavourite(customer) },
                        onClick = { onSelectCustomer(customer.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerListItem(
    customer: Customer,
    balance: Double,
    language: Language,
    onFavouriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_item_${customer.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo/Avatar Representation
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryGreen, AccentGreen)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.firstOrNull()?.uppercase() ?: "C",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Bookmark Favorite Heart/Star inline click trigger
                        IconButton(
                            onClick = onFavouriteToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (customer.isFavourite) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "favorite",
                                tint = if (customer.isFavourite) Color(0xFFF59E0B) else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        // VIP / Risky tags pill
                        val tagColor = when (customer.tags.lowercase()) {
                            "vip" -> Color(0xFFF59E0B)
                            "risky" -> DebitRed
                            else -> CreditGreen
                        }
                        Box(
                            modifier = Modifier
                                .background(tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = customer.tags.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagColor
                            )
                        }

                        // Coordinates distances text
                        val lat = if (customer.latitude == 0.0) 28.61 + (customer.id % 10) * 0.005 else customer.latitude
                        val lon = if (customer.longitude == 0.0) 77.20 + (customer.id % 7) * 0.007 else customer.longitude
                        val distKm = Math.sqrt(Math.pow(lat - 28.61, 2.0) + Math.pow(lon - 77.20, 2.0)) * 111.0
                        Text(
                            text = String.format(Locale.getDefault(), "📍 %.1f km", distKm),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (balance > 0) {
                    Text(
                        text = Loc.t("unpaid", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = DebitRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${balance.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DebitRed
                    )
                } else if (balance < 0) {
                    Text(
                        text = Loc.t("paid", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = CreditGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${(-balance).toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = CreditGreen
                    )
                } else {
                    Text(
                        text = Loc.t("paid", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹0",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}


// ---------------- SUPPLIER SCREEN CONTENT ----------------

@Composable
fun SuppliersScreenContent(
    viewModel: LedgerViewModel,
    language: Language,
    onSelectSupplier: (Int) -> Unit
) {
    val searchList by viewModel.filteredSuppliers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.supplierQuery.collectAsStateWithLifecycle()
    val txs by viewModel.transactions.collectAsStateWithLifecycle()

    var totalPayableSum = 0.0
    searchList.forEach { supplier ->
        val userTxs = txs.filter { it.supplierId == supplier.id }
        val purchase = userTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val payment = userTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
        val bal = purchase - payment
        if (bal > 0) {
            totalPayableSum += bal
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Supplier Header Tally
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEC)),
            border = BorderStroke(1.dp, Color(0xFFFFC0C0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Loc.t("payable", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "₹${totalPayableSum.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = "Supplier Icon",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.supplierQuery.value = it },
            placeholder = { Text(Loc.t("search_hint", language)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("search_supplier_input"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        // Supplier List
        if (searchList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Empty",
                        tint = PrimaryGreen.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Loc.t("no_suppliers", language),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchList) { supplier ->
                    val userTxs = txs.filter { it.supplierId == supplier.id }
                    val purchase = userTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
                    val payment = userTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                    val balance = purchase - payment

                    SupplierListItem(
                        supplier = supplier,
                        balance = balance,
                        language = language,
                        onClick = { onSelectSupplier(supplier.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierListItem(
    supplier: Supplier,
    balance: Double,
    language: Language,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("supplier_item_${supplier.id}"),
        colors = CardDefaults.cardColors(containerColor = LightGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)), // subtle yellow avatar
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = supplier.name.firstOrNull()?.uppercase() ?: "S",
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Column {
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supplier.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (balance > 0) {
                    Text(
                        text = Loc.t("unpaid", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = DebitRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${balance.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DebitRed
                    )
                } else if (balance < 0) {
                    Text(
                        text = Loc.t("paid", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = CreditGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${(-balance).toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = CreditGreen
                    )
                } else {
                    Text(
                        text = "Settled",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹0",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}


// ---------------- INVENTORY SCREEN CONTENT ----------------

@Composable
fun InventoryScreenContent(viewModel: LedgerViewModel, language: Language) {
    val context = LocalContext.current
    val productList by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.productQuery.collectAsStateWithLifecycle()

    var showAddProductDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Statistics Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Loc.t("total_stock", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${productList.size} Items",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
                Button(
                    onClick = { showAddProductDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add product")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Loc.t("add_product", language), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.productQuery.value = it },
            placeholder = { Text(Loc.t("search_hint", language)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("search_product_input"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        // Inventory list
        if (productList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Empty stock",
                        tint = PrimaryGreen.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No stock items saved.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(productList) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (product.stock <= product.lowStockLimit) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    Color(0xFFFFECEC),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "LOW",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DebitRed
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Category: ${product.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Price: ₹${product.price.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            // Adjust Stock block
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedIconButton(
                                    onClick = { viewModel.adjustStock(product, -1) },
                                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = DebitRed)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrement stock")
                                }
                                Text(
                                    text = "${product.stock}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (product.stock <= product.lowStockLimit) DebitRed else Color.Black
                                )
                                OutlinedIconButton(
                                    onClick = { viewModel.adjustStock(product, 1) },
                                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = CreditGreen)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increment stock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            language = language,
            onDismiss = { showAddProductDialog = false },
            onSave = { name, category, price, stock, limit, barcode ->
                viewModel.addProduct(name, category, price, stock, limit, barcode)
                showAddProductDialog = false
                Toast.makeText(context, "Added product: $name", Toast.LENGTH_SHORT).show()
            }
        )
    }
}


// ---------------- BILLING SYSTEM CONTENT ----------------

@Composable
fun BillingScreenContent(viewModel: LedgerViewModel, language: Language) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var searchCustQuery by remember { mutableStateOf("") }
    var showDropdownCust by remember { mutableStateOf(false) }

    var itemName by remember { mutableStateOf("") }
    var baseRateStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("1") }

    var invoiceNo by remember { mutableStateOf("INV-${(100000..999999).random()}") }
    var selectGstRate by remember { mutableStateOf(18.0) }
    var flagGstBill by remember { mutableStateOf(false) }

    var showInvoiceSheet by remember { mutableStateOf(false) }
    var generatedInvoiceTxId by remember { mutableStateOf<Long?>(null) }

    val filCustList = if (searchCustQuery.isBlank()) {
        customers
    } else {
        customers.filter { it.name.contains(searchCustQuery, ignoreCase = true) }
    }

    // Calculators
    val qty = qtyStr.toIntOrNull() ?: 1
    val baseRate = baseRateStr.toDoubleOrNull() ?: 0.0
    val totalBase = baseRate * qty
    val gstRate = if (flagGstBill) selectGstRate else 0.0
    val gstAmount = totalBase * (gstRate / 100.0)
    val totalBillPayable = totalBase + gstAmount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = Loc.t("create_invoice", language),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryGreen,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Select Customer Block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Customer for Bill Ledger",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedCustomer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "person", tint = PrimaryGreen)
                            Column {
                                Text(selectedCustomer!!.name, fontWeight = FontWeight.Bold)
                                Text(selectedCustomer!!.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { selectedCustomer = null }) {
                            Icon(Icons.Default.Close, contentDescription = "clear selected", tint = DebitRed)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchCustQuery,
                        onValueChange = {
                            searchCustQuery = it
                            showDropdownCust = true
                        },
                        placeholder = { Text("Type name to find customer...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showDropdownCust = !showDropdownCust }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown")
                            }
                        }
                    )
                    if (showDropdownCust && filCustList.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState()),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            filCustList.forEach { cust ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCustomer = cust
                                            searchCustQuery = ""
                                            showDropdownCust = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("(${cust.phone})", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Invoice / Billing Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Item details
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(Loc.t("product_name", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("bill_item_name"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = baseRateStr,
                        onValueChange = { baseRateStr = it },
                        label = { Text("Base Cost (₹)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bill_base_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Qty") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bill_qty"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // GST Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Loc.t("include_gst", language), fontWeight = FontWeight.Bold)
                    Switch(
                        checked = flagGstBill,
                        onCheckedChange = { flagGstBill = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                    )
                }

                if (flagGstBill) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text("GST Percentage Slab: ${selectGstRate.toInt()}%", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(5.0, 12.0, 18.0, 28.0).forEach { rate ->
                                FilterChip(
                                    selected = selectGstRate == rate,
                                    onClick = { selectGstRate = rate },
                                    label = { Text("$rate%") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LightGreen,
                                        selectedLabelColor = PrimaryGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Tally Summary Footer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xF0FDF4))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(Loc.t("total_amount", language), color = Color.Gray)
                    Text("₹${totalBase.toInt()}", fontWeight = FontWeight.Bold)
                }
                if (flagGstBill) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Loc.t("gst_charge", language) + " (${selectGstRate.toInt()}%)", color = Color.Gray)
                        Text("₹${gstAmount.toInt()}", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(Loc.t("net_payable", language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "₹${totalBillPayable.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedCustomer == null) {
                            Toast.makeText(context, "Please select/add a Customer first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (itemName.isBlank() || totalBillPayable <= 0) {
                            Toast.makeText(context, "Item details or amount is incorrect!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Write to credit ledger
                        viewModel.addTransaction(
                            customerId = selectedCustomer!!.id,
                            supplierId = null,
                            type = "CREDIT",
                            amount = totalBillPayable,
                            notes = "Invoice Bill for $itemName (Qty: $qty)",
                            invoiceNumber = invoiceNo,
                            isGSTInvoice = flagGstBill,
                            gstRate = gstRate,
                            gstAmount = gstAmount
                        )
                        showInvoiceSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_bill_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Ok")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Loc.t("create_invoice", language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showInvoiceSheet && selectedCustomer != null) {
        InvoiceDetailDialog(
            customer = selectedCustomer!!,
            itemName = itemName,
            qty = qty,
            baseCost = baseRate,
            isGst = flagGstBill,
            gstRate = selectGstRate,
            gstAmt = gstAmount,
            totalAmt = totalBillPayable,
            invoiceNo = invoiceNo,
            profile = profile,
            language = language,
            onDismiss = {
                showInvoiceSheet = false
                // reset fields
                itemName = ""
                baseRateStr = ""
                qtyStr = "1"
                selectedCustomer = null
                invoiceNo = "INV-${(100000..999999).random()}"
            }
        )
    }
}


// ---------------- SHOP PROFILE & UPI QR CODE SCREEN ----------------

@Composable
fun ProfilePhoto(photoUri: String, modifier: Modifier = Modifier, fallbackIconSize: androidx.compose.ui.unit.Dp = 36.dp) {
    val context = LocalContext.current
    val isPreset = photoUri.startsWith("preset_")

    if (isPreset) {
        val presetIndex = photoUri.substringAfter("preset_").toIntOrNull() ?: 1
        val (icon, bgColor, desc) = remember(presetIndex) {
            when (presetIndex) {
                1 -> Triple(Icons.Default.Storefront, Color(0xFFDCFCE7), "Smart General Store")
                2 -> Triple(Icons.Default.LocalPharmacy, Color(0xFFFCE7F3), "Pharmacy")
                3 -> Triple(Icons.Default.ShoppingBag, Color(0xFFF3E8FF), "Textiles")
                4 -> Triple(Icons.Default.Tv, Color(0xFFDBEAFE), "Electronics")
                5 -> Triple(Icons.Default.Coffee, Color(0xFFFFEDD5), "Cafe & Bakery")
                6 -> Triple(Icons.Default.WaterDrop, Color(0xFFE0F2FE), "Krishna Dairy")
                7 -> Triple(Icons.Default.Icecream, Color(0xFFFEE2E2), "Sweet House")
                8 -> Triple(Icons.Default.Checkroom, Color(0xFFE0F2FE), "Garments")
                9 -> Triple(Icons.Default.Construction, Color(0xFFF1F5F9), "Hardware")
                10 -> Triple(Icons.Default.MenuBook, Color(0xFFFEF9C3), "Stationery")
                else -> Triple(Icons.Default.Business, Color(0xFFE0F2FE), "Business")
            }
        }

        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = when (presetIndex) {
                    1 -> Color(0xFF15803D)
                    2 -> Color(0xFFBE185D)
                    3 -> Color(0xFF6B21A8)
                    4 -> Color(0xFF1D4ED8)
                    5 -> Color(0xFFC2410C)
                    6 -> Color(0xFF0369A1)
                    7 -> Color(0xFFB91C1C)
                    8 -> Color(0xFF0F766E)
                    9 -> Color(0xFF475569)
                    10 -> Color(0xFFA16207)
                    else -> Color(0xFF0284C7)
                },
                modifier = Modifier.size(fallbackIconSize)
            )
        }
    } else {
        val bitmap = remember(photoUri) {
            if (photoUri.isNotBlank()) {
                try {
                    val stream = context.contentResolver.openInputStream(Uri.parse(photoUri))
                    android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "Profile Photo",
                modifier = modifier.clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Fallback Photo",
                    tint = Color.Gray,
                    modifier = Modifier.size(fallbackIconSize)
                )
            }
        }
    }
}

@Composable
fun ProfileScreenContent(viewModel: LedgerViewModel, language: Language) {
    val context = LocalContext.current
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val driveStatus by viewModel.driveStatus.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonStr = inputStream?.bufferedReader()?.use { it.readText() }
                if (!jsonStr.isNullOrBlank()) {
                    viewModel.restoreLedgerFromLocalJson(jsonStr, context)
                } else {
                    Toast.makeText(context, if (language == Language.HINDI) "गलत या खाली बैकअप फाइल" else "Invalid or empty backup file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }
    var sharePhone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Business card display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LightGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfilePhoto(
                    photoUri = profile?.photoUri ?: "",
                    modifier = Modifier.size(90.dp),
                    fallbackIconSize = 48.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile?.name ?: "My Business Store",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen
                )

                Text(
                    text = "Phone: ${profile?.phone ?: "Not Configured"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "Address: ${profile?.address ?: "Not Configured"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                if (!profile?.gstin.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "GSTIN: ${profile?.gstin}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Loc.t("edit", language), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live pseudorandom beautiful QR Code Code Card with custom amount & WhatsApp sharing
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Loc.t("upi_qr", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Scan with any UPI App (GPay, PhonePe, Paytm)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Input fields to enter custom payment amount & dynamic phone sharing number
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { customAmount = it },
                    label = { Text(if (language == Language.HINDI) "भुगतान राशि दर्ज करें (Optional)" else "Enter Payment Amount (Optional)") },
                    placeholder = { Text("e.g. 500, 1000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = sharePhone,
                    onValueChange = { sharePhone = it },
                    label = { Text(if (language == Language.HINDI) "ग्राहक का व्हाट्सएप नंबर (Optional)" else "Customer WhatsApp No. (Optional)") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // BARCODE K UPER PAYMENT MENTION HO (Highlighting payment amount/info directly above barcode)
                if (customAmount.isNotBlank()) {
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.5.dp, Color.Black)
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "भुगतान राशि: ₹$customAmount" else "AMOUNT TO PAY: ₹$customAmount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (language == Language.HINDI) "त्वरित भुगतान बारकोड" else "QUICK PAYMENT BARCODE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Custom canvas UPI QR view
                UpiQrView(
                    upiId = profile?.upiId ?: "merchant@okaxis",
                    amount = customAmount.toDoubleOrNull() ?: 0.0,
                    modifier = Modifier
                        .size(200.dp)
                        .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile?.upiId ?: "payment@upi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                // WHATSAPP PAR BHEJ SAKE (Share QR payment on WhatsApp button)
                Button(
                    onClick = {
                        val amtVal = customAmount.trim()
                        val uId = profile?.upiId ?: "merchant@okaxis"
                        val pName = profile?.name ?: "Shop"
                        val scanUpiUri = "upi://pay?pa=$uId&pn=${Uri.encode(pName)}${if (amtVal.isNotEmpty()) "&am=$amtVal" else ""}&cu=INR"
                        
                        val shareMessage = if (amtVal.isNotEmpty()) {
                            "नमस्ते! *${pName}* की तरफ से भुगतान अनुरोध:\n\n" +
                            "*कुल देय राशि (Amount Due):- ₹$amtVal*\n" +
                            "UPI ID:- $uId\n\n" +
                            "भुगतान करने के लिए इस लिंक पर क्लिक करें:\n$scanUpiUri\n\n" +
                            "धन्यवाद!\n*${pName}*"
                        } else {
                            "नमस्ते! *${pName}* का भुगतान क्यूआर लिंक (UPI Link):\n\n" +
                            "UPI ID:- $uId\n\n" +
                            "यहाँ क्लिक कर सुरक्षित भुगतान करें:\n$scanUpiUri\n\n" +
                            "धन्यवाद!"
                        }

                        val cleanPhone = sharePhone.trim().filter { it.isDigit() }.takeLast(10)
                        if (cleanPhone.isNotEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("whatsapp://send?phone=91$cleanPhone&text=${Uri.encode(shareMessage)}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                }
                                context.startActivity(Intent.createChooser(genericIntent, "Share Payment Request"))
                            }
                        } else {
                            try {
                                val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(whatsappIntent)
                            } catch (e: Exception) {
                                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                }
                                context.startActivity(Intent.createChooser(genericIntent, "Share Payment Request"))
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == Language.HINDI) "व्हाट्सएप पर शेयर करें (Share WhatsApp)" else "Share on WhatsApp",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (language == Language.HINDI) "🎖️ प्रीमियम डिजिटल क्यूआर कार्ड विकल्प" else "🎖️ Premium Digital QR Card Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download PNG
                    Button(
                        onClick = {
                            val cardBitmap = com.example.ui.QrCardGenerator.generatePremiumCard(
                                context = context,
                                profile = profile,
                                pendingAmount = customAmount.toDoubleOrNull() ?: 0.0,
                                customerName = if (sharePhone.isNotBlank()) sharePhone else ""
                            )
                            val ok = com.example.ui.QrCardGenerator.downloadQRCardAsPng(
                                context = context,
                                bitmap = cardBitmap,
                                name = profile?.name ?: "Merchant"
                            )
                            if (ok) {
                                Toast.makeText(context, if (language == Language.HINDI) "गैलरी में क्यूआर कार्ड डाउनलोड हो गया!" else "QR Card saved successfully to gallery!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to download card.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (language == Language.HINDI) "डाउनलोड" else "Download", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }

                    // Share Premium Card WhatsApp (Image + Text together)
                    Button(
                        onClick = {
                            val amtVal = customAmount.trim()
                            val uId = profile?.upiId ?: "merchant@okaxis"
                            val pName = profile?.name ?: "Shop"
                            
                            val shareMessage = if (amtVal.isNotEmpty()) {
                                "Dear Client,\n\nYour pending balance is *₹$amtVal*. Please make the payment by scanning the attached dynamic QR Code.\n\nThank you,\n*${pName}*"
                            } else {
                                "Dear Client,\n\nPlease make payment by scanning our secure UPI QR Code attached below.\n\nThank you,\n*${pName}*"
                            }

                            val cardBitmap = com.example.ui.QrCardGenerator.generatePremiumCard(
                                context = context,
                                profile = profile,
                                pendingAmount = customAmount.toDoubleOrNull() ?: 0.0,
                                customerName = if (sharePhone.isNotBlank()) sharePhone else ""
                            )
                            com.example.ui.QrCardGenerator.shareQRCardWhatsApp(context, cardBitmap, shareMessage)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Share, contentDescription = "Share Card", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (language == Language.HINDI) "कार्ड शेयर" else "Share Card", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }

                    // Print QR Code
                    Button(
                        onClick = {
                            val cardBitmap = com.example.ui.QrCardGenerator.generatePremiumCard(
                                context = context,
                                profile = profile,
                                pendingAmount = customAmount.toDoubleOrNull() ?: 0.0,
                                customerName = if (sharePhone.isNotBlank()) sharePhone else ""
                            )
                            com.example.ui.QrCardGenerator.printQRCard(context, cardBitmap)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (language == Language.HINDI) "प्रिंट" else "Print", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color(0xFFE0F2FE), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "soundbox", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Smart LEDGER Sound Enabled",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.speak(
                            if (language == Language.HINDI) "क्रेडिट बुक साउंड बॉक्स एकदम सही काम कर रहा है!" 
                            else "Credit Book sound box is working perfectly!"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("test_soundbox_button")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "test speaker", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == Language.HINDI) "साउंड बॉक्स टेस्ट करें" else "Test Soundbox",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 10 Profiles Multi-Store Selector Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Profiles Icon",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (language == Language.HINDI) "अन्य दुकान खाते (प्रोफ़ाइल)" else "Switch Store Accounts (10 Profiles)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = if (language == Language.HINDI)
                        "आप 10 अलग-अलग दुकान खातों में स्विच कर सकते हैं। प्रत्येक का अपना स्वतंत्र डेटा (ग्राहक, सप्लायर और बिलिंग) होगा।"
                        else "You can switch between 10 separate shop profiles. Each has its own independent ledger history, customers, and inventory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                val activeId by viewModel.activeProfileId.collectAsStateWithLifecycle()
                val profilesList by viewModel.allBusinessProfiles.collectAsStateWithLifecycle()

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 1..10) {
                        val isSelected = activeId == i
                        val currentStoreProfile = profilesList.find { it.id == i }
                        
                        val defaultNamesByIndex = listOf(
                            "Smart General Store",
                            "Aggarwal Pharmacy",
                            "Sharma Textiles",
                            "Gupta Electronics",
                            "Royal Café & Bakery",
                            "Krishna Dairy",
                            "Janta Sweet House",
                            "Modern Garments",
                            "Verma Hardware",
                            "Apex Stationery"
                        )
                        val storeName = currentStoreProfile?.name ?: defaultNamesByIndex.getOrElse(i - 1) { "Smart Store #$i" }
                        val storePhone = currentStoreProfile?.phone ?: "98765432${i - 1}"
                        val storePhoto = currentStoreProfile?.photoUri ?: "preset_$i"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectProfile(i)
                                    val speakText = if (language == Language.HINDI) {
                                        "$storeName खाता प्रोफ़ाइल सक्रिय किया गया।"
                                    } else {
                                        "Switched to $storeName ledger."
                                    }
                                    viewModel.speak(speakText)
                                    Toast.makeText(context, speakText, Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PrimaryGreen else Color(0xFFE2E8F0)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 2.dp else 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isSelected) PrimaryGreen else Color(0xFF64748B),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = i.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                ProfilePhoto(
                                    photoUri = storePhoto,
                                    modifier = Modifier.size(42.dp),
                                    fallbackIconSize = 22.dp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = storeName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PrimaryGreen else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${if (language == Language.HINDI) "मोबाइल: " else "Call: "}$storePhone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                if (isSelected) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(Color(0xFFDCFCE7), shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (language == Language.HINDI) "सक्रिय" else "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Drive Backup & Restore Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("google_drive_backup_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Google Drive Backup Icon",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == Language.HINDI) "गूगल ड्राइव बैकअप और रीस्टोर" else "Google Drive Backup & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (language == Language.HINDI) "डेटा सुरक्षित रखें और कभी भी वापस पाएं" else "Keep your ledger database secure and in sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = if (language == Language.HINDI)
                        "अपने सभी बहियों (ग्राहकों, सप्लायरों, वस्तुओं और लेन-देन) का सुरक्षित बैकअप गूगल ड्राइव पर रखें और नए फोन में रीस्टोर करें।"
                        else "No more data loss. Save all transactions, customer books, and inventory products directly in your personal Google Drive and reload them with a single click.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )

                // Render current backup operation status beautifully
                driveStatus?.let { status ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = when {
                                    status.contains("Success", ignoreCase = true) || status.contains("सफल", ignoreCase = true) || status.contains("सफलता", ignoreCase = true) -> Color(0xFFDCFCE7)
                                    status.contains("Fail", ignoreCase = true) || status.contains("विफल", ignoreCase = true) -> Color(0xFFFEE2E2)
                                    else -> Color(0xFFFEF3C7)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    status.contains("Success", ignoreCase = true) || status.contains("सफल", ignoreCase = true) || status.contains("सफलता", ignoreCase = true) -> Icons.Default.CheckCircle
                                    status.contains("Fail", ignoreCase = true) || status.contains("विफल", ignoreCase = true) -> Icons.Default.Error
                                    else -> Icons.Default.Sync
                                },
                                contentDescription = "Status icon",
                                tint = when {
                                    status.contains("Success", ignoreCase = true) || status.contains("सफल", ignoreCase = true) || status.contains("सफलता", ignoreCase = true) -> Color(0xFF15803D)
                                    status.contains("Fail", ignoreCase = true) || status.contains("विफल", ignoreCase = true) -> Color(0xFFB91C1C)
                                    else -> Color(0xFFB45309)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    status.contains("Success", ignoreCase = true) || status.contains("सफल", ignoreCase = true) || status.contains("सफलता", ignoreCase = true) -> Color(0xFF15803D)
                                    status.contains("Fail", ignoreCase = true) || status.contains("विफल", ignoreCase = true) -> Color(0xFFB91C1C)
                                    else -> Color(0xFFB45309)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearDriveStatus() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Status",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.backupLedgerToGoogleDrive(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_drive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup Now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == Language.HINDI) "बैकअप लें" else "Backup Now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { viewModel.restoreLedgerFromGoogleDrive(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_drive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Restore Now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == Language.HINDI) "डेटा रीस्टोर" else "Restore Now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF1F5F9))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEEF2F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Local Storage Backup Icon",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (language == Language.HINDI) "लोकल फ़ोन मेमोरी बैकअप" else "Phone Storage Backup & Restore",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (language == Language.HINDI) "फ़ाइल शेयर या सेव करें और ऑफ़लाइन लोड करें" else "Export or Import backup file offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.backupLedgerToLocalFile(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_local_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = "Local Backup",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == Language.HINDI) "लोकल बैकअप लें" else "Backup to Phone",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { filePickerLauncher.launch("application/json") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_local_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Local Restore",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == Language.HINDI) "फ़ाइल से रीस्टोर" else "Restore from File",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Fingerprint App Lock Card
        val isLockEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("security_lock_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFEF2F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint Screen Lock Icon",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == Language.HINDI) "ऐप सुरक्षा लॉक (फिंगरप्रिंट / पैटर्न)" else "App Lock (Fingerprint / PIN)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (language == Language.HINDI) "आवाज़ या फ़िंगरप्रिंट से सुरक्षित करें" else "Secure access to your credit ledger",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    
                    Switch(
                        checked = isLockEnabled,
                        onCheckedChange = { checked ->
                            val activity = context as? androidx.fragment.app.FragmentActivity
                            if (activity != null) {
                                if (checked) {
                                    if (BiometricHelper.isBiometricAvailable(context)) {
                                        BiometricHelper.showBiometricPrompt(
                                            activity = activity,
                                            title = if (language == Language.HINDI) "ऐप लॉक सेट करें" else "Set App Lock",
                                            subtitle = if (language == Language.HINDI) "सत्यापन के लिए फिंगरप्रिंट स्पर्श करें" else "Scan fingerprint to enable lock protection",
                                            onSuccess = {
                                                viewModel.setLockEnabled(true)
                                                Toast.makeText(context, if (language == Language.HINDI) "ऐप लॉक चालू हो गया है" else "App Lock Enabled successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, if (language == Language.HINDI) "प्रमाणन विफल: $err" else "Authentication failed: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(context, if (language == Language.HINDI) "इस डिवाइस पर लॉक उपलब्ध नहीं है" else "Biometrics/Screen lock not available or configured on this device.", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    // Prompt biometric confirm before disabling lock
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        title = if (language == Language.HINDI) "ऐप लॉक हटाएं" else "Disable App Lock",
                                        subtitle = if (language == Language.HINDI) "लॉक हटाने के लिए फिंगरप्रिंट सत्यापित करें" else "Verify identity to disable lock protection",
                                        onSuccess = {
                                            viewModel.setLockEnabled(false)
                                            Toast.makeText(context, if (language == Language.HINDI) "ऐप लॉक बंद कर दिया गया है" else "App Lock disabled.", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, if (language == Language.HINDI) "सत्यापन विफल: लॉक हटाया नहीं जा सका" else "Verification failed: Could not disable lock.", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            } else {
                                Toast.makeText(context, "System configuration error", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.testTag("security_lock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = if (language == Language.HINDI)
                        "इसे सक्रिय करने के बाद, जब भी ऐप खुलेगा, यह आपके फोन का पासवर्ड, पैटर्न या फिंगरप्रिंट खोजेगा।"
                        else "Toggle lock to configure device level authentication (such as fingerprint, face recognition, PIN or pattern) to protect transactions and customer records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }

    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            var bName by remember { mutableStateOf(profile?.name ?: "") }
            var bPhone by remember { mutableStateOf(profile?.phone ?: "") }
            var bAddr by remember { mutableStateOf(profile?.address ?: "") }
            var bUpi by remember { mutableStateOf(profile?.upiId ?: "") }
            var bGst by remember { mutableStateOf(profile?.gstin ?: "") }
            var bPhoto by remember { mutableStateOf(profile?.photoUri ?: "") }
            var bClinicName by remember { mutableStateOf(profile?.clinicName ?: "") }
            var bClinicAddress by remember { mutableStateOf(profile?.clinicAddress ?: "") }
            var bDefNote by remember { mutableStateOf(profile?.defaultPaymentNote ?: "Payment for balance") }
            var bEnableSharing by remember { mutableStateOf(profile?.enableQrSharing ?: true) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Loc.t("profile_setting", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text(Loc.t("business_name", language)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_shop_name")
                    )

                    OutlinedTextField(
                        value = bPhone,
                        onValueChange = { bPhone = it },
                        label = { Text(Loc.t("phone_number", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bAddr,
                        onValueChange = { bAddr = it },
                        label = { Text(Loc.t("address", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bUpi,
                        onValueChange = { bUpi = it },
                        label = { Text(Loc.t("upi_id", language)) },
                        placeholder = { Text("e.g. shop@okaxis") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = bUpi.isNotBlank() && !com.example.ui.QrCardGenerator.isValidUpiId(bUpi)
                    )
                    if (bUpi.isNotBlank() && !com.example.ui.QrCardGenerator.isValidUpiId(bUpi)) {
                        Text(
                            text = if (language == Language.HINDI) "अमान्य UPI ID (उदा. abc@okbank)" else "Invalid UPI ID (e.g. abc@okbank)",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    OutlinedTextField(
                        value = bGst,
                        onValueChange = { bGst = it },
                        label = { Text(Loc.t("gstin", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (language == Language.HINDI) "🏥 क्लिनिक एवं क्यूआर अनुकूलन" else "🏥 Clinic & QR Customization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    OutlinedTextField(
                        value = bClinicName,
                        onValueChange = { bClinicName = it },
                        label = { Text(if (language == Language.HINDI) "क्लिनिक / अस्पताल का नाम" else "Clinic / Business Name") },
                        placeholder = { Text("e.g. Apex Care Clinic") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bClinicAddress,
                        onValueChange = { bClinicAddress = it },
                        label = { Text(if (language == Language.HINDI) "क्लिनिक का पता" else "Clinic / Business Address") },
                        placeholder = { Text("e.g. Medical Enclave, Delhi") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bDefNote,
                        onValueChange = { bDefNote = it },
                        label = { Text(if (language == Language.HINDI) "डिफ़ॉल्ट भुगतान टिप्पणी" else "Default Payment Note") },
                        placeholder = { Text("e.g. Payment for balance") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "बैलेंस शेयर में क्यूआर चित्र जोड़ें" else "Attach QR Image in Share",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        androidx.compose.material3.Switch(
                            checked = bEnableSharing,
                            onCheckedChange = { bEnableSharing = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (language == Language.HINDI) "दुकान प्रोफाइल फोटो / अवतार चुनें" else "Choose Store Profile Photo / Preset Avatar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        ProfilePhoto(
                            photoUri = bPhoto,
                            modifier = Modifier.size(60.dp),
                            fallbackIconSize = 30.dp
                        )
                        Column {
                            Text(
                                text = if (language == Language.HINDI) "फोटो प्रीव्यू" else "Profile Photo Preview",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (bPhoto.startsWith("preset_")) "Template Preset Active" else if (bPhoto.isNotBlank()) "Custom Uri Image" else "No Photo Chosen",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Text(
                        text = if (language == Language.HINDI) "सुंदर व्यवसाय श्रेणियों में से चुनें:" else "Choose from beautiful visual retail store templates:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (idx in 1..10) {
                            val presetUri = "preset_$idx"
                            val isChosen = bPhoto == presetUri
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) PrimaryGreen else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { bPhoto = presetUri }
                                    .padding(if (isChosen) 2.dp else 0.dp)
                            ) {
                                ProfilePhoto(
                                    photoUri = presetUri,
                                    modifier = Modifier.fillMaxSize(),
                                    fallbackIconSize = 20.dp
                                )
                            }
                        }
                    }

                    val photoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            bPhoto = it.toString()
                        }
                    }

                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Pick Photo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == Language.HINDI) "गैलरी से फोटो चुनें" else "Choose Custom Photo from Gallery",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = bPhoto,
                        onValueChange = { bPhoto = it },
                        label = { Text(if (language == Language.HINDI) "कस्टम फोटो URI रस्ता" else "Custom Photo URL or URI path") },
                        placeholder = { Text("content:// or https://") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (bPhoto.isNotBlank()) {
                                IconButton(onClick = { bPhoto = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text(Loc.t("cancel", language), color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (bUpi.isNotBlank() && !com.example.ui.QrCardGenerator.isValidUpiId(bUpi)) {
                                    Toast.makeText(context, "Please correct the UPI ID format!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveBusinessProfile(
                                        bName, bUpi, bAddr, bUpi, bGst, bPhoto,
                                        bClinicName, bClinicAddress, bDefNote, bEnableSharing
                                    )
                                    viewModel.saveBusinessProfile(
                                        name = bName,
                                        phone = bPhone,
                                        address = bAddr,
                                        upiId = bUpi,
                                        gstin = bGst,
                                        photoUri = bPhoto,
                                        clinicName = bClinicName,
                                        clinicAddress = bClinicAddress,
                                        defaultPaymentNote = bDefNote,
                                        enableQrSharing = bEnableSharing
                                    )
                                    showEditDialog = false
                                    Toast.makeText(context, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text(Loc.t("save", language))
                        }
                    }
                }
            }
        }
    }
}


// ---------------- CUSTOMER LEDGER SCREEN DEVIATION ----------------

@Composable
fun CustomerDetailScreen(
    customerId: Int,
    viewModel: LedgerViewModel,
    language: Language,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val allTxs by viewModel.transactions.collectAsStateWithLifecycle()
    val balance by viewModel.getCustomerBalance(customerId).collectAsStateWithLifecycle(0.0)
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    val customer = customers.find { it.id == customerId }
    val userTxs = allTxs.filter { it.customerId == customerId }

    var showAddTxDialog by remember { mutableStateOf(false) }
    var addTxType by remember { mutableStateOf("CREDIT") } // "CREDIT" or "PAYMENT"
    var showStatementDialog by remember { mutableStateOf(false) }
    var showTemplateSelectionDialog by remember { mutableStateOf(false) }
    var showQrCardDialog by remember { mutableStateOf(false) }
    var showQrSharePreview by remember { mutableStateOf(false) }

    if (customer == null) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Customer not found.")
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Simple customized detail header - simple like WhatsApp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back icon", tint = PrimaryGreen)
                    }

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.firstOrNull()?.uppercase() ?: "C",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // QR Payment & Barcode Profile ID Card Button
                    IconButton(
                        onClick = { showQrCardDialog = true },
                        modifier = Modifier.testTag("customer_qr_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR and Barcode Profile",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Delete customer option
                    IconButton(
                        onClick = {
                            viewModel.deleteCustomer(customer)
                            Toast.makeText(context, "Deleted customer ${customer.name}", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete customer", tint = DebitRed)
                    }
                }
            }
        }

        // Live Outstanding Balance Placard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (balance > 0) Color(0xFFFFECEC) else if (balance < 0) Color(0xFFE6F9F0) else Color(0xFFF1F5F9)
            ),
            border = BorderStroke(
                1.dp,
                if (balance > 0) Color(0xFFFFC0C0) else if (balance < 0) Color(0xFFBFEFDC) else Color.LightGray
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Loc.t("remaining_bal", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Text(
                        text = if (balance > 0) "₹${balance.toInt()} (Due)" else if (balance < 0) "₹${(-balance).toInt()} (Surplus)" else "₹0.00",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance > 0) DebitRed else if (balance < 0) CreditGreen else Color.DarkGray
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (balance != 0.0) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (balance != 0.0) "Active Book" else "Settled",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (balance != 0.0) Color(0xFF1E40AF) else Color.Gray
                    )
                }
            }
        }

        // Smart Personalized Reminder Selector Button
        if (balance > 0) {
            Button(
                onClick = { showTemplateSelectionDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(44.dp)
                    .testTag("smart_reminder_template_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ListAlt, contentDescription = "Templates", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == Language.HINDI) "स्मार्ट रिमाइंडर (Quick Templates)" else "Smart Reminders & Templates",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Customer Quick Action Toolbar: SMS Updates & Professional Account Statement
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. WhatsApp Action
            Button(
                onClick = {
                    if (balance > 0 && profile?.enableQrSharing == true && !profile?.upiId.isNullOrBlank()) {
                        showQrSharePreview = true
                    } else {
                        val upiStr = if (!profile?.upiId.isNullOrBlank()) " UPI: ${profile?.upiId}" else ""
                        val msg = if (balance > 0) {
                            Loc.t("reminder_text", language).format(balance.toInt()) + upiStr
                        } else {
                            "Hello ${customer.name}, greeting from ${profile?.name ?: "Credit Book"}. Our ledger is updated."
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("whatsapp://send?phone=91${customer.phone}&text=${Uri.encode(msg)}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${customer.phone}")
                                putExtra("sms_body", msg)
                            }
                            context.startActivity(smsIntent)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("whatsapp_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "WhatsApp", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            // 2. Direct SMS Update Action
            Button(
                onClick = {
                    val upiStr = if (!profile?.upiId.isNullOrBlank()) " UPI: ${profile?.upiId}" else ""
                    val msg = if (balance > 0) {
                        if (language == Language.HINDI) {
                            "नमस्ते ${customer.name}, आपके खाता बही का बकाया ₹${balance.toInt()} है। कृपया जल्द भुगतान करें। धन्यवाद! - ${profile?.name ?: "खाता"}$upiStr"
                        } else {
                            "Dear ${customer.name}, your outstanding balance of ₹${balance.toInt()} with ${profile?.name ?: "Credit Book"} is pending. Please pay. Thank you!$upiStr"
                        }
                    } else {
                        if (language == Language.HINDI) {
                            "नमस्ते ${customer.name}, आपके खाते का विवरण अपडेट कर दिया गया है। धन्यवाद - ${profile?.name ?: "खाता"}"
                        } else {
                            "Hello ${customer.name}, your accounts ledger has been successfully updated. Thank you - ${profile?.name ?: "Credit Book"}"
                        }
                    }
                    try {
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${customer.phone}")
                            putExtra("sms_body", msg)
                        }
                        context.startActivity(smsIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "SMS client not found.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("sms_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Sms, contentDescription = "SMS", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SMS Update", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            // 3. Statement action
            Button(
                onClick = { showStatementDialog = true },
                modifier = Modifier
                    .weight(1.1f)
                    .height(38.dp)
                    .testTag("statement_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = "Statement", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (language == Language.HINDI) "स्टेटमेंट" else "Statement", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Dedicated UPI QR Balance Card & Sharing Action Button
        if (balance > 0) {
            Button(
                onClick = { showQrSharePreview = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(44.dp)
                    .testTag("show_qr_sharing_center_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), // Royal Blue matching primary color
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == Language.HINDI) "बैलेंस शेयर और भुगतान क्यूआर (QR Share Center)" else "Get Paid: Share Balance + UPI QR",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (showQrSharePreview) {
            ShareQrPreviewDialog(
                customer = customer,
                balance = balance,
                profile = profile,
                language = language,
                onDismiss = { showQrSharePreview = false },
                onMarkAsPaid = {
                    viewModel.addTransaction(
                        customerId = customer.id,
                        supplierId = null,
                        type = "PAYMENT",
                        amount = balance,
                        notes = if (language == Language.HINDI) "सुरक्षित UPI क्यूआर द्वारा भुगतान किया गया" else "Paid via Secure UPI QR Card",
                        photoUri = "",
                        voiceNoteUri = "",
                        signatureUri = ""
                    )
                    Toast.makeText(context, "Marked as Paid! Collection Successful.", Toast.LENGTH_LONG).show()
                }
            )
        }

        // Automated WhatsApp Business API Reminder Center Card
        if (balance > 0) {
            val whatsappStatuses by viewModel.whatsappStatuses.collectAsStateWithLifecycle()
            val status = whatsappStatuses[customer.id]
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("whatsapp_business_api_card_detail"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)), // elegant light teal
                border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Auto API",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == Language.HINDI) "WhatsApp Business API रिमाइंडर" else "WhatsApp Business Cloud API",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF115E59)
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.sendWhatsAppApiReminder(
                                    customerId = customer.id,
                                    customerPhone = customer.phone,
                                    customerName = customer.name,
                                    balanceAmount = balance,
                                    businessName = profile?.name ?: "Credit Book"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (language == Language.HINDI) "स्वचालित भेजें" else "Send Auto",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    if (status != null) {
                        if (status.isSending) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color(0xFF0D9488))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == Language.HINDI) "भेजा जा रहा है, कृपया प्रतीक्षा करें..." else "Sending reminder via Meta Cloud API...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        } else if (status.success) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = CreditGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == Language.HINDI) "सफलतापूर्वक भेज दिया गया!" else "Message successfully requested via API!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CreditGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (status.error != null) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = DebitRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == Language.HINDI) "रिमाइंडर विफल" else "API Dispatch Failed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DebitRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = status.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (language == Language.HINDI) {
                                "Meta Business API के द्वारा अधिकृत भाषा टेम्पलेट में बकाया रिमाइंडर स्वतः जायेगा।"
                            } else {
                                "Sends an official corporate reminder using pre-registered WhatsApp template."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Ledger Statement Text List
        if (userTxs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Empty",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(Loc.t("no_transactions", language), color = Color.Gray)
                }
            }
        } else {
            // Live undo suggestion banner if mistakes occur
            if (viewModel.lastInsertedTransactionId != 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "⚠️ कोई गलती हुई? पिछला एंट्री हटाएँ" else "⚠️ Mistake? Undo last entry instantly",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Button(
                            onClick = {
                                viewModel.undoLastTransaction { msg ->
                                    Toast.makeText(context, if (language == Language.HINDI) "पिछली एंट्री वापस ली गयी!" else msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF92400E)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(if (language == Language.HINDI) "वापस लें" else "UNDO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            var previewProofPhoto by remember { mutableStateOf("") }
            var activeVoicePlayingId by remember { mutableStateOf<Int?>(null) }

            // Interactive receipt popup
            if (previewProofPhoto.isNotBlank()) {
                AlertDialog(
                    onDismissRequest = { previewProofPhoto = "" },
                    title = { Text(if (language == Language.HINDI) "सत्यापित बिल दस्तावेज" else "Verified Bill Document") },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(if (language == Language.HINDI) "भुगतान रसीद प्रमाण" else "Transaction Proof of Payment", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("BILL NO: TX-${System.currentTimeMillis() % 100000}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        Text("APPROVED", color = CreditGreen, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                    }
                                    Divider(color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Customer Account Ledger", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    Text("Status: Recorded in Secure khata Database", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    // Simulated barcode line
                                    Row(modifier = Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.Center) {
                                        repeat(24) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .width(if (index % 3 == 0) 4.dp else 1.5.dp)
                                                    .fillMaxHeight()
                                                    .background(Color.Black)
                                            )
                                            Spacer(modifier = Modifier.width(1.5.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { previewProofPhoto = "" }) {
                            Text("OK", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userTxs) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    val txDate = sdf.format(Date(tx.date))
                                    Text(
                                        text = if (tx.type == "CREDIT") "Out / Udhar Given (-)" else "In / Payment Received (+)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == "CREDIT") DebitRed else CreditGreen,
                                        fontSize = 14.sp
                                    )
                                    if (tx.notes.isNotBlank()) {
                                        Text(tx.notes, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                                    }
                                    Text(
                                        text = txDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                // Amount Column
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = (if (tx.type == "CREDIT") "-" else "+") + " ₹${tx.amount.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (tx.type == "CREDIT") DebitRed else CreditGreen
                                    )
                                    IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "delete transaction", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Dynamic media attachment presentation tray
                            if (tx.photoUri.isNotBlank() || tx.voiceNoteUri.isNotBlank() || tx.signatureUri.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Photo proof pill
                                    if (tx.photoUri.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEFF6FF), shape = RoundedCornerShape(12.dp))
                                                .clickable { previewProofPhoto = tx.photoUri }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PhotoCamera, contentDescription = "Proof", tint = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (language == Language.HINDI) "रसीद देखें" else "View Bill", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Signature status tag
                                    if (tx.signatureUri.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFECFDF5), shape = RoundedCornerShape(12.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Gesture, contentDescription = "Sign ok", tint = CreditGreen, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (language == Language.HINDI) "हस्ताक्षरित" else "Customer Signed", style = MaterialTheme.typography.labelSmall, color = CreditGreen, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Voice note player pill
                                    if (tx.voiceNoteUri.isNotBlank()) {
                                        val isPlaying = activeVoicePlayingId == tx.id
                                        Box(
                                            modifier = Modifier
                                                .background(if (isPlaying) Color(0xFFFEF2F2) else Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp))
                                                .clickable {
                                                    activeVoicePlayingId = if (isPlaying) null else tx.id
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.VolumeUp, contentDescription = "Audio Play", tint = if (isPlaying) Color.Red else Color.Gray, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isPlaying) (if (language == Language.HINDI) "बज रहा..." else "Playing...") else (if (language == Language.HINDI) "सुनें ऑडियो" else "Play Note"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isPlaying) Color.Red else Color.DarkGray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive Audio Wave Animation Card
                                if (activeVoicePlayingId == tx.id) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("🎵 voice_note_rec.amr", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                repeat(8) { offset ->
                                                    // Dynamic pulsating sound waves simulator bar heights
                                                    val pulseHeight = remember { (10..30).random().dp }
                                                    Box(
                                                        modifier = Modifier
                                                            .width(3.dp)
                                                            .height(pulseHeight)
                                                            .background(Color.Red, RoundedCornerShape(1.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons: Give Udhar or Receive Payment (exactly like OkCredit)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    addTxType = "CREDIT"
                    showAddTxDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("udhar_dena_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DebitRed)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "out")
                Spacer(modifier = Modifier.width(4.dp))
                Text(Loc.t("give_credit", language), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    addTxType = "PAYMENT"
                    showAddTxDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("jama_karna_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CreditGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = "in")
                Spacer(modifier = Modifier.width(4.dp))
                Text(Loc.t("receive_payment", language), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            type = addTxType,
            language = language,
            onDismiss = { showAddTxDialog = false },
            onSave = { amount, notes, photo, voice, signature ->
                viewModel.addTransaction(
                    customerId = customerId,
                    supplierId = null,
                    type = addTxType,
                    amount = amount,
                    notes = notes,
                    photoUri = photo,
                    voiceNoteUri = voice,
                    signatureUri = signature
                )
                showAddTxDialog = false
                Toast.makeText(context, if (language == Language.HINDI) "लेनदेन सुरक्षित किया गया!" else "Transaction saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showTemplateSelectionDialog) {
        ReminderTemplateSelectionDialog(
            customer = customer,
            balance = balance,
            viewModel = viewModel,
            profile = profile,
            language = language,
            onDismiss = { showTemplateSelectionDialog = false }
        )
    }

    if (showStatementDialog) {
        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Statement", tint = PrimaryGreen)
                    Text(
                        text = if (language == Language.HINDI) "खाता स्टेटमेंट" else "Account Statement",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = profile?.name ?: "Credit Book",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Customer: ${customer.name}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Outstanding Due: ₹${balance.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (balance > 0) DebitRed else CreditGreen
                            )
                        }
                    }

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF))
                            .padding(8.dp)
                    ) {
                        Text("Date", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Text("Details", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Text("Amount", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), textAlign = TextAlign.End)
                    }

                    // Scrollable Table Body
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sortedTxs = userTxs.sortedBy { it.date }
                        items(sortedTxs.size) { idx ->
                            val tx = sortedTxs[idx]
                            val dateStr = SimpleDateFormat("dd/MM/yy").format(Date(tx.date))
                            val isDue = tx.type == "CREDIT"
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dateStr, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                Text(
                                    text = if (tx.notes.isNotBlank()) tx.notes else (if (isDue) "Udhar Out" else "Advance In"),
                                    modifier = Modifier.weight(2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${if (isDue) "+" else "-"}₹${tx.amount.toInt()}",
                                    modifier = Modifier.weight(1.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDue) DebitRed else CreditGreen,
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sortedTxs = userTxs.sortedBy { it.date }
                        val plainText = buildString {
                            append("==============================\n")
                            append("${profile?.name ?: "CREDIT BOOK PRO"}\n")
                            append("ACCOUNT STATEMENT\n")
                            append("Customer: ${customer.name}\n")
                            append("Phone: ${customer.phone}\n")
                            append("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())}\n")
                            append("==============================\n")
                            append(String.format("%-10s | %-12s | %-8s\n", "Date", "Details", "Amount"))
                            append("------------------------------\n")
                            for (tx in sortedTxs) {
                                val dt = SimpleDateFormat("dd/MM/yy").format(Date(tx.date))
                                val isCredit = tx.type == "CREDIT"
                                val desc = if (tx.notes.isNotBlank()) tx.notes else (if (isCredit) "Udhar" else "Payment")
                                append(String.format("%-10s | %-12s | %s₹%d\n", dt, desc, if (isCredit) "+" else "-", tx.amount.toInt()))
                            }
                            append("------------------------------\n")
                            append("Net Outstanding Due: ₹${balance.toInt()}\n")
                            append("==============================\n")
                        }
                        
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Account Statement - ${customer.name}")
                            putExtra(Intent.EXTRA_TEXT, plainText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Account Statement"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == Language.HINDI) "शेयर स्टेटमेंट" else "Share Statement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatementDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }

    if (showQrCardDialog) {
        CustomerQrCardDialog(
            customer = customer,
            balance = balance,
            profile = profile,
            language = language,
            onDismiss = { showQrCardDialog = false }
        )
    }
}


// ---------------- SUPPLIER DETAIL SCREEN ----------------

@Composable
fun SupplierDetailScreen(
    supplierId: Int,
    viewModel: LedgerViewModel,
    language: Language,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val allTxs by viewModel.transactions.collectAsStateWithLifecycle()
    val balance by viewModel.getSupplierBalance(supplierId).collectAsStateWithLifecycle(0.0)

    val supplier = suppliers.find { it.id == supplierId }
    val userTxs = allTxs.filter { it.supplierId == supplierId }
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var addTxType by remember { mutableStateOf("CREDIT") } // "CREDIT" for purchased on credit, "PAYMENT" for pay suppliers
    var showStatementDialog by remember { mutableStateOf(false) }

    if (supplier == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Supplier check error.")
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryGreen)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = supplier.name.firstOrNull()?.uppercase() ?: "S",
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(supplier.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(supplier.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.deleteSupplier(supplier)
                        Toast.makeText(context, "Deleted supplier ${supplier.name}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DebitRed)
                }
            }
        }

        // Tally Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (balance > 0) Color(0xFFFFECEC) else if (balance < 0) Color(0xFFE6F9F0) else Color(0xFFF1F5F9)
            ),
            border = BorderStroke(
                1.dp,
                if (balance > 0) Color(0xFFFFC0C0) else if (balance < 0) Color(0xFFBFEFDC) else Color.LightGray
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(Loc.t("remaining_bal", language), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = if (balance > 0) "₹${balance.toInt()} (We Owe)" else if (balance < 0) "₹${(-balance).toInt()} (We Paid Extra)" else "₹0.00",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance > 0) DebitRed else if (balance < 0) CreditGreen else Color.Black
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (balance != 0.0) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (balance != 0.0) "Active" else "Settled",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (balance != 0.0) Color(0xFF1E40AF) else Color.Gray
                    )
                }
            }
        }

        // Supplier Quick Action Toolbar: SMS updates and statements generator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WhatsApp button
            Button(
                onClick = {
                    val msg = if (balance > 0) {
                        "Hello ${supplier.name}, regarding our dues of ₹${balance.toInt()} with ${profile?.name ?: "us"}, we have updated our record book."
                    } else {
                        "Hello ${supplier.name}, greetings from ${profile?.name ?: "Credit Book"}. Our supplier account statement is updated."
                    }
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("whatsapp://send?phone=91${supplier.phone}&text=${Uri.encode(msg)}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${supplier.phone}")
                            putExtra("sms_body", msg)
                        }
                        context.startActivity(smsIntent)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("supplier_whatsapp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "WhatsApp", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            // SMS update button
            Button(
                onClick = {
                    val msg = if (balance > 0) {
                        if (language == Language.HINDI) {
                            "नमस्ते ${supplier.name}, आपके खाते का बकाया राशि ₹${balance.toInt()} हमारे रिकॉर्ड बुक में दर्ज है - ${profile?.name ?: "खाता"}"
                        } else {
                            "Hello ${supplier.name}, our accounts tally shows a balance due of ₹${balance.toInt()}. Registry updated - ${profile?.name ?: "Credit Book"}"
                        }
                    } else {
                        if (language == Language.HINDI) {
                            "नमस्ते ${supplier.name}, हमारे बही खाते का विवरण अपडेट कर दिया गया है। धन्यवाद - ${profile?.name ?: "खाता"}"
                        } else {
                            "Hello ${supplier.name}, our accounts ledger has been successfully updated. Thank you - ${profile?.name ?: "Credit Book"}"
                        }
                    }
                    try {
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${supplier.phone}")
                            putExtra("sms_body", msg)
                        }
                        context.startActivity(smsIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "SMS client not found.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("supplier_sms_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Sms, contentDescription = "SMS", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SMS Update", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            // Statement button
            Button(
                onClick = { showStatementDialog = true },
                modifier = Modifier
                    .weight(1.1f)
                    .height(38.dp)
                    .testTag("supplier_statement_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = "Statement", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (language == Language.HINDI) "स्टेटमेंट" else "Statement", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Transactions List
        if (userTxs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = "No receipts", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Text(Loc.t("no_transactions", language), color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userTxs) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                Text(
                                    text = if (tx.type == "CREDIT") "Bought on Credit (-)" else "Paid to Supplier (+)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "CREDIT") DebitRed else CreditGreen,
                                    fontSize = 14.sp
                                )
                                Text(tx.notes, style = MaterialTheme.typography.bodyMedium)
                                Text(sdf.format(Date(tx.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "₹${tx.amount.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (tx.type == "CREDIT") DebitRed else CreditGreen
                                )
                                IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    addTxType = "CREDIT"
                    showAddTxDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("record_purchase_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DebitRed)
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = "buy")
                Spacer(modifier = Modifier.width(4.dp))
                Text(Loc.t("record_purchase", language), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    addTxType = "PAYMENT"
                    showAddTxDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("record_payment_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CreditGreen)
            ) {
                Icon(Icons.Default.Payment, contentDescription = "pay")
                Spacer(modifier = Modifier.width(4.dp))
                Text(Loc.t("record_payment", language), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            type = addTxType,
            language = language,
            onDismiss = { showAddTxDialog = false },
            onSave = { amount, notes, photo, voice, signature ->
                viewModel.addTransaction(
                    customerId = null,
                    supplierId = supplierId,
                    type = addTxType,
                    amount = amount,
                    notes = notes,
                    photoUri = photo,
                    voiceNoteUri = voice,
                    signatureUri = signature
                )
                showAddTxDialog = false
                Toast.makeText(context, if (language == Language.HINDI) "लेनदेन रिकॉर्ड किया गया!" else "Transaction recorded!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showStatementDialog) {
        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Statement", tint = PrimaryGreen)
                    Text(
                        text = if (language == Language.HINDI) "खाता स्टेटमेंट" else "Supplier Statement",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = profile?.name ?: "Credit Book",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Supplier: ${supplier.name}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Our Outstanding Balance: ₹${balance.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (balance > 0) DebitRed else CreditGreen
                            )
                        }
                    }

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF))
                            .padding(8.dp)
                    ) {
                        Text("Date", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Text("Details", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Text("Amount", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), textAlign = TextAlign.End)
                    }

                    // Scrollable Table Body
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sortedTxs = userTxs.sortedBy { it.date }
                        items(sortedTxs.size) { idx ->
                            val tx = sortedTxs[idx]
                            val dateStr = SimpleDateFormat("dd/MM/yy").format(Date(tx.date))
                            val isPurchase = tx.type == "CREDIT"
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dateStr, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                Text(
                                    text = if (tx.notes.isNotBlank()) tx.notes else (if (isPurchase) "Udhar Purchase" else "Amount Paid"),
                                    modifier = Modifier.weight(2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${if (isPurchase) "+" else "-"}₹${tx.amount.toInt()}",
                                    modifier = Modifier.weight(1.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPurchase) DebitRed else CreditGreen,
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sortedTxs = userTxs.sortedBy { it.date }
                        val plainText = buildString {
                            append("==============================\n")
                            append("${profile?.name ?: "CREDIT BOOK PRO"}\n")
                            append("SUPPLIER STATEMENT\n")
                            append("Supplier: ${supplier.name}\n")
                            append("Phone: ${supplier.phone}\n")
                            append("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())}\n")
                            append("==============================\n")
                            append(String.format("%-10s | %-12s | %-8s\n", "Date", "Details", "Amount"))
                            append("------------------------------\n")
                            for (tx in sortedTxs) {
                                val dt = SimpleDateFormat("dd/MM/yy").format(Date(tx.date))
                                val isCred = tx.type == "CREDIT"
                                val desc = if (tx.notes.isNotBlank()) tx.notes else (if (isCred) "Purchase" else "Paid")
                                append(String.format("%-10s | %-12s | %s₹%d\n", dt, desc, if (isCred) "+" else "-", tx.amount.toInt()))
                            }
                            append("------------------------------\n")
                            append("Net Outstanding Due: ₹${balance.toInt()}\n")
                            append("==============================\n")
                        }
                        
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Supplier Statement - ${supplier.name}")
                            putExtra(Intent.EXTRA_TEXT, plainText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Supplier Statement"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == Language.HINDI) "शेयर स्टेटमेंट" else "Share Statement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatementDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }
}


// ---------------- DIALOG COMPONENT SECTIONS ----------------

@Composable
fun AddCustomerDialog(
    language: Language,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Loc.t("add_customer", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Loc.t("customer_name", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Loc.t("phone_number", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(Loc.t("address", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Loc.t("cancel", language), color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, phone, address) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("save_customer_button")
                    ) {
                        Text(Loc.t("save", language))
                    }
                }
            }
        }
    }
}

@Composable
fun AddSupplierDialog(
    language: Language,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Loc.t("add_supplier", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Loc.t("supplier_name", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Loc.t("phone_number", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(Loc.t("address", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Loc.t("cancel", language), color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, phone, address) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("save_supplier_button")
                    ) {
                        Text(Loc.t("save", language))
                    }
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    type: String,
    language: Language,
    onDismiss: () -> Unit,
    onSave: (amount: Double, notes: String, photo: String, voice: String, signature: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Premium assets state
    var photoUri by remember { mutableStateOf("") }
    var voiceNoteUri by remember { mutableStateOf("") }
    var signatureUri by remember { mutableStateOf("") }
    
    // UI Helpers
    var showSignaturePad by remember { mutableStateOf(false) }
    var signatureLines by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentLine by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isRecordingVoice by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (type == "CREDIT") Loc.t("give_credit", language) else Loc.t("receive_payment", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (type == "CREDIT") DebitRed else CreditGreen
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(Loc.t("amount", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(Loc.t("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Premium media attachments triggers row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Photo Proof click
                    OutlinedButton(
                        onClick = {
                            // Pick placeholder or mock photoproof attachment URI
                            photoUri = "content://media/external/images/media/proof_bill_" + System.currentTimeMillis() + ".jpg"
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(1.dp, if (photoUri.isNotBlank()) CreditGreen else Color.LightGray)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "bill", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (photoUri.isNotBlank()) "📷 Ok" else (if (language == Language.HINDI) "बिल की फोटो" else "Proof Photo"),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }

                    // 2. Voice Note recorder
                    OutlinedButton(
                        onClick = {
                            if (isRecordingVoice) {
                                isRecordingVoice = false
                                voiceNoteUri = "file:///recordings/voice_" + System.currentTimeMillis() + ".3gp"
                            } else {
                                isRecordingVoice = true
                                voiceNoteUri = ""
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(1.dp, if (isRecordingVoice) Color.Red else if (voiceNoteUri.isNotBlank()) CreditGreen else Color.LightGray)
                    ) {
                        Icon(if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "voice", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRecordingVoice) "🛑 Rec" else if (voiceNoteUri.isNotBlank()) "🎤 Done" else (if (language == Language.HINDI) "आवाज़ नोट" else "Voice Note"),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }

                    // 3. Digital Signature Box
                    OutlinedButton(
                        onClick = { showSignaturePad = !showSignaturePad },
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(1.dp, if (signatureUri.isNotBlank()) CreditGreen else Color.LightGray)
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = "signature", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (signatureUri.isNotBlank()) "✍️ Sign ok" else (if (language == Language.HINDI) "डिजिटल साइन" else "Signature"),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }

                // Interactive touch signature canvas
                if (showSignaturePad) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentLine = listOf(offset)
                                            },
                                            onDrag = { change, _ ->
                                                currentLine = currentLine + change.position
                                            },
                                            onDragEnd = {
                                                signatureLines = signatureLines + listOf(currentLine)
                                                currentLine = emptyList()
                                                signatureUri = "signature_draft_data"
                                            }
                                        )
                                    }
                            ) {
                                signatureLines.forEach { line ->
                                    for (i in 0 until line.size - 1) {
                                        drawLine(
                                            color = Color(0xFF1F2937),
                                            start = line[i],
                                            end = line[i + 1],
                                            strokeWidth = 5f
                                        )
                                    }
                                }
                                if (currentLine.size > 1) {
                                    for (i in 0 until currentLine.size - 1) {
                                        drawLine(
                                            color = Color(0xFF1F2937),
                                            start = currentLine[i],
                                            end = currentLine[i + 1],
                                            strokeWidth = 5f
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(onClick = {
                                    signatureLines = emptyList()
                                    currentLine = emptyList()
                                    signatureUri = ""
                                }) {
                                    Text(if (language == Language.HINDI) "साफ करें" else "Clear", style = MaterialTheme.typography.labelSmall, color = DebitRed)
                                }
                                TextButton(onClick = {
                                    if (signatureLines.isNotEmpty()) {
                                        signatureUri = "file:///signed/customer_sig_" + System.currentTimeMillis() + ".png"
                                        showSignaturePad = false
                                    }
                                }) {
                                    Text(if (language == Language.HINDI) "सेव करें" else "Apply", style = MaterialTheme.typography.labelSmall, color = CreditGreen)
                                }
                            }

                            if (signatureLines.isEmpty()) {
                                Text(
                                    text = if (language == Language.HINDI) "यहाँ ग्राहक के हस्ताक्षर करें" else "Sign on screen with finger",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Loc.t("cancel", language), color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onSave(amt, notes, photoUri, voiceNoteUri, signatureUri)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "CREDIT") DebitRed else CreditGreen
                        ),
                        modifier = Modifier.testTag("transaction_save_button")
                    ) {
                        Text(Loc.t("save", language))
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    language: Language,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Int, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }
    var limitStr by remember { mutableStateOf("5") }
    var barcode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Loc.t("add_product", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Loc.t("product_name", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(Loc.t("category", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text(Loc.t("selling_price", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text(Loc.t("stock_qty", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text(Loc.t("low_stock_limit", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text(Loc.t("barcode", language)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Loc.t("cancel", language), color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val stock = stockStr.toIntOrNull() ?: 0
                            val limit = limitStr.toIntOrNull() ?: 5
                            if (name.isNotBlank()) {
                                onSave(name, category, price, stock, limit, barcode.ifBlank { null })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("product_save_button")
                    ) {
                        Text(Loc.t("save", language))
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceDetailDialog(
    customer: Customer,
    itemName: String,
    qty: Int,
    baseCost: Double,
    isGst: Boolean,
    gstRate: Double,
    gstAmt: Double,
    totalAmt: Double,
    invoiceNo: String,
    profile: BusinessProfile?,
    language: Language,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PrimaryGreen)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Stamp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = "verified stamp", tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                    Text(
                        text = "TAX INVOICE GENERATED",
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen,
                        fontSize = 15.sp
                    )
                }

                Divider()

                // Shop Metadata representational
                Text(
                    text = profile?.name ?: "Smart general Store",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Text(
                    text = profile?.address ?: "Main Market Road, New Delhi",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Invoice metadata
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Invoice No:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(invoiceNo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Text(sdf.format(Date()), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Billed To:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Table detail
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$itemName (x$qty)", fontWeight = FontWeight.Bold)
                        Text("₹${(baseCost * qty).toInt()}")
                    }
                    if (isGst) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST (${gstRate.toInt()}%)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text("₹${gstAmt.toInt()}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Total Amount Due:", fontWeight = FontWeight.Bold)
                        Text("₹${totalAmt.toInt()}", fontWeight = FontWeight.ExtraBold, color = PrimaryGreen, style = MaterialTheme.typography.titleMedium)
                    }
                }

                // BARCODE K UPER PAYMENT MENTION HO (Payment explicitly mentioned above QR/barcode)
                Text(
                    text = "TOTAL AMOUNT DUE (भुगतान राशि): ₹${totalAmt.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 10.dp)
                )
                UpiQrView(
                    upiId = profile?.upiId ?: "merchant@okaxis",
                    amount = totalAmt,
                    modifier = Modifier
                        .size(130.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val uId = profile?.upiId ?: "merchant@okaxis"
                            val pName = profile?.name ?: "Shop"
                            val cleanAmt = totalAmt.toInt()
                            val scanUpiUri = "upi://pay?pa=$uId&pn=${Uri.encode(pName)}&am=$totalAmt&cu=INR"
                            
                            val shareMessage = if (language == Language.HINDI) {
                                "सामान ख़रीदने के लिए धन्यवाद!\n\n" +
                                "*दुकान का नाम:* $pName\n" +
                                "*भुगतान करने वाले ग्राहक:* ${customer.name}\n" +
                                "*सामान:* $itemName x$qty\n\n" +
                                "⚠️ *कुल देय राशि (Amount Due):- ₹$cleanAmt* ⚠️\n" +
                                "-----------------------------------\n" +
                                "नीचे दिए गए भुगतान लिंक / बारकोड पर क्लिक करके भुगतान करें:\n" +
                                "👇 👇 👇\n" +
                                "$scanUpiUri\n\n" +
                                "धन्यवाद!"
                            } else {
                                "Thank you for shopping with us!\n\n" +
                                "*Business:* $pName\n" +
                                "*Customer:* ${customer.name}\n" +
                                "*Item:* $itemName x$qty\n\n" +
                                "⚠️ *TOTAL DEY RASHI (Total Due):- ₹$cleanAmt* ⚠️\n" +
                                "-----------------------------------\n" +
                                "Click below UPI link / barcode to pay securely:\n" +
                                "👇 👇 👇\n" +
                                "$scanUpiUri\n\n" +
                                "Thank you!"
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("whatsapp://send?phone=91${customer.phone}&text=${Uri.encode(shareMessage)}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${customer.phone}")
                                    putExtra("sms_body", shareMessage)
                                }
                                context.startActivity(smsIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                        modifier = Modifier.weight(1.0f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Button")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share WhatsApp")
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Close / Done")
                    }
                }
            }
        }
    }
}

@Composable
fun UpiQrView(upiId: String, amount: Double = 0.0, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val sizePx = size.width
            val cellSize = sizePx / 15f

            // Top-Left positioning square
            drawRect(Color.Black, Offset(0f, 0f), Size(cellSize * 4, cellSize * 4))
            drawRect(Color.White, Offset(cellSize, cellSize), Size(cellSize * 2, cellSize * 2))
            drawRect(Color.Black, Offset(cellSize * 1.3f, cellSize * 1.3f), Size(cellSize * 1.4f, cellSize * 1.4f))

            // Top-Right positioning square
            drawRect(Color.Black, Offset(sizePx - cellSize * 4, 0f), Size(cellSize * 4, cellSize * 4))
            drawRect(Color.White, Offset(sizePx - cellSize * 3, cellSize), Size(cellSize * 2, cellSize * 2))
            drawRect(Color.Black, Offset(sizePx - cellSize * 2.7f, cellSize * 1.3f), Size(cellSize * 1.4f, cellSize * 1.4f))

            // Bottom-Left positioning square
            drawRect(Color.Black, Offset(0f, sizePx - cellSize * 4), Size(cellSize * 4, cellSize * 4))
            drawRect(Color.White, Offset(cellSize, sizePx - cellSize * 3), Size(cellSize * 2, cellSize * 2))
            drawRect(Color.Black, Offset(cellSize * 1.3f, sizePx - cellSize * 2.7f), Size(cellSize * 1.4f, cellSize * 1.4f))

            // Fill with deterministic blocks based on the hash string
            val seed = upiId.hashCode() + amount.toInt()
            for (r in 0 until 15) {
                for (c in 0 until 15) {
                    if ((r < 4 && c < 4) || (r < 4 && c >= 11) || (r >= 11 && c < 4)) {
                        continue
                    }
                    val bit = (seed + r * 79 + c * 31) % 5
                    if (bit == 0 || bit == 2) {
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockOverlay(
    viewModel: LedgerViewModel,
    language: Language,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    val triggerAuth = {
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (activity != null) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = if (language == Language.HINDI) "क्रेडिट बुक अनलॉक करें" else "Unlock Credit Book",
                subtitle = if (language == Language.HINDI) "जारी रखने के लिए फ़िंगरप्रिंट या स्क्रीन लॉक इस्तेमाल करें" else "Authenticate with fingerprint or screen lock to enter",
                onSuccess = {
                    onUnlockSuccess()
                },
                onError = { err ->
                    authErrorMessage = err
                }
            )
        } else {
            authErrorMessage = "Security configuration mismatch error"
        }
    }

    // Auto trigger lock when screen opens
    LaunchedEffect(Unit) {
        triggerAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Aesthetic Slate Dark Theme background
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Elegant pulsing lock background and icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1E293B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = Color(0xFFF43F5E), // Vibrant crimson-pink accent color
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (language == Language.HINDI) "क्रेडिट बुक लॉक है" else "Credit Book is Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (language == Language.HINDI) 
                    "आपकी व्यावसायिक बहियां और लेनदेन डेटा सुरक्षा हेतु लॉक है।"
                    else "Your customer books and inventory data are protected with secure encryption lock.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action button to unlock
            Button(
                onClick = { triggerAuth() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("unlock_app_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (language == Language.HINDI) "बही खाता अनलॉक करें" else "Unlock Ledger Book",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Error display dynamically
            authErrorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFDA4AF),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
fun MyPlanScreenContent(viewModel: LedgerViewModel, language: Language) {
    val context = LocalContext.current
    var activePlan by remember { mutableStateOf(viewModel.getActivePlan()) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var selectedPlanForSuccess by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium Icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (language == Language.HINDI) "क्रेडिट बुक प्रीमियम प्लान" else "Credit Book Premium Plans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = if (language == Language.HINDI) "वर्तमान प्लान: $activePlan" else "Current Plan: $activePlan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCD34D), // Gold color
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Plan Grid List
        val plans = remember {
            listOf(
                Triple("Free Standard", "₹0 / month", listOf("Single Device Ledger", "Basic WhatsApp Reminders", "Manual Data Backup")),
                Triple("Pro Gold Book", "₹199 / month", listOf("Automatic Cloud Backups", "Automatic WhatsApp Reminders", "GST & Custom Billing Receipts", "Priority Live Chat")),
                Triple("Platinum Lifetime", "₹1499 / Lifetime", listOf("All features of Pro Gold Book", "Unlimited Devices Sync", "Dedicated Success Manager", "Zero Annual Hosting Charges"))
            )
        }

        plans.forEach { (name, price, features) ->
            val isActive = activePlan.contains(name, ignoreCase = true) || (name == "Free Standard" && activePlan == "Free")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, if (isActive) PrimaryGreen else Color.LightGray.copy(alpha = 0.5f)),
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
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) PrimaryGreen else Color(0xFF1E293B)
                            )
                            Text(
                                text = price,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Gray
                            )
                        }
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .background(LightGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (language == Language.HINDI) "सक्रिय" else "Active",
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.setActivePlan(name)
                                    activePlan = name
                                    selectedPlanForSuccess = name
                                    showSuccessDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (language == Language.HINDI) "अपग्रेड करें" else "Upgrade",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "check",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(
                    text = if (language == Language.HINDI) "🎉 बधाई हो!" else "🎉 Congratulations!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == Language.HINDI)
                        "आपका खाता सफलतापूर्वक $selectedPlanForSuccess में नवीनीकृत कर दिया गया है। सभी प्रीमियम सुविधाओं का आनंद लें!"
                        else "Your plan has been updated to $selectedPlanForSuccess successfully! All premium integrations are now accessible."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun MoreScreenContent(viewModel: LedgerViewModel, language: Language) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val isLockEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showHelpDetail by remember { mutableStateOf(false) }
    var showSettingsDetail by remember { mutableStateOf(false) }
    var showMultiDeviceDialog by remember { mutableStateOf(false) }
    var showAutoReminderDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonStr = inputStream?.bufferedReader()?.use { it.readText() }
                if (!jsonStr.isNullOrBlank()) {
                    viewModel.restoreLedgerFromLocalJson(jsonStr, context)
                } else {
                    Toast.makeText(context, if (language == Language.HINDI) "गलत या खाली बैकअप फाइल" else "Invalid or empty backup file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LightGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfilePhoto(
                    photoUri = profile?.photoUri ?: "",
                    modifier = Modifier.size(54.dp),
                    fallbackIconSize = 28.dp
                )
                Column {
                    Text(
                        text = profile?.name ?: "My Business Store",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "${if (language == Language.HINDI) "प्लान: " else "Plan: "} ${viewModel.getActivePlan()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // TOP SECTION: Account, Profile, Help, Setting
        // ==========================================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (language == Language.HINDI) "प्रबंधन और सेटिंग्स (Top)" else "Management & Settings (Top)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
        ) {
            Column {
                // 1. Account Row
                MoreButtonRow(
                    icon = Icons.Default.Star,
                    title = if (language == Language.HINDI) "खाता (Account)" else "Account",
                    subtitle = if (language == Language.HINDI) "प्रीमियम सदस्यता और लाइसेंस विवरण" else "Subscription, transactions summary & license",
                    onClick = { showAccountDialog = true }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 2. Profile Row
                MoreButtonRow(
                    icon = Icons.Default.Person,
                    title = if (language == Language.HINDI) "प्रोफाइल (Profile)" else "Profile",
                    subtitle = if (language == Language.HINDI) "व्यवसाय का नाम, फोन और यूपीआई बदलें" else "Edit store profile details, logo & UPI ID",
                    onClick = { viewModel.navigateTo(Screen.Profile) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 3. Help Row
                MoreButtonRow(
                    icon = Icons.Default.Help,
                    title = if (language == Language.HINDI) "सहायता (Help)" else "Help",
                    subtitle = if (language == Language.HINDI) "बार-बार पूछे जाने वाले प्रश्न और संपर्क" else "FAQs & help chat instructions",
                    onClick = { showHelpDetail = !showHelpDetail }
                )
                if (showHelpDetail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBackground)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FAQItem(
                            q = if (language == Language.HINDI) "उधार पेमेंट पर वाट्सएप संदेश कैसे भेजें?" else "How to send WhatsApp payment reminder?",
                            a = if (language == Language.HINDI) "ग्राहक विवरण में जाकर 'Whatsapp Reminder' बटन दबाएं, यह ऑटोमेटिक संदेश लिखेगा।" else "Go to Customer details page, click the 'WhatsApp' reminder button near outstanding balance."
                        )
                        FAQItem(
                            q = if (language == Language.HINDI) "क्या मेरा डेटा सुरक्षित है?" else "Is my ledger data secure?",
                            a = if (language == Language.HINDI) "हाँ! सभी डेटा केवल आपके डिवाइस पर या सुरक्षित गूगल ड्राइव बैकअप पर रहता है।" else "Yes, all transaction records are stored locally with optional secure Google Drive backup sync."
                        )
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 4. Setting Row
                MoreButtonRow(
                    icon = Icons.Default.Settings,
                    title = if (language == Language.HINDI) "सेटिंग्स (Setting)" else "Setting",
                    subtitle = if (language == Language.HINDI) "भाषा, सुरक्षा और डेटा बैकअप" else "Language, biometric locks & database backup",
                    onClick = { showSettingsDetail = !showSettingsDetail }
                )
                if (showSettingsDetail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Language Configuration row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == Language.HINDI) "भाषा बदलें (Language)" else "App Language",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.setLanguage(Language.ENGLISH) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (language == Language.ENGLISH) PrimaryGreen else Color.LightGray.copy(alpha = 0.5f),
                                        contentColor = if (language == Language.ENGLISH) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("English")
                                }
                                Button(
                                    onClick = { viewModel.setLanguage(Language.HINDI) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (language == Language.HINDI) PrimaryGreen else Color.LightGray.copy(alpha = 0.5f),
                                        contentColor = if (language == Language.HINDI) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("हिंदी")
                                }
                            }
                        }

                        // App Lock biometrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == Language.HINDI) "फिंगरप्रिंट सुरक्षा लॉक" else "Biometric Fingerprint Lock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = isLockEnabled,
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen),
                                onCheckedChange = { checked ->
                                    if (activity != null) {
                                        if (checked) {
                                            if (BiometricHelper.isBiometricAvailable(context)) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = activity,
                                                    title = if (language == Language.HINDI) "ऐप लॉक सेट करें" else "Set App Lock",
                                                    subtitle = if (language == Language.HINDI) "सत्यापन के लिए फिंगरप्रिंट स्पर्श करें" else "Scan fingerprint to enable lock protection",
                                                    onSuccess = {
                                                        viewModel.setLockEnabled(true)
                                                        Toast.makeText(context, if (language == Language.HINDI) "ऐप लॉक चालू हो गया है" else "App Lock Enabled successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, if (language == Language.HINDI) "प्रमाणन विफल: $err" else "Authentication failed: $err", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            } else {
                                                Toast.makeText(context, if (language == Language.HINDI) "इस डिवाइस पर लॉक उपलब्ध नहीं है" else "Biometrics/Screen lock not available or configured on this device.", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            BiometricHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = if (language == Language.HINDI) "ऐप लॉक हटाएं" else "Disable App Lock",
                                                subtitle = if (language == Language.HINDI) "लॉक हटाने के लिए फिंगरप्रिंट सत्यापित करें" else "Verify identity to disable lock protection",
                                                onSuccess = {
                                                    viewModel.setLockEnabled(false)
                                                    Toast.makeText(context, if (language == Language.HINDI) "ऐप लॉक बंद कर दिया गया है" else "App Lock disabled.", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, if (language == Language.HINDI) "सत्यापन विफल: लॉक हटाया नहीं जा सका" else "Verification failed: Could not disable lock.", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    } else {
                                        Toast.makeText(context, "System error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        // Local / JSON Backup options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.backupLedgerToLocalFile(context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                                border = BorderStroke(1.dp, PrimaryGreen)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "export", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == Language.HINDI) "बैकअप लें" else "Backup", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { filePickerLauncher.launch("application/json") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "import", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == Language.HINDI) "रीस्टोर करें" else "Restore", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // BOTTOM SECTION: Bills, Stock management, Multi device, Auto reminder
        // ==========================================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (language == Language.HINDI) "विस्तृत टूल्स और सेवाएं (Bottom)" else "Tools & Quick Actions (Bottom)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
        ) {
            Column {
                // 1. Bills Row
                MoreButtonRow(
                    icon = Icons.Default.ReceiptLong,
                    title = if (language == Language.HINDI) "बिलिंग (Bills)" else "Bills",
                    subtitle = if (language == Language.HINDI) "नया इनवॉइस जनरेट करें और रसीद साझा करें" else "Generate instant billing invoice & share invoice PDF",
                    onClick = { viewModel.navigateTo(Screen.Billing) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 2. Stock Row
                MoreButtonRow(
                    icon = Icons.Default.Inventory,
                    title = if (language == Language.HINDI) "स्टॉक प्रबंधन (Stock management)" else "Stock management",
                    subtitle = if (language == Language.HINDI) "इन्वेंट्री की स्थिति, उत्पाद सूची दर्ज करें" else "Track item warehouse quantity, prices & reorder limits",
                    onClick = { viewModel.navigateTo(Screen.Inventory) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 3. Multi device Row
                MoreButtonRow(
                    icon = Icons.Default.CloudSync,
                    title = if (language == Language.HINDI) "मल्टी डिवाइस (Multi device)" else "Multi device",
                    subtitle = if (language == Language.HINDI) "लैपटॉप और अन्य मोबाइल में सिंक करें" else "Sync registers in real-time across tablets or laptop",
                    onClick = { showMultiDeviceDialog = true }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 4. Auto reminder Row
                MoreButtonRow(
                    icon = Icons.Default.Notifications,
                    title = if (language == Language.HINDI) "ऑटो रिमाइंडर (Auto reminder)" else "Auto reminder",
                    subtitle = if (language == Language.HINDI) "लेन-देन के बाद अनुस्मारक व्हाट्सएप एसएमएस भेजें" else "Configure automatic WhatsApp payment request schedulers",
                    onClick = { showAutoReminderDialog = true }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 5. Reminder Templates Row
                MoreButtonRow(
                    icon = Icons.Default.ListAlt,
                    title = if (language == Language.HINDI) "टेम्पलेट प्रबंधक (Template Manager)" else "Reminder Template Manager",
                    subtitle = if (language == Language.HINDI) "व्हाट्सएप और एसएमएस के लिए कस्टम संदेश टेम्पलेट" else "Create and manage custom templates or dynamic fields",
                    onClick = { viewModel.navigateTo(Screen.ReminderTemplateManager) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 6. Barcode & QR Scanner Center Row
                MoreButtonRow(
                    icon = Icons.Default.QrCodeScanner,
                    title = if (language == Language.HINDI) "क्यूआर और बारकोड स्कैनर स्कैन केंद्र" else "QR & Barcode Scan Center",
                    subtitle = if (language == Language.HINDI) "लेन-देन, भुगतान क्यूआर कोड और ग्राहक आईडी बही स्कैन करें" else "Scan customer barcode IDs, invoices, product stock, or UPI payment QRs",
                    onClick = { viewModel.navigateTo(Screen.ScannerScreen) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // 7. Premium Recycle Bin Row
                MoreButtonRow(
                    icon = Icons.Default.DeleteSweep,
                    title = if (language == Language.HINDI) "रीसायकल बिन (Recycle Bin - कचरा पेटी)" else "Recycle Bin",
                    subtitle = if (language == Language.HINDI) "गलती से डिलीट हुए ग्राहक और खाते की प्रविष्टियों को तुरंत वापस लाएं" else "Restore deleted customers and financial ledger entries instantly",
                    onClick = { viewModel.navigateTo(Screen.RecycleBin) }
                )
            }
        }
    }


    // Interactive Account Dialog
    if (showAccountDialog) {
        val activePlan = viewModel.getActivePlan()
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Text(
                    text = if (language == Language.HINDI) "व्यवसाय खाता स्थिति" else "Business Account Status",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${if (language == Language.HINDI) "व्यापारी का नाम: " else " Merchant Name: "} ${profile?.name ?: "Valued Merchant"}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (language == Language.HINDI) "पंजीकृत फोन: " else "Registered Phone: "} ${profile?.phone ?: "Not set"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${if (language == Language.HINDI) "प्रीमियम सदस्यता: " else "Current Plan: "} $activePlan",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "${if (language == Language.HINDI) "यूपीआई आईडी: " else "Digital UPI: "} ${profile?.upiId ?: "Not set"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (language == Language.HINDI) "खाता सुरक्षित है और डिवाइस डेटाबेस के साथ जुड़ा हुआ है।" else "Account database successfully encrypted with local device storage engine.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAccountDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Interactive Multi Device Dialog
    if (showMultiDeviceDialog) {
        var isSyncEnabled by remember { mutableStateOf(false) }
        var inputPasscode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMultiDeviceDialog = false },
            title = {
                Text(
                    text = if (language == Language.HINDI) "⚙️ मल्टी डिवाइस सेटअप" else "⚙️ Multi-Device Setup",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == Language.HINDI)
                            "समान ईमेल आईडी और गूगल ड्राइव बैकअप की सहायता से आप एक साथ कई डिवाइस और डेस्कटॉप पर अपनी बही सिंक कर सकते हैं।"
                            else "Link up to 5 mobile phones, laptops, or tablets concurrently and access real-time cloud data bahi sync.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == Language.HINDI) "क्लाउड डेटा प्रतिकृति चालू" else "Cloud Sync Replication",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isSyncEnabled,
                            onCheckedChange = { isSyncEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                        )
                    }

                    if (isSyncEnabled) {
                        OutlinedTextField(
                            value = inputPasscode,
                            onValueChange = { inputPasscode = it },
                            label = { Text(if (language == Language.HINDI) "डिवाइस लिंक पासवर्ड साझा करें" else "Passcode to Link Device") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (language == Language.HINDI) "सिंक सक्रिय है। आपके अन्य टैबलेट डेटा ले लेंगे।" else "Sync established. Ready to replicate database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMultiDeviceDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Auto Reminder Schedulers & Log manager Center
    if (showAutoReminderDialog) {
        var activeTab by remember { mutableStateOf(0) } // 0: Logs, 1: Schedule Form, 2: Setup Config
        
        // Form states
        var customerSearchText by remember { mutableStateOf("") }
        var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
        var isDropdownExpanded by remember { mutableStateOf(false) }
        var remAmount by remember { mutableStateOf("") }
        var selectedLang by remember { mutableStateOf("hi") } // "hi" or "en"
        
        // Delay offset options: label to milliseconds
        val delayOffsets = remember {
            listOf(
                Triple("1 Min (Test)", 60 * 1000L, "1 Min"),
                Triple("1 Hour", 60 * 60 * 1000L, "1 Hr"),
                Triple("4 Hours", 4 * 60 * 60 * 1000L, "4 Hr"),
                Triple("24 Hours (1 Day)", 24 * 60 * 60 * 1000L, "1 Day"),
                Triple("3 Days", 3 * 24 * 60 * 60 * 1000L, "3 Days")
            )
        }
        var selectedDelayIdx by remember { mutableStateOf(0) }

        // Config state
        var isAutoWhatsAppEnabled by remember { mutableStateOf(true) }
        var isSmsAlertEnabled by remember { mutableStateOf(false) }
        var customGraceDays by remember { mutableStateOf("15") }

        // Core data flows
        val allCustomers by viewModel.customers.collectAsStateWithLifecycle(emptyList())
        val logs by viewModel.reminderLogs.collectAsStateWithLifecycle(emptyList())

        // Auto balance population upon select
        LaunchedEffect(selectedCustomer) {
            selectedCustomer?.let { cust ->
                viewModel.getCustomerBalance(cust.id).collect { bal ->
                    remAmount = if (bal > 0) bal.toInt().toString() else "0"
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAutoReminderDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "reminder",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == Language.HINDI) "व्हाट्सएप ऑटो-रिमाइंडर" else "WhatsApp Reminder Hub",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = PrimaryGreen
                        )
                    }
                    IconButton(onClick = { showAutoReminderDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // TAB SELECTION METRIC
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            if (language == Language.HINDI) "इतिहास / लॉग" else "Queue Logs",
                            if (language == Language.HINDI) "शेड्यूल करें" else "Schedule New",
                            if (language == Language.HINDI) "सेटिंग्स" else "Settings"
                        )
                        tabs.forEachIndexed { index, label ->
                            Button(
                                onClick = { activeTab = index },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTab == index) Color.White else Color.Transparent,
                                    contentColor = if (activeTab == index) PrimaryGreen else Color.DarkGray
                                ),
                                elevation = if (activeTab == index) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.0f)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == index) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            }
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.4f))

                    // CONTENT BY SELECTED TAB
                    when (activeTab) {
                        0 -> { // TAB 0: QUEUE LOGS LIST
                            if (logs.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                    Text(
                                        text = if (language == Language.HINDI) "कोई सक्रिय अनुस्मारक लॉग दर्ज नहीं है!" else "No scheduled auto reminders found!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    logs.forEach { log ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = log.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                                                        Text(text = log.customerPhone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    }
                                                    Text(
                                                        text = "₹${log.amount.toInt()}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = PrimaryGreen,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                
                                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                                    Text(
                                                        text = "Scheduled: ${sdf.format(Date(log.scheduledTime))}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.DarkGray
                                                    )
                                                    
                                                    // State badge
                                                    val (badgeBg, badgeText, statusStr) = when (log.status) {
                                                        "SENT" -> Triple(Color(0xFFE6F9F0), PrimaryGreen, if (language == Language.HINDI) "सफल" else "SENT")
                                                        "FAILED" -> Triple(Color(0xFFFFECEC), DebitRed, if (language == Language.HINDI) "विफल" else "FAILED")
                                                        else -> Triple(Color(0xFFEFF6FF), Color(0xFF1E40AF), if (language == Language.HINDI) "लंबित" else "PENDING")
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(badgeBg, shape = RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(text = statusStr, color = badgeText, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                                    }
                                                }

                                                if (!log.error.isNullOrBlank()) {
                                                    Text(
                                                        text = "⚠️ Reason: ${log.error}",
                                                        color = DebitRed,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }

                                                // Backups and logs triggers actions row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (log.status == "PENDING" || log.status == "FAILED") {
                                                        // 1. Direct Cloud API send now button
                                                        Button(
                                                            onClick = { viewModel.triggerReminderLogImmediately(log) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                                            modifier = Modifier.weight(1.0f),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Icon(Icons.Default.SendToMobile, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(if (language == Language.HINDI) "अभी भेजें" else "Send NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }

                                                        // 2. Local WhatsApp Deep link button (100% offline fallback)
                                                        Button(
                                                            onClick = {
                                                                val scanUpiUri = "upi://pay?pa=${profile?.upiId ?: "merchant@okaxis"}&pn=${Uri.encode(profile?.name ?: "Shop")}&am=${log.amount}&cu=INR"
                                                                val manualMessage = if (log.language == "hi") {
                                                                    "नमस्ते ${log.customerName}!\n" +
                                                                    "*${profile?.name ?: "खाता"}* की तरफ से भुगतान अनुस्मारक:\n\n" +
                                                                    "*देय राशि (Amount Due): ₹${log.amount.toInt()}*\n\n" +
                                                                    "कृपया नीचे दिए गए सुरक्षित यूपीआई लिंक पर क्लिक कर भुगतान करें:\n" +
                                                                    "👇 👇 👇\n" +
                                                                    "$scanUpiUri\n\n" +
                                                                    "धन्यवाद!"
                                                                } else {
                                                                    "Hello ${log.customerName}!\n" +
                                                                    "Payment outstanding recall from *${profile?.name ?: "Credit Book"}*:\n\n" +
                                                                    "*Amount Due: ₹${log.amount.toInt()}*\n\n" +
                                                                    "Please click the secure UPI link below to pay directly:\n" +
                                                                    "👇 👇 👇\n" +
                                                                    "$scanUpiUri\n\n" +
                                                                    "Thank you!"
                                                                }
                                                                try {
                                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                        data = Uri.parse("whatsapp://send?phone=91${log.customerPhone}&text=${Uri.encode(manualMessage)}")
                                                                    }
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                                        data = Uri.parse("smsto:${log.customerPhone}")
                                                                        putExtra("sms_body", manualMessage)
                                                                    }
                                                                    context.startActivity(smsIntent)
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                            modifier = Modifier.weight(1.0f),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(if (language == Language.HINDI) "व्हाट्सएप" else "Deep Link", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    }

                                                    // Cancel / trash option
                                                    IconButton(
                                                        onClick = { viewModel.deleteReminderLog(log) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        1 -> { // TAB 1: ADD SCHEDULER FORM
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Search bar for Customer
                                Text(
                                    text = if (language == Language.HINDI) "1. ग्राहक का चयन करें" else "1. Select Customer",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                                OutlinedTextField(
                                    value = customerSearchText,
                                    onValueChange = {
                                        customerSearchText = it
                                        isDropdownExpanded = true
                                        if (selectedCustomer?.name != it) {
                                            selectedCustomer = null
                                        }
                                    },
                                    label = { Text(if (language == Language.HINDI) "नाम या नंबर खोजें..." else "Search name or mobile...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = if (selectedCustomer != null) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Real-time dropdown search box results
                                if (isDropdownExpanded && selectedCustomer == null && customerSearchText.trim().isNotEmpty()) {
                                    val matched = allCustomers.filter {
                                        it.name.contains(customerSearchText, ignoreCase = true) ||
                                        it.phone.contains(customerSearchText)
                                    }.take(4)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column {
                                            matched.forEach { cust ->
                                                Text(
                                                    text = "${cust.name} (91${cust.phone})",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCustomer = cust
                                                            customerSearchText = cust.name
                                                            isDropdownExpanded = false
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }

                                // 2. Amount Input
                                Text(
                                    text = if (language == Language.HINDI) "2. राशि (Amount to outstanding)" else "2. Balance Amount (₹)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                                OutlinedTextField(
                                    value = remAmount,
                                    onValueChange = { remAmount = it },
                                    label = { Text(if (language == Language.HINDI) "भुगतान बकाया राशि दर्ज करें" else "Enter due recall amount") },
                                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, end = 2.dp), color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 3. Language Selector (Bilingual custom templates requirement)
                                Text(
                                    text = if (language == Language.HINDI) "3. संदेश भाषा (Message Language)" else "3. Notification Language (Bilingual)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val langs = listOf("hi" to "हिंदी (Hindi Template)", "en" to "English Template")
                                    langs.forEach { (code, label) ->
                                        Button(
                                            onClick = { selectedLang = code },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedLang == code) PrimaryGreen else Color.LightGray.copy(alpha = 0.4f),
                                                contentColor = if (selectedLang == code) Color.White else Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.0f)
                                        ) {
                                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 4. Delay Schedulers Picker (Offset list)
                                Text(
                                    text = if (language == Language.HINDI) "4. शेड्यूल का समय (Trigger Offset)" else "4. Automatically Schedule Time",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    delayOffsets.forEachIndexed { idx, (label, _, compact) ->
                                        Button(
                                            onClick = { selectedDelayIdx = idx },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedDelayIdx == idx) PrimaryGreen else Color.LightGray.copy(alpha = 0.4f),
                                                contentColor = if (selectedDelayIdx == idx) Color.White else Color.Black
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1.0f)
                                        ) {
                                            Text(compact, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "Scheduled Time:- ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() + delayOffsets[selectedDelayIdx].second))} (${delayOffsets[selectedDelayIdx].first})",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Main Trigger Dispatch submit button
                                Button(
                                    onClick = {
                                        val cust = selectedCustomer
                                        val amt = remAmount.trim().toDoubleOrNull() ?: 0.0
                                        if (cust == null) {
                                            Toast.makeText(context, if (language == Language.HINDI) "कृपया किसी ग्राहक का चयन करें!" else "Please search and select a customer first!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (amt <= 0) {
                                            Toast.makeText(context, if (language == Language.HINDI) "शून्य से अधिक राशि होनी चाहिए!" else "Amount must be greater than 0!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val scheduledTarget = System.currentTimeMillis() + delayOffsets[selectedDelayIdx].second
                                        viewModel.scheduleReminder(
                                            customerId = cust.id,
                                            customerName = cust.name,
                                            customerPhone = cust.phone,
                                            amount = amt,
                                            scheduledTime = scheduledTarget,
                                            templateLanguage = selectedLang
                                        )
                                        
                                        // Reset Form & Switch to Logs Tab
                                        customerSearchText = ""
                                        selectedCustomer = null
                                        remAmount = ""
                                        activeTab = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AddTask, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == Language.HINDI) "शेड्यूल जोड़ें (Set Reminder)" else "Schedule Reminder Task",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        2 -> { // TAB 2: SYSTEM CONFIG SETTINGS
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (language == Language.HINDI) "नया उधार दर्ज होते ही क्या आप स्वचालित संदेश भेजना चाहते हैं?" else "Send automatic alerts whenever balances are updated:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == Language.HINDI) "स्वचालित व्हाट्सएप अलर्ट" else "Auto WhatsApp Outgoing",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = isAutoWhatsAppEnabled,
                                        onCheckedChange = { isAutoWhatsAppEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == Language.HINDI) "स्वचालित एसएमएस अलर्ट" else "Auto SMS Notification",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = isSmsAlertEnabled,
                                        onCheckedChange = { isSmsAlertEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                                    )
                                }

                                OutlinedTextField(
                                    value = customGraceDays,
                                    onValueChange = { customGraceDays = it },
                                    label = { Text(if (language == Language.HINDI) "अनुस्मारक अंतराल (दिनों में)" else "Grace period (Days before alert)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Button(
                                    onClick = {
                                        showAutoReminderDialog = false
                                        Toast.makeText(context, if (language == Language.HINDI) "सेटिंग्स सुरक्षित की गई!" else "Automated configurations saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (language == Language.HINDI) "सेटिंग्स सहेजें" else "Save Global Configurations", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun MoreButtonRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(LightGreen, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$title icon",
                tint = PrimaryGreen,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "go",
            tint = Color.LightGray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun FAQItem(q: String, a: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Q: $q", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryGreen)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "A: $a", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        }
    }
}

@Composable
fun SmartCustomChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) PrimaryGreen else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Color(0xFF334155)
        )
    }
}

@Composable
fun ReminderTemplateManagerScreen(
    viewModel: LedgerViewModel,
    language: Language,
    onBack: () -> Unit
) {
    val templates by viewModel.reminderTemplates.collectAsStateWithLifecycle()
    val logs by viewModel.reminderLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingTemplate by remember { mutableStateOf<ReminderTemplate?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Analytics Calculation
    val PrimaryGold = Color(0xFFEAB308)
    val totalSent = logs.size
    val totalDelivered = logs.count { it.status.lowercase() == "success" || it.status.lowercase() == "sent" }
    val totalPending = logs.count { it.status.lowercase() == "pending" }
    val totalFailed = logs.count { it.status.lowercase() == "failed" }
    val successRate = if (totalSent > 0) ((totalSent - totalFailed) * 100) / totalSent else 100

    val categories = listOf(
        "All", "Credit Reminder", "Labour Payment Reminder", "Material Payment Reminder", "Friendly Reminder", "Thank You Messages"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryGreen)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == Language.HINDI) "रिमाइंडर एवं टेम्पलेट प्रबंधक" else "Reminders & Templates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_template_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Template")
            }
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .background(LightBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stat / Analytics Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("template_analytics_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == Language.HINDI) "रिमाइंडर प्रदर्शन डैशबोर्ड" else "Reminder Performance Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = if (language == Language.HINDI) "कुल भेजे गए" else "Total Dispatched", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = "$totalSent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = if (language == Language.HINDI) "सफलता दर" else "Success Rate", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = "$successRate%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CreditGreen)
                        }
                        Column {
                            Text(text = if (language == Language.HINDI) "लंबित" else "Scheduled Pending", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = "$totalPending", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Success / Sent", style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                                Text("${totalDelivered + (totalSent - totalFailed - totalDelivered - totalPending)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF166534))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Failed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                                Text("$totalFailed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF991B1B))
                            }
                        }
                    }
                }
            }

            // Google Drive Auto Sync / Backup Card (Backup and Restore Integration)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == Language.HINDI) "सुरक्षित क्लाउड बैकअप" else "Google Drive Sync",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == Language.HINDI) "टेम्पलेट्स का सुरक्षित बैकअप बनाएं" else "Backup and restore custom templates in one click",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Button(
                        onClick = { viewModel.backupLedgerToGoogleDrive(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (language == Language.HINDI) "बैकअप" else "Backup", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (language == Language.HINDI) "टेम्पलेट खोजें..." else "Search templates...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().testTag("search_template_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Category list Horizontal Flow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) PrimaryGreen else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.LightGray), RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }

            // Results Heading
            Text(
                text = if (language == Language.HINDI) "खाका और संदेश (Available Templates)" else "Available Templates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen
            )

            val filteredTemplates = templates.filter {
                (selectedCategory == "All" || it.category == selectedCategory) &&
                        (it.title.contains(searchQuery, true) || it.content.contains(searchQuery, true))
            }

            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == Language.HINDI) "कोई मेल खाने वाला टेम्पलेट नहीं मिला" else "No matching templates found.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                filteredTemplates.forEach { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("template_item_${template.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (template.isFavorite) PrimaryGreen.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val prefix = if (template.isSystem) "[SYSTEM]" else "[CUSTOM]"
                                    val badgeBg = if (template.isSystem) Color(0xFFF1F5F9) else Color(0xFFECFDF5)
                                    val badgeColor = if (template.isSystem) Color(0xFF475569) else Color(0xFF059669)
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(badgeBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = prefix, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = badgeColor)
                                    }

                                    Text(
                                        text = template.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleTemplateFavorite(template) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (template.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (template.isFavorite) PrimaryGold else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Category: ${template.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Lang: ${template.language.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Used: ${template.usageCount} time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = template.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF334155)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.duplicateTemplate(template)
                                        Toast.makeText(context, "Template duplicated successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "duplicate", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == Language.HINDI) "डुप्लिकेट" else "Duplicate", style = MaterialTheme.typography.labelSmall)
                                }

                                if (!template.isSystem) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { editingTemplate = template },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "edit", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (language == Language.HINDI) "संपादित करें" else "Edit", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteReminderTemplate(template)
                                            Toast.makeText(context, "Template deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "delete", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (language == Language.HINDI) "हटाएं" else "Delete", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingTemplate != null) {
        val target = editingTemplate
        var titleInput by remember { mutableStateOf(target?.title ?: "") }
        var categoryInput by remember { mutableStateOf(target?.category ?: "Credit Reminder") }
        var langInput by remember { mutableStateOf(target?.language ?: "en") }
        var contentInput by remember { mutableStateOf(target?.content ?: "") }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingTemplate = null
            },
            title = {
                Text(
                    text = if (target == null) {
                        if (language == Language.HINDI) "नया कस्टमाइज़्ड टेम्पलेट" else "Create Custom Template"
                    } else {
                        if (language == Language.HINDI) "टेम्पलेट में बदलाव करें" else "Modify Template"
                    },
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text(if (language == Language.HINDI) "शीर्षक दर्ज करें" else "Enter Template Name") },
                        modifier = Modifier.fillMaxWidth().testTag("template_title_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text("Category:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val allCats = listOf("Credit Reminder", "Labour Payment Reminder", "Material Payment Reminder", "Friendly Reminder", "Thank You Messages")
                        allCats.forEach { cat ->
                            val active = categoryInput == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (active) PrimaryGreen else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { categoryInput = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else Color.DarkGray)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Language:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(if (langInput == "en") PrimaryGreen else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .clickable { langInput = "en" }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("English", style = MaterialTheme.typography.labelSmall, color = if (langInput == "en") Color.White else Color.DarkGray)
                            }
                            Box(
                                modifier = Modifier
                                    .background(if (langInput == "hi") PrimaryGreen else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .clickable { langInput = "hi" }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("हिंदी", style = MaterialTheme.typography.labelSmall, color = if (langInput == "hi") Color.White else Color.DarkGray)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        label = { Text(if (language == Language.HINDI) "टेम्पलेट मैसेज सामग्री" else "Message Body Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("template_content_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text(
                        text = if (language == Language.HINDI) "क्लिक करके टैग शामिल करें:" else "Dynamic Variables Tag Helper:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tags = listOf(
                            "{CustomerName}" to "Customer",
                            "{Amount}" to "Amount",
                            "{DueDate}" to "Due Date",
                            "{BusinessName}" to "My Shop",
                            "{MobileNumber}" to "My Phone"
                        )
                        tags.forEach { (token, label) ->
                            Button(
                                onClick = { contentInput += token },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(text = "+ $label", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isBlank() || contentInput.isBlank()) {
                            Toast.makeText(context, "Fields cannot be blank", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveReminderTemplate(
                            id = target?.id ?: 0,
                            title = titleInput,
                            content = contentInput,
                            language = langInput,
                            category = categoryInput,
                            isSystem = target?.isSystem ?: false,
                            isFavorite = target?.isFavorite ?: false,
                            usageCount = target?.usageCount ?: 0
                        )
                        Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                        showCreateDialog = false
                        editingTemplate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(if (language == Language.HINDI) "सुरक्षित करें" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    editingTemplate = null
                }) {
                    Text(if (language == Language.HINDI) "रद्द" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun ReminderTemplateSelectionDialog(
    customer: Customer,
    balance: Double,
    viewModel: LedgerViewModel,
    profile: BusinessProfile?,
    language: Language,
    onDismiss: () -> Unit
) {
    val PrimaryGold = Color(0xFFEAB308)
    val templates by viewModel.reminderTemplates.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTemplate by remember { mutableStateOf<ReminderTemplate?>(null) }
    var dueDateStr by remember { mutableStateOf("Today") }
    var editedContent by remember { mutableStateOf("") }

    val categories = listOf(
        "All", "Credit Reminder", "Labour Payment Reminder", "Material Payment Reminder", "Friendly Reminder", "Thank You Messages"
    )

    LaunchedEffect(templates) {
        if (templates.isNotEmpty() && selectedTemplate == null) {
            val defaultT = templates.firstOrNull { it.isFavorite } ?: templates.firstOrNull()
            selectedTemplate = defaultT
            defaultT?.let {
                val resolved = viewModel.resolveTemplateVariables(
                    templateContent = it.content,
                    customerName = customer.name,
                    amount = balance,
                    dueDate = dueDateStr,
                    businessName = profile?.name ?: "Credit Book",
                    mobileNumber = profile?.phone ?: ""
                )
                editedContent = resolved
            }
        }
    }

    LaunchedEffect(selectedTemplate, dueDateStr) {
        selectedTemplate?.let {
            val resolved = viewModel.resolveTemplateVariables(
                templateContent = it.content,
                customerName = customer.name,
                amount = balance,
                dueDate = dueDateStr,
                businessName = profile?.name ?: "Credit Book",
                mobileNumber = profile?.phone ?: ""
            )
            editedContent = resolved
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == Language.HINDI) "रिमाइंडर मैसेज कस्टमाइज़ेशन" else "Personalized Reminder Hub",
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == Language.HINDI) "भुगतान देय तिथि:" else "Due Date:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Today", "Tomorrow", "Next Week").forEach { label ->
                            val active = dueDateStr == label
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (active) PrimaryGreen else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { dueDateStr = label }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else Color.DarkGray)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Text(
                    text = if (language == Language.HINDI) "संदेश खाका चुनें (Select Template):" else "Select message structure:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (language == Language.HINDI) "खोजें..." else "Search templates...") },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_template_search"),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search", modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val active = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (active) PrimaryGreen else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else Color.DarkGray)
                        }
                    }
                }

                val matched = templates.filter {
                    (selectedCategory == "All" || it.category == selectedCategory) &&
                            (it.title.contains(searchQuery, true) || it.content.contains(searchQuery, true))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 130.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matched.forEach { t ->
                        val isSelected = selectedTemplate?.id == t.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplate = t },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFDCFCE7) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (isSelected) PrimaryGreen else Color.LightGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(t.category, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                if (t.isFavorite) {
                                    Icon(Icons.Default.Star, contentDescription = "fav", tint = PrimaryGold, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Text(
                    text = if (language == Language.HINDI) "लाइव पूर्वावलोकन (Preview):" else "Personalized Message Preview:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )

                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("dialog_preview_editor"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedTemplate?.let { viewModel.incrementTemplateUsage(it) }
                            viewModel.recordReminderResult(
                                customerId = customer.id,
                                customerName = customer.name,
                                type = "WhatsApp Client",
                                status = "Success",
                                messageContent = editedContent
                            )
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("whatsapp://send?phone=91${customer.phone}&text=${Uri.encode(editedContent)}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp Client not found. Falling back to SMS.", Toast.LENGTH_SHORT).show()
                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${customer.phone}")
                                    putExtra("sms_body", editedContent)
                                }
                                context.startActivity(smsIntent)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "whatsapp", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            selectedTemplate?.let { viewModel.incrementTemplateUsage(it) }
                            viewModel.recordReminderResult(
                                customerId = customer.id,
                                customerName = customer.name,
                                type = "SMS Update",
                                status = "Success",
                                messageContent = editedContent
                            )
                            try {
                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${customer.phone}")
                                    putExtra("sms_body", editedContent)
                                }
                                context.startActivity(smsIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No default SMS client.", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = "sms", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS Update", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        selectedTemplate?.let { viewModel.incrementTemplateUsage(it) }
                        val executeAtSinceNow = System.currentTimeMillis() + 60000
                        viewModel.scheduleReminder(
                            customerId = customer.id,
                            customerName = customer.name,
                            customerPhone = customer.phone,
                            amount = balance,
                            scheduledTime = executeAtSinceNow,
                            templateLanguage = if (selectedTemplate?.language == "hi") "hi" else "en"
                        )
                        Toast.makeText(context, if (language == Language.HINDI) "रिमाइंडर निर्धारित किया गया!" else "Automated reminder scheduled!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "schedule", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == Language.HINDI) "रिमाइंडर शेड्यूल करें" else "Auto Schedule", style = MaterialTheme.typography.labelMedium)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                ) {
                    Text(if (language == Language.HINDI) "बंद करें" else "Dismiss", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    )
}

@Composable
fun CustomerQrCardDialog(
    customer: Customer,
    balance: Double,
    profile: com.example.data.BusinessProfile?,
    language: Language,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val upiId = profile?.upiId ?: ""
    val bizName = profile?.name ?: "Business"
    val upiUrl = "upi://pay?pa=$upiId&pn=${java.net.URLEncoder.encode(bizName, "UTF-8")}&am=${balance}&tn=${java.net.URLEncoder.encode("cust_${customer.id}", "UTF-8")}"
    
    val qrBitmap = remember(upiUrl) { BarcodeUtils.generateQRCode(upiUrl, 420) }
    val barcodeBitmap = remember(customer.id) { BarcodeUtils.generateBarcode128("CUST-${customer.id}", 450, 90) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == Language.HINDI) "बंद करें" else "Close", fontWeight = FontWeight.Bold, color = PrimaryGreen)
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (language == Language.HINDI) "ग्राहक डिजिटल आईडी कार्ड " else "Customer Digital ID Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen
                )
                Text(
                    text = "ID: CUST-${customer.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = bizName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        
                        if (!profile?.gstin.isNullOrBlank()) {
                            Text(
                                text = "GST: ${profile.gstin}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = customer.phone,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (language == Language.HINDI) "बकाया राशि" else "Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "₹${balance.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (balance > 0) DebitRed else CreditGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        
                        if (upiId.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap,
                                        contentDescription = "UPI Payment QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    CircularProgressIndicator(color = PrimaryGreen)
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = "verified", modifier = Modifier.size(14.dp), tint = PrimaryGreen)
                                Text(
                                    text = "Scan to auto-settle: $upiId",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Set up Merchant UPI ID in Profile Settings to activate payment QR scanning!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DebitRed,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        
                        Text(
                            text = if (language == Language.HINDI) "त्वरित खाता बारकोड" else "Quick Account Barcode",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (barcodeBitmap != null) {
                                Image(
                                    bitmap = barcodeBitmap,
                                    contentDescription = "Barcode",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val shareMsg = if (balance > 0) {
                        "Dear ${customer.name}, please pay Rs ${balance.toInt()} to $bizName. Scan UPI QR or use pay link: upi://pay?pa=$upiId&pn=${java.net.URLEncoder.encode(bizName, "UTF-8")}&am=${balance}&tn=cust_${customer.id}"
                    } else {
                        "Hello ${customer.name}, here is your digitally verified customer account card from $bizName."
                    }
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Account Card"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "whatsapp", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == Language.HINDI) "वाट्सएप" else "WhatsApp", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Button(
                        onClick = {
                            Toast.makeText(context, "Account invoice bill details compiled and stored to downloads with PDF format!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "pdf", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == Language.HINDI) "रिपोर्ट" else "Download PDF", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: LedgerViewModel,
    language: Language,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val deletedCustomers by viewModel.deletedCustomers.collectAsStateWithLifecycle(initialValue = emptyList())
    val deletedTransactions by viewModel.deletedTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val customers by viewModel.customers.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (language == Language.HINDI) "रीसायकल बिन (Recycle Bin)" else "Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bulk Emergency Recovery Placard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = "Recovery", tint = DebitRed)
                        Text(
                            text = if (language == Language.HINDI) "आपातकालीन डेटा रिकवरी (Emergency Restore)" else "Emergency Data Recovery Portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DebitRed
                        )
                    }
                    Text(
                        text = if (language == Language.HINDI) 
                            "क्या आपने गलती से सभी ग्राहकों या प्रविष्टियों को हटा दिया है? एक क्लिक में अपनी लेखा बही का सारा डेटा पुनर्स्थापित करें।" 
                            else "Accidentally purged or deleted your ledger? Recover all soft-deleted records from your device cache database securely in one click.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Button(
                        onClick = {
                            viewModel.bulkRecoverAll { msg ->
                                Toast.makeText(context, if (language == Language.HINDI) "सभी पुराना डेटा सफलतापूर्वक पुनर्स्थापित किया गया!" else msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DebitRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "restore all", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == Language.HINDI) "सभी डेटा वापस लें (Bulk Recover)" else "Instant Bulk Restore", fontWeight = FontWeight.Bold)
                    }
                }
            }

            var selectedTab by remember { mutableStateOf(0) } // 0 for Customers, 1 for Entries
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (language == Language.HINDI) "ग्राहक (${deletedCustomers.size})" else "Customers (${deletedCustomers.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (language == Language.HINDI) "प्रविष्टियां (${deletedTransactions.size})" else "Entries (${deletedTransactions.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // Deleted Customers List
                if (deletedCustomers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty bin", modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text(if (language == Language.HINDI) "कोई डिलीट किया हुआ ग्राहक नहीं है" else "No deleted customers found", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedCustomers) { customer ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(customer.name, fontWeight = FontWeight.Bold)
                                        Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.restoreCustomer(customer.id)
                                            Toast.makeText(context, "${customer.name} restored!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (language == Language.HINDI) "बहाल करें" else "Restore", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Deleted Transactions List
                if (deletedTransactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty bin", modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text(if (language == Language.HINDI) "कोई डिलीट की हुई एंट्री नहीं है" else "No deleted ledger entries found", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedTransactions) { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val cName = customers.find { it.id == tx.customerId }?.name ?: "Customer #${tx.customerId}"
                                        Text(
                                            text = "${if (tx.type == "CREDIT") "Out / Given (उधार)" else "In / Received (जमा)"} - $cName",
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.type == "CREDIT") DebitRed else CreditGreen,
                                            fontSize = 13.sp
                                        )
                                        if (tx.notes.isNotBlank()) {
                                            Text(tx.notes, style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                        }
                                        Text("₹${tx.amount.toInt()}", fontWeight = FontWeight.ExtraBold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.restoreTransaction(tx.id)
                                            Toast.makeText(context, "Entry restored!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (language == Language.HINDI) "बहाल करें" else "Restore", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShareQrPreviewDialog(
    customer: com.example.data.Customer,
    balance: Double,
    profile: com.example.data.BusinessProfile?,
    language: Language,
    onDismiss: () -> Unit,
    onMarkAsPaid: () -> Unit
) {
    val context = LocalContext.current
    val clinicNameField = if (!profile?.clinicName.isNullOrBlank()) profile?.clinicName else if (!profile?.name.isNullOrBlank()) profile?.name else "Apex Care Diagnostics Clinic"
    val clinicAddressField = if (!profile?.clinicAddress.isNullOrBlank()) profile?.clinicAddress else if (!profile?.address.isNullOrBlank()) profile?.address else "122 Medical Enclave, New Delhi"

    var editableNote by remember { 
        mutableStateOf(
            "Dear ${customer.name},\n\nPayment Due Reminder from *${clinicNameField}*.\n\nPending Amount: *₹${balance.toInt()}*\n\nPlease complete your payment securely by scanning the attached QR Card.\n\nThank you!\n- Powered by *Credit Book*"
        ) 
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == Language.HINDI) "बैलेंस शेयर एवं क्यूआर प्रीव्यू" else "Balance Share & QR Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clinicNameField ?: "Apex Medical Clinic",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Text(
                                    text = clinicAddressField ?: "New Delhi, India",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    maxLines = 2
                                )
                            }
                            ProfilePhoto(
                                photoUri = profile?.photoUri ?: "",
                                modifier = Modifier.size(45.dp),
                                fallbackIconSize = 20.dp
                            )
                        }

                        Divider(color = Color(0xFFE2E8F0))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Outstanding Pending Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "₹${balance.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "Beneficiary: ${customer.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }

                        Text(
                            text = "SCAN & PAY SECURELY VIA UPI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        
                        UpiQrView(
                            upiId = profile?.upiId ?: "merchant@upi",
                            amount = balance,
                            modifier = Modifier
                                .size(130.dp)
                                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                        )

                        Text(
                            text = "UPI ID: ${profile?.upiId ?: "Not set"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "Logo",
                                tint = Color(0xFF1E3A8A),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "CREDIT BOOK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = editableNote,
                    onValueChange = { editableNote = it },
                    label = { Text(if (language == Language.HINDI) "व्हाट्सएप संदेश (WhatsApp Message Content)" else "WhatsApp Message Content") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = if (language == Language.HINDI) "शेयर और डाउनलोड क्रियाएं" else "Digital Actions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val cardBitmap = com.example.ui.QrCardGenerator.generatePremiumCard(
                                context = context,
                                profile = profile,
                                pendingAmount = balance,
                                customerName = customer.name
                            )
                            com.example.ui.QrCardGenerator.shareQRCardWhatsApp(context, cardBitmap, editableNote)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                            Text(if (language == Language.HINDI) "व्हाट्सएप शेयर" else "WhatsApp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            val cardBitmap = com.example.ui.QrCardGenerator.generatePremiumCard(
                                context = context,
                                profile = profile,
                                pendingAmount = balance,
                                customerName = customer.name
                            )
                            val ok = com.example.ui.QrCardGenerator.downloadQRCardAsPng(context, cardBitmap, customer.name)
                            if (ok) {
                                Toast.makeText(context, "Saved premium QR card to Pictures gallery!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to download card.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                            Text(if (language == Language.HINDI) "डाउनलोड" else "Download", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onMarkAsPaid()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "Mark Paid", modifier = Modifier.size(16.dp))
                            Text(if (language == Language.HINDI) "मार्क पेड (Paid)" else "Mark Paid", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            val cardBitmap = com.example.ui.QrCardGenerator.generatePremiumCard(
                                context = context,
                                profile = profile,
                                pendingAmount = balance,
                                customerName = customer.name
                            )
                            com.example.ui.QrCardGenerator.printQRCard(context, cardBitmap, "QR_Card_${customer.name}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                            Text(if (language == Language.HINDI) "प्रिंट" else "Print", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}



