# Live Studio V16.1 — bộ mã nền iOS

## Mục đích
Bộ này thay thế các tệp cấu hình và script dựng ứng dụng. Tệp giao diện `Studio_Livestream.html` được giữ nguyên, không viết lại hoặc đổi thiết kế.

## Những tệp cần thay trên GitHub
- `package.json`
- `capacitor.config.ts`
- `codemagic.yaml`
- `.gitignore`
- `scripts/sync-web.mjs`

Không xóa hoặc đổi tên:
- `Studio_Livestream.html`
- `manifest.json`
- `sw.js`
- `icons/` (nếu repo đang có)
- `screenshots/` (nếu repo đang có)
- `server/` (nếu repo đang có)

## Tạo nhánh trên GitHub bằng iPhone
1. Mở repo `manhsuc93/Sucstudiolive`.
2. Chạm nút chọn nhánh đang hiện `main`.
3. Nhập `rebuild/ios-foundation`.
4. Chọn tạo nhánh mới từ `main`.
5. Mở từng tệp ở danh sách trên, chọn Edit (biểu tượng bút), thay toàn bộ nội dung bằng tệp tương ứng trong bộ mã này, rồi Commit changes vào nhánh `rebuild/ios-foundation`.

Nếu muốn tách việc dựng iOS khỏi cấu hình nền, sau khi nhánh nền ổn định có thể tạo nhánh thứ hai tên `rebuild/ios-build` từ nhánh `rebuild/ios-foundation`. Chưa cần tạo nhánh thứ hai ngay.

## Quan trọng về IPA
Workflow trong bộ này tạo **IPA chưa ký (unsigned)**. File này dùng để kiểm tra build, không thể cài trực tiếp lên iPhone thông thường. Muốn cài trên iPhone XS Max iOS 18.7.2 cần ký ứng dụng bằng chứng chỉ và provisioning profile hợp lệ hoặc dùng phương thức sideload tương thích. Không có cách biến IPA unsigned thành IPA cài được chỉ bằng đổi tên file.

## Giới hạn chức năng
Bộ này làm sạch phần cấu hình đóng gói iOS và đồng bộ giao diện web. Nó không tự tạo máy chủ livestream. Chức năng phát WHIP/RTMP còn phụ thuộc máy chủ HTTPS/WHIP và cấu hình tài khoản/đích phát. Không nên coi build thành công là bằng chứng camera, ghi hình và livestream đã được kiểm thử thực tế trên iPhone.
