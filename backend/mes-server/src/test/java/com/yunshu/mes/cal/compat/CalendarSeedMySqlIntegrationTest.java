package com.yunshu.mes.cal.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class CalendarSeedMySqlIntegrationTest {

    @Test
    void flagshipCalendarSeedIsRepeatableInIsolatedMySqlSchema() throws Exception {
        DbSettings settings = localSettings();
        String schema = "mes_cal_seed_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = connectOrSkip(settings.adminUrl(), settings.username(), settings.password())) {
            execute(admin, "CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            try (Connection db = DriverManager.getConnection(
                    settings.schemaUrl(schema), settings.username(), settings.password())) {
                createMinimalSchema(db);
                seedUsers(db);
                List<String> statements = calendarSeedStatements();

                executeAll(db, statements);
                Counts first = counts(db);
                executeAll(db, statements);
                Counts second = counts(db);

                assertEquals(new Counts(54, 27, 0, 4), first);
                assertEquals(first, second);
            } finally {
                execute(admin, "DROP DATABASE IF EXISTS `" + schema + "`");
            }
        }
    }

    private Connection connectOrSkip(String url, String username, String password) throws SQLException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException ex) {
            Assumptions.assumeTrue(false, "Local MySQL unavailable: " + ex.getSQLState());
            throw ex;
        }
    }

    private void createMinimalSchema(Connection db) throws SQLException {
        executeAll(db, List.of(
                "CREATE TABLE sys_dict_type (dict_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
                        + "dict_name VARCHAR(100), dict_type VARCHAR(100), status CHAR(1), create_by VARCHAR(64), "
                        + "create_time DATETIME, remark VARCHAR(500), UNIQUE KEY uk_dict_type(dict_type))",
                "CREATE TABLE sys_dict_data (dict_code BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
                        + "dict_sort INT, dict_label VARCHAR(100), dict_value VARCHAR(100), dict_type VARCHAR(100), "
                        + "list_class VARCHAR(100), is_default CHAR(1), status CHAR(1), create_by VARCHAR(64), "
                        + "create_time DATETIME, remark VARCHAR(500))",
                "CREATE TABLE sys_user (user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64), "
                        + "real_name VARCHAR(64), UNIQUE KEY uk_username(username))",
                "CREATE TABLE cal_holiday (holiday_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, the_day DATE, "
                        + "holiday_type VARCHAR(64), remark VARCHAR(500), create_by VARCHAR(64), create_time DATETIME(3), "
                        + "UNIQUE KEY uk_cal_holiday_day(the_day))",
                "CREATE TABLE cal_team (team_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, team_code VARCHAR(64), "
                        + "team_name VARCHAR(255), calendar_type VARCHAR(64), remark VARCHAR(500), enable_flag CHAR(1), "
                        + "create_by VARCHAR(64), create_time DATETIME(3), UNIQUE KEY uk_cal_team_code(team_code))",
                "CREATE TABLE cal_plan (plan_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, plan_code VARCHAR(64), "
                        + "plan_name VARCHAR(255), calendar_type VARCHAR(64), start_date DATE, end_date DATE, "
                        + "shift_type VARCHAR(64), shift_method VARCHAR(64), status VARCHAR(64), shift_count INT, "
                        + "remark VARCHAR(500), enable_flag CHAR(1), create_by VARCHAR(64), create_time DATETIME(3), "
                        + "UNIQUE KEY uk_cal_plan_code(plan_code))",
                "CREATE TABLE cal_shift (shift_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, plan_id BIGINT UNSIGNED, "
                        + "order_num INT, shift_name VARCHAR(255), start_time TIME, end_time TIME, remark VARCHAR(500), "
                        + "enable_flag CHAR(1), create_by VARCHAR(64), create_time DATETIME(3))",
                "CREATE TABLE cal_plan_team (record_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
                        + "plan_id BIGINT UNSIGNED, team_id BIGINT UNSIGNED, team_code VARCHAR(64), team_name VARCHAR(64), "
                        + "create_by VARCHAR(64), create_time DATETIME(3))",
                "CREATE TABLE cal_team_member (member_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
                        + "team_id BIGINT UNSIGNED, user_id BIGINT UNSIGNED, user_name VARCHAR(64), nick_name VARCHAR(64), "
                        + "create_by VARCHAR(64), create_time DATETIME(3))",
                "CREATE TABLE cal_teamshift (record_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, the_day VARCHAR(16), "
                        + "team_id BIGINT UNSIGNED, team_name VARCHAR(255), shift_id BIGINT UNSIGNED, "
                        + "shift_name VARCHAR(255), order_num INT, plan_id BIGINT UNSIGNED, calendar_type VARCHAR(64), "
                        + "shift_type VARCHAR(64), remark VARCHAR(500), create_by VARCHAR(64), create_time DATETIME(3))"));
    }

    private void seedUsers(Connection db) throws SQLException {
        execute(db, """
                INSERT INTO sys_user (username, real_name) VALUES
                ('worker01','产线操作工人'),('qc01','质检员'),
                ('supervisor01','生产主管'),('warehouse01','仓库物料员'),
                ('repair01','设备维修员')
                """);
    }

    private List<String> calendarSeedStatements() throws IOException {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/seed/R__seed_story_06_calendar_system.sql"));
        int start = sql.indexOf("-- ========== 排班日历字典 ==========");
        int end = sql.indexOf("-- ========== 账号别名");
        String section = sql.substring(start, end);
        StringBuilder executable = new StringBuilder();
        for (String line : section.lines().toList()) {
            if (!line.stripLeading().startsWith("--")) {
                executable.append(line).append('\n');
            }
        }
        List<String> statements = new ArrayList<>();
        for (String statement : executable.toString().split(";")) {
            if (!statement.isBlank()) {
                statements.add(statement.trim());
            }
        }
        return statements;
    }

    private Counts counts(Connection db) throws SQLException {
        return new Counts(
                count(db, "SELECT COUNT(*) FROM cal_teamshift WHERE calendar_type='ZZ'"),
                count(db, "SELECT COUNT(*) FROM cal_teamshift WHERE calendar_type='CK'"),
                count(db, "SELECT COUNT(*) FROM cal_teamshift WHERE DAYOFWEEK(STR_TO_DATE(the_day,'%Y-%m-%d'))=1"),
                count(db, "SELECT COUNT(*) FROM cal_team_member"));
    }

    private int count(Connection db, String sql) throws SQLException {
        try (Statement statement = db.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private void executeAll(Connection db, List<String> statements) throws SQLException {
        for (String sql : statements) {
            execute(db, sql);
        }
    }

    private void execute(Connection db, String sql) throws SQLException {
        try (Statement statement = db.createStatement()) {
            statement.execute(sql);
        }
    }

    private DbSettings localSettings() throws IOException {
        String yaml = Files.readString(Path.of("src/main/resources/application-local.yml"));
        String url = value(yaml, "url:");
        String username = value(yaml, "username:");
        String password = value(yaml, "password:");
        int databaseStart = url.indexOf('/', "jdbc:mysql://".length()) + 1;
        int queryStart = url.indexOf('?', databaseStart);
        String prefix = url.substring(0, databaseStart);
        String query = queryStart >= 0 ? url.substring(queryStart) : "";
        return new DbSettings(prefix + query, prefix, query, username, password);
    }

    private String value(String yaml, String key) {
        return yaml.lines()
                .map(String::trim)
                .filter(line -> line.startsWith(key))
                .map(line -> line.substring(key.length()).trim())
                .findFirst()
                .orElseThrow();
    }

    private record DbSettings(String adminUrl, String schemaPrefix, String query,
            String username, String password) {
        String schemaUrl(String schema) {
            return schemaPrefix + schema + query;
        }
    }

    private record Counts(int assembly, int warehouse, int sunday, int members) {
    }
}
