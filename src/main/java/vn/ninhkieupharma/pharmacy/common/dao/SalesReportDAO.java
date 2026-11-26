package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.domain.sales.SalesReport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SalesReportDAO {
    private static final String OPTIONS_SQL = """
            SELECT 'EMPLOYEE' AS dimension, employee_id AS option_id, employee_code AS option_code,
                   full_name AS option_name
            FROM employees
            UNION ALL
            SELECT 'PRODUCT', product_id, product_code, product_name
            FROM products
            UNION ALL
            SELECT 'CATEGORY', category_id, '', category_name
            FROM product_categories
            ORDER BY dimension, option_name, option_code, option_id
            """;

    // Compute the shares before applying item filters. Subtracting two rounded cumulative
    // shares allocates every cent of invoice revenue and discount across its lines.
    private static final String REPORT_SQL = """
            WITH filter_values AS (
                SELECT CAST(? AS SIGNED) AS employee_id, CAST(? AS SIGNED) AS product_id,
                       CAST(? AS SIGNED) AS category_id
            ), report_sales AS (
                SELECT s.sale_id, DATE(s.sold_at) AS sale_day, s.employee_id,
                       s.total_amount, s.discount_amount
                FROM sales s
                CROSS JOIN filter_values f
                WHERE s.status = 'COMPLETED' AND s.sold_at >= ? AND s.sold_at < ?
                  AND (f.employee_id IS NULL OR s.employee_id = f.employee_id)
            ), invoice_items AS (
                SELECT s.sale_id, s.total_amount, s.discount_amount, si.sale_item_id,
                       si.product_id, p.category_id, si.line_total, si.line_discount, si.quantity,
                       SUM(si.line_total) OVER (PARTITION BY s.sale_id) AS items_total,
                       SUM(si.line_discount) OVER (PARTITION BY s.sale_id) AS items_discount
                FROM report_sales s
                JOIN sale_items si ON si.sale_id = s.sale_id
                JOIN products p ON p.product_id = si.product_id
            ), weighted_items AS (
                SELECT sale_id, sale_item_id, product_id, category_id, total_amount,
                       CASE WHEN items_total > 0 THEN line_total ELSE quantity END AS item_weight,
                       CASE WHEN items_discount <= discount_amount THEN line_discount ELSE 0 END AS line_discount,
                       CASE WHEN items_discount <= discount_amount THEN discount_amount - items_discount
                            ELSE discount_amount END AS shared_discount
                FROM invoice_items
            ), measured_items AS (
                SELECT w.*,
                       SUM(item_weight) OVER (PARTITION BY sale_id) AS invoice_weight,
                       SUM(item_weight) OVER (PARTITION BY sale_id ORDER BY sale_item_id
                           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_weight
                FROM weighted_items w
            ), matched_items AS (
                SELECT m.sale_id, m.product_id, m.category_id,
                       ROUND(total_amount * running_weight / invoice_weight, 2)
                           - ROUND(total_amount * (running_weight - item_weight) / invoice_weight, 2) AS revenue,
                       line_discount + ROUND(shared_discount * running_weight / invoice_weight, 2)
                           - ROUND(shared_discount * (running_weight - item_weight) / invoice_weight, 2) AS discount
                FROM measured_items m
                CROSS JOIN filter_values f
                WHERE (f.product_id IS NULL OR m.product_id = f.product_id)
                  AND (f.category_id IS NULL OR m.category_id = f.category_id)
            ), matched_item_totals AS (
                SELECT sale_id, SUM(revenue) AS revenue, SUM(discount) AS discount
                FROM matched_items
                GROUP BY sale_id
            ), matched_sales AS (
                SELECT s.sale_day, s.employee_id,
                       CASE WHEN f.product_id IS NULL AND f.category_id IS NULL THEN s.total_amount
                            ELSE i.revenue END AS revenue,
                       CASE WHEN f.product_id IS NULL AND f.category_id IS NULL THEN s.discount_amount
                            ELSE i.discount END AS discount
                FROM report_sales s
                CROSS JOIN filter_values f
                LEFT JOIN matched_item_totals i ON i.sale_id = s.sale_id
                WHERE (f.product_id IS NULL AND f.category_id IS NULL) OR i.sale_id IS NOT NULL
            )
            SELECT 'DAY' AS dimension, sale_day, NULL AS option_id, '' AS option_code, '' AS option_name,
                   COUNT(*) AS order_count, SUM(revenue) AS revenue, SUM(discount) AS discount
            FROM matched_sales
            GROUP BY sale_day
            UNION ALL
            SELECT 'EMPLOYEE', NULL, e.employee_id, e.employee_code, e.full_name,
                   COUNT(*), SUM(s.revenue), SUM(s.discount)
            FROM matched_sales s
            JOIN employees e ON e.employee_id = s.employee_id
            GROUP BY e.employee_id, e.employee_code, e.full_name
            UNION ALL
            SELECT 'PRODUCT', NULL, p.product_id, p.product_code, p.product_name,
                   COUNT(DISTINCT i.sale_id), SUM(i.revenue), SUM(i.discount)
            FROM matched_items i
            JOIN products p ON p.product_id = i.product_id
            GROUP BY p.product_id, p.product_code, p.product_name
            UNION ALL
            SELECT 'CATEGORY', NULL, c.category_id, '', c.category_name,
                   COUNT(DISTINCT i.sale_id), SUM(i.revenue), SUM(i.discount)
            FROM matched_items i
            JOIN product_categories c ON c.category_id = i.category_id
            GROUP BY c.category_id, c.category_name
            ORDER BY dimension, sale_day, revenue DESC, option_name, option_id
            """;

    private SalesReportDAO() {
    }

    static SalesReport.FilterOptions findOptions(Connection connection) throws SQLException {
        Map<String, List<SalesReport.Option>> options = new HashMap<>();
        try (var statement = connection.prepareStatement(OPTIONS_SQL);
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                options.computeIfAbsent(rows.getString("dimension"), dimension -> new ArrayList<>())
                        .add(option(rows));
            }
        }
        return new SalesReport.FilterOptions(options.getOrDefault("EMPLOYEE", List.of()),
                options.getOrDefault("PRODUCT", List.of()), options.getOrDefault("CATEGORY", List.of()));
    }

    static SalesReport.Rows findRows(Connection connection, SalesReport.Filter filter) throws SQLException {
        List<SalesReport.Row> timeSeries = new ArrayList<>();
        Map<String, List<SalesReport.Breakdown>> breakdowns = new HashMap<>();
        try (var statement = connection.prepareStatement(REPORT_SQL)) {
            statement.setObject(1, filter.employeeId(), Types.BIGINT);
            statement.setObject(2, filter.productId(), Types.BIGINT);
            statement.setObject(3, filter.categoryId(), Types.BIGINT);
            statement.setObject(4, filter.fromInclusive().atStartOfDay());
            statement.setObject(5, filter.endExclusive().atStartOfDay());
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    SalesReport.Totals totals = new SalesReport.Totals(rows.getLong("order_count"),
                            rows.getBigDecimal("revenue"), rows.getBigDecimal("discount"));
                    String dimension = rows.getString("dimension");
                    if ("DAY".equals(dimension)) {
                        timeSeries.add(new SalesReport.Row(rows.getObject("sale_day", LocalDate.class), totals));
                    } else {
                        breakdowns.computeIfAbsent(dimension, key -> new ArrayList<>())
                                .add(new SalesReport.Breakdown(option(rows), totals));
                    }
                }
            }
        }
        return new SalesReport.Rows(timeSeries, breakdowns.getOrDefault("EMPLOYEE", List.of()),
                breakdowns.getOrDefault("PRODUCT", List.of()), breakdowns.getOrDefault("CATEGORY", List.of()));
    }

    private static SalesReport.Option option(ResultSet row) throws SQLException {
        String code = row.getString("option_code");
        String name = row.getString("option_name");
        String label = code.isEmpty() ? name : name + " (" + code + ")";
        return new SalesReport.Option(row.getLong("option_id"), label);
    }
}
