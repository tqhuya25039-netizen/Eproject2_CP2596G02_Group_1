package vn.ninhkieupharma.pharmacy.common.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationTest {
    @Test
    void staffWorkspaceMessagesAreAvailableInEnglish() {
        boolean originalLanguage = Localization.isEnglish();
        if (!originalLanguage) {
            Localization.toggleLanguage();
        }
        try {
            assertEquals("Employee Workspace",
                    Localization.text("Không gian làm việc của nhân viên"));
            assertEquals("Work Functions", Localization.text("Chức năng công việc"));
            assertEquals("Staff Account", Localization.text("Tài khoản nhân viên"));
            assertEquals("Access Denied", Localization.text("Không đủ quyền truy cập"));
            assertEquals("This screen is currently read-only. Write operations require related information to preserve data integrity.",
                    Localization.text("Màn hình này hiện chỉ đọc. Thao tác ghi cần thêm thông tin liên quan để bảo toàn dữ liệu."));
        } finally {
            if (Localization.isEnglish() != originalLanguage) {
                Localization.toggleLanguage();
            }
        }
    }

    @Test
    void salesAndTopbarLabelsUseTheSameEnglishLocale() {
        boolean originalLanguage = Localization.isEnglish();
        if (!originalLanguage) {
            Localization.toggleLanguage();
        }
        try {
            assertEquals("Average Order Value", Localization.text("Giá trị trung bình"));
            assertEquals("Invoice Count", Localization.text("Số lượng hóa đơn"));
            assertEquals("Archived · Sales Restricted", Localization.text("Đã lưu trữ · hạn chế bán"));
            assertEquals("Pending", Localization.text("Đang chờ"));
            assertEquals("Completed", Localization.text("Hoàn tất"));
            assertEquals("Voided", Localization.text("Đã hủy"));
        } finally {
            if (Localization.isEnglish() != originalLanguage) {
                Localization.toggleLanguage();
            }
        }
    }

    @Test
    void productRowsUseEnglishCatalogValues() {
        boolean originalLanguage = Localization.isEnglish();
        if (!originalLanguage) {
            Localization.toggleLanguage();
        }
        try {
            assertEquals("Pain Relief Medicine", Localization.productText("Thuốc giảm đau"));
            assertEquals("Medical Supplies", Localization.productText("Vật tư y tế"));
            assertEquals("Blister Pack", Localization.productText("Vỉ"));
            assertEquals("Tablet", Localization.productText("Viên nén"));
            assertEquals("Yes", Localization.productText("Yes"));
            assertEquals("No", Localization.productText("No"));
            assertEquals("Active", Localization.productText("ACTIVE"));
            assertEquals("Inactive", Localization.productText("INACTIVE"));
        } finally {
            if (Localization.isEnglish() != originalLanguage) {
                Localization.toggleLanguage();
            }
        }
    }
}
