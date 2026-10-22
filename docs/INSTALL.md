# Install and test EmberCast

EmberCast has two apps. Install the receiver on an Android based Fire TV, then install EmberCast Phone on an Android phone if you want to mirror Android. A Mac uses its built in AirPlay controls and does not need a companion app.

## 1. Check compatibility

* The receiver needs Android API 24 or newer. The tested device is a Fire TV Stick 3rd Gen running Fire OS 7 and Android API 28.
* EmberCast Phone needs Android API 26 or newer. Playback audio capture needs Android 10 or newer, and the source app must allow capture.
* Fire TV and sender must be on the same local network. The network must allow device to device traffic and multicast DNS discovery.
* Fire TV devices using Vega OS are not supported by these Android APKs.

There is no signed public release yet. Use a debug APK artifact from a successful [Android build](https://github.com/mmorrone90/embercast/actions/workflows/android.yml) or build the APKs from source as described below. GitHub Actions artifacts are temporary test builds, not permanent releases.

## 2. Get the APKs

To download test builds, open the repository's **Actions** tab, select **Android build**, open the latest successful run on `main`, and download the `EmberCast-receiver-debug` artifact. Download `EmberCast-phone-debug` as well if you want to mirror an Android phone. Each artifact is a zip containing its APK. GitHub shows when an artifact expires. If no successful run is available, build the APKs from source:

On a Mac or Linux computer, install JDK 17, Android SDK Platform Tools, Android SDK Platform 36, CMake, and Android NDK `27.0.12077973`. Open the Android SDK Manager and accept the Android SDK and NDK licenses. Then clone the project with its receiver source and build both apps:

Set `ANDROID_HOME` to the Android SDK folder and add Platform Tools to `PATH`. On the default Mac installation:

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

On Linux, use the SDK folder selected in Android Studio.

```sh
git clone --recurse-submodules https://github.com/mmorrone90/embercast.git
cd embercast
./scripts/build-receiver.sh
./scripts/build-phone.sh
```

The scripts create:

* `dist/EmberCast-receiver-debug.apk` for Fire TV
* `dist/EmberCast-phone-debug.apk` for Android phones

The APKs use the local Android debug signing key. An APK signed with a different key cannot update an existing installation. For a fresh install, uninstall the older package first if Android reports a signing conflict. Uninstalling removes that app's settings and saved captures.

## 3. Prepare Fire TV

1. Connect the TV and computer to the same home network.
2. On Fire TV, open **Settings → My Fire TV → About**. On some models the menu is called **Device & Software**.
3. If **Developer Options** is hidden, select the device name seven times with the remote's Select button. Return to **My Fire TV**.
4. Open **Developer Options** and enable **ADB Debugging**. If the TV shows **Install unknown apps**, allow your chosen installer. ADB installation uses the debugging setting.
5. Open **Settings → My Fire TV → About → Network** and note the TV's IP address.

## 4. Install the receiver

Add Android SDK Platform Tools to your computer's `PATH`, then run this from the project folder. Replace `FIRE_TV_IP` with the address from the TV:

```sh
./scripts/install.sh FIRE_TV_IP
```

The TV asks whether to allow USB debugging. Choose **Always allow from this computer**, then **OK**. The script installs and opens EmberCast. If you downloaded the APK artifact instead, unzip it and run:

```sh
adb connect FIRE_TV_IP:5555
adb -s FIRE_TV_IP:5555 install -r EmberCast-receiver-debug.apk
adb -s FIRE_TV_IP:5555 shell am start -n dev.firemirror.receiver/io.github.jqssun.airplay.MainActivity
```

Leave EmberCast open while connecting a sender. The receiver does not need ADB after installation.

## 5. Mirror a Mac

1. Confirm EmberCast is open on the TV.
2. On the Mac, open **Control Centre → Screen Mirroring**.
3. Choose **EmberCast**.
4. To stop, open Screen Mirroring again and disconnect.

The Mac and TV must be able to discover and connect to each other on the local network. A guest network or wireless client isolation can prevent this.

## 6. Install EmberCast Phone and mirror Android

Copy `dist/EmberCast-phone-debug.apk` to the phone and open it, or connect the phone with USB debugging enabled and install it with:

```sh
adb install -r dist/EmberCast-phone-debug.apk
```

Android may ask you to allow installation from that file manager or installer. Then:

1. Open EmberCast on the Fire TV.
2. Open EmberCast Phone on Android and select the TV under Nearby EmberCast.
3. Tap **Mirror this phone** and approve Android's screen capture prompt.
4. Approve the connection on the TV if its approval setting is enabled.
5. On Android 10 or newer, allow playback audio capture if you want supported app audio on the TV. EmberCast Phone silences local media playback while that audio session is active and restores the previous volume when sharing stops.
6. Tap **Stop sharing** on the phone or its sharing notification to end the session.

Some Android apps disallow playback capture. Android's protected video surfaces may show a black image on the TV. EmberCast cannot bypass DRM or those system restrictions.

## Optional: install with an AI assistant

Codex or another AI assistant with access to a local terminal can run the build and ADB installer for you. This still requires Android SDK tools on the computer, ADB debugging enabled on the Fire TV, and both devices on the same local network. The first connection must be approved on the TV itself. An AI chat on a phone or in a browser cannot reach your Fire TV unless it is connected to a computer that has ADB access.

Open the EmberCast project folder in your local coding assistant and paste this prompt, replacing `FIRE_TV_IP` with the address shown in Fire TV network settings:

```text
Read docs/INSTALL.md, then build and install the EmberCast receiver on my Fire TV at FIRE_TV_IP. Check that ADB, Java 17, and the Android SDK are available first. Build with ./scripts/build-receiver.sh and install with ./scripts/install.sh FIRE_TV_IP. Do not uninstall any app, change project files, or install the phone APK on the TV. If the TV asks me to authorize ADB, stop and tell me to accept the prompt, then continue after I confirm. If a command fails, explain the error and wait before trying a different approach. When installation succeeds, tell me how to connect my Mac or Android phone.
```

The phone app is optional and is installed separately on an Android phone. Use the manual steps above or ask the assistant to build it with `./scripts/build-phone.sh` and install the resulting APK on the connected phone. The receiver APK belongs on Fire TV; the phone APK belongs on Android.

## Troubleshooting

**The Mac does not list EmberCast.** Keep the receiver open, verify both devices use the same network, and check that multicast DNS and local device traffic are allowed. The TV's Connection health screen shows service registration status. On the computer, `dns-sd -B _airplay._tcp local.` lists AirPlay receivers visible on the local network.

**The phone does not list the TV.** Check the same network and local traffic settings. On the TV, open Connection health and enter the displayed address manually in the phone app.

**ADB says `unauthorized`.** Look at the TV for the debugging prompt and accept it. Run `adb devices -l` again.

**ADB says `offline` or connection is refused.** Confirm the TV IP, ADB Debugging, and that the computer is on the same network. Do not expose ADB port 5555 to the internet.

**The app cannot update.** Debug APKs built on different computers usually have different signing keys. Uninstall the old package before installing the new APK. This deletes app data.

**Video is black in a streaming app.** The service may protect its video surface. Use that service's native Fire TV app. EmberCast does not remove DRM protection.

**Uninstall the receiver or disconnect ADB.**

```sh
adb -s FIRE_TV_IP:5555 uninstall dev.firemirror.receiver
adb disconnect FIRE_TV_IP:5555
```

To uninstall the phone app through ADB, run `adb uninstall dev.firemirror.phone` while the phone is connected.
