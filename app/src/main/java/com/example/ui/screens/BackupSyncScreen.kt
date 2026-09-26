package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.AppBackupData
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import com.example.util.LocalBackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSyncScreen(
    viewModel: DoorBillingViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val googleAccount by viewModel.googleAccount.collectAsStateWithLifecycle()
    val driveInfo by viewModel.driveBackupInfo.collectAsStateWithLifecycle()
    val isOperating by viewModel.isBackupOperating.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastBackupType by viewModel.lastBackupType.collectAsStateWithLifecycle()

    val bills by viewModel.allBills.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()

    // Dialog state for restoring
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreSourceType by remember { mutableStateOf("Google Drive") } // "Google Drive" or "File"
    var pendingFileJson by remember { mutableStateOf<String?>(null) }
    var pendingBackupPreview by remember { mutableStateOf<AppBackupData?>(null) }
    var clearExistingOnRestore by remember { mutableStateOf(true) }

    // Launcher for creating & saving backup file directly into Storage / Drive (100% reliable)
    val saveBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveBackupToUri(uri, context)
        }
    }

    // Launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onGoogleSignInResult(result.resultCode, result.data)
    }

    // Launcher for picking backup file from device/Drive
    val pickBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val readResult = LocalBackupHelper.readBackupFromUri(context, uri)
                readResult.fold(
                    onSuccess = { content ->
                        try {
                            val parsed = AppBackupData.fromJsonString(content)
                            withContext(Dispatchers.Main) {
                                pendingFileJson = content
                                pendingBackupPreview = parsed
                                restoreSourceType = "Selected File"
                                showRestoreConfirmDialog = true
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                viewModel.showMessage("Invalid backup file: ${e.localizedMessage}")
                            }
                        }
                    },
                    onFailure = { err ->
                        withContext(Dispatchers.Main) {
                            viewModel.showMessage("Error reading file: ${err.localizedMessage}")
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshGoogleAccount()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Backup & Multi-Device Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Google Drive & Local Data Backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (googleAccount != null) {
                        IconButton(
                            onClick = { viewModel.checkDriveBackup() },
                            enabled = !isOperating
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Drive Status")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("backup_sync_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (googleAccount != null) Color(0xFFE8F5E9) else Color(0xFFF1F5F9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (googleAccount != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (googleAccount != null) Icons.Default.CloudDone else Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (googleAccount != null) "Google Drive Connected" else "Google Drive Not Connected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (googleAccount != null) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (googleAccount != null) {
                                        googleAccount?.email ?: "Account linked"
                                    } else {
                                        "Connect to backup & access data across devices"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (googleAccount != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Last Backup",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (lastBackupTime > 0) {
                                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastBackupTime)) +
                                                if (lastBackupType.isNotBlank()) " ($lastBackupType)" else ""
                                    } else {
                                        "No backup taken yet"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "${bills.size} Bills | ${customers.size} Customers",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Google Drive Cloud Backup Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GOOGLE DRIVE CLOUD SYNC",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Text(
                            text = "Save your database to your personal Google Drive account. On another device, log in with the same account and tap Restore to sync all bills and balances.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (googleAccount == null) {
                            // Primary 1-Tap Google Drive Backup Button
                            Button(
                                onClick = {
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    saveBackupFileLauncher.launch("door_billing_backup_$timeStamp.json")
                                },
                                enabled = !isOperating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("backup_to_drive_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Backup to Google Drive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }

                            // Primary 1-Tap Google Drive Restore Button
                            OutlinedButton(
                                onClick = {
                                    pickBackupFileLauncher.launch("*/*")
                                },
                                enabled = !isOperating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("restore_from_drive_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF1E88E5))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Restore from Google Drive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Secondary Quick Actions: Drive App Upload & WhatsApp Transfer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val json = viewModel.getExportBackupJson()
                                            val file = LocalBackupHelper.createBackupFile(context, json)
                                            val intent = LocalBackupHelper.createDriveUploadIntent(context, file)
                                            context.startActivity(intent)
                                        }
                                    },
                                    enabled = !isOperating,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("upload_via_drive_app_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Drive App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val json = viewModel.getExportBackupJson()
                                            val file = LocalBackupHelper.createBackupFile(context, json)
                                            val intent = LocalBackupHelper.shareBackupFile(context, file)
                                            val chooser = Intent.createChooser(intent, "Share Data Backup via").apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(chooser)
                                        }
                                    },
                                    enabled = !isOperating,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("export_backup_file_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 2-Phone Sync Guide Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Doosre Phone me Sync Kaise Karein:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    Text(
                                        text = "1. Is phone me 'Backup to Google Drive' dabayein aur apna Drive folder chunein.\n" +
                                                "2. Doosre phone me ye app kholein aur 'Restore from Google Drive' dabakar vahi backup file select karein.\n" +
                                                "3. Aapke sabhi bills, customers aur balances turant sync ho jayenge!",
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            // User is signed in
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = googleAccount?.displayName ?: "Google Account",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = googleAccount?.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.signOutGoogleDrive() },
                                        enabled = !isOperating
                                    ) {
                                        Text("Disconnect", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Cloud file info
                            driveInfo?.let { info ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Cloud Backup Available",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF166534)
                                            )
                                            Text(
                                                text = "Size: ${String.format(Locale.US, "%.1f", info.sizeBytes / 1024.0)} KB | File: ${info.name}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Backup button
                                Button(
                                    onClick = {
                                        viewModel.backupToGoogleDrive()
                                    },
                                    enabled = !isOperating,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("backup_to_drive_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                                ) {
                                    if (isOperating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Backup Now", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Restore button
                                OutlinedButton(
                                    onClick = {
                                        restoreSourceType = "Google Drive"
                                        pendingFileJson = null
                                        showRestoreConfirmDialog = true
                                    },
                                    enabled = !isOperating && driveInfo != null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("restore_from_drive_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Data", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Local & WhatsApp File Transfer Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Devices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OFFLINE & DIRECT FILE TRANSFER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }

                        Text(
                            text = "Save your complete database directly into your Phone storage or Google Drive folder in 1-tap. You can also share the backup file via WhatsApp or restore any saved file anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1-Tap Save to Phone / Google Drive Button (100% Reliable SAF)
                        Button(
                            onClick = {
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                saveBackupFileLauncher.launch("door_billing_backup_$timeStamp.json")
                            },
                            enabled = !isOperating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_backup_to_storage_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Phone / Google Drive", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Export and share
                            Button(
                                onClick = {
                                    scope.launch {
                                        val json = viewModel.getExportBackupJson()
                                        val file = LocalBackupHelper.createBackupFile(context, json)
                                        val intent = LocalBackupHelper.shareBackupFile(context, file)
                                        val chooser = Intent.createChooser(intent, "Share Data Backup via").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                    }
                                },
                                enabled = !isOperating,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_backup_file_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp green
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp Share", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Pick file and restore
                            OutlinedButton(
                                onClick = {
                                    pickBackupFileLauncher.launch("*/*")
                                },
                                enabled = !isOperating,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("restore_from_file_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore File", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // How Multi-Device Works Help Guide
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to access on another device:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "1. Device 1 (Current): Connect your Google Drive and tap 'Backup Now' (or tap 'Share Backup' and send via WhatsApp).\n\n" +
                                    "2. Device 2 (New): Open this app, go to Backup & Sync, connect the same Google account, and tap 'Restore Data' (or tap 'Select File' and choose the file sent via WhatsApp).\n\n" +
                                    "3. All your bills, customers, measurements, rates, and outstanding balances will appear immediately on the second device.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog before Restoring
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isOperating) {
                    showRestoreConfirmDialog = false
                    pendingFileJson = null
                    pendingBackupPreview = null
                }
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Data Restore",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you sure you want to restore data from $restoreSourceType?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    pendingBackupPreview?.let { preview ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Backup Preview:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "• Business: ${preview.companyProfile?.businessName ?: "Nirmal Door"}\n" +
                                            "• Bills: ${preview.bills.size} entries\n" +
                                            "• Customers: ${preview.customers.size} records\n" +
                                            "• Purchases: ${preview.purchases.size} records",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Restore Mode:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = clearExistingOnRestore,
                            onClick = { clearExistingOnRestore = true }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Replace all current data", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Best for full device sync (recommended)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = !clearExistingOnRestore,
                            onClick = { clearExistingOnRestore = false }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Merge with current data", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Keeps existing data and adds new", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreSourceType == "Google Drive") {
                            viewModel.restoreFromGoogleDrive(clearExisting = clearExistingOnRestore) { success, _ ->
                                showRestoreConfirmDialog = false
                            }
                        } else {
                            pendingFileJson?.let { json ->
                                viewModel.restoreFromJsonString(json, clearExisting = clearExistingOnRestore) { success, _ ->
                                    showRestoreConfirmDialog = false
                                    pendingFileJson = null
                                    pendingBackupPreview = null
                                }
                            }
                        }
                    },
                    enabled = !isOperating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Restore Now", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingFileJson = null
                        pendingBackupPreview = null
                    },
                    enabled = !isOperating
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
