<p align="center"><img src="logo.svg" width="128" alt="AShare logo"></p>

# AShare

A tiny Android app that turns **"Open with"** into **"Share"**.

AShare registers as a handler for every file type. When you open a file with it, it does not
display the file — it immediately brings up the Android share sheet, so you can send the file
to a messenger, mail client, cloud drive or any other app.

Useful wherever an app only offers "Open" but you want to forward the file instead: download
notifications, file managers, attachments in other apps.

## Usage

- **From another app:** open a file, choose **Share…** (AShare) in the "Open with" list, then
  pick where to send it.
- **From the launcher:** tap the AShare icon, pick a file, then pick where to send it.

AShare has no screen of its own, requests no permissions and does not copy the file; it passes
the original on with a temporary read grant.

### Limitations

- Only `content://` files are handled, which is what current Android apps send. Legacy
  `file://` links from very old file managers are not supported.
- Requires Android 8.0 (API 26) or newer.

## Building

Requires JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

### Release builds

`./gradlew assembleRelease` signs the APK when these environment variables are set, and
produces an unsigned APK otherwise:

| Variable | Meaning |
| --- | --- |
| `ANDROID_KEYSTORE_FILE` | Path to the keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password (also used as the key password) |
| `ANDROID_KEY_ALIAS` | Alias of the signing key |
| `VERSION_NAME`, `VERSION_CODE` | Optional version overrides |

The [Release workflow](.github/workflows/release.yml) does this on GitHub from the repository
secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_ALIAS`:

- every commit to `main` builds a signed APK and attaches it to the workflow run as an artifact;
- pushing a tag such as `v1.2` also publishes it as a GitHub release.
