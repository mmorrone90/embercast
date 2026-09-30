# FireMirror

FireMirror is a local Mac to Fire TV screen mirroring prototype. Its receiver uses the pinned [Android AirPlay Server](https://github.com/jqssun/android-airplay-server) source and UxPlay protocol implementation. The original Kotlin TV interface remains in `app/` as the Phase 1 prototype; the functional receiver build is in `receiver/`.

On 30 September 2026, the receiver baseline started on an Amazon Fire TV Stick (3rd Gen), advertised itself to a Mac over Bonjour, and displayed the Mac desktop on the TV. This is a device test, not a promise of compatibility with every Mac, Fire TV, codec, or protected video service. Audio and sustained performance have not yet been verified.

## Build the receiver

Install JDK 17 and the Android SDK with platform 36 and CMake. The upstream build uses NDK 27.0.12077973 and Gradle can install it when its SDK license is accepted. Clone this repository with submodules or let the build script initialize them:

```sh
git clone https://github.com/mmorrone90/firemirror.git
cd firemirror
./scripts/build-receiver.sh
```

The script pins the upstream receiver, applies `patches/receiver-firemirror.patch`, builds the APK, and writes `dist/FireMirror-receiver-debug.apk`. The patch sets the app ID to `dev.firemirror.tv`, the visible receiver name to `FireMirror`, and the default listener port to 7100. Fire OS already uses port 7000 on the tested device. Automatic server start when the app opens remains enabled; automatic start at device boot is disabled.

The APK is signed by the local Android debug keystore. Keep that key for updates that preserve app data. A build on another computer normally has a different debug key and will not install as an update over this build.

## Install on the Fire TV

Enable ADB Debugging on the Fire TV and connect from a Mac on the same LAN. Replace `FIRE_TV_IP` with the address shown under Fire TV Settings → My Fire TV → About → Network.

```sh
adb connect FIRE_TV_IP:5555
adb -s FIRE_TV_IP:5555 install -r dist/FireMirror-receiver-debug.apk
adb -s FIRE_TV_IP:5555 shell am start -n dev.firemirror.tv/io.github.jqssun.airplay.MainActivity
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

This project is for ordinary desktop mirroring. Protected streaming content and DRM bypass are outside scope. AirPlay, macOS, and Mac are Apple trademarks; Fire TV is an Amazon trademark. The project is not affiliated with Apple or Amazon.
