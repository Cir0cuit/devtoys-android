# DevToys for Android

57 offline developer utilities for Android: encoders, formatters, converters, generators, crypto and network tools in one app.

![DevToys for Android](docs/images/hero.jpg)

## Why

A developer, IT admin or tinkerer often needs to decode a JWT, check a subnet, format some JSON or generate a UUID, and usually has to paste the data into a website to do it. DevToys for Android puts those tools on your phone and keeps the data there. Everything runs on the device. The app has no network access, so nothing you paste into it can leave your phone.

## Highlights

- **Offline and private:** no INTERNET permission, no ads, no subscriptions, no telemetry, no analytics.
- **57 tools** in eight categories, with search, favorites and recently used tools.
- **Share and open files:** send text, images or files to DevToys from other apps, or open `.json`, `.xml`, `.yaml`, `.csv`, `.sql`, `.md`, `.pem`, `.gz` and other files from a file manager. DevToys routes them to the tool that handles them.
- **Themes:** system, light or dark mode, AMOLED black, accent colors including dynamic color, and a high-contrast mode.
- **Open source:** Apache-2.0, built with Kotlin and Jetpack Compose.

## Screenshots

<p>
  <img src="docs/images/screenshots/home.png" width="19%" alt="Home screen" />
  <img src="docs/images/screenshots/qr.png" width="19%" alt="QR and barcode generator" />
  <img src="docs/images/screenshots/timestamp.png" width="19%" alt="Timestamp converter" />
  <img src="docs/images/screenshots/json.png" width="19%" alt="JSON formatter" />
  <img src="docs/images/screenshots/settings.png" width="19%" alt="Settings" />
</p>

## Tools

### Converters

| Tool | What it does |
|------|--------------|
| Timestamp Converter | Convert timestamps and calculate dates |
| Cron Parser & Builder | Read, build and preview cron schedules |
| Number Base Converter | Convert numbers between bin, oct, dec and hex |
| Unit Converter | Convert data size, time, temperature and more |
| Math Evaluator | Evaluate math with hex, binary and bitwise ops |
| JSON to Types | Generate typed models from JSON |
| YAML ↔ JSON | Convert between YAML and JSON |
| XML ↔ JSON | Convert between XML and JSON |
| CSV Viewer & Converter | View CSV or JSON as a table and convert it |

### Encoders & decoders

| Tool | What it does |
|------|--------------|
| Base64 Encoder / Decoder | Encode and decode Base64 text |
| Base32 / Base58 / Base85 | Encode text as Base32, Base58, Base85 or Base36 |
| URL Encoder / Decoder | Percent-encode, decode and parse URLs |
| HTML Entity Encoder | Escape and unescape HTML entities |
| Hex Encoder / Decoder | Convert text to hex bytes and back |
| GZip / Deflate | Compress and decompress GZip, Zlib, Deflate |
| ROT13 / Classic Ciphers | ROT13, Caesar, Vigenère, XOR and more |
| JWT Decoder | Decode, verify and sign JSON Web Tokens |
| Basic Auth Header | Build and decode HTTP Basic auth headers |

### Formatters

| Tool | What it does |
|------|--------------|
| JSON Formatter | Format, validate and escape JSON |
| XML Formatter | Format, minify and query XML with XPath |
| SQL Formatter | Format or minify SQL queries |

### Generators

| Tool | What it does |
|------|--------------|
| UUID Generator | Generate and decode UUIDs v1-v7 |
| ID & Token Generator | ULID, NanoID, Snowflake, KSUID, tokens |
| Password Generator | Passwords, passphrases and PINs |
| QR & Barcode Generator | Make and scan QR codes and barcodes |
| Lorem Ipsum Generator | Generate placeholder text |

### Cryptography

| Tool | What it does |
|------|--------------|
| Hash / Checksum | Hash text or files with MD5, SHA and CRC |
| HMAC Calculator | Sign or verify text with an HMAC key |
| OTP Generator | Generate and check TOTP and HOTP codes |
| Text Encryption | Encrypt and decrypt text with AES |
| Key Pair Generator | Generate RSA and EC keys as PEM, SSH or JWK |
| X.509 Certificate Decoder | Inspect PEM, DER and PKCS#7 certificates |

### Text

| Tool | What it does |
|------|--------------|
| Case Converter | Convert text to upper, lower, title, camel, snake or kebab case |
| Line Tools | Sort, filter, dedupe, join or fix lines |
| Text Comparer | Compare two texts line by line |
| JSON Diff | Compare two JSON documents by structure |
| List Comparer | Find lines in both lists or only in one |
| Regex Tester | Test, replace and split text with a regex |
| Find & Replace | Replace text or regex matches |
| String Escape | Escape strings for code, SQL, shell or CSV |
| Text Statistics | Count characters, words, lines and bytes |
| Unicode Inspector | Inspect code points and find hidden characters |
| Markdown Editor | Write Markdown and preview it as you type |
| Semver Compare | Compare, sort and range-check versions |

### Network & system

| Tool | What it does |
|------|--------------|
| CIDR Calculator | Work out the address range of a subnet |
| IP Address Converter | Convert and inspect IPv4 and IPv6 addresses |
| MAC Address Lookup | Format a MAC address and find its vendor |
| Chmod Calculator | Turn file permissions into chmod values |
| Well-Known Ports | Look up common TCP and UDP port numbers |
| HTTP Reference | HTTP status codes, methods and headers |
| MIME Types | Look up MIME types and file extensions |
| Cheatsheets | Look up common Git, shell and tool commands |
| Device Information | Show OS, screen, ABI, memory and locale |

### Graphics

| Tool | What it does |
|------|--------------|
| Color Converter | Convert colors between HEX, RGB, HSL and HSV |
| Color Palette Generator | Build matching color schemes from one color |
| Contrast Checker | Check WCAG contrast between two colors |
| Image ↔ Base64 | Turn images into Base64 and back |

## Install

Download the latest APK from [GitHub Releases](https://github.com/Cir0cuit/devtoys-android/releases) and open it on your device. You may need to allow installs from your browser or file manager. Requires Android 8.0 (API 26) or newer.

DevToys is no longer published on Google Play.

## Build from source

Requirements: JDK 17 to 21 and the Android SDK (API 35). Newer JDKs are not supported by the Gradle version in use, so point `JAVA_HOME` at a JDK 17-21 install if your default `java` is newer.

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

Tests and lint:

```sh
./gradlew testDebugUnitTest
./gradlew lintDebug
```

### Signed release builds

Release builds are minified with R8 and signed only when a keystore is configured. Create a
`keystore.properties` file in the project root (it is gitignored):

```properties
storeFile=path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Alternatively set `DEVTOYS_KEYSTORE_FILE`, `DEVTOYS_KEYSTORE_PASSWORD`,
`DEVTOYS_KEY_ALIAS` and `DEVTOYS_KEY_PASSWORD`. Then run:

```sh
./gradlew assembleRelease
```

Without a keystore, `assembleRelease` produces an unsigned APK.

## Project structure

| Module | Contents |
|--------|----------|
| `app/` | `MainActivity`, main screen and tool host, settings, favorites, share and open-file handling, privacy policy text |
| `core/designsystem/` | Theme, colors and bundled IBM Plex fonts |
| `core/ui/` | Shared composables such as `ToolScaffold`, `MonoTextField` and `OutputPanel` |
| `tools/base/` | `Tool` interface, `ToolDescriptor`, `ToolCategory`, tool registry |
| `tools/<group>/` | One file per tool, grouped as `encoders`, `text`, `data`, `generators`, `crypto`, `developer`, `reference` |

Unit tests (JUnit 4) live in `tools/*/src/test`.

### Adding a tool

1. Create a class implementing `Tool` in the matching `tools/<group>` module and give it a `ToolDescriptor` with an id, title, description, icon, category and search keywords.
2. If the tool can take content from other apps, set `accepts` (`ShareKind.TEXT`, `IMAGE`, `FILE`) and, for files it should open, `fileTypes` (extensions without the dot).
3. Register it in the `DefaultToolRegistry` list in `app/src/main/java/com/cir0cuit/devtoys/di/AppModule.kt`.
4. Keep pure logic in top-level `internal` functions and cover it with a JVM unit test.

## Privacy

DevToys follows a zero-data policy. It does not collect, log or transmit personal data, usage analytics, telemetry or crash reports, and it has no ads. All processing happens on your device. Content you share or open is passed only to the tool that handles it and is never stored elsewhere or transmitted. The app does not request the INTERNET permission.

The full policy is shown in the app under Settings and is kept in [`app/src/main/java/com/cir0cuit/devtoys/ui/LegalTexts.kt`](app/src/main/java/com/cir0cuit/devtoys/ui/LegalTexts.kt).

## Contributing

Bug reports and feature requests are welcome in [GitHub Issues](https://github.com/Cir0cuit/devtoys-android/issues). Pull requests are welcome too. Please run `./gradlew testDebugUnitTest lintDebug assembleRelease` before opening one. New dependencies must be actively maintained, Apache-2.0, MIT or BSD compatible, and available from Maven Central or Google.

## License

Licensed under the [Apache License 2.0](LICENSE).

Bundled third-party assets:

- IBM Plex Sans and IBM Plex Mono, SIL Open Font License 1.1 ([core/designsystem/FONTS-LICENSE.txt](core/designsystem/FONTS-LICENSE.txt))
- EFF Large Wordlist for passphrases, © Electronic Frontier Foundation, CC BY 3.0 US
