package vn.ninhkieupharma.pharmacy.features.sales.model;

import java.util.UUID;

public record SalesInvoice(UUID id, String invoiceCode, String issuedAt, String customer, String employee, String total, String status) {
}
