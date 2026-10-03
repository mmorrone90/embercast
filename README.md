# FireMirror

FireMirror is a local Mac and phone to Fire TV screen mirroring prototype. Its receiver uses the pinned [Android AirPlay Server](https://github.com/jqssun/android-airplay-server) source and UxPlay protocol implementation. The original Kotlin TV interface remains in `app/` as the Phase 1 prototype; the functional receiver build is in `receiver/`. The Android phone sender is in `phone/`.

Version 0.3 adds a redesigned TV home screen, fit, crop, stretch and zoom controls for live mirroring, an optional TV approval prompt for new network addresses, audio only discovery mode, screenshots, silent MP4 recordings, and a Captures screen for sharing or deleting saved files. Recording uses Android's screen capture permission prompt. A Mac to Fire TV session verified live video, zoom, screenshot saving, and a playable silent recording. Audio output and iPhone or iPad sending still need device tests.

Version 0.3.4 fixes the icon shown for sideloaded installations on the tested Fire TV Stick. Its xhdpi square icon is scaled correctly inside Fire TV's rectangular Apps & Channels tile, and its separate 320 × 180 TV banner remains available. Amazon uses a separately supplied 1280 × 720 image for an Appstore listing. Fire TV kept an earlier package's tile artwork through reinstalls, so this version uses the new app ID `dev.firemirror.receiver`. The old app's settings, AirPlay identity, screenshot, and recording were migrated on the test device before that package was removed.

Version 0.4 adds automatic receiver discovery recovery after the TV network changes, Smooth, Balanced and Sharp quality presets, a Connection health screen with video and audio status, a TV speaker test, and accessible audio timing controls. Connection approval can remember a network address, give it a name, and revoke it later. This is address recognition, not cryptographic device identity: an address may change or be reused. Captures can now be viewed in the app and downloaded to a Mac through a temporary private local link that closes when its dialog closes or after ten minutes. Audio playback, audio synchronization, network recovery, and the PIN flow still require a live Mac and TV test before they can be called reliable.

Version 0.5 adds an Android phone sender and a second receiver path inside the same Fire TV app. The phone app discovers FireMirror on the local network, asks Android for screen capture permission, waits for TV approval, and sends H.264 video to the TV decoder. An address field helps when local discovery is blocked. This path mirrors video only at up to 960 pixels on the long edge; phone audio, orientation changes during a session, and actual Android hardware have not yet been tested. The transport is not encrypted, so use it only on a trusted local network. Two emulators verified permission, approval, live phone video on the receiver, and Stop sharing. AirPlay remains available for Mac and iPhone; iPhone mirroring still needs a device test.

Version 0.5.1 adds new Fire TV artwork and lets the Android sender resize its capture when the shared screen rotates. The receiver updates its video decoder and aspect ratio without requiring another approval. The 1280 × 720 PNG in `artwork/` is ready for an Amazon Appstore listing. On our sideloaded Fire TV, the launcher still frames the APK icon as a square, so the store artwork is needed for a full rectangular listing tile. Emulator rotation and a receiver protocol test pass; a real Samsung phone test remains open. Android phone audio remains unavailable.

AirPlay is the receiver protocol for Macs, iPhones, and iPads. The Mac baseline has been tested; iPhone and iPad behavior still needs device testing. Android phones use the FireMirror Phone app, not the system Cast menu. On Fire TV models that support it, Windows and Android users can also use Fire TV's separate built in Miracast screen. Google Cast is not implemented in FireMirror. Google's official [Cast receiver documentation](https://developers.google.com/cast/docs/android_tv_receiver/core_features) describes an Android TV receiver tied to Cast device and application registration, not a general Fire TV screen mirroring library. Amazon documents [Fire TV Miracast setup](https://digprjsurvey.amazon.com/csad/help/node/ToUjwyfVuffzVfbR6b) separately.

On 30 September 2026, the receiver baseline started on an Amazon Fire TV Stick (3rd Gen), advertised itself to a Mac over Bonjour, and displayed the Mac desktop on the TV. This is a device test, not a promise of compatibility with every Mac, Fire TV, codec, or protected video service. Audio and sustained performance have not yet been verified.

## Build the receiver

Install JDK 17 and the Android SDK with platform 36 and CMake. The upstream build uses NDK 27.0.12077973 and Gradle can install it when its SDK license is accepted. Clone this repository with submodules or let the build script initialize them:

```sh
git clone https://github.com/mmorrone90/firemirror.git
cd firemirror
./scripts/build-receiver.sh
```

The script pins the upstream receiver, applies `patches/receiver-firemirror.patch`, builds the APK, and writes `dist/FireMirror-receiver-debug.apk`. The patch sets the app ID to `dev.firemirror.receiver`, the visible receiver name to `FireMirror`, and the default listener port to 7100. Fire OS already uses port 7000 on the tested device. Automatic server start when the app opens remains enabled; automatic start at device boot is disabled. A nested UxPlay patch provides the optional connection approval gate.

The APK is signed by the local Android debug keystore. Keep that key for updates that preserve app data. A build on another computer normally has a different debug key and will not install as an update over this build.

## Build and use the Android phone app

Run `./scripts/build-phone.sh` to create `dist/FireMirror-phone-debug.apk`. Install that APK on an Android phone running Android 8 or newer. The phone and TV must be on the same local network. Open FireMirror on the TV, then FireMirror Phone on Android. Select the TV shown under Nearby Fire TV and tap Mirror this phone. Approve the Android screen capture prompt and the connection prompt on the TV. Tap Stop sharing on the phone or its notification to end the session.

If discovery does not list the TV, enter the address shown on the TV's Connection health screen. The manual address points at the same FireMirror app and uses port 7102. The Mac and iPhone continue to use the Screen Mirroring menu and AirPlay on port 7100. Android phone mirroring currently sends video without audio and does not appear in Android's built in Cast menu.

## Install on the Fire TV

Enable ADB Debugging on the Fire TV and connect from a Mac on the same LAN. Replace `FIRE_TV_IP` with the address shown under Fire TV Settings → My Fire TV → About → Network.

```sh
adb connect FIRE_TV_IP:5555
adb -s FIRE_TV_IP:5555 install -r dist/FireMirror-receiver-debug.apk
adb -s FIRE_TV_IP:5555 shell am start -n dev.firemirror.receiver/io.github.jqssun.airplay.MainActivity
```

Accept the debugging prompt on the TV. Leave FireMirror open. On the Mac, open Control Center → Screen Mirroring → FireMirror. The TV and Mac must be able to reach each other on the local network. No account, cloud server, or internet route is required for the mirroring protocol.

If the app starts but the Mac does not list it, inspect the local service and device log:

```sh
dns-sd -B _airplay._tcp local.
dns-sd -L FireMirror _airplay._tcp local.
adb -s FIRE_TV_IP:5555 logcat -v time 'AirPlayService:I' 'NsdServiceManager:I' '*:S'
```

For detailed ADB setup, updates, and removal, see [installation instructions](docs/INSTALL.md). [Validation](docs/VALIDATION.md) records exactly what has been observed on the tested device. [Phases](docs/PHASES.md) lists the remaining acceptance work.

## Source and licensing

`receiver/` is pinned to Android AirPlay Server revision `c8defdd70d7e6a04f4f1b71d353653682d594106`. Its native dependencies are pinned through its own submodules. The patch contains FireMirror branding, package ID, version, port, and boot behavior. The receiver and its components retain their upstream licenses and notices. This repository is GPL version 3 only; see [notices](NOTICE.md) and the upstream source. When distributing an APK, distribute the corresponding source, patch, dependency revisions, and build scripts.

This project is for ordinary screen mirroring. Protected streaming content and DRM bypass are outside scope. AirPlay, macOS, and Mac are Apple trademarks; Fire TV is an Amazon trademark. The project is not affiliated with Apple or Amazon.
