package com.cir0cuit.devtoys.tools.reference

// --- Status code details ----------------------------------------------------------------------

/** Status codes defined in RFC 9110 §15, in section order per class (15.2 = 1xx ... 15.6 = 5xx). */
private val RFC9110_CODES = listOf(
    listOf(100, 101),
    listOf(200, 201, 202, 203, 204, 205, 206),
    listOf(300, 301, 302, 303, 304, 305, 306, 307, 308),
    (400..417).toList() + listOf(418, 421, 422, 426),
    listOf(500, 501, 502, 503, 504, 505)
)

private val OTHER_SPECS = mapOf(
    102 to "RFC 2518 (WebDAV)", 103 to "RFC 8297",
    207 to "RFC 4918 (WebDAV)", 208 to "RFC 5842 (WebDAV)", 226 to "RFC 3229",
    423 to "RFC 4918 (WebDAV)", 424 to "RFC 4918 (WebDAV)", 425 to "RFC 8470",
    428 to "RFC 6585", 429 to "RFC 6585", 431 to "RFC 6585", 451 to "RFC 7725",
    506 to "RFC 2295", 507 to "RFC 4918 (WebDAV)", 508 to "RFC 5842 (WebDAV)",
    510 to "RFC 2774", 511 to "RFC 6585"
)

/** The document and section that defines [code], e.g. "RFC 9110 §15.5.5" for 404. */
internal fun httpSpec(code: Int): String {
    val cls = code / 100
    val index = RFC9110_CODES.getOrNull(cls - 1)?.indexOf(code) ?: -1
    return if (index >= 0) "RFC 9110 §15.${cls + 1}.${index + 1}" else OTHER_SPECS[code] ?: "Not standardized"
}

/** Heuristically cacheable status codes (RFC 9110 §15.1). */
internal val CACHEABLE_BY_DEFAULT = setOf(200, 203, 204, 206, 300, 301, 308, 404, 405, 410, 414, 501)

/** Codes where retrying the same request later can succeed. */
internal val WORTH_RETRYING = setOf(408, 425, 429, 502, 503, 504)

internal fun httpHasBody(code: Int): Boolean = code >= 200 && code != 204 && code != 205 && code != 304

internal val HTTP_STATUS_HEADERS = mapOf(
    101 to "Upgrade, Connection",
    103 to "Link",
    201 to "Location",
    206 to "Content-Range",
    300 to "Location",
    301 to "Location",
    302 to "Location",
    303 to "Location",
    304 to "ETag, Last-Modified, Cache-Control",
    307 to "Location",
    308 to "Location",
    401 to "WWW-Authenticate",
    405 to "Allow",
    407 to "Proxy-Authenticate",
    408 to "Connection: close",
    412 to "If-Match, If-Unmodified-Since",
    413 to "Retry-After",
    415 to "Content-Type, Accept-Encoding",
    416 to "Content-Range",
    426 to "Upgrade",
    428 to "If-Match, If-Unmodified-Since",
    429 to "Retry-After",
    503 to "Retry-After"
)

/** Usual causes and fixes for the codes developers meet most. */
internal val HTTP_STATUS_TIPS = mapOf(
    101 to "Seen during a WebSocket handshake. If you get 200 or 400 instead, check the Upgrade and Connection headers and any proxy in between.",
    201 to "The URL of the new resource is in the Location header.",
    204 to "A common reply to PUT or DELETE. Do not try to parse a body.",
    206 to "The reply to a Range request, used for resumable downloads and media seeking.",
    301 to "Browsers and search engines cache it for a long time. Use 302 or 307 while you test a redirect.",
    302 to "Browsers turn a POST into a GET when they follow it. Use 307 to keep the method.",
    303 to "Typical after a form POST, so reloading the page does not submit the form again.",
    304 to "The reply to If-None-Match or If-Modified-Since. The client reuses its cached copy.",
    307 to "Use it instead of 302 when the method and body must be kept.",
    308 to "Use it instead of 301 when the method and body must be kept.",
    400 to "Check the JSON or form syntax, the required fields and the Content-Type header.",
    401 to "Send credentials in the Authorization header. A missing or expired token is the usual cause.",
    403 to "The credentials work but lack permission. Check roles, token scopes, file permissions or a firewall rule.",
    404 to "Check the path, a trailing slash, the base path and whether the route exists for this method.",
    405 to "The Allow header lists the methods this resource accepts.",
    408 to "The client sent the request too slowly. Retry, and check slow networks and keep-alive timeouts.",
    409 to "Typical for duplicate keys or edit conflicts. Fetch the current state, then try again.",
    410 to "Unlike 404, the server says the resource will not come back. Remove links to it.",
    412 to "The resource changed since you fetched it. Get the new ETag, then retry.",
    413 to "Send a smaller body or raise the server limit, such as client_max_body_size in nginx.",
    415 to "Set Content-Type to a type the server accepts, such as application/json.",
    422 to "The body parsed, but a field failed validation. The response body usually names the field.",
    429 to "Slow down. Wait for the time in Retry-After, then back off exponentially.",
    500 to "A bug or an unhandled exception on the server. Check the server logs.",
    502 to "The proxy could not reach the app or got an invalid reply. Check that the app runs and listens on the expected port.",
    503 to "The server is overloaded or in maintenance. Retry after the time in Retry-After.",
    504 to "The upstream took too long. Look for slow queries, or raise the proxy's read timeout."
)

// --- Methods ----------------------------------------------------------------------------------

internal class HttpMethod(
    val name: String,
    val description: String,
    val safe: Boolean,
    val idempotent: Boolean,
    val cacheable: String,
    val requestBody: String
)

/** RFC 9110 §9 and RFC 5789 (PATCH). */
internal val HTTP_METHODS = listOf(
    HttpMethod("GET", "Fetch a representation of the resource.", true, true, "Yes", "No meaning"),
    HttpMethod("HEAD", "Like GET, but without a response body. Checks headers, size or existence.", true, true, "Yes", "No meaning"),
    HttpMethod("POST", "Send data for the resource to process: create items, submit forms, run actions.", false, false, "Rarely", "Yes"),
    HttpMethod("PUT", "Replace the resource with the request body, or create it at this URL.", false, true, "No", "Yes"),
    HttpMethod("PATCH", "Apply a partial change to the resource (RFC 5789).", false, false, "No", "Yes"),
    HttpMethod("DELETE", "Remove the resource.", false, true, "No", "No meaning"),
    HttpMethod("OPTIONS", "Ask which methods and features the resource supports. Used for CORS preflight.", true, true, "No", "Optional"),
    HttpMethod("TRACE", "Echo the received request back to debug proxies. Usually disabled.", true, true, "No", "No"),
    HttpMethod("CONNECT", "Open a tunnel to the target host, for example HTTPS through a proxy.", false, false, "No", "No meaning")
)

// --- Headers ----------------------------------------------------------------------------------

internal class HttpHeader(val name: String, val kind: String, val description: String, val example: String)

private const val REQ = "Request"
private const val RES = "Response"
private const val BOTH = "Request and response"

internal val HTTP_HEADERS = listOf(
    "Caching" to listOf(
        HttpHeader("Cache-Control", BOTH, "Caching rules: max-age, no-cache, no-store, private, public, immutable.", "public, max-age=31536000, immutable"),
        HttpHeader("ETag", RES, "Version tag of the representation, used by conditional requests.", "\"33a64df5\""),
        HttpHeader("Last-Modified", RES, "When the resource last changed.", "Wed, 21 Oct 2015 07:28:00 GMT"),
        HttpHeader("Expires", RES, "Date after which the response is stale. Cache-Control max-age wins over it.", "Wed, 21 Oct 2015 07:28:00 GMT"),
        HttpHeader("Age", RES, "Seconds the response has been in a shared cache.", "24"),
        HttpHeader("Vary", RES, "Request headers that select the representation. Caches store one copy per value.", "Accept-Encoding, Origin")
    ),
    "Conditional and range requests" to listOf(
        HttpHeader("If-None-Match", REQ, "Return 304 if the ETag still matches.", "\"33a64df5\""),
        HttpHeader("If-Modified-Since", REQ, "Return 304 if the resource has not changed since this date.", "Wed, 21 Oct 2015 07:28:00 GMT"),
        HttpHeader("If-Match", REQ, "Only apply the change if the ETag matches. Prevents lost updates.", "\"33a64df5\""),
        HttpHeader("If-Unmodified-Since", REQ, "Only apply the change if the resource has not changed since this date.", "Wed, 21 Oct 2015 07:28:00 GMT"),
        HttpHeader("Range", REQ, "Ask for part of the resource.", "bytes=0-1023"),
        HttpHeader("Accept-Ranges", RES, "Says the server supports range requests.", "bytes"),
        HttpHeader("Content-Range", RES, "Which part of the resource a 206 response holds.", "bytes 0-1023/146515")
    ),
    "Content" to listOf(
        HttpHeader("Content-Type", BOTH, "Media type and charset of the body.", "application/json; charset=utf-8"),
        HttpHeader("Content-Length", BOTH, "Size of the body in bytes.", "348"),
        HttpHeader("Content-Encoding", BOTH, "Compression applied to the body.", "gzip"),
        HttpHeader("Content-Disposition", RES, "Show the body inline or download it as a file.", "attachment; filename=\"report.pdf\""),
        HttpHeader("Content-Language", RES, "Language of the content.", "en-US"),
        HttpHeader("Accept", REQ, "Media types the client can handle, with optional weights.", "application/json, text/plain;q=0.9"),
        HttpHeader("Accept-Encoding", REQ, "Compression formats the client understands.", "gzip, deflate, br, zstd"),
        HttpHeader("Accept-Language", REQ, "Preferred languages.", "en-US,en;q=0.9"),
        HttpHeader("Transfer-Encoding", BOTH, "HTTP/1.1 only: how the body is framed.", "chunked")
    ),
    "Authentication and cookies" to listOf(
        HttpHeader("Authorization", REQ, "Credentials for the server.", "Bearer eyJhbGciOiJIUzI1NiJ9..."),
        HttpHeader("WWW-Authenticate", RES, "Sent with 401: how to authenticate.", "Bearer realm=\"api\", error=\"invalid_token\""),
        HttpHeader("Proxy-Authorization", REQ, "Credentials for a proxy.", "Basic dXNlcjpwYXNzd29yZA=="),
        HttpHeader("Proxy-Authenticate", RES, "Sent with 407: how to authenticate with the proxy.", "Basic realm=\"proxy\""),
        HttpHeader("Cookie", REQ, "Cookies the client stored for this site.", "session=abc123; theme=dark"),
        HttpHeader("Set-Cookie", RES, "Store a cookie. Add Secure, HttpOnly and SameSite.", "session=abc123; Path=/; Secure; HttpOnly; SameSite=Lax")
    ),
    "CORS" to listOf(
        HttpHeader("Origin", REQ, "Scheme, host and port of the page making the request.", "https://app.example.com"),
        HttpHeader("Access-Control-Allow-Origin", RES, "Origin allowed to read the response, or * without credentials.", "https://app.example.com"),
        HttpHeader("Access-Control-Allow-Methods", RES, "Preflight reply: methods allowed.", "GET, POST, PUT, DELETE"),
        HttpHeader("Access-Control-Allow-Headers", RES, "Preflight reply: request headers allowed.", "Content-Type, Authorization"),
        HttpHeader("Access-Control-Allow-Credentials", RES, "Allow cookies and auth headers. Needs an exact origin, not *.", "true"),
        HttpHeader("Access-Control-Expose-Headers", RES, "Response headers that scripts may read.", "X-Request-Id"),
        HttpHeader("Access-Control-Max-Age", RES, "Seconds the browser may cache the preflight result.", "86400"),
        HttpHeader("Access-Control-Request-Method", REQ, "Preflight: the method the real request will use.", "PUT"),
        HttpHeader("Access-Control-Request-Headers", REQ, "Preflight: the headers the real request will send.", "Content-Type, Authorization")
    ),
    "Security" to listOf(
        HttpHeader("Strict-Transport-Security", RES, "HSTS: use only HTTPS for this host from now on.", "max-age=63072000; includeSubDomains; preload"),
        HttpHeader("Content-Security-Policy", RES, "Where scripts, styles, images and frames may load from.", "default-src 'self'; img-src 'self' data:"),
        HttpHeader("X-Content-Type-Options", RES, "Stop browsers from guessing the media type.", "nosniff"),
        HttpHeader("X-Frame-Options", RES, "Block framing to prevent clickjacking. CSP frame-ancestors replaces it.", "DENY"),
        HttpHeader("Referrer-Policy", RES, "How much of the URL goes into the Referer header.", "strict-origin-when-cross-origin"),
        HttpHeader("Permissions-Policy", RES, "Turn browser features such as the camera on or off.", "camera=(), geolocation=(self)"),
        HttpHeader("Cross-Origin-Opener-Policy", RES, "Isolate the window from cross-origin popups.", "same-origin"),
        HttpHeader("Cross-Origin-Embedder-Policy", RES, "Only load cross-origin resources that opt in.", "require-corp"),
        HttpHeader("Cross-Origin-Resource-Policy", RES, "Which origins may embed this resource.", "same-site")
    ),
    "Routing and connection" to listOf(
        HttpHeader("Host", REQ, "Host name and port the request is for. Required in HTTP/1.1.", "api.example.com"),
        HttpHeader("User-Agent", REQ, "Identifies the client software.", "curl/8.5.0"),
        HttpHeader("Referer", REQ, "URL of the page that linked here. The misspelling is official.", "https://example.com/page"),
        HttpHeader("Location", RES, "Target of a redirect, or the URL of a new resource after 201.", "/users/42"),
        HttpHeader("Retry-After", RES, "Seconds or a date to wait before retrying, with 429 or 503.", "120"),
        HttpHeader("Allow", RES, "Methods the resource supports. Sent with 405.", "GET, HEAD, POST"),
        HttpHeader("Connection", BOTH, "HTTP/1.1 only: keep the connection open or close it.", "keep-alive"),
        HttpHeader("Upgrade", BOTH, "Switch protocols, such as to WebSocket.", "websocket"),
        HttpHeader("Expect", REQ, "Ask the server to confirm before the body is sent.", "100-continue"),
        HttpHeader("Forwarded", REQ, "Standard form of the X-Forwarded-* headers.", "for=192.0.2.60;proto=https;by=203.0.113.43"),
        HttpHeader("X-Forwarded-For", REQ, "De facto: client IP and the proxies it passed.", "203.0.113.195, 70.41.3.18"),
        HttpHeader("X-Forwarded-Proto", REQ, "De facto: the scheme the client used to reach the proxy.", "https"),
        HttpHeader("Via", BOTH, "Proxies the message passed through.", "1.1 proxy.example.com"),
        HttpHeader("Alt-Svc", RES, "Advertises another endpoint or protocol, such as HTTP/3.", "h3=\":443\"; ma=86400"),
        HttpHeader("Link", RES, "Related resources, such as preload hints.", "</style.css>; rel=preload; as=style"),
        HttpHeader("Server", RES, "Identifies the server software.", "nginx"),
        HttpHeader("Date", RES, "When the message was sent.", "Wed, 21 Oct 2015 07:28:00 GMT"),
        HttpHeader("X-Request-Id", BOTH, "De facto: traces a request across services and logs.", "f058ebd6-02f7-4d3f-942e-904344e8cde5")
    )
)
