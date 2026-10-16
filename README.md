# DevToys for Android

A collection of offline developer utilities for Android, built with Jetpack Compose.
All processing happens on the device; the app has no network access, telemetry or ads.

## Tools

- **Encoders / decoders:** Base64, Base64 image, URL, HTML entities, hex, ROT13 / Caesar, JWT decoder
- **Formatters:** JSON, XML, SQL
- **Converters:** YAML ⇄ JSON, number bases, Unix timestamps, cron expressions, JSON to types
- **Text:** case converter, line tools, text diff, regex tester, Markdown editor and preview
- **Generators:** UUID, password, Lorem Ipsum, QR code
- **Cryptography:** hash calculator, HMAC, X.509 certificate decoder
- **Network / system:** CIDR calculator, MAC address vendor lookup, chmod calculator, HTTP status codes
- **Graphics:** color picker, color palette
- **Data:** CSV viewer
- **Reference:** cheat sheets

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
