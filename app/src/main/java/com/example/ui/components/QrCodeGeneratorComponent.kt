package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CodeType
import com.example.util.QrDotStyle
import com.example.util.QrGenerator
import com.example.util.ShareHelper

/**
 * Input mode for the QR Code Generator Component.
 */
enum class QrInputMode(val label: String) {
    URL("Website URL"),
    TEXT("Plain Text")
}

/**
 * Reusable Material 3 UI component allowing users to input text or URLs
 * and generate a customized QR code image using the ZXing library.
 */
@Composable
fun QrCodeGeneratorComponent(
    modifier: Modifier = Modifier,
    initialMode: QrInputMode = QrInputMode.URL,
    initialUrl: String = "https://",
    initialText: String = "",
    onSaveToHistory: ((content: String, format: String, type: CodeType, title: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var inputMode by remember { mutableStateOf(initialMode) }
    var urlInput by remember { mutableStateOf(initialUrl) }
    var textInput by remember { mutableStateOf(initialText) }

    // Customization styling
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedDotStyle by remember { mutableStateOf(QrDotStyle.SQUARE) }

    // Generated QR state
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentContent by remember { mutableStateOf("") }

    val colorOptions = listOf(
        Color.Black to "Black",
        Color(0xFF0066FF) to "Blue",
        Color(0xFF7C3AED) to "Purple",
        Color(0xFF059669) to "Emerald",
        Color(0xFFDC2626) to "Red",
        Color(0xFFD97706) to "Amber"
    )

    // Compute active raw string
    val activeString = remember(inputMode, urlInput, textInput) {
        when (inputMode) {
            QrInputMode.URL -> urlInput.trim()
            QrInputMode.TEXT -> textInput.trim()
        }
    }

    // Generate QR bitmap whenever input or styling changes
    LaunchedEffect(activeString, selectedColor, selectedDotStyle, inputMode) {
        val isValid = when (inputMode) {
            QrInputMode.URL -> activeString.isNotBlank() && activeString != "https://" && activeString != "http://"
            QrInputMode.TEXT -> activeString.isNotBlank()
        }

        if (isValid) {
            currentContent = activeString
            generatedBitmap = QrGenerator.generateQrBitmap(
                content = activeString,
                size = 700,
                foregroundColor = selectedColor.toArgb(),
                backgroundColor = android.graphics.Color.WHITE,
                dotStyle = selectedDotStyle
            )
        } else {
            currentContent = ""
            generatedBitmap = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_code_generator_component")
    ) {
        // Mode Selector: URL vs Plain Text
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Input Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = inputMode == QrInputMode.URL,
                        onClick = { inputMode = QrInputMode.URL },
                        label = { Text("Website URL") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_mode_url")
                    )

                    FilterChip(
                        selected = inputMode == QrInputMode.TEXT,
                        onClick = { inputMode = QrInputMode.TEXT },
                        label = { Text("Plain Text") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_mode_text")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Field depending on active mode
                when (inputMode) {
                    QrInputMode.URL -> {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Enter Website URL") },
                            placeholder = { Text("https://example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Language, contentDescription = null)
                            },
                            trailingIcon = {
                                if (urlInput.isNotEmpty() && urlInput != "https://") {
                                    IconButton(onClick = { urlInput = "https://" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_input_url_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick URL suggestion chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("https://", ".com", ".org", ".io", "www.").forEach { chipText ->
                                SuggestionChip(
                                    onClick = {
                                        if (chipText == "https://") {
                                            if (!urlInput.startsWith("https://")) {
                                                urlInput = "https://$urlInput"
                                            }
                                        } else {
                                            urlInput += chipText
                                        }
                                    },
                                    label = { Text(chipText, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    QrInputMode.TEXT -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Enter Text to Encode") },
                            placeholder = { Text("Type any text, message, or note...") },
                            leadingIcon = {
                                Icon(Icons.Default.TextFields, contentDescription = null)
                            },
                            trailingIcon = {
                                if (textInput.isNotEmpty()) {
                                    IconButton(onClick = { textInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            minLines = 3,
                            maxLines = 6,
                            supportingText = {
                                Text("${textInput.length} characters")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_input_text_field")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customization Options: Color & Dot Style
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "QR Style & Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dot Style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedDotStyle == QrDotStyle.SQUARE,
                        onClick = { selectedDotStyle = QrDotStyle.SQUARE },
                        label = { Text("Classic Square") },
                        leadingIcon = if (selectedDotStyle == QrDotStyle.SQUARE) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedDotStyle == QrDotStyle.ROUNDED,
                        onClick = { selectedDotStyle = QrDotStyle.ROUNDED },
                        label = { Text("Modern Rounded") },
                        leadingIcon = if (selectedDotStyle == QrDotStyle.ROUNDED) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Palette
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { (color, _) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QR Code Preview & Action Buttons
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("qr_preview_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (generatedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Generated ZXing QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("qr_code_image_preview")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Generated with ZXing Engine",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val bmp = generatedBitmap
                                if (bmp != null) {
                                    val title = if (inputMode == QrInputMode.URL) "QR_URL" else "QR_Text"
                                    val saved = ShareHelper.saveBitmapToGallery(context, bmp, title)
                                    if (saved) {
                                        Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                        onSaveToHistory?.invoke(
                                            currentContent,
                                            "QR_CODE",
                                            if (inputMode == QrInputMode.URL) CodeType.URL else CodeType.TEXT,
                                            if (inputMode == QrInputMode.URL) "URL QR Code" else "Text QR Code"
                                        )
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("qr_save_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
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
                                .testTag("qr_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentContent))
                            Toast.makeText(context, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("qr_copy_content_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Raw Content")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (inputMode == QrInputMode.URL) {
                                    "Enter a URL above to generate QR code"
                                } else {
                                    "Enter text above to generate QR code"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
