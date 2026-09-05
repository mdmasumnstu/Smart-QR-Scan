package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.IdCardData
import com.example.util.ShareHelper

@Composable
fun IdCardResultDialog(
    idCardData: IdCardData,
    onDismiss: () -> Unit,
    onSaveToHistory: (IdCardData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ID Card Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (idCardData.isAiPowered) "✨ AI Extracted" else "Document Scanned",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (idCardData.isAiPowered) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("id_card_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Digital ID Card Preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("id_card_preview_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = idCardData.documentType.uppercase(),
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = idCardData.fullName.ifBlank { "Cardholder" },
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = idCardData.idNumber.ifBlank { "ID: Not specified" },
                                            color = Color(0xFF38BDF8),
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (idCardData.dateOfBirth.isNotBlank()) {
                                        Column {
                                            Text(text = "DOB", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                            Text(text = idCardData.dateOfBirth, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (idCardData.expiryDate.isNotBlank()) {
                                        Column {
                                            Text(text = "EXPIRES", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                            Text(text = idCardData.expiryDate, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (idCardData.countryOrState.isNotBlank()) {
                                        Column {
                                            Text(text = "AUTHORITY", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                            Text(text = idCardData.countryOrState, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Structured Fields Table
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Extracted Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IdFieldRow(
                            label = "Full Name",
                            value = idCardData.fullName.ifBlank { "N/A" },
                            icon = Icons.Default.Person,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(idCardData.fullName))
                                ShareHelper.showToast(context, "Copied name")
                            }
                        )

                        IdFieldRow(
                            label = "Document / ID Number",
                            value = idCardData.idNumber.ifBlank { "N/A" },
                            icon = Icons.Default.Fingerprint,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(idCardData.idNumber))
                                ShareHelper.showToast(context, "Copied ID number")
                            }
                        )

                        if (idCardData.dateOfBirth.isNotBlank()) {
                            IdFieldRow(
                                label = "Date of Birth",
                                value = idCardData.dateOfBirth,
                                icon = Icons.Default.CalendarToday,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(idCardData.dateOfBirth))
                                    ShareHelper.showToast(context, "Copied DOB")
                                }
                            )
                        }

                        if (idCardData.expiryDate.isNotBlank()) {
                            IdFieldRow(
                                label = "Expiry Date",
                                value = idCardData.expiryDate,
                                icon = Icons.Default.CalendarToday,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(idCardData.expiryDate))
                                    ShareHelper.showToast(context, "Copied expiry date")
                                }
                            )
                        }

                        if (idCardData.countryOrState.isNotBlank()) {
                            IdFieldRow(
                                label = "Issuing Authority / Country",
                                value = idCardData.countryOrState,
                                icon = Icons.Default.Public,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(idCardData.countryOrState))
                                    ShareHelper.showToast(context, "Copied authority")
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val text = buildString {
                                appendLine("--- ${idCardData.documentType} ---")
                                appendLine("Name: ${idCardData.fullName}")
                                appendLine("ID Number: ${idCardData.idNumber}")
                                if (idCardData.dateOfBirth.isNotBlank()) appendLine("DOB: ${idCardData.dateOfBirth}")
                                if (idCardData.expiryDate.isNotBlank()) appendLine("Expiry: ${idCardData.expiryDate}")
                                if (idCardData.countryOrState.isNotBlank()) appendLine("Authority: ${idCardData.countryOrState}")
                            }
                            clipboardManager.setText(AnnotatedString(text.trim()))
                            ShareHelper.showToast(context, "All ID details copied")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy All", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val text = "ID Document: ${idCardData.documentType}\nName: ${idCardData.fullName}\nID#: ${idCardData.idNumber}\nDOB: ${idCardData.dateOfBirth}"
                            ShareHelper.shareText(context, text)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSaveToHistory(idCardData)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("id_card_save_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to History", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IdFieldRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
