# Sucstudiolive — trạng thái native RTMP

## Đã viết trong source
- Capacitor plugin package local `@sucstudiolive/rtmp-publisher`.
- JavaScript bridge được bundle vào `www/native-bridge.js`; app HTML gọi plugin đã đăng ký thay vì chỉ dựa vào `Capacitor.Plugins`.
- Android: HaishinKit.kt 0.14.2; CameraSource + AudioRecordSource; RTMP/RTMPS; H.264 video + AAC audio; đợi status `NetStream.Publish.Start` mới báo LIVE.
- iOS: HaishinKit.swift 1.9.1; AVFoundation camera/microphone; VideoToolbox hardware encoder; H.264/AAC; tùy chọn HEVC/H.265 nếu máy có hardware encoder và server nhận Enhanced RTMP; đợi status publish.
- Logo, banner, ticker và scoreboard được vẽ thành bitmap native trong plugin và đưa vào screen compositor trước khi stream encode; `updateOverlay()` cập nhật trong lúc live.
- UI ngừng camera/micro WebView trước khi native camera/micro mở để tránh chiếm thiết bị hai lần.
- Có sự kiện `rtmpStatus` để UI nhận biết server đóng kết nối.

## Giới hạn được chủ động bật/tắt
- Android có cấu hình HEVC Main nếu thiết bị có encoder HEVC phần cứng; máy chủ nhận vẫn phải hỗ trợ Enhanced RTMP/HEVC. Nếu không đủ khả năng phần cứng, plugin từ chối H.265.
- iOS HEVC chỉ được chọn nếu VideoToolbox báo có mã hóa HEVC phần cứng; máy chủ đích vẫn phải hỗ trợ Enhanced RTMP/HEVC.
- Preview camera native chưa được gắn vào vùng preview của WebView. Khi bắt đầu phát, preview web được dừng để nhường camera cho native publisher; luồng phát vẫn lấy hình từ camera native.
- Không có chia sẻ màn hình hoặc nhạc nền theo yêu cầu trước đó.

## Chưa được xác nhận
- Chưa chạy Android Gradle build hoặc Xcode/CocoaPods build trong môi trường sửa mã.
- Chưa kiểm thử thiết bị thật, RTMP/RTMPS tới YouTube/Facebook/custom ingest, reconnect, đổi camera, nhiệt độ hoặc livestream dài.
- Cần xem kết quả Codemagic CI; nếu có lỗi compile do khác biệt API thư viện, cần sửa theo log CI.
- Đây là bản phát triển có mã nguồn native và pipeline thực, chưa được xác nhận production-ready.

## Build
Từ thư mục dự án:
1. `npm install`
2. `npm run web:sync` (đồng bộ HTML, build plugin TS và bundle native bridge)
3. `npx cap add android` / `npx cap add ios` nếu thư mục nền tảng chưa tồn tại
4. `npx cap sync android` hoặc `npx cap sync ios`
5. Build Android bằng `cd android && ./gradlew assembleDebug`
6. Build iOS bằng Xcode/Codemagic; cần Apple signing để cài lên iPhone.

Không đưa Stream Key, mật khẩu, signing certificate hay keystore vào GitHub.
