LIVE STUDIO V16 — CAPACITOR ANDROID / IOS
=========================================

ỨNG DỤNG
- Giao diện Live Studio được đóng gói bằng Capacitor cho Android và iOS.
- Bật camera/micro, ghép hình với overlay (banner, logo, chữ chạy, bảng điểm), ghi REC cục bộ.
- Gửi một luồng WebRTC/WHIP từ điện thoại lên VPS.
- Người dùng chọn thủ công MỘT đích phát: YouTube Live, Facebook Live hoặc RTMP tùy chỉnh.
- Không có chat mạng xã hội, không đọc bình luận và không phát đồng thời nhiều đích.

CHỌN VÀ ĐỔI NỀN TẢNG
1. Vào tab Đích, chọn một nền tảng.
2. Nhập địa chỉ gốc HTTPS của VPS, ví dụ https://stream.tenmiencuaban.vn (không thêm đường dẫn).
3. Nhập tài khoản studio và mật khẩu VPS. Khóa RTMP của từng nền tảng chỉ lưu trong mediamtx.yml trên VPS.
4. Bật camera và micro rồi nhấn BẮT ĐẦU PHÁT.
5. Muốn đổi nền tảng: nhấn KẾT THÚC PHÁT, chọn nền tảng mới, rồi bắt đầu lại. Không thể đổi đích trong khi đang LIVE.

MÁY CHỦ
- Cài MediaMTX + FFmpeg + Caddy theo server/README-VI.txt.
- VPS có ba path độc lập: youtube, facebook, custom; mỗi path chỉ có một output RTMP.
- Mỗi phiên WHIP chỉ kết nối vào một path theo lựa chọn trong app.
- Không đưa stream key hoặc mật khẩu thật vào ứng dụng, GitHub hay kho công khai.

GIỚI HẠN VÀ KIỂM THỬ
- Cần VPS, domain, khóa phát và kiểm thử thực tế trên Android/iPhone.
- Nút kiểm tra cấu hình chỉ kiểm tra định dạng, không xác nhận khóa RTMP.
- Ghi hình sử dụng MediaRecorder; thời lượng dài và lưu nền cần kiểm thử/triển khai native riêng.
- Chia sẻ màn hình và phát nền bị giới hạn theo hệ điều hành.
- Xem README-CAPACITOR-VI.md để tạo dự án native và ký APK/IPA.
