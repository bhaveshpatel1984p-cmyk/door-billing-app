package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.db.BillWithItems
import com.example.data.db.CompanyProfileEntity
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import com.example.util.DimensionCalculator

@Composable
fun DashboardScreen(
    viewModel: DoorBillingViewModel,
    onViewBill: (BillWithItems) -> Unit
) {
    val context = LocalContext.current
    val company by viewModel.companyProfile.collectAsStateWithLifecycle()
    val bills by viewModel.allBills.collectAsStateWithLifecycle()

    var showExitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Company Profile Header
        item {
            DashboardHeader(
                company = company,
                onCompanyClick = { viewModel.navigateTo(AppScreen.COMPANY_PROFILE) }
            )
        }

        // Action Buttons Row 1: 1) Create Customer & 2) New Entry
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "1) Create Customer",
                    subtitle = "Add party with GST",
                    icon = Icons.Default.PersonAdd,
                    testTag = "create_customer_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.prepareNewCustomer()
                        viewModel.navigateTo(AppScreen.CREATE_CUSTOMER)
                    }
                )

                DashboardActionButton(
                    title = "2) New Entry",
                    subtitle = "Create door bill",
                    icon = Icons.Default.AddCircle,
                    testTag = "new_entry_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startNewBill() }
                )
            }
        }

        // Action Buttons Row 2: 3) Edit Entry & 4) View Balance
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "3) Edit Entry",
                    subtitle = "Search & modify bills",
                    icon = Icons.Default.EditNote,
                    testTag = "edit_entry_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.EDIT_ENTRY) }
                )

                DashboardActionButton(
                    title = "4) View Balance",
                    subtitle = "Customer dues & ledger",
                    icon = Icons.Default.AccountBalance,
                    testTag = "view_customer_balance_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CUSTOMER_BALANCE) }
                )
            }
        }

        // Action Buttons Row 3: 5) Company Profile & 6) Financial Stats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "5) Company Profile",
                    subtitle = "Settings & print details",
                    icon = Icons.Default.Business,
                    testTag = "company_profile_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.COMPANY_PROFILE) }
                )

                DashboardActionButton(
                    title = "6) Financial Stats",
                    subtitle = "Reports & sales summary",
                    icon = Icons.Default.Assessment,
                    testTag = "financial_stats_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.FINANCIAL_STATS) }
                )
            }
        }

        // Action Buttons Row 4: 7) Purchase Hub & 8) Cloud Backup
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "7) Purchase Hub",
                    subtitle = "Purchases & inventory",
                    icon = Icons.Default.ShoppingCart,
                    testTag = "purchase_hub_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.PURCHASE_HUB) }
                )

                DashboardActionButton(
                    title = "8) Cloud Backup",
                    subtitle = "Export & sync data",
                    icon = Icons.Default.CloudSync,
                    testTag = "cloud_backup_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.BACKUP_SYNC) }
                )
            }
        }

        // Action Buttons Row 5: 9) Exit Application
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "9) Exit",
                    subtitle = "Close application",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    testTag = "exit_button",
                    modifier = Modifier.weight(1f),
                    onClick = { showExitDialog = true }
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Recent Invoices Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Bills",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (bills.isNotEmpty()) {
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.EDIT_ENTRY) }) {
                        Text("View All (${bills.size})")
                    }
                }
            }
        }

        // Recent Bills List
        if (bills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No bills created yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        } else {
            items(bills.take(5)) { billWithItems ->
                RecentBillItem(
                    billWithItems = billWithItems,
                    onClick = { onViewBill(billWithItems) }
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Application") },
            text = { Text("Are you sure you want to exit the app?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finishAffinity()
                    }
                ) {
                    Text("Exit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DashboardHeader(
    company: CompanyProfileEntity,
    onCompanyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onCompanyClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!company.logoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = company.logoUri,
                            contentDescription = "Company Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.img_nirmal_door_logo),
                            contentDescription = "Nirmal Door Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.businessName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                if (company.gstNo.isNotBlank()) {
                    Text(
                        text = "GST: ${company.gstNo}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    )
                }
                Text(
                    text = "📞 ${company.displayMobile}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                )
                if (company.state.isNotBlank()) {
                    Text(
                        text = company.state,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            IconButton(onClick = onCompanyClick) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Edit Company Profile",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun DashboardActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(100.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RecentBillItem(
    billWithItems: BillWithItems,
    onClick: () -> Unit
) {
    val bill = billWithItems.bill
    val totalSqFt = billWithItems.items.sumOf { it.sqFt }
    val isFullPaid = bill.paidAmount >= bill.grandTotal
    val isPartial = bill.paidAmount > 0 && bill.paidAmount < bill.grandTotal

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.invoiceNo,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DimensionCalculator.formatDate(bill.dateMillis),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bill.customerName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${billWithItems.items.size} items • ${String.format(java.util.Locale.US, "%.1f", totalSqFt)} Sq.Ft",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DimensionCalculator.formatCurrency(bill.grandTotal),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = when {
                        isFullPaid -> "PAID"
                        isPartial -> "PARTIAL"
                        else -> "UNPAID"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isFullPaid -> Color(0xFF16A34A)
                            isPartial -> Color(0xFFD97706)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                )
            }
        }
    }
}
