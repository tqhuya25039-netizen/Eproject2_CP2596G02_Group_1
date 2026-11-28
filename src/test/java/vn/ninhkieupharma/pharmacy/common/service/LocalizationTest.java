package vn.ninhkieupharma.pharmacy.common.service;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationTest {
    @Test
    void goodsReceiptLineDescriptionIsLocalizedWithoutReceiptWording() {
        String vietnamese = "Mỗi dòng hàng ghi nhận sản phẩm, mã lô, hạn sử dụng, vị trí lưu kho, số lượng và giá nhập.";
        boolean originalLanguage = Localization.isEnglish();
        try {
            if (originalLanguage) {
                Localization.toggleLanguage();
            }
            assertEquals(vietnamese, Localization.text(vietnamese));
            Localization.toggleLanguage();
            assertEquals("Each line records a product, batch, expiry date, storage location, quantity, and unit cost.",
                    Localization.text(vietnamese));
        } finally {
            if (Localization.isEnglish() != originalLanguage) {
                Localization.toggleLanguage();
            }
        }
    }

    @Test
    void goodsReceiptDetailLabelsAreLocalizedInEnglish() {
        boolean originalLanguage = Localization.isEnglish();
        try {
            if (!originalLanguage) {
                Localization.toggleLanguage();
            }
            assertEquals("View Batches", Localization.text("Xem lô hàng"));
            assertEquals("Received Batch Details", Localization.text("Chi tiết lô hàng nhập"));
            assertEquals("Goods Receipt", Localization.text("Phiếu nhập"));
            assertEquals("This receipt has no batches.", Localization.text("Phiếu nhập này chưa có lô hàng."));
            assertEquals("No Expiry Date", Localization.text("Chưa có hạn sử dụng"));
        } finally {
            if (Localization.isEnglish() != originalLanguage) {
                Localization.toggleLanguage();
            }
        }
    }

    @Test
    void warehouseModulesAndWorkflowMessagesAreLocalizedInEnglish() {
        boolean originalLanguage = Localization.isEnglish();
        try {
            if (!originalLanguage) {
                Localization.toggleLanguage();
            }
            ModuleCatalog.MODULES.stream()
                    .filter(module -> Set.of("UC-12", "UC-13", "UC-14", "UC-15").contains(module.id()))
                    .map(Localization::localize)
                    .flatMap(module -> Stream.of(
                            Stream.of(module.title(), module.group(), module.description(), module.actionLabel()),
                            module.columns().stream(), module.formFields().stream()).flatMap(stream -> stream))
                    .forEach(label -> assertTrue(label.matches("\\p{ASCII}*"), () -> "Untranslated label: " + label));
            assertEquals("Supplier Invoice No. / Notes", Localization.text("Số hóa đơn NCC / Ghi chú"));
            assertEquals("Create Storage Location", Localization.text("Tạo vị trí kho"));
            assertEquals("Stocktake Details", Localization.text("Chi tiết kiểm kê"));
            assertEquals("Goods Receipt PN-123", Localization.inventoryMovementNoteText("Nhập hàng PN-123"));
            assertEquals("Stocktake adjustment KK-3",
                    Localization.inventoryMovementNoteText("Điều chỉnh từ phiên KK-3"));
            assertEquals("Damaged stock recorded", Localization.inventoryMovementNoteText("Ghi nhận hư hỏng"));
            assertEquals("Quantity must be a valid number.",
                    Localization.text("Số lượng") + Localization.text(" phải là số hợp lệ."));
        } finally {
            if (Localization.isEnglish() != originalLanguage) {
                Localization.toggleLanguage();
            }
        }
    }

    @Test
    void salesModuleMetadataAndStandardSaleValuesAreAvailableInEnglish() {
        boolean originalLanguage = Localization.isEnglish();
        if (!originalLanguage) {
            Localization.toggleLanguage();
        }
        try {
            ModuleCatalog.MODULES.stream()
                    .filter(module -> Set.of("UC-16", "UC-17", "UC-18", "UC-19").contains(module.id()))
                    .map(Localization::localize)
                    .flatMap(module -> Stream.of(
                            Stream.of(module.title(), module.group(), module.description(), module.actionLabel()),
                            module.columns().stream(), module.formFields().stream()).flatMap(stream -> stream))
                    .forEach(label -> assertTrue(label.matches("\\p{ASCII}*"), () -> "Untranslated label: " + label));
            assertEquals("Could not save the pending order.", Localization.text("Không thể lưu đơn bán hàng đang chờ."));
            assertEquals("Pending", Localization.saleStatusText("PENDING"));
            assertEquals("Voided", Localization.saleStatusText("VOIDED"));
            assertEquals("Blister Pack", Localization.saleUnitText("vi"));
            assertEquals("Box", Localization.saleUnitText("hộp"));
        } finally {
            if (Localization.isEnglish() != originalLanguage) {
                Localization.toggleLanguage();
            }
        }
    }

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
