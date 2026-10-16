package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Http
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

internal class HttpStatus(val code: Int, val name: String, val meaning: String)

internal val HTTP_STATUSES = listOf(
    "1xx Informational" to listOf(
        HttpStatus(100, "Continue", "Headers received; the client should send the request body."),
        HttpStatus(101, "Switching Protocols", "The server is switching to the protocol in the Upgrade header, such as WebSocket."),
        HttpStatus(102, "Processing", "WebDAV: the request was received and is still being processed."),
        HttpStatus(103, "Early Hints", "Preliminary headers, such as Link preloads, sent before the final response.")
    ),
    "2xx Success" to listOf(
        HttpStatus(200, "OK", "The request succeeded."),
        HttpStatus(201, "Created", "The request succeeded and a new resource was created."),
        HttpStatus(202, "Accepted", "The request was accepted for processing, which has not finished yet."),
        HttpStatus(203, "Non-Authoritative Information", "Success, but the payload was modified by a proxy."),
        HttpStatus(204, "No Content", "Success with no response body."),
        HttpStatus(205, "Reset Content", "Success; the client should reset the document view, such as a form."),
        HttpStatus(206, "Partial Content", "Only the byte range asked for in the Range header is returned."),
        HttpStatus(207, "Multi-Status", "WebDAV: the body holds separate status codes for several resources."),
        HttpStatus(208, "Already Reported", "WebDAV: members of this binding were already listed earlier."),
        HttpStatus(226, "IM Used", "The response is the result of instance manipulations, such as a delta.")
    ),
    "3xx Redirection" to listOf(
        HttpStatus(300, "Multiple Choices", "The resource has several representations to choose from."),
        HttpStatus(301, "Moved Permanently", "The resource has a new permanent URL, given in Location."),
        HttpStatus(302, "Found", "The resource is temporarily at another URL, given in Location."),
        HttpStatus(303, "See Other", "Fetch the result from another URL with GET."),
        HttpStatus(304, "Not Modified", "The cached copy is still valid; no body is sent."),
        HttpStatus(305, "Use Proxy", "Deprecated: the resource must be accessed through a proxy."),
        HttpStatus(307, "Temporary Redirect", "Like 302, but the method and body must not change."),
        HttpStatus(308, "Permanent Redirect", "Like 301, but the method and body must not change.")
    ),
    "4xx Client error" to listOf(
        HttpStatus(400, "Bad Request", "The request is malformed or invalid."),
        HttpStatus(401, "Unauthorized", "Authentication is missing or invalid."),
        HttpStatus(402, "Payment Required", "Reserved; some APIs use it when payment is needed."),
        HttpStatus(403, "Forbidden", "Authenticated, but not allowed to access this resource."),
        HttpStatus(404, "Not Found", "No resource exists at this URL."),
        HttpStatus(405, "Method Not Allowed", "The resource does not support this HTTP method."),
        HttpStatus(406, "Not Acceptable", "No representation matches the Accept headers."),
        HttpStatus(407, "Proxy Authentication Required", "The client must authenticate with the proxy first."),
        HttpStatus(408, "Request Timeout", "The server gave up waiting for the request."),
        HttpStatus(409, "Conflict", "The request conflicts with the current state of the resource."),
        HttpStatus(410, "Gone", "The resource was removed permanently."),
        HttpStatus(411, "Length Required", "The request needs a Content-Length header."),
        HttpStatus(412, "Precondition Failed", "A conditional header, such as If-Match, did not hold."),
        HttpStatus(413, "Payload Too Large", "The request body is larger than the server allows."),
        HttpStatus(414, "URI Too Long", "The URL is longer than the server will process."),
        HttpStatus(415, "Unsupported Media Type", "The server does not accept this Content-Type."),
        HttpStatus(416, "Range Not Satisfiable", "The requested byte range is outside the resource."),
        HttpStatus(417, "Expectation Failed", "The Expect header cannot be met."),
        HttpStatus(418, "I'm a teapot", "April Fools' joke from RFC 2324; the server refuses to brew coffee."),
        HttpStatus(421, "Misdirected Request", "The request reached a server that cannot answer for this host."),
        HttpStatus(422, "Unprocessable Entity", "The syntax is valid, but the content fails validation."),
        HttpStatus(423, "Locked", "WebDAV: the resource is locked."),
        HttpStatus(424, "Failed Dependency", "WebDAV: an action this request depends on failed."),
        HttpStatus(425, "Too Early", "The server will not process a request that might be replayed."),
        HttpStatus(426, "Upgrade Required", "The client must switch to the protocol in the Upgrade header."),
        HttpStatus(428, "Precondition Required", "The request must be conditional, such as with If-Match."),
        HttpStatus(429, "Too Many Requests", "Rate limit hit; see Retry-After for when to try again."),
        HttpStatus(431, "Request Header Fields Too Large", "One header, or all headers together, are too large."),
        HttpStatus(451, "Unavailable For Legal Reasons", "Access is blocked for legal reasons, such as censorship.")
    ),
    "5xx Server error" to listOf(
        HttpStatus(500, "Internal Server Error", "An unexpected error occurred on the server."),
        HttpStatus(501, "Not Implemented", "The server does not support this functionality."),
        HttpStatus(502, "Bad Gateway", "A gateway or proxy got an invalid response from upstream."),
        HttpStatus(503, "Service Unavailable", "The server is overloaded or down for maintenance."),
        HttpStatus(504, "Gateway Timeout", "A gateway or proxy got no timely response from upstream."),
        HttpStatus(505, "HTTP Version Not Supported", "The server does not support this HTTP version."),
        HttpStatus(506, "Variant Also Negotiates", "Content negotiation is misconfigured into a loop."),
        HttpStatus(507, "Insufficient Storage", "WebDAV: the server cannot store what is needed to finish."),
        HttpStatus(508, "Loop Detected", "WebDAV: an infinite loop was found while processing."),
        HttpStatus(510, "Not Extended", "The request needs further extensions to be fulfilled."),
        HttpStatus(511, "Network Authentication Required", "Sign in to the network first, such as a captive portal.")
    )
)

internal fun filterStatuses(query: String): List<Pair<String, List<HttpStatus>>> {
    val q = query.trim()
    if (q.isEmpty()) return HTTP_STATUSES
    return HTTP_STATUSES.mapNotNull { (group, statuses) ->
        statuses.filter {
            it.code.toString().startsWith(q) || it.name.contains(q, ignoreCase = true) || it.meaning.contains(q, ignoreCase = true)
        }.takeIf { it.isNotEmpty() }?.let { group to it }
    }
}

internal fun filterHeaders(query: String): List<Pair<String, List<HttpHeader>>> {
    val q = query.trim()
    if (q.isEmpty()) return HTTP_HEADERS
    return HTTP_HEADERS.mapNotNull { (group, headers) ->
        headers.filter { it.name.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
            .takeIf { it.isNotEmpty() }?.let { group to it }
    }
}

private val TABS = listOf("Status", "Methods", "Headers")

class HttpStatusTool : Tool {
    override val descriptor = ToolDescriptor(
        "http-status",
        "HTTP Reference",
        "HTTP status codes, methods and headers",
        Icons.Default.Http,
        ToolCategory.NETWORK_SYSTEM,
        listOf("http", "status", "codes", "methods", "headers", "cors", "rfc")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(modifier: Modifier) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var query by rememberSaveable { mutableStateOf("") }
        var collapsed by rememberSaveable { mutableStateOf(listOf<String>()) }
        var detailCode by rememberSaveable { mutableStateOf<Int?>(null) }
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        val copy: (String, String) -> Unit = { text, what ->
            clipboardManager.setText(AnnotatedString(text))
            confirmCopied(context, what)
        }
        val q = query.trim()

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TABS.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = tab == index,
                        onClick = { tab = index },
                        shape = SegmentedButtonDefaults.itemShape(index, TABS.size)
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(8.dp))
            SearchField(
                query,
                { query = it },
                when (tab) { 0 -> "Search by code or name"; 1 -> "Search methods"; else -> "Search headers" }
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> {
                        val groups = filterStatuses(q)
                        if (groups.isEmpty()) item { EmptyHint("No status code matches \"$q\". Try a number like 404 or a word like redirect.") }
                        groups.forEach { (group, statuses) ->
                            // While searching every match is shown, so collapsing is disabled.
                            val isExpanded = q.isNotEmpty() || group !in collapsed
                            item(key = group) {
                                SectionHeader(group, isExpanded, onToggle = if (q.isNotEmpty()) null else {
                                    { collapsed = if (isExpanded) collapsed + group else collapsed - group }
                                })
                            }
                            if (isExpanded) {
                                items(statuses, key = { it.code }) { status ->
                                    StatusRow(
                                        status,
                                        onOpen = { detailCode = status.code },
                                        onCopy = { copy("${status.code} ${status.name}", "Status") }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        val methods = HTTP_METHODS.filter { q.isEmpty() || it.name.contains(q, true) || it.description.contains(q, true) }
                        if (methods.isEmpty()) item { EmptyHint("No method matches \"$q\". Try a name like PATCH or a word like tunnel.") }
                        items(methods, key = { it.name }) { MethodRow(it) { copy(it.name, "Method") } }
                    }
                    else -> {
                        val groups = filterHeaders(q)
                        if (groups.isEmpty()) item { EmptyHint("No header matches \"$q\". Try a name like Cache-Control or a word like cookie.") }
                        groups.forEach { (group, headers) ->
                            item(key = "h/$group") { SectionHeader(group, true, onToggle = null) }
                            items(headers, key = { "h/${it.name}" }) { header ->
                                HeaderRow(header) { copy("${header.name}: ${header.example}", "Header") }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        val detail = detailCode?.let { code -> HTTP_STATUSES.flatMap { it.second }.find { it.code == code } }
        if (detail != null) {
            ModalBottomSheet(onDismissRequest = { detailCode = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                StatusDetail(detail) { copy("${detail.code} ${detail.name}", "Status") }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusRow(status: HttpStatus, onOpen: () -> Unit, onCopy: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClickLabel = "Show details", onLongClickLabel = "Copy status line", onLongClick = onCopy, onClick = onOpen)
            .padding(vertical = 8.dp)
    ) {
        Text(
            status.code.toString(),
            style = CodeTextStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(48.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(status.name, style = MaterialTheme.typography.bodyLarge)
            Text(status.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusDetail(status: HttpStatus, onCopy: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Text("${status.code} ${status.name}", style = MaterialTheme.typography.titleLarge)
        Text(status.meaning, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        HTTP_STATUS_TIPS[status.code]?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        LabeledSlab(
            listOfNotNull(
                "Defined in" to httpSpec(status.code),
                HTTP_STATUS_HEADERS[status.code]?.let { "Related headers" to it },
                "Has a body" to if (httpHasBody(status.code)) "Yes" else "No",
                "Cacheable by default" to if (status.code in CACHEABLE_BY_DEFAULT) "Yes" else "No",
                "Worth retrying" to if (status.code in WORTH_RETRYING) "Yes, after a delay" else "No"
            ),
            Modifier.padding(top = 16.dp)
        )
        TextButton(onClick = onCopy, modifier = Modifier.padding(top = 8.dp)) { Text("Copy status line") }
    }
}

@Composable
private fun MethodRow(method: HttpMethod, onCopy: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Copy method", onClick = onCopy)
            .padding(vertical = 8.dp)
    ) {
        Text(method.name, style = CodeTextStyle, color = MaterialTheme.colorScheme.primary)
        Text(method.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Flag("Safe", if (method.safe) "Yes" else "No", Modifier.weight(1f))
            Flag("Idempotent", if (method.idempotent) "Yes" else "No", Modifier.weight(1f))
            Flag("Cacheable", method.cacheable, Modifier.weight(1f))
            Flag("Body", method.requestBody, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Flag(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun HeaderRow(header: HttpHeader, onCopy: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(header.name, style = CodeTextStyle, color = MaterialTheme.colorScheme.primary)
        Text(
            "${header.kind}. ${header.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        Surface(
            color = LocalSlabColor.current,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Copy header", onClick = onCopy)
        ) {
            Text(
                "${header.name}: ${header.example}",
                style = CodeTextStyle,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}
