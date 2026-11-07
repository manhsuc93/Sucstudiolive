Add these keys to `ios/App/App/Info.plist` after `npx cap add ios`:

- `NSCameraUsageDescription`: Live Studio cần camera để phát hình ảnh trực tiếp.
- `NSMicrophoneUsageDescription`: Live Studio cần micro để phát âm thanh trực tiếp.

Do not add photo-library/location/contact permissions unless a concrete feature requires them.
