# Staged implementation and acceptance gates

Build and lint after each implemented phase. A successful build proves compilation, not AirPlay compatibility. Preserve evidence from the actual Mac and Fire TV before treating a device acceptance gate as passed.

| Phase | Current state | Acceptance gate |
| :--- | :--- | :--- |
| 1. TV application | Interface prototype smoke tested on Fire TV; full checklist incomplete | Install, launch, navigate, rename, and inspect diagnostics on Fire TV |
| 2. Bonjour | Fire TV advertisement and Mac discovery verified; unregister untested | Discover the actual service from a second LAN computer, verify TXT and port, and test unregister |
| 3. Receiver integration | Pinned upstream receiver built; baseline mirrored; branded connection pending | Build pinned Android receiver bridge, verify licenses, and see receiver in macOS Screen Mirroring |
| 4. Video transport | Baseline Mac desktop video reached Fire TV; codec not recorded | Connect from this Mac and record actual negotiated codec and received frames |
| 5. Display video | Baseline displayed Mac desktop; hardware decoder mode unverified | Decode H.264 to SurfaceView on the Fire TV with aspect ratio, no simulated frames |
| 6. Audio | Not verified | Hear unprotected desktop audio and measure synchronization |
| 7. Performance | Pending | Measure real frame rate, latency, disconnect recovery, memory, and sustained playback |
| 8. Settings and status | Upstream receiver settings included; FireMirror integration needs testing | Verify all settings against real playback and protocol callbacks |

## Phase 1 test on the Fire TV

1. Install the APK following INSTALL.md. Launch FireMirror from the TV launcher.
2. Verify fullscreen landscape layout and the headline Ready to Mirror.
3. Verify the phase notice, receiver name, real local IP, Receiver not installed status, and No stream values.
4. Move left and right with the remote between Settings and Diagnostics. The focused button has a thick pale green outline. Select opens the chosen screen. Back returns.
5. Open Settings. Change the receiver name, save, and confirm the main screen updates. Force stop and relaunch; verify the name persists. Cancel must leave the saved name unchanged.
6. Try an empty name and a name longer than 63 UTF8 bytes. Both must show an error and prevent save.
7. Open Diagnostics and verify the model, API level, build, ABI, LAN address, and codec list. Refresh should not freeze navigation.
8. Enable Diagnostic logging, return to the main screen, and verify the local logcat messages.
9. Disconnect and reconnect WiFi. Verify the address clears and returns. Ethernet should also work. No internet route should be required.
10. Confirm FireMirror does not appear in Screen Mirroring yet. This is the expected result for Phase 1.

Do not mark the full Phase 1 checklist complete from an emulator test or from the narrower Fire TV smoke test. The receiver prototype has since been added and tested separately; see VALIDATION.md.

## Phase 2 and subsequent tests

On the Mac, these commands can inspect the receiver's current Bonjour services:

```sh
dns-sd -B _airplay._tcp local.
dns-sd -B _raop._tcp local.
dns-sd -L 'Living Room Fire TV' _airplay._tcp local.
```

Browsing confirms discovery only. It does not prove screen mirroring. Confirm that the resolved port belongs to a running server. Require successful macOS pairing, SETUP, incoming video frames, and actual hardware output as separate gates.

The same subnet is required for the initial prototype. Guest networks and wireless client isolation can prevent discovery. Disable neither platform security controls nor SELinux to make the receiver pass. Keep protected streaming content outside the test set.
