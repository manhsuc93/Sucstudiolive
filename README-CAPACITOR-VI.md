# Sucstudiolive — trạng thái chuyển sang RTMP trực tiếp

## Mục tiêu

- Phát tới một đích RTMP/RTMPS do người dùng chọn (YouTube, Facebook hoặc máy chủ tùy chỉnh).
- Không dùng VPS relay, MediaMTX, FFmpeg trung gian hoặc WHIP.
- Không có nhạc nền BGM và không có chức năng chia sẻ màn hình.
- Giữ các công cụ banner, logo, ticker và bảng điểm trong giao diện studio.

## Trạng thái mã nguồn trong gói này

- Giao diện `Studio_Livestream.html` đã có phần nhập RTMP URL và Stream Key.
- `www/` đã được đồng bộ lại từ `Studio_Livestream.html` bằng `npm run web:sync`; đây là thư mục Capacitor đang đóng gói (`webDir: www`).
- Mã JavaScript hiện gọi `window.Capacitor?.Plugins?.RtmpPublisher.start()` và `.stop()`.
- **Plugin native `RtmpPublisher` chưa được triển khai trong gói này.** Vì vậy nút phát sẽ báo chưa có plugin và bản web/PWA không thể phát RTMP trực tiếp.
- Chưa có dự án native `android/` hoặc `ios/`; Capacitor CLI sẽ sinh chúng trên máy build.
- Chưa build APK/IPA, chưa ký ứng dụng và chưa kiểm thử trên thiết bị thật.

## Kiến trúc native bắt buộc để hoàn thiện

Trình duyệt không thể chuyển một `MediaStream` qua Capacitor bridge như một luồng video/audio native. Plugin native phải tự quản lý:
- Quyền camera và micro.
- Camera capture và preview native.
- H.264 hardware encoding và AAC audio encoding.
- Kết nối RTMP/RTMPS trực tiếp, reconnect và dừng phiên.
- Trạng thái kết nối, lỗi và thống kê gửi về giao diện.

Để banner/logo/ticker/bảng điểm xuất hiện trong video gửi đi, các lớp đồ họa cần được render trong native video compositor hoặc được chuyển thành dữ liệu overlay mà compositor native hiểu. Canvas đang hiển thị trong WebView **không tự động** đi vào luồng native RTMP.

Thư viện nền tảng đang được đánh giá:
- Android: HaishinKit.kt (RTMP publisher, camera/microphone, video mixing).
- iOS: HaishinKit.swift (RTMP publisher, camera/microphone, video mixing).

Tài liệu chính thức:
- https://github.com/HaishinKit/HaishinKit.kt
- https://github.com/HaishinKit/HaishinKit.swift

## Chuẩn bị môi trường

Yêu cầu Node.js 20+ và npm. Android cần Android Studio + Android SDK. iOS cần macOS + Xcode.

```bash
npm install
npm run android:add
npm run cap:sync
```

Trên macOS để tạo dự án iOS:

```bash
npm run ios:add
npm run cap:sync
```

## Kiểm thử hiện có

- Các khối JavaScript inline trong HTML đã được kiểm tra cú pháp bằng `node --check`.
- `npm run web:sync` chạy thành công và cập nhật `www/index.html`.
- Chưa kiểm thử build Android/iOS hoặc phát RTMP thật.

## Bảo mật

Không đưa Stream Key thật vào GitHub, ảnh chụp màn hình, log hoặc báo cáo lỗi. Chỉ nhập khóa phát trong ứng dụng trên thiết bị của bạn.
