# Contributing

Thanks for helping improve EmberCast. Start with a fork and a branch, then keep the change small enough to test on its target device.

## Set up

Clone with all source submodules, install JDK 17 and Android SDK Platform 36, and accept the required SDK and NDK licenses:

```sh
git clone --recurse-submodules https://github.com/mmorrone90/embercast.git
cd embercast
```

The receiver build also needs NDK `27.0.12077973` and CMake. For install and device setup, see [docs/INSTALL.md](docs/INSTALL.md). For build and release checks, see [docs/PROCEDURES.md](docs/PROCEDURES.md).

## Make and check a change

Build and test the affected components before opening a pull request:

```sh
./gradlew :app:assembleDebug :app:lintDebug
./gradlew :phone:testDebugUnitTest :phone:lintDebug
./scripts/build-phone.sh
./scripts/build-receiver.sh
(cd receiver && ./gradlew :app:testDebugUnitTest)
```

The receiver source is a pinned upstream Git submodule. Product changes belong in the root receiver patch and must keep the upstream license notices. Do not commit local device backups, debug APKs, keystores, network addresses, or private recordings.

## Pull request checklist

* Describe the user visible change and the affected device.
* List the commands and physical devices used for testing. Mark untested behavior clearly.
* Include screenshots only when they contain no personal or network information.
* Check license and attribution requirements for new code, icons, fonts, and media.
* Do not add DRM circumvention, secret credentials, or an unencrypted service exposed beyond the local network.

## License

Application code and receiver patches are GPL 3.0 only. Project authored documentation and original artwork listed in [NOTICE.md](NOTICE.md) are MIT licensed. Third party assets keep their own notices.
