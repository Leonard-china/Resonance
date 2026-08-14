# Resonance Android 0.1.1 test report

Date: 2026-08-14

## Root cause and fix

The 0.1.0 launch crash was reproduced on MuMu. `AndroidPlatformServices` registered Activity Result launchers from inside Compose after `MainActivity` had reached `RESUMED`, which violates `ActivityResultRegistry` lifecycle requirements. Services and launchers are now created synchronously in `onCreate` before `setContent`.

## Test environments

- Official Android Emulator: Android 16 / API 36, 320x640 low-density stress profile, portrait and landscape.
- MuMu Player 12: Android 12 / API 32, 1080x1920 and 1920x1080, vendor-emulator comparison.
- Windows 11 distributable build: Compose Desktop application launched from the packaged output.

## Verified scenarios

- Signed release clean install and repeated cold starts: Android 16 (5), MuMu (3), no `AndroidRuntime` or Compose crash.
- First-run audio permission: one tap grants permission and resumes scanning automatically.
- MediaStore scan: MP3 metadata separates title, artist and album; duration and embedded artwork are read.
- Playback: Media3 foreground media session, play, pause, resume, next, progress and background playback.
- Audio focus/noisy-route handling enabled in the release playback service.
- KuGou public playlist import through the official signed endpoint using `https://t1.kugou.com/1tVfj4cG4V3`: playlist `Leonard`, 21 tracks.
- Imported playlist survives force-stop/restart; unavailable catalog entries are clearly marked and do not fake playback.
- Catalog-to-local rematching: local `Hotel California` / `Eagles` automatically changed the playlist from 0 to 1 playable match.
- KGMA v3/slot-1 fixture containing MP3: folder permission, streaming decrypt, managed MP3 output, metadata/artwork and source-file hash preservation.
- Responsive rendering on compact phone, Android landscape, MuMu portrait/landscape, and packaged Windows desktop.
- Accessibility stress pass at 1.5x system font scale with all system animations disabled; the compact 320x640 Android 16 layout remained usable and crash-free.
- Desktop unit suite: conversion, KGMA cipher, encrypted sync package, LAN sharing and KuGou parsing.
- Release: versionCode 2, versionName 0.1.1, minSdk 26, targetSdk 36, RSA-4096 APK Signature Scheme v2.

Final APK SHA-256: `785B03B9EFD976E24953DAB2D2AA9CAD261BFD228A6E0669D54782EE8DBD8361`

## Known boundary

This section described 0.1.1. It is superseded by 0.1.2 below: Android now includes an FFmpeg audio encoder for KGMA payloads containing FLAC/Ogg.

## 0.1.2 follow-up

- Fixed the import-page overlap by replacing a multi-child `Box` with a vertically spaced `Column`.
- Added Android FFmpeg audio transcoding for FLAC/Ogg payloads inside KGMA; MP3 payloads stay lossless.
- Added multi-file KGMA selection; processing is sequential and each source KGMA is deleted only after successful MP3 creation.
- Added confirmed local MP3 deletion and playlist/index cleanup.
- Added KuGou network retries and a readable VPN/private-DNS troubleshooting message.
