package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException

class WhatsAppReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "WhatsAppReminderWorker"
    }

    override suspend fun doWork(): Result {
        val reminderId = inputData.getInt("reminder_id", -1)
        if (reminderId == -1) {
            Log.e(TAG, "Worker executed with invalid reminder_id")
            return Result.failure()
        }

        val database = AppDatabase.getInstance(applicationContext)
        val dao = database.ledgerDao()

        val reminder = dao.getReminderLogById(reminderId)
        if (reminder == null) {
            Log.e(TAG, "Reminder with id $reminderId not found in local db")
            return Result.failure()
        }

        // Double send prevention or check state
        if (reminder.status == "SENT") {
            Log.d(TAG, "Reminder $reminderId already marked as SENT. Skipping.")
            return Result.success()
        }

        // 1. Phone number validation
        val rawPhone = reminder.customerPhone.trim()
        val cleanPhone = rawPhone.filter { it.isDigit() }
        if (cleanPhone.length < 10) {
            val errorMsg = "Invalid Phone: '$rawPhone' (Must be at least 10 digits)"
            Log.e(TAG, errorMsg)
            dao.updateReminderLog(
                reminder.copy(
                    status = "FAILED",
                    error = errorMsg,
                    sentTime = System.currentTimeMillis()
                )
            )
            return Result.failure()
        }

        // 2. Fetch business profile name context
        val profiles = dao.getAllBusinessProfiles().firstOrNull()
        val businessName = profiles?.firstOrNull()?.name ?: "Credit Book"

        // 3. Dispatch payment reminder through WhatsApp Service API
        Log.d(TAG, "Triggering automatic API reminder $reminderId to $cleanPhone")
        val apiResult = WhatsAppService.sendPaymentReminder(
            customerPhone = cleanPhone,
            customerName = reminder.customerName,
            balanceAmount = reminder.amount,
            businessName = businessName,
            templateName = reminder.templateName,
            languageCode = reminder.language
        )

        val runCount = runAttemptCount // 0 for first execution, 1 for first retry, etc.

        apiResult.fold(
            onSuccess = { response ->
                Log.i(TAG, "WhatsApp API success for reminder $reminderId: $response")
                dao.updateReminderLog(
                    reminder.copy(
                        status = "SENT",
                        sentTime = System.currentTimeMillis(),
                        error = null,
                        retryCount = runCount
                    )
                )
                return Result.success()
            },
            onFailure = { exception ->
                val errorMsg = exception.localizedMessage ?: "Unknown API exception"
                Log.e(TAG, "WhatsApp API failure for reminder $reminderId (attempt $runCount): $errorMsg")

                // Distinguish credential error (401, 400 or blank tokens) from offline/timeout network errors
                val isCredentialError = errorMsg.contains("401", ignoreCase = true) || 
                                         errorMsg.contains("unauthorized", ignoreCase = true) ||
                                         errorMsg.contains("configured", ignoreCase = true) ||
                                         errorMsg.contains("400", ignoreCase = true)

                if (isCredentialError) {
                    // Do not retry API authentication errors or configuration issues
                    val detailedErr = if (errorMsg.contains("401")) {
                        "WhatsApp API Unauthorized (Error 401). Please configure valid WHATSAPP_ACCESS_TOKEN and PHONE_NUMBER_ID credentials in AI Studio Secrets."
                    } else {
                        "WhatsApp API configuration field mismatch: $errorMsg"
                    }
                    dao.updateReminderLog(
                        reminder.copy(
                            status = "FAILED",
                            sentTime = System.currentTimeMillis(),
                            error = detailedErr,
                            retryCount = runCount
                        )
                    )
                    return Result.failure()
                }

                // Retry only for network timeout / IOException up to 3 total attempts (attempt = 0, 1, 2)
                if (exception is IOException || errorMsg.contains("timeout", ignoreCase = true) || errorMsg.contains("connect", ignoreCase = true)) {
                    if (runCount < 2) {
                        Log.w(TAG, "Scheduling Retry for reminder $reminderId due to connection failure.")
                        dao.updateReminderLog(
                            reminder.copy(
                                status = "PENDING",
                                error = "Connection error: $errorMsg. Retrying...",
                                retryCount = runCount + 1
                            )
                        )
                        return Result.retry()
                    }
                }

                // Exhausted retries or non-recoverable error
                dao.updateReminderLog(
                    reminder.copy(
                        status = "FAILED",
                        sentTime = System.currentTimeMillis(),
                        error = "API Dispatch Failed: $errorMsg",
                        retryCount = runCount + 1
                    )
                )
                return Result.failure()
            }
        )
    }
}
