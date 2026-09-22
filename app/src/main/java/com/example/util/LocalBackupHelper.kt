package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalBackupHelper {

    fun createBackupFile(context: Context, jsonContent: String): File {
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "door_billing_backup_$timeStamp.json"
        val file = File(backupDir, fileName)

        FileOutputStream(file).use { out ->
            out.write(jsonContent.toByteArray(Charsets.UTF_8))
        }

        return file
    }

    fun shareBackupFile(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Nirmal Door Billing - Data Backup (${file.name})")
            putExtra(
                Intent.EXTRA_TEXT,
                "Here is the data backup file for Nirmal Door Billing & Accounting app.\nOpen this app on any device and select 'Restore from File' to restore all data."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open selected file"))

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append('\n')
            }
            reader.close()
            inputStream.close()
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
