package com.example.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DriveFileInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedTime: String
)

data class GoogleSignInOutcome(
    val account: GoogleSignInAccount? = null,
    val isCancelled: Boolean = false,
    val statusCode: Int? = null,
    val errorMessage: String? = null
)

class GoogleDriveManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val DRIVE_FILE_NAME = "door_billing_backup.json"
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val OAUTH_CLIENT_ID = "509121070165-fdlbslfnjml82j7f9bhhdacrg0qk7uv1.apps.googleusercontent.com"
    }

    private val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(OAUTH_CLIENT_ID)
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE))) {
            account
        } else {
            null
        }
    }

    fun handleSignInResultDetailed(resultCode: Int, data: Intent?): GoogleSignInOutcome {
        if (data == null) {
            return GoogleSignInOutcome(
                isCancelled = true,
                errorMessage = "Google Sign-In dialog closed"
            )
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                GoogleSignInOutcome(account = task.result)
            } else {
                val ex = task.exception
                val apiEx = ex as? ApiException
                val code = apiEx?.statusCode ?: -1
                val isCancelled = (code == 12501)
                val errorMsg = when (code) {
                    12501 -> "Google Sign-In was cancelled."
                    10 -> "Direct Google Drive API sync requires Google Cloud Console registration (Code 10). Use 'Save to Phone/Drive' or 'Share Backup' below for instant 1-tap backup without any setup!"
                    12500 -> "Google Sign-In failed (Code 12500). Please check your Google account on this device or use 'Save to Phone/Drive' below."
                    7 -> "Network connection error. Please check your internet connection."
                    4 -> "Google Sign-In required. Please sign into a Google account in phone Settings."
                    else -> apiEx?.localizedMessage ?: ex?.localizedMessage ?: "Google Sign-In status code: $code"
                }
                GoogleSignInOutcome(
                    isCancelled = isCancelled,
                    statusCode = code,
                    errorMessage = errorMsg
                )
            }
        } catch (e: ApiException) {
            val code = e.statusCode
            val isCancelled = (code == 12501)
            val errorMsg = when (code) {
                12501 -> "Google Sign-In was cancelled."
                10 -> "Direct Google Drive API sync requires Google Cloud Console registration (Code 10). Use 'Save to Phone/Drive' or 'Share Backup' below for instant 1-tap backup without any setup!"
                12500 -> "Google Sign-In failed (Code 12500). Use 'Save to Phone/Drive' below."
                7 -> "Network error during Google Sign-In."
                else -> e.localizedMessage ?: "Sign-in error code: $code"
            }
            GoogleSignInOutcome(
                isCancelled = isCancelled,
                statusCode = code,
                errorMessage = errorMsg
            )
        } catch (e: Exception) {
            GoogleSignInOutcome(
                isCancelled = false,
                errorMessage = e.localizedMessage ?: "Unexpected error during Google Sign-In"
            )
        }
    }

    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            account
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        try {
            googleSignInClient.signOut()
            true
        } catch (e: Exception) {
            false
        }
    }

    var lastRecoverableIntent: Intent? = null

    suspend fun fetchOAuthToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val androidAccount = account.account ?: android.accounts.Account(account.email ?: "", "com.google")
            GoogleAuthUtil.getToken(
                context,
                androidAccount,
                "oauth2:$DRIVE_SCOPE"
            )
        } catch (e: UserRecoverableAuthException) {
            lastRecoverableIntent = e.intent
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun findBackupFile(token: String): DriveFileInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files" +
                    "?q=name%3D'$DRIVE_FILE_NAME'+and+trashed%3Dfalse" +
                    "&spaces=drive" +
                    "&fields=files(id,name,size,modifiedTime)"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: return@withContext null

            if (files.length() > 0) {
                val f = files.getJSONObject(0)
                DriveFileInfo(
                    id = f.optString("id"),
                    name = f.optString("name"),
                    sizeBytes = f.optLong("size", 0L),
                    modifiedTime = f.optString("modifiedTime")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadBackup(token: String, backupJson: String): Result<DriveFileInfo> = withContext(Dispatchers.IO) {
        try {
            val existing = findBackupFile(token)
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            if (existing != null) {
                // Update existing file content
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/${existing.id}?uploadType=media"
                val body = backupJson.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(existing.copy(sizeBytes = backupJson.toByteArray().size.toLong()))
                } else {
                    Result.failure(Exception("Drive upload failed with code: ${response.code}"))
                }
            } else {
                // Create new file via multipart upload
                val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

                val metadataJson = JSONObject().apply {
                    put("name", DRIVE_FILE_NAME)
                    put("mimeType", "application/json")
                    put("description", "Nirmal Door Billing and Accounting Backup")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(metadataJson.toRequestBody(jsonMediaType))
                    .addPart(backupJson.toRequestBody(jsonMediaType))
                    .build()

                val request = Request.Builder()
                    .url(createUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .post(multipartBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    val json = JSONObject(respBody)
                    val id = json.optString("id")
                    Result.success(
                        DriveFileInfo(
                            id = id,
                            name = DRIVE_FILE_NAME,
                            sizeBytes = backupJson.toByteArray().size.toLong(),
                            modifiedTime = "Just now"
                        )
                    )
                } else {
                    Result.failure(Exception("Drive creation failed with code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(token: String, fileId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val content = response.body?.string() ?: ""
                Result.success(content)
            } else {
                Result.failure(Exception("Download failed with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
