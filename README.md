# Sucstudiolive

Ứng dụng studio livestream dùng Capacitor 7, hướng tới Android và iOS.

## RTMP native
- Plugin native `RtmpPublisher` đã có mã Kotlin/Swift tích hợp HaishinKit.
- H.264/AAC mặc định; H.265/HEVC khi thiết bị có encoder và máy chủ nhận hỗ trợ Enhanced RTMP.
- RTMP/RTMPS trực tiếp từ camera/micro native.
- Logo, banner, ticker strip và scoreboard được vẽ qua native bitmap compositor.
- Không có nhạc nền BGM hoặc chia sẻ màn hình.

## Build
Đọc `HUONG-DAN-BUILD-BANG-IPHONE-VI.md`. Chạy `npm install` rồi `npm run web:sync` trước khi `npx cap sync android` hoặc `npx cap sync ios`.

## Giới hạn hiện tại
Mã native chưa được build bằng Gradle/Xcode và chưa kiểm thử RTMP trên thiết bị thật trong môi trường chỉnh sửa. Native preview chưa được gắn vào vùng preview WebView; ticker hiện là dải chữ tĩnh. Cần CI và kiểm thử thiết bị trước khi phát hành.
