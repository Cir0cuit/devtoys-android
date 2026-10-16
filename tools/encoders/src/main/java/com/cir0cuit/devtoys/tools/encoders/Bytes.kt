package com.cir0cuit.devtoys.tools.encoders

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** The bytes as text when they are valid UTF-8, otherwise null. */
internal fun utf8OrNull(bytes: ByteArray): String? =
    try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: CharacterCodingException) {
        null
    }

/** `hexdump -C` style lines: offset, 16 hex bytes, printable ASCII. */
internal fun hexDump(bytes: ByteArray): String = buildString {
    for (offset in bytes.indices step 16) {
        val row = bytes.copyOfRange(offset, minOf(offset + 16, bytes.size))
        append("%08x  ".format(offset))
        for (i in 0 until 16) {
            append(if (i < row.size) "%02x ".format(row[i]) else "   ")
            if (i == 7) append(' ')
        }
        append(" |")
        row.forEach { b -> append(if (b in 0x20..0x7e) b.toInt().toChar() else '.') }
        append("|\n")
    }
}.trimEnd()

/** File type guessed from magic bytes, as (label, file extension), or null when unknown. */
internal fun detectFileType(b: ByteArray): Pair<String, String>? {
    fun starts(vararg sig: Int) = b.size >= sig.size && sig.indices.all { b[it] == sig[it].toByte() }
    return when {
        starts(0x89, 0x50, 0x4E, 0x47) -> "PNG image" to "png"
        starts(0xFF, 0xD8, 0xFF) -> "JPEG image" to "jpg"
        starts(0x47, 0x49, 0x46, 0x38) -> "GIF image" to "gif"
        starts(0x52, 0x49, 0x46, 0x46) && b.size >= 12 && String(b, 8, 4, Charsets.US_ASCII) == "WEBP" -> "WebP image" to "webp"
        starts(0x25, 0x50, 0x44, 0x46) -> "PDF document" to "pdf"
        starts(0x50, 0x4B, 0x03, 0x04) -> "ZIP archive" to "zip"
        starts(0x1F, 0x8B) -> "GZip data" to "gz"
        starts(0x30, 0x82) -> "DER (ASN.1) data" to "der"
        starts(0x7F, 0x45, 0x4C, 0x46) -> "ELF binary" to "elf"
        else -> null
    }
}

internal fun formatSize(n: Int): String = when {
    n < 1024 -> "$n B"
    n < 1024 * 1024 -> "%.1f KB".format(n / 1024.0)
    else -> "%.1f MB".format(n / (1024.0 * 1024))
}
