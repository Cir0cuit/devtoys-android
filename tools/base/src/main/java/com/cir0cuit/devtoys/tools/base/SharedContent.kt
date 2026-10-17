package com.cir0cuit.devtoys.tools.base

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf

/** Content another app shared into DevToys through the system share sheet. */
sealed interface SharedContent {
    data class Text(val text: String) : SharedContent
    data class Image(val uri: Uri) : SharedContent
}

enum class ShareKind { TEXT, IMAGE }

val SharedContent.kind: ShareKind get() = if (this is SharedContent.Text) ShareKind.TEXT else ShareKind.IMAGE

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
