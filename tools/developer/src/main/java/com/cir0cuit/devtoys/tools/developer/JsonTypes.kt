package com.cir0cuit.devtoys.tools.developer

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.util.IdentityHashMap

// ---- Inferred model: one pass over the JSON, shared by every language emitter. ----

internal sealed interface JType
internal data object TUnknown : JType // nothing seen yet (empty array)
internal data object TNull : JType
internal data object TBool : JType
internal data object TInt : JType
internal data object TLong : JType
internal data object TDouble : JType
internal data object TStr : JType
internal data object TMixed : JType // conflicting kinds, rendered as the language's "any"
internal data class TNullable(val type: JType) : JType
internal data class TArr(val item: JType) : JType
internal class TObj(val fields: Map<String, JField>) : JType
internal data class JField(val type: JType, val optional: Boolean)

/** ponytail: only the first 500 array elements are merged; enough for real API samples. */
private const val MAX_MERGED_ELEMENTS = 500

internal fun inferType(v: Any?): JType = when (v) {
    null, JSONObject.NULL -> TNull
    is Boolean -> TBool
    is Int -> TInt
    is Long -> TLong
    is Number -> TDouble
    is String -> TStr
    is JSONObject -> TObj(LinkedHashMap<String, JField>().also { m -> v.keys().forEach { k -> m[k] = JField(inferType(v.opt(k)), false) } })
    is JSONArray -> TArr((0 until minOf(v.length(), MAX_MERGED_ELEMENTS)).fold<Int, JType>(TUnknown) { acc, i -> mergeTypes(acc, inferType(v.opt(i))) })
    else -> TStr
}

private fun nullable(t: JType) = if (t is TNullable || t is TNull) t else TNullable(t)

/** Widens two observed types into one: missing keys become optional, nulls nullable, Int+Double Double. */
internal fun mergeTypes(a: JType, b: JType): JType = when {
    a == b -> a
    a is TUnknown -> b
    b is TUnknown -> a
    a is TNull -> nullable(b)
    b is TNull -> nullable(a)
    a is TNullable -> nullable(mergeTypes(a.type, b))
    b is TNullable -> nullable(mergeTypes(a, b.type))
    a is TArr && b is TArr -> TArr(mergeTypes(a.item, b.item))
    a is TObj && b is TObj -> TObj(LinkedHashMap<String, JField>().also { m ->
        for (k in a.fields.keys + b.fields.keys) {
            val fa = a.fields[k]
            val fb = b.fields[k]
            m[k] = if (fa != null && fb != null) JField(mergeTypes(fa.type, fb.type), fa.optional || fb.optional)
            else JField((fa ?: fb)!!.type, true)
        }
    })
    a in NUMBERS && b in NUMBERS -> if (a == TDouble || b == TDouble) TDouble else TLong
    else -> TMixed
}

private val NUMBERS = setOf(TInt, TLong, TDouble)

// ---- Names ----

private fun splitWords(key: String): List<String> = key.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }

internal fun toPropertyName(key: String): String {
    val words = splitWords(key)
    var name = if (words.isEmpty()) "field" else
        words.first().replaceFirstChar { it.lowercase() } + words.drop(1).joinToString("") { w -> w.replaceFirstChar { it.uppercase() } }
    if (name.first().isDigit()) name = "_$name"
    return name
}

internal fun toClassName(key: String): String {
    val words = splitWords(key)
    var name = if (words.isEmpty()) "Field" else words.joinToString("") { w -> w.replaceFirstChar { it.uppercase() } }
    if (name.first().isDigit()) name = "_$name"
    return name
}

internal fun toSnakeName(key: String): String {
    val words = splitWords(key.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2"))
    var name = if (words.isEmpty()) "field" else words.joinToString("_") { it.lowercase() }
    if (name.first().isDigit()) name = "_$name"
    return name
}

internal fun uniqueName(base: String, used: MutableSet<String>): String {
    var name = base
    var i = 2
    while (!used.add(name)) name = base + i++
    return name
}

/** Bracket depth outside strings; guards the recursive inference against stack overflow. */
internal fun maxDepth(s: String): Int {
    var depth = 0
    var max = 0
    var inString = false
    var escaped = false
    for (c in s) {
        if (inString) {
            if (escaped) escaped = false
            else if (c == '\\') escaped = true
            else if (c == '"') inString = false
            continue
        }
        when (c) {
            '"' -> inString = true
            '{', '[' -> { depth++; if (depth > max) max = depth }
            '}', ']' -> depth--
        }
    }
    return max
}

/** Type names that would shadow a builtin in at least one target language. */
private val RESERVED_TYPE_NAMES = setOf(
    "Any", "Unit", "Nothing", "String", "Char", "Boolean", "Bool", "Byte", "Short", "Int", "Integer", "Long",
    "Float", "Double", "Number", "Array", "List", "Map", "Set", "Pair", "Triple", "Result", "Object", "Date",
    "Error", "Type", "Self", "Optional", "Option", "Vec", "Box", "Record", "Dictionary", "Function", "Null",
    "None", "True", "False", "Value", "SerialName", "Serializable", "Json", "JsonClass", "JsonElement", "JsonObject",
    "Codable", "CodingKeys"
)

internal class TypeModel(val root: JType, val classes: List<Pair<String, TObj>>, val names: Map<TObj, String>)

/** Names every non-empty object pre-order (root first, then its children depth-first). */
internal fun buildTypeModel(root: JType, rootName: String = "Root"): TypeModel {
    val used = mutableSetOf<String>()
    val classes = mutableListOf<Pair<String, TObj>>()
    val names = IdentityHashMap<TObj, String>()
    fun visit(t: JType, base: String) {
        when (t) {
            is TNullable -> visit(t.type, base)
            is TArr -> visit(t.item, base + "Item")
            is TObj -> if (t.fields.isNotEmpty()) {
                val name = uniqueName(if (base in RESERVED_TYPE_NAMES) base + "Type" else base, used)
                names[t] = name
                classes += name to t
                t.fields.forEach { (k, f) -> visit(f.type, toClassName(k)) }
            }
            else -> {}
        }
    }
    used += rootName
    if (root is TObj && root.fields.isNotEmpty()) {
        names[root] = rootName
        classes += rootName to root
        root.fields.forEach { (k, f) -> visit(f.type, toClassName(k)) }
    } else visit(root, rootName)
    return TypeModel(root, classes, names)
}

// ---- Emitters ----

internal enum class TypeLang(val label: String) {
    KOTLIN("Kotlin"), TYPESCRIPT("TypeScript"), SWIFT("Swift"), GO("Go"), JAVA("Java"),
    PYTHON("Python"), CSHARP("C#"), DART("Dart"), RUST("Rust")
}

internal enum class KotlinLib(val label: String) { KOTLINX("kotlinx"), MOSHI("Moshi"), GSON("Gson"), NONE("None") }
internal enum class JavaLib(val label: String) { JACKSON("Jackson"), GSON("Gson"), NONE("None") }

internal data class TypeOptions(
    val kotlinLib: KotlinLib = KotlinLib.KOTLINX,
    val javaLib: JavaLib = JavaLib.JACKSON,
    val allNullable: Boolean = false,
    /** Kotlin var, Swift var, Dart non-final. */
    val mutable: Boolean = false,
    /** TypeScript only: rename keys to camelCase. */
    val camelCase: Boolean = false
)

private class TypeSyntax(
    val bool: String, val int: String, val long: String, val double: String, val str: String,
    val any: String, val map: String, val list: (String) -> String, val opt: (String) -> String
)

private fun TypeSyntax.render(t: JType, names: Map<TObj, String>): String = when (t) {
    TBool -> bool
    TInt -> int
    TLong -> long
    TDouble -> double
    TStr -> str
    TUnknown, TMixed -> any
    TNull -> opt(any)
    is TNullable -> opt(render(t.type, names))
    is TArr -> list(render(t.item, names))
    is TObj -> names[t] ?: map
}

private val JAVA_BOX = mapOf("boolean" to "Boolean", "int" to "Integer", "long" to "Long", "double" to "Double")

private fun syntaxFor(lang: TypeLang, opts: TypeOptions): TypeSyntax = when (lang) {
    // kotlinx.serialization has no serializer for Any, so unknown values use its JSON tree types.
    TypeLang.KOTLIN -> if (opts.kotlinLib == KotlinLib.KOTLINX)
        TypeSyntax("Boolean", "Int", "Long", "Double", "String", "JsonElement", "JsonObject", { "List<$it>" }, { if (it.endsWith("?")) it else "$it?" })
    else TypeSyntax("Boolean", "Int", "Long", "Double", "String", "Any", "Map<String, Any?>", { "List<$it>" }, { if (it.endsWith("?")) it else "$it?" })
    TypeLang.TYPESCRIPT -> TypeSyntax("boolean", "number", "number", "number", "string", "unknown", "Record<string, unknown>",
        { if (' ' in it) "($it)[]" else "$it[]" }, { if (it == "unknown" || it.endsWith("| null")) it else "$it | null" })
    TypeLang.SWIFT -> TypeSyntax("Bool", "Int", "Int", "Double", "String", "AnyCodable", "[String: AnyCodable]", { "[$it]" }, { if (it.endsWith("?")) it else "$it?" })
    TypeLang.GO -> TypeSyntax("bool", "int", "int64", "float64", "string", "any", "map[string]any", { "[]$it" },
        { if (it == "any" || it.startsWith("*") || it.startsWith("[]") || it.startsWith("map[")) it else "*$it" })
    TypeLang.JAVA -> TypeSyntax("boolean", "int", "long", "double", "String", "Object", "Map<String, Object>", { "List<${JAVA_BOX[it] ?: it}>" }, { JAVA_BOX[it] ?: it })
    TypeLang.PYTHON -> TypeSyntax("bool", "int", "int", "float", "str", "Any", "dict[str, Any]", { "list[$it]" }, { if (it == "Any" || it.endsWith("| None")) it else "$it | None" })
    TypeLang.CSHARP -> TypeSyntax("bool", "int", "long", "double", "string", "object", "Dictionary<string, object>", { "List<$it>" }, { if (it.endsWith("?")) it else "$it?" })
    TypeLang.DART -> TypeSyntax("bool", "int", "int", "double", "String", "dynamic", "Map<String, dynamic>", { "List<$it>" }, { if (it == "dynamic" || it.endsWith("?")) it else "$it?" })
    TypeLang.RUST -> TypeSyntax("bool", "i64", "i64", "f64", "String", "serde_json::Value", "serde_json::Map<String, serde_json::Value>", { "Vec<$it>" },
        { if (it == "serde_json::Value" || it.startsWith("Option<")) it else "Option<$it>" })
}

/** Double-quoted string literal body; [dollar] also escapes `$` for Kotlin and Dart templates. */
internal fun quoteLiteral(s: String, dollar: Boolean = false) = buildString {
    for (c in s) when (c) {
        '\\' -> append("\\\\")
        '"' -> append("\\\"")
        '$' -> append(if (dollar) "\\\$" else "$")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
    }
}

private val KOTLIN_KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null",
    "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while"
)
private val SWIFT_KEYWORDS = setOf(
    "associatedtype", "class", "deinit", "enum", "extension", "func", "import", "init", "inout", "let", "operator",
    "protocol", "static", "struct", "subscript", "typealias", "var", "break", "case", "continue", "default", "defer",
    "do", "else", "fallthrough", "for", "guard", "if", "in", "repeat", "return", "switch", "where", "while", "as",
    "catch", "false", "is", "nil", "super", "self", "throw", "throws", "true", "try", "Type", "Self"
)
private val JAVA_KEYWORDS = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default",
    "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import",
    "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return",
    "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
    "void", "volatile", "while", "true", "false", "null", "record", "var", "yield"
)
private val PYTHON_KEYWORDS = setOf(
    "false", "none", "true", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif",
    "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or",
    "pass", "raise", "return", "try", "while", "with", "yield"
)
private val DART_KEYWORDS = setOf(
    "assert", "break", "case", "catch", "class", "const", "continue", "default", "do", "else", "enum", "extends",
    "false", "final", "finally", "for", "if", "in", "is", "new", "null", "rethrow", "return", "super", "switch", "this",
    "throw", "true", "try", "var", "void", "while", "with", "required", "late", "dynamic"
)
private val RUST_KEYWORDS = setOf(
    "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
    "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "static", "struct", "super", "trait",
    "true", "type", "unsafe", "use", "where", "while", "async", "await", "dyn", "abstract", "become", "box", "do",
    "final", "macro", "override", "priv", "typeof", "unsized", "virtual", "yield", "try"
)

private class TypeProp(val key: String, val name: String, val type: String, val nullable: Boolean, val optional: Boolean) {
    val renamed get() = name.trim('`') != key
}

/** Field name and rendered type per language, unique within the class. */
private fun props(obj: TObj, lang: TypeLang, syntax: TypeSyntax, model: TypeModel, opts: TypeOptions, className: String): List<TypeProp> {
    val used = mutableSetOf<String>()
    return obj.fields.map { (key, f) ->
        var t = f.type
        // TypeScript marks missing keys with `?` instead of `| null`.
        val nullable = opts.allNullable || (f.optional && lang != TypeLang.TYPESCRIPT)
        if (nullable && t !is TNullable && t !is TNull) t = TNullable(t)
        val base = when (lang) {
            TypeLang.KOTLIN, TypeLang.SWIFT, TypeLang.JAVA, TypeLang.DART -> toPropertyName(key)
            TypeLang.TYPESCRIPT -> if (opts.camelCase) toPropertyName(key) else key
            TypeLang.GO, TypeLang.CSHARP -> toClassName(key).let { if (lang == TypeLang.CSHARP && it == className) it + "Value" else it }
            TypeLang.PYTHON, TypeLang.RUST -> toSnakeName(key)
        }
        val unique = if (lang == TypeLang.TYPESCRIPT && !opts.camelCase) base else uniqueName(base, used)
        val name = when (lang) {
            TypeLang.KOTLIN -> if (unique in KOTLIN_KEYWORDS) "`$unique`" else unique
            TypeLang.SWIFT -> if (unique in SWIFT_KEYWORDS) "`$unique`" else unique
            TypeLang.JAVA -> if (unique in JAVA_KEYWORDS) unique + "_" else unique
            TypeLang.PYTHON -> if (unique.lowercase() in PYTHON_KEYWORDS) unique + "_" else unique
            TypeLang.DART -> if (unique in DART_KEYWORDS) unique + "_" else unique
            TypeLang.RUST -> if (unique in RUST_KEYWORDS) unique + "_" else unique
            TypeLang.TYPESCRIPT -> if (Regex("[A-Za-z_$][A-Za-z0-9_$]*").matches(unique)) unique else "\"${quoteLiteral(unique)}\""
            else -> unique
        }
        TypeProp(key, name, syntax.render(t, model.names), t is TNullable || t is TNull, f.optional)
    }
}

private const val MAX_DEPTH = 512

/** Generated [code], or an [error] saying what is wrong with the JSON. */
internal data class TypesResult(val code: String = "", val error: String? = null)

internal fun convertJsonToTypes(input: String, lang: TypeLang, opts: TypeOptions): TypesResult =
    if (input.isBlank()) TypesResult() else try {
        if (maxDepth(input) > MAX_DEPTH) TypesResult(error = "JSON is nested too deeply. Keep it under $MAX_DEPTH levels.")
        else TypesResult(generateTypes(input, lang, opts))
    } catch (e: StackOverflowError) {
        TypesResult(error = "JSON is nested too deeply to convert.")
    } catch (e: Exception) {
        TypesResult(error = "Not valid JSON: ${e.message ?: "check brackets, quotes and commas"}")
    }

/** Generates type declarations for [json] (an object or array text) in [lang]. Throws on invalid JSON. */
internal fun generateTypes(json: String, lang: TypeLang, opts: TypeOptions = TypeOptions()): String =
    emitTypes(buildTypeModel(inferType(parseJsonRoot(json))), lang, opts)

/** The object or array in [json]; anything after it is an error. Error messages give the line and column, never the input. */
internal fun parseJsonRoot(json: String): Any {
    val tokener = JSONTokener(json)
    val value = try {
        tokener.nextValue().also { if (tokener.nextClean() != '\u0000') throw tokener.syntaxError("Unexpected text after the end of the JSON") }
    } catch (e: JSONException) {
        throw IllegalArgumentException(jsonErrorAt(json, e.message.orEmpty()))
    }
    require(value is JSONObject || value is JSONArray) { "the top level must be an object or an array." }
    return value
}

/**
 * Turns an org.json message into "line L, column C: problem.". Android's message ends in
 * "at character N of <input>", the JVM artifact's in "at N [character C line L]"; both carry the offset N.
 */
internal fun jsonErrorAt(json: String, message: String): String {
    val m = Regex("""^(.*?) at (?:character )?(\d+)""", RegexOption.DOT_MATCHES_ALL).find(message)
        ?: return "check brackets, quotes and commas."
    // N counts the characters read, so the offending one is at N - 1.
    val offset = (m.groupValues[2].toInt() - 1).coerceIn(0, json.length)
    val lineStart = json.lastIndexOf('\n', offset - 1) + 1
    val line = json.substring(0, offset).count { it == '\n' } + 1
    return "line $line, column ${offset - lineStart + 1}: ${m.groupValues[1].trimEnd('.')}."
}

internal fun emitTypes(model: TypeModel, lang: TypeLang, opts: TypeOptions): String {
    val syntax = syntaxFor(lang, opts)
    val rootIsClass = model.root is TObj && model.names[model.root] == "Root"
    val rootType = syntax.render(model.root, model.names)
    val out = StringBuilder()
    fun header(vararg lines: String) { lines.forEach { out.append(it).append('\n') }; out.append('\n') }
    val blocks = model.classes.map { (name, obj) -> name to props(obj, lang, syntax, model, opts, name) }
    val anyRenamed = blocks.any { (_, ps) -> ps.any { it.renamed } }

    when (lang) {
        TypeLang.KOTLIN -> {
            val kw = if (opts.mutable) "var" else "val"
            if (opts.kotlinLib == KotlinLib.KOTLINX) {
                val types = blocks.flatMap { (_, ps) -> ps.map { it.type } } + rootType
                fun uses(type: String) = types.any { Regex("\\b$type\\b").containsMatchIn(it) }
                val imports = listOfNotNull(
                    "import kotlinx.serialization.SerialName".takeIf { anyRenamed },
                    "import kotlinx.serialization.Serializable".takeIf { blocks.isNotEmpty() },
                    "import kotlinx.serialization.json.JsonElement".takeIf { uses("JsonElement") },
                    "import kotlinx.serialization.json.JsonObject".takeIf { uses("JsonObject") }
                )
                if (imports.isNotEmpty()) header(*imports.toTypedArray())
            }
            blocks.forEach { (name, ps) ->
                when (opts.kotlinLib) {
                    KotlinLib.KOTLINX -> out.append("@Serializable\n")
                    KotlinLib.MOSHI -> out.append("@JsonClass(generateAdapter = true)\n")
                    else -> {}
                }
                out.append("data class $name(\n")
                ps.forEach { p ->
                    if (p.renamed) when (opts.kotlinLib) {
                        KotlinLib.KOTLINX -> out.append("    @SerialName(\"${quoteLiteral(p.key, true)}\")\n")
                        KotlinLib.MOSHI -> out.append("    @Json(name = \"${quoteLiteral(p.key, true)}\")\n")
                        KotlinLib.GSON -> out.append("    @SerializedName(\"${quoteLiteral(p.key, true)}\")\n")
                        KotlinLib.NONE -> {}
                    }
                    out.append("    $kw ${p.name}: ${p.type}${if (p.nullable) " = null" else ""},\n")
                }
                out.append(")\n\n")
            }
            if (!rootIsClass) out.append("typealias Root = $rootType\n")
        }
        TypeLang.TYPESCRIPT -> {
            blocks.forEach { (name, ps) ->
                out.append("export interface $name {\n")
                ps.forEach { p -> out.append("  ${p.name}${if (p.optional) "?" else ""}: ${p.type};\n") }
                out.append("}\n\n")
            }
            if (!rootIsClass) out.append("export type Root = $rootType;\n")
        }
        TypeLang.SWIFT -> {
            val kw = if (opts.mutable) "var" else "let"
            blocks.forEach { (name, ps) ->
                out.append("struct $name: Codable {\n")
                ps.forEach { p -> out.append("    $kw ${p.name}: ${p.type}\n") }
                if (ps.any { it.renamed }) {
                    out.append("\n    enum CodingKeys: String, CodingKey {\n")
                    ps.forEach { p -> out.append("        case ${p.name}" + (if (p.renamed) " = \"${quoteLiteral(p.key)}\"" else "") + "\n") }
                    out.append("    }\n")
                }
                out.append("}\n\n")
            }
            if (!rootIsClass) out.append("typealias Root = $rootType\n")
        }
        TypeLang.GO -> {
            blocks.forEach { (name, ps) ->
                out.append("type $name struct {\n")
                ps.forEach { p ->
                    val tag = quoteLiteral(p.key).replace("`", "") + if (p.optional) ",omitempty" else ""
                    out.append("\t${p.name} ${p.type} `json:\"$tag\"`\n")
                }
                out.append("}\n\n")
            }
            if (!rootIsClass) out.append("type Root $rootType\n")
        }
        TypeLang.JAVA -> {
            if (opts.javaLib == JavaLib.JACKSON && anyRenamed) header("import com.fasterxml.jackson.annotation.JsonProperty;")
            if (opts.javaLib == JavaLib.GSON && anyRenamed) header("import com.google.gson.annotations.SerializedName;")
            blocks.forEach { (name, ps) ->
                out.append("public record $name(\n")
                ps.forEachIndexed { i, p ->
                    val ann = if (!p.renamed) "" else when (opts.javaLib) {
                        JavaLib.JACKSON -> "@JsonProperty(\"${quoteLiteral(p.key)}\") "
                        JavaLib.GSON -> "@SerializedName(\"${quoteLiteral(p.key)}\") "
                        JavaLib.NONE -> ""
                    }
                    out.append("    $ann${p.type} ${p.name}${if (i < ps.lastIndex) "," else ""}\n")
                }
                out.append(") {}\n\n")
            }
            if (!rootIsClass) out.append("// Root: $rootType\n")
        }
        TypeLang.PYTHON -> {
            header("from __future__ import annotations", "", "from dataclasses import dataclass", "from typing import Any")
            blocks.forEach { (name, ps) ->
                out.append("@dataclass(kw_only=True)\nclass $name:\n")
                ps.forEach { p ->
                    out.append("    ${p.name}: ${p.type}${if (p.nullable) " = None" else ""}")
                    out.append(if (p.renamed) "  # JSON key: \"${quoteLiteral(p.key)}\"\n" else "\n")
                }
                out.append("\n\n")
            }
            if (!rootIsClass) out.append("Root = $rootType\n")
        }
        TypeLang.CSHARP -> {
            header("using System.Collections.Generic;", "using System.Text.Json.Serialization;")
            blocks.forEach { (name, ps) ->
                out.append("public class $name\n{\n")
                ps.forEachIndexed { i, p ->
                    if (i > 0) out.append('\n')
                    out.append("    [JsonPropertyName(\"${quoteLiteral(p.key)}\")]\n")
                    out.append("    public ${p.type} ${p.name} { get; set; }\n")
                }
                out.append("}\n\n")
            }
            if (!rootIsClass) out.append("// Root: $rootType\n")
        }
        TypeLang.DART -> {
            header("import 'package:json_annotation/json_annotation.dart';", "", "part 'model.g.dart';")
            val kw = if (opts.mutable) "" else "final "
            blocks.forEach { (name, ps) ->
                out.append("@JsonSerializable()\nclass $name {\n")
                ps.forEach { p ->
                    if (p.renamed) out.append("  @JsonKey(name: \"${quoteLiteral(p.key, true)}\")\n")
                    out.append("  $kw${p.type} ${p.name};\n")
                }
                out.append("\n  $name({${ps.joinToString(", ") { (if (it.nullable || it.type == "dynamic") "" else "required ") + "this.${it.name}" }}});\n\n")
                out.append("  factory $name.fromJson(Map<String, dynamic> json) => _\$${name}FromJson(json);\n")
                out.append("  Map<String, dynamic> toJson() => _\$${name}ToJson(this);\n")
                out.append("}\n\n")
            }
            if (!rootIsClass) out.append("typedef Root = $rootType;\n")
        }
        TypeLang.RUST -> {
            header("use serde::{Deserialize, Serialize};")
            blocks.forEach { (name, ps) ->
                out.append("#[derive(Debug, Clone, Serialize, Deserialize)]\npub struct $name {\n")
                ps.forEach { p ->
                    if (p.renamed) out.append("    #[serde(rename = \"${quoteLiteral(p.key)}\")]\n")
                    out.append("    pub ${p.name}: ${p.type},\n")
                }
                out.append("}\n\n")
            }
            if (!rootIsClass) out.append("pub type Root = $rootType;\n")
        }
    }
    return out.toString().trimEnd()
}
