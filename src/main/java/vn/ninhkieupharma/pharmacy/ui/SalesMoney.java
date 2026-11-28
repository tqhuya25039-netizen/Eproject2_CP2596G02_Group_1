package vn.ninhkieupharma.pharmacy.ui;

import vn.ninhkieupharma.pharmacy.common.service.ExchangeRateService;
import vn.ninhkieupharma.pharmacy.common.service.Localization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

record SalesMoney(boolean english, BigDecimal usdToVnd) {
    static SalesMoney capture(ExchangeRateService exchangeRateService) {
        return new SalesMoney(Localization.isEnglish(), exchangeRateService.snapshot().usdToVnd());
    }

    boolean available() {
        return !english || usdToVnd != null;
    }

    String currency() {
        return english ? "USD" : "VNĐ";
    }

    BigDecimal displayAmount(BigDecimal vnd, int scale) {
        return english ? vnd.divide(usdToVnd, scale, RoundingMode.HALF_UP) : vnd;
    }

    BigDecimal toVnd(BigDecimal amount) {
        return english ? amount.multiply(usdToVnd) : amount;
    }

    String format(BigDecimal vnd, int scale) {
        if (!available()) {
            return Localization.text("Tỷ giá chưa sẵn sàng");
        }
        BigDecimal amount = displayAmount(vnd, scale);
        NumberFormat formatter = NumberFormat.getNumberInstance(
                english ? Locale.US : Locale.forLanguageTag("vi-VN"));
        formatter.setMaximumFractionDigits(scale);
        formatter.setMinimumFractionDigits(english ? 2 : 0);
        return formatter.format(amount) + " " + currency();
    }
}
