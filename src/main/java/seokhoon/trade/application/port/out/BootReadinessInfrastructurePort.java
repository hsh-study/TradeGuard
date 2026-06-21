package seokhoon.trade.application.port.out;

public interface BootReadinessInfrastructurePort {
    ProbeResult checkDatabase();
    ProbeResult checkFlyway();

    record ProbeResult(boolean ready, String status) {}
}
