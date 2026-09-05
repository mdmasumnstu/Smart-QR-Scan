package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

enum class QrDotStyle {
    SQUARE,
    ROUNDED
}

object QrGenerator {

    fun generateQrBitmap(
        content: String,
        size: Int = 800,
        foregroundColor: Int = android.graphics.Color.BLACK,
        backgroundColor: Int = android.graphics.Color.WHITE,
        dotStyle: QrDotStyle = QrDotStyle.SQUARE,
        centerIcon: Bitmap? = null
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                // Use high error correction level especially if center icon is used
                put(EncodeHintType.ERROR_CORRECTION, if (centerIcon != null) ErrorCorrectionLevel.H else ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 2)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fill background
            val bgPaint = Paint().apply {
                color = backgroundColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Draw QR modules
            val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = foregroundColor
                style = Paint.Style.FILL
            }

            // Reserve center area if logo is provided
            val logoRadiusRatio = 0.18f
            val logoCenter = width / 2f
            val logoBoxSize = (width * logoRadiusRatio)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (bitMatrix.get(x, y)) {
                        // If center icon is present, skip modules in center region
                        if (centerIcon != null &&
                            x >= (logoCenter - logoBoxSize) && x <= (logoCenter + logoBoxSize) &&
                            y >= (logoCenter - logoBoxSize) && y <= (logoCenter + logoBoxSize)
                        ) {
                            continue
                        }

                        if (dotStyle == QrDotStyle.ROUNDED) {
                            canvas.drawCircle(x + 0.5f, y + 0.5f, 0.45f, fgPaint)
                        } else {
                            canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, fgPaint)
                        }
                    }
                }
            }

            // Draw center logo if present
            if (centerIcon != null) {
                val logoTargetSize = (logoBoxSize * 1.8f).toInt()
                val left = (logoCenter - logoTargetSize / 2f)
                val top = (logoCenter - logoTargetSize / 2f)

                // White rounded badge behind logo
                val badgePadding = 8f
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = backgroundColor
                    style = Paint.Style.FILL
                    setShadowLayer(6f, 0f, 2f, 0x44000000)
                }
                val badgeRect = RectF(
                    left - badgePadding,
                    top - badgePadding,
                    left + logoTargetSize + badgePadding,
                    top + logoTargetSize + badgePadding
                )
                canvas.drawRoundRect(badgeRect, 16f, 16f, badgePaint)

                // Draw scaled logo
                val scaledLogo = Bitmap.createScaledBitmap(centerIcon, logoTargetSize, logoTargetSize, true)
                canvas.drawBitmap(scaledLogo, left, top, null)
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
