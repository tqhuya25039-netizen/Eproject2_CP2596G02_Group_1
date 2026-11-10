# Mô tả cấu trúc CSDL quản lý cửa hàng

CSDL được thiết kế cho MySQL 8.0.16 trở lên. File khởi tạo tương ứng là
[01_schema_quan_ly_cua_hang.sql](./01_schema_quan_ly_cua_hang.sql). Chạy file này trên
một database mới; giá trị mẫu trong cột `password_hash` chỉ là chuỗi giữ chỗ, không dùng để đăng nhập.

Các quy tắc nghiệp vụ như kiểm tra nhân viên có rảnh trong ca, phát hiện hai ca bị chồng
thời gian, trừ tồn kho theo lô FEFO, cộng điểm và xác nhận chương trình khuyến mãi cần
được kiểm tra trong ứng dụng và thực hiện trong transaction. Các khóa và CHECK trong
database chỉ bảo vệ các điều kiện được thể hiện trực tiếp trong từng bảng.

## 1. `roles` — Vai trò tài khoản

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| role_id | BIGINT UNSIGNED | Mã vai trò | Khóa chính, tự tăng |
| role_code | VARCHAR(30) | Mã vai trò, ví dụ ADMIN hoặc STAFF | NOT NULL, UNIQUE |
| role_name | VARCHAR(80) | Tên hiển thị vai trò | NOT NULL |
| description | VARCHAR(255) | Mô tả vai trò | Cho phép NULL |
| created_at | TIMESTAMP | Thời điểm tạo vai trò | NOT NULL, mặc định thời điểm hiện tại |

## 2. `users` — Tài khoản hệ thống

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| user_id | BIGINT UNSIGNED | Mã tài khoản | Khóa chính, tự tăng |
| role_id | BIGINT UNSIGNED | Vai trò của tài khoản | NOT NULL, khóa ngoại → `roles.role_id` |
| username | VARCHAR(50) | Tên đăng nhập | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | Chuỗi hash mật khẩu | NOT NULL; không lưu mật khẩu rõ |
| is_active | TINYINT(1) | Trạng thái sử dụng | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |
| created_at | TIMESTAMP | Thời điểm tạo tài khoản | NOT NULL, mặc định thời điểm hiện tại |

## 3. `employees` — Hồ sơ nhân viên

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| employee_id | BIGINT UNSIGNED | Mã nhân viên | Khóa chính, tự tăng |
| user_id | BIGINT UNSIGNED | Tài khoản đăng nhập liên kết | Cho phép NULL, UNIQUE, khóa ngoại → `users.user_id` |
| employee_code | VARCHAR(20) | Mã nhân viên nghiệp vụ | NOT NULL, UNIQUE |
| full_name | VARCHAR(120) | Họ tên | NOT NULL |
| phone | VARCHAR(20) | Số điện thoại | Cho phép NULL |
| email | VARCHAR(120) | Email | Cho phép NULL, UNIQUE |
| hire_date | DATE | Ngày bắt đầu làm việc | NOT NULL |
| employment_status | VARCHAR(20) | Trạng thái làm việc | NOT NULL, mặc định ACTIVE; CHECK: ACTIVE, ON_LEAVE hoặc INACTIVE |

## 4. `shifts` — Danh mục ca làm

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| shift_id | BIGINT UNSIGNED | Mã ca | Khóa chính, tự tăng |
| shift_name | VARCHAR(80) | Tên ca | NOT NULL |
| start_time | TIME | Giờ bắt đầu | NOT NULL, CHECK nhỏ hơn `end_time` |
| end_time | TIME | Giờ kết thúc | NOT NULL, CHECK lớn hơn `start_time` |
| break_minutes | SMALLINT UNSIGNED | Số phút nghỉ giữa ca | NOT NULL, mặc định 0 |
| is_active | TINYINT(1) | Ca còn được sử dụng không | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |

## 5. `employee_availabilities` — Lịch khả dụng của nhân viên

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| availability_id | BIGINT UNSIGNED | Mã đăng ký khả dụng | Khóa chính, tự tăng |
| employee_id | BIGINT UNSIGNED | Nhân viên đăng ký | NOT NULL, khóa ngoại → `employees.employee_id` |
| available_date | DATE | Ngày có thể làm | NOT NULL |
| start_time | TIME | Giờ bắt đầu có thể làm | NOT NULL, CHECK nhỏ hơn `end_time` |
| end_time | TIME | Giờ kết thúc có thể làm | NOT NULL, CHECK lớn hơn `start_time` |
| note | VARCHAR(255) | Ghi chú đăng ký | Cho phép NULL |

## 6. `work_schedules` — Lịch làm theo tuần

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| schedule_id | BIGINT UNSIGNED | Mã lịch tuần | Khóa chính, tự tăng |
| week_start_date | DATE | Ngày đầu tuần của lịch | NOT NULL, UNIQUE |
| status | VARCHAR(20) | Trạng thái lịch | NOT NULL, mặc định DRAFT; CHECK: DRAFT, PUBLISHED hoặc CANCELLED |
| created_by | BIGINT UNSIGNED | Tài khoản lập lịch | NOT NULL, khóa ngoại → `users.user_id` |
| created_at | TIMESTAMP | Thời điểm lập lịch | NOT NULL, mặc định thời điểm hiện tại |

## 7. `schedule_assignments` — Phân ca cho nhân viên

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| assignment_id | BIGINT UNSIGNED | Mã phân ca | Khóa chính, tự tăng |
| schedule_id | BIGINT UNSIGNED | Lịch tuần chứa ca | NOT NULL, khóa ngoại → `work_schedules.schedule_id` |
| employee_id | BIGINT UNSIGNED | Nhân viên được xếp | NOT NULL, khóa ngoại → `employees.employee_id` |
| shift_id | BIGINT UNSIGNED | Ca được xếp | NOT NULL, khóa ngoại → `shifts.shift_id` |
| work_date | DATE | Ngày làm việc | NOT NULL |
| status | VARCHAR(20) | Trạng thái phân ca | NOT NULL, mặc định PLANNED; CHECK: PLANNED, CANCELLED hoặc COMPLETED |
| note | VARCHAR(255) | Ghi chú phân ca | Cho phép NULL |

Ràng buộc UNIQUE trên `schedule_id`, `employee_id`, `work_date`, `shift_id` ngăn tạo trùng
cùng một phân ca. Việc phát hiện hai ca khác nhau bị chồng giờ cần kiểm tra ở ứng dụng.

## 8. `attendance_records` — Chấm công

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| attendance_id | BIGINT UNSIGNED | Mã bản ghi chấm công | Khóa chính, tự tăng |
| assignment_id | BIGINT UNSIGNED | Ca được chấm công | NOT NULL, UNIQUE, khóa ngoại → `schedule_assignments.assignment_id` |
| check_in_at | DATETIME | Thời điểm vào làm thực tế | Cho phép NULL khi chưa chấm công |
| check_out_at | DATETIME | Thời điểm kết thúc thực tế | Cho phép NULL; CHECK không sớm hơn giờ vào |
| note | VARCHAR(255) | Ghi chú chấm công | Cho phép NULL |

## 9. `product_categories` — Danh mục sản phẩm

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| category_id | BIGINT UNSIGNED | Mã danh mục | Khóa chính, tự tăng |
| category_name | VARCHAR(100) | Tên ngành hàng/danh mục | NOT NULL, UNIQUE |
| description | VARCHAR(255) | Mô tả danh mục | Cho phép NULL |
| is_active | TINYINT(1) | Danh mục có đang hoạt động | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |

## 10. `suppliers` — Nhà cung cấp

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| supplier_id | BIGINT UNSIGNED | Mã nhà cung cấp | Khóa chính, tự tăng |
| supplier_code | VARCHAR(20) | Mã nhà cung cấp | NOT NULL, UNIQUE |
| supplier_name | VARCHAR(150) | Tên nhà cung cấp | NOT NULL |
| phone | VARCHAR(20) | Số điện thoại | Cho phép NULL |
| email | VARCHAR(120) | Email liên hệ | Cho phép NULL |
| address | VARCHAR(255) | Địa chỉ | Cho phép NULL |
| is_active | TINYINT(1) | Nhà cung cấp có đang hoạt động | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |

## 11. `products` — Sản phẩm

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| product_id | BIGINT UNSIGNED | Mã sản phẩm | Khóa chính, tự tăng |
| category_id | BIGINT UNSIGNED | Danh mục của sản phẩm | NOT NULL, khóa ngoại → `product_categories.category_id` |
| product_code | VARCHAR(30) | Mã sản phẩm nội bộ | NOT NULL, UNIQUE |
| barcode | VARCHAR(50) | Mã vạch | Cho phép NULL, UNIQUE |
| product_name | VARCHAR(180) | Tên sản phẩm | NOT NULL |
| dosage_form | VARCHAR(80) | Dạng bào chế/quy cách | Cho phép NULL |
| unit_name | VARCHAR(30) | Đơn vị bán/quản lý | NOT NULL |
| requires_prescription | TINYINT(1) | Sản phẩm có yêu cầu đơn thuốc | NOT NULL, mặc định 0, CHECK chỉ nhận 0 hoặc 1 |
| is_active | TINYINT(1) | Sản phẩm có đang kinh doanh | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |
| created_at | TIMESTAMP | Thời điểm tạo sản phẩm | NOT NULL, mặc định thời điểm hiện tại |

## 12. `supplier_products` — Sản phẩm do nhà cung cấp phân phối

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| supplier_id | BIGINT UNSIGNED | Nhà cung cấp | Khóa chính ghép, khóa ngoại → `suppliers.supplier_id` |
| product_id | BIGINT UNSIGNED | Sản phẩm được cung cấp | Khóa chính ghép, khóa ngoại → `products.product_id` |
| supplier_sku | VARCHAR(50) | Mã sản phẩm theo nhà cung cấp | Cho phép NULL |
| purchase_price | DECIMAL(12,2) | Giá nhập tham khảo | NOT NULL, CHECK ≥ 0 |
| lead_time_days | SMALLINT UNSIGNED | Số ngày giao hàng dự kiến | NOT NULL, mặc định 0 |

## 13. `warehouse_locations` — Vị trí vật lý trong kho

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| location_id | BIGINT UNSIGNED | Mã vị trí | Khóa chính, tự tăng |
| location_code | VARCHAR(30) | Mã kệ/tủ/vị trí | NOT NULL, UNIQUE |
| location_name | VARCHAR(100) | Tên vị trí hiển thị | NOT NULL |
| description | VARCHAR(255) | Mô tả hoặc hướng dẫn vị trí | Cho phép NULL |
| is_active | TINYINT(1) | Vị trí có đang sử dụng | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |

## 14. `inventory_batches` — Tồn kho theo lô và vị trí

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| batch_id | BIGINT UNSIGNED | Mã lô tồn kho | Khóa chính, tự tăng |
| product_id | BIGINT UNSIGNED | Sản phẩm trong lô | NOT NULL, khóa ngoại → `products.product_id` |
| location_id | BIGINT UNSIGNED | Vị trí lưu lô | NOT NULL, khóa ngoại → `warehouse_locations.location_id` |
| batch_code | VARCHAR(50) | Số/mã lô của nhà sản xuất | NOT NULL |
| expiry_date | DATE | Hạn sử dụng | Cho phép NULL nếu sản phẩm không quản lý hạn |
| quantity_on_hand | DECIMAL(12,3) | Số lượng tồn hiện tại | NOT NULL, mặc định 0, CHECK ≥ 0 |
| updated_at | TIMESTAMP | Thời điểm cập nhật tồn gần nhất | NOT NULL, tự cập nhật khi dòng thay đổi |

Ràng buộc UNIQUE ghép `product_id`, `location_id`, `batch_code` không cho khai báo trùng
một lô của cùng sản phẩm tại cùng vị trí.

## 15. `goods_receipts` — Phiếu nhập kho

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| receipt_id | BIGINT UNSIGNED | Mã phiếu nhập | Khóa chính, tự tăng |
| receipt_code | VARCHAR(30) | Số phiếu nhập | NOT NULL, UNIQUE |
| supplier_id | BIGINT UNSIGNED | Nhà cung cấp giao hàng | NOT NULL, khóa ngoại → `suppliers.supplier_id` |
| received_by | BIGINT UNSIGNED | Tài khoản nhận hàng | NOT NULL, khóa ngoại → `users.user_id` |
| received_at | DATETIME | Thời điểm nhận hàng | NOT NULL |
| status | VARCHAR(20) | Trạng thái phiếu | NOT NULL, mặc định RECEIVED; CHECK: RECEIVED hoặc CANCELLED |
| note | VARCHAR(255) | Ghi chú phiếu nhập | Cho phép NULL |

## 16. `goods_receipt_items` — Chi tiết phiếu nhập

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| receipt_item_id | BIGINT UNSIGNED | Mã dòng chi tiết | Khóa chính, tự tăng |
| receipt_id | BIGINT UNSIGNED | Phiếu nhập chứa dòng hàng | NOT NULL, khóa ngoại → `goods_receipts.receipt_id` |
| batch_id | BIGINT UNSIGNED | Lô hàng được nhập | NOT NULL, khóa ngoại → `inventory_batches.batch_id` |
| quantity | DECIMAL(12,3) | Số lượng nhập | NOT NULL, CHECK > 0 |
| unit_cost | DECIMAL(12,2) | Giá nhập trên một đơn vị | NOT NULL, CHECK ≥ 0 |

Mỗi lô chỉ xuất hiện tối đa một lần trong một phiếu nhập (UNIQUE `receipt_id`, `batch_id`).

## 17. `stocktakes` — Phiên kiểm kê

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| stocktake_id | BIGINT UNSIGNED | Mã phiên kiểm kê | Khóa chính, tự tăng |
| location_id | BIGINT UNSIGNED | Vị trí được kiểm kê | NOT NULL, khóa ngoại → `warehouse_locations.location_id` |
| stocktake_date | DATE | Ngày kiểm kê | NOT NULL |
| status | VARCHAR(20) | Trạng thái phiên | NOT NULL, mặc định DRAFT; CHECK: DRAFT, COMPLETED hoặc CANCELLED |
| created_by | BIGINT UNSIGNED | Tài khoản tạo phiên | NOT NULL, khóa ngoại → `users.user_id` |
| note | VARCHAR(255) | Ghi chú kiểm kê | Cho phép NULL |

## 18. `stocktake_items` — Chi tiết kiểm kê

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| stocktake_item_id | BIGINT UNSIGNED | Mã dòng kiểm kê | Khóa chính, tự tăng |
| stocktake_id | BIGINT UNSIGNED | Phiên kiểm kê chứa dòng | NOT NULL, khóa ngoại → `stocktakes.stocktake_id` |
| batch_id | BIGINT UNSIGNED | Lô được đếm | NOT NULL, khóa ngoại → `inventory_batches.batch_id` |
| expected_quantity | DECIMAL(12,3) | Số lượng trên hệ thống tại lúc kiểm kê | NOT NULL, CHECK ≥ 0 |
| counted_quantity | DECIMAL(12,3) | Số lượng đếm thực tế | NOT NULL, CHECK ≥ 0 |
| difference_quantity | DECIMAL(12,3) | Chênh lệch thực tế trừ hệ thống | NOT NULL, CHECK bằng `counted_quantity - expected_quantity` |

Mỗi lô chỉ có một dòng trong một phiên (UNIQUE `stocktake_id`, `batch_id`).

## 19. `customers` — Khách hàng

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| customer_id | BIGINT UNSIGNED | Mã khách hàng | Khóa chính, tự tăng |
| customer_code | VARCHAR(20) | Mã khách hàng | NOT NULL, UNIQUE |
| full_name | VARCHAR(120) | Họ tên khách hàng | NOT NULL |
| phone | VARCHAR(20) | Số điện thoại | Cho phép NULL, UNIQUE |
| email | VARCHAR(120) | Email | Cho phép NULL |
| loyalty_points | INT UNSIGNED | Số điểm hiện có | NOT NULL, mặc định 0 |
| is_active | TINYINT(1) | Trạng thái khách hàng | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |
| created_at | TIMESTAMP | Thời điểm tạo hồ sơ | NOT NULL, mặc định thời điểm hiện tại |

## 20. `customer_allergies` — Dị ứng/tiền sử khách hàng khai báo

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| customer_allergy_id | BIGINT UNSIGNED | Mã khai báo dị ứng | Khóa chính, tự tăng |
| customer_id | BIGINT UNSIGNED | Khách hàng khai báo | NOT NULL, khóa ngoại → `customers.customer_id` |
| allergen_name | VARCHAR(120) | Tên chất/nhóm chất gây dị ứng | NOT NULL |
| note | VARCHAR(255) | Ghi chú bổ sung | Cho phép NULL |
| created_at | TIMESTAMP | Thời điểm ghi nhận | NOT NULL, mặc định thời điểm hiện tại |

UNIQUE ghép `customer_id`, `allergen_name` tránh ghi trùng một dị ứng cho cùng khách.

## 21. `loyalty_programs` — Cấu hình tích điểm

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| loyalty_program_id | BIGINT UNSIGNED | Mã cấu hình chương trình | Khóa chính, tự tăng |
| program_name | VARCHAR(100) | Tên chương trình | NOT NULL |
| points_per_1000_currency | DECIMAL(10,4) | Điểm nhận được trên mỗi 1.000 đơn vị tiền | NOT NULL, CHECK ≥ 0 |
| currency_per_point | DECIMAL(12,2) | Giá trị tiền quy đổi cho một điểm | NOT NULL, CHECK ≥ 0 |
| is_active | TINYINT(1) | Chương trình có bật | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |
| updated_at | TIMESTAMP | Thời điểm cập nhật cấu hình | NOT NULL, tự cập nhật khi dòng thay đổi |

## 22. `promotions` — Chương trình khuyến mãi

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| promotion_id | BIGINT UNSIGNED | Mã khuyến mãi | Khóa chính, tự tăng |
| promotion_code | VARCHAR(30) | Mã chương trình | NOT NULL, UNIQUE |
| promotion_name | VARCHAR(120) | Tên chương trình | NOT NULL |
| discount_percent | DECIMAL(5,2) | Phần trăm giảm giá | NOT NULL, mặc định 0, CHECK từ 0 đến 100 |
| points_multiplier | DECIMAL(8,2) | Hệ số nhân điểm | NOT NULL, mặc định 1, CHECK ≥ 0 |
| starts_at | DATETIME | Thời điểm bắt đầu | NOT NULL, CHECK nhỏ hơn `ends_at` |
| ends_at | DATETIME | Thời điểm kết thúc | NOT NULL, CHECK lớn hơn `starts_at` |
| is_active | TINYINT(1) | Khuyến mãi đang bật hay tắt | NOT NULL, mặc định 1, CHECK chỉ nhận 0 hoặc 1 |

## 23. `sales` — Hóa đơn bán hàng

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| sale_id | BIGINT UNSIGNED | Mã hóa đơn | Khóa chính, tự tăng |
| sale_code | VARCHAR(30) | Số hóa đơn | NOT NULL, UNIQUE |
| customer_id | BIGINT UNSIGNED | Khách hàng mua | Cho phép NULL, khóa ngoại → `customers.customer_id` |
| employee_id | BIGINT UNSIGNED | Nhân viên bán hàng | NOT NULL, khóa ngoại → `employees.employee_id` |
| promotion_id | BIGINT UNSIGNED | Khuyến mãi áp dụng | Cho phép NULL, khóa ngoại → `promotions.promotion_id` |
| sold_at | DATETIME | Thời điểm bán | NOT NULL |
| subtotal | DECIMAL(12,2) | Tổng tiền trước giảm giá | NOT NULL, CHECK ≥ 0 |
| discount_amount | DECIMAL(12,2) | Tổng tiền giảm | NOT NULL, mặc định 0, CHECK ≥ 0 |
| total_amount | DECIMAL(12,2) | Khách phải thanh toán | NOT NULL, CHECK bằng `subtotal - discount_amount` |
| points_earned | INT UNSIGNED | Điểm phát sinh từ hóa đơn | NOT NULL, mặc định 0 |
| points_redeemed | INT UNSIGNED | Điểm đã dùng để đổi | NOT NULL, mặc định 0 |
| status | VARCHAR(20) | Trạng thái hóa đơn | NOT NULL, mặc định COMPLETED; CHECK: COMPLETED hoặc VOIDED |

## 24. `sale_items` — Chi tiết hóa đơn

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| sale_item_id | BIGINT UNSIGNED | Mã dòng chi tiết | Khóa chính, tự tăng |
| sale_id | BIGINT UNSIGNED | Hóa đơn chứa dòng hàng | NOT NULL, khóa ngoại → `sales.sale_id` |
| product_id | BIGINT UNSIGNED | Sản phẩm được bán | NOT NULL, khóa ngoại → `products.product_id` |
| quantity | DECIMAL(12,3) | Số lượng bán | NOT NULL, CHECK > 0 |
| unit_price | DECIMAL(12,2) | Giá bán một đơn vị tại thời điểm bán | NOT NULL, CHECK ≥ 0 |
| line_discount | DECIMAL(12,2) | Mức giảm cho dòng hàng | NOT NULL, mặc định 0, CHECK ≥ 0 |
| line_total | DECIMAL(12,2) | Thành tiền dòng | NOT NULL, CHECK bằng `quantity * unit_price - line_discount` |

## 25. `inventory_movements` — Lịch sử biến động kho

| Tên trường | Kiểu dữ liệu | Mô tả ý nghĩa | Điều kiện |
|---|---|---|---|
| movement_id | BIGINT UNSIGNED | Mã biến động kho | Khóa chính, tự tăng |
| batch_id | BIGINT UNSIGNED | Lô hàng biến động | NOT NULL, khóa ngoại → `inventory_batches.batch_id` |
| movement_type | VARCHAR(20) | Loại biến động | NOT NULL, CHECK: RECEIPT, SALE, ADJUSTMENT, DAMAGED hoặc EXPIRED |
| quantity_delta | DECIMAL(12,3) | Lượng thay đổi; dương là nhập, âm là xuất/giảm | NOT NULL, CHECK khác 0 |
| source_type | VARCHAR(20) | Loại chứng từ nguồn | Cho phép NULL; ví dụ GOODS_RECEIPT, SALE, STOCKTAKE |
| source_id | BIGINT UNSIGNED | Mã chứng từ nguồn | Cho phép NULL; không tạo FK đa hình |
| moved_at | DATETIME | Thời điểm phát sinh | NOT NULL |
| created_by | BIGINT UNSIGNED | Tài khoản ghi nhận | NOT NULL, khóa ngoại → `users.user_id` |
| note | VARCHAR(255) | Diễn giải | Cho phép NULL |

## Lưu ý triển khai

- Dữ liệu demo có từ 1 đến 10 bản ghi cho mỗi bảng; mật khẩu demo chỉ là chuỗi giữ chỗ.
- `inventory_batches.quantity_on_hand` là số tồn hiện tại; ứng dụng phải cập nhật tồn và
  thêm `inventory_movements` trong cùng transaction khi nhập, bán, kiểm kê, hư hỏng hoặc
  hết hạn. Các dòng biến động mẫu minh họa nhập 100, bán 2, điều chỉnh giảm 1.
- `source_type`/`source_id` dùng để tra chứng từ nguồn, nên được kiểm tra trong ứng dụng
  do không thể đặt một khóa ngoại thông thường tới nhiều bảng.
- Báo cáo ca/giờ làm lấy từ `schedule_assignments` và `attendance_records`; báo cáo doanh thu
  lấy từ `sales`/`sale_items`; báo cáo ngành hàng, nhà cung cấp và hạn dùng tra theo các
  khóa ngoại tương ứng.
