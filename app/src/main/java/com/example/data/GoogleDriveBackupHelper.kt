package com.example.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object GoogleDriveBackupHelper {
    private const val TAG = "GoogleDriveBackup"
    private val client = OkHttpClient()

    // Scopes to request
    private const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    suspend fun backupToDrive(context: Context, jsonStr: String): Result<String> {
        return kotlinx.coroutines.Dispatchers.IO.run {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: return@run Result.failure(Exception("No Google account signed in. Please sign in first."))

                val googleAccount = account.account
                    ?: return@run Result.failure(Exception("Failed to get Google account details."))

                // Get OAuth Token from Play Services
                val scopeString = "oauth2:$DRIVE_APP_DATA_SCOPE $DRIVE_FILE_SCOPE"
                val accessToken = GoogleAuthUtil.getToken(context, googleAccount, scopeString)
                    ?: return@run Result.failure(Exception("Failed to obtain Google Drive token."))

                // 1. Search if backup file already exists in private appDataFolder
                val searchUrl = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='smart_ledger_backup.json'&fields=files(id,name)"
                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val searchResponse = client.newCall(searchRequest).execute()
                if (!searchResponse.isSuccessful) {
                    val errMsg = searchResponse.body?.string() ?: ""
                    return@run Result.failure(Exception("Drive search failed: Code ${searchResponse.code} $errMsg"))
                }

                val searchResultJson = JSONObject(searchResponse.body?.string() ?: "{}")
                val filesArray = searchResultJson.optJSONArray("files")
                var existingFileId: String? = null
                if (filesArray != null && filesArray.length() > 0) {
                    existingFileId = filesArray.getJSONObject(0).optString("id")
                }

                if (existingFileId != null) {
                    // 2. File exists! Directly upload media byte contents using PATCH
                    val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                    val requestBody = jsonStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val uploadRequest = Request.Builder()
                        .url(uploadUrl)
                        .header("Authorization", "Bearer $accessToken")
                        .patch(requestBody)
                        .build()

                    val uploadResponse = client.newCall(uploadRequest).execute()
                    if (uploadResponse.isSuccessful) {
                        Result.success("Success: Ledger backup updated on Google Drive!")
                    } else {
                        val errMsg = uploadResponse.body?.string() ?: ""
                        Result.failure(Exception("Restore file update failed: ${uploadResponse.code} $errMsg"))
                    }
                } else {
                    // 3. File doesn't exist! Create metadata first using POST
                    val createUrl = "https://www.googleapis.com/drive/v3/files"
                    val metadataJson = JSONObject().apply {
                        put("name", "smart_ledger_backup.json")
                        val parentsArr = org.json.JSONArray().apply { put("appDataFolder") }
                        put("parents", parentsArr)
                    }
                    val metadataBody = metadataJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val createRequest = Request.Builder()
                        .url(createUrl)
                        .header("Authorization", "Bearer $accessToken")
                        .post(metadataBody)
                        .build()

                    val createResponse = client.newCall(createRequest).execute()
                    if (!createResponse.isSuccessful) {
                        val errMsg = createResponse.body?.string() ?: ""
                        return@run Result.failure(Exception("Failed to create Drive metadata: ${createResponse.code} $errMsg"))
                    }

                    val fileId = JSONObject(createResponse.body?.string() ?: "{}").optString("id")
                    if (fileId.isEmpty()) {
                        return@run Result.failure(Exception("Created file ID was empty."))
                    }

                    // 4. Upload actual JSON data to newly created file
                    val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
                    val requestBody = jsonStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val uploadRequest = Request.Builder()
                        .url(uploadUrl)
                        .header("Authorization", "Bearer $accessToken")
                        .patch(requestBody)
                        .build()

                    val uploadResponse = client.newCall(uploadRequest).execute()
                    if (uploadResponse.isSuccessful) {
                        Result.success("Success: Ledger backup created on Google Drive!")
                    } else {
                        val errMsg = uploadResponse.body?.string() ?: ""
                        Result.failure(Exception("File content upload failed: ${uploadResponse.code} $errMsg"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun restoreFromDrive(context: Context): Result<String> {
        return kotlinx.coroutines.Dispatchers.IO.run {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                    ?: return@run Result.failure(Exception("No Google account signed in. Please sign in first."))

                val googleAccount = account.account
                    ?: return@run Result.failure(Exception("Failed to get Google account details."))

                val scopeString = "oauth2:$DRIVE_APP_DATA_SCOPE $DRIVE_FILE_SCOPE"
                val accessToken = GoogleAuthUtil.getToken(context, googleAccount, scopeString)
                    ?: return@run Result.failure(Exception("Failed to obtain Google Drive token."))

                // 1. Search for existing file
                val searchUrl = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='smart_ledger_backup.json'&fields=files(id,name)"
                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val searchResponse = client.newCall(searchRequest).execute()
                if (!searchResponse.isSuccessful) {
                    val errMsg = searchResponse.body?.string() ?: ""
                    return@run Result.failure(Exception("Drive search failed: Code ${searchResponse.code} $errMsg"))
                }

                val searchResultJson = JSONObject(searchResponse.body?.string() ?: "{}")
                val filesArray = searchResultJson.optJSONArray("files")
                var existingFileId: String? = null
                if (filesArray != null && filesArray.length() > 0) {
                    existingFileId = filesArray.getJSONObject(0).optString("id")
                }

                if (existingFileId == null) {
                    return@run Result.failure(Exception("No backup file found on Google Drive. Create a backup first!"))
                }

                // 2. Retrieve alt=media content
                val downloadUrl = "https://www.googleapis.com/drive/v3/files/$existingFileId?alt=media"
                val downloadRequest = Request.Builder()
                    .url(downloadUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val downloadResponse = client.newCall(downloadRequest).execute()
                if (downloadResponse.isSuccessful) {
                    val rawBody = downloadResponse.body?.string() ?: ""
                    if (rawBody.trim().startsWith("{")) {
                        Result.success(rawBody)
                    } else {
                        Result.failure(Exception("Retrieved raw content is not a valid JSON string."))
                    }
                } else {
                    val errMsg = downloadResponse.body?.string() ?: ""
                    Result.failure(Exception("Download failed: Code ${downloadResponse.code} $errMsg"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
