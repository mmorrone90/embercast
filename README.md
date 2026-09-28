# FireMirror

A native Kotlin TV application for a personal Mac to Fire TV mirroring receiver.

**Current deliverable: Phase 1. This APK does not receive AirPlay or advertise itself in macOS Screen Mirroring.** The interface states this explicitly. No dummy service, simulated connection, or invented protocol implementation is included.

## Included

Fullscreen landscape interface with TV launcher integration, visible remote focus, a saved receiver name, local IPv4 monitoring for WiFi and Ethernet, and device and video codec diagnostics. The network monitor supports LANs without internet. Settings currently expose receiver name and diagnostic logging. Video, audio, resolution, hardware decoding, and automatic receiver startup remain future stages.

The application uses Android Views and Kotlin. It has no Google Play Services dependency, accounts, advertising, analytics, cloud calls, or native binaries. The only app runtime dependency is Kotlin's standard library. Internet access is not requested in this build.

## Requirements

Android based Fire OS with Android API 24 or newer. Older Fire OS 5 devices are excluded. Vega OS devices need a separate application format and SDK; this Android APK targets Fire OS. Confirm the operating system in the device's About page before installing. See the [Amazon platform overview](https://developer.amazon.com/en/apps-and-games/fire-tv) and [Vega app submission documentation](https://developer.amazon.com/docs/vega/0.21/app-submission.html).

## Build

Open this directory in Android Studio, or use JDK 17 and an Android SDK with platform 35 and Build Tools 35.0.0 installed. Gradle 8.11.1, Android Gradle Plugin 8.10.1, and Kotlin 2.1.0 are pinned. The wrapper verifies the Gradle distribution SHA256.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug :app:lintDebug
```

Use your JDK and SDK locations on other machines. Android Studio can write your ignored `local.properties`, or create it with `sdk.dir=/absolute/path/to/android/sdk`. The generated APK is `app/build/outputs/apk/debug/app-debug.apk`.

The debug APK is signed by Android's local debug keystore. Updates must use the same signing key. CI builds on a fresh machine can have a different debug key; preserve a stable private signing key for future distributed builds. No signing secrets are shipped in this source.

## Install and test

Follow [Fire TV installation](docs/INSTALL.md). The [Phase 1 checklist](docs/PHASES.md) is the acceptance gate before network receiver work. [Validation](docs/VALIDATION.md) records the checks performed for this delivery.

```sh
adb connect FIRE_TV_IP:5555
adb -s FIRE_TV_IP:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s FIRE_TV_IP:5555 shell am start -n dev.firemirror.tv/.MainActivity
```

## Receiver direction

The recommended implementation is the protocol and Android bridge from [Android AirPlay Server](https://github.com/jqssun/android-airplay-server), using UxPlay through JNI and Android hardware decoding. Selection is based on inspected source and upstream reports. It is not yet integrated or verified with this Mac or a Fire TV.

Read the [feasibility assessment](docs/FEASIBILITY.md) for alternatives, licensing, encryption, known issues, and the [architecture](docs/ARCHITECTURE.md) for integration boundaries. The Mac inspected for this task runs macOS 26.5.2 on arm64. No Fire TV was connected through ADB during development.

## Source layout

`app/src/main/java/dev/firemirror/tv` holds activities, preferences, network monitoring, receiver state, and codec diagnostics. `app/src/main/res` holds native layouts, focus styles, icon, and the TV banner. `docs` contains the staged development and testing plan. `scripts/install.sh` provides explicit ADB installation. GitHub Actions builds and lints the project without publishing anything.

## License

GPL version 3 only. See `LICENSE` and `NOTICE.md`. No receiver implementation is vendored in Phase 1. Future integration must preserve the receiver's copyright notices, component licenses, and corresponding source obligations. AirPlay is Apple's trademark; this project is not affiliated with Apple or Amazon.
