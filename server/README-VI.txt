LIVE STUDIO - PHÁT THỦ CÔNG MỘT NỀN TẢNG MỖI LẦN
===================================================

CÁCH HOẠT ĐỘNG
- Điện thoại gửi MỘT luồng WebRTC/WHIP lên VPS.
- Người dùng chọn thủ công đúng MỘT đích: YouTube, Facebook hoặc RTMP tùy chỉnh.
- WHIP path được chọn tương ứng: /youtube/whip, /facebook/whip hoặc /custom/whip.
- MediaMTX chỉ chạy một tiến trình FFmpeg ở path đang nhận luồng; FFmpeg chỉ có một output RTMP.
- Không có chat nền tảng và không phát đồng thời nhiều mạng xã hội.
- Muốn đổi nền tảng: nhấn KẾT THÚC PHÁT, chọn đích khác, rồi BẮT ĐẦU PHÁT lại.

YÊU CẦU
- VPS Ubuntu 22.04/24.04, Docker Engine + Docker Compose Plugin.
- Tên miền trỏ về VPS, HTTPS hoạt động.
- Mở TCP 80, TCP 443, UDP 8189 (có thể mở TCP 8189 làm dự phòng).
- Không mở công khai RTSP 8554 hoặc MediaMTX 8889.

CẤU HÌNH
1. Sửa Caddyfile: thay stream.YOUR-DOMAIN.vn bằng tên miền VPS.
2. Sửa mediamtx.yml:
   - YOUR-APP-DOMAIN.example: domain đang host ứng dụng Live Studio (nếu dùng app domain khác).
   - Origin https://localhost (Android Capacitor), capacitor://localhost (iOS Capacitor) và http://localhost (dev) đã được cho phép trong webrtcAllowOrigins.
   - stream.YOUR-DOMAIN.vn: hostname VPS ở webrtcAdditionalHosts.
   - CHANGE_ME_STUDIO_PASSWORD: mật khẩu dài, ngẫu nhiên, nhập trong app.
   - CHANGE_ME_RELAY_PASSWORD: mật khẩu nội bộ dài, chỉ dùng cho FFmpeg.
   - CHANGE_ME_YOUTUBE_STREAM_KEY: khóa YouTube Live.
   - CHANGE_ME_FACEBOOK_STREAM_KEY: khóa Facebook Live.
   - CHANGE_ME_CUSTOM_RTMP_URL_WITH_KEY: URL đầy đủ gồm server + stream key do nền tảng RTMP khác cấp, ví dụ rtmp(s)://host/app/KEY.
3. Không đưa khóa RTMP, mật khẩu VPS hoặc file mediamtx.yml đã điền khóa lên GitHub/public hosting.
4. Chạy trong thư mục server: docker compose up -d
5. Kiểm tra log: docker compose logs -f mediamtx

CẤU HÌNH TRONG ỨNG DỤNG
- Nền tảng đích: chọn YouTube Live / Facebook Live / RTMP tùy chỉnh.
- Địa chỉ gốc máy chủ: https://stream.tenmiencuaban.vn (chỉ nhập domain, không thêm /live/whip).
- Username: studio
- Password: CHANGE_ME_STUDIO_PASSWORD đã đặt trên VPS.
- Chọn chất lượng 1080p hoặc 720p; bật camera và micro.
- Nhấn BẮT ĐẦU PHÁT.

ĐỔI ĐÍCH PHÁT
- Bắt buộc kết thúc phiên hiện tại trước khi đổi nền tảng.
- Chọn nền tảng khác trong tab Đích rồi bắt đầu lại.
- Mỗi thời điểm chỉ có một path nhận WHIP và một output RTMP. Nếu cần đổi khóa RTMP, sửa trên VPS và khởi động lại MediaMTX.

LƯU Ý
- Nút kiểm tra cấu hình chỉ kiểm tra định dạng đầu vào; không xác nhận khóa RTMP hợp lệ.
- Nền tảng có thể yêu cầu người dùng bấm Go Live/Phát trực tiếp trong trang quản trị của họ.
- Cần kiểm tra log FFmpeg/MediaMTX và trạng thái preview trên nền tảng trong lần chạy thực tế đầu tiên.
- Video được copy H.264 nếu trình duyệt gửi H.264; âm thanh được chuyển sang AAC.
- Không bao giờ chụp/gửi ảnh có lộ khóa phát hoặc mật khẩu.
