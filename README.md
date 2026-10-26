# LTM Campus 🎓

Ứng dụng Android độc lập giúp sinh viên quản lý thời khóa biểu, ghi chú bài học, lịch thi và lịch kiểm tra.

## Phạm vi MVP v0.1.0
- Lịch học: nhập phản hồi JSON NCTU, lưu và xem ngoại tuyến.
- Ghi chú bài học theo môn.
- Lịch thi/kiểm tra và đếm ngược.
- Mở trang đăng nhập NCTU chính thức trong trình duyệt; ứng dụng **không thu thập mật khẩu hay cookie**.

## Đồng bộ NCTU
Định dạng JSON đã được khảo sát. Chưa kích hoạt gọi API tự động cho đến khi xác minh luồng cấp quyền phù hợp với chính sách NCTU. Nhập JSON là phương án dự phòng.

## Công nghệ
Kotlin, Jetpack Compose, SQLite; Android SDK và Gradle. Thiết kế hướng offline-first.

## Bảo mật
Không commit mã sinh viên, lịch cá nhân, khóa API, token, cookie, mật khẩu hay keystore ký APK lên repository công khai.

## Tình trạng
Khởi tạo repository. Mã nguồn MVP ban đầu đang được chuẩn bị để tích hợp và kiểm thử; chưa có APK phát hành.

Designed by LocTM.
