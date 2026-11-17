package vn.ninhkieupharma.pharmacy.common.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeRateServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void refreshesOncePerDayAndPersistsTheExchangeRate() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/latest", exchange -> {
            requests.incrementAndGet();
            byte[] body = """
                    {"result":"success","base_code":"USD","time_last_update_utc":"Tue, 03 Jun 2025 12:00:00 +0000","rates":{"VND":25000.5}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            Clock clock = Clock.fixed(
                    Instant.parse("2025-06-03T12:00:00Z"), ZoneOffset.UTC);
            Path cache = temporaryDirectory.resolve("exchange-rate.properties");
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/latest");
            ExchangeRateService service = new ExchangeRateService(
                    cache, clock, endpoint, HttpClient.newHttpClient());

            ExchangeRateService.Snapshot refreshed = service.refreshOncePerDay();
            ExchangeRateService.Snapshot unchanged = service.refreshOncePerDay();
            ExchangeRateService restored = new ExchangeRateService(
                    cache, clock, endpoint, HttpClient.newHttpClient());

            assertEquals(new BigDecimal("25000.5"), refreshed.usdToVnd());
            assertEquals(LocalDate.of(2025, 6, 3), refreshed.rateDate());
            assertTrue(unchanged.checkedToday(LocalDate.of(2025, 6, 3)));
            assertEquals(1, requests.get());
            assertEquals(refreshed.usdToVnd(), restored.snapshot().usdToVnd());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void cachesAFailedDailyRefreshWithoutInventingAnExchangeRate() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/latest", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            Clock clock = Clock.fixed(
                    Instant.parse("2025-06-03T12:00:00Z"), ZoneOffset.UTC);
            Path cache = temporaryDirectory.resolve("failed-rate.properties");
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/latest");
            ExchangeRateService service = new ExchangeRateService(
                    cache, clock, endpoint, HttpClient.newHttpClient());

            ExchangeRateService.Snapshot failed = service.refreshOncePerDay();
            ExchangeRateService restored = new ExchangeRateService(
                    cache, clock, endpoint, HttpClient.newHttpClient());
            ExchangeRateService.Snapshot unchanged = restored.refreshOncePerDay();

            assertFalse(failed.available());
            assertTrue(failed.warning().contains("HTTP 503"));
            assertTrue(unchanged.checkedToday(LocalDate.of(2025, 6, 3)));
            assertEquals(failed.warning(), unchanged.warning());
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }
}
