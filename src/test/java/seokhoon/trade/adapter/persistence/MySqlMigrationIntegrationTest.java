package seokhoon.trade.adapter.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlMigrationIntegrationTest {
    @Test
    void flywayMigrationCreatesMysqlCompatibleSchemaAndUniqueConstraints() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available"
        );

        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0")) {
            mysql.start();
            DataSource dataSource = dataSource(mysql);

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            assertThat(jdbcTemplate.queryForObject("select count(*) from stocks", Integer.class)).isZero();
            assertThat(tableExists(jdbcTemplate, "daily_prices")).isTrue();
            assertThat(tableExists(jdbcTemplate, "indicator_snapshots")).isTrue();
            assertThat(tableExists(jdbcTemplate, "trading_signals")).isTrue();
            assertThat(tableExists(jdbcTemplate, "trading_signal_reasons")).isTrue();
            assertThat(tableExists(jdbcTemplate, "order_requests")).isTrue();

            insertTradingSignal(jdbcTemplate);
            assertThatThrownBy(() -> insertTradingSignal(jdbcTemplate))
                    .isInstanceOf(DuplicateKeyException.class);

            insertOrderRequest(jdbcTemplate);
            assertThatThrownBy(() -> insertOrderRequest(jdbcTemplate))
                    .isInstanceOf(DuplicateKeyException.class);
        }
    }

    private static DataSource dataSource(MySQLContainer<?> mysql) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(mysql.getDriverClassName());
        dataSource.setUrl(mysql.getJdbcUrl());
        dataSource.setUsername(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        return dataSource;
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where table_schema = database()
                          and table_name = ?
                        """,
                Integer.class,
                tableName
        );
        return count != null && count == 1;
    }

    private static void insertTradingSignal(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                """
                        insert into trading_signals
                            (strategy_name, stock_code, signal_date, signal_type, score, status)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                "CLOSING_BET",
                "005930",
                Date.valueOf("2026-06-05"),
                "BUY_CANDIDATE",
                80,
                "CREATED"
        );
    }

    private static void insertOrderRequest(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                """
                        insert into order_requests
                            (stock_code, side, order_type, quantity, limit_price, status,
                             broker_order_no, strategy_name, trade_date)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                "005930",
                "BUY",
                "LIMIT",
                1,
                BigDecimal.valueOf(50_000),
                "CREATED",
                null,
                "CLOSING_BET",
                Date.valueOf("2026-06-05")
        );
    }
}
