# EmberCast

<div align="center">
  <img src="artwork/fire_tv_icon_1280x720.png" width="880" alt="EmberCast, a phone and television joined by a cast signal">
  <p><strong>Mirror your Mac or Android phone to your Fire TV.</strong></p>
  <p>Keep both devices on the same local network. No cloud account is needed.</p>
  <p>
    <a href="docs/INSTALL.md">Install EmberCast</a> ·
    <a href="docs/INSTALL.md#optional-install-with-an-ai-assistant">Use an AI assistant</a> ·
    <a href="docs/PROCEDURES.md">Build and test</a> ·
    <a href="SUPPORT.md">Get support</a>
  </p>
  <p>
    <a href="https://github.com/mmorrone90/embercast/actions/workflows/android.yml"><img alt="Android build status" src="https://github.com/mmorrone90/embercast/actions/workflows/android.yml/badge.svg?branch=main"></a>
    <a href="LICENSE"><img alt="Application code license: GPL 3.0 only" src="https://img.shields.io/badge/code-license%20GPL--3.0--only-blue"></a>
  </p>
</div>

EmberCast turns an Android based Fire TV into a local screen mirroring receiver. Macs connect through AirPlay. Android phones connect with the separate EmberCast Phone app.

## How it works

![Animated diagram showing Mac and Android screen sharing over a local Wi Fi network to EmberCast on Fire TV](docs/assets/mirroring-flow.svg)

## What you can do

* Mirror a Mac desktop through AirPlay. This path was tested on a Fire TV Stick 3rd Gen.
* Mirror an Android phone with EmberCast Phone. Supported playback audio can play through the TV on Android 10 and newer.
* Approve new phone connections on the TV.
* Fit, crop, stretch, or zoom the picture, check connection health, show AirPlay photos, save screenshots, and record silent local clips.

## Compatibility and limits

* The receiver runs on Android based Fire OS. Amazon Vega OS is not supported.
* iPhone and iPad mirroring uses AirPlay, but has not been verified on an Apple phone or tablet.
* Protected video can appear black even when subtitles or controls remain visible. EmberCast does not bypass DRM. Use the streaming service's Fire TV app for protected video.
* Android phone video travels over the local network without encryption. Use EmberCast only on a trusted network.
* Google Cast is not implemented. Fire TV's built in Miracast feature is separate.

Audio and picture quality depend on the sender, network, codec, and TV hardware. See [validation notes](docs/VALIDATION.md) for tested devices and remaining gaps.

## Install and try it

There is no signed release APK yet. You can download temporary test APKs from a successful [Android build](https://github.com/mmorrone90/embercast/actions/workflows/android.yml) run, or build them yourself. The [step by step guide](docs/INSTALL.md) covers manual installation and the AI assisted option.

To build from source, install JDK 17 and Android SDK tools, then run:

```sh
git clone --recurse-submodules https://github.com/mmorrone90/embercast.git
cd embercast
./scripts/build-receiver.sh
./scripts/build-phone.sh
```

The receiver APK is saved as `dist/EmberCast-receiver-debug.apk`. The optional Android sender is `dist/EmberCast-phone-debug.apk`. To install the receiver, enable ADB debugging on Fire TV and run:

```sh
./scripts/install.sh FIRE_TV_IP
```

Open EmberCast on the TV. On a Mac, choose **Control Centre → Screen Mirroring → EmberCast**. On Android, open EmberCast Phone, select the TV, and follow the screen capture and TV approval prompts.

## Development

The root `app/` folder is an earlier TV interface prototype. The functional receiver is built from the pinned `receiver/` source and `patches/receiver-embercast.patch`. Android application IDs retain their older `dev.firemirror.*` names so existing installations can receive compatible updates. See [contributing](CONTRIBUTING.md) and [development procedures](docs/PROCEDURES.md). GitHub Actions builds both APKs and runs project checks.

## License

Application code and receiver changes are GPL 3.0 only because the receiver is based on GPL licensed upstream code. The MIT license applies only to the documentation and original artwork listed in [NOTICE.md](NOTICE.md). Tabler device icons have their own MIT license.

EmberCast is independent and is not affiliated with Apple or Amazon. AirPlay, Mac, macOS, and Fire TV are names of their respective owners.
