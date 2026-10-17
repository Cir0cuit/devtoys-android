package com.cir0cuit.devtoys.tools.developer

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uses the real org.json artifact declared by the module (on device the platform copy is used).
 * The JVM artifact does not keep key order, so multi-key samples only assert with contains().
 */
class JsonToTypesTest {
    private fun gen(json: String, lang: TypeLang = TypeLang.KOTLIN, opts: TypeOptions = TypeOptions(kotlinLib = KotlinLib.KOTLINX)) =
        generateTypes(json, lang, opts)

    private fun assertHas(out: String, vararg parts: String) = parts.forEach { assertTrue("missing <$it> in:\n$out", out.contains(it)) }

    // ---- identifiers ----

    @Test fun propertyNamesAreLowerCamelCase() {
        assertEquals("firstName", toPropertyName("first_name"))
        assertEquals("userId", toPropertyName("user-id"))
        assertEquals("fooBarBaz", toPropertyName("foo bar.baz"))
        assertEquals("ref", toPropertyName("\$ref"))
        assertEquals("_123abc", toPropertyName("123abc"))
        assertEquals("field", toPropertyName("!!!"))
    }

    @Test fun classAndSnakeNames() {
        assertEquals("FirstName", toClassName("first_name"))
        assertEquals("_2fa", toClassName("2fa"))
        assertEquals("Field", toClassName("--"))
        assertEquals("first_name", toSnakeName("firstName"))
        assertEquals("user_id", toSnakeName("user-id"))
    }

    @Test fun uniqueNameAppendsCounter() {
        val used = mutableSetOf("A")
        assertEquals("A2", uniqueName("A", used))
        assertEquals("A3", uniqueName("A", used))
        assertEquals("B", uniqueName("B", used))
    }

    @Test fun maxDepthIgnoresBracketsInStrings() {
        assertEquals(0, maxDepth(""))
        assertEquals(3, maxDepth("[[[]]]"))
        assertEquals(1, maxDepth("{\"a\":\"[[[{{{\"}"))
        assertEquals(1, maxDepth("{\"a\":\"\\\"[[[\"}"))
    }

    // ---- inference ----

    @Test fun arrayElementsAreMerged() {
        val t = inferType(JSONArray("[{\"a\":1,\"b\":null},{\"a\":2.5,\"b\":\"x\",\"c\":true}]")) as TArr
        val o = t.item as TObj
        assertEquals(JField(TDouble, false), o.fields["a"])
        assertEquals(JField(TNullable(TStr), false), o.fields["b"])
        assertEquals(JField(TBool, true), o.fields["c"])
    }

    @Test fun mixedPrimitivesBecomeAny() {
        assertEquals(TArr(TMixed), inferType(JSONArray("[1,\"a\"]")))
        assertEquals(TArr(TLong), inferType(JSONArray("[1,12345678901]")))
        assertEquals(TArr(TArr(TNullable(TInt))), inferType(JSONArray("[[1],[null]]")))
        assertEquals(TArr(TUnknown), inferType(JSONArray("[]")))
    }

    // ---- Kotlin ----

    @Test fun kotlinBasicTypes() {
        val out = gen("{\"id\":1,\"name\":\"x\",\"ok\":true,\"big\":12345678901,\"nothing\":null}")
        assertTrue(out, out.startsWith("import kotlinx.serialization.Serializable\nimport kotlinx.serialization.json.JsonElement\n\n@Serializable\ndata class Root(\n"))
        assertHas(out, "    val id: Int,\n", "    val name: String,\n", "    val ok: Boolean,\n", "    val big: Long,\n", "    val nothing: JsonElement? = null,\n")
    }

    @Test fun kotlinxUsesJsonTreeTypesNotAny() {
        val out = gen("{\"email\":null,\"meta\":{},\"user_id\":1}")
        assertHas(out, "import kotlinx.serialization.SerialName\n", "import kotlinx.serialization.json.JsonObject\n",
            "val email: JsonElement? = null,", "val meta: JsonObject,")
        assertFalse(out, out.contains("Any"))
        assertEquals("import kotlinx.serialization.json.JsonElement\n\ntypealias Root = List<JsonElement>", gen("[1,\"a\"]"))
        // Moshi and Gson handle Any themselves.
        assertHas(gen("{\"meta\":{}}", opts = TypeOptions(kotlinLib = KotlinLib.MOSHI)), "val meta: Map<String, Any?>,")
        assertFalse(gen("{\"meta\":{}}", opts = TypeOptions(kotlinLib = KotlinLib.GSON)).contains("import"))
    }

    @Test fun kotlinAnnotationsOnlyWhenRenamed() {
        assertHas(gen("{\"first_name\":\"x\"}"), "    @SerialName(\"first_name\")\n    val firstName: String,")
        assertFalse(gen("{\"name\":\"x\"}").contains("@SerialName"))
        assertHas(gen("{\"first_name\":\"x\"}", opts = TypeOptions(kotlinLib = KotlinLib.MOSHI)), "@JsonClass(generateAdapter = true)", "@Json(name = \"first_name\")")
        assertHas(gen("{\"first_name\":\"x\"}", opts = TypeOptions(kotlinLib = KotlinLib.GSON)), "@SerializedName(\"first_name\")")
        assertFalse(gen("{\"first_name\":\"x\"}", opts = TypeOptions(kotlinLib = KotlinLib.NONE)).contains("@"))
    }

    @Test fun kotlinKeywordsAndEscapes() {
        val out = gen("{\"class\":\"x\"}")
        assertHas(out, "val `class`: String,")
        assertFalse(out, out.contains("@SerialName"))
        assertHas(gen("{\"\$ref\":\"x\"}"), "@SerialName(\"\\\$ref\")")
        assertHas(gen("{\"a\\nb\":\"x\"}"), "@SerialName(\"a\\nb\")")
    }

    @Test fun kotlinOptionalAndMutable() {
        val out = gen("{\"items\":[{\"id\":1},{\"id\":2,\"note\":\"n\"}]}", opts = TypeOptions(mutable = true, allNullable = false))
        assertHas(out, "var items: List<ItemsItem>,", "data class ItemsItem(", "var id: Int,", "var note: String? = null,")
        assertHas(gen("{\"id\":1}", opts = TypeOptions(allNullable = true)), "val id: Int? = null,")
    }

    @Test fun kotlinClassNaming() {
        assertHas(gen("{\"root\":{\"a\":1}}"), "val root: Root2,", "data class Root2(")
        val out = gen("{\"string\":{\"a\":1}}")
        assertFalse(out, out.contains("data class String("))
        assertHas(out, "data class StringType(")
        val twice = gen("{\"a\":{\"b\":{\"x\":1}},\"b\":{\"y\":\"s\"}}")
        assertEquals(1, Regex("data class B\\(").findAll(twice).count())
        assertEquals(1, Regex("data class B2\\(").findAll(twice).count())
    }

    @Test fun kotlinEmptyObjectAndRootArray() {
        val out = gen("{\"meta\":{},\"tags\":[]}")
        assertHas(out, "val meta: JsonObject,", "val tags: List<JsonElement>,")
        assertFalse(out, Regex("data class \\w+\\(\\s*\\)").containsMatchIn(out))
        assertHas(gen("[{\"id\":1}]"), "data class RootItem(", "typealias Root = List<RootItem>")
        assertEquals("typealias Root = List<Int>", gen("[1,2]"))
    }

    // ---- other languages: one golden sample each ----

    private val sample = "{\"user_id\":1}"

    @Test fun typescript() {
        assertEquals("export interface Root {\n  user_id: number;\n}", gen(sample, TypeLang.TYPESCRIPT))
        assertHas(gen(sample, TypeLang.TYPESCRIPT, TypeOptions(camelCase = true)), "  userId: number;")
        assertHas(gen("[{\"a\":1},{\"b\":null}]", TypeLang.TYPESCRIPT), "  a?: number;", "  b?: unknown;", "export type Root = RootItem[];")
        assertHas(gen("{\"my-key\":[1,null]}", TypeLang.TYPESCRIPT), "  \"my-key\": (number | null)[];")
    }

    @Test fun swift() {
        assertEquals(
            "struct Root: Codable {\n    let userId: Int\n\n    enum CodingKeys: String, CodingKey {\n        case userId = \"user_id\"\n    }\n}",
            gen(sample, TypeLang.SWIFT)
        )
    }

    @Test fun go() {
        assertEquals("type Root struct {\n\tUserId int `json:\"user_id\"`\n}", gen(sample, TypeLang.GO))
        assertHas(gen("[{\"a\":1},{}]", TypeLang.GO), "\tA *int `json:\"a,omitempty\"`")
    }

    @Test fun java() {
        assertEquals(
            "import com.fasterxml.jackson.annotation.JsonProperty;\n\npublic record Root(\n    @JsonProperty(\"user_id\") int userId\n) {}",
            gen(sample, TypeLang.JAVA)
        )
        assertHas(gen("{\"ids\":[1]}", TypeLang.JAVA), "List<Integer> ids")
        assertHas(gen(sample, TypeLang.JAVA, TypeOptions(javaLib = JavaLib.GSON)), "@SerializedName(\"user_id\") int userId")
    }

    @Test fun python() {
        assertHas(gen("{\"userId\":1,\"class\":\"x\"}", TypeLang.PYTHON), "@dataclass(kw_only=True)\nclass Root:\n", "    user_id: int  # JSON key: \"userId\"\n", "    class_: str")
    }

    @Test fun csharp() {
        assertHas(gen(sample, TypeLang.CSHARP), "public class Root\n{\n    [JsonPropertyName(\"user_id\")]\n    public int UserId { get; set; }\n}")
    }

    @Test fun dart() {
        assertHas(
            gen(sample, TypeLang.DART),
            "@JsonSerializable()\nclass Root {\n  @JsonKey(name: \"user_id\")\n  final int userId;\n\n  Root({required this.userId});\n",
            "factory Root.fromJson(Map<String, dynamic> json) => _\$RootFromJson(json);"
        )
    }

    @Test fun rust() {
        assertHas(
            gen("{\"user_id\":1,\"type\":null}", TypeLang.RUST),
            "pub struct Root {", "    pub user_id: i64,\n", "    #[serde(rename = \"type\")]\n    pub type_: serde_json::Value,\n"
        )
    }

    @Test fun invalidJsonExplainsTheProblem() {
        val bad = convertJsonToTypes("{", TypeLang.KOTLIN, TypeOptions())
        assertEquals("", bad.code)
        assertTrue(bad.error.orEmpty().startsWith("Not valid JSON"))
        assertEquals(TypesResult(), convertJsonToTypes("  ", TypeLang.KOTLIN, TypeOptions()))
    }

    @Test fun trailingTextIsRejected() {
        val bad = convertJsonToTypes("{\"a\": 1}x", TypeLang.KOTLIN, TypeOptions())
        assertEquals("", bad.code)
        assertTrue(bad.error.orEmpty(), bad.error.orEmpty().startsWith("Not valid JSON: line 1, column 9:"))
        assertEquals("", convertJsonToTypes("{\"a\": 1}\n  ", TypeLang.KOTLIN, TypeOptions()).error.orEmpty())
    }

    @Test fun errorsGiveLineAndColumnWithoutEchoingInput() {
        val error = convertJsonToTypes("{\n  \"secret\": 1,\n  \"b\": 2", TypeLang.KOTLIN, TypeOptions()).error.orEmpty()
        assertTrue(error, error.startsWith("Not valid JSON: line 3,"))
        assertFalse(error, error.contains("secret"))
        // Android's message format.
        assertEquals("line 2, column 3: Unterminated object.", jsonErrorAt("{\n  \"a\"", "Unterminated object at character 5 of {\n  \"a\""))
    }
}
