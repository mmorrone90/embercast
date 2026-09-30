# Fire TV installation

These steps apply to Android based Fire OS, not Vega OS. This APK requires Android API 24 or newer. [Amazon's ADB guide](https://developer.amazon.com/docs/fire-tv/connecting-adb-to-device.html) is the reference for device connection, and [Amazon's installation guide](https://developer.amazon.com/docs/fire-tv/installing-and-running-your-app.html) covers package management.

For Mac screen mirroring, build and install `dist/FireMirror-receiver-debug.apk` with `./scripts/build-receiver.sh`. The root `app/` module is the earlier interface prototype and cannot receive a stream.

## Prepare the Fire TV

1. Put the Mac and Fire TV on the same LAN.
2. Open Settings → My Fire TV (or Device & Software) → About.
3. Record the model and software version. If Developer Options is hidden, select the device entry in About seven times with the remote's Select button. Return to My Fire TV.
4. Open Developer Options and enable ADB Debugging. On versions with Apps from Unknown Sources, enable it. On versions with Install unknown apps, grant permission to your chosen sideload installer if using one. ADB uses the debugging connection.
5. Find the IP under About → Network. Substitute that address for `FIRE_TV_IP` below.

## Prepare this Mac

Android Studio and SDK Platform Tools are already installed on the development Mac. For the current terminal session:

```sh
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
adb version
```

## Connect

```sh
adb connect FIRE_TV_IP:5555
adb devices -l
```

Accept the debugging authorization prompt on the TV. `unauthorized` means authorization is still pending. `offline` is not a successful connection. If connection is refused, confirm the IP and ADB Debugging setting. Do not forward port 5555 through your router.

## Check compatibility

```sh
adb -s FIRE_TV_IP:5555 shell getprop ro.product.model
adb -s FIRE_TV_IP:5555 shell getprop ro.build.version.sdk
adb -s FIRE_TV_IP:5555 shell getprop ro.build.display.id
adb -s FIRE_TV_IP:5555 shell getprop ro.product.cpu.abilist
```

The API level must be 24 or higher. Phase 1 has no native library and is ABI independent. Later receiver builds must include both `armeabi-v7a` and `arm64-v8a`, because a 64 bit processor does not guarantee a 64 bit Fire OS userspace.

## Install the receiver

From the project directory:

```sh
./scripts/build-receiver.sh
adb -s FIRE_TV_IP:5555 install -r dist/FireMirror-receiver-debug.apk
adb -s FIRE_TV_IP:5555 shell am start -n dev.firemirror.receiver/io.github.jqssun.airplay.MainActivity
```

The receiver uses port 7100 because port 7000 is occupied on the tested Fire TV. With the app open, select FireMirror in the Mac's Control Center → Screen Mirroring menu. The Mac and Fire TV must be on a LAN that permits mDNS discovery and direct device connections.

## Install the earlier interface prototype

From the project directory:

```sh
adb -s FIRE_TV_IP:5555 install app/build/outputs/apk/debug/app-debug.apk
adb -s FIRE_TV_IP:5555 shell am start -n dev.firemirror.tv/.MainActivity
```

The separately delivered `FireMirror-phase1-debug.apk` is the same build and can be installed using its path. FireMirror also appears in the TV app launcher. If it is absent from the recent apps row, use Your Apps & Channels or start through ADB.

## Update while preserving settings

```sh
adb -s FIRE_TV_IP:5555 install -r dist/FireMirror-receiver-debug.apk
```

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` usually indicates a signing key mismatch. Rebuild with the original key to retain settings. Uninstalling removes saved preferences.

Version 0.3.4 uses the new package ID `dev.firemirror.receiver`. It installs alongside versions that used `dev.firemirror.tv`, so an APK update alone does not move their settings or captures. Back up those files before removing the older package. On the tested Fire TV, the app's internal `files` and `shared_prefs` were copied with `adb shell run-as` and `tar`; its screenshot and recording were copied from `/sdcard/Android/data/dev.firemirror.tv/files/` into the corresponding directory for `dev.firemirror.receiver`. The old app was removed only after the new app displayed both captures and registered its AirPlay service.

## Logs

For the receiver's service and discovery messages:

```sh
adb -s FIRE_TV_IP:5555 logcat -v time 'AirPlayService:I' 'NsdServiceManager:I' '*:S'
```

The earlier interface prototype has a separate Diagnostic logging setting:

```sh
adb -s FIRE_TV_IP:5555 logcat -v time 'FireMirror:D' 'AndroidRuntime:E' '*:S'
```

For a broader device report:

```sh
adb -s FIRE_TV_IP:5555 shell dumpsys package dev.firemirror.receiver
adb -s FIRE_TV_IP:5555 shell dumpsys connectivity
```

The prototype Diagnostics page shows model, API level, build, ABI, LAN address, and available H.264/HEVC decoders. Decoder presence does not prove working playback.

## Uninstall and disconnect

```sh
adb -s FIRE_TV_IP:5555 uninstall dev.firemirror.receiver
adb disconnect FIRE_TV_IP:5555
```

You can turn off ADB Debugging after testing. FireMirror itself does not need ADB to launch.
