# Xuất APK và IPA bằng Codemagic

## Workflow đã cấu hình
- `live-studio-android-debug`: tạo APK debug tại `android/app/build/outputs/apk/debug/app-debug.apk`.
- `live-studio-ios-unsigned`: tạo IPA unsigned tại `build/ios/Sucstudiolive-iOS-unsigned.ipa`.

## Cách tải file
1. Đưa toàn bộ mã nguồn trong repo lên GitHub.
2. Mở Codemagic và chọn đúng repository.
3. Chọn workflow Android hoặc iOS trong `codemagic.yaml`, rồi bấm Start new build.
4. Chờ build hoàn tất; mở trang build và tải file trong mục Artifacts.
5. Android: tải `app-debug.apk` để cài thử trên thiết bị Android.
6. iPhone: tải `Sucstudiolive-iOS-unsigned.ipa` để lưu trữ/kiểm tra cấu trúc.

## Lưu ý bắt buộc về iPhone
IPA unsigned không cài trực tiếp được trên iPhone thông thường. Muốn cài qua Xcode, TestFlight hoặc phân phối Ad Hoc cần Apple Developer signing certificate và provisioning profile hợp lệ, đồng thời cấu hình Code signing trong Codemagic. Workflow hiện tại cố ý xuất IPA unsigned, không giả định có chứng chỉ ký của bạn.

## Kiểm tra trước build
Codemagic chạy `npm run web:sync`, `npm run verify`, sau đó mới tạo project native và build. Nếu build thất bại, mở log và gửi phần bắt đầu từ `* What went wrong:` cùng các dòng `Caused by:`.
