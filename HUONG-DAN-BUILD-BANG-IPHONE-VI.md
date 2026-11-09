# Sucstudiolive — build Android và iPhone bằng điện thoại

Bạn không cần cài Android Studio hoặc Xcode trên iPhone. Việc biên dịch sẽ chạy trên dịch vụ cloud như Codemagic.

## Nội dung đã chuẩn bị
- Dự án Capacitor 7 dùng chung Android/iOS.
- Plugin native RTMP Kotlin và Swift dựa trên HaishinKit.
- Camera và micro native; H.264/AAC mặc định.
- H.265/HEVC khi thiết bị có encoder phần cứng và máy chủ hỗ trợ Enhanced RTMP.
- RTMP/RTMPS trực tiếp.
- Overlay logo, banner, ticker strip và bảng điểm được render thành bitmap native.
- Build workflows trong `codemagic.yaml`.

## Cách đưa mã nguồn lên GitHub bằng iPhone
1. Tải ZIP dự án và giải nén trong ứng dụng Tệp.
2. Mở GitHub bằng Safari, vào repository Sucstudiolive.
3. Dùng giao diện web GitHub để upload toàn bộ nội dung đã giải nén. Nếu upload qua trình duyệt gặp giới hạn, dùng GitHub Codespaces hoặc giao diện upload file theo từng thư mục.
4. Đảm bảo file `codemagic.yaml` nằm ở thư mục gốc repository.

## Build Android
1. Mở https://codemagic.io/ bằng Safari.
2. Đăng nhập và kết nối GitHub.
3. Chọn repository Sucstudiolive.
4. Chạy workflow `live-studio-android-debug`.
5. Khi build thành công, mở Artifacts và tải APK về thiết bị Android để cài thử. iPhone không cài được APK.

## Build iPhone
1. Chạy workflow `live-studio-ios-unsigned` để kiểm tra biên dịch iOS.
2. IPA unsigned không cài trực tiếp lên iPhone.
3. Để cài trên iPhone, cần cấu hình Apple Developer signing/provisioning trên Codemagic hoặc phân phối qua TestFlight/App Store Connect.

## Lệnh build khi có môi trường Node/Capacitor
```bash
npm install
npm run web:sync
npx cap sync android
npx cap sync ios
```
`npm run web:sync` đồng bộ HTML, biên dịch TypeScript plugin và tạo bundle JS bridge cho Capacitor. Codemagic workflow đã gọi bước này.

## Những điểm chưa thể xác nhận
- Mã native đã được viết nhưng chưa chạy Gradle/Xcode build trong môi trường chỉnh sửa này.
- Chưa test RTMP/RTMPS thực tế trên iPhone/Android hoặc các ingest YouTube/Facebook.
- Native preview chưa được ghép vào vùng preview WebView; khi live, camera preview web được dừng để nhường camera cho plugin native.
- Ticker hiện là dải chữ tĩnh, chưa cuộn liên tục.
- H.265 phụ thuộc encoder phần cứng và Enhanced RTMP của máy chủ nhận.
- Cần kiểm tra log Codemagic và thử thiết bị thật trước khi coi là bản phát hành.

## Bảo mật
Không đưa Stream Key, mật khẩu, chứng chỉ ký hoặc keystore vào GitHub. Dùng biến môi trường/secret của Codemagic.
