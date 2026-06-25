package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class WhatsAppMessageRequest(
    @Json(name = "messaging_product") val messagingProduct: String = "whatsapp",
    @Json(name = "recipient_type") val recipientType: String = "individual",
    @Json(name = "to") val to: String,
    @Json(name = "type") val type: String = "template",
    @Json(name = "template") val template: WhatsAppTemplate
)

@JsonClass(generateAdapter = true)
data class WhatsAppTemplate(
    @Json(name = "name") val name: String,
    @Json(name = "language") val language: WhatsAppLanguage,
    @Json(name = "components") val components: List<WhatsAppTemplateComponent>? = null
)

@JsonClass(generateAdapter = true)
data class WhatsAppLanguage(
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class WhatsAppTemplateComponent(
    @Json(name = "type") val type: String = "body",
    @Json(name = "parameters") val parameters: List<WhatsAppTemplateParameter>
)

@JsonClass(generateAdapter = true)
data class WhatsAppTemplateParameter(
    @Json(name = "type") val type: String = "text",
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class WhatsAppResponse(
    @Json(name = "messaging_product") val messagingProduct: String? = null,
    @Json(name = "contacts") val contacts: List<WhatsAppContact>? = null,
    @Json(name = "messages") val messages: List<WhatsAppMessageId>? = null
)

@JsonClass(generateAdapter = true)
data class WhatsAppContact(
    @Json(name = "input") val input: String,
    @Json(name = "wa_id") val waId: String
)

@JsonClass(generateAdapter = true)
data class WhatsAppMessageId(
    @Json(name = "id") val id: String
)

interface WhatsAppApi {
    @POST("v19.0/{phone_number_id}/messages")
    suspend fun sendTemplateMessage(
        @Path("phone_number_id") phoneNumberId: String,
        @Header("Authorization") authorization: String,
        @Body request: WhatsAppMessageRequest
    ): Response<WhatsAppResponse>
}

object WhatsAppService {
    private const val TAG = "WhatsAppService"
    private const val BASE_URL = "https://graph.facebook.com/"

    private val api: WhatsAppApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)
    }

    /**
     * Send a template payment reminder.
     * Standard pre-approved template contains 3 parameters in body (e.g. customer name, balance amount, business name).
     */
    suspend fun sendPaymentReminder(
        customerPhone: String,
        customerName: String,
        balanceAmount: Double,
        businessName: String,
        templateName: String = BuildConfig.WHATSAPP_TEMPLATE_NAME,
        languageCode: String = BuildConfig.WHATSAPP_TEMPLATE_LANG
    ): Result<WhatsAppResponse> {
        val accessToken = BuildConfig.WHATSAPP_ACCESS_TOKEN
        val phoneNumberId = BuildConfig.WHATSAPP_PHONE_NUMBER_ID

        // Validate configuration safely
        if (accessToken.isBlank() || accessToken == "YOUR_WHATSAPP_ACCESS_TOKEN_PLACEHOLDER") {
            return Result.failure(Exception("WhatsApp Access Token is not configured in Secrets"))
        }
        if (phoneNumberId.isBlank() || phoneNumberId == "YOUR_WHATSAPP_PHONE_NUMBER_ID_PLACEHOLDER") {
            return Result.failure(Exception("WhatsApp Phone Number ID is not configured in Secrets"))
        }

        // Format phone number according to standard (needs landcode/prefix, e.g. "91" for India. We can sanitize)
        val sanitizedPhone = sanitizePhoneNumber(customerPhone)

        val parameters = listOf(
            WhatsAppTemplateParameter(text = customerName),
            WhatsAppTemplateParameter(text = "₹${balanceAmount.toInt()}"),
            WhatsAppTemplateParameter(text = businessName)
        )

        val request = WhatsAppMessageRequest(
            to = sanitizedPhone,
            template = WhatsAppTemplate(
                name = templateName,
                language = WhatsAppLanguage(code = languageCode),
                components = listOf(WhatsAppTemplateComponent(parameters = parameters))
            )
        )

        return try {
            val authHeader = "Bearer $accessToken"
            val response = api.sendTemplateMessage(phoneNumberId, authHeader, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "API failed response (Code ${response.code()}): $errorBody")
                Result.failure(Exception("WhatsApp API Error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception sending WhatsApp reminder", e)
            Result.failure(e)
        }
    }

    private fun sanitizePhoneNumber(phone: String): String {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return if (clean.length == 10) {
            "91$clean" // default to Indian prefix if exactly 10 digits
        } else {
            clean
        }
    }
}
