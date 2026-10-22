# License and attribution notices

## EmberCast source code

The application source, Android phone sender, FireMirror modifications, build scripts, and receiver patch are licensed under GNU GPL version 3 only. See [`LICENSE`](LICENSE). The functional receiver is based on Android AirPlay Server revision `c8defdd70d7e6a04f4f1b71d353653682d594106`, included as the `receiver/` Git submodule. Its source retains its upstream copyright and GPL notices. The UxPlay implementation and its patches are also GPL licensed.

The native source dependencies are pinned in the receiver submodules: UxPlay `462153392f2e30937424922039ff9f0cda5e7b1a`, FFmpeg `38b88335f99e76ed89ff3c93f877fdefce736c13`, libplist `f41b1ea67045e0c09339974d83e389972d84f166`, and openssl-cmake `4edd36a8dab5f85a8f92650b5bdf6e0cab13aab8`. Keep their license files and notices when building or distributing. FFmpeg and libplist contain components with different license terms; review their source trees and build configuration before redistributing a binary.

Kotlin standard library is provided by JetBrains under Apache License 2.0. Android platform APIs are supplied by the operating system. The Gradle wrapper is under Apache License 2.0. A copy of Apache License 2.0 is in [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt).

## Project documentation and original artwork

The project authored documentation and artwork listed below are licensed under the MIT License in [`LICENSE-MIT`](LICENSE-MIT):

* `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, and `SUPPORT.md`
* `docs/**`, except the Tabler SVG files in `docs/assets/icons/`
* `artwork/**`

This separate MIT grant does not change the GPL license for application source code, patches, or build scripts.

## Tabler icons

The SVG files in `docs/assets/icons/` are copied from [Tabler Icons](https://github.com/tabler/tabler-icons), copyright Paweł Kuna and contributors, under the MIT License. Their source commit and individual paths are recorded in [`docs/assets/icons/README.md`](docs/assets/icons/README.md). The connection diagram in `docs/assets/mirroring-flow.svg` also adapts their device and Wi Fi icon paths. A copy of the upstream license is in [`licenses/Tabler-Icons-MIT.txt`](licenses/Tabler-Icons-MIT.txt).

AirPlay, Mac, and macOS are Apple trademarks. Fire TV is an Amazon trademark. EmberCast is an independent interoperability project and has no affiliation with Apple or Amazon. This project does not include a license to Apple AirPlay, FairPlay, or DRM technology.
