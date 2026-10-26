# LTM Campus Android

Ứng dụng Android độc lập tự cập nhật lịch NCTU, lưu lịch ngoại tuyến, ghi chú theo môn và quản lý lịch thi. Designed by LocTM.

## Sử dụng bản 0.3.0

1. Cài APK đã ký; có thể cài đè bản 0.2.0 cùng khóa mà không gỡ ứng dụng.
2. Chọn **Cập nhật** hoặc **Kết nối → Đăng nhập & bật tự cập nhật**.
3. Đăng nhập trên trang NCTU chính chủ **trong ứng dụng**, đợi thông báo cập nhật thành công rồi đóng cổng NCTU.
4. Lịch tự cập nhật khi mở ứng dụng (giới hạn một lần trong 5 phút), khi chọn Cập nhật, và qua tác vụ nền chu kỳ 30 phút khi có mạng. Android có thể trì hoãn tác vụ nền khi tiết kiệm pin hoặc ứng dụng bị buộc dừng.

Không cần người dùng xuất hoặc nhập JSON cho luồng chính. Nhập JSON chỉ là công cụ dự phòng. Khi phiên đăng nhập không thể làm mới qua bộ kết nối NCTU, ứng dụng hiển thị yêu cầu đăng nhập lại và giữ lịch cũ.

## Đồng bộ

- Dùng chính module JavaScript đã tải bởi trang `https://sv.nctu.edu.vn` để tìm đối tượng có `getAllStudentSchedules` và `getPeriodTimes`, không cố định tên hash hoặc alias minify.
- Gọi bộ kết nối chính chủ với khoảng ngày từ đầu tháng trước đến hết 6 tháng tiếp theo. Việc xác thực/làm mới phiên do client NCTU xử lý.
- Android chỉ nhận các trường lịch được cho phép qua WebMessageListener giới hạn chính xác origin HTTPS của cổng sinh viên, khung chính và nonce từng yêu cầu.
- Không đọc ô mật khẩu, không trích xuất token sang mã Android, không có máy chủ trung gian. Phiên web nằm trong WebView trên thiết bị.
- Snapshot hợp lệ cập nhật đổi phòng/giờ/trạng thái và gỡ các buổi đã đồng bộ không còn có trong khoảng nhận lịch. Lịch thủ công ngoài phạm vi, ghi chú và lịch thi riêng được giữ.
- Dữ liệu lỗi, bị giới hạn, thiếu danh tính hoặc yêu cầu hết phiên không được dùng để xóa lịch. Snapshot cũ không ghi đè kết quả mới hơn. Đổi tài khoản gỡ cache NCTU của tài khoản trước; không xóa ghi chú hay kỳ thi riêng.
- Cơ sở dữ liệu v1 → v2 chỉ thêm bảng theo dõi đồng bộ. Sao lưu thủ công giữ cả nguồn lịch để khôi phục vẫn xử lý buổi bị gỡ đúng; không sao lưu phiên đăng nhập.

NCTU chưa cung cấp SDK ổn định cho ứng dụng này: nếu trang đổi giao diện xuất module hoặc cấu trúc API, bộ kết nối báo lỗi và giữ cache, cần cập nhật adapter. Luồng tài khoản thật/SSO trên điện thoại cần xác nhận qua lần đăng nhập đầu tiên; các kiểm thử tự động dùng client mô phỏng và dữ liệu lịch đã cung cấp.

## Chức năng khác

Lịch theo ngày/tuần, tìm môn/mã lớp; thêm/sửa/xóa ghi chú; tạo kỳ thi, đếm ngược và chuyển sự kiện sang lịch điện thoại; xuất/khôi phục sao lưu JSON có xác nhận, rollback khi tệp lỗi.

## Build và kiểm thử

JDK 17, Android SDK 35; Gradle Wrapper 8.9:

```sh
node tools/test-nctu-sync.mjs
./gradlew clean testDebugUnitTest lintDebug assembleRelease
```

GitHub Actions chạy kiểm thử và lưu APK chưa ký. Biến `CAMPUS_KEYSTORE`, `CAMPUS_STORE_PASSWORD`, `CAMPUS_KEY_PASSWORD` cho phép ký release bằng khóa giữ riêng. Không commit keystore, mật khẩu, lịch thật hoặc token vào repository.

Android tối thiểu API 26. WebView phải hỗ trợ WEB_MESSAGE_LISTENER; nếu thiếu, ứng dụng yêu cầu cập nhật Android System WebView. Phiên bản, kết quả kiểm chứng và SHA-256 APK nằm trong RELEASE.md.
