# Security policy

Use the latest source and rebuild the debug APKs to test a fix. There is no formal release signing key or signed public APK release yet.

The Android phone sender uses a custom local video and audio transport that is not encrypted. Use it only on a trusted local network. Do not forward its ports or the Fire TV ADB port to the internet. A connection approval prompt is an address check, not cryptographic device authentication.

For a suspected security issue, use GitHub's private **Report a vulnerability** flow for this repository when available. Do not publish exploit details, phone captures, private network addresses, device identifiers, or logs containing personal information in a public issue. If private reporting is unavailable, open an issue asking for a private contact route without including the vulnerability details.
