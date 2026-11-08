# Sucstudiolive — hướng dẫn build bằng iPhone (không cần Mac cá nhân)

## Đã chuẩn bị trong gói này
- Giữ dự án Capacitor 7 cho Android và iOS.
- Đồng bộ `www/` từ `Studio_Livestream.html` bằng `npm run web:sync`; tránh dùng bản www cũ.
- Thêm bộ khung plugin `RtmpPublisher` cho Android/iOS. Bộ khung này **chưa có engine RTMP thật** và cố ý trả lỗi `RTMP_ENGINE_NOT_LINKED`, không giả lập phát trực tiếp.
- Thêm 2 workflow Codemagic: build APK debug Android và build iOS unsigned.
- Không cần cài Android Studio/Xcode trên iPhone; việc build chạy trên máy cloud.

## Cách dùng từ iPhone
1. Tải ZIP này và giải nén trong ứng dụng Tệp.
2. Mở https://codemagic.io/ bằng Safari và đăng nhập.
3. Kết nối tài khoản GitHub, chọn repository Sucstudiolive hoặc tạo repository mới rồi upload toàn bộ nội dung ZIP đã giải nén.
4. Chọn workflow `live-studio-android-debug` để build APK thử nghiệm.
5. Khi build thành công, mở Artifacts và tải APK về Android để cài thử. iPhone không cài được APK.
6. Chọn workflow `live-studio-ios-unsigned` để kiểm tra build iOS. IPA unsigned **không cài trực tiếp lên iPhone**; cần Apple Developer signing/provisioning hoặc phân phối TestFlight.

## Điều kiện để cài app iPhone
- Cần Apple Developer account phù hợp và cấu hình signing trong dịch vụ build, hoặc phát hành qua App Store Connect/TestFlight.
- Một file IPA unsigned chỉ chứng minh bước biên dịch, không phải file cài đặt hợp lệ.

## Trạng thái RTMP — cần đọc kỹ
Hiện plugin trong `native/rtmp-publisher/` chỉ là bridge scaffold:
- Android Java và iOS Swift nhận lời gọi Capacitor.
- `start()` kiểm tra URL/Stream Key rồi trả `RTMP_ENGINE_NOT_LINKED`.
- Chưa có camera capture native, H.264/AAC encoder, RTMP socket publisher hay native overlay compositor.
- Vì vậy APK/IPA build được từ workflow này **chưa phát RTMP trực tiếp**. Không được phát hành hoặc quảng cáo đây là bản RTMP hoàn chỉnh.

## Việc kỹ thuật tiếp theo
1. Tích hợp HaishinKit Android và Swift với version tương thích toolchain đã chọn.
2. Viết capture pipeline camera/microphone, H.264/AAC, RTMP/RTMPS connect/publish/disconnect.
3. Đưa overlay (logo/banner/ticker/scoreboard) vào native compositor trước encoder.
4. Đăng ký plugin đúng theo Capacitor 7 trên cả hai nền tảng.
5. Kiểm thử thiết bị thật Android và iPhone, lỗi mạng, quyền, chuyển camera, đồng bộ tiếng-hình, nhiệt độ và thời lượng dài.
6. Chỉ khi hoàn thành và kiểm thử mới đổi mã lỗi thành trạng thái live thành công.

## Bảo mật
Không commit Stream Key, mật khẩu, chứng chỉ ký hoặc keystore vào GitHub. Dùng biến môi trường/secret của dịch vụ build.
