# Sucstudiolive — hướng dẫn đồng bộ mã nguồn

## Trạng thái
Dự án dùng Capacitor 7. Tệp giao diện chính là `Studio_Livestream.html`; script `scripts/sync-web.mjs` tạo lại thư mục `www/` để Capacitor đóng gói.

## Đồng bộ giao diện
Sau khi sửa giao diện, chạy:

```bash
npm run web:sync
```

## Tạo dự án native
Cần Node.js 20+ và npm.

```bash
npm install
npm run android:add
npm run cap:sync
```

Để tạo dự án iOS cần máy macOS có Xcode:

```bash
npm run ios:add
npm run cap:sync
```

## Lưu ý quan trọng về RTMP
Giao diện hiện có các trường RTMP URL/Stream Key và lời gọi Capacitor `RtmpPublisher`. Tuy nhiên plugin native `RtmpPublisher` chưa có trong gói mã này. Vì vậy app chưa phát RTMP thật; không thể cài PWA rồi mong phát RTMP trực tiếp từ trình duyệt.

Muốn hoàn thiện cần plugin native Android/iOS tự quản lý camera, micro, preview, H.264/AAC encoder và RTMP/RTMPS connection. Các lớp đồ họa cần được chuyển vào native compositor để xuất hiện trong video live.

## Phát hành
- Android: Android Studio + Android SDK; cần build và ký APK/AAB.
- iOS: macOS + Xcode; cần Bundle ID, Team và signing/provisioning profile hợp lệ.
- Chỉ build thành công không có nghĩa là livestream đã được kiểm thử. Phải test trên thiết bị thật và xác nhận video/âm thanh ở đích phát.
