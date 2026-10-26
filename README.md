# LTM Campus Android

Ứng dụng học tập cá nhân cho sinh viên, Android 8.0 trở lên. Designed by LocTM.

## Phiên bản 0.2.0

- Lịch học ngoại tuyến: nhập JSON NCTU, lọc ngày/tuần, tìm môn và mã lớp, hiển thị tạm ngưng.
- Lịch lặp có thứ/ngày bắt đầu/kết thúc được mở rộng thành từng buổi. Khóa dữ liệu gồm ID và ngày để không mất các buổi cùng lớp.
- Ghi chú theo môn: thêm, sửa, xóa có xác nhận.
- Thi và kiểm tra: ngày giờ Việt Nam, đếm ngược, chuyển sự kiện sang lịch hệ thống để thiết lập nhắc nhở.
- Đăng nhập NCTU chính chủ trong WebView; có mở trình duyệt ngoài và xóa phiên. Không đọc mật khẩu, không lấy token và không có máy chủ riêng.
- Xuất/khôi phục sao lưu JSON; nhập lỗi được rollback, không xóa dữ liệu hiện có. Khôi phục yêu cầu xác nhận thay thế dữ liệu.

## Kết nối NCTU

Đăng nhập và dữ liệu lịch ngoại tuyến hiện là hai luồng riêng. Tự đồng bộ API chưa triển khai; không suy đoán token hoặc gắn tài khoản cá nhân vào APK. Người dùng nhập tệp phản hồi `success: true, data.sessions`, như `sample/nctu-redacted-example.json`.

Nhập lịch là thao tác gộp: các buổi cùng ID/ngày được cập nhật; những buổi không xuất hiện trong tệp vẫn được giữ. Lịch lặp chỉ hỗ trợ khoảng ngày và thứ; không suy diễn chuỗi tuần học không được mô tả rõ. Tệp sai bị từ chối toàn bộ. Tối đa 8 MB/tệp, 10.000 bản ghi nguồn.

WebView chỉ cho điều hướng HTTPS trong miền `nctu.edu.vn`; liên kết HTTPS ngoài miền do người dùng nhấn được mở bằng trình duyệt. Lỗi chứng chỉ bị từ chối. Không có cầu nối JavaScript vào dữ liệu ứng dụng. SSO bên ngoài có thể cần trình duyệt hệ thống. Cần kiểm tra đăng nhập trên thiết bị với tài khoản của chính người dùng.

## Build và kiểm thử

JDK 17, Android SDK 35, Gradle 8.9. Chạy:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

GitHub Actions chạy cùng các bước và lưu APK cùng báo cáo. APK release từ CI chưa ký; không coi debug APK là bản phát hành. Khóa ký release phải lưu riêng ngoài GitHub, dùng cùng khóa cho mọi lần cập nhật. Không gỡ ứng dụng nếu muốn giữ dữ liệu.

## Quyền riêng tư

Cơ sở dữ liệu SQLite nằm trong bộ nhớ riêng của ứng dụng. Tắt sao lưu tự động hệ thống để tránh đưa phiên WebView vào cloud backup. Xuất JSON thủ công chỉ gồm lịch, ghi chú và kỳ thi. Không có quảng cáo, analytics, máy chủ trung gian hoặc khóa API nhúng sẵn. Người dùng tự bảo vệ tệp sao lưu.

Không commit lịch thật, mã sinh viên, email cá nhân, bundle JavaScript của bên thứ ba, token hoặc khóa ký vào repository.

## Phạm vi còn lại

Chưa có AI biên soạn/tóm tắt, tin tức, widget hoặc đồng bộ đa thiết bị. Không tuyên bố hỗ trợ các tính năng đó trong phiên bản này. Ứng dụng không liên kết chính thức với NCTU.
