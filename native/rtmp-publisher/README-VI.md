# RtmpPublisher — Capacitor native RTMP

## Engine versions
- Android: HaishinKit.kt 0.14.2.
- iOS: HaishinKit.swift 1.9.1 (CocoaPods).

## Implemented pipeline
- Camera + microphone capture natively on the device.
- H.264 + AAC on Android and iOS.
- RTMP/RTMPS publish; plugin resolves `start()` only after server returns `NetStream.Publish.Start`.
- H.265/HEVC profile selection on Android and iOS when the platform reports a hardware encoder. The ingest server must also support Enhanced RTMP/HEVC.
- Native bitmap composition for logo, banner, ticker strip and scoreboard, updated through `updateOverlay()`.
- `rtmpStatus` event notifies the JS UI if the RTMP connection closes.

## Important limitations
- The native camera preview has not yet been embedded into the WebView preview region. The WebView camera/microphone are stopped when native capture starts to prevent device contention; the native camera continues sending video to RTMP.
- The ticker is currently a static lower-third strip, not a continuously scrolling ticker animation.
- No Android/iOS CI build or physical-device RTMP test has been run in the editing environment.
- This is an integration-development build, not a release-certified app.

## API
- `start(options)`
- `stop()`
- `updateOverlay(overlay)`
- `getCapabilities()`
- event `rtmpStatus`
