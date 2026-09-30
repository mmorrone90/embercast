# AirPlay receiver feasibility

Initial research date: 28 September 2026. The target Mac was inspected locally: macOS 26.5.2, build 25F84, arm64. On 30 September, an Amazon Fire TV Stick (3rd Gen) running Fire OS 7 and Android API 28 was connected and tested. Actual video decoder selection remains unverified.

## Decision

Use the existing UxPlay protocol implementation through an Android adaptation. The selected source is [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server), revision `c8defdd70d7e6a04f4f1b71d353653682d594106`, pinned in the `receiver/` submodule and configured by the checked in FireMirror patch. Its source includes an Android DNS discovery shim, JNI callbacks, native protocol build, and media renderers. The earlier Phase 1 interface did not include this code; the receiver prototype now does.

Upstream reports Android 7+, Android TV, macOS senders, H.264/HEVC mirroring, AAC/ALAC audio, and optional PIN pairing. It builds native libraries for both ARM ABIs and x86_64. The Kotlin video path uses MediaCodec. Native audio includes Oboe and an FFmpeg ALAC decoder. There is no need to port the desktop GStreamer application wholesale. These claims and components were checked against the [README](https://github.com/jqssun/android-airplay-server/blob/c8defdd70d7e6a04f4f1b71d353653682d594106/README.md), [Android build](https://github.com/jqssun/android-airplay-server/blob/c8defdd70d7e6a04f4f1b71d353653682d594106/app/build.gradle.kts), and [CMake source](https://github.com/jqssun/android-airplay-server/blob/c8defdd70d7e6a04f4f1b71d353653682d594106/app/src/main/cpp/CMakeLists.txt).

This particular Fire OS 7 device has now built and run the receiver, advertised itself to the Mac, and displayed the Mac desktop. Compatibility with other Fire TV models remains an inference from Android APIs and ABI support. Fire OS 6 and newer Android based devices may meet API 24; older Fire OS 5 devices do not. Check the actual API using ADB. Vega OS is a separate target requiring its own package and SDK. [Amazon distinguishes Fire OS Android binaries from Vega VPKGs](https://developer.amazon.com/docs/app-submission/target-app.html).

## Alternatives

| Candidate | License | Android and Fire OS fit | Mirroring, Mac, and audio | Decision |
| :--- | :--- | :--- | :--- | :--- |
| Android AirPlay Server | GPLv3 | Existing NDK/JNI Android 7+ implementation; Fire TV needs testing | Reports Mac mirroring, H.264/HEVC, AAC/ALAC, PIN pairing | Best current integration starting point |
| [FDH2/UxPlay](https://github.com/FDH2/UxPlay) | GPLv3 | Maintained native protocol core; desktop executable uses GStreamer and platform DNS | Reports Mac desktop mirroring and audio; explicitly excludes AirPlay 2 multiroom audio | Use core through Android bridge |
| [RPiPlay](https://github.com/FD-/RPiPlay) | GPLv3 with mixed component notices | Raspberry Pi OpenMAX or desktop GStreamer; substantial Android port needed | Mirroring and audio reported, old compatibility claims | Older base; not first choice |
| [FirePlay](https://github.com/jimnoneill/FirePlay) | GPLv3 | Fire TV focused, native MediaCodec and AAudio | README gives iOS device evidence, but contradictory mirroring status; current Mac compatibility unproven | Secondary reference, not accepted as verified Mac receiver |
| [iMirror](https://github.com/prat3ik/iMirror) | GPLv3 combined work | Kotlin Android TV / Fire TV project | Reports H.264 mirroring and audio with pairing; Playfair required | Alternative worth testing; preserve its component notices |

GitHub API inspection found last repository push dates of 23 August 2026 for the Android adaptation and iMirror, 28 September 2026 for UxPlay, 25 June 2026 for FirePlay, and 14 April 2023 for RPiPlay. These are activity snapshots, not proof of maintenance quality or working releases.

UxPlay's AirPlay naming does not imply complete implementation of every feature branded AirPlay 2. Test the desktop mirroring path from this Mac specifically. Music app casting, HLS casting, protected media, and multiroom audio are separate requirements; they are not substitutes for successful Control Center mirroring.

## Known compatibility risks

The selected Android project currently has open reports of [mirroring disconnects on a MediaTek Android TV](https://github.com/jqssun/android-airplay-server/issues/50) and [HEVC decoder detection failure](https://github.com/jqssun/android-airplay-server/issues/51). They concern a Hisense TV, not a verified Fire TV failure. The ashmem cause in issue 50 is a reporter's hypothesis. Do not disable SELinux or claim the cause is confirmed. Reproduce and trace actual failures on the target Fire TV.

Begin with H.264 1080p. Query device codec capabilities and verify real decoded output before adding HEVC or 60 FPS capability advertising. A codec appearing in MediaCodecList does not guarantee low latency, usable output, or correct configuration on a vendor device.

Inspect and pin native submodule commits, including UxPlay, libplist, OpenSSL build support, and FFmpeg. Keep the upstream Android patches with their original notices. Before removing HLS, media browsing, or any unrelated UI dependency, establish a working upstream baseline on the target devices.

## Discovery and connection requirements

Bonjour discovery normally advertises `_airplay._tcp` and `_raop._tcp` with protocol feature TXT data, a persistent receiver identity, and live listener ports. Android's NSD and multicast behavior must be tested with the Mac resolver. Use the adopted receiver's own capability construction rather than copying a random Apple TV TXT record. Reconcile registration callbacks and stop behavior, especially with two services.

Discovery alone is insufficient. The receiver must accept the Mac's control requests, pairing and verification, negotiated stream setup, timing/event traffic, and encrypted mirroring media. Reuse the existing library for binary property lists, transport framing, cryptography, session keys, and timing. Do not implement a guessed RTSP endpoint that merely makes a device name appear.

The initial receiver works on an existing shared LAN. Wireless direct Apple AWDL support is not required. Do not add router forwarding or public listeners. When the receiver is integrated, use a supported local interface strategy and test binding changes on WiFi reconnect. Where a library listens on all interfaces, document that fact and restrict exposure rather than claiming it is automatically confined to a subnet.

## Encryption, licensing, and protected content

Normal desktop mirroring can still use encrypted transport. Pairing, pair verification, and FairPlay related setup are compatibility components handled by the existing receiver. OpenSSL provides open crypto primitives; libplist parses property lists. This is not the same as obtaining Apple's proprietary AirPlay certification or a FairPlay DRM license.

Playfair code exists in the inspected native build. Its historical provenance needs care: [RPiPlay's upstream disclaimer](https://github.com/FD-/RPiPlay#disclaimer) explicitly describes uncertainty around that library, and [iMirror's notices](https://github.com/prat3ik/iMirror/blob/main/NOTICE.md) preserve the same concern. A GPL notice by itself does not settle patent, trademark, or reverse engineering questions. No Apple signed binary, proprietary SDK, device certificate, or commercial DRM credential is supplied here.

Adopting GPL receiver code requires preserving its license and component notices. For APK distribution, provide the corresponding complete source, build scripts, modifications, and dependent component source under the applicable licenses. [GNU GPL version 3](https://www.gnu.org/licenses/gpl-3.0.html) describes those distribution requirements. The current project uses GPL version 3 only to align with this direction; the Phase 1 runtime contains no receiver code.

No protected content decryption or DRM bypass is implemented. Tests target unprotected desktop output. Protected streaming applications may display a black region or refuse playback; that is outside scope.

## Remaining evidence

The pinned upstream baseline built and mirrored this Mac to the Fire TV after moving from occupied port 7000 to 7100. The FireMirror branded build started and advertised on the same device. Complete a fresh branded connection test, then record the negotiated codec, decoder in use, audio behavior, frame rate, latency, reconnect behavior, and sustained playback. A positive report for iOS, another Mac version, or a different TV is useful evidence, not a substitute for this test.
