# Notices

FireMirror Phase 1 is newly authored Kotlin and Android resource code, licensed under GPL version 3 only. The later receiver APK is based on Android AirPlay Server revision `c8defdd70d7e6a04f4f1b71d353653682d594106`, pinned as the `receiver/` submodule. FireMirror's modifications are recorded in `patches/receiver-firemirror.patch`; the build script applies them before compilation. The upstream project and UxPlay are GPL version 3. The upstream README and source attribution remain in the pinned submodule.

The native dependencies are pinned by the receiver's own submodules: UxPlay `462153392f2e30937424922039ff9f0cda5e7b1a`, FFmpeg `38b88335f99e76ed89ff3c93f877fdefce736c13`, libplist `f41b1ea67045e0c09339974d83e389972d84f166`, and openssl-cmake `4edd36a8dab5f85a8f92650b5bdf6e0cab13aab8`. Their licenses and component notices are in their respective source trees. FFmpeg includes LGPL licensed code and optional components with other terms; libplist includes LGPL and GPL notices; openssl-cmake is MIT licensed. See the exact files and build configuration before redistributing a binary.

Kotlin standard library is provided by JetBrains under Apache License 2.0. Android platform APIs are supplied by the operating system. The generated Gradle wrapper belongs to the Gradle project and is distributed under Apache License 2.0. See [Kotlin license](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) and [Gradle license](https://github.com/gradle/gradle/blob/master/LICENSE).

A copy of Apache License 2.0 is included in `licenses/Apache-2.0.txt`. Kotlin is copyright JetBrains s.r.o. and Kotlin Programming Language contributors. Gradle is copyright the original author or authors of the Gradle project; the generated wrapper's original headers are retained.

AirPlay, macOS, and Mac are Apple trademarks. Fire TV is an Amazon trademark. Names describe interoperability targets only.

This source and the linked submodules are needed to reproduce the receiver APK. This file does not imply an Apple AirPlay or FairPlay license.
