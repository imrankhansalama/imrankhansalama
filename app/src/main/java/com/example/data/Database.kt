package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String,
    val createdAt: Long = System.currentTimeMillis(),
    val profileId: Int = 1,
    val photoUri: String = "",
    val isFavourite: Boolean = false,
    val tags: String = "Regular", // "Regular", "VIP", "Risky"
    val isDeleted: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val notes: String = "",
    val documentUri: String = "",
    val signatureUri: String = ""
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String,
    val createdAt: Long = System.currentTimeMillis(),
    val profileId: Int = 1
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int? = null,
    val supplierId: Int? = null,
    val type: String, // "CREDIT" (Udhar Given / Supplier Buy) or "PAYMENT" (Received / Paid)
    val amount: Double,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val invoiceNumber: String? = null,
    val isGSTInvoice: Boolean = false,
    val gstRate: Double = 0.0,
    val gstAmount: Double = 0.0,
    val profileId: Int = 1,
    val photoUri: String = "", // bill photo / receipt attachment
    val voiceNoteUri: String = "", // audio notes path
    val signatureUri: String = "", // digital signature path
    val isDeleted: Boolean = false
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val lowStockLimit: Int = 5,
    val barcode: String? = null,
    val profileId: Int = 1
)

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phone: String,
    val address: String,
    val upiId: String,
    val gstin: String = "",
    val photoUri: String = "",
    val clinicName: String = "",
    val clinicAddress: String = "",
    val defaultPaymentNote: String = "Payment for balance",
    val enableQrSharing: Boolean = true
)

@Entity(tableName = "reminder_logs")
data class ReminderLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String,
    val amount: Double,
    val scheduledTime: Long,
    val sentTime: Long? = null,
    val status: String, // "PENDING", "SENT", "FAILED"
    val error: String? = null,
    val retryCount: Int = 0,
    val templateName: String = "payment_reminder",
    val language: String = "en_US"
)

@Entity(tableName = "reminder_templates")
data class ReminderTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val language: String, // "en" or "hi"
    val category: String, // "Credit Reminder", "Labour Payment Reminder", "Material Payment Reminder", "Friendly Reminder", "Thank You Messages"
    val isSystem: Boolean = false,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)

@Entity(tableName = "scan_logs")
data class ScanLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: String,
    val scanType: String, // "BARCODE" or "QR"
    val resolvedEntity: String, // "Customer: John Doe", "Invoice #4", etc.
    val actionTaken: String // "Opened Ledger", "Updated Stock", etc.
)

@Dao
interface LedgerDao {
    // Customers
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY isFavourite DESC, name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteCustomer(id: Int)

    @Query("SELECT * FROM customers WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedCustomers(): Flow<List<Customer>>

    @Query("UPDATE customers SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreCustomer(id: Int)

    @Query("UPDATE customers SET isDeleted = 0 WHERE isDeleted = 1")
    suspend fun bulkRecoverCustomers()

    // Suppliers
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    // Transactions
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE supplierId = :supplierId ORDER BY date DESC")
    fun getTransactionsForSupplier(supplierId: Int): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: Int)

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY date DESC")
    fun getDeletedTransactions(): Flow<List<Transaction>>

    @Query("UPDATE transactions SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreTransaction(id: Int)

    @Query("UPDATE transactions SET isDeleted = 0 WHERE isDeleted = 1")
    suspend fun bulkRecoverTransactions()

    // Products / Inventory
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // Business Profile
    @Query("SELECT * FROM business_profile WHERE id = :id LIMIT 1")
    fun getBusinessProfileById(id: Int): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getBusinessProfile(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile ORDER BY id ASC")
    fun getAllBusinessProfiles(): Flow<List<BusinessProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessProfile(profile: BusinessProfile)

    // Reminder Logs
    @Query("SELECT * FROM reminder_logs ORDER BY scheduledTime DESC")
    fun getAllReminderLogs(): Flow<List<ReminderLog>>

    @Query("SELECT * FROM reminder_logs WHERE status = 'PENDING' AND scheduledTime <= :currentTime")
    suspend fun getPendingRemindersToTrigger(currentTime: Long): List<ReminderLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderLog(log: ReminderLog): Long

    @Update
    suspend fun updateReminderLog(log: ReminderLog)

    @Delete
    suspend fun deleteReminderLog(log: ReminderLog)
    
    @Query("SELECT * FROM reminder_logs WHERE id = :id LIMIT 1")
    suspend fun getReminderLogById(id: Int): ReminderLog?

    // Reminder Templates
    @Query("SELECT * FROM reminder_templates ORDER BY isSystem DESC, usageCount DESC, title ASC")
    fun getAllReminderTemplates(): Flow<List<ReminderTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderTemplate(template: ReminderTemplate): Long

    @Update
    suspend fun updateReminderTemplate(template: ReminderTemplate)

    @Delete
    suspend fun deleteReminderTemplate(template: ReminderTemplate)

    @Query("SELECT * FROM reminder_templates WHERE id = :id LIMIT 1")
    suspend fun getReminderTemplateById(id: Int): ReminderTemplate?

    // Scan Logs
    @Query("SELECT * FROM scan_logs ORDER BY timestamp DESC")
    fun getAllScanLogs(): Flow<List<ScanLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanLog(scanLog: ScanLog): Long

    @Query("DELETE FROM scan_logs")
    suspend fun clearScanLogs()
}

@Database(
    entities = [
        Customer::class, 
        Supplier::class, 
        Transaction::class, 
        Product::class, 
        BusinessProfile::class, 
        ReminderLog::class,
        ReminderTemplate::class,
        ScanLog::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_ledger_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
