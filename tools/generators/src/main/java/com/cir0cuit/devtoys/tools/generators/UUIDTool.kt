package com.cir0cuit.devtoys.tools.generators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.SelectField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.security.SecureRandom
import java.util.UUID

private val UUID_COUNTS = listOf(1, 5, 25, 100)
private const val CUSTOM_NAMESPACE = "Custom"

/** Batch of [count] UUIDs; time-based ones are sorted so the batch is monotonic. */
internal fun generateUuids(version: UuidVersion, count: Int, namespace: UUID?, name: String, nowMs: Long, rng: java.util.Random): List<UUID> =
    when (version) {
        UuidVersion.V4 -> List(count) { UUID.randomUUID() }
        // ponytail: sorting the batch gives in-batch monotonicity; no cross-batch counter.
        UuidVersion.V7 -> List(count) { uuidV7(nowMs, rng) }.sortedBy { it.toString() }
        UuidVersion.V1 -> List(count) { uuidV1(nowMs, rng) }
        UuidVersion.V5, UuidVersion.V3 -> listOfNotNull(namespace?.let { uuidNameBased(it, name, version == UuidVersion.V5) })
        UuidVersion.NIL -> listOf(UUID_NIL)
        UuidVersion.MAX -> listOf(UUID_MAX)
    }

internal fun uuidListAsJson(items: List<String>) = items.joinToString(",\n  ", "[\n  ", "\n]") { "\"$it\"" }

class UUIDTool : Tool {
    override val descriptor: ToolDescriptor = ToolDescriptor(
        id = "uuid",
        title = "UUID Generator",
        description = "Generate and decode UUIDs v1-v7",
        icon = Icons.Default.Fingerprint,
        category = ToolCategory.GENERATORS,
        keywords = listOf("uuid", "guid", "generate", "v7", "v5", "decode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var tab by rememberSaveable { mutableStateOf(0) }
        ToolScaffold(title = descriptor.title, modifier = modifier) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Generate") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Decode") })
            }
            Spacer(Modifier.height(16.dp))
            if (tab == 0) GeneratePane() else InspectPane("UUID")
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun GeneratePane() {
        var version by rememberSaveable { mutableStateOf(UuidVersion.V4) }
        var count by rememberSaveable { mutableStateOf(1) }
        var namespaceKey by rememberSaveable { mutableStateOf("DNS") }
        var customNamespace by rememberSaveable { mutableStateOf("") }
        var name by rememberSaveable { mutableStateOf("") }
        var format by rememberSaveable { mutableStateOf(UuidFormat.STANDARD) }
        var isUppercase by rememberSaveable { mutableStateOf(false) }
        var hyphens by rememberSaveable { mutableStateOf(true) }
        var asJson by rememberSaveable { mutableStateOf(false) }
        var nonce by rememberSaveable { mutableStateOf(0) }

        val nameBased = version == UuidVersion.V3 || version == UuidVersion.V5
        val namespace = if (namespaceKey == CUSTOM_NAMESPACE) parseUuid(customNamespace) else parseUuid(UUID_NAMESPACES.getValue(namespaceKey))
        val raw: List<String> = rememberSaveable(version, count, namespace, name, nonce) {
            ArrayList(generateUuids(version, count, namespace, name, System.currentTimeMillis(), SecureRandom()).map { it.toString() })
        }
        val formatted = raw.map { formatUuid(UUID.fromString(it), format, isUppercase, hyphens) }
        val output = if (asJson) uuidListAsJson(formatted) else formatted.joinToString("\n")

        SelectField("Version", version, UuidVersion.entries.toList(), { version = it }, optionLabel = { it.label })
        if (nameBased) {
            Spacer(Modifier.height(8.dp))
            SelectField("Namespace", namespaceKey, UUID_NAMESPACES.keys.toList() + CUSTOM_NAMESPACE, { namespaceKey = it })
            if (namespaceKey == CUSTOM_NAMESPACE) {
                Spacer(Modifier.height(8.dp))
                SingleLineField(customNamespace, { customNamespace = it }, "Namespace UUID", isError = customNamespace.isNotEmpty() && namespace == null)
            }
            Spacer(Modifier.height(8.dp))
            SingleLineField(name, { name = it }, "Name")
        } else if (version != UuidVersion.NIL && version != UuidVersion.MAX) {
            Spacer(Modifier.height(16.dp))
            Text("How many", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ChoiceRow(UUID_COUNTS, count, { count = it })
        }
        Spacer(Modifier.height(16.dp))
        Text("Format", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ChoiceRow(UuidFormat.entries.toList(), format, { format = it }, { it.label })
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (format != UuidFormat.BASE64) {
                FilterChip(selected = isUppercase, onClick = { isUppercase = !isUppercase }, label = { Text("Uppercase") })
                FilterChip(selected = hyphens, onClick = { hyphens = !hyphens }, label = { Text("Hyphens") })
            }
            FilterChip(selected = asJson, onClick = { asJson = !asJson }, label = { Text("JSON array") })
        }
        Spacer(Modifier.height(16.dp))
        RegenerateButton("Generate", enabled = !nameBased) { nonce++ }
        Spacer(Modifier.height(16.dp))
        if (nameBased && namespace == null) {
            if (customNamespace.isEmpty()) {
                Text("Enter the namespace UUID.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("The custom namespace is not a valid UUID. Enter 32 hex digits, with or without hyphens.", color = MaterialTheme.colorScheme.error)
            }
        } else {
            // Unwrapped: a quoted JSON item is wider than a phone panel and would break across lines.
            UnwrappedOutputPanel(content = output, label = if (raw.size == 1) "UUID" else "${raw.size} UUIDs")
        }
    }
}

/** Paste-an-ID pane shared by the UUID and ID tools; decoding is cheap, so it runs inline. */
@Composable
internal fun InspectPane(what: String) {
    var input by rememberSaveable { mutableStateOf("") }
    val result = if (input.isNotBlank()) runCatching { inspectId(input) } else null
    SingleLineField(input.take(200), { input = it.take(200) }, what, isError = result?.isFailure == true)
    Spacer(Modifier.height(16.dp))
    if (result != null) {
        result.onSuccess { LabeledValueSlab(it) }
        result.onFailure { Text(it.message ?: "Not a recognized ID.", color = MaterialTheme.colorScheme.error) }
    }
}
