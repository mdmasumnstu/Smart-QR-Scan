package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IdPhotoPreset(
    val name: String,
    val dimensionLabel: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val description: String
) {
    val aspectRatio: Float get() = widthRatio / heightRatio
}

val standardPresets = listOf(
    IdPhotoPreset("US Passport & Visa", "2 x 2 in (51x51mm)", 1f, 1f, "Standard US Passport, Visa, Green Card"),
    IdPhotoPreset("Schengen / EU / UK", "35 x 45 mm", 35f, 45f, "EU, UK, Schengen, Canada & Australia"),
    IdPhotoPreset("Driver's License / ID", "30 x 40 mm", 30f, 40f, "Standard State ID & Driver's License"),
    IdPhotoPreset("2 x 2.5 in", "50 x 64 mm", 2f, 2.5f, "Standard Corporate & Student Badges"),
    IdPhotoPreset("Stamp Size", "25 x 30 mm", 25f, 30f, "Small ID stamp photo format")
)

data class BackgroundColorOption(
    val name: String,
    val color: Color
)

val backgroundOptions = listOf(
    BackgroundColorOption("White", Color.White),
    BackgroundColorOption("Light Blue", Color(0xFFE0F2FE)),
    BackgroundColorOption("Light Gray", Color(0xFFF3F4F6)),
    BackgroundColorOption("Royal Blue", Color(0xFF1E3A8A)),
    BackgroundColorOption("Red", Color(0xFFDC2626)),
    BackgroundColorOption("Original", Color.Transparent)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdPhotoMakerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPreset by remember { mutableStateOf(standardPresets[0]) }
    var selectedBgColor by remember { mutableStateOf(backgroundOptions[0]) }
    var showGuides by remember { mutableStateOf(true) }

    var brightness by remember { mutableFloatStateOf(0f) } // -50 to 50
    var contrast by remember { mutableFloatStateOf(1f) } // 0.5 to 1.5
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Size, 1: Background, 2: Adjust, 3: Print Sheet
    var showPrintSheetPreview by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bmp != null) {
                    sourceBitmap = bmp
                    zoomScale = 1f
                    panOffset = Offset.Zero
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ID Photo Maker", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(selectedPreset.dimensionLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("id_photo_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("id_photo_pick_image_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Choose Photo")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Viewport Photo Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (sourceBitmap == null) {
                    // Empty Placeholder State
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color(0xFF334155), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Import Portrait Photo",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Choose a front-facing photo from your gallery to format into official passport, visa, or ID photo standards.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("id_photo_upload_button")
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Photo")
                        }
                    }
                } else {
                    // Interactive Canvas with Aspect Ratio Box & Guide Overlay
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxW = maxWidth
                        val maxH = maxHeight
                        val targetAspect = selectedPreset.aspectRatio

                        val (frameWidth, frameHeight) = if (maxW / maxH > targetAspect) {
                            Pair(maxH * targetAspect * 0.9f, maxH * 0.9f)
                        } else {
                            Pair(maxW * 0.9f, (maxW * 0.9f) / targetAspect)
                        }

                        Box(
                            modifier = Modifier
                                .size(width = frameWidth, height = frameHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(selectedBgColor.color)
                                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4f)
                                        panOffset += pan
                                    }
                                }
                        ) {
                            // Render user image with pan & zoom
                            val bmp = sourceBitmap!!
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "ID Portrait",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {}
                            )

                            // Biometric Face & Head Oval Guide Overlay
                            if (showGuides) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // Head Oval (Standard 70-80% of photo height)
                                    val ovalWidth = w * 0.58f
                                    val ovalHeight = h * 0.72f
                                    val ovalLeft = (w - ovalWidth) / 2f
                                    val ovalTop = h * 0.12f

                                    drawOval(
                                        color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                                        topLeft = Offset(ovalLeft, ovalTop),
                                        size = androidx.compose.ui.geometry.Size(ovalWidth, ovalHeight),
                                        style = Stroke(width = 2.dp.toPx())
                                    )

                                    // Eye Level Guide Line (around 55-60% from bottom of chin)
                                    val eyeY = ovalTop + ovalHeight * 0.42f
                                    drawLine(
                                        color = Color(0xFFFBBF24).copy(alpha = 0.7f),
                                        start = Offset(ovalLeft, eyeY),
                                        end = Offset(ovalLeft + ovalWidth, eyeY),
                                        strokeWidth = 1.5.dp.toPx()
                                    )

                                    // Crown Limit Guide Line
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.5f),
                                        start = Offset(w * 0.2f, ovalTop),
                                        end = Offset(w * 0.8f, ovalTop),
                                        strokeWidth = 1.dp.toPx()
                                    )

                                    // Chin Limit Guide Line
                                    val chinY = ovalTop + ovalHeight
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.5f),
                                        start = Offset(w * 0.2f, chinY),
                                        end = Offset(w * 0.8f, chinY),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Controls Tabs & Options Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Presets", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Background", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Adjust", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Brightness6, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Print Sheet", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Preset Selector
                                Text("Select Standard ID / Passport Dimension", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(standardPresets) { preset ->
                                        FilterChip(
                                            selected = selectedPreset == preset,
                                            onClick = { selectedPreset = preset },
                                            label = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(preset.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                    Text(preset.dimensionLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedPreset.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FilledTonalButton(
                                        onClick = { showGuides = !showGuides },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (showGuides) "Guides ON" else "Guides OFF", fontSize = 11.sp)
                                    }
                                }
                            }

                            1 -> {
                                // Background Color Options
                                Text("Official ID Background Color", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    backgroundOptions.forEach { opt ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { selectedBgColor = opt }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .background(
                                                        if (opt.color == Color.Transparent) Color.Gray.copy(alpha = 0.2f) else opt.color,
                                                        CircleShape
                                                    )
                                                    .border(
                                                        width = if (selectedBgColor == opt) 3.dp else 1.dp,
                                                        color = if (selectedBgColor == opt) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (selectedBgColor == opt) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (opt.color == Color.White) Color.Black else Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(opt.name, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // Adjustments: Brightness & Contrast
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Brightness", fontSize = 12.sp)
                                    Text("${brightness.toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -50f..50f
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Contrast", fontSize = 12.sp)
                                    Text("${(contrast * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0.5f..1.5f
                                )
                            }

                            3 -> {
                                // 4x6" Printable Sheet Info & Actions
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Printable 4x6 Inch Multi-Photo Grid", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Tiles multiple ID photos onto a standard 4x6\" sheet ready to print at any local pharmacy or photo printer.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { showPrintSheetPreview = true },
                                            enabled = sourceBitmap != null,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Print, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Preview Sheet", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom Actions: Save Single Photo & Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (sourceBitmap != null) {
                                        scope.launch {
                                            ShareHelper.shareBitmap(context, sourceBitmap!!, "My_ID_Photo")
                                        }
                                    } else {
                                        ShareHelper.showToast(context, "Please select a photo first")
                                    }
                                },
                                enabled = sourceBitmap != null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("id_photo_share_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (sourceBitmap != null) {
                                        scope.launch {
                                            ShareHelper.saveBitmapToGallery(context, sourceBitmap!!, "ID_Photo_${System.currentTimeMillis()}")
                                        }
                                    } else {
                                        ShareHelper.showToast(context, "Please select a photo first")
                                    }
                                },
                                enabled = sourceBitmap != null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("id_photo_save_button")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Photo", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Printable Sheet Preview Dialog
    if (showPrintSheetPreview && sourceBitmap != null) {
        Dialog(onDismissRequest = { showPrintSheetPreview = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("4x6\" Printable Photo Sheet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ready to print 4x6 inch (10x15cm) card", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4x6 Sheet Preview Card with tiled grid of photos
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f), // 6:4 = 1.5 ratio
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            // 2x3 Grid of ID Photos
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(2) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 72.dp, height = (72.dp / selectedPreset.aspectRatio))
                                                    .border(0.5.dp, Color.LightGray)
                                                    .background(selectedBgColor.color)
                                            ) {
                                                Image(
                                                    bitmap = sourceBitmap!!.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPrintSheetPreview = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    // Generate composite 4x6 sheet bitmap
                                    val sheet = generate4x6SheetBitmap(sourceBitmap!!, selectedPreset, selectedBgColor.color.toArgb())
                                    ShareHelper.saveBitmapToGallery(context, sheet, "ID_Sheet_4x6_${System.currentTimeMillis()}")
                                    showPrintSheetPreview = false
                                }
                            },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Sheet")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun generate4x6SheetBitmap(
    photo: Bitmap,
    preset: IdPhotoPreset,
    bgColor: Int
): Bitmap = withContext(Dispatchers.Default) {
    val sheetWidth = 1800 // 4x6 at 300 DPI = 1800 x 1200
    val sheetHeight = 1200
    val sheet = Bitmap.createBitmap(sheetWidth, sheetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(sheet)
    canvas.drawColor(android.graphics.Color.WHITE)

    val rows = 2
    val cols = 3
    val itemW = 460
    val itemH = (itemW / preset.aspectRatio).toInt().coerceAtMost(520)

    val totalGridW = cols * itemW
    val startX = (sheetWidth - totalGridW) / 2
    val totalGridH = rows * itemH
    val startY = (sheetHeight - totalGridH) / 2

    val bgPaint = Paint().apply { color = bgColor }
    val borderPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val x = startX + c * itemW
            val y = startY + r * itemH
            val destRect = RectF(x + 10f, y + 10f, x + itemW - 10f, y + itemH - 10f)

            canvas.drawRect(destRect, bgPaint)
            canvas.drawBitmap(photo, null, destRect, null)
            canvas.drawRect(destRect, borderPaint)
        }
    }

    sheet
}
