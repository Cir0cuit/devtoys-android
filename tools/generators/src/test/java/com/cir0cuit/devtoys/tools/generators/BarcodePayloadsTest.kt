package com.cir0cuit.devtoys.tools.generators

import com.google.zxing.BarcodeFormat
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodePayloadsTest {

    private val amsterdam = ZoneId.of("Europe/Amsterdam")

    private fun scan(text: String, format: BarcodeFormat = BarcodeFormat.QR_CODE) = describeScan(ScanHit(format, text, null), amsterdam)

    private fun error(type: PayloadType, f: Map<String, String>) =
        runCatching { buildPayload(type, f, amsterdam) }.exceptionOrNull()?.message.orEmpty()

    @Test
    fun wifiEscapesSpecialCharactersAndRoundTrips() {
        val payload = buildPayload(PayloadType.WIFI, mapOf("ssid" to "my;net", "password" to "pa:ss\"word1", "hidden" to "true"))
        assertEquals("WIFI:T:WPA;S:my\\;net;P:pa\\:ss\\\"word1;H:true;;", payload)
        val info = scan(payload)
        assertEquals("Wi-Fi network", info.typeLabel)
        assertEquals(listOf("Network" to "my;net", "Security" to "WPA", "Password" to "pa:ss\"word1", "Hidden network" to "Yes"), info.rows)
    }

    @Test
    fun wifiOpenNetworkHasNoPassword() {
        assertEquals("WIFI:T:nopass;S:cafe;;", buildPayload(PayloadType.WIFI, mapOf("ssid" to "cafe", "security" to "nopass", "password" to "ignored")))
        assertEquals(listOf("Network" to "cafe", "Security" to "None"), scan("WIFI:T:nopass;S:cafe;;").rows)
    }

    @Test
    fun wifiValidation() {
        assertEquals("Enter the network name.", error(PayloadType.WIFI, emptyMap()))
        assertEquals("WPA passwords are 8 to 63 characters. This one has 5.", error(PayloadType.WIFI, mapOf("ssid" to "x", "password" to "short")))
        assertEquals("Enter the Wi-Fi password.", error(PayloadType.WIFI, mapOf("ssid" to "x")))
        assertEquals("Enter the WEP key.", error(PayloadType.WIFI, mapOf("ssid" to "x", "security" to "WEP")))
    }

    @Test
    fun wifiPasswordErrorTreatsEmptyAsPrompt() {
        assertEquals("Enter the Wi-Fi password.", wifiPasswordError("WPA", ""))
        assertEquals(null, wifiPasswordError("WPA", "12345678"))
        assertEquals(null, wifiPasswordError("nopass", ""))
        assertEquals(null, wifiPasswordError("WEP", "k"))
    }

    @Test
    fun contactBuildsVcard() {
        val payload = buildPayload(
            PayloadType.CONTACT,
            mapOf(
                "first" to "Jane", "last" to "Doe", "org" to "Acme, Inc.", "title" to "CTO", "phone" to "+31 20 123 4567",
                "email" to "jane@example.com", "url" to "https://example.com", "address" to "Main St 1\nAmsterdam",
            )
        )
        assertEquals(
            "BEGIN:VCARD\nVERSION:3.0\nN:Doe;Jane;;;\nFN:Jane Doe\nORG:Acme\\, Inc.\nTITLE:CTO\nTEL:+31 20 123 4567\n" +
                "EMAIL:jane@example.com\nURL:https://example.com\nADR:;;Main St 1\\nAmsterdam;;;;\nEND:VCARD",
            payload
        )
        val info = scan(payload)
        assertEquals("Contact", info.typeLabel)
        assertTrue(info.rows.contains("Name" to "Jane Doe"))
        assertTrue(info.rows.contains("Organization" to "Acme, Inc."))
        assertTrue(info.rows.contains("Address" to "Main St 1\nAmsterdam"))
        assertEquals("Enter a name or an organization.", error(PayloadType.CONTACT, mapOf("phone" to "1")))
    }

    @Test
    fun emailSmsPhoneAndLink() {
        assertEquals(
            "mailto:jane@example.com?subject=Hi%20there&body=Line%201",
            buildPayload(PayloadType.EMAIL, mapOf("to" to "jane@example.com", "subject" to "Hi there", "body" to "Line 1"))
        )
        assertEquals("mailto:a@b.co", buildPayload(PayloadType.EMAIL, mapOf("to" to "a@b.co")))
        assertTrue(error(PayloadType.EMAIL, mapOf("to" to "nope")).startsWith("Enter an email address"))
        assertEquals("SMSTO:+31201234567:Hello there", buildPayload(PayloadType.SMS, mapOf("number" to "+31 20 1234567", "message" to "Hello there")))
        assertEquals("tel:+31201234567", buildPayload(PayloadType.PHONE, mapOf("number" to "+31 (20) 123-4567")))
        assertEquals("https://devtoys.app", buildPayload(PayloadType.URL, mapOf("url" to "devtoys.app")))
        assertEquals("ftp://x.org", buildPayload(PayloadType.URL, mapOf("url" to "ftp://x.org")))
        assertEquals(listOf("To" to "jane@example.com", "Subject" to "Hi there", "Message" to "Line 1"), scan("mailto:jane@example.com?subject=Hi%20there&body=Line%201").rows)
        assertEquals("SMS", scan("SMSTO:+31201234567:Hello there").typeLabel)
        assertEquals(listOf("Number" to "+31201234567"), scan("tel:+31201234567").rows)
    }

    @Test
    fun geoValidatesRanges() {
        assertEquals("geo:52.3731,4.8922", buildPayload(PayloadType.GEO, mapOf("lat" to "52.3731", "lon" to "4.8922")))
        assertTrue(error(PayloadType.GEO, mapOf("lat" to "91", "lon" to "0")).startsWith("Latitude is a number from -90 to 90"))
        assertTrue(error(PayloadType.GEO, mapOf("lat" to "0", "lon" to "east")).startsWith("Longitude is a number"))
        assertEquals("Location", scan("geo:52.3731,4.8922").typeLabel)
    }

    @Test
    fun eventIsWrittenInUtcAndReadBackInLocalTime() {
        val payload = buildPayload(PayloadType.EVENT, mapOf("summary" to "Team, sync", "location" to "Room 1", "start" to "2026-10-17 18:00"), amsterdam)
        assertEquals("BEGIN:VEVENT\nSUMMARY:Team\\, sync\nDTSTART:20261017T160000Z\nDTEND:20261017T170000Z\nLOCATION:Room 1\nEND:VEVENT", payload)
        val info = scan(payload)
        assertEquals("Event", info.typeLabel)
        assertEquals(listOf("Title" to "Team, sync", "Starts" to "2026-10-17 18:00", "Ends" to "2026-10-17 19:00", "Location" to "Room 1"), info.rows)
        assertTrue(error(PayloadType.EVENT, mapOf("summary" to "x", "start" to "tomorrow")).startsWith("Enter the start as"))
        assertEquals(
            "The event ends before it starts. Fix the end time.",
            error(PayloadType.EVENT, mapOf("summary" to "x", "start" to "2026-10-17 18:00", "end" to "2026-10-17 17:00"))
        )
    }

    @Test
    fun scansOfPlainDataAreLabelled() {
        assertEquals("Link", scan("https://devtoys.app").typeLabel)
        assertEquals(listOf("Product code" to "5901234123457"), scan("5901234123457", BarcodeFormat.EAN_13).rows)
        val text = scan("just some words")
        assertEquals("Text", text.typeLabel)
        assertTrue(text.rows.isEmpty())
    }
}
