# Architecture

## Implemented in Phase 1

The app is a single Android application module. Native Android Views avoid an extra rendering framework and work with the Fire TV remote through standard directional focus navigation.

`MainActivity` renders a `ReceiverSnapshot`. Its initial state is UNAVAILABLE. The model also defines WAITING, CONNECTING, CONNECTED, DISCONNECTED, and ERROR for real callbacks once a receiver exists. No timer or button simulates a connection.

`NetworkMonitor` observes WiFi and Ethernet using `ConnectivityManager`, reports a LAN IPv4 address, and cleans up its callbacks when the activity stops. It does not require a validated internet route and excludes VPN interfaces. Name and logging settings use private `SharedPreferences`. Network logs report availability, not stream contents.

`DeviceDiagnostics` enumerates codec names on one background executor. It uses Android's hardware acceleration flag where available, and explicitly reports uncertainty on older APIs.

Only ACCESS_NETWORK_STATE is requested. No network service is running, no mDNS is advertised, and there is no INTERNET permission in Phase 1. No Surface or decoder is allocated before the media integration stages.

## Receiver prototype added after Phase 1

The `receiver/` submodule pins Android AirPlay Server and its UxPlay, FFmpeg, libplist, and OpenSSL build dependencies. `scripts/build-receiver.sh` applies the EmberCast patch before compiling. This APK uses the existing JNI protocol bridge, Android NSD advertisement, MediaCodec video path, and upstream audio path. It replaces the earlier interface APK under the same application ID. Port 7100 is the tested default because port 7000 is occupied by Fire OS on the target device.

The baseline receiver advertised both AirPlay and RAOP services and displayed the Mac desktop on the TV. The branded build has been verified to start and advertise. Codec selection, audio, latency, and long running behavior still need measurement. The upstream receiver UI currently owns playback and settings; the original Phase 1 activity is retained as source history, not part of the receiver APK.

## Proposed integration

```text
Mac Control Center
    ↓ Bonjour discovery on the LAN
Android discovery adapter
    ↓ TCP/UDP transport to the actual listening receiver
Pinned UxPlay protocol core with Android JNI bridge
    ↓ decrypted elementary video and timestamped audio
Video: MediaCodec → SurfaceView → HDMI display
Audio: AAC/ALAC decoder → PCM output → TV speakers
    ↓ measured callbacks
ReceiverSnapshot → MainActivity
```

The proposed direction adapts tested upstream components. It does not introduce a custom AirPlay parser, pairing implementation, or crypto layer. The interface remains in Kotlin. Native C/C++ is restricted to the existing receiver library and bridge.

Discovery uses upstream TXT data and real listener ports. Maintain one discovery owner, stable receiver identity, serialized registrations where needed, and complete unregister/release behavior on stop or network changes. Never advertise guessed capability flags or a listener that is not running. Phase 2 may use a clearly labelled private diagnostic service type to test mDNS before an AirPlay server exists.

The service should start only while the app is foreground for the first prototype. A later opt in foreground service needs correct lifecycle and target SDK permissions; boot startup is outside the current scope. A receiver restart, surface loss, sender disconnect, and LAN address change must each release codecs, sockets, discovery registrations, and multicast locks.

Prefer H.264 at 1080p first. HEVC support depends on both sender negotiation and the device decoder, and must be advertised only after tested. Use capability checks for low latency decoder keys, bounded queues, codec configuration records from the actual protocol library, and output to a Surface without software transcoding. Preserve aspect ratio for portrait and landscape senders. Do not disable audio timestamps to create a misleading latency improvement.

Audio should reuse the upstream synchronization and codec path, with AudioTrack or the upstream Oboe output depending on the adopted bridge. Preserve stream timestamps and clock conversion from the receiver rather than inventing a wall clock. Report rendered frame rate from measured output events, not the requested refresh rate. Phase 7 measures queue depth, frame drops, memory, thermal behavior, and reconnection stability on the actual Fire TV.
