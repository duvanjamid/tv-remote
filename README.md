# TV Remote

Android app (Kotlin + Jetpack Compose) that replaces the physical remote for whatever TV or TV box happens to be in the room. No companion server, no cloud — the phone talks straight to the TV over the local network (or Bluetooth) using each vendor's own control protocol.

Four TV "backends" live side by side behind one UI:

| Backend | Target | How it talks to the TV |
|---|---|---|
| `webos/` | LG webOS Smart TVs | SSAP (LG's WebSocket control protocol), reverse-engineered from LG's own `cordova-plugin-lgtv` trail and network captures — no official public SDK for it |
| `androidtv/` | Certified Android TV (Google TV, Sony, TCL, etc.) | Android TV Remote Service v2 — same protobuf-over-TLS protocol Google's own Android TV Remote app uses, modeled on [tronikos/androidtvremote2](https://github.com/tronikos/androidtvremote2) |
| `adb/` | Any generic Android TV box (no SSAP, no certified remote service) | Wireless ADB — pairs with the box's own ADB keypair and sends `input keyevent` shell commands, via the pure-JVM [`dadb`](https://github.com/mobile-dev-inc/dadb) client (no PC, no root required) |
| `bluetooth/` | Fallback when the TV isn't reachable over the network | Bluetooth HID — the phone advertises itself as a keyboard/consumer-control HID device and the TV pairs with it like any other remote |

## Discovery

`discovery/` finds TVs on the LAN automatically: SSDP for webOS/UPnP devices and mDNS/NSD for Android TV's `_androidtvremote2._tcp` service. The device list screen shows results live behind an animated wireframe sphere ("Jarvis orb" — `ui/discovery/JarvisOrb.kt`) while a scan is in progress.

## UI

Jetpack Compose throughout, Material You (dynamic color) theming. Per-protocol screens (`ui/WebOsScreen.kt`, `ui/AndroidTvScreen.kt`, `ui/AdbScreen.kt`, `ui/BluetoothScreen.kt`) share common controls — D-pad, touchpad, rocker buttons (`ui/common/`) — and the webOS screen additionally exposes apps, channels and input switching, since SSAP supports querying those directly from the TV.

Devices (once discovered or paired) are cached to local storage (`discovery/DeviceCacheCodec.kt`, `storage/PrefsStore.kt`) so reconnecting doesn't require a fresh scan or re-pairing every time.

## Build

Requires Android Studio, or a standalone Gradle 8.7+ + Android SDK with `compileSdk 35` / JDK 17. (No Gradle wrapper is checked in yet — open the project in Android Studio and it'll offer to add one, or run with your own `gradle` install.)

```bash
gradle assembleDebug
```

Install on a device/TV box on the same network:

```bash
gradle installDebug
```

No signing config, backend keys, or server setup needed — everything runs from the APK.

## Project layout

```
app/src/main/java/com/proyectos/tvremote/
├── webos/        # SSAP client (LG webOS)
├── androidtv/    # Android TV Remote Service v2 client + pairing/certs
├── adb/          # Wireless ADB client (dadb) for generic Android TV boxes
├── bluetooth/    # Bluetooth HID controller
├── discovery/    # SSDP + mDNS/NSD device discovery, device cache
├── storage/      # Preferences / cached device persistence
└── ui/           # Compose screens, shared controls, theme, Jarvis orb
```
