package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.IdCardData
import com.example.ui.components.BatchReviewDialog
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.IdCardResultDialog
import com.example.ui.components.ScanMode
import com.example.ui.components.ScannerOverlay
import com.example.util.ParsedBarcodeResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    isTorchOn: Boolean,
    isFrontCamera: Boolean,
    zoomRatio: Float,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onBarcodeDetected: (Barcode) -> Unit,
    onImageSelected: (Bitmap) -> Unit,
    onBack: () -> Unit,
    scanMode: ScanMode = ScanMode.SINGLE,
    onScanModeChange: (ScanMode) -> Unit = {},
    batchItems: List<ParsedBarcodeResult> = emptyList(),
    lastBatchFeedback: String? = null,
    onDeleteBatchItem: (Int) -> Unit = {},
    onSaveBatchAll: () -> Unit = {},
    onClearBatch: () -> Unit = {},
    onCaptureIdCardBitmap: (Bitmap) -> Unit = {},
    activeIdCard: IdCardData? = null,
    isIdCardAnalyzing: Boolean = false,
    onSaveIdCard: (IdCardData) -> Unit = {},
    onDismissIdCard: () -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var showBatchDialog by remember { mutableStateOf(false) }
    var captureFrameProvider by remember { mutableStateOf<(() -> Bitmap?)?>(null) }
    var highlightTrigger by remember { mutableStateOf(0L) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    if (scanMode == ScanMode.ID_CARD) {
                        onCaptureIdCardBitmap(bitmap)
                    } else {
                        onImageSelected(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Live CameraX Feed
            CameraPreviewView(
                isTorchOn = isTorchOn,
                isFrontCamera = isFrontCamera,
                zoomRatio = zoomRatio,
                onBarcodeDetected = { barcode ->
                    highlightTrigger = System.currentTimeMillis()
                    try {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onBarcodeDetected(barcode)
                },
                isBatchMode = scanMode == ScanMode.BATCH,
                onCaptureReady = { provider ->
                    captureFrameProvider = provider
                }
            )

            // Scanning Overlay with corners, laser line, zoom slider, mode selector, batch badge
            ScannerOverlay(
                isTorchOn = isTorchOn,
                onToggleTorch = onToggleTorch,
                onSwitchCamera = onSwitchCamera,
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onBack = onBack,
                zoomRatio = zoomRatio,
                onZoomChange = onZoomChange,
                scanMode = scanMode,
                onScanModeChange = onScanModeChange,
                batchCount = batchItems.size,
                onOpenBatchReview = { showBatchDialog = true },
                lastScannedFeedback = lastBatchFeedback,
                highlightTrigger = highlightTrigger,
                onCaptureIdCard = {
                    val frameBmp = captureFrameProvider?.invoke()
                    if (frameBmp != null) {
                        onCaptureIdCardBitmap(frameBmp)
                    } else {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            )

            // ID Card AI Analyzing Loading Overlay
            if (isIdCardAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analyzing ID Card with AI...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Extracting document type, name, ID number, and dates",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Batch Review Dialog
            if (showBatchDialog) {
                BatchReviewDialog(
                    items = batchItems,
                    onDismiss = { showBatchDialog = false },
                    onDeleteItem = { index -> onDeleteBatchItem(index) },
                    onSaveAll = { onSaveBatchAll() },
                    onClearAll = { onClearBatch() }
                )
            }

            // ID Card Result Dialog
            if (activeIdCard != null) {
                IdCardResultDialog(
                    idCardData = activeIdCard,
                    onDismiss = onDismissIdCard,
                    onSaveToHistory = onSaveIdCard
                )
            }
        } else {
            // Permission Request Fallback State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF1E293B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Camera Permission Needed",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To scan QR codes, barcodes, and ID cards with your device camera in real-time, please grant camera permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Permission")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("scan_from_gallery_fallback_button")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                    Text(" Scan from Gallery", color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .testTag("scanner_denied_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
