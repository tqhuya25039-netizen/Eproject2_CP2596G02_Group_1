package vn.ninhkieupharma.pharmacy.features.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.features.attendance.dao.AttendanceDAOImpl;
import vn.ninhkieupharma.pharmacy.features.attendance.model.Attendance;
import vn.ninhkieupharma.pharmacy.features.availability.dao.AvailabilityDAOImpl;
import vn.ninhkieupharma.pharmacy.features.availability.model.Availability;
import vn.ninhkieupharma.pharmacy.features.employeeaccounts.dao.EmployeeAccountDAOImpl;
import vn.ninhkieupharma.pharmacy.features.employeeaccounts.model.EmployeeAccount;
import vn.ninhkieupharma.pharmacy.features.goodsreceiving.dao.GoodsReceiptDAOImpl;
import vn.ninhkieupharma.pharmacy.features.goodsreceiving.model.GoodsReceipt;
import vn.ninhkieupharma.pharmacy.features.productcategories.dao.ProductCategoryDAOImpl;
import vn.ninhkieupharma.pharmacy.features.productcategories.model.ProductCategory;
import vn.ninhkieupharma.pharmacy.features.products.dao.ProductDAOImpl;
import vn.ninhkieupharma.pharmacy.features.products.model.Product;
import vn.ninhkieupharma.pharmacy.features.promotionsloyalty.dao.PromotionDAOImpl;
import vn.ninhkieupharma.pharmacy.features.promotionsloyalty.model.Promotion;
import vn.ninhkieupharma.pharmacy.features.sales.dao.SalesInvoiceDAOImpl;
import vn.ninhkieupharma.pharmacy.features.sales.model.SalesInvoice;
import vn.ninhkieupharma.pharmacy.features.stocktakeadjustments.dao.StocktakeAdjustmentDAOImpl;
import vn.ninhkieupharma.pharmacy.features.stocktakeadjustments.model.StocktakeAdjustment;
import vn.ninhkieupharma.pharmacy.features.storagelocations.dao.StorageLocationDAOImpl;
import vn.ninhkieupharma.pharmacy.features.storagelocations.model.StorageLocation;
import vn.ninhkieupharma.pharmacy.features.storeschedule.dao.StoreScheduleDAOImpl;
import vn.ninhkieupharma.pharmacy.features.storeschedule.model.StoreSchedule;
import vn.ninhkieupharma.pharmacy.features.suppliers.dao.SupplierDAOImpl;
import vn.ninhkieupharma.pharmacy.features.suppliers.model.Supplier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatureDaoMappingTest {
    @Test
    void typedFeatureDaosMapEveryFieldWithoutConnectingToMysql() {
        EmployeeAccount employee = new EmployeeAccount(id(), "E-1", "Employee", "0901", "Staff", "Active");
        Availability availability = new Availability(id(), "Employee", "Monday", "09:00", "17:00", "note");
        StoreSchedule schedule = new StoreSchedule(id(), "Monday", "Morning", "Employee", "Published", "note");
        Attendance attendance = new Attendance(id(), "Employee", "Morning", "09:00", "17:00", "Present");
        ProductCategory category = new ProductCategory(id(), "C-1", "Category", "Description", "Active");
        Product product = new Product(id(), "P-1", "Product", "Category", "Box", "No", "Active", "Supplier");
        Supplier supplier = new Supplier(id(), "S-1", "Supplier", "0902", "s@example.test", "Active");
        GoodsReceipt receipt = new GoodsReceipt(id(), "R-1", "Supplier", "2026-01-02", "3", "Received");
        StorageLocation location = new StorageLocation(id(), "L-1", "Shelf", "Description", "Active");
        StocktakeAdjustment stocktake = new StocktakeAdjustment(id(), "I-1", "Shelf", "2026-01-03", "1", "Done");
        SalesInvoice invoice = new SalesInvoice(id(), "V-1", "2026-01-04", "Customer", "Employee", "100", "Paid");
        Promotion promotion = new Promotion(id(), "PR-1", "Promotion", "10%", "2026-01", "Active");

        assertMapping(EmployeeAccountDAOImpl.codec(), employee,
                List.of("E-1", "Employee", "0901", "Staff", "Active"));
        assertMapping(AvailabilityDAOImpl.codec(), availability,
                List.of("Employee", "Monday", "09:00", "17:00", "note"));
        assertMapping(StoreScheduleDAOImpl.codec(), schedule,
                List.of("Monday", "Morning", "Employee", "Published", "note"));
        assertMapping(AttendanceDAOImpl.codec(), attendance,
                List.of("Employee", "Morning", "09:00", "17:00", "Present"));
        assertMapping(ProductCategoryDAOImpl.codec(), category,
                List.of("C-1", "Category", "Description", "Active"));
        assertMapping(ProductDAOImpl.codec(), product,
                List.of("P-1", "Product", "Category", "Box", "No", "Active", "Supplier"));
        assertMapping(SupplierDAOImpl.codec(), supplier,
                List.of("S-1", "Supplier", "0902", "s@example.test", "Active"));
        assertMapping(GoodsReceiptDAOImpl.codec(), receipt,
                List.of("R-1", "Supplier", "2026-01-02", "3", "Received"));
        assertMapping(StorageLocationDAOImpl.codec(), location,
                List.of("L-1", "Shelf", "Description", "Active"));
        assertMapping(StocktakeAdjustmentDAOImpl.codec(), stocktake,
                List.of("I-1", "Shelf", "2026-01-03", "1", "Done"));
        assertMapping(SalesInvoiceDAOImpl.codec(), invoice,
                List.of("V-1", "2026-01-04", "Customer", "Employee", "100", "Paid"));
        assertMapping(PromotionDAOImpl.codec(), promotion,
                List.of("PR-1", "Promotion", "10%", "2026-01", "Active"));
    }

    private static <T> void assertMapping(EntityCodec<T> codec, T entity, List<String> expectedValues) {
        assertEquals(expectedValues, codec.values(entity));
        assertEquals(entity, codec.fromValues(codec.id(entity), expectedValues));
    }

    private static UUID id() {
        return UUID.randomUUID();
    }
}
