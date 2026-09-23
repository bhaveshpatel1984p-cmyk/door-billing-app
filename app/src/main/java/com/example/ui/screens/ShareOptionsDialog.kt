package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ShareOptionsDialog(
    title: String = "Share Options",
    subtitle: String = "Choose how you want to share",
    onDismiss: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareWhatsAppBusiness: () -> Unit = onShareWhatsApp,
    onSharePdf: () -> Unit,
    onShareText: (() -> Unit)? = null,
    onPrint: (() -> Unit)? = null,
    onDeliveryChallan: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("share_options_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: WhatsApp Direct PDF
                ShareOptionRow(
                    icon = Icons.Default.Chat,
                    iconBgColor = Color(0xFF25D366),
                    title = "Share on WhatsApp (PDF)",
                    subtitle = "Generates official PDF & sends to WhatsApp",
                    testTag = "share_option_whatsapp",
                    onClick = {
                        onDismiss()
                        onShareWhatsApp()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: WhatsApp Business Direct PDF
                ShareOptionRow(
                    icon = Icons.Default.Business,
                    iconBgColor = Color(0xFF0F766E),
                    title = "Share on WhatsApp Business (PDF)",
                    subtitle = "Generates official PDF & sends to WA Business",
                    testTag = "share_option_whatsapp_business",
                    onClick = {
                        onDismiss()
                        onShareWhatsAppBusiness()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Share PDF (Android System Chooser)
                ShareOptionRow(
                    icon = Icons.Default.PictureAsPdf,
                    iconBgColor = Color(0xFFEF4444),
                    title = "Share PDF to Other Apps",
                    subtitle = "Gmail, Drive, Quick Share, Bluetooth & more",
                    testTag = "share_option_pdf_general",
                    onClick = {
                        onDismiss()
                        onSharePdf()
                    }
                )

                // Option 4: Print / Save PDF (if available)
                if (onPrint != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShareOptionRow(
                        icon = Icons.Default.Print,
                        iconBgColor = Color(0xFF0284C7),
                        title = "Print / Save PDF",
                        subtitle = "Android printer or save locally on phone",
                        testTag = "share_option_print",
                        onClick = {
                            onDismiss()
                            onPrint()
                        }
                    )
                }

                // Option 5: Delivery Challan / Dispatch Slip (if available)
                if (onDeliveryChallan != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShareOptionRow(
                        icon = Icons.Default.LocalShipping,
                        iconBgColor = Color(0xFF0369A1),
                        title = "Delivery Challan / Gate Pass",
                        subtitle = "Vehicle dispatch slip with door sizes & count",
                        testTag = "share_option_challan",
                        onClick = {
                            onDismiss()
                            onDeliveryChallan()
                        }
                    )
                }

                // Option 6: Text Summary (if available)
                if (onShareText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShareOptionRow(
                        icon = Icons.Default.Description,
                        iconBgColor = Color(0xFF6366F1),
                        title = "Share Text Summary (No PDF)",
                        subtitle = "Fast summary for SMS, chat or notes",
                        testTag = "share_option_text",
                        onClick = {
                            onDismiss()
                            onShareText()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareOptionRow(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    testTag: String = "",
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
