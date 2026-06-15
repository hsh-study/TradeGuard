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
import java.sql.Timestamp;
import java.time.Instant;

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
            assertThat(tableExists(
                    jdbcTemplate,
                    "trading_signal_status_histories"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "order_request_status_histories"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "scheduler_execution_histories"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_candidate_performances"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_follow_up_results"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_strategy_experiments"
            )).isTrue();
            assertThat(tableExists(jdbcTemplate, "market_calendar_days")).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "market_calendar_day_audits"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_data_captures"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_ranking_snapshots"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_after_hours_snapshots"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_intraday_bar_snapshots"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "early_market_market_snapshots"
            )).isTrue();
            assertThat(tableExists(jdbcTemplate, "live_order_requests")).isTrue();
            assertThat(tableExists(jdbcTemplate, "live_positions")).isTrue();
            assertThat(tableExists(jdbcTemplate, "live_position_exit_rules")).isTrue();
            assertThat(tableExists(jdbcTemplate, "live_trade_fills")).isTrue();
            assertThat(tableExists(jdbcTemplate, "live_order_status_histories")).isTrue();
            assertThat(tableExists(jdbcTemplate, "live_trading_runtime_state")).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "live_order_cancel_requests"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "kis_access_tokens"
            )).isTrue();
            assertThat(tableExists(
                    jdbcTemplate,
                    "indicator_warmup_histories"
            )).isTrue();
            assertThat(columnExists(jdbcTemplate,"kis_access_tokens",
                    "encrypted_access_token")).isTrue();
            assertThat(columnExists(jdbcTemplate,"live_order_requests",
                    "remaining_quantity")).isTrue();
            assertThat(columnExists(jdbcTemplate,"live_order_requests",
                    "filled_quantity")).isTrue();
            assertThat(columnExists(jdbcTemplate,"live_order_requests",
                    "expire_at")).isTrue();
            assertThat(columnExists(
                    jdbcTemplate,
                    "trading_signal_status_histories",
                    "actor"
            )).isTrue();
            assertThat(columnExists(
                    jdbcTemplate,
                    "trading_signal_status_histories",
                    "request_correlation_id"
            )).isTrue();
            assertThat(columnExists(
                    jdbcTemplate,
                    "order_request_status_histories",
                    "actor"
            )).isTrue();
            assertThat(columnExists(
                    jdbcTemplate,
                    "order_request_status_histories",
                    "request_correlation_id"
            )).isTrue();
            assertThat(columnExists(
                    jdbcTemplate,
                    "scheduler_execution_histories",
                    "correlation_id"
            )).isTrue();
            assertThat(columnExists(jdbcTemplate, "order_requests", "failure_reason")).isTrue();
            assertThat(columnExists(jdbcTemplate, "order_requests", "failed_at")).isTrue();
            assertThat(columnExists(jdbcTemplate, "order_requests", "retryable")).isTrue();
            assertThat(columnExists(jdbcTemplate, "order_requests", "signal_id")).isTrue();
            assertThat(columnExists(
                    jdbcTemplate,
                    "order_requests",
                    "retry_requested_at"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "order_requests",
                    "idx_order_requests_signal_id"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "trading_signal_status_histories",
                    "idx_signal_status_history_target_created"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "order_request_status_histories",
                    "idx_order_status_history_target_created"
            )).isTrue();
            assertThat(foreignKeyExists(
                    jdbcTemplate,
                    "trading_signal_status_histories",
                    "fk_signal_status_history_signal"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "scheduler_execution_histories",
                    "idx_scheduler_execution_trade_date"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "scheduler_execution_histories",
                    "idx_scheduler_execution_name_trade_date"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "scheduler_execution_histories",
                    "idx_scheduler_execution_status"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_follow_up_results",
                    "idx_early_market_follow_up_trade_date"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_follow_up_results",
                    "idx_early_market_follow_up_decision"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_strategy_experiments",
                    "idx_early_market_experiment_created_at"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "market_calendar_days",
                    "idx_market_calendar_trade_date"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "market_calendar_days",
                    "idx_market_calendar_trading_day"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "market_calendar_days",
                    "idx_market_calendar_source"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "market_calendar_day_audits",
                    "idx_market_calendar_audit_trade_date"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "market_calendar_day_audits",
                    "idx_market_calendar_audit_created_at"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_data_captures",
                    "uk_early_market_capture_date_type"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_intraday_bar_snapshots",
                    "uk_early_market_intraday_bar"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_market_snapshots",
                    "uk_early_market_market_snapshot"
            )).isTrue();
            assertThat(foreignKeyExists(
                    jdbcTemplate,
                    "order_request_status_histories",
                    "fk_order_status_history_order"
            )).isTrue();
            assertThat(foreignKeyExists(
                    jdbcTemplate,
                    "order_requests",
                    "fk_order_requests_trading_signal"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "order_requests",
                    "idx_order_requests_status_retry_requested_at"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_candidate_performances",
                    "idx_early_market_performance_trade_date"
            )).isTrue();
            assertThat(indexExists(
                    jdbcTemplate,
                    "early_market_candidate_performances",
                    "idx_early_market_performance_type_trade_date"
            )).isTrue();
            assertThat(foreignKeyExists(
                    jdbcTemplate,
                    "early_market_candidate_performances",
                    "fk_early_market_performance_signal"
            )).isTrue();

            insertTradingSignal(jdbcTemplate);
            assertThatThrownBy(() -> insertTradingSignal(jdbcTemplate))
                    .isInstanceOf(DuplicateKeyException.class);

            insertOrderRequest(jdbcTemplate);
            Long signalId = jdbcTemplate.queryForObject(
                    "select id from trading_signals where stock_code = '005930'",
                    Long.class
            );
            jdbcTemplate.update(
                    "update order_requests set signal_id = ? where stock_code = '005930'",
                    signalId
            );
            assertThat(jdbcTemplate.queryForObject(
                    "select signal_id from order_requests where stock_code = '005930'",
                    Long.class
            )).isEqualTo(signalId);
            assertThat(jdbcTemplate.queryForObject(
                    "select retryable from order_requests where stock_code = '005930'",
                    Boolean.class
            )).isFalse();
            assertThatThrownBy(() -> insertOrderRequest(jdbcTemplate))
                    .isInstanceOf(DuplicateKeyException.class);

            insertMarketCalendarDay(jdbcTemplate);
            assertThatThrownBy(() -> insertMarketCalendarDay(jdbcTemplate))
                    .isInstanceOf(DuplicateKeyException.class);

            Instant failedAt = Instant.parse("2026-06-05T06:01:00Z");
            jdbcTemplate.update(
                    """
                            update order_requests
                            set status = ?, failure_reason = ?, failed_at = ?, retryable = ?
                            where stock_code = ?
                            """,
                    "BROKER_FAILED",
                    "broker timeout",
                    Timestamp.from(failedAt),
                    true,
                    "005930"
            );
            assertThat(jdbcTemplate.queryForObject(
                    "select failure_reason from order_requests where stock_code = '005930'",
                    String.class
            )).isEqualTo("broker timeout");
            assertThat(jdbcTemplate.queryForObject(
                    "select retryable from order_requests where stock_code = '005930'",
                    Boolean.class
            )).isTrue();
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

    private static boolean columnExists(
            JdbcTemplate jdbcTemplate,
            String tableName,
            String columnName
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = ?
                          and column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count == 1;
    }

    private static boolean indexExists(
            JdbcTemplate jdbcTemplate,
            String tableName,
            String indexName
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.statistics
                        where table_schema = database()
                          and table_name = ?
                          and index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count >= 1;
    }

    private static boolean foreignKeyExists(
            JdbcTemplate jdbcTemplate,
            String tableName,
            String constraintName
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.table_constraints
                        where constraint_schema = database()
                          and table_name = ?
                          and constraint_name = ?
                          and constraint_type = 'FOREIGN KEY'
                        """,
                Integer.class,
                tableName,
                constraintName
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

    private static void insertMarketCalendarDay(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                """
                        insert into market_calendar_days
                            (market, trade_date, trading_day, holiday_name, source,
                             created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "KRX_STOCK",
                Date.valueOf("2026-01-01"),
                false,
                "NEW_YEAR",
                "FALLBACK_GENERATED",
                Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"))
        );
    }
}
