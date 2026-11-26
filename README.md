# Ninh Kieu Pharma

Ứng dụng JavaFX quản lý nhà thuốc, dùng MySQL để lưu dữ liệu nhân sự, sản phẩm, tồn kho và bán hàng.

## Cài và chạy trên Windows

1. Cài JDK 21 trở lên, Maven 3.9 trở lên và MySQL 8.0.16 trở lên. Kiểm tra `java -version` và `mvn -version` trong terminal.
2. Khởi động MySQL. Với database mới, chỉ cần nhập [`environment/01_schema_quan_ly_cua_hang.sql`](environment/01_schema_quan_ly_cua_hang.sql) bằng MySQL Workbench, phpMyAdmin hoặc công cụ MySQL bạn đang dùng. File tạo database `quan_ly_cua_hang` với schema mới nhất và dữ liệu mẫu.
3. Mở PowerShell ở thư mục gốc dự án và chạy `.\environment\run.bat`. Khi được hỏi, nhập thông tin kết nối MySQL và đặt mật khẩu ban đầu cho `admin_demo`. Script gợi ý máy chủ `127.0.0.1`, cổng `3306`, database `quan_ly_cua_hang` và tài khoản MySQL `root`.
4. Đăng nhập bằng `admin_demo` và mật khẩu vừa đặt. Để thử vai trò nhân viên, mở **Nhân viên & tài khoản (UC-01)** và tạo tài khoản `STAFF`.

Script lưu cấu hình trên máy vào `environment/.env`. Git bỏ qua file này. Chạy `.\environment\run.bat /setup` để đổi cấu hình.

Nếu đã có database `quan_ly_cua_hang` và muốn cài thêm một bản mới, tạo bản sao của file SQL rồi đổi tên database ở cả hai câu lệnh `CREATE DATABASE` và `USE` trước khi nhập. Chọn tên mới khi chạy `.\environment\run.bat /setup`.

## Chạy trực tiếp bằng Maven

Đặt các biến môi trường sau trong phiên terminal hoặc cấu hình chạy của IDE rồi chạy `mvn javafx:run` tại thư mục gốc dự án. Maven tải JavaFX và trình điều khiển MySQL theo `pom.xml`.

| Biến môi trường | Giá trị |
| --- | --- |
| `PHARMACY_DB_HOST` | Máy chủ MySQL, mặc định `127.0.0.1` |
| `PHARMACY_DB_PORT` | Cổng MySQL, mặc định `3306` |
| `PHARMACY_DB_NAME` | Tên database, mặc định `quan_ly_cua_hang` |
| `PHARMACY_DB_USER` | Tài khoản MySQL, mặc định `root` |
| `PHARMACY_DB_PASSWORD` | Mật khẩu MySQL, mặc định để trống |
| `PHARMACY_SAMPLE_ADMIN_PASSWORD` | Mật khẩu tự chọn cho lần đầu khởi tạo `admin_demo` |

## Kiểm thử

Chạy `mvn test`. Có thể bật thêm các ca kiểm thử bằng biến môi trường sau, với giá trị `true`.

- `PHARMACY_DB_INTEGRATION_TEST` kiểm tra schema MySQL đã cấu hình.
- `PHARMACY_DB_WRITE_INTEGRATION_TEST` bật kiểm thử ghi. Tài khoản MySQL cần quyền tạo và xóa database thử, cùng quyền tạo trigger trong database thử.
- `PHARMACY_UI_INTEGRATION_TEST` bật kiểm thử JavaFX trên máy có giao diện đồ họa.
