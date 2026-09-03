package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val customerBalances by viewModel.customerBalances.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var showExitDialog by remember { mutableStateOf(false) }

    val totalOutstanding = customerBalances.sumOf { it.balance }
    val totalRevenue = bills.sumOf { it.bill.grandTotal }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Header with Company Profile
        item {
            DashboardHeader(company = company, onCompanyClick = { viewModel.navigateTo(AppScreen.COMPANY_PROFILE) })
        }

        // Summary Stats Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Billed",
                    value = DimensionCalculator.formatCurrency(totalRevenue),
                    subtitle = "${bills.size} Invoices (Tap for Stats)",
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AppScreen.FINANCIAL_STATS) }
                )
                StatCard(
                    title = "Due Balance",
                    value = DimensionCalculator.formatCurrency(totalOutstanding),
                    subtitle = "${customers.size} Customers (Tap for Stats)",
                    color = if (totalOutstanding > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AppScreen.FINANCIAL_STATS) }
                )
            }
        }

        // Main Navigation Grid / Action Buttons as specified in prompt
        item {
            Text(
                text = "MAIN MENU / DASHBOARD ACTIONS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 1) Create Customer & 2) New Entry
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "1) Create Customer",
                    subtitle = "Add Name, Mobile, GST & Address",
                    icon = Icons.Default.PersonAdd,
                    iconBgColor = Color(0xFF0284C7),
                    testTag = "create_customer_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.prepareNewCustomer()
                        viewModel.navigateTo(AppScreen.CREATE_CUSTOMER)
                    }
                )

                DashboardActionButton(
                    title = "2) New Entry",
                    subtitle = "Create Door Bill (Sq.Ft calc)",
                    icon = Icons.Default.AddCircle,
                    iconBgColor = Color(0xFF16A34A),
                    testTag = "new_entry_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startNewBill() }
                )
            }
        }

        // 3) Edit Entry & 4) View Customer Balance
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "3) Edit Entry",
                    subtitle = "View, Edit & Delete Bills",
                    icon = Icons.Default.EditNote,
                    iconBgColor = Color(0xFFD97706),
                    testTag = "edit_entry_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.EDIT_ENTRY) }
                )

                DashboardActionButton(
                    title = "4) View Balance",
                    subtitle = "Ledger, Print & WhatsApp",
                    icon = Icons.Default.AccountBalance,
                    iconBgColor = Color(0xFF7C3AED),
                    testTag = "view_customer_balance_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CUSTOMER_BALANCE) }
                )
            }
        }

        // 5) Company Profile view/edit & Financial Stats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "5) Company Profile",
                    subtitle = "GST, Bank & Logo Settings",
                    icon = Icons.Default.Business,
                    iconBgColor = Color(0xFF0F766E),
                    testTag = "company_profile_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.COMPANY_PROFILE) }
                )

                DashboardActionButton(
                    title = "6) Financial Stats",
                    subtitle = "Revenue, Due & Invoices",
                    icon = Icons.Default.Assessment,
                    iconBgColor = Color(0xFF0284C7),
                    testTag = "financial_stats_button",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.FINANCIAL_STATS) }
                )
            }
        }

        // 7) Exit
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionButton(
                    title = "7) Exit",
                    subtitle = "Close Application",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconBgColor = Color(0xFF475569),
                    testTag = "exit_button",
                    modifier = Modifier.weight(1f),
                    onClick = { showExitDialog = true }
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Recent Invoices Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT INVOICES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (bills.isNotEmpty()) {
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.EDIT_ENTRY) }) {
                        Text("View All (${bills.size})")
                    }
                }
            }
        }

        if (bills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Bills Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap 'New Entry' above to create your first door invoice with Sq.ft calculation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(bills.take(4)) { billWithItems ->
                RecentBillItem(
                    billWithItems = billWithItems,
                    onClick = { onViewBill(billWithItems) }
                )
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App?") },
            text = { Text("Are you sure you want to close the Door Billing app?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finishAffinity()
                    }
                ) {
                    Text("Exit", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0369A1),
                            Color(0xFF075985),
                            Color(0xFF0C4A6E)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Company Logo or Default Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(2.dp),
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

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = company.businessName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "GST: ${company.gstNo} • ${company.state}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "📞 ${company.mobile}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun DashboardActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(115.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
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
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconBgColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bill.customerName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${billWithItems.items.size} items • ${String.format(java.util.Locale.US, "%.1f", totalSqFt)} Sq.Ft",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DimensionCalculator.formatCurrency(bill.grandTotal),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1)
                    )
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (bill.paidAmount >= bill.grandTotal) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = if (bill.paidAmount >= bill.grandTotal) "Paid" else "Unpaid",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (bill.paidAmount >= bill.grandTotal) Color(0xFF166534) else Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
