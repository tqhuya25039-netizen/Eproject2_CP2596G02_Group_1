package vn.ninhkieupharma.pharmacy.service;

import java.util.List;
import java.util.Map;

public final class DemoData {
    public record Product(
            String code,
            String name,
            String category,
            String unit,
            String prescription,
            long price,
            int stock) {
    }

    private static final List<Product> PRODUCTS = List.of(
            new Product("SP0001", "Paracetamol 500mg", "Thuốc giảm đau", "Vỉ", "Không", 12000, 240),
            new Product("SP0002", "Vitamin C 500mg", "Vitamin & khoáng chất", "Hộp", "Không", 68000, 86),
            new Product("SP0003", "Amoxicillin 500mg", "Thuốc kháng sinh", "Vỉ", "Có", 45000, 54),
            new Product("SP0004", "Nước muối sinh lý 0,9%", "Vật tư y tế", "Chai", "Không", 8000, 320),
            new Product("SP0005", "Men vi sinh BioCare", "Thực phẩm bổ sung", "Hộp", "Không", 125000, 42),
            new Product("SP0006", "Khẩu trang y tế 4 lớp", "Vật tư y tế", "Hộp", "Không", 35000, 70));

    private static final Map<String, List<List<String>>> ROWS = Map.ofEntries(
            Map.entry("UC-01", List.of(
                    List.of("NV001", "Nguyễn Minh Anh", "0903 123 456", "Quản trị viên", "Đang hoạt động"),
                    List.of("NV002", "Trần Quốc Bảo", "0912 456 780", "Nhân viên", "Đang hoạt động"),
                    List.of("NV003", "Lê Thu Hà", "0935 246 810", "Nhân viên", "Đang hoạt động"),
                    List.of("NV004", "Phạm Gia Huy", "0981 357 924", "Nhân viên", "Đang hoạt động"),
                    List.of("NV005", "Võ Ngọc Lan", "0908 654 321", "Nhân viên", "Tạm nghỉ"))),
            Map.entry("UC-02", List.of(
                    List.of("Trần Quốc Bảo", "02/11/2026", "07:00", "12:00", "Có thể làm ca sáng"),
                    List.of("Lê Thu Hà", "02/11/2026", "12:00", "18:00", "Ưu tiên ca chiều"),
                    List.of("Phạm Gia Huy", "03/11/2026", "18:00", "22:00", "Sau giờ học"),
                    List.of("Võ Ngọc Lan", "04/11/2026", "07:00", "15:00", "Linh hoạt"),
                    List.of("Trần Quốc Bảo", "05/11/2026", "12:00", "22:00", "Có thể tăng ca"))),
            Map.entry("UC-03", List.of(
                    List.of("02/11/2026", "Ca sáng · 07:00–12:00", "Trần Quốc Bảo", "Đã công bố", ""),
                    List.of("02/11/2026", "Ca chiều · 12:00–18:00", "Lê Thu Hà", "Đã công bố", ""),
                    List.of("02/11/2026", "Ca tối · 18:00–22:00", "Phạm Gia Huy", "Đã công bố", ""),
                    List.of("03/11/2026", "Ca sáng · 07:00–12:00", "Võ Ngọc Lan", "Bản nháp", ""),
                    List.of("03/11/2026", "Ca chiều · 12:00–18:00", "Đang tuyển nhân sự", "Còn thiếu", "Cần bổ sung 1 nhân viên"))),
            Map.entry("UC-04", List.of(
                    List.of("Trần Quốc Bảo", "Ca sáng · 07:00–12:00", "06:56", "12:03", "Đúng giờ"),
                    List.of("Lê Thu Hà", "Ca chiều · 12:00–18:00", "12:08", "18:02", "Đi trễ 8 phút"),
                    List.of("Phạm Gia Huy", "Ca tối · 18:00–22:00", "17:58", "22:01", "Đúng giờ"),
                    List.of("Võ Ngọc Lan", "Ca sáng · 07:00–12:00", "07:00", "—", "Đang làm việc"))),
            Map.entry("UC-05", List.of(
                    List.of("Trần Quốc Bảo", "10/2026", "22", "108,5 giờ", "Đã duyệt"),
                    List.of("Lê Thu Hà", "10/2026", "20", "97 giờ", "Đã duyệt"),
                    List.of("Phạm Gia Huy", "10/2026", "18", "86 giờ", "Chờ duyệt"),
                    List.of("Võ Ngọc Lan", "10/2026", "21", "101 giờ", "Đã duyệt"),
                    List.of("Toàn cửa hàng", "10/2026", "81", "392,5 giờ", "Độ phủ ca 94%"))),
            Map.entry("UC-06", List.of(
                    List.of("Trần Quốc Bảo", "Ca sáng", "07:00–12:00", "06:56–12:03", "Đúng giờ"),
                    List.of("Lê Thu Hà", "Ca chiều", "12:00–18:00", "12:08–18:02", "Trễ 8 phút"),
                    List.of("Phạm Gia Huy", "Ca tối", "18:00–22:00", "17:58–22:01", "Đúng giờ"),
                    List.of("Võ Ngọc Lan", "Ca sáng", "07:00–12:00", "07:00–11:55", "Đúng giờ"))),
            Map.entry("UC-07", List.of(
                    List.of("DM001", "Thuốc giảm đau", "Giảm đau, hạ sốt", "Đang hoạt động"),
                    List.of("DM002", "Thuốc kháng sinh", "Thuốc kê đơn", "Đang hoạt động"),
                    List.of("DM003", "Vitamin & khoáng chất", "Bổ sung vi chất", "Đang hoạt động"),
                    List.of("DM004", "Thực phẩm bổ sung", "Hỗ trợ sức khỏe", "Đang hoạt động"),
                    List.of("DM005", "Vật tư y tế", "Dụng cụ và vật tư y tế", "Đang hoạt động"))),
            Map.entry("UC-08", List.of(
                    List.of("SP0001", "Paracetamol 500mg", "Thuốc giảm đau", "Vỉ", "Không", "Đang bán"),
                    List.of("SP0002", "Vitamin C 500mg", "Vitamin & khoáng chất", "Hộp", "Không", "Đang bán"),
                    List.of("SP0003", "Amoxicillin 500mg", "Thuốc kháng sinh", "Vỉ", "Có", "Đang bán"),
                    List.of("SP0004", "Nước muối sinh lý 0,9%", "Vật tư y tế", "Chai", "Không", "Đang bán"),
                    List.of("SP0005", "Men vi sinh BioCare", "Thực phẩm bổ sung", "Hộp", "Không", "Đang bán"),
                    List.of("SP0006", "Khẩu trang y tế 4 lớp", "Vật tư y tế", "Hộp", "Không", "Đang bán"))),
            Map.entry("UC-09", List.of(
                    List.of("NCC001", "Dược phẩm Hậu Giang", "0292 389 0001", "contact@dhg-demo.vn", "Đang hợp tác"),
                    List.of("NCC002", "Thiết bị y tế Mekong", "0292 389 0002", "sales@mekong-demo.vn", "Đang hợp tác"),
                    List.of("NCC003", "Phân phối An Khang", "0292 389 0003", "info@ankhang-demo.vn", "Đang hợp tác"),
                    List.of("NCC004", "Dược liệu Tây Đô", "0292 389 0004", "hello@taydo-demo.vn", "Tạm ngưng"))),
            Map.entry("UC-10", List.of(
                    List.of("Thuốc giảm đau", "12", "30%", "Nhiều mặt hàng"),
                    List.of("Thuốc kháng sinh", "10", "25%", "Nhiều mặt hàng"),
                    List.of("Vitamin & khoáng chất", "9", "22,5%", "Ổn định"),
                    List.of("Thực phẩm bổ sung", "6", "15%", "Có thể mở rộng"),
                    List.of("Vật tư y tế", "3", "7,5%", "Ít mặt hàng"))),
            Map.entry("UC-11", List.of(
                    List.of("Dược phẩm Hậu Giang", "18", "45%", "Cao"),
                    List.of("Thiết bị y tế Mekong", "11", "27,5%", "Trung bình"),
                    List.of("Phân phối An Khang", "8", "20%", "Trung bình"),
                    List.of("Dược liệu Tây Đô", "3", "7,5%", "Thấp"))),
            Map.entry("UC-12", List.of(
                    List.of("PN00125", "Dược phẩm Hậu Giang", "01/11/2026", "8 mặt hàng", "Đã nhận"),
                    List.of("PN00124", "Thiết bị y tế Mekong", "30/10/2026", "4 mặt hàng", "Đã nhận"),
                    List.of("PN00123", "Phân phối An Khang", "28/10/2026", "6 mặt hàng", "Đang kiểm"),
                    List.of("PN00122", "Dược phẩm Hậu Giang", "25/10/2026", "5 mặt hàng", "Đã nhận"))),
            Map.entry("UC-13", List.of(
                    List.of("K-A01", "Kệ A · Tầng 1", "Thuốc không kê đơn", "Đang sử dụng"),
                    List.of("K-A02", "Kệ A · Tầng 2", "Vitamin và thực phẩm bổ sung", "Đang sử dụng"),
                    List.of("K-B01", "Kệ B · Tầng 1", "Thuốc kê đơn", "Đang sử dụng"),
                    List.of("T-KHO", "Tủ bảo quản", "Sản phẩm cần điều kiện riêng", "Đang sử dụng"))),
            Map.entry("UC-14", List.of(
                    List.of("KK00031", "Kệ A · Tầng 1", "01/11/2026", "+2 sản phẩm", "Đang kiểm"),
                    List.of("KK00030", "Kệ B · Tầng 1", "30/10/2026", "-1 sản phẩm", "Đã xác nhận"),
                    List.of("KK00029", "Kệ A · Tầng 2", "27/10/2026", "Khớp tồn", "Hoàn tất"))),
            Map.entry("UC-15", List.of(
                    List.of("01/11/2026 09:30", "Paracetamol 500mg · Lô P102", "Bán hàng", "-2 hộp", "Hóa đơn HD00842"),
                    List.of("01/11/2026 08:15", "Vitamin C 500mg · Lô V203", "Nhập hàng", "+24 hộp", "Phiếu PN00125"),
                    List.of("31/10/2026 16:40", "Nước muối sinh lý · Lô N081", "Hư hỏng", "-1 chai", "Bao bì rò rỉ"),
                    List.of("30/10/2026 10:05", "Men vi sinh BioCare · Lô M112", "Sắp hết hạn", "8 hộp", "Hạn dùng 30/11/2026"))),
            Map.entry("UC-16", List.of(
                    List.of("KH0001", "Nguyễn Thị Mai", "0905 111 222", "Không ghi nhận", "1.250", "Thành viên"),
                    List.of("KH0002", "Trần Văn Nam", "0918 222 333", "Dị ứng penicillin", "680", "Thành viên"),
                    List.of("KH0003", "Lê Ngọc Hân", "0939 333 444", "Không ghi nhận", "2.410", "Thành viên"),
                    List.of("KH0004", "Phạm Quốc Đạt", "0987 444 555", "Dị ứng aspirin", "120", "Thành viên"))),
            Map.entry("UC-17", List.of(
                    List.of("HD00842", "01/11/2026 09:30", "Nguyễn Thị Mai", "Trần Quốc Bảo", "156.000 ₫", "Hoàn tất"),
                    List.of("HD00841", "01/11/2026 09:05", "Khách lẻ", "Lê Thu Hà", "68.000 ₫", "Hoàn tất"),
                    List.of("HD00840", "01/11/2026 08:45", "Trần Văn Nam", "Trần Quốc Bảo", "235.000 ₫", "Hoàn tất"),
                    List.of("HD00839", "31/10/2026 21:12", "Khách lẻ", "Phạm Gia Huy", "43.000 ₫", "Hoàn tất"))),
            Map.entry("UC-18", List.of(
                    List.of("KM-THANG11", "Tháng tri ân khách hàng", "10%", "01/11–30/11/2026", "Đang áp dụng"),
                    List.of("KM-VITAMIN", "Ưu đãi vitamin", "5%", "01/11–15/11/2026", "Đang áp dụng"),
                    List.of("KM-THANHVIEN", "Nhân đôi điểm thành viên", "0%", "01/11–07/11/2026", "Đang áp dụng"),
                    List.of("KM-THANG10", "Chào thu tháng 10", "8%", "01/10–31/10/2026", "Đã kết thúc"))),
            Map.entry("UC-19", List.of(
                    List.of("01/11/2026", "42", "4.860.000 ₫", "216.000 ₫", "115.714 ₫"),
                    List.of("31/10/2026", "58", "6.240.000 ₫", "320.000 ₫", "107.586 ₫"),
                    List.of("30/10/2026", "37", "3.920.000 ₫", "148.000 ₫", "105.946 ₫"),
                    List.of("29/10/2026", "46", "5.180.000 ₫", "265.000 ₫", "112.609 ₫"),
                    List.of("28/10/2026", "39", "4.210.000 ₫", "190.000 ₫", "107.949 ₫"))));

    private DemoData() {
    }

    public static List<List<String>> rowsFor(String useCaseId) {
        return ROWS.getOrDefault(useCaseId, List.of());
    }

    public static List<Product> products() {
        return PRODUCTS;
    }

    public static int activeEmployees() {
        return 4;
    }

    public static int totalProducts() {
        return PRODUCTS.size();
    }

    public static int totalStock() {
        return PRODUCTS.stream().mapToInt(Product::stock).sum();
    }

    public static String todayRevenue() {
        return "4.860.000 ₫";
    }
}
