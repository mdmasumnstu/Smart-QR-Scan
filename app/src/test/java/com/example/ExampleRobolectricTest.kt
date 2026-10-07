package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CodeType
import com.example.util.BarcodeParser
import com.example.util.QrDotStyle
import com.example.util.QrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart QR Scanner & Generator", appName)
    }

    @Test
    fun `parse url string correctly`() {
        val parsed = BarcodeParser.parseRawString("https://example.com/login", "QR_CODE")
        assertEquals(CodeType.URL, parsed.type)
        assertEquals("https://example.com/login", parsed.url)
        assertEquals("example.com", parsed.title)
    }

    @Test
    fun `parse wifi string correctly`() {
        val parsed = BarcodeParser.parseRawString("WIFI:S:MyHomeWifi;T:WPA;P:SuperSecret123;;", "QR_CODE")
        assertEquals(CodeType.WIFI, parsed.type)
        assertEquals("MyHomeWifi", parsed.wifiSsid)
        assertEquals("SuperSecret123", parsed.wifiPassword)
    }

    @Test
    fun `generate qr bitmap returns valid bitmap`() {
        val bitmap = QrGenerator.generateQrBitmap(
            content = "https://example.com",
            size = 200,
            dotStyle = QrDotStyle.SQUARE
        )
        assertNotNull(bitmap)
        assertTrue(bitmap!!.width > 0)
        assertTrue(bitmap.height > 0)
    }

    @Test
    fun `generate qr bitmap for plain text and rounded dots`() {
        val bitmap = QrGenerator.generateQrBitmap(
            content = "Hello, world! This is a test message for ZXing QR code generator.",
            size = 300,
            dotStyle = QrDotStyle.ROUNDED
        )
        assertNotNull(bitmap)
        assertEquals(300, bitmap!!.width)
        assertEquals(300, bitmap.height)
    }

    @Test
    fun `generate qr bitmap returns null for empty string`() {
        val bitmap = QrGenerator.generateQrBitmap(
            content = "",
            size = 200
        )
        org.junit.Assert.assertNull(bitmap)
    }
}

