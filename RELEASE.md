# LTM Campus 0.2.0 — 26/10/2026

Bản MVP Android độc lập, không phụ thuộc LTMstu hoặc LTMsav.

- applicationId: `vn.loctm.ltmcampus`
- versionCode: `2`; versionName: `0.2.0`
- Android tối thiểu: 8.0 / API 26; target SDK: 35
- APK release khoảng 6.24 MiB, không bật debug.
- SHA-256 APK: `9c8c008538c18be37062df408be6d69dcefb5395b9132669f403b2068c4c8b83`
- SHA-256 chứng chỉ ký: `46e52032dd956e026e1cc93f8da4662a2f162f10dab22f34a44ebdd5a84a065e`

## Chức năng

Lịch học ngoại tuyến theo ngày/tuần, tìm môn/mã lớp, nhập JSON NCTU; ghi chú theo môn có sửa/xóa; tạo lịch thi và đếm ngược theo giờ Việt Nam, chuyển sang lịch hệ thống; đăng nhập NCTU chính chủ trong app với trình duyệt ngoài dự phòng; xuất/khôi phục sao lưu JSON.

## Kiểm chứng

- Clean build: testDebugUnitTest, lintDebug, assembleDebug, assembleRelease đạt.
- 12 kiểm thử đơn vị đạt, gồm dữ liệu thật 70 buổi được cung cấp riêng để kiểm thử cục bộ. Dữ liệu cá nhân không được commit.
- Android Lint: 0 lỗi, 0 cảnh báo trên mã cuối.
- APK được kiểm chứng chữ ký v2/v3, zip alignment 16 KB.
- Khóa ký được giữ riêng, không lưu trong repository hoặc workflow.

## Giới hạn xác minh

Chưa chạy trên điện thoại thật hoặc kiểm tra đăng nhập SSO với tài khoản NCTU. Đăng nhập không tự đồng bộ lịch: phiên bản này nhập JSON. Không tuyên bố có AI, widget hoặc đồng bộ đa thiết bị.

Nhập JSON gộp lịch, không xóa các buổi ngoài tệp. Khôi phục sao lưu thay thế dữ liệu sau xác nhận và rollback nếu tệp lỗi. Bản sao lưu thủ công không chứa phiên đăng nhập NCTU.
