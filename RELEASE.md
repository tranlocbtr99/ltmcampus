# LTM Campus 0.3.1 — sửa mở Google

versionCode 4, ký cùng khóa của 0.2.0 và 0.3.0; không gỡ ứng dụng khi cập nhật.

## Đã sửa

NCTU dùng intent:// để mở Chrome, còn Google OAuth chuyển hướng bằng JavaScript. Bản cũ chặn cả hai. Bản mới nhận diện chính xác các liên kết này và mở toàn bộ luồng đăng nhập từ trang NCTU trong Chrome/trình duyệt mặc định. Không mở nguyên intent không tin cậy, không truyền component, package hoặc fallback URL do trang cung cấp vào Intent Android. Không giả mạo user-agent.

Cho phép HTTPS trong khung con của trang, trong khi cầu nối dữ liệu vẫn giới hạn origin NCTU và khung chính.

## Chưa hoàn tất

Đăng nhập Chrome không chuyển cookie sang WebView. Trong các module NCTU đã kiểm tra chưa xác định được luồng bàn giao phiên cho ứng dụng bên thứ ba. Chưa thể xác nhận tự đồng bộ sau đăng nhập Google; cần cơ chế bàn giao/ủy quyền được NCTU hỗ trợ. App hiển thị browser_required, không tự bật hoặc báo đồng bộ thành công khi quay lại từ Chrome. Lịch đã lưu, ghi chú và lịch thi được giữ.

## Kiểm chứng

- Clean build: testDebugUnitTest, lintDebug, assembleRelease đạt.
- 29 kiểm thử JVM đạt; Lint không lỗi/cảnh báo trong lần build này.
- Xác minh chữ ký v2/v3, chứng chỉ SHA-256: 46e52032dd956e026e1cc93f8da4662a2f162f10dab22f34a44ebdd5a84a065e.
- APK SHA-256: a90c1e133b98ad554f439e7765c0ae1201d589276321253b70001e84c3e15270.
- Chưa kiểm tra giao diện Chrome/Google trên điện thoại thật hoặc tài khoản thật. Kiểm thử điều hướng xác minh cách phân loại và URL mở trình duyệt; không thay thế kiểm thử tài khoản trực tiếp.
