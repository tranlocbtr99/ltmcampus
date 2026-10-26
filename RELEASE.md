# LTM Campus 0.3.0 — 26/10/2026

Tự đồng bộ lịch qua phiên NCTU chính chủ trong ứng dụng, thay cho luồng nhập JSON của 0.2.0.

- applicationId: `vn.loctm.ltmcampus`; versionCode `3`; versionName `0.3.0`.
- Sau đăng nhập tự lấy lịch; kiểm tra khi mở app và theo tác vụ nền chu kỳ 30 phút, có thể bị Android trì hoãn.
- Hỗ trợ đổi giờ/phòng/trạng thái và gỡ buổi không còn trong snapshot hợp lệ. Có trạng thái cập nhật gần nhất, cập nhật ngay và tắt đồng bộ.
- Không thay khóa ký; migration dữ liệu v1 → v2 thêm bảng, giữ ghi chú và lịch thi.
- SHA-256 APK: `cfe5618c47cd4e324c808df0a8acda6aa880944f5351020f5e33fba00793d9b8`.
- SHA-256 chứng chỉ ký: `46e52032dd956e026e1cc93f8da4662a2f162f10dab22f34a44ebdd5a84a065e` (cùng bản 0.2.0).

## Kiểm chứng

- Clean build testDebugUnitTest, lintDebug, assembleRelease thành công.
- 24 kiểm thử JVM đạt, gồm SQLite thực thi bằng Robolectric: migration, cập nhật/gỡ buổi, snapshot lỗi rollback, phản hồi cũ, đổi tài khoản và sao lưu/khôi phục nguồn lịch.
- 11 kiểm tra adapter JavaScript đạt, gồm xác thực hết hạn, mất mạng, schema lỗi, domain không hợp lệ, lọc dữ liệu nhạy cảm và dữ liệu 70 buổi được cung cấp riêng.
- Android Lint: 0 lỗi; 1 cảnh báo có phiên bản activity-compose mới hơn. Giữ phiên bản hiện có để tránh nâng thư viện ngoài phạm vi thay đổi.
- APK release khoảng 6,62 MiB, xác minh chữ ký v2/v3 và zip alignment 16 KB.

Chưa xác minh bằng tài khoản NCTU thật trên điện thoại. Client chính chủ và định dạng API được xác định từ JavaScript/JSON đã cung cấp; kiểm thử adapter dùng client mô phỏng. Cần đăng nhập lần đầu trong ứng dụng để xác nhận kết nối thực tế. Không tuyên bố tự cập nhật tức thời hoặc tiếp tục cập nhật khi phiên NCTU không thể làm mới.
