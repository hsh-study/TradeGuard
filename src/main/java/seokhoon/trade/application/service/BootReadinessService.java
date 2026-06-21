package seokhoon.trade.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.GetBootReadinessReportUseCase;
import seokhoon.trade.application.port.in.LiveTradingReadinessUseCase;
import seokhoon.trade.application.port.out.BootReadinessInfrastructurePort;
import seokhoon.trade.application.port.out.KisConfigurationPort;
import seokhoon.trade.application.port.out.LiveTradingRuntimeStatePort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.*;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.kis.KisTokenCacheMode;
import seokhoon.trade.domain.operations.BootReadinessReport;
import seokhoon.trade.domain.operations.BootReadinessReport.ComponentStatus;
import seokhoon.trade.domain.operations.BootReadinessReport.OverallStatus;
import seokhoon.trade.domain.research.DisclosureProvider;

import java.time.Clock;
import java.util.*;

@Service
public class BootReadinessService implements GetBootReadinessReportUseCase {
    private static final Logger log = LoggerFactory.getLogger(BootReadinessService.class);
    private static final Set<String> REQUIRED_ACTUATOR_ENDPOINTS = Set.of("health", "info", "metrics", "prometheus");

    private final BootReadinessInfrastructurePort infrastructure;
    private final KisConfigurationPort kis;
    private final InvestorFlowProperties investorFlow;
    private final DartProperties dart;
    private final DisclosureActualProviderProperties disclosure;
    private final ConsensusProviderProperties consensus;
    private final PaperTradingReportProperties paperTrading;
    private final LiveTradingProperties liveTrading;
    private final LiveTradingReadinessUseCase liveReadiness;
    private final LiveTradingRuntimeStatePort runtimeState;
    private final OperationalMetricsPort metrics;
    private final Environment springEnvironment;
    private final Clock clock;
    private volatile BootReadinessReport latestReport;

    @Autowired
    public BootReadinessService(BootReadinessInfrastructurePort infrastructure,
            KisConfigurationPort kis, InvestorFlowProperties investorFlow,
            DartProperties dart, DisclosureActualProviderProperties disclosure,
            ConsensusProviderProperties consensus, PaperTradingReportProperties paperTrading,
            LiveTradingProperties liveTrading, LiveTradingReadinessUseCase liveReadiness,
            LiveTradingRuntimeStatePort runtimeState, OperationalMetricsPort metrics,
            Environment springEnvironment) {
        this(infrastructure, kis, investorFlow, dart, disclosure, consensus, paperTrading,
                liveTrading, liveReadiness, runtimeState, metrics, springEnvironment, Clock.systemUTC());
    }

    BootReadinessService(BootReadinessInfrastructurePort infrastructure,
            KisConfigurationPort kis, InvestorFlowProperties investorFlow,
            DartProperties dart, DisclosureActualProviderProperties disclosure,
            ConsensusProviderProperties consensus, PaperTradingReportProperties paperTrading,
            LiveTradingProperties liveTrading, LiveTradingReadinessUseCase liveReadiness,
            LiveTradingRuntimeStatePort runtimeState, OperationalMetricsPort metrics,
            Environment springEnvironment, Clock clock) {
        this.infrastructure=infrastructure; this.kis=kis; this.investorFlow=investorFlow;
        this.dart=dart; this.disclosure=disclosure; this.consensus=consensus;
        this.paperTrading=paperTrading; this.liveTrading=liveTrading;
        this.liveReadiness=liveReadiness; this.runtimeState=runtimeState;
        this.metrics=metrics; this.springEnvironment=springEnvironment; this.clock=clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent ignored) {
        BootReadinessReport report = generateReport();
        log.atInfo()
                .addKeyValue("overallStatus", report.overallStatus())
                .addKeyValue("blockingIssuesCount", report.blockingIssues().size())
                .addKeyValue("warningsCount", report.warnings().size())
                .addKeyValue("recommendedActionsCount", report.recommendedActions().size())
                .log("Boot readiness report generated");
    }

    public BootReadinessReport generateReport() {
        List<String> blocking = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        var databaseProbe = infrastructure.checkDatabase();
        ComponentStatus databaseStatus = status(databaseProbe.status(),
                "hibernateValidation=STARTUP_PASSED");
        if (!databaseProbe.ready()) {
            block(blocking, actions, "DATABASE_UNAVAILABLE", "Check database connectivity and credentials");
        }

        var flywayProbe = infrastructure.checkFlyway();
        ComponentStatus flywayStatus = status(flywayProbe.status());
        if (!flywayProbe.ready()) {
            block(blocking, actions, "FLYWAY_NOT_READY", "Review pending or failed Flyway migrations");
        }

        Set<String> exposed = exposedActuatorEndpoints();
        boolean actuatorReady = exposed.containsAll(REQUIRED_ACTUATOR_ENDPOINTS);
        boolean prometheusEnabled = springEnvironment.getProperty(
                "management.endpoint.prometheus.enabled", Boolean.class, false);
        ComponentStatus actuatorStatus = status(actuatorReady ? "READY" : "INCOMPLETE",
                "health="+exposed.contains("health"), "info="+exposed.contains("info"),
                "metrics="+exposed.contains("metrics"), "prometheus="+exposed.contains("prometheus"));
        if (!actuatorReady) warn(warnings, actions, "ACTUATOR_ENDPOINT_EXPOSURE_INCOMPLETE",
                "Expose health, info, metrics, and prometheus endpoints");

        boolean credentialsConfigured = kis.credentialsConfigured();
        boolean encryptionConfigured = kis.tokenEncryptionConfigured();
        ComponentStatus kisStatus = status(kisStatus(credentialsConfigured, encryptionConfigured),
                "environment="+kis.readOnlyEnvironment(), "tokenCacheMode="+kis.tokenCacheMode(),
                "credentialsConfigured="+credentialsConfigured,
                "tokenEncryptionConfigured="+encryptionConfigured,
                "dailyRefreshEnabled="+kis.tokenDailyRefreshEnabled(),
                "tradingEnabled="+liveTrading.isKisTradingEnabled());
        if (kis.tokenCacheMode() == KisTokenCacheMode.DB && !encryptionConfigured) {
            block(blocking, actions, "KIS_DB_TOKEN_ENCRYPTION_NOT_CONFIGURED",
                    "Configure KIS token encryption before using DB token cache");
        }
        if (!credentialsConfigured && kisRequired()) {
            block(blocking, actions, "KIS_CREDENTIALS_NOT_CONFIGURED",
                    "Configure KIS credentials for enabled KIS features");
        } else if (!credentialsConfigured && kis.tokenDailyRefreshEnabled()) {
            warn(warnings, actions, "KIS_DAILY_REFRESH_WITHOUT_CREDENTIALS",
                    "Disable daily token refresh or configure KIS credentials");
        }
        if (kis.readOnlyEnvironment() == KisEnvironment.REAL) {
            warn(warnings, actions, "KIS_READ_ONLY_ENVIRONMENT_REAL",
                    "Prefer DEMO unless REAL read-only data is explicitly required");
        }

        boolean dartConfigured = hasText(dart.getApiBaseUrl()) && hasText(dart.getApiKey());
        ComponentStatus dartStatus = status(dart.isProviderEnabled()
                        ? dartConfigured ? "READY" : "MISCONFIGURED" : "DISABLED",
                "enabled="+dart.isProviderEnabled(), "configurationPresent="+dartConfigured);
        if (dart.isProviderEnabled() && !dartConfigured) {
            block(blocking, actions, "DART_PROVIDER_CONFIGURATION_MISSING",
                    "Configure DART API settings or disable the DART provider");
        }

        boolean amountVerified = investorFlow.isKisAmountUnitVerified();
        ComponentStatus investorStatus = status(investorFlowStatus(amountVerified),
                "enabled="+investorFlow.isProviderEnabled(), "type="+safeProviderType(investorFlow.getProviderType()),
                "amountUnit="+investorFlow.getKisAmountUnit(), "amountUnitVerified="+amountVerified,
                "autoRun="+investorFlow.isImportAutoRun(),
                "diagnosticEnabled="+investorFlow.isDiagnosticEnabled(),
                "diagnosticAllowHttp="+investorFlow.isDiagnosticAllowHttp());
        if (investorFlow.isKisProviderWithUnverifiedAmountUnit() && investorFlow.isImportAutoRun()) {
            block(blocking, actions, "INVESTOR_FLOW_AUTO_RUN_WITH_UNVERIFIED_AMOUNT_UNIT",
                    "Verify KIS investor flow amount unit before enabling auto-run");
        } else if (investorFlow.isKisProviderWithUnverifiedAmountUnit()) {
            warn(warnings, actions, "INVESTOR_FLOW_AMOUNT_UNIT_UNVERIFIED",
                    "Verify KIS investor flow amount unit before collection");
        }
        if (investorFlow.isDiagnosticEnabled() && investorFlow.isDiagnosticAllowHttp()) {
            warn(warnings, actions, "INVESTOR_FLOW_DIAGNOSTIC_HTTP_ENABLED",
                    "Disable investor flow diagnostic HTTP after verification");
        }

        boolean disclosureNeedsDart = disclosure.isEnabled() && disclosure.getType() == DisclosureProvider.DART;
        ComponentStatus disclosureStatus = status(!disclosure.isEnabled() ? "DISABLED"
                        : disclosureNeedsDart && (!dart.isProviderEnabled() || !dartConfigured) ? "MISCONFIGURED" : "READY",
                "enabled="+disclosure.isEnabled(), "type="+disclosure.getType(), "autoRun="+disclosure.isAutoRun());
        if (disclosureNeedsDart && (!dart.isProviderEnabled() || !dartConfigured)) {
            block(blocking, actions, "DISCLOSURE_DART_CONFIGURATION_MISSING",
                    "Configure DART before enabling disclosure actual provider");
        }

        String consensusType = safeProviderType(consensus.getType());
        boolean localConsensus = "CSV".equals(consensusType) || "MANUAL".equals(consensusType);
        ComponentStatus consensusStatus = status(!consensus.isEnabled() ? "DISABLED"
                        : localConsensus ? "READY" : "EXTERNAL_PROVIDER_WARNING",
                "enabled="+consensus.isEnabled(), "type="+consensusType, "autoRun="+consensus.isAutoRun());
        if (consensus.isEnabled() && !localConsensus) {
            warn(warnings, actions, "EXTERNAL_CONSENSUS_PROVIDER_ENABLED",
                    "Keep external consensus providers disabled in Boot Readiness v1");
        }

        List<String> autoRunSchedulers = new ArrayList<>(List.of("EARLY_MARKET", "CLOSING_BET"));
        if (investorFlow.isImportAutoRun()) autoRunSchedulers.add("INVESTOR_FLOW");
        if (disclosure.isAutoRun()) autoRunSchedulers.add("DISCLOSURE");
        if (paperTrading.isAutoRun()) autoRunSchedulers.add("PAPER_TRADING_REPORT");
        boolean liveAutomation = liveTrading.isLiveTradingEnabled() || liveTrading.isKisTradingEnabled();
        ComponentStatus schedulerStatus = status(liveAutomation ? "LIVE_AUTOMATION_GUARD_REQUIRED" : "SAFE",
                "autoRun="+String.join(",", autoRunSchedulers), "liveOrderAutomationEnabled="+liveAutomation);

        boolean killSwitchEnabled = readKillSwitch(warnings, actions);
        ComponentStatus liveStatus;
        if (!liveAutomation) {
            liveStatus = status("SAFE_DISABLED", "liveTradingEnabled=false", "kisTradingEnabled=false",
                    "killSwitchEnabled="+killSwitchEnabled);
        } else {
            var readiness = liveReadiness.checkReadiness();
            liveStatus = status(readiness.ready() ? "FLAGS_ENABLED_READY" : "BLOCKED",
                    "liveTradingEnabled="+liveTrading.isLiveTradingEnabled(),
                    "kisTradingEnabled="+liveTrading.isKisTradingEnabled(),
                    "killSwitchEnabled="+killSwitchEnabled, "readiness="+readiness.ready());
            block(blocking, actions, "LIVE_TRADING_FLAGS_ENABLED_AT_BOOT",
                    "Disable live trading flags unless a supervised live session is intended");
            if (!readiness.ready()) {
                block(blocking, actions, "LIVE_TRADING_READINESS_BLOCKED",
                        "Resolve live trading readiness issues while keeping trading disabled");
            }
        }

        ComponentStatus observabilityStatus = status(
                actuatorReady && prometheusEnabled ? "READY" : "INCOMPLETE",
                "prometheusEndpointEnabled="+prometheusEnabled,
                "externalContainersChecked=false");
        if (!prometheusEnabled || !exposed.contains("prometheus")) {
            warn(warnings, actions, "PROMETHEUS_ENDPOINT_NOT_EXPOSED",
                    "Enable and expose the Prometheus actuator endpoint");
        }

        blocking = distinct(blocking); warnings = distinct(warnings); actions = distinct(actions);
        OverallStatus overall = !blocking.isEmpty() ? OverallStatus.BLOCKED
                : !warnings.isEmpty() ? OverallStatus.WARNING : OverallStatus.READY;
        BootReadinessReport report = new BootReadinessReport(clock.instant(), activeProfiles(),
                springEnvironment.getProperty("tradeguard.environment", "LOCAL"), overall,
                nullable(springEnvironment.getProperty("info.app.version")), databaseStatus, flywayStatus,
                actuatorStatus, kisStatus, dartStatus, investorStatus, disclosureStatus, consensusStatus,
                schedulerStatus, liveStatus, observabilityStatus, blocking, warnings, actions);
        latestReport = report;
        metrics.recordBootReadiness(overall.name().toLowerCase(Locale.ROOT));
        return report;
    }

    @Override
    public Optional<BootReadinessReport> getLatestReport() {
        return Optional.ofNullable(latestReport);
    }

    private boolean kisRequired() {
        return liveTrading.isLiveTradingEnabled() || liveTrading.isKisTradingEnabled()
                || (investorFlow.isProviderEnabled() && "KIS".equalsIgnoreCase(investorFlow.getProviderType()));
    }

    private String kisStatus(boolean credentials, boolean encryption) {
        if (kis.tokenCacheMode() == KisTokenCacheMode.DB && !encryption) return "MISCONFIGURED";
        if (kisRequired() && !credentials) return "MISCONFIGURED";
        return credentials ? "READY" : "NOT_CONFIGURED";
    }

    private String investorFlowStatus(boolean amountVerified) {
        if (!investorFlow.isProviderEnabled()) return "DISABLED";
        if (!amountVerified && investorFlow.isImportAutoRun()) return "BLOCKED";
        return amountVerified ? "READY" : "WARNING";
    }

    private boolean readKillSwitch(List<String> warnings, List<String> actions) {
        try {
            return runtimeState.get().killSwitchEnabled();
        } catch (RuntimeException exception) {
            warn(warnings, actions, "LIVE_TRADING_RUNTIME_STATE_UNAVAILABLE",
                    "Check live trading runtime state persistence");
            return true;
        }
    }

    private Set<String> exposedActuatorEndpoints() {
        String configured = springEnvironment.getProperty("management.endpoints.web.exposure.include", "");
        if (configured.contains("*")) return REQUIRED_ACTUATOR_ENDPOINTS;
        Set<String> values = new HashSet<>();
        Arrays.stream(configured.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(values::add);
        return values;
    }

    private String activeProfiles() {
        String[] profiles = springEnvironment.getActiveProfiles();
        return profiles.length == 0 ? "default" : String.join(",", profiles);
    }

    private static ComponentStatus status(String status, String... facts) {
        return new ComponentStatus(status, List.of(facts));
    }
    private static void block(List<String> issues, List<String> actions, String issue, String action) {
        issues.add(issue); actions.add(action);
    }
    private static void warn(List<String> warnings, List<String> actions, String warning, String action) {
        warnings.add(warning); actions.add(action);
    }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static String safeProviderType(String value) {
        if (value == null || value.isBlank()) return "UNCONFIGURED";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z0-9_-]{1,30}") ? normalized : "CUSTOM";
    }
    private static String nullable(String value) { return hasText(value) ? value : null; }
    private static List<String> distinct(List<String> values) { return values.stream().distinct().toList(); }
}
