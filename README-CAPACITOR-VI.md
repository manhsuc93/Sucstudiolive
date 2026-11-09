# Sucstudiolive — trạng thái triển khai Capacitor

## Mục tiêu
- Phát RTMP/RTMPS trực tiếp từ camera/micro native, không qua VPS relay.
- Không dùng WHIP.
- Không có nhạc nền BGM và chia sẻ màn hình.
- Giữ overlay banner, logo, ticker và bảng điểm.

## Mã nguồn hiện có
- `Studio_Livestream.html`: giao diện studio, cấu hình RTMP URL/Stream Key, chất lượng và codec.
- `native/rtmp-publisher/`: package Capacitor local với plugin Kotlin/Swift.
- Android: HaishinKit.kt 0.14.2, CameraSource + AudioRecordSource, H.264/AAC, HEVC khi encoder phần cứng có, RTMP/RTMPS.
- iOS: HaishinKit.swift 1.9.1, AVFoundation + VideoToolbox, H.264/AAC và HEVC khi phần cứng hỗ trợ, RTMP/RTMPS.
- Native bitmap compositor vẽ logo/banner/ticker strip/scoreboard trước khi encode.
- `scripts/native-bridge.ts` đăng ký plugin JS; `npm run web:sync` bundle bridge vào `www/native-bridge.js`.
- Codemagic workflows build APK debug và iOS unsigned.

## Giới hạn và kiểm thử
- Chưa chạy Gradle/Xcode build hoặc test RTMP trên thiết bị thật từ môi trường chỉnh sửa.
- Native preview chưa được ghép vào vùng preview WebView. Camera/micro web dừng khi native capture bắt đầu.
- Ticker hiện là dải chữ tĩnh, chưa chạy cuộn.
- H.265 chỉ hoạt động khi thiết bị có encoder HEVC và server ingest hỗ trợ Enhanced RTMP/HEVC.
- IPA unsigned không cài trực tiếp lên iPhone; cần ký bằng Apple Developer signing hoặc TestFlight.

## Build
```bash
npm install
npm run web:sync
npx cap sync android
npx cap sync ios
```
Nếu thư mục platform chưa tồn tại, chạy `npx cap add android` hoặc `npx cap add ios` trước `cap sync`.

Tài liệu upstream:
- https://github.com/HaishinKit/HaishinKit.kt
- https://github.com/HaishinKit/HaishinKit.swift
