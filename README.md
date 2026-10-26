# LTM Campus Android

Ứng dụng Android độc lập tự cập nhật lịch NCTU, lưu lịch ngoại tuyến, ghi chú theo môn và quản lý lịch thi. Designed by LocTM.

## Bản 0.3.1: sửa mở đăng nhập Google

Bản 0.3.0 đã chặn liên kết Chrome `intent://` mà NCTU dùng, rồi tiếp tục chặn chuyển hướng Google bằng JavaScript vì không có user gesture. Bản 0.3.1 nhận diện hai trường hợp này và khởi chạy toàn bộ luồng `https://sv.nctu.edu.vn/login?auto_google=1` bằng Chrome hoặc trình duyệt mặc định, giữ OAuth state và cookie trong cùng trình duyệt. Không giả mạo user-agent và không đưa Google OAuth vào WebView.

**Giới hạn quan trọng:** đăng nhập thành công trong Chrome không đồng nghĩa đã đăng nhập bộ đồng bộ của ứng dụng. Cookie Chrome và WebView tách biệt. Trong các module NCTU được kiểm tra chưa xác định được luồng bàn giao phiên cho ứng dụng Android bên thứ ba. Bản này hiển thị rõ trạng thái `browser_required`, không tự bật đồng bộ hoặc báo thành công sau khi người dùng quay lại từ Chrome. Cần cơ chế ủy quyền/bàn giao phiên do NCTU hỗ trợ để hoàn tất tự đồng bộ cho tài khoản đăng nhập Google.

Có thể cài đè APK 0.3.1 lên 0.2.0/0.3.0 cùng khóa, không gỡ ứng dụng. Chọn **Kết nối → Đăng nhập** rồi **Google qua Chrome**, hoặc dùng nút Google trên trang NCTU. Lịch đã lưu, ghi chú, lịch thi và khóa ký được giữ nguyên.

Bộ đồng bộ 0.3.0 bên dưới chỉ hoạt động khi đã có phiên NCTU hợp lệ trong WebView. Không coi kiểm thử client mô phỏng là bằng chứng đăng nhập Google thực tế hoạt động.

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
