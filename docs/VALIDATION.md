# Delivery validation

Phase 1 build and emulator validation completed on 28 September 2026. Fire TV tests and receiver prototype work followed on 30 September 2026.

## Phase 1 build and package

The following command completed successfully with JDK 17 and the installed Android SDK:

```sh
./gradlew clean :app:assembleDebug :app:lintDebug --console=plain
```

Android lint reported `No issues found`. Warnings are treated as errors. `app/lint.xml` documents narrow exceptions for pinned tooling, intentional API 35 targeting, TV landscape orientation, and the fixed TV banner density. No general lint baseline or blanket error suppression is used.

The APK signature was verified with Android Build Tools 35.0.0 `apksigner`. It has one signer and a valid APK Signature Scheme v2 signature. `aapt dump badging` confirmed package `dev.firemirror.tv`, version `0.1.0`, minimum API 24, target API 35, standard and Leanback launch intents, TV banner, and optional touchscreen support.

Only ACCESS_NETWORK_STATE is requested. No INTERNET permission or native library is present. The installer script passed `bash -n` syntax validation.

## Phase 1 emulator runtime

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

## Phase 1 physical Fire TV smoke test

The APK installed successfully over ADB on an Amazon Fire TV Stick (3rd Gen), model `AFTSSS`, running Fire OS 7 and Android 9 (API 28). The device reported a 32 bit `armeabi-v7a` userspace. The main activity launched successfully at 1920 × 1080, displayed the TV's actual LAN address, and showed the expected unavailable receiver and stream states. The main screen was visually inspected from a device screenshot.

Remote directional and Select input opened Settings and Diagnostics, and Back returned to the main screen. Both screens were visually inspected. Diagnostics reported the expected model, API, build, ABI, LAN address, and available AVC decoder. The installed package was confirmed through Android package management, the main activity was confirmed resumed, and sampled logcat output contained no app crash.

This is a smoke test, not completion of the full Phase 1 device acceptance gate. Receiver name editing and persistence, input validation, diagnostic logging, network disconnect and reconnect behavior, and launcher discovery were not retested on the physical Fire TV. The local Mac version was inspected, but no Mac to receiver mirroring connection was attempted because Phase 1 has no receiver.

The Phase 1 APK does not implement mDNS, AirPlay, or playback. It has been superseded on the tested Fire TV by the receiver prototype below.

## Receiver prototype on the Fire TV

The pinned Android AirPlay Server source at `c8defdd70d7e6a04f4f1b71d353653682d594106` built successfully with its native submodules. Its unmodified default server port 7000 failed to start because that port was already occupied on this Fire TV. Port 7100 started successfully. The receiver registered `_airplay._tcp` and `_raop._tcp` as FireMirror, and macOS `dns-sd` discovered both services. The Mac resolved FireMirror to `Android.local.:7100` with AirPlay capability TXT data and successfully opened a TCP connection to the Fire TV on port 7100.

During the baseline receiver test, the Fire TV displayed the Mac desktop, including its wallpaper and menu bar, in a device screenshot. That confirms discovery, connection, transport, and rendered video for this Mac and Fire TV. The test did not establish an actual frame rate, latency, codec, decoder mode, audio output, long running stability, or reconnect behavior.

The FireMirror build script applied the checked in patch to use package `dev.firemirror.tv`, version `0.2.0`, receiver name FireMirror, port 7100, and no start at boot. The branded APK built successfully and its signature matched the earlier prototype's local debug signature. It updated the original package on the Fire TV, launched, started its server on port 7100, registered both discovery services, and displayed its waiting screen. macOS discovered the branded build's FireMirror service. A complete Mac to branded build playback test is still pending.

The upstream receiver's `lintDebug` task did not pass: it reported 29 errors and 105 warnings, beginning with an inherited service `onStartCommand` super call warning. The receiver APK build and device smoke tests passed; lint cleanup remains separate work. The repository CI continues to lint the Phase 1 module and build the receiver APK.

## FireMirror 0.3 feature build

The updated receiver APK assembled successfully for `armeabi-v7a`, `arm64-v8a`, and `x86_64`. It was installed on the Fire TV Stick (3rd Gen), which reported package version `0.3.0` and Android API 28. The redesigned Overview and Captures screens were visually inspected at 1920 × 1080. The new mode controls were exercised through ADB input.

With connection approval enabled, a TCP connection from the Mac's local address displayed the approval dialog on the TV, including that address and Allow and Deny actions. The dialog was dismissed with Deny and the receiver stayed running. The prompt is an address gate, not authenticated identification of a Mac. It allows an approved address for the remainder of the current server run. Full AirPlay pairing through that gate is pending a sender test.

Audio only mode restarted the receiver and registered `_raop._tcp` without registering `_airplay._tcp`. Switching back to Mirror screen registered both services again. An actual audio stream was not played during this test.

The Captures screen displayed an injected test PNG, its file size, and Share and Delete actions. The delete confirmation removed that file and the view returned to its empty state. After the Mac was unlocked, it connected through Screen Mirroring and the Fire TV rendered the live desktop at 1920 × 1080. Zoom in visibly enlarged the desktop image. The screenshot control saved a PNG containing the mirrored video. Recording prompted for Android screen capture permission, then saved a 23 second silent 1280 × 720 H.264 MP4. `ffprobe` found 207 video frames, and a decoded frame visibly contained the Mac desktop. Playback on the TV and sharing through an installed target app were not tested.

The live session revealed that a Fire TV remote Select press did not restore controls after their eight second timeout. The fullscreen view now focuses its controls while visible, focuses the video container when they hide, and responds to Select by showing the controls again. This correction was rebuilt and installed for a repeat device check.

Mac audio output, connection approval during full AirPlay pairing, iPhone and iPad traffic, and long running stability have not been verified. Google Cast is not implemented, and Miracast is a separate Fire TV system feature.

## FireMirror 0.3.3 artwork

The receiver APK includes a rectangular 1280 × 720 launcher icon, a matching 320 × 180 TV banner, and a separate square round icon. [Amazon's Fire TV Appstore guidance](https://developer.amazon.com/docs/app-submission/appstore-details.html#guidelines_firetv_app_icon) specifies a 1280 × 720 PNG app icon for the Fire TV app row. [Android TV guidance](https://developer.android.com/training/tv/get-started/create#banner) specifies a 320 × 180 banner with text. The manifest points the application and launch activity at the rectangular artwork.

The Fire TV reports version `0.3.3`, and `aapt dump badging` reports `firemirror_launcher_icon.png` as its application icon and `firemirror_banner.png` as its banner. The Fire TV Apps & Channels tile still showed the older monitor artwork after the update. It also showed that artwork after an earlier clean reinstall of version 0.3.2, so this device's launcher has not been observed rendering the new assets. The existing screenshot, recording, and settings were backed up and restored around that earlier reinstall.
