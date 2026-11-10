# Ninh Kieu Pharma — JavaFX Pharmacy Management

Ứng dụng JavaFX quản lý nhà thuốc, dùng JDBC trực tiếp với MySQL (XAMPP), không dùng ORM. Ứng dụng kết nối tới schema chuẩn hóa trong `01_schema_quan_ly_cua_hang.sql`; không tạo database/bảng và không seed thêm dữ liệu nghiệp vụ.

## Yêu cầu

- JDK 21 trở lên
- Maven 3.9 trở lên
- MySQL/MariaDB đang chạy (XAMPP thường dùng `127.0.0.1:3306`)

## Kết nối MySQL

Trước khi chạy app, hãy tự chạy file `01_schema_quan_ly_cua_hang.sql` trong MySQL/phpMyAdmin. Script tạo database `quan_ly_cua_hang`, các bảng chuẩn hóa và dữ liệu ví dụ ban đầu. Ứng dụng mặc định kết nối `127.0.0.1:3306/quan_ly_cua_hang`; nếu database hoặc bảng cần thiết chưa tồn tại, app báo lỗi và dừng thay vì tự tạo schema.

Mở PowerShell tại thư mục dự án. Thiết lập biến môi trường trong **cùng cửa sổ PowerShell** dùng để chạy ứng dụng. Nhập mật khẩu MySQL thực tế khi được hỏi:

```powershell
$dbPassword = Read-Host "MySQL password" -AsSecureString
$env:PHARMACY_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $dbPassword).Password
$env:PHARMACY_DB_HOST = "127.0.0.1"
$env:PHARMACY_DB_PORT = "3306"
$env:PHARMACY_DB_NAME = "quan_ly_cua_hang"
$env:PHARMACY_DB_USER = "root"

mvn javafx:run
```

Đảm bảo MySQL trong XAMPP đang chạy. Nếu không kết nối được, kiểm tra MySQL có đang nghe ở cổng `3306` hay không và trong phpMyAdmin đã có database `quan_ly_cua_hang` cùng các bảng từ script. Tài khoản MySQL chỉ cần quyền kết nối/đọc/ghi phù hợp với các bảng hiện có; app không yêu cầu quyền tạo database hoặc bảng. Ứng dụng không chuyển sang kho in-memory khi kết nối lỗi.

Nếu lỗi ghi `Access denied for user 'root' ... (using password: NO)`, ứng dụng chưa nhận được `PHARMACY_DB_PASSWORD`. Chạy lệnh PowerShell ở trên rồi chạy `mvn javafx:run` ngay trong cùng cửa sổ; nếu khởi chạy từ IDE, hãy cấu hình biến `PHARMACY_DB_PASSWORD` trong cấu hình chạy của IDE.

## Thiết lập tài khoản quản trị đầu tiên

Tên đăng nhập quản trị ban đầu là `admin_demo`. Trước lần chạy đầu tiên sau khi tạo database, đặt mật khẩu riêng qua biến môi trường trong cùng cửa sổ PowerShell:

```powershell
$initialAdminPassword = Read-Host "Choose initial admin password" -AsSecureString
$env:PHARMACY_SAMPLE_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $initialAdminPassword).Password
mvn javafx:run
```

Khi `users.password_hash` của `admin_demo` còn placeholder, ứng dụng dùng biến này để tạo BCrypt hash và thay đúng placeholder đó. Không lưu mật khẩu rõ trong source code. Sau khi tài khoản đã có hash hợp lệ, biến này không cần đặt lại; ứng dụng không đổi mật khẩu đã lưu. Nếu tài khoản mẫu thiếu, ứng dụng báo lỗi và không tự tạo tài khoản.

## Cấu trúc mã nguồn

```text
src/main/java/vn/ninhkieupharma/pharmacy/
├── auth/                       # User, vai trò, UserDAO/JDBC, xác thực, LoginController
├── database/                   # cấu hình và kết nối MySQL
├── features/
│   ├── common/                 # dịch vụ, model dùng chung, adapter/view coordinator
│   └── <feature>/              # model + DAO interface + DAOImpl + service theo chức năng
└── NinhKieuPharmaApp.java
```

`RelationalFeatureModuleDAO` ánh xạ các chức năng tới bảng `employees`, `users`, `roles`, `employee_availabilities`, `products`, `product_categories`, `suppliers`, `warehouse_locations`, `customers`, `promotions` và các bảng quan hệ/báo cáo tương ứng. Mục UC-01 cho phép quản trị viên tạo hồ sơ nhân viên cùng tài khoản STAFF trong một giao dịch; mật khẩu được lưu bằng BCrypt. Form không cho tạo tài khoản quản trị viên, và mục này hiện chỉ hỗ trợ tạo mới (chưa sửa/xóa tài khoản). Thao tác ghi nghiệp vụ khác hỗ trợ UC-02, UC-07, UC-08, UC-09, UC-13, UC-16 và UC-18. Liên kết nhà cung cấp của sản phẩm không được thay đổi qua form sản phẩm vì form không thu thập giá nhập bắt buộc. Báo cáo đọc trực tiếp dữ liệu MySQL. UC-03, UC-04, UC-12, UC-14 và UC-17 hiện chỉ đọc vì biểu mẫu chưa thu thập đủ thông tin liên quan để ghi an toàn vào các bảng chuẩn hóa (ca, chấm công, phiếu và chi tiết, lô/tồn kho, hóa đơn và mặt hàng). Chức năng chưa hỗ trợ sẽ không ghi một phần hoặc tạo bảng thay thế.

Ở màn hình đăng nhập và form tạo tài khoản nhân viên, nút `Hiện` / `Ẩn` bật tắt hiển thị mật khẩu đang nhập.

Tài khoản `STAFF` có dashboard và danh sách chức năng riêng theo quyền; không hiển thị số liệu doanh thu, nhân sự hoặc báo cáo quản trị trên dashboard. Các mục điều hướng không được cấp quyền sẽ bị ẩn và màn hình cũng kiểm tra quyền trước khi mở. Dùng nút đổi ngôn ngữ để chuyển nội dung dashboard và thông báo giữa tiếng Việt và tiếng Anh.

Ứng dụng giữ nguyên dữ liệu đã có, không tự xóa, thay thế hoặc seed thêm bản ghi nghiệp vụ. Có thể chạy `mvn clean test`; kiểm thử đọc schema MySQL tích hợp chỉ chạy khi đặt `PHARMACY_DB_INTEGRATION_TEST=true`.

Hãy dùng tài khoản MySQL phù hợp với môi trường; không dùng thông tin đăng nhập phát triển cho hệ thống triển khai.
