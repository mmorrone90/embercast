# Delivery validation

Build and emulator validation completed on 28 September 2026. Physical Fire TV smoke testing completed on 30 September 2026. This document records Phase 1 only.

## Build and package

The following command completed successfully with JDK 17 and the installed Android SDK:

```sh
./gradlew clean :app:assembleDebug :app:lintDebug --console=plain
```

Android lint reported `No issues found`. Warnings are treated as errors. `app/lint.xml` documents narrow exceptions for pinned tooling, intentional API 35 targeting, TV landscape orientation, and the fixed TV banner density. No general lint baseline or blanket error suppression is used.

The APK signature was verified with Android Build Tools 35.0.0 `apksigner`. It has one signer and a valid APK Signature Scheme v2 signature. `aapt dump badging` confirmed package `dev.firemirror.tv`, version `0.1.0`, minimum API 24, target API 35, standard and Leanback launch intents, TV banner, and optional touchscreen support.

Only ACCESS_NETWORK_STATE is requested. No INTERNET permission or native library is present. The installer script passed `bash -n` syntax validation.

## Runtime

The APK was installed and launched on an isolated arm64 Android API 36.1 emulator configured with a 1920 × 1080 TV display profile and density 320. It uses a Google APIs phone system image with a TV display profile; it is not a Fire OS or Android TV firmware emulator.

Seventeen checks passed across the interface and network tests:

1. Main screen launched with an explicit phase notice and unavailable receiver status.
2. Stream resolution and frame rate stayed unavailable instead of displaying invented values.
3. The displayed local address matched the emulator LAN.
4. Directional remote input obtained button focus.
5. Remote Select opened Diagnostics.
6. The codec query completed and listed an AVC decoder.
7. Remote Left and Select opened Settings.
8. Saving a receiver name updated the main screen.
9. The saved name persisted after force stop and process restart.
10. An empty name was rejected without leaving Settings.
11. The default receiver name was restored.
12. Opt in diagnostic messages appeared in logcat.
13. The captured app log contained no runtime crash.
14. Cancel discarded an edited receiver name.
15. The main screen was confirmed active for network testing.
16. Disabling emulator WiFi cleared the local address.
17. Reenabling WiFi restored the local address.

The main screen screenshot was visually inspected. Focus visibility and layout were checked through emulator directional input and UI hierarchy snapshots. Emulator addresses are test addresses, not a Fire TV address.

## Physical Fire TV smoke test

The APK installed successfully over ADB on an Amazon Fire TV Stick (3rd Gen), model `AFTSSS`, running Fire OS 7 and Android 9 (API 28). The device reported a 32 bit `armeabi-v7a` userspace. The main activity launched successfully at 1920 × 1080, displayed the TV's actual LAN address, and showed the expected unavailable receiver and stream states. The main screen was visually inspected from a device screenshot.

Remote directional and Select input opened Settings and Diagnostics, and Back returned to the main screen. Both screens were visually inspected. Diagnostics reported the expected model, API, build, ABI, LAN address, and available AVC decoder. The installed package was confirmed through Android package management, the main activity was confirmed resumed, and sampled logcat output contained no app crash.

This is a smoke test, not completion of the full Phase 1 device acceptance gate. Receiver name editing and persistence, input validation, diagnostic logging, network disconnect and reconnect behavior, and launcher discovery were not retested on the physical Fire TV. The local Mac version was inspected, but no Mac to receiver mirroring connection was attempted because Phase 1 has no receiver.

mDNS advertisement, AirPlay pairing, media transport, hardware video playback, audio synchronization, real FPS, and latency are not implemented or verified by this delivery. Follow PHASES.md before proceeding.
