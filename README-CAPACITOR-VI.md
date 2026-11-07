# Live Studio — Capacitor Android/iOS

## Trạng thái bản này

Đã tạo cấu trúc Capacitor 7, cấu hình app ID `vn.livestudio.mobile`, assets web, hướng dẫn quyền camera/micro,
nút ghi chương trình cục bộ (MediaRecorder), và sửa lỗi `getProgramStream()` bị khai báo lần hai ở cuối file.
Khai báo trùng đã ghi đè luồng chương trình đã ghép camera + overlay bằng canvas overlay trong suốt;
đã bỏ khai báo trùng. Cơ chế `Authorization: Bearer username:password` được giữ nguyên vì MediaMTX
hỗ trợ dạng này cho WebRTC/WHIP.

**Chưa phải bản phát hành:** chưa có thư mục `android/` và `ios/` do chúng được sinh bằng Capacitor CLI trên máy build; chưa có APK/IPA ký. App chỉ chọn một đích phát ở mỗi phiên; không có chat mạng xã hội hoặc phát đồng thời nhiều đích.

## Cài dependencies và sinh native projects

Yêu cầu Node.js 20+, npm, Android Studio + Android SDK (Android), và macOS + Xcode (iOS).

```bash
npm install
npm run android:add
# Trên macOS:
npm run ios:add
npm run cap:sync
```

Mở IDE:

```bash
npm run android:open
npm run ios:open
```

## Android APK

1. Trong `android/`, kiểm tra `minSdkVersion` (khuyến nghị 24+) và compile SDK theo Capacitor 7.
2. Kiểm tra AndroidManifest có `CAMERA`, `RECORD_AUDIO`, `INTERNET`, `ACCESS_NETWORK_STATE`.
3. Không khai báo quyền vị trí/danh bạ không cần thiết.
4. Android Studio → Build → Generate Signed Bundle / APK → APK; tạo keystore riêng và sao lưu an toàn.
5. Phát hành Google Play nên tạo AAB; APK phù hợp cài thử nội bộ.

## iOS IPA

1. Chạy trên macOS có Xcode; `npm run ios:add`, `npm run cap:sync`.
2. Thêm `NSCameraUsageDescription` và `NSMicrophoneUsageDescription` vào Info.plist.
3. Chọn Team, Bundle ID `vn.livestudio.mobile`, signing & capabilities.
4. Product → Archive → Distribute App → App Store Connect hoặc Ad Hoc.
5. IPA Ad Hoc/Enterprise cần profile và thiết bị/quyền phù hợp; TestFlight dùng App Store Connect.

## Ghi hình

Nút REC ghi chương trình đã ghép overlay + âm thanh bằng MediaRecorder. Bản ghi tạm nằm trong RAM cho
đến khi dừng, sau đó mở Share Sheet/tải file. Không dùng cho phiên dài nhiều giờ: cần triển khai native
chunked writer qua Filesystem/Media plugin để ghi từng phần vào bộ nhớ thiết bị.

## Tính năng và giới hạn hiện tại

- Overlay: banner, ticker, logo và bảng điểm; không có thành phần chat. Output chương trình dùng compositor canvas.
- Phát đích: người dùng chọn YouTube, Facebook hoặc RTMP tùy chỉnh; app gửi WHIP đến đúng path tương ứng. Chỉ một path/đích được chọn cho mỗi phiên; phải kết thúc phiên trước khi đổi.
- Stream Key: không lưu trong app; khóa của từng đích nằm trong cấu hình riêng trên VPS. Bảo vệ file cấu hình và giới hạn quyền truy cập VPS.
- iOS background streaming, khóa màn hình, chia sẻ màn hình và MediaRecorder phụ thuộc phiên bản iOS;
  cần test thiết bị thật. Không cam kết phát nền liên tục với WebView.

## Kiểm thử bắt buộc

- iPhone thật (ít nhất một bản iOS mục tiêu) và Android thật; camera trước/sau, mic, tai nghe Bluetooth.
- WHIP auth, ICE qua Wi-Fi/4G/5G, chuyển mạng, mạng yếu, mất kết nối và DELETE session.
- Kiểm tra bitrate/codec H.264, đồng bộ AV, nhiệt độ và pin sau 30–60 phút.
- Xác minh từng đích YouTube/Facebook/RTMP nhận luồng thật sau khi chọn riêng từng đích; xác nhận không có output thứ hai chạy ngoài ý muốn.
- Kiểm tra Stream Key không xuất hiện trong logs, crash reports, analytics, app bundle hoặc backup.

## Kiểm tra nhanh mã nguồn trong gói

- JavaScript inline: đã chạy `node --check` cho các khối script.
- Cấu trúc ZIP: đã kiểm tra sau khi đóng gói.
- `npm install`: chưa hoàn tất trong môi trường tạo gói do giới hạn thời gian/kết nối registry; chưa có package-lock.
- Chưa build Gradle/Xcode, chưa ký, chưa chạy trên thiết bị thật và chưa kiểm thử với VPS.
