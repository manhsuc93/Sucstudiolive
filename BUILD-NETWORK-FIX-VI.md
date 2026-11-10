# Bản sửa lỗi tải Google Maven cho Codemagic

- Dựa trên mã nguồn gốc `applivesuc-main.zip`.
- Giữ nguyên HaishinKit 0.14.2 và toàn bộ mã Kotlin/Swift.
- Thêm HTTPS mirror `https://maven.aliyun.com/repository/google` trước `google()`
  trong `android/settings.gradle` và `android/build.gradle` được tạo trong CI.
- Giữ Google Maven chính thức làm fallback.
- Gradle chạy với `--refresh-dependencies`.

Log gần nhất cho thấy không tải được Android Gradle Plugin 8.7.2 và Google
Services 4.4.2 từ `dl.google.com` do lỗi socket/SSL. Đây là workaround cho
đường tải dependency, không phải xác nhận toàn bộ ứng dụng đã build thành công.

Mirror là dịch vụ bên thứ ba. Nếu sau đó log chuyển sang lỗi không tìm thấy
HaishinKit 0.14.2 trên JitPack, cần xử lý riêng theo log đó. Bản này cố ý
không nâng phiên bản HaishinKit khi API chưa được xác minh.

Workflow iOS và mã native iOS không thay đổi. Chưa có lần chạy Codemagic thực tế
để xác nhận build thành công.
