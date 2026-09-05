package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppDrawer
import com.example.ui.components.ScanMode
import com.example.ui.navigation.AppScreen
import com.example.ui.screens.CreateQrScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IdPhotoMakerScreen
import com.example.ui.screens.MyQrScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val recentScans by viewModel.recentScans.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val favoriteScans by viewModel.favoriteScans.collectAsState()
    val historySearchQuery by viewModel.historySearchQuery.collectAsState()

    val activeResult by viewModel.activeResult.collectAsState()
    val activeScanItem by viewModel.activeScanItem.collectAsState()

    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()

    val scanMode by viewModel.scanMode.collectAsState()
    val batchItems by viewModel.batchItems.collectAsState()
    val lastBatchFeedback by viewModel.lastBatchFeedback.collectAsState()

    val activeIdCard by viewModel.activeIdCard.collectAsState()
    val isIdCardAnalyzing by viewModel.isIdCardAnalyzing.collectAsState()

    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAiAnalyzing by viewModel.isAiAnalyzing.collectAsState()

    val scanErrorMessage by viewModel.scanErrorMessage.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()

    // Handle back button
    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.HOME) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateBack()
        }
    }

    // Error alert (e.g. image without QR code)
    if (scanErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text("Scan Failed") },
            text = { Text(scanErrorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text("OK")
                }
            }
        )
    }

    val showBottomBar = currentScreen in listOf(
        AppScreen.HOME,
        AppScreen.HISTORY,
        AppScreen.FAVORITES,
        AppScreen.MY_QR
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    viewModel.navigateTo(screen)
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    AppBottomBar(
                        currentScreen = currentScreen,
                        onNavigate = { screen -> viewModel.navigateTo(screen) }
                    )
                }
            },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else Dp(0f))
            ) {
                Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                    when (screen) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                recentScans = recentScans,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenScanItem = { viewModel.openSavedItem(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onImageSelected = { bitmap -> viewModel.scanImageBitmap(bitmap) },
                                onStartBatchScan = {
                                    viewModel.setScanMode(ScanMode.BATCH)
                                    viewModel.navigateTo(AppScreen.SCANNER)
                                },
                                onStartIdScan = {
                                    viewModel.setScanMode(ScanMode.ID_CARD)
                                    viewModel.navigateTo(AppScreen.SCANNER)
                                }
                            )
                        }

                        AppScreen.SCANNER -> {
                            ScannerScreen(
                                isTorchOn = isTorchOn,
                                isFrontCamera = isFrontCamera,
                                zoomRatio = zoomRatio,
                                onToggleTorch = { viewModel.toggleTorch() },
                                onSwitchCamera = { viewModel.switchCamera() },
                                onZoomChange = { viewModel.setZoomRatio(it) },
                                onBarcodeDetected = { barcode -> viewModel.onBarcodeDetected(barcode) },
                                onImageSelected = { bitmap -> viewModel.scanImageBitmap(bitmap) },
                                onBack = { viewModel.navigateBack() },
                                scanMode = scanMode,
                                onScanModeChange = { viewModel.setScanMode(it) },
                                batchItems = batchItems,
                                lastBatchFeedback = lastBatchFeedback,
                                onDeleteBatchItem = { viewModel.removeBatchItem(it) },
                                onSaveBatchAll = { viewModel.saveAllBatchToHistory() },
                                onClearBatch = { viewModel.clearBatch() },
                                onCaptureIdCardBitmap = { viewModel.analyzeIdCardBitmap(it) },
                                activeIdCard = activeIdCard,
                                isIdCardAnalyzing = isIdCardAnalyzing,
                                onSaveIdCard = { viewModel.saveIdCardToHistory(it) },
                                onDismissIdCard = { viewModel.clearActiveIdCard() }
                            )
                        }

                        AppScreen.RESULT_DETAIL -> {
                            if (activeResult != null) {
                                ResultScreen(
                                    result = activeResult!!,
                                    scanItem = activeScanItem,
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onDelete = { viewModel.deleteScan(it) },
                                    onBack = { viewModel.navigateBack() },
                                    aiAnalysis = aiAnalysis,
                                    isAiAnalyzing = isAiAnalyzing,
                                    onAnalyzeWithAi = { viewModel.analyzeActiveResultWithAi() }
                                )
                            } else {
                                viewModel.navigateTo(AppScreen.HOME)
                            }
                        }

                        AppScreen.CREATE_QR -> {
                            CreateQrScreen(
                                onSaveToHistory = { content, format, type, title ->
                                    viewModel.saveCreatedQr(content, format, type, title)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.ID_PHOTO_MAKER -> {
                            IdPhotoMakerScreen(
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.HISTORY -> {
                            HistoryScreen(
                                items = historyItems,
                                searchQuery = historySearchQuery,
                                onSearchChange = { viewModel.setHistorySearchQuery(it) },
                                onOpenItem = { viewModel.openSavedItem(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onDeleteItem = { viewModel.deleteScan(it) },
                                onRestoreItem = { viewModel.restoreScan(it) },
                                onClearAll = { viewModel.clearAllHistory() },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.FAVORITES -> {
                            FavoritesScreen(
                                items = favoriteScans,
                                onOpenItem = { viewModel.openSavedItem(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onDeleteItem = { viewModel.deleteScan(it) },
                                onRestoreItem = { viewModel.restoreScan(it) },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.MY_QR -> {
                            MyQrScreen(
                                profile = myProfile,
                                onSaveProfile = { profile ->
                                    viewModel.settingsManager.saveMyProfile(profile)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                settingsManager = viewModel.settingsManager,
                                onClearAllHistory = { viewModel.clearAllHistory() },
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
