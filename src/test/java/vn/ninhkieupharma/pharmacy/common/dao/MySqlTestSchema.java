package vn.ninhkieupharma.pharmacy.common.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final class MySqlTestSchema implements AutoCloseable {
    static final String ENABLED_ENV = "PHARMACY_DB_WRITE_INTEGRATION_TEST";
    static final Path CURRENT_SQL = Path.of("environment", "01_schema_quan_ly_cua_hang.sql");
    private static final String JDBC_OPTIONS =
            "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";

    private final String name = "pharmacy_test_" + UUID.randomUUID().toString().replace("-", "");
    private final String serverUrl;
    private final Properties credentials;

    private MySqlTestSchema() {
        String host = setting("PHARMACY_DB_HOST", "127.0.0.1");
        int port = Integer.parseInt(setting("PHARMACY_DB_PORT", "3306"));
        if (!host.matches("[A-Za-z0-9.-]+") || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Configure a valid PHARMACY_DB_HOST and PHARMACY_DB_PORT.");
        }
        serverUrl = "jdbc:mysql://" + host + ":" + port + "/";
        credentials = new Properties();
        credentials.setProperty("user", setting("PHARMACY_DB_USER", "root"));
        credentials.setProperty("password", setting("PHARMACY_DB_PASSWORD", ""));
        credentials.setProperty("allowPublicKeyRetrieval", "true");
        credentials.setProperty("useSSL", "false");
    }

    static MySqlTestSchema open() throws SQLException {
        MySqlTestSchema schema = new MySqlTestSchema();
        try (Connection connection = schema.openConnection("");
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + schema.name
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        return schema;
    }

    static MySqlTestSchema withCurrentSchema() throws IOException, SQLException {
        MySqlTestSchema schema = open();
        try {
            schema.executeScript(CURRENT_SQL);
            return schema;
        } catch (IOException | SQLException | RuntimeException exception) {
            try {
                schema.close();
            } catch (SQLException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    Connection connect() throws SQLException {
        return openConnection(name);
    }

    void execute(String sql) throws SQLException {
        try (Connection connection = connect(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    void executeScript(Path path) throws IOException, SQLException {
        executeScript(Files.readString(path));
    }

    void executeScript(String sql) throws SQLException {
        String commands = sql.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(Collectors.joining("\n"));
        try (Connection connection = connect(); var statement = connection.createStatement()) {
            for (String command : commands.split(";\\h*(?:\\R|$)")) {
                String statementSql = command.strip();
                if (statementSql.isEmpty()
                        || statementSql.equalsIgnoreCase("USE quan_ly_cua_hang")
                        || statementSql.matches("(?i)CREATE DATABASE IF NOT EXISTS quan_ly_cua_hang\\s+"
                                + "CHARACTER SET utf8mb4\\s+COLLATE utf8mb4_unicode_ci")) {
                    continue;
                }
                if (statementSql.matches("(?is)^(USE\\b|(?:CREATE|ALTER|DROP)\\s+(?:DATABASE|SCHEMA)\\b).*")) {
                    throw new IllegalArgumentException("The schema fixture already selected a disposable database.");
                }
                statement.execute(statementSql);
            }
        }
    }

    void runInChild(Class<?> mainType, String... args) throws IOException, InterruptedException {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        String java = Path.of(System.getProperty("java.home"), "bin", executable).toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        List<String> command = new ArrayList<>(List.of(java, "-cp", classpath, mainType.getName()));
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder = new ProcessBuilder(command).inheritIO();
        builder.environment().put("PHARMACY_DB_NAME", name);
        Process child = builder.start();
        try {
            if (!child.waitFor(45, TimeUnit.SECONDS)) {
                throw new AssertionError("MySQL test runner timed out.");
            }
            if (child.exitValue() != 0) {
                throw new AssertionError("MySQL test runner failed. See child output.");
            }
        } finally {
            if (child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        try (Connection connection = openConnection(""); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE " + name);
        }
    }

    private Connection openConnection(String database) throws SQLException {
        return DriverManager.getConnection(serverUrl + database + JDBC_OPTIONS, credentials);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
