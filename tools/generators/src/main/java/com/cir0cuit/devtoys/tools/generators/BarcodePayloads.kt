package com.cir0cuit.devtoys.tools.generators

import com.google.zxing.Result
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.CalendarParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.ExpandedProductParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ISBNParsedResult
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ProductParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.VINParsedResult
import com.google.zxing.client.result.WifiParsedResult
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal enum class FieldKind { TEXT, MULTILINE, URL, EMAIL, PHONE, DECIMAL, PASSWORD }

internal data class PayloadField(val key: String, val label: String, val kind: FieldKind = FieldKind.TEXT, val placeholder: String? = null)

/** Structured content a 2D code can carry. TEXT uses the free text field; Wi-Fi also has "security" and "hidden". */
internal enum class PayloadType(val label: String, val fields: List<PayloadField>) {
    TEXT("Text", emptyList()),
    URL("Link", listOf(PayloadField("url", "Link", FieldKind.URL, "https://example.com"))),
    WIFI("Wi-Fi", listOf(PayloadField("ssid", "Network name"), PayloadField("password", "Password", FieldKind.PASSWORD))),
    CONTACT(
        "Contact",
        listOf(
            PayloadField("first", "First name"), PayloadField("last", "Last name"),
            PayloadField("phone", "Phone", FieldKind.PHONE), PayloadField("email", "Email", FieldKind.EMAIL),
            PayloadField("org", "Organization"), PayloadField("title", "Job title"),
            PayloadField("url", "Website", FieldKind.URL), PayloadField("address", "Address", FieldKind.MULTILINE),
        )
    ),
    EMAIL(
        "Email",
        listOf(PayloadField("to", "To", FieldKind.EMAIL, "name@example.com"), PayloadField("subject", "Subject"), PayloadField("body", "Message", FieldKind.MULTILINE))
    ),
    SMS("SMS", listOf(PayloadField("number", "Phone number", FieldKind.PHONE), PayloadField("message", "Message", FieldKind.MULTILINE))),
    PHONE("Phone", listOf(PayloadField("number", "Phone number", FieldKind.PHONE, "+31 20 123 4567"))),
    GEO("Location", listOf(PayloadField("lat", "Latitude", FieldKind.DECIMAL, "52.3731"), PayloadField("lon", "Longitude", FieldKind.DECIMAL, "4.8922"))),
    EVENT(
        "Event",
        listOf(
            PayloadField("summary", "Title"), PayloadField("location", "Location"),
            PayloadField("start", "Starts", placeholder = "2026-10-17 18:00"), PayloadField("end", "Ends (optional)", placeholder = "2026-10-17 19:00"),
        )
    ),
}

internal val WIFI_SECURITY = listOf("WPA" to "WPA/WPA2", "WEP" to "WEP", "nopass" to "None")

private fun escapeWifi(s: String) = s.replace(Regex("""([\\;,:"])"""), """\\$1""")

private fun escapeVcard(s: String) = s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")

private fun encodeQuery(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

private val EVENT_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val ICAL_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

private fun parseLocal(value: String, label: String): LocalDateTime = try {
    LocalDateTime.parse(value.trim(), EVENT_INPUT)
} catch (e: DateTimeParseException) {
    throw IllegalArgumentException("Enter $label as year-month-day hour:minute, for example 2026-10-17 18:00.")
}

/** Builds the text a scanner app understands for [type]; throws [IllegalArgumentException] naming the field to fix. */
internal fun buildPayload(type: PayloadType, f: Map<String, String>, zone: ZoneId = ZoneId.systemDefault()): String {
    fun v(key: String) = f[key].orEmpty().trim()
    return when (type) {
        PayloadType.TEXT -> f["text"].orEmpty()
        PayloadType.URL -> {
            val url = v("url")
            require(url.isNotEmpty()) { "Enter a link." }
            if (url.contains("://") || url.startsWith("mailto:")) url else "https://$url"
        }
        PayloadType.WIFI -> {
            val ssid = f["ssid"].orEmpty()
            val security = f["security"].takeUnless { it.isNullOrEmpty() } ?: "WPA"
            val password = f["password"].orEmpty()
            require(ssid.isNotEmpty()) { "Enter the network name." }
            if (security == "WPA") require(password.length in 8..63) { "WPA passwords are 8 to 63 characters. This one has ${password.length}." }
            if (security == "WEP") require(password.isNotEmpty()) { "Enter the WEP key." }
            buildString {
                append("WIFI:T:$security;S:${escapeWifi(ssid)};")
                if (security != "nopass") append("P:${escapeWifi(password)};")
                if (f["hidden"] == "true") append("H:true;")
                append(";")
            }
        }
        PayloadType.CONTACT -> {
            val first = v("first")
            val last = v("last")
            val org = v("org")
            require(first.isNotEmpty() || last.isNotEmpty() || org.isNotEmpty()) { "Enter a name or an organization." }
            val full = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { org }
            buildString {
                append("BEGIN:VCARD\nVERSION:3.0\n")
                append("N:${escapeVcard(last)};${escapeVcard(first)};;;\n")
                append("FN:${escapeVcard(full)}\n")
                if (org.isNotEmpty()) append("ORG:${escapeVcard(org)}\n")
                if (v("title").isNotEmpty()) append("TITLE:${escapeVcard(v("title"))}\n")
                if (v("phone").isNotEmpty()) append("TEL:${v("phone")}\n")
                if (v("email").isNotEmpty()) append("EMAIL:${v("email")}\n")
                if (v("url").isNotEmpty()) append("URL:${v("url")}\n")
                if (v("address").isNotEmpty()) append("ADR:;;${escapeVcard(v("address"))};;;;\n")
                append("END:VCARD")
            }
        }
        PayloadType.EMAIL -> {
            val to = v("to")
            require('@' in to && ' ' !in to) { "Enter an email address, for example name@example.com." }
            val query = listOfNotNull(
                v("subject").takeIf { it.isNotEmpty() }?.let { "subject=${encodeQuery(it)}" },
                v("body").takeIf { it.isNotEmpty() }?.let { "body=${encodeQuery(it)}" },
            )
            "mailto:$to" + if (query.isEmpty()) "" else query.joinToString("&", prefix = "?")
        }
        PayloadType.SMS -> {
            require(v("number").isNotEmpty()) { "Enter a phone number." }
            "SMSTO:${v("number").filterNot { it == ' ' }}:${f["message"].orEmpty()}"
        }
        PayloadType.PHONE -> {
            val number = v("number").filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            require(number.any { it.isDigit() }) { "Enter a phone number." }
            "tel:$number"
        }
        PayloadType.GEO -> {
            val lat = v("lat").toDoubleOrNull()
            val lon = v("lon").toDoubleOrNull()
            require(lat != null && lat in -90.0..90.0) { "Latitude is a number from -90 to 90, for example 52.3731." }
            require(lon != null && lon in -180.0..180.0) { "Longitude is a number from -180 to 180, for example 4.8922." }
            "geo:${v("lat")},${v("lon")}"
        }
        PayloadType.EVENT -> {
            require(v("summary").isNotEmpty()) { "Enter a title for the event." }
            val start = parseLocal(v("start"), "the start")
            val end = if (v("end").isEmpty()) start.plusHours(1) else parseLocal(v("end"), "the end")
            require(!end.isBefore(start)) { "The event ends before it starts. Fix the end time." }
            fun utc(t: LocalDateTime) = t.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).format(ICAL_UTC)
            buildString {
                append("BEGIN:VEVENT\nSUMMARY:${escapeVcard(v("summary"))}\n")
                append("DTSTART:${utc(start)}\nDTEND:${utc(end)}\n")
                if (v("location").isNotEmpty()) append("LOCATION:${escapeVcard(v("location"))}\n")
                append("END:VEVENT")
            }
        }
    }
}

/** A scanned code interpreted: what it is and its fields as label/value rows. */
internal class ScanInfo(val typeLabel: String, val rows: List<Pair<String, String>>, val parsed: ParsedResult)

private fun Array<String>?.joined() = this?.filter { it.isNotBlank() }?.joinToString("\n").orEmpty()

internal fun describeScan(hit: ScanHit, zone: ZoneId = ZoneId.systemDefault()): ScanInfo {
    val parsed = ResultParser.parseResult(Result(hit.text, null, null, hit.format))
    val rows = mutableListOf<Pair<String, String>>()
    fun row(label: String, value: String?) { if (!value.isNullOrBlank()) rows += label to value }
    val label = when (parsed) {
        is URIParsedResult -> { row("Link", parsed.getURI()); row("Title", parsed.title); "Link" }
        is WifiParsedResult -> {
            row("Network", parsed.ssid)
            row("Security", if (parsed.networkEncryption.equals("nopass", true)) "None" else parsed.networkEncryption)
            row("Password", parsed.password)
            if (parsed.isHidden) row("Hidden network", "Yes")
            "Wi-Fi network"
        }
        is AddressBookParsedResult -> {
            row("Name", parsed.names.joined()); row("Organization", parsed.org); row("Job title", parsed.title)
            row("Phone", parsed.phoneNumbers.joined()); row("Email", parsed.emails.joined())
            row("Address", parsed.addresses.joined()); row("Website", parsed.getURLs().joined()); row("Note", parsed.note)
            "Contact"
        }
        is EmailAddressParsedResult -> { row("To", parsed.tos.joined()); row("Cc", parsed.getCCs().joined()); row("Subject", parsed.subject); row("Message", parsed.body); "Email" }
        is SMSParsedResult -> { row("To", parsed.numbers.joined()); row("Subject", parsed.subject); row("Message", parsed.body); "SMS" }
        is TelParsedResult -> { row("Number", parsed.number); "Phone number" }
        is GeoParsedResult -> {
            row("Latitude", parsed.latitude.toString()); row("Longitude", parsed.longitude.toString())
            if (parsed.altitude > 0) row("Altitude", "${parsed.altitude} m")
            row("Query", parsed.query)
            "Location"
        }
        is CalendarParsedResult -> {
            fun time(ts: Long, allDay: Boolean): String {
                val t = Instant.ofEpochMilli(ts).atZone(zone)
                return if (allDay) LocalDate.from(t).toString() else t.format(EVENT_INPUT)
            }
            row("Title", parsed.summary)
            if (parsed.startTimestamp >= 0) row("Starts", time(parsed.startTimestamp, parsed.isStartAllDay))
            if (parsed.endTimestamp >= 0) row("Ends", time(parsed.endTimestamp, parsed.isEndAllDay))
            row("Location", parsed.location); row("Description", parsed.description)
            "Event"
        }
        is ISBNParsedResult -> { row("ISBN", parsed.getISBN()); "Book (ISBN)" }
        is ProductParsedResult -> { row("Product code", parsed.normalizedProductID); "Product code" }
        is ExpandedProductParsedResult -> {
            row("Product (GTIN)", parsed.productID); row("Lot", parsed.lotNumber); row("Best before", parsed.bestBeforeDate)
            row("Expires", parsed.expirationDate); row("Weight", parsed.weight)
            "GS1 product data"
        }
        is VINParsedResult -> {
            row("VIN", parsed.getVIN()); row("Manufacturer code", parsed.worldManufacturerID); row("Country code", parsed.countryCode)
            if (parsed.modelYear > 0) row("Model year", parsed.modelYear.toString())
            "Vehicle VIN"
        }
        else -> if (parsed.type == ParsedResultType.TEXT) "Text" else parsed.type.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    return ScanInfo(label, rows, parsed)
}
