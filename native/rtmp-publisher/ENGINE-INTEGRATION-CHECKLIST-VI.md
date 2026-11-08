# Checklist nối engine RTMP thật

## iOS
- [ ] Chọn HaishinKit.swift 2.1.x nếu môi trường build là Xcode 16.4 / Swift 6; xác nhận version thực tế trước khi khóa package.
- [ ] Thêm Swift Package `https://github.com/HaishinKit/HaishinKit.swift`.
- [ ] Tạo `MediaMixer`, `RTMPConnection`, `RTMPStream`.
- [ ] `attachAudio` từ microphone và `attachVideo` từ camera theo `cameraFacing`.
- [ ] `mixer.addOutput(stream)`, `connection.connect(url)`, `stream.publish(key)`.
- [ ] Chỉ resolve `start` sau khi server xác nhận publish thành công.
- [ ] Thêm native compositor cho logo/banner/ticker/scoreboard.
- [ ] Dọn camera, audio session, stream và connection trong `stop`.

## Android
- [ ] Thêm JitPack và hai dependency `haishinkit` + `rtmp` theo version upstream đã xác minh.
- [ ] Tạo camera/audio capture, encoder H.264/AAC, RTMP connection và stream publish.
- [ ] Chỉ resolve `start` sau khi publish thành công.
- [ ] Thêm native compositor và lifecycle handling.
- [ ] Dừng/giải phóng tất cả tài nguyên khi stop hoặc mất quyền.

## Kiểm thử
- [ ] Kiểm thử thiết bị thật Android và iPhone.
- [ ] RTMP/RTMPS, stream key sai, server từ chối, mất mạng, chuyển Wi-Fi/4G.
- [ ] 720p/1080p, tiếng-hình đồng bộ, nhiệt độ và thời lượng 30+ phút.
- [ ] Xác minh overlay trong luồng nhận thực tế.
