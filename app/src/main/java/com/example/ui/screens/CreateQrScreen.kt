package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CodeType
import com.example.util.QrDotStyle
import com.example.util.QrGenerator
import com.example.util.ShareHelper

enum class QrCreateType(val title: String, val icon: ImageVector, val codeType: CodeType) {
    URL("URL", Icons.Default.Language, CodeType.URL),
    TEXT("Text", Icons.Default.TextFields, CodeType.TEXT),
    WIFI("Wi-Fi", Icons.Default.Wifi, CodeType.WIFI),
    CONTACT("Contact", Icons.Default.Person, CodeType.CONTACT),
    PHONE("Phone", Icons.Default.Phone, CodeType.PHONE),
    EMAIL("Email", Icons.Default.Email, CodeType.EMAIL),
    SMS("SMS", Icons.Default.Sms, CodeType.SMS),
    LOCATION("Location", Icons.Default.LocationOn, CodeType.LOCATION),
    CALENDAR("Event", Icons.Default.CalendarMonth, CodeType.CALENDAR)
}

enum class CenterIconOption(val label: String, val icon: ImageVector?) {
    NONE("None", null),
    QR("QR", Icons.Default.QrCode),
    STAR("Star", Icons.Default.Star),
    HEART("Heart", Icons.Default.Favorite),
    LINK("Link", Icons.Default.Link),
    WIFI("Wi-Fi", Icons.Default.Wifi),
    LOCATION("Pin", Icons.Default.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQrScreen(
    onSaveToHistory: (content: String, format: String, type: CodeType, title: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(QrCreateType.URL) }

    // Input fields for various types
    var urlText by remember { mutableStateOf("https://") }
    var plainText by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }

    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactCompany by remember { mutableStateOf("") }

    var phoneNumber by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }

    var smsNumber by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }

    var geoLat by remember { mutableStateOf("") }
    var geoLng by remember { mutableStateOf("") }

    var eventTitle by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }

    // Customization state
    var qrColor by remember { mutableStateOf(Color.Black) }
    var bgColor by remember { mutableStateOf(Color.White) }
    var dotStyle by remember { mutableStateOf(QrDotStyle.SQUARE) }
    var selectedCenterIcon by remember { mutableStateOf(CenterIconOption.NONE) }

    // Generated bitmap
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrContentString by remember { mutableStateOf("") }

    // Color presets
    val qrColorPresets = listOf(
        Color.Black,
        Color(0xFF0066FF),
        Color(0xFF10B981),
        Color(0xFF8B5CF6),
        Color(0xFFDC2626),
        Color(0xFFEA580C),
        Color(0xFF0284C7)
    )

    val bgColorPresets = listOf(
        Color.White,
        Color(0xFFF1F5F9),
        Color(0xFFFEF3C7),
        Color(0xFFEFF6FF),
        Color(0xFFF3E8FF)
    )

    // Compute encoded content
    val computedContent = remember(
        selectedType, urlText, plainText, wifiSsid, wifiPassword, wifiSecurity,
        contactName, contactPhone, contactEmail, contactCompany, phoneNumber,
        emailAddress, emailSubject, emailBody, smsNumber, smsMessage,
        geoLat, geoLng, eventTitle, eventLocation
    ) {
        when (selectedType) {
            QrCreateType.URL -> urlText.trim()
            QrCreateType.TEXT -> plainText.trim()
            QrCreateType.WIFI -> "WIFI:T:$wifiSecurity;S:$wifiSsid;P:$wifiPassword;;"
            QrCreateType.CONTACT -> {
                buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("FN:$contactName")
                    if (contactCompany.isNotBlank()) appendLine("ORG:$contactCompany")
                    if (contactPhone.isNotBlank()) appendLine("TEL;TYPE=CELL:$contactPhone")
                    if (contactEmail.isNotBlank()) appendLine("EMAIL:$contactEmail")
                    appendLine("END:VCARD")
                }.trim()
            }
            QrCreateType.PHONE -> "tel:$phoneNumber"
            QrCreateType.EMAIL -> "mailto:$emailAddress?subject=$emailSubject&body=$emailBody"
            QrCreateType.SMS -> "smsto:$smsNumber:$smsMessage"
            QrCreateType.LOCATION -> "geo:${geoLat.trim()},${geoLng.trim()}"
            QrCreateType.CALENDAR -> {
                buildString {
                    appendLine("BEGIN:VEVENT")
                    appendLine("SUMMARY:$eventTitle")
                    if (eventLocation.isNotBlank()) appendLine("LOCATION:$eventLocation")
                    appendLine("END:VEVENT")
                }.trim()
            }
        }
    }

    // Generate bitmap when inputs or styles change
    LaunchedEffect(computedContent, qrColor, bgColor, dotStyle, selectedCenterIcon) {
        qrContentString = computedContent
        if (computedContent.isNotBlank() && computedContent != "https://") {
            val centerLogoBitmap = if (selectedCenterIcon.icon != null) {
                createSimpleIconBitmap(selectedCenterIcon.label, qrColor.toArgb())
            } else null

            generatedBitmap = QrGenerator.generateQrBitmap(
                content = computedContent,
                size = 700,
                foregroundColor = qrColor.toArgb(),
                backgroundColor = bgColor.toArgb(),
                dotStyle = dotStyle,
                centerIcon = centerLogoBitmap
            )
        } else {
            generatedBitmap = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Create QR Code",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("create_qr_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Type Selector Tabs (Scrollable)
            ScrollableTabRow(
                selectedTabIndex = selectedType.ordinal,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("create_qr_type_tabs")
            ) {
                QrCreateType.values().forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(type.title)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Inputs based on type
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (selectedType) {
                        QrCreateType.URL -> {
                            OutlinedTextField(
                                value = urlText,
                                onValueChange = { urlText = it },
                                label = { Text("Website URL") },
                                placeholder = { Text("https://example.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Language, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (urlText.isNotEmpty() && urlText != "https://") {
                                        IconButton(onClick = { urlText = "https://" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_url")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("https://", ".com", ".org", ".io", "www.").forEach { chipText ->
                                    androidx.compose.material3.SuggestionChip(
                                        onClick = {
                                            if (chipText == "https://") {
                                                if (!urlText.startsWith("https://")) {
                                                    urlText = "https://$urlText"
                                                }
                                            } else {
                                                urlText += chipText
                                            }
                                        },
                                        label = { Text(chipText, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                        QrCreateType.TEXT -> {
                            OutlinedTextField(
                                value = plainText,
                                onValueChange = { plainText = it },
                                label = { Text("Plain Text") },
                                placeholder = { Text("Enter text to encode...") },
                                leadingIcon = {
                                    Icon(Icons.Default.TextFields, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (plainText.isNotEmpty()) {
                                        IconButton(onClick = { plainText = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                minLines = 3,
                                maxLines = 6,
                                supportingText = {
                                    Text("${plainText.length} characters")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_plain_text")
                            )
                        }
                        QrCreateType.WIFI -> {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("Network Name (SSID)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_wifi_ssid")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("Wi-Fi Password") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_wifi_password")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("WPA", "WEP", "None").forEach { sec ->
                                    FilterChip(
                                        selected = wifiSecurity == sec,
                                        onClick = { wifiSecurity = sec },
                                        label = { Text(sec) }
                                    )
                                }
                            }
                        }
                        QrCreateType.CONTACT -> {
                            OutlinedTextField(
                                value = contactName,
                                onValueChange = { contactName = it },
                                label = { Text("Full Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_contact_name")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = { Text("Phone Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = contactEmail,
                                onValueChange = { contactEmail = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = contactCompany,
                                onValueChange = { contactCompany = it },
                                label = { Text("Company / Organization") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrCreateType.PHONE -> {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("+1234567890") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_phone")
                            )
                        }
                        QrCreateType.EMAIL -> {
                            OutlinedTextField(
                                value = emailAddress,
                                onValueChange = { emailAddress = it },
                                label = { Text("Recipient Email") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_email")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = emailSubject,
                                onValueChange = { emailSubject = it },
                                label = { Text("Subject (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = emailBody,
                                onValueChange = { emailBody = it },
                                label = { Text("Message Body") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrCreateType.SMS -> {
                            OutlinedTextField(
                                value = smsNumber,
                                onValueChange = { smsNumber = it },
                                label = { Text("Recipient Number") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sms_number")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = smsMessage,
                                onValueChange = { smsMessage = it },
                                label = { Text("SMS Message") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrCreateType.LOCATION -> {
                            OutlinedTextField(
                                value = geoLat,
                                onValueChange = { geoLat = it },
                                label = { Text("Latitude") },
                                placeholder = { Text("37.7749") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_lat")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = geoLng,
                                onValueChange = { geoLng = it },
                                label = { Text("Longitude") },
                                placeholder = { Text("-122.4194") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_lng")
                            )
                        }
                        QrCreateType.CALENDAR -> {
                            OutlinedTextField(
                                value = eventTitle,
                                onValueChange = { eventTitle = it },
                                label = { Text("Event Title") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_event_title")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = eventLocation,
                                onValueChange = { eventLocation = it },
                                label = { Text("Event Location") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Customization Options
            Text(
                text = "Customize QR Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dot Style (Square vs Rounded)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = dotStyle == QrDotStyle.SQUARE,
                    onClick = { dotStyle = QrDotStyle.SQUARE },
                    label = { Text("Square Dots") },
                    leadingIcon = if (dotStyle == QrDotStyle.SQUARE) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = dotStyle == QrDotStyle.ROUNDED,
                    onClick = { dotStyle = QrDotStyle.ROUNDED },
                    label = { Text("Rounded Dots") },
                    leadingIcon = if (dotStyle == QrDotStyle.ROUNDED) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // QR Code Color
            Text(
                text = "QR Color",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                qrColorPresets.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { qrColor = color }
                            .border(
                                width = if (qrColor == color) 3.dp else 1.dp,
                                color = if (qrColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrColor == color) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (color == Color.White) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Background Color
            Text(
                text = "Background Color",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                bgColorPresets.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { bgColor = color }
                            .border(
                                width = if (bgColor == color) 3.dp else 1.dp,
                                color = if (bgColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bgColor == color) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Logo Option
            Text(
                text = "Center Icon",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CenterIconOption.values().forEach { option ->
                    FilterChip(
                        selected = selectedCenterIcon == option,
                        onClick = { selectedCenterIcon = option },
                        label = { Text(option.label) },
                        leadingIcon = option.icon?.let { icon ->
                            { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live Preview Card
            Text(
                text = "QR Code Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_qr_preview_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (generatedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = generatedBitmap!!.asImageBitmap(),
                                contentDescription = "Generated QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action buttons: Save to Gallery & Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val bmp = generatedBitmap
                                    if (bmp != null) {
                                        val saved = ShareHelper.saveBitmapToGallery(context, bmp, "QR_${selectedType.title}")
                                        if (saved) {
                                            onSaveToHistory(
                                                qrContentString,
                                                "QR Code",
                                                selectedType.codeType,
                                                "Generated ${selectedType.title}"
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("save_qr_gallery_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Image")
                            }

                            OutlinedButton(
                                onClick = {
                                    val bmp = generatedBitmap
                                    if (bmp != null) {
                                        ShareHelper.shareBitmap(context, bmp)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_qr_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Enter content to generate",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun createSimpleIconBitmap(label: String, color: Int): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText(label.take(2).uppercase(), size / 2f, size / 2f + 14f, paint)
    return bitmap
}
