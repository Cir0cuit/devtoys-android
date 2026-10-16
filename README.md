# DevToys for Android

A collection of offline developer utilities for Android, built with Jetpack Compose.
All processing happens on the device; the app has no network access, telemetry or ads.

## Tools

57 offline tools, including:

- **Encoders / decoders:** Base64 (URL-safe, MIME), Base32/58/85/36, URL with query parser, HTML entities, hex dumps, GZip/Deflate, classical ciphers, JWT (decode, verify, sign)
- **Formatters & data:** JSON (tree view, sort, diff), XML (XPath), SQL, YAML ⇄ JSON, XML ⇄ JSON, CSV ⇄ JSON
- **Converters:** timestamps and date math, cron (Unix, Spring, Quartz), number bases and bit widths, units, JSON to types (Kotlin, TypeScript, Swift, Go, Java, Python, C#, Dart, Rust)
- **Text:** case and slugs, line tools, diff with patch export, regex tester, find and replace, string escaping, statistics, Unicode inspector, list compare, Markdown editor
- **Generators:** QR and 12 barcode formats with scanning from photos, UUID (v1–v7), ULID/NanoID/Snowflake/KSUID, passwords and passphrases, tokens, Lorem Ipsum
- **Cryptography:** hashes and checksum files, HMAC verify, TOTP/HOTP, AES text encryption, RSA/EC key pairs, X.509 decoder
- **Network & system:** CIDR and subnetting, IP converter, MAC vendor lookup, chmod/umask, ports, MIME types, HTTP status codes, device info
- **Graphics:** color converter (Lab, OKLCH), palettes and tints, WCAG contrast checker, image to Base64
- **Reference:** cheat sheets, semver checker, math evaluator, Basic auth headers

## Building

Requirements: JDK 17 or newer and the Android SDK (API 35).

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

### Signed release builds

Release builds are signed only when a keystore is configured. Create a
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

## Contributing

Bug reports and feature requests: [GitHub Issues](https://github.com/Cir0cuit/devtoys-android/issues).
Pull requests are welcome.

## License

Licensed under the [Apache License 2.0](LICENSE).

Bundled third-party assets:

- IBM Plex Sans and IBM Plex Mono, SIL Open Font License 1.1 ([core/designsystem/FONTS-LICENSE.txt](core/designsystem/FONTS-LICENSE.txt))
- EFF Large Wordlist for passphrases, © Electronic Frontier Foundation, CC BY 3.0 US
