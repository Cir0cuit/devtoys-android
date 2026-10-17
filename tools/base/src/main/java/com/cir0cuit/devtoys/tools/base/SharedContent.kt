package com.cir0cuit.devtoys.tools.base

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf

/** Content another app shared into DevToys through the system share sheet. */
sealed interface SharedContent {
    data class Text(val text: String) : SharedContent
    data class Image(val uri: Uri) : SharedContent
    /** A file opened or shared from another app; [name] is its display name, used to pick a tool by extension. */
    data class File(val uri: Uri, val name: String) : SharedContent
}

enum class ShareKind { TEXT, IMAGE, FILE }

val SharedContent.kind: ShareKind get() = when (this) {
    is SharedContent.Text -> ShareKind.TEXT
    is SharedContent.Image -> ShareKind.IMAGE
    is SharedContent.File -> ShareKind.FILE
}

/** Lower-case extension of a shared file name, or "" when it has none. */
val SharedContent.File.extension: String get() = name.substringAfterLast('.', "").lowercase()

/** Set while a tool was opened from the share sheet; tools that declare [ToolDescriptor.accepts] read it. */
val LocalSharedContent = staticCompositionLocalOf<SharedContent?> { null }

/** Runs [apply] once with shared text when this tool was opened from the share sheet. */
@Composable
fun OnSharedText(apply: (String) -> Unit) {
    val shared = LocalSharedContent.current
    LaunchedEffect(shared) { (shared as? SharedContent.Text)?.let { apply(it.text) } }
}

/** Runs [apply] once with a shared image when this tool was opened from the share sheet. */
@Composable
fun OnSharedImage(apply: (Uri) -> Unit) {
    val shared = LocalSharedContent.current
    LaunchedEffect(shared) { (shared as? SharedContent.Image)?.let { apply(it.uri) } }
}

/** Runs [apply] once with a shared file when this tool was opened with one. Only tools that accept [ShareKind.FILE] get files. */
@Composable
fun OnSharedFile(apply: (SharedContent.File) -> Unit) {
    val shared = LocalSharedContent.current
    LaunchedEffect(shared) { (shared as? SharedContent.File)?.let(apply) }
}
