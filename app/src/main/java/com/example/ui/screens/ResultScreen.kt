package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiPassword
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiAnalysisResult
import com.example.data.ai.SecurityRating
import com.example.data.model.CodeType
import com.example.data.model.ScanItem
import com.example.ui.components.UrlSafetyDialog
import com.example.util.ParsedBarcodeResult
import com.example.util.ShareHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: ParsedBarcodeResult,
    scanItem: ScanItem?,
    onToggleFavorite: (ScanItem) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    aiAnalysis: AiAnalysisResult? = null,
    isAiAnalyzing: Boolean = false,
    onAnalyzeWithAi: () -> Unit = {}
) {
    val context = LocalContext.current
    var showUrlWarning by remember { mutableStateOf(false) }
    var pendingUrlToOpen by remember { mutableStateOf("") }

    val icon = getIconForType(result.type)
    val color = getColorForType(result.type)

    if (showUrlWarning) {
        UrlSafetyDialog(
            url = pendingUrlToOpen,
            onConfirm = {
                showUrlWarning = false
                ShareHelper.openUrl(context, pendingUrlToOpen)
            },
            onDismiss = {
                showUrlWarning = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Scan Result",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("result_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (scanItem != null && scanItem.id > 0) {
                        IconButton(
                            onClick = { onToggleFavorite(scanItem) },
                            modifier = Modifier.testTag("result_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (scanItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (scanItem.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = { ShareHelper.shareText(context, result.rawValue) },
                        modifier = Modifier.testTag("result_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Hero Type & Title Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = result.format,
                                color = color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = result.type.name.replace("_", " "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (result.subtitle.isNotBlank() && result.subtitle != result.title) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (scanItem != null && scanItem.timestamp > 0) {
                        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dateFormat.format(Date(scanItem.timestamp)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Smart Action depending on type
            Text(
                text = "Smart Actions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            when (result.type) {
                CodeType.URL -> {
                    Button(
                        onClick = {
                            val targetUrl = result.url ?: result.rawValue
                            pendingUrlToOpen = targetUrl
                            showUrlWarning = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_open_url"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Website", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CodeType.PHONE -> {
                    Button(
                        onClick = {
                            val phone = result.phone ?: result.rawValue
                            ShareHelper.dialPhone(context, phone)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_call"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Number", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CodeType.EMAIL -> {
                    Button(
                        onClick = {
                            val email = result.email ?: result.rawValue
                            ShareHelper.sendEmail(context, email)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_email"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Email", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CodeType.WIFI -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!result.wifiPassword.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    ShareHelper.copyToClipboard(context, result.wifiPassword, "Wi-Fi Password")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("result_action_wifi_password"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = color)
                            ) {
                                Icon(imageVector = Icons.Default.WifiPassword, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Wi-Fi Password (${result.wifiPassword})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                CodeType.CONTACT -> {
                    Button(
                        onClick = {
                            ShareHelper.insertContact(
                                context = context,
                                name = result.contactName ?: "New Contact",
                                phone = result.phone,
                                email = result.email,
                                org = result.contactOrg
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_save_contact"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Contacts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CodeType.SMS -> {
                    Button(
                        onClick = {
                            val phone = result.smsNumber ?: ""
                            val body = result.smsBody ?: ""
                            ShareHelper.sendSms(context, phone, body)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_send_sms"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send SMS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CodeType.LOCATION -> {
                    Button(
                        onClick = {
                            val lat = result.geoLat ?: 0.0
                            val lng = result.geoLng ?: 0.0
                            ShareHelper.openMaps(context, lat, lng, result.title)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_open_map"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in Maps", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CodeType.ID_CARD -> {
                    Button(
                        onClick = {
                            ShareHelper.copyToClipboard(context, result.rawValue, "ID Document Data")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_id_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy ID Card Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            val searchUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(result.rawValue, "UTF-8")}"
                            ShareHelper.openUrl(context, searchUrl)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_action_web_search"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search on Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Common secondary actions: Copy, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { ShareHelper.copyToClipboard(context, result.rawValue) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("result_copy_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                FilledTonalButton(
                    onClick = { ShareHelper.shareText(context, result.rawValue) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("result_share_button_row")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gemini AI Intelligence Section
            Text(
                text = "✨ Gemini AI Intelligence",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF7C3AED),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (isAiAnalyzing) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF7C3AED),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Analyzing content & security with Gemini...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (aiAnalysis != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Verdict & Category Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (verdictColor, verdictIcon, verdictText) = when (aiAnalysis.securityLevel) {
                                SecurityRating.SAFE -> Triple(Color(0xFF10B981), Icons.Default.CheckCircle, "SAFE CONTENT")
                                SecurityRating.CAUTION -> Triple(Color(0xFFF59E0B), Icons.Default.Warning, "PROCEED WITH CAUTION")
                                SecurityRating.DANGEROUS -> Triple(Color(0xFFEF4444), Icons.Default.GppBad, "SUSPICIOUS / RISK")
                                SecurityRating.INFO -> Triple(Color(0xFF6B7280), Icons.Default.HelpOutline, "ANALYZED")
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = verdictColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = verdictIcon,
                                        contentDescription = null,
                                        tint = verdictColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = verdictText,
                                        color = verdictColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = aiAnalysis.category,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Summary
                        Text(
                            text = aiAnalysis.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        // Key Insights
                        if (aiAnalysis.keyInsights.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Key Insights:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                for (insight in aiAnalysis.keyInsights) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("• ", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                        Text(text = insight, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // Security Details
                        if (aiAnalysis.securityDetails.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = aiAnalysis.securityDetails,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Button to trigger AI analysis
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF7C3AED).copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Analyze with Gemini AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF7C3AED)
                            )
                            Text(
                                text = "Check security risks & extract key insights",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onAnalyzeWithAi,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            modifier = Modifier.testTag("result_analyze_ai_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyze", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Raw Content Card
            Text(
                text = "Raw Content",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = result.rawValue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Delete action if from history
            if (scanItem != null && scanItem.id > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        onDelete(scanItem.id)
                        onBack()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_delete_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete from History")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
