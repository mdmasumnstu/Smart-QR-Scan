package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAnalysisResult
import com.example.data.ai.GeminiService
import com.example.data.ai.IdCardData
import com.example.data.local.AppDatabase
import com.example.data.model.CodeType
import com.example.data.model.ScanItem
import com.example.data.preferences.AppSettingsManager
import com.example.data.preferences.MyQrProfile
import com.example.data.repository.ScanRepository
import com.example.ui.components.ScanMode
import com.example.ui.navigation.AppScreen
import com.example.util.BarcodeParser
import com.example.util.FeedbackHelper
import com.example.util.ParsedBarcodeResult
import com.example.util.ShareHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ScanRepository(database.scanDao())
    val settingsManager = AppSettingsManager(application)
    private val feedbackHelper = FeedbackHelper(application)

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<AppScreen>()

    // Current Scan Result
    private val _activeResult = MutableStateFlow<ParsedBarcodeResult?>(null)
    val activeResult: StateFlow<ParsedBarcodeResult?> = _activeResult.asStateFlow()

    private val _activeScanItem = MutableStateFlow<ScanItem?>(null)
    val activeScanItem: StateFlow<ScanItem?> = _activeScanItem.asStateFlow()

    // AI Analysis for Active Result
    private val _aiAnalysis = MutableStateFlow<AiAnalysisResult?>(null)
    val aiAnalysis: StateFlow<AiAnalysisResult?> = _aiAnalysis.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    // Scanner Mode & Batch Scanning
    private val _scanMode = MutableStateFlow(ScanMode.SINGLE)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private val _batchItems = MutableStateFlow<List<ParsedBarcodeResult>>(emptyList())
    val batchItems: StateFlow<List<ParsedBarcodeResult>> = _batchItems.asStateFlow()

    private val _lastBatchFeedback = MutableStateFlow<String?>(null)
    val lastBatchFeedback: StateFlow<String?> = _lastBatchFeedback.asStateFlow()

    // ID Card Scan state
    private val _activeIdCard = MutableStateFlow<IdCardData?>(null)
    val activeIdCard: StateFlow<IdCardData?> = _activeIdCard.asStateFlow()

    private val _isIdCardAnalyzing = MutableStateFlow(false)
    val isIdCardAnalyzing: StateFlow<Boolean> = _isIdCardAnalyzing.asStateFlow()

    // History & Search
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    val historyItems: StateFlow<List<ScanItem>> = _historySearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allScans
            } else {
                repository.searchScans(query)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentScans: StateFlow<List<ScanItem>> = repository.recentScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteScans: StateFlow<List<ScanItem>> = repository.favoriteScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val themeMode = settingsManager.themeMode
    val vibrateOnScan = settingsManager.vibrateOnScan
    val soundOnScan = settingsManager.soundOnScan
    val autoCopy = settingsManager.autoCopy
    val autoOpenUrls = settingsManager.autoOpenUrls
    val saveHistory = settingsManager.saveHistory
    val myProfile = settingsManager.myProfile

    // Flashlight & Camera switch in scanner
    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _zoomRatio = MutableStateFlow(0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    // Dialog state for gallery scanning error
    private val _scanErrorMessage = MutableStateFlow<String?>(null)
    val scanErrorMessage: StateFlow<String?> = _scanErrorMessage.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            return true
        }
        return false
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun switchCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun setZoomRatio(ratio: Float) {
        _zoomRatio.value = ratio
    }

    fun clearErrorMessage() {
        _scanErrorMessage.value = null
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun onBarcodeDetected(barcode: Barcode) {
        val parsed = BarcodeParser.parse(barcode)
        if (_scanMode.value == ScanMode.BATCH) {
            handleBatchScan(parsed)
        } else if (_scanMode.value == ScanMode.ID_CARD && parsed.type == CodeType.ID_CARD) {
            handleIdCardBarcode(parsed)
        } else {
            handleSuccessfulScan(parsed)
        }
    }

    private fun handleBatchScan(parsed: ParsedBarcodeResult) {
        if (vibrateOnScan.value) feedbackHelper.vibrate()
        if (soundOnScan.value) feedbackHelper.playBeep()

        val currentList = _batchItems.value
        if (currentList.lastOrNull()?.rawValue == parsed.rawValue) {
            return
        }

        _batchItems.value = currentList + parsed
        _lastBatchFeedback.value = parsed.title.ifBlank { parsed.rawValue }

        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            if (_lastBatchFeedback.value == (parsed.title.ifBlank { parsed.rawValue })) {
                _lastBatchFeedback.value = null
            }
        }
    }

    fun removeBatchItem(index: Int) {
        val current = _batchItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _batchItems.value = current
        }
    }

    fun clearBatch() {
        _batchItems.value = emptyList()
        _lastBatchFeedback.value = null
    }

    fun saveAllBatchToHistory() {
        viewModelScope.launch {
            val items = _batchItems.value
            for (parsed in items) {
                repository.insert(
                    ScanItem(
                        rawValue = parsed.rawValue,
                        displayTitle = parsed.title,
                        displaySubtitle = parsed.subtitle,
                        codeFormat = parsed.format,
                        codeType = parsed.type,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            val count = items.size
            clearBatch()
            ShareHelper.showToast(getApplication(), "Saved $count codes to history")
        }
    }

    fun analyzeIdCardBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _isIdCardAnalyzing.value = true
            try {
                val result = GeminiService.analyzeIdCard(bitmap)
                _activeIdCard.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isIdCardAnalyzing.value = false
            }
        }
    }

    private fun handleIdCardBarcode(parsed: ParsedBarcodeResult) {
        if (vibrateOnScan.value) feedbackHelper.vibrate()
        if (soundOnScan.value) feedbackHelper.playBeep()
        viewModelScope.launch {
            _isIdCardAnalyzing.value = true
            val result = GeminiService.analyzeIdCard(null, parsed.rawValue)
            _activeIdCard.value = result
            _isIdCardAnalyzing.value = false
        }
    }

    fun clearActiveIdCard() {
        _activeIdCard.value = null
    }

    fun saveIdCardToHistory(idData: IdCardData) {
        viewModelScope.launch {
            val item = ScanItem(
                rawValue = "ID:${idData.idNumber} | Name:${idData.fullName} | Type:${idData.documentType}",
                displayTitle = idData.fullName.ifBlank { idData.documentType },
                displaySubtitle = "ID: ${idData.idNumber} (${idData.documentType})",
                codeFormat = "ID_DOCUMENT",
                codeType = CodeType.ID_CARD,
                timestamp = System.currentTimeMillis()
            )
            repository.insert(item)
            ShareHelper.showToast(getApplication(), "ID Card saved to history")
        }
    }

    fun analyzeActiveResultWithAi() {
        val active = _activeResult.value ?: return
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            try {
                val result = GeminiService.analyzeContent(
                    content = active.rawValue,
                    type = active.type
                )
                _aiAnalysis.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAiAnalyzing.value = false
            }
        }
    }

    fun scanImageBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val first = barcodes.firstOrNull()
                if (first != null) {
                    val parsed = BarcodeParser.parse(first)
                    handleSuccessfulScan(parsed)
                } else {
                    _scanErrorMessage.value = "No QR code or barcode found in the selected image. Please try a clearer image."
                }
            }
            .addOnFailureListener {
                _scanErrorMessage.value = "Failed to analyze image: ${it.localizedMessage}"
            }
    }

    private fun handleSuccessfulScan(parsed: ParsedBarcodeResult) {
        // Feedback
        if (vibrateOnScan.value) {
            feedbackHelper.vibrate()
        }
        if (soundOnScan.value) {
            feedbackHelper.playBeep()
        }
        if (autoCopy.value) {
            ShareHelper.copyToClipboard(getApplication(), parsed.rawValue)
        }

        viewModelScope.launch {
            var savedItem: ScanItem? = null
            if (saveHistory.value) {
                val item = ScanItem(
                    rawValue = parsed.rawValue,
                    displayTitle = parsed.title,
                    displaySubtitle = parsed.subtitle,
                    codeFormat = parsed.format,
                    codeType = parsed.type,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = false
                )
                val id = repository.insert(item)
                savedItem = item.copy(id = id)
            } else {
                savedItem = ScanItem(
                    id = -1,
                    rawValue = parsed.rawValue,
                    displayTitle = parsed.title,
                    displaySubtitle = parsed.subtitle,
                    codeFormat = parsed.format,
                    codeType = parsed.type,
                    timestamp = System.currentTimeMillis()
                )
            }
            // Brief delay to allow the frame highlight animation and haptic feedback to be perceived
            kotlinx.coroutines.delay(280)
            _activeResult.value = parsed
            _activeScanItem.value = savedItem
            _aiAnalysis.value = null
            _isTorchOn.value = false // Reset torch
            navigateTo(AppScreen.RESULT_DETAIL)
        }
    }

    fun openSavedItem(item: ScanItem) {
        val parsed = BarcodeParser.parseRawString(item.rawValue, item.codeFormat)
        _activeResult.value = parsed
        _activeScanItem.value = item
        _aiAnalysis.value = null
        navigateTo(AppScreen.RESULT_DETAIL)
    }

    fun toggleFavorite(item: ScanItem) {
        viewModelScope.launch {
            if (item.id > 0) {
                repository.toggleFavorite(item.id, item.isFavorite)
                val updated = item.copy(isFavorite = !item.isFavorite)
                if (_activeScanItem.value?.id == item.id) {
                    _activeScanItem.value = updated
                }
            }
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_activeScanItem.value?.id == id) {
                _activeScanItem.value = null
            }
        }
    }

    fun restoreScan(item: ScanItem) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun saveCreatedQr(content: String, format: String, type: CodeType, title: String) {
        viewModelScope.launch {
            if (saveHistory.value) {
                repository.insert(
                    ScanItem(
                        rawValue = content,
                        displayTitle = title.ifBlank { "Created QR Code" },
                        displaySubtitle = "Generated QR Code",
                        codeFormat = format,
                        codeType = type,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackHelper.release()
    }
}
