package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.CodeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class AiAnalysisResult(
    val summary: String,
    val securityLevel: SecurityRating,
    val securityDetails: String,
    val category: String,
    val keyInsights: List<String>,
    val isAiPowered: Boolean = true
)

enum class SecurityRating {
    SAFE,
    CAUTION,
    DANGEROUS,
    INFO
}

data class IdCardData(
    val documentType: String = "ID Card",
    val fullName: String = "",
    val idNumber: String = "",
    val dateOfBirth: String = "",
    val expiryDate: String = "",
    val issueDate: String = "",
    val countryOrState: String = "",
    val rawExtractedText: String = "",
    val isAiPowered: Boolean = true
)

data class BarcodeProductInfo(
    val barcodeNumber: String,
    val format: String,
    val title: String,
    val originCountry: String,
    val category: String,
    val description: String,
    val isAiPowered: Boolean = true
)

object GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private fun getApiKey(): String {
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        return if (key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder", ignoreCase = true)) {
            key
        } else {
            ""
        }
    }

    suspend fun analyzeContent(content: String, type: CodeType): AiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    You are a cybersecurity and QR/barcode data analysis assistant.
                    Analyze this scanned content:
                    Content: "$content"
                    Type: ${type.name}

                    Provide a JSON response with the following keys ONLY (no markdown backticks, just raw json):
                    {
                      "summary": "Brief 1-2 sentence description of what this content is and what it does",
                      "securityLevel": "SAFE" or "CAUTION" or "DANGEROUS" or "INFO",
                      "securityDetails": "Explanation of safety analysis (phishing risk, tracking params, malware flags, or verification details)",
                      "category": "E-Commerce / Social / Wi-Fi / Contact / Payment / Authentication / General",
                      "keyInsights": ["Point 1", "Point 2", "Point 3"]
                    }
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    }
                    put("contents", contentsArray)
                }

                val url = "$BASE_URL/gemini-3.5-flash:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val text = root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanJson = text.replace("```json", "").replace("```", "").trim()
                    val parsed = JSONObject(cleanJson)

                    val secStr = parsed.optString("securityLevel", "SAFE")
                    val rating = when (secStr.uppercase()) {
                        "DANGEROUS" -> SecurityRating.DANGEROUS
                        "CAUTION" -> SecurityRating.CAUTION
                        "INFO" -> SecurityRating.INFO
                        else -> SecurityRating.SAFE
                    }

                    val insights = mutableListOf<String>()
                    val insightsArray = parsed.optJSONArray("keyInsights")
                    if (insightsArray != null) {
                        for (i in 0 until insightsArray.length()) {
                            insights.add(insightsArray.getString(i))
                        }
                    }

                    return@withContext AiAnalysisResult(
                        summary = parsed.optString("summary", "Scanned content verified."),
                        securityLevel = rating,
                        securityDetails = parsed.optString("securityDetails", "Safe verified content."),
                        category = parsed.optString("category", "General"),
                        keyInsights = insights,
                        isAiPowered = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Offline / Fallback Heuristic Analysis
        return@withContext fallbackAnalysis(content, type)
    }

    suspend fun analyzeIdCard(bitmap: Bitmap?, textHints: String? = null): IdCardData = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank() && bitmap != null) {
            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                val prompt = """
                    You are an official identity document scanner and OCR extractor.
                    Carefully analyze this ID card/Driver's License/Passport image.
                    Extract the details into a strict JSON object with NO markdown formatting:
                    {
                      "documentType": "Driver's License / Passport / National ID / Residence Permit / Student ID / Membership Card",
                      "fullName": "Extracted Full Name",
                      "idNumber": "Document or License Number",
                      "dateOfBirth": "YYYY-MM-DD or DD/MM/YYYY if visible",
                      "expiryDate": "YYYY-MM-DD or DD/MM/YYYY if visible",
                      "issueDate": "YYYY-MM-DD if visible",
                      "countryOrState": "Issuing Country or State/Authority",
                      "rawExtractedText": "Key details text summary"
                    }
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply { put("parts", parts) })
                    })
                }

                val url = "$BASE_URL/gemini-2.5-flash-image:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val text = root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanJson = text.replace("```json", "").replace("```", "").trim()
                    val parsed = JSONObject(cleanJson)

                    return@withContext IdCardData(
                        documentType = parsed.optString("documentType", "Identity Document"),
                        fullName = parsed.optString("fullName", "Unknown Name"),
                        idNumber = parsed.optString("idNumber", ""),
                        dateOfBirth = parsed.optString("dateOfBirth", ""),
                        expiryDate = parsed.optString("expiryDate", ""),
                        issueDate = parsed.optString("issueDate", ""),
                        countryOrState = parsed.optString("countryOrState", ""),
                        rawExtractedText = parsed.optString("rawExtractedText", text),
                        isAiPowered = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Local Heuristic parsing
        return@withContext fallbackIdCardParser(textHints ?: "")
    }

    suspend fun getBarcodeProductInfo(barcodeNumber: String, format: String): BarcodeProductInfo = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    Identify this barcode:
                    Number: $barcodeNumber
                    Format: $format

                    Provide a JSON response (NO markdown backticks):
                    {
                      "title": "Product name or standard classification",
                      "originCountry": "Country or jurisdiction of prefix (GS1)",
                      "category": "Food / Electronics / Retail / Media / Healthcare / Logistics",
                      "description": "Short explanation of product or prefix"
                    }
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                }

                val url = "$BASE_URL/gemini-3.5-flash:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val text = root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanJson = text.replace("```json", "").replace("```", "").trim()
                    val parsed = JSONObject(cleanJson)

                    return@withContext BarcodeProductInfo(
                        barcodeNumber = barcodeNumber,
                        format = format,
                        title = parsed.optString("title", "Product Code $barcodeNumber"),
                        originCountry = parsed.optString("originCountry", getGs1Country(barcodeNumber)),
                        category = parsed.optString("category", "Commercial Product"),
                        description = parsed.optString("description", "Standard retail barcode."),
                        isAiPowered = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext BarcodeProductInfo(
            barcodeNumber = barcodeNumber,
            format = format,
            title = "Item #$barcodeNumber",
            originCountry = getGs1Country(barcodeNumber),
            category = "Standard Retail ($format)",
            description = "Retail barcode compliant with GS1 international standard.",
            isAiPowered = false
        )
    }

    private fun getGs1Country(code: String): String {
        if (code.length < 3) return "International"
        val prefix = code.take(3).toIntOrNull() ?: return "International"
        return when (prefix) {
            in 0..139 -> "United States & Canada"
            in 300..379 -> "France"
            in 400..440 -> "Germany"
            in 450..459 -> "Japan"
            in 460..469 -> "Russia"
            471 -> "Taiwan"
            489 -> "Hong Kong"
            in 490..499 -> "Japan"
            in 500..509 -> "United Kingdom"
            in 520..521 -> "Greece"
            in 540..549 -> "Belgium & Luxembourg"
            560 -> "Portugal"
            in 570..579 -> "Denmark"
            in 590..599 -> "Poland"
            in 600..601 -> "South Africa"
            in 640..649 -> "Finland"
            in 690..699 -> "China"
            in 700..709 -> "Norway"
            in 730..739 -> "Sweden"
            750 -> "Mexico"
            in 760..769 -> "Switzerland"
            in 770..771 -> "Colombia"
            779 -> "Argentina"
            in 789..790 -> "Brazil"
            in 800..839 -> "Italy"
            in 840..849 -> "Spain"
            in 868..869 -> "Turkey"
            in 870..879 -> "Netherlands"
            880 -> "South Korea"
            885 -> "Thailand"
            888 -> "Singapore"
            890 -> "India"
            893 -> "Vietnam"
            in 930..939 -> "Australia"
            in 940..949 -> "New Zealand"
            else -> "International GS1"
        }
    }

    private fun fallbackAnalysis(content: String, type: CodeType): AiAnalysisResult {
        val insights = mutableListOf<String>()
        var rating = SecurityRating.SAFE
        var secDetails = "Content structure matches expected format."
        var category = "General"

        when (type) {
            CodeType.URL -> {
                category = "Web Link"
                val isHttps = content.startsWith("https://", ignoreCase = true)
                if (!isHttps) {
                    rating = SecurityRating.CAUTION
                    secDetails = "Unencrypted connection (HTTP). Avoid entering credentials or passwords."
                    insights.add("Uses unencrypted HTTP protocol")
                } else {
                    insights.add("Encrypted connection with standard HTTPS")
                }

                val suspiciousKeywords = listOf("free", "win", "gift", "prize", "urgent", "login-verify", "bank-secure", "apple-id", "update-account")
                if (suspiciousKeywords.any { content.contains(it, ignoreCase = true) }) {
                    rating = SecurityRating.DANGEROUS
                    secDetails = "Matches common phishing or deceptive domain patterns. Proceed with extreme caution."
                    insights.add("Suspicious keyword detected in URL structure")
                }

                if (content.contains("bit.ly") || content.contains("tinyurl.com") || content.contains("t.co")) {
                    if (rating == SecurityRating.SAFE) rating = SecurityRating.CAUTION
                    insights.add("URL shortener detected; target destination is masked")
                }
            }
            CodeType.WIFI -> {
                category = "Wi-Fi Network"
                insights.add("Wi-Fi network configuration profile")
                insights.add("Never share home network credentials with untrusted parties")
            }
            CodeType.CONTACT -> {
                category = "Contact (vCard)"
                insights.add("Electronic business card with direct import capability")
            }
            CodeType.PHONE -> {
                category = "Telephony"
                insights.add("Direct telephone dialer trigger")
            }
            CodeType.EMAIL -> {
                category = "Electronic Mail"
                insights.add("Pre-formatted email dispatch link")
            }
            CodeType.SMS -> {
                category = "Messaging"
                insights.add("Pre-composed text message payload")
            }
            else -> {
                insights.add("Standard text or alphanumeric barcode payload")
            }
        }

        return AiAnalysisResult(
            summary = "Detected ${type.name.replace("_", " ")}: ${content.take(60)}${if (content.length > 60) "..." else ""}",
            securityLevel = rating,
            securityDetails = secDetails,
            category = category,
            keyInsights = insights,
            isAiPowered = false
        )
    }

    private fun fallbackIdCardParser(text: String): IdCardData {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var docType = "ID Card / Driver's License"
        var name = ""
        var idNum = ""
        var dob = ""
        var exp = ""
        var authority = ""

        for (line in lines) {
            val upper = line.uppercase()
            if (upper.contains("DRIVER") || upper.contains("LICENCE") || upper.contains("LICENSE") || upper.contains("DL")) {
                docType = "Driver's License"
            } else if (upper.contains("PASSPORT")) {
                docType = "Passport"
            } else if (upper.contains("IDENTITY") || upper.contains("NATIONAL ID")) {
                docType = "National Identity Card"
            }

            if (upper.startsWith("FN ") || upper.startsWith("NAME") || upper.startsWith("LN ")) {
                name = line.substringAfter(":").ifEmpty { line.substringAfter(" ") }.trim()
            }
            if (upper.contains("DOB") || upper.contains("BIRTH")) {
                dob = line.substringAfter(":").trim()
            }
            if (upper.contains("EXP") || upper.contains("EXPIRES")) {
                exp = line.substringAfter(":").trim()
            }
            if (upper.contains("DL") || upper.contains("ID:") || upper.contains("NO.")) {
                idNum = line.substringAfter(":").trim()
            }
        }

        if (name.isBlank() && lines.isNotEmpty()) {
            name = lines.firstOrNull { it.length in 5..30 && !it.any { c -> c.isDigit() } } ?: "Cardholder"
        }
        if (idNum.isBlank() && lines.isNotEmpty()) {
            idNum = lines.firstOrNull { it.any { c -> c.isDigit() } && it.length in 6..18 } ?: "ID-000000"
        }

        return IdCardData(
            documentType = docType,
            fullName = name,
            idNumber = idNum,
            dateOfBirth = dob.ifEmpty { "1990-01-01" },
            expiryDate = exp.ifEmpty { "2030-12-31" },
            countryOrState = authority.ifEmpty { "Issued Authority" },
            rawExtractedText = text,
            isAiPowered = false
        )
    }
}
