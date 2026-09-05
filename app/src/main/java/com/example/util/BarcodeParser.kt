package com.example.util

import android.net.Uri
import com.example.data.model.CodeType
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.Locale

data class ParsedBarcodeResult(
    val rawValue: String,
    val format: String,
    val type: CodeType,
    val title: String,
    val subtitle: String,
    val url: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val wifiEncryption: String? = null,
    val contactName: String? = null,
    val contactOrg: String? = null,
    val smsNumber: String? = null,
    val smsBody: String? = null,
    val geoLat: Double? = null,
    val geoLng: Double? = null,
    val calendarTitle: String? = null,
    val calendarLocation: String? = null
)

object BarcodeParser {

    fun formatToString(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_ITF -> "ITF"
            else -> "Barcode"
        }
    }

    fun parse(mlkitBarcode: Barcode): ParsedBarcodeResult {
        val raw = mlkitBarcode.rawValue ?: mlkitBarcode.displayValue ?: ""
        val formatStr = formatToString(mlkitBarcode.format)

        when (mlkitBarcode.valueType) {
            Barcode.TYPE_URL -> {
                val urlObj = mlkitBarcode.url
                val fullUrl = urlObj?.url ?: raw
                val host = try {
                    Uri.parse(fullUrl).host ?: fullUrl
                } catch (e: Exception) {
                    fullUrl
                }
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.URL,
                    title = host,
                    subtitle = fullUrl,
                    url = fullUrl
                )
            }
            Barcode.TYPE_PHONE -> {
                val phone = mlkitBarcode.phone?.number ?: raw
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.PHONE,
                    title = phone,
                    subtitle = "Phone Number",
                    phone = phone
                )
            }
            Barcode.TYPE_EMAIL -> {
                val emailObj = mlkitBarcode.email
                val address = emailObj?.address ?: raw
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.EMAIL,
                    title = address,
                    subtitle = emailObj?.subject ?: "Email Message",
                    email = address
                )
            }
            Barcode.TYPE_WIFI -> {
                val wifi = mlkitBarcode.wifi
                val ssid = wifi?.ssid ?: ""
                val pass = wifi?.password ?: ""
                val encType = when (wifi?.encryptionType) {
                    Barcode.WiFi.TYPE_WPA -> "WPA/WPA2"
                    Barcode.WiFi.TYPE_WEP -> "WEP"
                    Barcode.WiFi.TYPE_OPEN -> "None (Open)"
                    else -> "Wi-Fi"
                }
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.WIFI,
                    title = ssid.ifBlank { "Wi-Fi Network" },
                    subtitle = "Security: $encType",
                    wifiSsid = ssid,
                    wifiPassword = pass,
                    wifiEncryption = encType
                )
            }
            Barcode.TYPE_CONTACT_INFO -> {
                val contact = mlkitBarcode.contactInfo
                val name = contact?.name?.formattedName ?: "Contact"
                val org = contact?.organization ?: ""
                val phone = contact?.phones?.firstOrNull()?.number
                val email = contact?.emails?.firstOrNull()?.address
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.CONTACT,
                    title = name,
                    subtitle = org.ifBlank { phone ?: email ?: "vCard" },
                    contactName = name,
                    contactOrg = org,
                    phone = phone,
                    email = email
                )
            }
            Barcode.TYPE_SMS -> {
                val sms = mlkitBarcode.sms
                val number = sms?.phoneNumber ?: ""
                val message = sms?.message ?: ""
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.SMS,
                    title = "SMS: $number",
                    subtitle = message.ifBlank { "Send SMS" },
                    smsNumber = number,
                    smsBody = message
                )
            }
            Barcode.TYPE_GEO -> {
                val geo = mlkitBarcode.geoPoint
                val lat = geo?.lat ?: 0.0
                val lng = geo?.lng ?: 0.0
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.LOCATION,
                    title = String.format(Locale.US, "%.5f, %.5f", lat, lng),
                    subtitle = "Geographic Coordinates",
                    geoLat = lat,
                    geoLng = lng
                )
            }
            Barcode.TYPE_CALENDAR_EVENT -> {
                val event = mlkitBarcode.calendarEvent
                val summary = event?.summary ?: "Calendar Event"
                val loc = event?.location ?: ""
                return ParsedBarcodeResult(
                    rawValue = raw,
                    format = formatStr,
                    type = CodeType.CALENDAR,
                    title = summary,
                    subtitle = loc.ifBlank { "Calendar Event" },
                    calendarTitle = summary,
                    calendarLocation = loc
                )
            }
            else -> {
                // Fallback smart parser on raw string
                return parseRawString(raw, formatStr)
            }
        }
    }

    fun parseRawString(raw: String, formatStr: String = "QR Code"): ParsedBarcodeResult {
        val trimmed = raw.trim()

        // Check Wi-Fi pattern: WIFI:S:...;T:...;P:...;;
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val ssid = extractWifiField(trimmed, "S:")
            val pass = extractWifiField(trimmed, "P:")
            val enc = extractWifiField(trimmed, "T:").ifBlank { "WPA" }
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.WIFI,
                title = ssid.ifBlank { "Wi-Fi Network" },
                subtitle = "Security: $enc",
                wifiSsid = ssid,
                wifiPassword = pass,
                wifiEncryption = enc
            )
        }

        // Check vCard
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            val name = extractVCardField(trimmed, "FN:")
                .ifBlank { extractVCardField(trimmed, "N:") }
                .ifBlank { "Contact Card" }
            val org = extractVCardField(trimmed, "ORG:")
            val tel = extractVCardField(trimmed, "TEL:")
            val email = extractVCardField(trimmed, "EMAIL:")
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.CONTACT,
                title = name,
                subtitle = org.ifBlank { tel.ifBlank { email.ifBlank { "vCard" } } },
                contactName = name,
                contactOrg = org,
                phone = tel.ifBlank { null },
                email = email.ifBlank { null }
            )
        }

        // Check URL
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true)
        ) {
            val fullUrl = if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
            val host = try {
                Uri.parse(fullUrl).host ?: fullUrl
            } catch (e: Exception) {
                fullUrl
            }
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.URL,
                title = host,
                subtitle = fullUrl,
                url = fullUrl
            )
        }

        // Check Email
        if (trimmed.startsWith("mailto:", ignoreCase = true) ||
            (trimmed.contains("@") && !trimmed.contains(" ") && trimmed.contains("."))
        ) {
            val address = trimmed.removePrefix("mailto:").substringBefore("?")
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.EMAIL,
                title = address,
                subtitle = "Email Address",
                email = address
            )
        }

        // Check Phone
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val phone = trimmed.removePrefix("tel:")
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.PHONE,
                title = phone,
                subtitle = "Phone Number",
                phone = phone
            )
        }

        // Check SMS
        if (trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true)) {
            val content = trimmed.removePrefix("smsto:").removePrefix("sms:")
            val parts = content.split(":", limit = 2)
            val number = parts.getOrNull(0) ?: ""
            val body = parts.getOrNull(1) ?: ""
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.SMS,
                title = "SMS: $number",
                subtitle = body.ifBlank { "Send Message" },
                smsNumber = number,
                smsBody = body
            )
        }

        // Check Geo
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            val coords = trimmed.removePrefix("geo:").substringBefore("?").split(",")
            val lat = coords.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val lng = coords.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.LOCATION,
                title = String.format(Locale.US, "%.5f, %.5f", lat, lng),
                subtitle = "Coordinates",
                geoLat = lat,
                geoLng = lng
            )
        }

        // Check Calendar
        if (trimmed.contains("BEGIN:VEVENT", ignoreCase = true)) {
            val summary = extractVCardField(trimmed, "SUMMARY:").ifBlank { "Calendar Event" }
            val location = extractVCardField(trimmed, "LOCATION:")
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.CALENDAR,
                title = summary,
                subtitle = location.ifBlank { "Calendar Event" },
                calendarTitle = summary,
                calendarLocation = location
            )
        }

        // Check AAMVA ID Card / Driver's License (PDF417)
        if (formatStr == "PDF417" && (trimmed.contains("AAMVA", ignoreCase = true) || trimmed.contains("ANSI ", ignoreCase = true) || trimmed.contains("DAQ", ignoreCase = true))) {
            val firstName = extractAamvaField(trimmed, "DAC")
            val lastName = extractAamvaField(trimmed, "DCS")
            val idNumber = extractAamvaField(trimmed, "DAQ")
            val name = if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName".trim() else "ID Document"
            return ParsedBarcodeResult(
                rawValue = raw,
                format = formatStr,
                type = CodeType.ID_CARD,
                title = name,
                subtitle = if (idNumber.isNotBlank()) "License/ID #$idNumber" else "Driver's License / ID Card",
                contactName = name
            )
        }

        // Barcode or Text
        val isBarcode = formatStr != "QR Code"
        val codeType = if (isBarcode) CodeType.BARCODE else CodeType.TEXT
        val display = if (raw.length > 30) raw.take(28) + "..." else raw
        return ParsedBarcodeResult(
            rawValue = raw,
            format = formatStr,
            type = codeType,
            title = display,
            subtitle = if (isBarcode) "$formatStr: $raw" else "Plain Text"
        )
    }

    private fun extractWifiField(raw: String, key: String): String {
        val index = raw.indexOf(key, ignoreCase = true)
        if (index == -1) return ""
        val start = index + key.length
        val end = raw.indexOf(";", start)
        return if (end != -1) raw.substring(start, end) else raw.substring(start)
    }

    private fun extractVCardField(raw: String, key: String): String {
        val lines = raw.lines()
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith(key, ignoreCase = true)) {
                return trimmedLine.substring(key.length).trim()
            }
        }
        return ""
    }

    private fun extractAamvaField(raw: String, subfileKey: String): String {
        val lines = raw.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith(subfileKey)) {
                return trimmed.substring(subfileKey.length).trim()
            }
        }
        val idx = raw.indexOf(subfileKey)
        if (idx != -1) {
            val start = idx + subfileKey.length
            val end = raw.indexOfAny(charArrayOf('\n', '\r', '\u001e'), start)
            return if (end != -1) raw.substring(start, end).trim() else raw.substring(start).trim()
        }
        return ""
    }
}
