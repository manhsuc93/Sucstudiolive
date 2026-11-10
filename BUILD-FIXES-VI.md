# Các thay đổi sửa lỗi build Android

1. Nâng HaishinKit Android từ 0.14.2 lên 0.15.0 vì JitPack không phân giải được artifact `rtmp:0.14.2` trong log Codemagic.
2. Đổi lớp ảnh overlay sang `ImageScreenObject` theo thay đổi tên ScreenObject của dòng 0.15.
3. Thêm import tường minh `EventUtils`.
4. Dùng constructor `RtmpStream(connection)` theo ví dụ API của HaishinKit.
5. Thêm `--refresh-dependencies` cho bước Gradle trong Codemagic.

Lưu ý:
- Đây là sửa lỗi dựa trên log build và thay đổi API đã công bố; chưa có môi trường Android SDK/Gradle đầy đủ ở đây để chạy build xác nhận.
- Chạy lại workflow Android trên Codemagic. Nếu còn lỗi, gửi phần log bắt đầu từ `* What went wrong:` và dòng `Caused by:`.
- Workflow iOS và mã Swift được giữ nguyên.
