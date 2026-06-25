package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

sealed class Screen {
    object Dashboard : Screen()
    object Customers : Screen()
    object Suppliers : Screen()
    object Inventory : Screen()
    object Billing : Screen()
    object Profile: Screen()
    object MyPlan : Screen()
    object More : Screen()
    object ReminderTemplateManager : Screen()
    object ScannerScreen : Screen()
    data class CustomerDetail(val customerId: Int) : Screen()
    data class SupplierDetail(val supplierId: Int) : Screen()
    object RecycleBin : Screen()
}

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val db = AppDatabase.getInstance(application)

    private val repository = LedgerRepository(db.ledgerDao())

    // Persistent Active Profile Configuration
    private val prefs = application.getSharedPreferences("smart_khata_prefs", Context.MODE_PRIVATE)

    fun getActivePlan(): String {
        return prefs.getString("active_plan", "Free") ?: "Free"
    }

    fun setActivePlan(plan: String) {
        prefs.edit().putString("active_plan", plan).apply()
    }

    // Language state
    private val _language = MutableStateFlow(
        Language.values().getOrElse(prefs.getInt("app_language_ord", Language.ENGLISH.ordinal)) { Language.ENGLISH }
    )
    val language: StateFlow<Language> = _language.asStateFlow()

    // Screen State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setLanguage(lang: Language) {
        prefs.edit().putInt("app_language_ord", lang.ordinal).apply()
        _language.value = lang
        val locale = when (lang) {
            Language.HINDI -> Locale("hi", "IN")
            Language.BENGALI -> Locale("bn", "IN")
            Language.TELUGU -> Locale("te", "IN")
            Language.MARATHI -> Locale("mr", "IN")
            Language.TAMIL -> Locale("ta", "IN")
            Language.GUJARATI -> Locale("gu", "IN")
            Language.URDU -> Locale("ur", "IN")
            Language.KANNADA -> Locale("kn", "IN")
            Language.MALAYALAM -> Locale("ml", "IN")
            Language.PUNJABI -> Locale("pa", "IN")
            else -> Locale.US
        }
        try {
            tts?.setLanguage(locale)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchLanguage() {
        val nextLang = if (_language.value == Language.ENGLISH) Language.HINDI else Language.ENGLISH
        setLanguage(nextLang)
    }

    // Search query states
    val customerQuery = MutableStateFlow("")
    val supplierQuery = MutableStateFlow("")
    val productQuery = MutableStateFlow("")

    private val _activeProfileId = MutableStateFlow(prefs.getInt("active_profile_id", 1))
    val activeProfileId: StateFlow<Int> = _activeProfileId.asStateFlow()

    // Fingerprint / Biometric Lock configuration
    private val _isLockEnabled = MutableStateFlow(prefs.getBoolean("is_lock_enabled", false))
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isUnlocked = MutableStateFlow(!prefs.getBoolean("is_lock_enabled", false))
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_lock_enabled", enabled).apply()
        _isLockEnabled.value = enabled
        if (!enabled) {
            _isUnlocked.value = true
        }
    }

    fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
    }

    // Observe all business profiles
    val allBusinessProfiles: StateFlow<List<BusinessProfile>> = repository.allBusinessProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observe active profile dynamically
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val businessProfile: StateFlow<BusinessProfile?> = _activeProfileId
        .flatMapLatest { id -> repository.getBusinessProfileById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Data streams (automatically filtered by the current selected profile ID)
    val customers: StateFlow<List<Customer>> = combine(repository.allCustomers, _activeProfileId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = combine(repository.allSuppliers, _activeProfileId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = combine(repository.allTransactions, _activeProfileId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = combine(repository.allProducts, _activeProfileId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanLogs: StateFlow<List<ScanLog>> = repository.allScanLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Premium features state streams
    val deletedCustomers: StateFlow<List<Customer>> = repository.deletedCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedTransactions: StateFlow<List<Transaction>> = repository.deletedTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var lastInsertedTransactionId: Int? = null
        private set

    fun undoLastTransaction(onComplete: (String) -> Unit) {
        val lastId = lastInsertedTransactionId
        if (lastId != null) {
            viewModelScope.launch {
                repository.softDeleteTransaction(lastId)
                lastInsertedTransactionId = null
                onComplete("Last transaction undo successful!")
            }
        } else {
            onComplete("No recent transaction found to undo!")
        }
    }

    fun toggleCustomerFavourite(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer.copy(isFavourite = !customer.isFavourite))
        }
    }

    fun updateCustomerTag(customer: Customer, newTag: String) {
        viewModelScope.launch {
            repository.updateCustomer(customer.copy(tags = newTag))
        }
    }

    fun updateCustomerProfileDetails(customer: Customer, notes: String, photoUri: String, docUri: String) {
        viewModelScope.launch {
            repository.updateCustomer(customer.copy(
                notes = notes,
                photoUri = photoUri,
                documentUri = docUri
            ))
        }
    }

    fun softDeleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.softDeleteCustomer(customer.id)
            if (_currentScreen.value is Screen.CustomerDetail && (_currentScreen.value as Screen.CustomerDetail).customerId == customer.id) {
                _currentScreen.value = Screen.Customers
            }
        }
    }

    fun softDeleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.softDeleteTransaction(id)
        }
    }

    fun restoreCustomer(id: Int) {
        viewModelScope.launch {
            repository.restoreCustomer(id)
        }
    }

    fun restoreTransaction(id: Int) {
        viewModelScope.launch {
            repository.restoreTransaction(id)
        }
    }

    fun bulkRecoverAll(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            repository.bulkRecoverCustomers()
            repository.bulkRecoverTransactions()
            onComplete("Emergency Data Recovery Completed! All deleted records restored successfully.")
        }
    }

    fun clearScanLogs() {
        viewModelScope.launch {
            repository.clearScanLogs()
        }
    }

    fun handleScanPayload(payload: String, scanType: String, onNotification: (String) -> Unit) {
        viewModelScope.launch {
            var resolvedEntity = "Unrecognized Code"
            var actionTaken = "Recorded in log"

            if (payload.startsWith("SmartKhata_Customer_")) {
                val customerId = payload.substringAfter("SmartKhata_Customer_").toIntOrNull()
                if (customerId != null) {
                    val customer = customers.value.find { it.id == customerId }
                    if (customer != null) {
                        resolvedEntity = "Customer: ${customer.name}"
                        actionTaken = "Opened customer ledger"
                        navigateTo(Screen.CustomerDetail(customerId))
                        onNotification("Customer Account [${customer.name}] Opened!")
                    } else {
                        resolvedEntity = "Customer ID #$customerId"
                        actionTaken = "Encountered search error: Customer not found"
                        onNotification("Customer was not found in records.")
                    }
                }
            } else if (payload.startsWith("SmartKhata_Invoice_")) {
                val transactionId = payload.substringAfter("SmartKhata_Invoice_").toIntOrNull()
                if (transactionId != null) {
                    val tx = transactions.value.find { it.id == transactionId }
                    if (tx != null) {
                        resolvedEntity = "Invoice #${tx.id} - Rs ${tx.amount}"
                        actionTaken = "Loaded invoice details"
                        onNotification("Scanned Invoice #${tx.id} for Rs ${tx.amount}!")
                        if (tx.customerId != null) {
                            navigateTo(Screen.CustomerDetail(tx.customerId))
                        }
                    } else {
                        resolvedEntity = "Invoice ID #$transactionId"
                        actionTaken = "Encountered search error: Transaction not found"
                        onNotification("Invoice not found in transaction history.")
                    }
                }
            } else if (payload.startsWith("upi://pay")) {
                // Parse UPI URI parameters manually
                val parts = payload.substringAfter("?").split("&")
                var pa = "Unknown UPI ID"
                var am = "0.0"
                var tn = "Payment"
                var pn = ""
                for (part in parts) {
                    val kv = part.split("=")
                    if (kv.size == 2) {
                        when (kv[0]) {
                            "pa" -> pa = kv[1]
                            "am" -> am = kv[1]
                            "tn" -> tn = java.net.URLDecoder.decode(kv[1], "UTF-8")
                            "pn" -> pn = java.net.URLDecoder.decode(kv[1], "UTF-8")
                        }
                    }
                }
                val amount = am.toDoubleOrNull() ?: 0.0
                resolvedEntity = "UPI payment QR code (UPI ID: $pa, Amount: Rs $amount)"

                if (amount > 0.0) {
                    // Match customer dynamically
                    var matchedCustomer: Customer? = null
                    // Extract cust_id or CUST-ID
                    val custIdInNote = tn.substringAfter("cust_", "").substringBefore(" ").toIntOrNull()
                        ?: tn.substringAfter("CUST-", "").substringBefore(" ").toIntOrNull()
                        ?: tn.toIntOrNull()

                    if (custIdInNote != null) {
                        matchedCustomer = customers.value.find { it.id == custIdInNote }
                    } else {
                        // Match by name
                        matchedCustomer = customers.value.find { tn.contains(it.name, ignoreCase = true) || pn.contains(it.name, ignoreCase = true) }
                    }

                    if (matchedCustomer != null) {
                        val newTx = Transaction(
                            customerId = matchedCustomer.id,
                            type = "PAYMENT",
                            amount = amount,
                            notes = "[QR PAY] UPI Ref: QR-${System.currentTimeMillis() % 1000000} ($tn)",
                            profileId = _activeProfileId.value
                        )
                        repository.insertTransaction(newTx)
                        actionTaken = "Payment of Rs $amount automatically recorded for customer ${matchedCustomer.name}"
                        onNotification("Payment Received! Rs $amount transaction logged for ${matchedCustomer.name}.")
                        navigateTo(Screen.CustomerDetail(matchedCustomer.id))
                    } else {
                        actionTaken = "Scanned UPI payment link"
                        onNotification("UPI QR: Rs $amount, but no matching customer found to record payment.")
                    }
                } else {
                    actionTaken = "Scanned raw UPI code without payment amount"
                    onNotification("Scanned generic UPI Payment link for $pn ($pa).")
                }
            } else {
                // Product material barcode lookup
                val product = products.value.find { it.barcode == payload }
                if (product != null) {
                    resolvedEntity = "Product: ${product.name}"
                    actionTaken = "Stock check of ${product.name} (Current level: ${product.stock})"
                    onNotification("Material match: ${product.name}. Current stock: ${product.stock}.")
                    navigateTo(Screen.Inventory)
                } else {
                    // Check if it matches any customer ID directly
                    val matchedCust = customers.value.find { it.phone == payload || "CUST-${it.id}" == payload }
                    if (matchedCust != null) {
                        resolvedEntity = "Customer: ${matchedCust.name}"
                        actionTaken = "Opened ledger from customer ID barcode match"
                        navigateTo(Screen.CustomerDetail(matchedCust.id))
                        onNotification("Customer Account [${matchedCust.name}] Opened via Barcode!")
                    } else {
                        resolvedEntity = "Raw: $payload"
                        actionTaken = "Unrecognized scan payload logged"
                        onNotification("Scanned code: $payload (No matching entity found)")
                    }
                }
            }

            repository.insertScanLog(
                ScanLog(
                    payload = payload,
                    scanType = scanType,
                    resolvedEntity = resolvedEntity,
                    actionTaken = actionTaken
                )
            )
        }
    }

    init {
        // Initialize TextToSpeech engine
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                isTtsInitialized = true
            }
        }

        // Initialize active profile if empty on startups
        selectProfile(_activeProfileId.value)

        // Seed Default Reminder Templates
        viewModelScope.launch {
            repository.allReminderTemplates.first().let { list ->
                if (list.isEmpty()) {
                    seedDefaultReminderTemplates()
                }
            }
        }
    }

    // Swapping profile and auto-generating standard defaults if newly visited
    fun selectProfile(id: Int) {
        if (id in 1..10) {
            prefs.edit().putInt("active_profile_id", id).apply()
            _activeProfileId.value = id

            viewModelScope.launch {
                val existing = repository.getBusinessProfileById(id).first()
                if (existing == null) {
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
                    val defaultName = defaultNamesByIndex.getOrNull(id - 1) ?: "Smart Shop #$id"
                    repository.insertBusinessProfile(
                        BusinessProfile(
                            id = id,
                            name = defaultName,
                            phone = "98765432${id - 1}",
                            address = "Market Square, Sector $id, New Delhi",
                            upiId = "merchant.shop$id@okaxis",
                            gstin = "07AAAAA111${id}A1Z1",
                            photoUri = "preset_$id"
                        )
                    )
                }
            }
        }
    }

    // Filtered lists
    val filteredCustomers: StateFlow<List<Customer>> = combine(customers, customerQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSuppliers: StateFlow<List<Supplier>> = combine(suppliers, supplierQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<Product>> = combine(products, productQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) || (it.barcode ?: "").contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations & Statistics
    val dashboardStats = combine(customers, suppliers, transactions) { custs, supps, txs ->
        // Live Udhar calculations
        var totalReceivable = 0.0
        var totalPayable = 0.0
        var todaySales = 0.0
        val nowMillis = System.currentTimeMillis()
        val startOfToday = nowMillis - (nowMillis % (24 * 60 * 60 * 1000))

        // Calculate for Customers
        custs.forEach { customer ->
            val custTxs = txs.filter { it.customerId == customer.id }
            val credit = custTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
            val payment = custTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
            val balance = credit - payment
            if (balance > 0) {
                totalReceivable += balance
            }
        }

        // Calculate for Suppliers
        supps.forEach { supplier ->
            val suppTxs = txs.filter { it.supplierId == supplier.id }
            val purchase = suppTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
            val payment = suppTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
            val balance = purchase - payment
            if (balance > 0) {
                totalPayable += balance
            }
        }

        // Today's total sales transaction
        val startOfDayMillis = System.currentTimeMillis() - (86400000) // approx 24h
        txs.forEach { tx ->
            if (tx.date >= startOfDayMillis && tx.customerId != null && tx.type == "CREDIT") {
                todaySales += tx.amount
            }
        }

        DashboardStats(
            receivable = totalReceivable,
            payable = totalPayable,
            todaySales = todaySales,
            totalTransactions = txs.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Customer balance calculations
    fun getCustomerBalance(customerId: Int): Flow<Double> = transactions.map { list ->
        val userTxs = list.filter { it.customerId == customerId }
        val credit = userTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val payment = userTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
        credit - payment
    }

    // Supplier balance calculations
    fun getSupplierBalance(supplierId: Int): Flow<Double> = transactions.map { list ->
        val userTxs = list.filter { it.supplierId == supplierId }
        val purchase = userTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val payment = userTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
        purchase - payment
    }

    // Operations
    fun addCustomer(name: String, phone: String, address: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertCustomer(Customer(name = name, phone = phone, address = address, profileId = _activeProfileId.value))
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.softDeleteCustomer(customer.id)
            // also clean up screen if necessary
            if (_currentScreen.value is Screen.CustomerDetail && (_currentScreen.value as Screen.CustomerDetail).customerId == customer.id) {
                _currentScreen.value = Screen.Customers
            }
        }
    }

    fun addSupplier(name: String, phone: String, address: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertSupplier(Supplier(name = name, phone = phone, address = address, profileId = _activeProfileId.value))
            }
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.updateSupplier(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            if (_currentScreen.value is Screen.SupplierDetail && (_currentScreen.value as Screen.SupplierDetail).supplierId == supplier.id) {
                _currentScreen.value = Screen.Suppliers
            }
        }
    }

    fun addTransaction(
        customerId: Int?,
        supplierId: Int?,
        type: String,
        amount: Double,
        notes: String,
        invoiceNumber: String? = null,
        isGSTInvoice: Boolean = false,
        gstRate: Double = 0.0,
        gstAmount: Double = 0.0,
        photoUri: String = "",
        voiceNoteUri: String = "",
        signatureUri: String = ""
    ) {
        viewModelScope.launch {
            if (amount > 0) {
                // Fetch entity name from state caches
                val entityName = if (customerId != null) {
                    customers.value.find { it.id == customerId }?.name ?: ""
                } else if (supplierId != null) {
                    suppliers.value.find { it.id == supplierId }?.name ?: ""
                } else {
                    ""
                }

                val rowId = repository.insertTransaction(
                    Transaction(
                        customerId = customerId,
                        supplierId = supplierId,
                        type = type,
                        amount = amount,
                        notes = notes,
                        invoiceNumber = invoiceNumber,
                        isGSTInvoice = isGSTInvoice,
                        gstRate = gstRate,
                        gstAmount = gstAmount,
                        date = System.currentTimeMillis(),
                        profileId = _activeProfileId.value,
                        photoUri = photoUri,
                        voiceNoteUri = voiceNoteUri,
                        signatureUri = signatureUri
                    )
                )
                lastInsertedTransactionId = rowId.toInt()

                // Speak out the amount received/given
                val isHindi = _language.value == Language.HINDI
                val speaksText = if (isHindi) {
                    if (customerId != null) {
                        if (type == "PAYMENT") {
                            if (entityName.isNotBlank()) "$entityName से ${amount.toInt()} रुपये प्राप्त हुए।" else "${amount.toInt()} रुपये प्राप्त हुए।"
                        } else {
                            if (entityName.isNotBlank()) "$entityName को ${amount.toInt()} रुपये का उधार दिया।" else "${amount.toInt()} रुपये का उधार दिया।"
                        }
                    } else if (supplierId != null) {
                        if (type == "PAYMENT") {
                            if (entityName.isNotBlank()) "$entityName को ${amount.toInt()} रुपये का भुगतान किया।" else "${amount.toInt()} रुपये का भुगतान किया।"
                        } else {
                            if (entityName.isNotBlank()) "$entityName से ${amount.toInt()} रुपये का उधार सामान लिया।" else "${amount.toInt()} रुपये का उधार सामान लिया।"
                        }
                    } else {
                        "${amount.toInt()} रुपये का भुगतान।"
                    }
                } else {
                    if (customerId != null) {
                        if (type == "PAYMENT") {
                            if (entityName.isNotBlank()) "Received ${amount.toInt()} rupees from $entityName." else "Received ${amount.toInt()} rupees."
                        } else {
                            if (entityName.isNotBlank()) "Rupees ${amount.toInt()} credited to $entityName." else "Rupees ${amount.toInt()} credited."
                        }
                    } else if (supplierId != null) {
                        if (type == "PAYMENT") {
                            if (entityName.isNotBlank()) "Paid ${amount.toInt()} rupees to $entityName." else "Paid ${amount.toInt()} rupees."
                        } else {
                            if (entityName.isNotBlank()) "Credit of ${amount.toInt()} rupees taken from $entityName." else "Credit of ${amount.toInt()} rupees taken."
                        }
                    } else {
                        "Rupees ${amount.toInt()} transacted."
                    }
                }

                speak(speaksText)
            }
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.softDeleteTransaction(id)
        }
    }

    fun addProduct(name: String, category: String, price: Double, stock: Int, lowLimit: Int, barcode: String? = null) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertProduct(
                    Product(
                        name = name,
                        category = category,
                        price = price,
                        stock = stock,
                        lowStockLimit = lowLimit,
                        barcode = barcode,
                        profileId = _activeProfileId.value
                    )
                )
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun adjustStock(product: Product, delta: Int) {
        viewModelScope.launch {
            val newStock = (product.stock + delta).coerceAtLeast(0)
            repository.updateProduct(product.copy(stock = newStock))
        }
    }

    // Reactive flow for Voice Processing result / banner feedback
    private val _voiceFeedback = MutableStateFlow<String?>(null)
    val voiceFeedback: StateFlow<String?> = _voiceFeedback.asStateFlow()

    fun clearVoiceFeedback() {
        _voiceFeedback.value = null
    }

    fun processVoiceCommand(input: String) {
        val text = input.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return

        _voiceFeedback.value = "Recognized: \"$input\""

        // 1. NAVIGATION COMMANDS
        if (text.contains("billing") || text.contains("bill") || text.contains("बिल") || text.contains("पर्चा")) {
            navigateTo(Screen.Billing)
            val msg = if (_language.value == Language.HINDI) "बिलिंग स्क्रीन खोली जा रही है।" else "Opening Billing screen."
            speak(msg)
            _voiceFeedback.value = "Opening Billing screen: \"$input\""
            return
        }
        if (text.contains("inventory") || text.contains("product") || text.contains("स्टॉक") || text.contains("सामान") || text.contains("माल")) {
            navigateTo(Screen.Inventory)
            val msg = if (_language.value == Language.HINDI) "इन्वेंट्री स्क्रीन खोली जा रही है।" else "Opening Inventory screen."
            speak(msg)
            _voiceFeedback.value = "Opening Inventory screen: \"$input\""
            return
        }
        if (text.contains("supplier") || text.contains("vender") || text.contains("सप्लायर") || text.contains("व्यापारी")) {
            navigateTo(Screen.Suppliers)
            val msg = if (_language.value == Language.HINDI) "सप्लायर सूची खोली जा रही है।" else "Opening Suppliers screen."
            speak(msg)
            _voiceFeedback.value = "Opening Suppliers: \"$input\""
            return
        }
        if (text.contains("customer") || text.contains("grahak") || text.contains("ग्राहक") || text.contains("खाता")) {
            navigateTo(Screen.Customers)
            val msg = if (_language.value == Language.HINDI) "ग्राहक सूची खोली जा रही है।" else "Opening Customers screen."
            speak(msg)
            _voiceFeedback.value = "Opening Customers: \"$input\""
            return
        }
        if (text.contains("dashboard") || text.contains("home") || text.contains("होम") || text.contains("मुख्य")) {
            navigateTo(Screen.Dashboard)
            val msg = if (_language.value == Language.HINDI) "मुख्य डैशबोर्ड खोला जा रहा है।" else "Opening Dashboard."
            speak(msg)
            _voiceFeedback.value = "Opening Dashboard: \"$input\""
            return
        }
        if (text.contains("profile") || text.contains("प्रोफ़ाइल") || text.contains("दुकान")) {
            navigateTo(Screen.Profile)
            val msg = if (_language.value == Language.HINDI) "दुकान प्रोफ़ाइल खोली जा रही है।" else "Opening Shop Profile."
            speak(msg)
            _voiceFeedback.value = "Opening Profile: \"$input\""
            return
        }

        // 2. ADJOINING NEW ENTITY VIA VOICE
        if (text.contains("add customer") || text.contains("new customer") || text.contains("naya customer") || text.contains("नया ग्राहक")) {
            val ignoredWords = setOf(
                "add", "customer", "new", "naya", "karo", "please", "grahak", "graahak", "नया", "ग्राहक", "जोड़ो", "जोडो"
            )
            val phoneRegex = "(\\d{10})".toRegex()
            val phoneMatched = phoneRegex.find(text)?.value ?: ""
            
            val words = text.split("\\s+".toRegex())
            val nameWords = words.filter { word ->
                word.none { it.isDigit() } && !ignoredWords.contains(word) && word.length > 2
            }
            val detectedNameRaw = nameWords.joinToString(" ").trim()
            val detectedName = if (detectedNameRaw.isNotBlank()) {
                detectedNameRaw.substring(0, 1).uppercase(Locale.ROOT) + detectedNameRaw.substring(1)
            } else {
                "Smart Voice User"
            }
            val finalPhone = phoneMatched.ifBlank { "9876543200" }
            
            addCustomer(detectedName, finalPhone, "Added via AI Voice Control")
            val feedback = if (_language.value == Language.HINDI) "नया ग्राहक $detectedName जोड़ा गया।" else "New customer $detectedName added."
            speak(feedback)
            _voiceFeedback.value = "AI Action: Added Customer \"$detectedName\" (${finalPhone})"
            return
        }

        if (text.contains("product") || text.contains("item") || text.contains("नया सामान") || text.contains("नया प्रोडक्ट") || text.contains("स्टॉक")) {
            val ignoredWords = setOf(
                "add", "new", "product", "item", "naya", "karo", "please", "नया", "प्रोडक्ट", "सामान", "जोड़ो", "रेट", "rate", "stock", "stok", "price", "reit"
            )
            val numberRegexSeq = "\\d+".toRegex()
            val numbersList = numberRegexSeq.findAll(text).map { it.value }.toList()
            val rate = numbersList.getOrNull(0)?.toDoubleOrNull() ?: 20.0
            val stock = numbersList.getOrNull(1)?.toIntOrNull() ?: 100

            val words = text.split("\\s+".toRegex())
            val nameWords = words.filter { word ->
                word.none { it.isDigit() } && !ignoredWords.contains(word) && word.length > 2
            }
            val detectedNameRaw = nameWords.joinToString(" ").trim()
            val detectedName = if (detectedNameRaw.isNotBlank()) {
                detectedNameRaw.substring(0, 1).uppercase(Locale.ROOT) + detectedNameRaw.substring(1)
            } else {
                "Voice Item"
            }

            addProduct(detectedName, "Voice Item", rate, stock, 10, null)
            val feedback = if (_language.value == Language.HINDI) "नया आइटम $detectedName स्टॉक में जोड़ा गया।" else "New item $detectedName added."
            speak(feedback)
            _voiceFeedback.value = "AI Action: Added Product \"$detectedName\" | Price: ₹${rate.toInt()} | Stock: $stock"
            return
        }

        // 3. TRANSACTION ADDING COMMANDS
        val numberRegex = "\\d+".toRegex()
        val matchNumbers = numberRegex.findAll(text).map { it.value }.toList()
        val amountValue = matchNumbers.firstOrNull()?.toDoubleOrNull()

        if (amountValue != null && amountValue > 0) {
            val ignoredWords = setOf(
                "ko", "se", "rupees", "rupee", "rupya", "rupaye", "upaye", "rs", "inr", "amount", "diya", "diye", "gave", "mile", "mila", "received", "lia", "liya", "payment", "credit", "debit", "daala", "dala", "add", "karo", "please", "to", "from", "for", "payment", "rupiya", "upya", "kiya", "ke", "ka", "on", "in", "the", "a", "an", "रुपये", "रुपया", "को", "से", "दिए", "दिया", "मिला", "मिले", "प्राप्त", "लिए", "लिया", "जमा", "उधार", "खाते"
            )
            val words = text.split("\\s+".toRegex())
            val candidateNameWords = words.filter { word ->
                word.none { it.isDigit() } && !ignoredWords.contains(word) && word.length > 2
            }
            val detectedNameRaw = candidateNameWords.take(2).joinToString(" ").trim()
            val detectedName = if (detectedNameRaw.isNotBlank()) {
                detectedNameRaw.substring(0, 1).uppercase(Locale.ROOT) + detectedNameRaw.substring(1)
            } else {
                ""
            }

            if (detectedName.isNotBlank()) {
                val isPayment = text.contains("mile") || text.contains("mila") || text.contains("received") || text.contains("payment") || text.contains("prapt") || text.contains("मिला") || text.contains("मिले") || text.contains("प्राप्त") || text.contains("जमा")
                val isSupplierMode = text.contains("supplier") || text.contains("सप्लायर") || text.contains("व्यापारी")

                if (isSupplierMode) {
                    val matchedSupplier = suppliers.value.find { it.name.contains(detectedName, ignoreCase = true) }
                    if (matchedSupplier != null) {
                        val finalType = if (isPayment) "PAYMENT" else "CREDIT"
                        addTransaction(
                            customerId = null,
                            supplierId = matchedSupplier.id,
                            type = finalType,
                            amount = amountValue,
                            notes = "Voice: \"$input\""
                        )
                        _voiceFeedback.value = "AI Transaction: ${if (finalType == "CREDIT") "Credit" else "Payment"} of ₹${amountValue.toInt()} on Supplier ${matchedSupplier.name}"
                    } else {
                        viewModelScope.launch {
                            val newId = (suppliers.value.maxOfOrNull { it.id } ?: 0) + 1
                            val newSupp = Supplier(id = newId, name = detectedName, phone = "9876543200", address = "Added via AI Voice Control", profileId = _activeProfileId.value)
                            repository.insertSupplier(newSupp)
                            val finalType = if (isPayment) "PAYMENT" else "CREDIT"
                            addTransaction(
                                customerId = null,
                                supplierId = newId,
                                type = finalType,
                                amount = amountValue,
                                notes = "Voice: \"$input\""
                            )
                            _voiceFeedback.value = "Auto-created Supplier \"$detectedName\" & added ₹${amountValue.toInt()} ${if (finalType == "CREDIT") "Credit" else "Payment"}"
                        }
                    }
                } else {
                    val matchedCustomer = customers.value.find { it.name.contains(detectedName, ignoreCase = true) }
                    if (matchedCustomer != null) {
                        val finalType = if (isPayment) "PAYMENT" else "CREDIT"
                        addTransaction(
                            customerId = matchedCustomer.id,
                            supplierId = null,
                            type = finalType,
                            amount = amountValue,
                            notes = "Voice: \"$input\""
                        )
                        _voiceFeedback.value = "AI Transaction: ${if (finalType == "CREDIT") "Udhar" else "Jama"} of ₹${amountValue.toInt()} on Customer ${matchedCustomer.name}"
                    } else {
                        viewModelScope.launch {
                            val newId = (customers.value.maxOfOrNull { it.id } ?: 0) + 1
                            val newCust = Customer(id = newId, name = detectedName, phone = "9876543200", address = "Added via AI Voice Control", profileId = _activeProfileId.value)
                            repository.insertCustomer(newCust)
                            val finalType = if (isPayment) "PAYMENT" else "CREDIT"
                            addTransaction(
                                customerId = newId,
                                supplierId = null,
                                type = finalType,
                                amount = amountValue,
                                notes = "Voice: \"$input\""
                            )
                            _voiceFeedback.value = "Auto-created Customer \"$detectedName\" & added ₹${amountValue.toInt()} ${if (finalType == "CREDIT") "Udhar" else "Jama"}"
                        }
                    }
                }
                return
            }
        }

        val fallbackMsg = if (_language.value == Language.HINDI) {
            "क्षमा करें, समझने में असमर्थ। कृपया कहें: रमेश को 500 रुपये दिए"
        } else {
            "Sorry, did not understand voice command. Try saying: Ramesh ko 500 upaye diye"
        }
        speak(fallbackMsg)
        _voiceFeedback.value = "Unrecognized Voice Entry: \"$input\""
    }

    fun saveBusinessProfile(
        name: String,
        phone: String,
        address: String,
        upiId: String,
        gstin: String,
        photoUri: String = "",
        clinicName: String = "",
        clinicAddress: String = "",
        defaultPaymentNote: String = "Payment for balance",
        enableQrSharing: Boolean = true
    ) {
        viewModelScope.launch {
            repository.insertBusinessProfile(
                BusinessProfile(
                    id = _activeProfileId.value,
                    name = name,
                    phone = phone,
                    address = address,
                    upiId = upiId,
                    gstin = gstin,
                    photoUri = photoUri,
                    clinicName = clinicName,
                    clinicAddress = clinicAddress,
                    defaultPaymentNote = defaultPaymentNote,
                    enableQrSharing = enableQrSharing
                )
            )
        }
    }

    // Google Drive Backup/Restore dynamic operations status
    private val _driveStatus = MutableStateFlow<String?>(null)
    val driveStatus: StateFlow<String?> = _driveStatus.asStateFlow()

    fun clearDriveStatus() {
        _driveStatus.value = null
    }

    suspend fun exportDatabaseToJson(): String {
        val customersList = customers.value
        val suppliersList = suppliers.value
        val transactionsList = transactions.value
        val productsList = products.value
        val profilesList = allBusinessProfiles.value

        val jsonObject = org.json.JSONObject()
        jsonObject.put("version", 2)
        jsonObject.put("timestamp", System.currentTimeMillis())

        val custArray = org.json.JSONArray()
        for (c in customersList) {
            val o = org.json.JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("phone", c.phone)
            o.put("address", c.address)
            o.put("createdAt", c.createdAt)
            o.put("profileId", c.profileId)
            custArray.put(o)
        }
        jsonObject.put("customers", custArray)

        val suppArray = org.json.JSONArray()
        for (s in suppliersList) {
            val o = org.json.JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("phone", s.phone)
            o.put("address", s.address)
            o.put("createdAt", s.createdAt)
            o.put("profileId", s.profileId)
            suppArray.put(o)
        }
        jsonObject.put("suppliers", suppArray)

        val txArray = org.json.JSONArray()
        for (t in transactionsList) {
            val o = org.json.JSONObject()
            o.put("id", t.id)
            o.put("customerId", if (t.customerId != null) t.customerId else org.json.JSONObject.NULL)
            o.put("supplierId", if (t.supplierId != null) t.supplierId else org.json.JSONObject.NULL)
            o.put("type", t.type)
            o.put("amount", t.amount)
            o.put("notes", t.notes)
            o.put("date", t.date)
            o.put("invoiceNumber", if (t.invoiceNumber != null) t.invoiceNumber else org.json.JSONObject.NULL)
            o.put("isGSTInvoice", t.isGSTInvoice)
            o.put("gstRate", t.gstRate)
            o.put("gstAmount", t.gstAmount)
            o.put("profileId", t.profileId)
            txArray.put(o)
        }
        jsonObject.put("transactions", txArray)

        val prodArray = org.json.JSONArray()
        for (p in productsList) {
            val o = org.json.JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("category", p.category)
            o.put("price", p.price)
            o.put("stock", p.stock)
            o.put("lowStockLimit", p.lowStockLimit)
            o.put("barcode", if (p.barcode != null) p.barcode else org.json.JSONObject.NULL)
            o.put("profileId", p.profileId)
            prodArray.put(o)
        }
        jsonObject.put("products", prodArray)

        val profArray = org.json.JSONArray()
        for (p in profilesList) {
            val o = org.json.JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("phone", p.phone)
            o.put("address", p.address)
            o.put("upiId", p.upiId)
            o.put("gstin", p.gstin)
            o.put("photoUri", p.photoUri)
            o.put("clinicName", p.clinicName)
            o.put("clinicAddress", p.clinicAddress)
            o.put("defaultPaymentNote", p.defaultPaymentNote)
            o.put("enableQrSharing", p.enableQrSharing)
            profArray.put(o)
        }
        jsonObject.put("profiles", profArray)

        // Export reminder templates
        val templatesList = repository.allReminderTemplates.first()
        val templateArray = org.json.JSONArray()
        for (t in templatesList) {
            val o = org.json.JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("content", t.content)
            o.put("language", t.language)
            o.put("category", t.category)
            o.put("isSystem", t.isSystem)
            o.put("isFavorite", t.isFavorite)
            o.put("usageCount", t.usageCount)
            templateArray.put(o)
        }
        jsonObject.put("reminder_templates", templateArray)

        return jsonObject.toString(2)
    }

    suspend fun importDatabaseFromJson(jsonStr: String): Boolean {
        return try {
            val jsonObject = org.json.JSONObject(jsonStr)

            // 1. Business Profiles
            val profArray = jsonObject.optJSONArray("profiles")
            if (profArray != null) {
                for (i in 0 until profArray.length()) {
                    val o = profArray.getJSONObject(i)
                    val prof = BusinessProfile(
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        phone = o.getString("phone"),
                        address = o.getString("address"),
                        upiId = o.getString("upiId"),
                        gstin = o.optString("gstin", ""),
                        photoUri = o.optString("photoUri", ""),
                        clinicName = o.optString("clinicName", ""),
                        clinicAddress = o.optString("clinicAddress", ""),
                        defaultPaymentNote = o.optString("defaultPaymentNote", "Payment for balance"),
                        enableQrSharing = o.optBoolean("enableQrSharing", true)
                    )
                    repository.insertBusinessProfile(prof)
                }
            }

            // Import reminder templates
            val tempArray = jsonObject.optJSONArray("reminder_templates")
            if (tempArray != null) {
                for (i in 0 until tempArray.length()) {
                    val o = tempArray.getJSONObject(i)
                    val temp = ReminderTemplate(
                        id = o.optInt("id", 0),
                        title = o.getString("title"),
                        content = o.getString("content"),
                        language = o.getString("language"),
                        category = o.getString("category"),
                        isSystem = o.optBoolean("isSystem", false),
                        isFavorite = o.optBoolean("isFavorite", false),
                        usageCount = o.optInt("usageCount", 0)
                    )
                    repository.insertReminderTemplate(temp)
                }
            }

            // 2. Customers
            val custArray = jsonObject.optJSONArray("customers")
            if (custArray != null) {
                for (i in 0 until custArray.length()) {
                    val o = custArray.getJSONObject(i)
                    val cust = Customer(
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        phone = o.getString("phone"),
                        address = o.getString("address"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        profileId = o.optInt("profileId", 1)
                    )
                    repository.insertCustomer(cust)
                }
            }

            // 3. Suppliers
            val suppArray = jsonObject.optJSONArray("suppliers")
            if (suppArray != null) {
                for (i in 0 until suppArray.length()) {
                    val o = suppArray.getJSONObject(i)
                    val supp = Supplier(
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        phone = o.getString("phone"),
                        address = o.getString("address"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        profileId = o.optInt("profileId", 1)
                    )
                    repository.insertSupplier(supp)
                }
            }

            // 4. Products
            val prodArray = jsonObject.optJSONArray("products")
            if (prodArray != null) {
                for (i in 0 until prodArray.length()) {
                    val o = prodArray.getJSONObject(i)
                    val prod = Product(
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        category = o.getString("category"),
                        price = o.getDouble("price"),
                        stock = o.getInt("stock"),
                        lowStockLimit = o.optInt("lowStockLimit", 5),
                        barcode = if (o.isNull("barcode")) null else o.optString("barcode", null),
                        profileId = o.optInt("profileId", 1)
                    )
                    repository.insertProduct(prod)
                }
            }

            // 5. Transactions
            val txArray = jsonObject.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    val tx = Transaction(
                        id = o.getInt("id"),
                        customerId = if (o.isNull("customerId")) null else o.getInt("customerId"),
                        supplierId = if (o.isNull("supplierId")) null else o.getInt("supplierId"),
                        type = o.getString("type"),
                        amount = o.getDouble("amount"),
                        notes = o.optString("notes", ""),
                        date = o.optLong("date", System.currentTimeMillis()),
                        invoiceNumber = if (o.isNull("invoiceNumber")) null else o.optString("invoiceNumber", null),
                        isGSTInvoice = o.optBoolean("isGSTInvoice", false),
                        gstRate = o.optDouble("gstRate", 0.0),
                        gstAmount = o.optDouble("gstAmount", 0.0),
                        profileId = o.optInt("profileId", 1)
                    )
                    repository.insertTransaction(tx)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun backupLedgerToGoogleDrive(context: Context) {
        viewModelScope.launch {
            _driveStatus.value = if (_language.value == Language.HINDI) "बैकअप लिया जा रहा है..." else "Backing up ledger..."
            val jsonStr = exportDatabaseToJson()
            val result = GoogleDriveBackupHelper.backupToDrive(context, jsonStr)
            result.onSuccess { msg ->
                _driveStatus.value = msg
                speak(if (_language.value == Language.HINDI) "गूगल ड्राइव पर बैकअप सफल हुआ।" else "Backup to Google Drive succeeded.")
            }.onFailure { err ->
                _driveStatus.value = "Backup Failed: ${err.localizedMessage}"
                speak(if (_language.value == Language.HINDI) "बैकअप विफल रहा।" else "Backup failed.")
            }
        }
    }

    fun restoreLedgerFromGoogleDrive(context: Context) {
        viewModelScope.launch {
            _driveStatus.value = if (_language.value == Language.HINDI) "पुनर्प्राप्त किया जा रहा है..." else "Restoring from drive..."
            val result = GoogleDriveBackupHelper.restoreFromDrive(context)
            result.onSuccess { jsonStr ->
                val ok = importDatabaseFromJson(jsonStr)
                if (ok) {
                    _driveStatus.value = if (_language.value == Language.HINDI) "सफलतापूर्वक रीस्टोर किया गया!" else "Successfully Restored!"
                    speak(if (_language.value == Language.HINDI) "क्रेडिट बही खाता सफलतापूर्वक रीस्टोर किया गया।" else "Ledger successfully restored from Google Drive.")
                } else {
                    _driveStatus.value = if (_language.value == Language.HINDI) "रीस्टोर विफल: अमान्य डेटा" else "Restore failed: Invalid data format."
                    speak(if (_language.value == Language.HINDI) "रीस्टोर विफल रहा।" else "Restore failed.")
                }
            }.onFailure { err ->
                _driveStatus.value = "Restore Failed: ${err.localizedMessage}"
                speak(if (_language.value == Language.HINDI) "रीस्टोर विफल रहा।" else "Restore failed.")
            }
        }
    }

    fun backupLedgerToLocalFile(context: Context) {
        viewModelScope.launch {
            try {
                _driveStatus.value = if (_language.value == Language.HINDI) "लोकल बैकअप तैयार किया जा रहा है..." else "Preparing local backup..."
                val jsonStr = exportDatabaseToJson()
                val file = java.io.File(context.cacheDir, "smart_khata_backup.json")
                file.writeText(jsonStr)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val title = if (_language.value == Language.HINDI) "बैकअप फ़ाइल सुरक्षित करें" else "Save or Share Backup File"
                context.startActivity(Intent.createChooser(intent, title))
                _driveStatus.value = if (_language.value == Language.HINDI) "बैकअप फ़ाइल एक्सपोर्ट की गई!" else "Backup exported successfully!"
            } catch (e: Exception) {
                _driveStatus.value = "Local Backup failed: ${e.localizedMessage}"
                e.printStackTrace()
            }
        }
    }

    fun restoreLedgerFromLocalJson(jsonStr: String, context: Context) {
        viewModelScope.launch {
            _driveStatus.value = if (_language.value == Language.HINDI) "लोकल पुनर्प्राप्ति जारी है..." else "Restoring local backup..."
            val ok = importDatabaseFromJson(jsonStr)
            if (ok) {
                _driveStatus.value = if (_language.value == Language.HINDI) "लोकल डेटा सफलतापूर्वक रीस्टोर हुआ!" else "Local Backup Restored successfully!"
                speak(if (_language.value == Language.HINDI) "लोकल बैकअप सफलतापूर्वक रीस्टोर किया गया।" else "Local backup successfully restored.")
            } else {
                _driveStatus.value = if (_language.value == Language.HINDI) "रीस्टोर विफल: गलत फ़ाइल फ़ॉर्मैट" else "Restore failed: Invalid json database template."
                speak(if (_language.value == Language.HINDI) "रीस्टोर विफल रहा।" else "Restore failed.")
            }
        }
    }

    // --- WhatsApp Business API Reminders Integration ---
    private val _whatsappStatuses = MutableStateFlow<Map<Int, WhatsAppSendingStatus>>(emptyMap())
    val whatsappStatuses: StateFlow<Map<Int, WhatsAppSendingStatus>> = _whatsappStatuses.asStateFlow()

    fun sendWhatsAppApiReminder(
        customerId: Int,
        customerPhone: String,
        customerName: String,
        balanceAmount: Double,
        businessName: String
    ) {
        viewModelScope.launch {
            _whatsappStatuses.update { it + (customerId to WhatsAppSendingStatus(isSending = true)) }
            val result = WhatsAppService.sendPaymentReminder(
                customerPhone = customerPhone,
                customerName = customerName,
                balanceAmount = balanceAmount,
                businessName = businessName
            )
            result.onSuccess {
                _whatsappStatuses.update { it + (customerId to WhatsAppSendingStatus(isSending = false, success = true)) }
                val msg = if (_language.value == Language.HINDI) "व्हाट्सएप एपीआई रिमाइंडर भेज दिया गया!" else "WhatsApp API reminder sent!"
                speak(msg)
            }.onFailure { err ->
                val errMessage = err.localizedMessage ?: "Unknown error"
                _whatsappStatuses.update { it + (customerId to WhatsAppSendingStatus(isSending = false, success = false, error = errMessage)) }
                val msg = if (_language.value == Language.HINDI) "रिमाइंडर विफल: $errMessage" else "Reminder failed: $errMessage"
                speak(msg)
            }
        }
    }

    fun sendBulkWhatsAppReminders(overdueCustomers: List<Pair<Customer, Double>>, businessName: String) {
        viewModelScope.launch {
            overdueCustomers.forEach { (cust, balance) ->
                if (balance > 0) {
                    sendWhatsAppApiReminder(
                        customerId = cust.id,
                        customerPhone = cust.phone,
                        customerName = cust.name,
                        balanceAmount = balance,
                        businessName = businessName
                    )
                }
            }
        }
    }

    fun clearWhatsAppStatus(customerId: Int) {
        _whatsappStatuses.update { it - customerId }
    }

    // --- Reminder Templates Support ---
    val reminderTemplates: StateFlow<List<ReminderTemplate>> = repository.allReminderTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun seedDefaultReminderTemplates() {
        val defaults = listOf(
            ReminderTemplate(
                title = "Payment Due Reminder (EN)",
                content = "Hello {CustomerName},\nYour outstanding dues of ₹{Amount} with {BusinessName} are pending. Kindly clear the balance by {DueDate}. Thank you!\nCall: {MobileNumber}",
                language = "en",
                category = "Credit Reminder",
                isSystem = true,
                isFavorite = true
            ),
            ReminderTemplate(
                title = "भुगतान बकाया अनुस्मारक (HI)",
                content = "नमस्ते {CustomerName},\n{BusinessName} के साथ आपका बकाया राशि ₹{Amount} है, जो कि {DueDate} तक देय है। कृपया जल्द भुगतान करें। धन्यवाद।\nफ़ोन: {MobileNumber}",
                language = "hi",
                category = "Credit Reminder",
                isSystem = true,
                isFavorite = true
            ),
            ReminderTemplate(
                title = "Overdue Immediate Alert (EN)",
                content = "URGENT PAYMENT OUTSTANDING!\nDear {CustomerName}, your ledger balance of ₹{Amount} is OVERDUE since {DueDate}. Please resolve immediately to keep your account in good standing. - {BusinessName} ({MobileNumber})",
                language = "en",
                category = "Credit Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "अतिदेय भुगतान चेतावनी (HI)",
                content = "अति आवश्यक भुगतान अनुस्मारक!\nप्रिय {CustomerName}, {BusinessName} के रिकॉर्ड में आपका ₹{Amount} का क्रेडिट {DueDate} से अतिदेय (Overdue) है। कृपया तुरंत भुगतान करके असुविधा से बचें। - {MobileNumber}",
                language = "hi",
                category = "Credit Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "Friendly Ledger Balance (EN)",
                content = "Hi {CustomerName}, hope you are doing well. This is a gentle ping to let you know that your outstanding credit with {BusinessName} is ₹{Amount}. Have a great day! {MobileNumber}",
                language = "en",
                category = "Friendly Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "सामान्य क्रेडिट स्मरण (HI)",
                content = "नमस्ते {CustomerName}, आशा है सब ठीक होगा। एक छोटा सा स्मरण कि {BusinessName} के बही खाते में आपकी बकाया राशि ₹{Amount} दर्ज है। आपका दिन शुभ हो! {MobileNumber}",
                language = "hi",
                category = "Friendly Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "Labor Payment Schedule (EN)",
                content = "Hello {CustomerName},\nThis is to notify that labor charges of ₹{Amount} are scheduled to be cleared on {DueDate}. - {BusinessName}",
                language = "en",
                category = "Labour Payment Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "लेबर मजदूरी भुगतान (HI)",
                content = "नमस्ते {CustomerName},\nलेबर मजदूरी का बकाया ₹{Amount} तिथि {DueDate} पर देय निर्धारित है। कृपया बही खाते की पुष्टि करें। - {BusinessName}",
                language = "hi",
                category = "Labour Payment Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "Raw Material Due (EN)",
                content = "Dear {CustomerName},\nThe invoice for material supplied worth ₹{Amount} is pending for settlement. Please clear by {DueDate}. Thank you. - {BusinessName}",
                language = "en",
                category = "Material Payment Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "सामग्री आपूर्ति देय (HI)",
                content = "प्रिय {CustomerName},\nआपूर्ति की गई सामग्री का चालान मूल्य ₹{Amount} लंबित है। कृपया {DueDate} तक भुगतान करना सुनिश्चित करें। - {BusinessName}",
                language = "hi",
                category = "Material Payment Reminder",
                isSystem = true
            ),
            ReminderTemplate(
                title = "Thank You for Payment (EN)",
                content = "Dear {CustomerName},\nWe have successfully received your payment of ₹{Amount}. Your outstanding ledger with {BusinessName} has been cleared. Thank you for your continued business!",
                language = "en",
                category = "Thank You Messages",
                isSystem = true,
                isFavorite = true
            ),
            ReminderTemplate(
                title = "भुगतान प्राप्त धन्यवाद (HI)",
                content = "नमस्ते {CustomerName},\nहमें ₹{Amount} का आपका भुगतान सफलतापूर्वक प्राप्त हो गया है। {BusinessName} में आपका खाता अपडेट कर दिया गया है। भुगतान के लिए बहुत-बहुत धन्यवाद!",
                language = "hi",
                category = "Thank You Messages",
                isSystem = true,
                isFavorite = true
            )
        )
        for (d in defaults) {
            repository.insertReminderTemplate(d)
        }
    }

    fun saveReminderTemplate(
        id: Int,
        title: String,
        content: String,
        language: String,
        category: String,
        isSystem: Boolean = false,
        isFavorite: Boolean = false,
        usageCount: Int = 0
    ) {
        viewModelScope.launch {
            val tem = ReminderTemplate(
                id = id,
                title = title,
                content = content,
                language = language,
                category = category,
                isSystem = isSystem,
                isFavorite = isFavorite,
                usageCount = usageCount
            )
            repository.insertReminderTemplate(tem)
        }
    }

    fun deleteReminderTemplate(template: ReminderTemplate) {
        viewModelScope.launch {
            repository.deleteReminderTemplate(template)
        }
    }

    fun toggleTemplateFavorite(template: ReminderTemplate) {
        viewModelScope.launch {
            repository.updateReminderTemplate(template.copy(isFavorite = !template.isFavorite))
        }
    }

    fun incrementTemplateUsage(template: ReminderTemplate) {
        viewModelScope.launch {
            repository.updateReminderTemplate(template.copy(usageCount = template.usageCount + 1))
        }
    }

    fun duplicateTemplate(template: ReminderTemplate) {
        viewModelScope.launch {
            val titleSuffix = if (_language.value == Language.HINDI) " - प्रतिलिपि" else " - Copy"
            val duplicated = ReminderTemplate(
                id = 0,
                title = "${template.title}$titleSuffix",
                content = template.content,
                language = template.language,
                category = template.category,
                isSystem = false,
                isFavorite = false,
                usageCount = 0
            )
            repository.insertReminderTemplate(duplicated)
        }
    }

    fun resolveTemplateVariables(
        templateContent: String,
        customerName: String,
        amount: Double,
        dueDate: String,
        businessName: String,
        mobileNumber: String
    ): String {
        return templateContent
            .replace("{CustomerName}", customerName, ignoreCase = true)
            .replace("{Amount}", amount.toInt().toString(), ignoreCase = true)
            .replace("{DueDate}", dueDate, ignoreCase = true)
            .replace("{BusinessName}", businessName, ignoreCase = true)
            .replace("{MobileNumber}", mobileNumber, ignoreCase = true)
    }

    // --- Reminder Scheduling and Logs ---
    val reminderLogs: StateFlow<List<ReminderLog>> = repository.allReminderLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scheduleReminder(
        customerId: Int,
        customerName: String,
        customerPhone: String,
        amount: Double,
        scheduledTime: Long,
        templateLanguage: String // "en" (English) or "hi" (Hindi)
    ) {
        viewModelScope.launch {
            val rawPhone = customerPhone.trim()
            val cleanPhone = rawPhone.filter { it.isDigit() }
            val finalStatus = if (cleanPhone.length < 10) "FAILED" else "PENDING"
            val finalError = if (cleanPhone.length < 10) {
                if (_language.value == Language.HINDI) "अमान्य मोबाइल नंबर! कम से कम 10 अंको का होना चाहिए।" else "Invalid Mobile Number: '$customerPhone'. Must contain at least 10 digits."
            } else null

            val log = ReminderLog(
                customerId = customerId,
                customerName = customerName,
                customerPhone = customerPhone,
                amount = amount,
                scheduledTime = scheduledTime,
                status = finalStatus,
                error = finalError,
                templateName = BuildConfig.WHATSAPP_TEMPLATE_NAME,
                language = if (templateLanguage == "hi") "hi" else "en_US"
            )
            val id = repository.insertReminderLog(log).toInt()

            if (finalStatus == "PENDING") {
                val context = getApplication<Application>().applicationContext
                val delay = maxOf(0L, scheduledTime - System.currentTimeMillis())
                val data = workDataOf("reminder_id" to id)

                val workRequest = OneTimeWorkRequestBuilder<WhatsAppReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("reminder_${id}")
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "reminder_job_${id}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                val msg = if (_language.value == Language.HINDI) "रिमाइंडर ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(scheduledTime)} के लिए निर्धारित किया गया!" else "Reminder scheduled for ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(scheduledTime)}!"
                speak(msg)
            } else {
                val msg = if (_language.value == Language.HINDI) "निर्धारण विफल: मोबाइल नंबर अमान्य है" else "Schedule failed: Mobile number is invalid"
                speak(msg)
            }
        }
    }

    fun triggerReminderLogImmediately(log: ReminderLog) {
        viewModelScope.launch {
            // Update state to PENDING in local DB
            val updatedStart = log.copy(status = "PENDING", error = null)
            repository.updateReminderLog(updatedStart)

            // Validate number
            val rawPhone = log.customerPhone.trim()
            val cleanPhone = rawPhone.filter { it.isDigit() }
            if (cleanPhone.length < 10) {
                val err = if (_language.value == Language.HINDI) "अमान्य फोन नंबर" else "Invalid Phone Number. Must be 10 digits."
                repository.updateReminderLog(log.copy(status = "FAILED", error = err))
                speak(err)
                return@launch
            }

            val resultMsg = if (_language.value == Language.HINDI) "भेजा जा रहा है..." else "Sending automated reminder..."
            speak(resultMsg)

            val profiles = db.ledgerDao().getAllBusinessProfiles().firstOrNull()
            val bizName = profiles?.firstOrNull()?.name ?: "Credit Book"

            val result = WhatsAppService.sendPaymentReminder(
                customerPhone = cleanPhone,
                customerName = log.customerName,
                balanceAmount = log.amount,
                businessName = bizName,
                templateName = log.templateName,
                languageCode = log.language
            )

            result.fold(
                onSuccess = {
                    repository.updateReminderLog(log.copy(status = "SENT", sentTime = System.currentTimeMillis(), error = null))
                    val successMsg = if (_language.value == Language.HINDI) "पेमेंट रिमाइंडर सफलतापूर्वक भेज दिया गया" else "Payment reminder successfully dispatched"
                    speak(successMsg)
                },
                onFailure = { err ->
                    val isAuthErr = err.localizedMessage?.contains("401", ignoreCase = true) == true
                    val errText = if (isAuthErr) {
                        if (_language.value == Language.HINDI) "त्रुटि 401: व्हाट्सएप क्रेडेंशियल अनुपलब्ध या अमान्य हैं" else "Error 401: WhatsApp Credentials unauthorized or missing"
                    } else {
                        err.localizedMessage ?: "API Dispatch Error"
                    }
                    repository.updateReminderLog(log.copy(status = "FAILED", error = errText))
                    speak(if (_language.value == Language.HINDI) "रिमाइंडर विफल" else "Reminder failed")
                }
            )
        }
    }

    fun cancelScheduledReminder(log: ReminderLog) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            WorkManager.getInstance(context).cancelUniqueWork("reminder_job_${log.id}")
            repository.deleteReminderLog(log)
            val msg = if (_language.value == Language.HINDI) "रिमाइंडर रद्द् कर दिया गया" else "Reminder cancelled"
            speak(msg)
        }
    }

    fun deleteReminderLog(log: ReminderLog) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            WorkManager.getInstance(context).cancelUniqueWork("reminder_job_${log.id}")
            repository.deleteReminderLog(log)
        }
    }

    fun recordReminderResult(
        customerId: Int,
        customerName: String,
        type: String,
        status: String,
        messageContent: String
    ) {
        viewModelScope.launch {
            val log = ReminderLog(
                customerId = customerId,
                customerName = customerName,
                customerPhone = "",
                amount = 0.0,
                scheduledTime = System.currentTimeMillis(),
                sentTime = System.currentTimeMillis(),
                status = status.uppercase(),
                error = if (status.uppercase() == "SUCCESS") null else "Direct dispatch unsuccessful",
                templateName = type,
                language = "en"
            )
            repository.insertReminderLog(log)
        }
    }
}

data class WhatsAppSendingStatus(
    val isSending: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)


data class DashboardStats(
    val receivable: Double = 0.0,
    val payable: Double = 0.0,
    val todaySales: Double = 0.0,
    val totalTransactions: Int = 0
)
