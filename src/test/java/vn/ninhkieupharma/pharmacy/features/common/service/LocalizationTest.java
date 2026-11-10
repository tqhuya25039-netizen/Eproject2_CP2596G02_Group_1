package vn.ninhkieupharma.pharmacy.features.common.service;

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
}
