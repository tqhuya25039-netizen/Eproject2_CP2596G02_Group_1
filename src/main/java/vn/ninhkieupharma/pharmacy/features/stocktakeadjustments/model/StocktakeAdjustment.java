package vn.ninhkieupharma.pharmacy.features.stocktakeadjustments.model;

import java.util.UUID;

public record StocktakeAdjustment(
        UUID id, String stocktakeCode, String location, String stocktakeDate, String variance, String status) {
}
