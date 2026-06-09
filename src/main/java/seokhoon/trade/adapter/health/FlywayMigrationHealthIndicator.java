package seokhoon.trade.adapter.health;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("flywayMigration")
public class FlywayMigrationHealthIndicator implements HealthIndicator {
    private final Flyway flyway;

    public FlywayMigrationHealthIndicator(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Health health() {
        try {
            MigrationInfo[] pending = flyway.info().pending();
            if (pending.length > 0) {
                return Health.down()
                        .withDetail("pendingMigrations", pending.length)
                        .build();
            }
            MigrationInfo current = flyway.info().current();
            return Health.up()
                    .withDetail(
                            "currentVersion",
                            current == null ? "none" : current.getVersion().getVersion()
                    )
                    .build();
        } catch (RuntimeException exception) {
            return Health.down()
                    .withDetail("migrationState", "unavailable")
                    .build();
        }
    }
}
