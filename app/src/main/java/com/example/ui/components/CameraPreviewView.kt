package com.example.ui.components

import android.annotation.SuppressLint
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreviewView(
    isTorchOn: Boolean,
    isFrontCamera: Boolean,
    zoomRatio: Float,
    onBarcodeDetected: (Barcode) -> Unit,
    isBatchMode: Boolean = false,
    onCaptureReady: ((() -> android.graphics.Bitmap?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    // Debounce to prevent multiple immediate triggers
    var isProcessing by remember { mutableStateOf(false) }
    var lastScanTimestamp by remember { mutableStateOf(0L) }

    LaunchedEffect(previewView) {
        previewView?.let { pv ->
            onCaptureReady?.invoke { pv.bitmap }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    LaunchedEffect(isTorchOn, camera) {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            try {
                camera?.cameraControl?.enableTorch(isTorchOn)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(zoomRatio, camera) {
        try {
            camera?.cameraControl?.setLinearZoom(zoomRatio.coerceIn(0f, 1f))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(previewView, isFrontCamera, lifecycleOwner) {
        val targetView = previewView ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = targetView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    val currentTime = System.currentTimeMillis()
                    val canProcess = if (isBatchMode) {
                        currentTime - lastScanTimestamp > 1400L
                    } else {
                        !isProcessing
                    }

                    if (mediaImage != null && canProcess) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val firstBarcode = barcodes.firstOrNull()
                                if (firstBarcode != null) {
                                    if (isBatchMode) {
                                        if (currentTime - lastScanTimestamp > 1400L) {
                                            lastScanTimestamp = currentTime
                                            onBarcodeDetected(firstBarcode)
                                        }
                                    } else if (!isProcessing) {
                                        isProcessing = true
                                        onBarcodeDetected(firstBarcode)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .testTag("camera_preview_view"),
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView = this
            }
        },
        update = { updatedView ->
            previewView = updatedView
        }
    )
}
