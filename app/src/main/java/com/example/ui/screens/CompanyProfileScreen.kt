package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.db.CompanyProfileEntity
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import com.example.util.QrCodeHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(
    viewModel: DoorBillingViewModel
) {
    val context = LocalContext.current
    val profileState by viewModel.companyProfile.collectAsStateWithLifecycle()

    var businessName by remember(profileState) { mutableStateOf(profileState.businessName) }
    var address by remember(profileState) { mutableStateOf(profileState.address) }
    var gstNo by remember(profileState) { mutableStateOf(profileState.gstNo) }
    var mobile by remember(profileState) { mutableStateOf(profileState.mobile) }
    var email by remember(profileState) { mutableStateOf(profileState.email) }
    var pan by remember(profileState) { mutableStateOf(profileState.pan) }
    var state by remember(profileState) { mutableStateOf(profileState.state) }
    var stateCode by remember(profileState) { mutableStateOf(profileState.stateCode) }
    var bankName by remember(profileState) { mutableStateOf(profileState.bankName) }
    var accountNo by remember(profileState) { mutableStateOf(profileState.accountNo) }
    var ifscCode by remember(profileState) { mutableStateOf(profileState.ifscCode) }
    var jurisdiction by remember(profileState) { mutableStateOf(profileState.jurisdiction) }
    var declaration by remember(profileState) { mutableStateOf(profileState.declaration) }
    var logoUri by remember(profileState) { mutableStateOf(profileState.logoUri) }
    var qrCodeUri by remember(profileState) { mutableStateOf(profileState.qrCodeUri) }
    var upiId by remember(profileState) { mutableStateOf(profileState.upiId) }

    // Android Photo Picker for company logo (zero permissions required)
    // Copies image permanently into internal storage to avoid permission revocation
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val logoFile = File(context.filesDir, "company_logo.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    logoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                logoUri = logoFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Android Photo Picker for Payment QR Code (PhonePe, Google Pay, Paytm, Bank Standee)
    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val qrFile = File(context.filesDir, "company_payment_qr.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    qrFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                qrCodeUri = qrFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Company Profile View / Edit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.BACKUP_SYNC) },
                        modifier = Modifier.testTag("company_backup_sync_button")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Backup & Multi-Device Sync")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("company_profile_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Company Logo & Branding Section
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "COMPANY LOGO & IDENTITY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Logo display
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!logoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = logoUri,
                                    contentDescription = "Company Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = com.example.R.drawable.img_nirmal_door_logo,
                                    contentDescription = "Default Nirmal Door Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload / Change Logo", fontSize = 12.sp)
                            }

                            if (!logoUri.isNullOrBlank()) {
                                TextButton(onClick = {
                                    try {
                                        File(context.filesDir, "company_logo.png").delete()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    logoUri = null
                                }) {
                                    Text("Reset Logo", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }

                        Text(
                            text = "This logo will appear on the top of printed bills, PDF invoices and customer ledger statements.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 2. Business Profile Fields
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "BUSINESS INFORMATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        // 1) Business ka Name
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business / Firm Name *") },
                            placeholder = { Text("e.g. Shree Ram Door & Timber Mart") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("company_name_input")
                        )

                        // 2) Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Business Address *") },
                            placeholder = { Text("Plot No 12, Timber Market, Main Road") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 3) GST No & 4) PAN No
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = gstNo,
                                onValueChange = { gstNo = it.uppercase() },
                                label = { Text("GSTIN No") },
                                placeholder = { Text("24ABCDE1234F1Z5") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1.2f)
                            )

                            OutlinedTextField(
                                value = pan,
                                onValueChange = { pan = it.uppercase() },
                                label = { Text("PAN No") },
                                placeholder = { Text("ABCDE1234F") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 5) Mobile & 6) Email
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("Mobile No *") },
                                placeholder = { Text("9876543210") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("info@doormart.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 7) State & 8) State Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State") },
                                placeholder = { Text("Gujarat") },
                                leadingIcon = { Icon(Icons.Default.PinDrop, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1.3f)
                            )

                            OutlinedTextField(
                                value = stateCode,
                                onValueChange = { stateCode = it },
                                label = { Text("State Code") },
                                placeholder = { Text("24") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }
            }

            // 3. Bank Account Details Section
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "BANK ACCOUNT DETAILS (FOR BILL PAYMENT)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        // 9) Bank Name
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            placeholder = { Text("e.g. State Bank of India") },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 10) Account No & 11) IFSC Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = accountNo,
                                onValueChange = { accountNo = it },
                                label = { Text("Account No.") },
                                placeholder = { Text("12345678901234") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1.2f)
                            )

                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it.uppercase() },
                                label = { Text("IFSC Code") },
                                placeholder = { Text("SBIN0001234") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Payment QR Code & UPI Section (Prints on Bill)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PAYMENT QR CODE & UPI (PRINT ON BILL)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // QR Preview Box
                        val activeQrBitmap = remember(qrCodeUri, upiId, businessName) {
                            if (!qrCodeUri.isNullOrBlank() && File(qrCodeUri!!).exists()) {
                                try {
                                    android.graphics.BitmapFactory.decodeFile(qrCodeUri)
                                } catch (e: Exception) {
                                    null
                                }
                            } else if (upiId.isNotBlank()) {
                                QrCodeHelper.generateQrBitmap(QrCodeHelper.buildUpiString(upiId, businessName), size = 300)
                            } else {
                                null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(
                                    2.dp,
                                    if (activeQrBitmap != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    qrPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeQrBitmap != null) {
                                Image(
                                    bitmap = activeQrBitmap.asImageBitmap(),
                                    contentDescription = "Payment QR Code",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "No QR Code",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        "Tap to upload",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Status Banner
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeQrBitmap != null) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (!qrCodeUri.isNullOrBlank()) {
                                    "✓ Custom QR Image Uploaded (PhonePe/GPay/Bank)"
                                } else if (upiId.isNotBlank()) {
                                    "✓ Auto-generating UPI QR from: $upiId"
                                } else {
                                    "⚠️ Upload a QR image or enter UPI ID below"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeQrBitmap != null) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    qrPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (qrCodeUri == null) "Upload QR Code" else "Change QR Image", fontSize = 12.sp)
                            }

                            if (!qrCodeUri.isNullOrBlank()) {
                                TextButton(onClick = {
                                    try {
                                        File(context.filesDir, "company_payment_qr.png").delete()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    qrCodeUri = null
                                }) {
                                    Text("Remove Image", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // UPI ID Field
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it.trim() },
                            label = { Text("UPI ID / VPA (Optional)") },
                            placeholder = { Text("e.g. nirmaldoor@upi or 9876543210@paytm") },
                            supportingText = {
                                Text("If no image is uploaded, bills will auto-generate a scannable UPI QR code using this ID.")
                            },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "💡 Scan to Pay: This QR code will appear directly on printed bills, PDF invoices, and ledger statements so customers can quickly pay from PhonePe, GPay, Paytm, or BHIM.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // 4. Legal & Terms Section
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "LEGAL & DECLARATION TERMS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        // 12) Jurisdiction
                        OutlinedTextField(
                            value = jurisdiction,
                            onValueChange = { jurisdiction = it },
                            label = { Text("Jurisdiction City / Clause") },
                            placeholder = { Text("KUDACHI") },
                            supportingText = { Text("Appears as 'SUBJECT TO [CITY] JURISDICTION ONLY' on bills") },
                            leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 13) Declaration
                        OutlinedTextField(
                            value = declaration,
                            onValueChange = { declaration = it },
                            label = { Text("Invoice Declaration / Terms") },
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                            singleLine = false,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        val updated = profileState.copy(
                            businessName = businessName.trim().ifBlank { "Door Craft & Billing" },
                            address = address.trim(),
                            gstNo = gstNo.trim().uppercase(),
                            mobile = mobile.trim(),
                            email = email.trim(),
                            pan = pan.trim().uppercase(),
                            state = state.trim(),
                            stateCode = stateCode.trim(),
                            bankName = bankName.trim(),
                            accountNo = accountNo.trim(),
                            ifscCode = ifscCode.trim().uppercase(),
                            jurisdiction = jurisdiction.trim(),
                            declaration = declaration.trim(),
                            logoUri = logoUri,
                            qrCodeUri = qrCodeUri,
                            upiId = upiId.trim()
                        )
                        viewModel.updateCompanyProfile(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_company_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Company Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // 4. Cloud Backup & Multi-Device Sync Shortcut Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.BACKUP_SYNC) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Drive Cloud Backup & Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Backup database & access on another device",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
