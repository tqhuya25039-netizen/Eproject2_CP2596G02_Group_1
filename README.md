# Ninh Kieu Pharma

Ứng dụng JavaFX quản lý nhà thuốc, dùng MySQL để lưu dữ liệu nhân sự, sản phẩm, tồn kho và bán hàng.

## Cài và chạy trên Windows

1. Cài JDK 21 trở lên, Maven 3.9 trở lên và MySQL 8.0.16 trở lên. Kiểm tra `java -version` và `mvn -version` trong terminal.
2. Khởi động MySQL. Với database mới, chỉ cần nhập [`environment/01_schema_nkp_db.sql`](environment/01_schema_nkp_db.sql) bằng MySQL Workbench, phpMyAdmin hoặc công cụ MySQL bạn đang dùng. File tạo database `nkp_db` với schema mới nhất và dữ liệu mẫu.
3. Mở PowerShell ở thư mục gốc dự án và chạy `.\environment\run.bat`. Khi được hỏi, nhập thông tin kết nối MySQL và đặt mật khẩu ban đầu cho `admin_demo`. Script gợi ý máy chủ `127.0.0.1`, cổng `3306`, database `nkp_db` và tài khoản MySQL `root`.
4. Đăng nhập bằng `admin_demo` và mật khẩu vừa đặt. Để thử vai trò nhân viên, mở **Nhân viên & tài khoản (UC-01)** và tạo tài khoản `STAFF`.

Script lưu cấu hình trên máy vào `environment/.env`. Git bỏ qua file này. Chạy `.\environment\run.bat /setup` để đổi cấu hình.

Nếu đã có database `nkp_db` và muốn cài thêm một bản mới, tạo bản sao của file SQL rồi đổi tên database ở cả hai câu lệnh `CREATE DATABASE` và `USE` trước khi nhập. Chọn tên mới khi chạy `.\environment\run.bat /setup`.

## Chạy trực tiếp bằng Maven

Đặt các biến môi trường sau trong phiên terminal hoặc cấu hình chạy của IDE rồi chạy `mvn clean javafx:run` tại thư mục gốc dự án. Maven xóa đầu ra build cũ trong `target`, biên dịch lại và khởi động ứng dụng.

| Biến môi trường | Giá trị |
| --- | --- |
| `PHARMACY_DB_HOST` | Máy chủ MySQL, mặc định `127.0.0.1` |
| `PHARMACY_DB_PORT` | Cổng MySQL, mặc định `3306` |
| `PHARMACY_DB_NAME` | Tên database, mặc định `nkp_db` |
| `PHARMACY_DB_USER` | Tài khoản MySQL, mặc định `root` |
| `PHARMACY_DB_PASSWORD` | Mật khẩu MySQL, mặc định để trống |
| `PHARMACY_SAMPLE_ADMIN_PASSWORD` | Mật khẩu tự chọn cho lần đầu khởi tạo `admin_demo` |

## Quan hệ trong database

`employees` lưu hồ sơ nhân viên và thông tin tài khoản trên cùng một dòng. `schedule_assignments` lưu cả phân ca và dữ liệu chấm công. Cờ `has_attendance` phân biệt ca chưa có bản ghi với ca có ghi nhận vắng mặt.

Mỗi khóa ngoại trong schema là một liên kết 1–n. Các cặp như nhà cung cấp và sản phẩm, hóa đơn và sản phẩm, hoặc nhân viên và lịch làm được nối lần lượt qua `supplier_products`, `sale_items` và `schedule_assignments`.

## Kiểm thử

Chạy `mvn test`. Có thể bật thêm các ca kiểm thử bằng biến môi trường sau, với giá trị `true`.

- `PHARMACY_DB_INTEGRATION_TEST` kiểm tra schema MySQL đã cấu hình.
- `PHARMACY_DB_WRITE_INTEGRATION_TEST` bật kiểm thử ghi. Tài khoản MySQL cần quyền tạo và xóa database thử, cùng quyền tạo trigger trong database thử.
- `PHARMACY_UI_INTEGRATION_TEST` bật kiểm thử JavaFX trên máy có giao diện đồ họa.
