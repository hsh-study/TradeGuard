package seokhoon.trade.adapter.health;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.BootReadinessInfrastructurePort;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class BootReadinessInfrastructureAdapter implements BootReadinessInfrastructurePort {
    private final DataSource dataSource;
    private final Flyway flyway;

    public BootReadinessInfrastructureAdapter(DataSource dataSource, Flyway flyway) {
        this.dataSource = dataSource;
        this.flyway = flyway;
    }

    @Override
    public ProbeResult checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2)
                    ? new ProbeResult(true, "CONNECTED")
                    : new ProbeResult(false, "UNAVAILABLE");
        } catch (Exception exception) {
            return new ProbeResult(false, "UNAVAILABLE");
        }
    }

    @Override
    public ProbeResult checkFlyway() {
        try {
            int pending = flyway.info().pending().length;
            return pending == 0
                    ? new ProbeResult(true, "UP_TO_DATE")
                    : new ProbeResult(false, "PENDING_MIGRATIONS");
        } catch (RuntimeException exception) {
            return new ProbeResult(false, "UNAVAILABLE");
        }
    }
}
