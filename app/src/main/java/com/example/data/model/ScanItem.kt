package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CodeType {
    QR_CODE,
    BARCODE,
    URL,
    PHONE,
    EMAIL,
    WIFI,
    CONTACT,
    SMS,
    LOCATION,
    CALENDAR,
    ID_CARD,
    TEXT
}

@Entity(tableName = "scan_items")
data class ScanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawValue: String,
    val displayTitle: String,
    val displaySubtitle: String = "",
    val codeFormat: String, // QR_CODE, EAN_13, etc.
    val codeType: CodeType,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
