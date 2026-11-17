package vn.ninhkieupharma.pharmacy.common.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExchangeRateService {
    private static final URI API_URI = URI.create("https://open.er-api.com/v6/latest/USD");
    private static final Pattern RESULT = Pattern.compile("\"result\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern BASE_CODE = Pattern.compile("\"base_code\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VND_RATE = Pattern.compile("\"VND\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern UPDATED_AT =
            Pattern.compile("\"time_last_update_utc\"\\s*:\\s*\"([^\"]+)\"");

    public record Snapshot(
            BigDecimal usdToVnd,
            LocalDate rateDate,
            LocalDate lastCheckedDate,
            String warning) {
        public boolean available() {
            return usdToVnd != null;
        }

        public boolean checkedToday(LocalDate today) {
            return today.equals(lastCheckedDate);
        }
    }

    private record Quote(BigDecimal usdToVnd, LocalDate rateDate) {
    }

    private final Path cachePath;
    private final Clock clock;
    private final URI endpoint;
    private final HttpClient httpClient;
    private volatile Snapshot snapshot;

    public ExchangeRateService() {
        this(defaultCachePath(), Clock.systemDefaultZone(), API_URI,
                HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(4)).build());
    }

    ExchangeRateService(Path cachePath, Clock clock, URI endpoint, HttpClient httpClient) {
        this.cachePath = cachePath;
        this.clock = clock;
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        try {
            snapshot = readCache();
        } catch (IOException | IllegalArgumentException exception) {
            snapshot = new Snapshot(null, null, null,
                    "Could not read the saved USD/VND exchange rate: " + exception.getMessage());
        }
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public synchronized Snapshot refreshOncePerDay() {
        LocalDate today = LocalDate.now(clock);
        Snapshot current = snapshot;
        if (current.checkedToday(today)) {
            return current;
        }

        Quote quote;
        try {
            quote = fetchQuote();
        } catch (IOException | InterruptedException | ExchangeRateException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String warning = "Could not refresh the USD/VND exchange rate: " + exception.getMessage();
            Snapshot failedRefresh = new Snapshot(
                    current.usdToVnd(), current.rateDate(), today, warning);
            try {
                persist(failedRefresh);
            } catch (IOException cacheException) {
                warning += " Could not save the refresh status: " + cacheException.getMessage();
                failedRefresh = new Snapshot(current.usdToVnd(), current.rateDate(), today, warning);
            }
            snapshot = failedRefresh;
            return snapshot;
        }

        Snapshot refreshed = new Snapshot(quote.usdToVnd(), quote.rateDate(), today, "");
        try {
            persist(refreshed);
            snapshot = refreshed;
        } catch (IOException exception) {
            snapshot = new Snapshot(
                    quote.usdToVnd(), quote.rateDate(), today,
                    "Could not save the refreshed USD/VND exchange rate: " + exception.getMessage());
        }
        return snapshot;
    }

    public BigDecimal convertVndToUsd(BigDecimal vndAmount) {
        Snapshot current = snapshot;
        if (!current.available()) {
            throw new IllegalStateException("USD/VND exchange rate is unavailable.");
        }
        return vndAmount.divide(current.usdToVnd(), 8, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal convertUsdToVnd(BigDecimal usdAmount) {
        Snapshot current = snapshot;
        if (!current.available()) {
            throw new IllegalStateException("USD/VND exchange rate is unavailable.");
        }
        return usdAmount.multiply(current.usdToVnd());
    }

    private Quote fetchQuote() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(java.time.Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "NinhKieuPharma/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new ExchangeRateException("ExchangeRate-API returned HTTP " + response.statusCode() + ".");
        }
        return parseQuote(response.body());
    }

    static Quote parseQuote(String json) {
        String result = capture(RESULT, json, "result");
        String baseCode = capture(BASE_CODE, json, "base_code");
        if (!"success".equals(result) || !"USD".equals(baseCode)) {
            throw new ExchangeRateException("ExchangeRate-API returned an unsuccessful USD quote.");
        }
        try {
            BigDecimal rate = new BigDecimal(capture(VND_RATE, json, "VND exchange rate"));
            if (rate.signum() <= 0) {
                throw new ExchangeRateException("ExchangeRate-API returned a non-positive VND rate.");
            }
            LocalDate rateDate = ZonedDateTime.parse(
                    capture(UPDATED_AT, json, "update timestamp"),
                    DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate();
            return new Quote(rate, rateDate);
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new ExchangeRateException("ExchangeRate-API returned an invalid rate or update timestamp.",
                    exception);
        }
    }

    private static String capture(Pattern pattern, String value, String field) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            throw new ExchangeRateException("ExchangeRate-API response is missing " + field + ".");
        }
        return matcher.group(1);
    }

    private Snapshot readCache() throws IOException {
        if (!Files.exists(cachePath)) {
            return new Snapshot(null, null, null, "");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(cachePath)) {
            properties.load(input);
        }
        String rate = properties.getProperty("usdToVnd");
        String rateDate = properties.getProperty("rateDate");
        String checkedDate = properties.getProperty("lastCheckedDate");
        String warning = properties.getProperty("warning", "");
        if (properties.isEmpty()) {
            return new Snapshot(null, null, null, "");
        }
        if (checkedDate == null || ((rate == null) != (rateDate == null))) {
            throw new IOException("The exchange-rate cache is incomplete.");
        }
        try {
            LocalDate lastChecked = LocalDate.parse(checkedDate);
            if (rate == null) {
                return new Snapshot(null, null, lastChecked, warning);
            }
            BigDecimal parsedRate = new BigDecimal(rate);
            if (parsedRate.signum() <= 0) {
                throw new IOException("The exchange-rate cache contains a non-positive rate.");
            }
            return new Snapshot(parsedRate, LocalDate.parse(rateDate), lastChecked, warning);
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new IOException("The exchange-rate cache contains invalid values.", exception);
        }
    }

    private void persist(Snapshot value) throws IOException {
        Path parent = cachePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        if (value.available()) {
            properties.setProperty("usdToVnd", value.usdToVnd().toPlainString());
            properties.setProperty("rateDate", value.rateDate().toString());
        }
        properties.setProperty("lastCheckedDate", value.lastCheckedDate().toString());
        properties.setProperty("warning", value.warning());

        Path temporaryFile = Files.createTempFile(parent, "exchange-rate-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "ExchangeRate-API Open USD/VND rate cache");
            }
            try {
                Files.move(temporaryFile, cachePath,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, cachePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static Path defaultCachePath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path applicationData = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".ninhkieupharma")
                : Path.of(localAppData, "NinhKieuPharma");
        return applicationData.resolve("exchange-rate.properties");
    }

    private static final class ExchangeRateException extends RuntimeException {
        private ExchangeRateException(String message) {
            super(message);
        }

        private ExchangeRateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
