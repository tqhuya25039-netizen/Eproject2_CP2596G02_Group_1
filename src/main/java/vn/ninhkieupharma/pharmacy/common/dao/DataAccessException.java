package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.common.service.Localization;

import java.sql.SQLException;

public final class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    static DataAccessException fromSql(SQLException cause, String fallback) {
        String message = switch (cause.getErrorCode()) {
            case 1205, 1213 -> "Dữ liệu đang được xử lý ở phiên khác. Hãy tải lại và thử lại.";
            case 1264, 1292, 1406, 3819 -> "Dữ liệu vượt giới hạn hoặc không đúng định dạng.";
            case 1452 -> "Dữ liệu liên quan không tồn tại hoặc đã thay đổi.";
            default -> fallback;
        };
        return new DataAccessException(Localization.text(message), cause);
    }
}
