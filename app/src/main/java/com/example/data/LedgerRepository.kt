package com.example.data

import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val ledgerDao: LedgerDao) {
    // Customers Flow & Actions
    val allCustomers: Flow<List<Customer>> = ledgerDao.getAllCustomers()

    suspend fun insertCustomer(customer: Customer): Long {
        return ledgerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) {
        ledgerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        ledgerDao.deleteCustomer(customer)
    }

    // Suppliers Flow & Actions
    val allSuppliers: Flow<List<Supplier>> = ledgerDao.getAllSuppliers()

    suspend fun insertSupplier(supplier: Supplier): Long {
        return ledgerDao.insertSupplier(supplier)
    }

    suspend fun updateSupplier(supplier: Supplier) {
        ledgerDao.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: Supplier) {
        ledgerDao.deleteSupplier(supplier)
    }

    // Transactions Flow & Actions
    val allTransactions: Flow<List<Transaction>> = ledgerDao.getAllTransactions()

    fun getTransactionsForCustomer(customerId: Int): Flow<List<Transaction>> {
        return ledgerDao.getTransactionsForCustomer(customerId)
    }

    fun getTransactionsForSupplier(supplierId: Int): Flow<List<Transaction>> {
        return ledgerDao.getTransactionsForSupplier(supplierId)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return ledgerDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Int) {
        ledgerDao.deleteTransactionById(id)
    }

    // Products Flow & Actions
    val allProducts: Flow<List<Product>> = ledgerDao.getAllProducts()

    suspend fun insertProduct(product: Product): Long {
        return ledgerDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        ledgerDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        ledgerDao.deleteProduct(product)
    }

    // Business Profile Flow & Actions
    val businessProfile: Flow<BusinessProfile?> = ledgerDao.getBusinessProfile()

    fun getBusinessProfileById(id: Int): Flow<BusinessProfile?> {
        return ledgerDao.getBusinessProfileById(id)
    }

    val allBusinessProfiles: Flow<List<BusinessProfile>> = ledgerDao.getAllBusinessProfiles()

    suspend fun insertBusinessProfile(profile: BusinessProfile) {
        ledgerDao.insertBusinessProfile(profile)
    }

    // Reminder Logs
    val allReminderLogs: Flow<List<ReminderLog>> = ledgerDao.getAllReminderLogs()

    suspend fun insertReminderLog(log: ReminderLog): Long {
        return ledgerDao.insertReminderLog(log)
    }

    suspend fun updateReminderLog(log: ReminderLog) {
        ledgerDao.updateReminderLog(log)
    }

    suspend fun deleteReminderLog(log: ReminderLog) {
        ledgerDao.deleteReminderLog(log)
    }

    suspend fun getReminderLogById(id: Int): ReminderLog? {
        return ledgerDao.getReminderLogById(id)
    }

    // Reminder Templates
    val allReminderTemplates: Flow<List<ReminderTemplate>> = ledgerDao.getAllReminderTemplates()

    suspend fun insertReminderTemplate(template: ReminderTemplate): Long {
        return ledgerDao.insertReminderTemplate(template)
    }

    suspend fun updateReminderTemplate(template: ReminderTemplate) {
        ledgerDao.updateReminderTemplate(template)
    }

    suspend fun deleteReminderTemplate(template: ReminderTemplate) {
        ledgerDao.deleteReminderTemplate(template)
    }

    suspend fun getReminderTemplateById(id: Int): ReminderTemplate? {
        return ledgerDao.getReminderTemplateById(id)
    }

    // Scan Logs
    val allScanLogs: Flow<List<ScanLog>> = ledgerDao.getAllScanLogs()

    suspend fun insertScanLog(scanLog: ScanLog): Long {
        return ledgerDao.insertScanLog(scanLog)
    }

    suspend fun clearScanLogs() {
        ledgerDao.clearScanLogs()
    }

    // Recycle Bin & Emergency Data Recovery mapping
    val deletedCustomers: Flow<List<Customer>> = ledgerDao.getDeletedCustomers()
    val deletedTransactions: Flow<List<Transaction>> = ledgerDao.getDeletedTransactions()

    suspend fun softDeleteCustomer(id: Int) {
        ledgerDao.softDeleteCustomer(id)
    }

    suspend fun restoreCustomer(id: Int) {
        ledgerDao.restoreCustomer(id)
    }

    suspend fun bulkRecoverCustomers() {
        ledgerDao.bulkRecoverCustomers()
    }

    suspend fun softDeleteTransaction(id: Int) {
        ledgerDao.softDeleteTransaction(id)
    }

    suspend fun restoreTransaction(id: Int) {
        ledgerDao.restoreTransaction(id)
    }

    suspend fun bulkRecoverTransactions() {
        ledgerDao.bulkRecoverTransactions()
    }
}
