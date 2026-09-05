package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LaserRed

enum class ScanMode {
    SINGLE,
    BATCH,
    ID_CARD
}

@Composable
fun ScannerOverlay(
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onPickImage: () -> Unit,
    onBack: () -> Unit,
    zoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    scanMode: ScanMode = ScanMode.SINGLE,
    onScanModeChange: (ScanMode) -> Unit = {},
    batchCount: Int = 0,
    onOpenBatchReview: () -> Unit = {},
    onCaptureIdCard: () -> Unit = {},
    lastScannedFeedback: String? = null,
    highlightTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val highlightAnim = remember { Animatable(0f) }

    LaunchedEffect(highlightTrigger) {
        if (highlightTrigger > 0L) {
            highlightAnim.snapTo(1f)
            highlightAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
    }
    val highlightProgress = highlightAnim.value

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_fraction"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()

        // Size of scanning frame depending on Mode
        val isIdCard = scanMode == ScanMode.ID_CARD
        val boxWidth = if (isIdCard) totalWidth * 0.88f else totalWidth * 0.76f
        val boxHeight = if (isIdCard) boxWidth / 1.586f else boxWidth * 0.95f

        val left = (totalWidth - boxWidth) / 2f
        val top = (totalHeight - boxHeight) / 2f - 40f
        val right = left + boxWidth
        val bottom = top + boxHeight

        val baseCornerLength = 32.dp.value * 2.5f
        val baseCornerStrokeWidth = 4.dp.value * 2.5f
        val baseCornerColor = if (isIdCard) Color(0xFF00E676) else Color(0xFF2196F3)

        val cornerColor = if (highlightProgress > 0f) {
            lerp(baseCornerColor, Color(0xFF00E676), highlightProgress)
        } else {
            baseCornerColor
        }
        val cornerLength = baseCornerLength + (6.dp.value * 2.5f * highlightProgress)
        val cornerStrokeWidth = baseCornerStrokeWidth + (2.dp.value * 2.5f * highlightProgress)

        // Draw Darkened Scrim, Corner Indicators, and Laser
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                )
            }
            drawPath(
                path = path,
                color = Color(0x99000000)
            )

            // Inner thin guide box
            drawRoundRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Visual Detection Animation: Glowing Frame Highlight
            if (highlightProgress > 0f) {
                // Outer diffused glow aura
                drawRoundRect(
                    color = Color(0xFF00E676).copy(alpha = 0.35f * highlightProgress),
                    topLeft = Offset(left - 6.dp.toPx(), top - 6.dp.toPx()),
                    size = Size(boxWidth + 12.dp.toPx(), boxHeight + 12.dp.toPx()),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx() * highlightProgress)
                )

                // Crisp illuminated frame border
                drawRoundRect(
                    color = Color(0xFF10B981).copy(alpha = 0.95f * highlightProgress),
                    topLeft = Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
                    size = Size(boxWidth + 4.dp.toPx(), boxHeight + 4.dp.toPx()),
                    cornerRadius = CornerRadius(17.dp.toPx(), 17.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx() + (3.dp.toPx() * highlightProgress))
                )

                // Subtle interior tint wash
                drawRoundRect(
                    color = Color(0xFF10B981).copy(alpha = 0.14f * highlightProgress),
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )

                // Center animated success checkmark badge
                val centerX = left + boxWidth / 2f
                val centerY = top + boxHeight / 2f
                val badgeScale = 0.85f + (0.2f * highlightProgress)
                val badgeRadius = 26.dp.toPx() * badgeScale

                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.92f * highlightProgress),
                    radius = badgeRadius,
                    center = Offset(centerX, centerY)
                )

                val checkPath = Path().apply {
                    moveTo(centerX - 10.dp.toPx() * badgeScale, centerY)
                    lineTo(centerX - 3.dp.toPx() * badgeScale, centerY + 7.dp.toPx() * badgeScale)
                    lineTo(centerX + 11.dp.toPx() * badgeScale, centerY - 7.dp.toPx() * badgeScale)
                }
                drawPath(
                    path = checkPath,
                    color = Color.White.copy(alpha = highlightProgress),
                    style = Stroke(width = 3.dp.toPx() * badgeScale, cap = StrokeCap.Round)
                )
            }

            // ID Card Special Guides (Photo Box & Text Lines)
            if (isIdCard) {
                // Photo Placeholder box on left of ID card
                val photoW = boxWidth * 0.28f
                val photoH = photoW * 1.25f
                val photoL = left + 16.dp.toPx()
                val photoT = top + (boxHeight - photoH) / 2f

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(photoL, photoT),
                    size = Size(photoW, photoH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Text placeholder lines on right
                val lineStart = photoL + photoW + 16.dp.toPx()
                val lineEnd = right - 16.dp.toPx()
                val lineSpacing = 16.dp.toPx()
                var currentY = photoT + 12.dp.toPx()

                repeat(4) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(lineStart, currentY),
                        end = Offset(lineEnd, currentY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    currentY += lineSpacing
                }
            }

            // Corner Brackets
            // Top-Left
            drawLine(
                color = cornerColor,
                start = Offset(left, top + cornerLength),
                end = Offset(left, top),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )

            // Top-Right
            drawLine(
                color = cornerColor,
                start = Offset(right - cornerLength, top),
                end = Offset(right, top),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, top),
                end = Offset(right, top + cornerLength),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Left
            drawLine(
                color = cornerColor,
                start = Offset(left, bottom - cornerLength),
                end = Offset(left, bottom),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left, bottom),
                end = Offset(left + cornerLength, bottom),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Right
            drawLine(
                color = cornerColor,
                start = Offset(right - cornerLength, bottom),
                end = Offset(right, bottom),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, bottom),
                end = Offset(right, bottom - cornerLength),
                strokeWidth = cornerStrokeWidth,
                cap = StrokeCap.Round
            )

            // Sweeping Laser
            val laserY = top + (bottom - top) * laserFraction
            val activeLaserColor = if (highlightProgress > 0f) {
                lerp(LaserRed, Color(0xFF00E676), highlightProgress)
            } else {
                LaserRed
            }

            val laserGradient = Brush.verticalGradient(
                colors = listOf(
                    activeLaserColor.copy(alpha = 0f),
                    activeLaserColor.copy(alpha = 0.25f),
                    activeLaserColor.copy(alpha = 0.85f),
                    Color.White
                ),
                startY = laserY - 14.dp.toPx(),
                endY = laserY
            )

            drawRect(
                brush = laserGradient,
                topLeft = Offset(left + 4.dp.toPx(), laserY - 14.dp.toPx()),
                size = Size(boxWidth - 8.dp.toPx(), 14.dp.toPx())
            )

            drawLine(
                color = activeLaserColor,
                start = Offset(left + 6.dp.toPx(), laserY),
                end = Offset(right - 6.dp.toPx(), laserY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Top Navigation & Action Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .testTag("scanner_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Batch Badge Pill if in Batch mode
            if (scanMode == ScanMode.BATCH) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2563EB).copy(alpha = 0.9f),
                    modifier = Modifier.clickable { onOpenBatchReview() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Batch: $batchCount",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("scanner_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Scan Image",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .background(
                            if (isTorchOn) Color(0xFFF59E0B).copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f),
                            CircleShape
                        )
                        .testTag("scanner_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flashlight",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("scanner_flip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Live Scanning Feedback Toast (animated popup when item scanned in batch)
        AnimatedVisibility(
            visible = lastScannedFeedback != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 105.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981).copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scanned: ${lastScannedFeedback ?: ""}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instruction text
            Text(
                text = when (scanMode) {
                    ScanMode.SINGLE -> "Align QR code or barcode inside frame"
                    ScanMode.BATCH -> "Batch Mode: Scan multiple codes consecutively"
                    ScanMode.ID_CARD -> "Align ID Card or Driver's License inside frame"
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Mode Selector Segmented Chips: [ Single | Batch | ID Card ]
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModeChip(
                    text = "Single",
                    selected = scanMode == ScanMode.SINGLE,
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { onScanModeChange(ScanMode.SINGLE) }
                )
                ModeChip(
                    text = if (batchCount > 0) "Batch ($batchCount)" else "Batch",
                    selected = scanMode == ScanMode.BATCH,
                    icon = Icons.Default.Layers,
                    onClick = { onScanModeChange(ScanMode.BATCH) }
                )
                ModeChip(
                    text = "ID Card",
                    selected = scanMode == ScanMode.ID_CARD,
                    icon = Icons.Default.Badge,
                    onClick = { onScanModeChange(ScanMode.ID_CARD) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Zoom slider & Mode-specific action
            if (scanMode == ScanMode.BATCH) {
                // Batch Review button
                Button(
                    onClick = onOpenBatchReview,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(46.dp)
                        .testTag("scanner_review_batch_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review Batch ($batchCount)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (scanMode == ScanMode.ID_CARD) {
                // ID Card Capture button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCaptureIdCard,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp)
                            .testTag("scanner_capture_id_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture & Analyze ID", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Zoom control slider (ZoomOut icon, Slider, ZoomIn icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )

                Slider(
                    value = zoomRatio,
                    onValueChange = onZoomChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scanner_zoom_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF2196F3),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom In",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    text: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.White else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.Black else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.Black else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
