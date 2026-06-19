package seokhoon.trade.adapter.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.out.OperationalMetricsPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "tradeguard.market-data.realtime-provider=fake",
                "tradeguard.kis.app-key=sensitive-health-app-key",
                "tradeguard.kis.app-secret=sensitive-health-app-secret",
                "tradeguard.notification.discord.webhook-url=https://discord.example/sensitive-health-webhook"
        }
)
class ActuatorHealthEndpointIntegrationTest {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private OperationalMetricsPort metrics;

    @Test
    void exposesHealthInfoLivenessAndReadinessWithoutSensitiveDetails()
            throws IOException, InterruptedException {
        HttpResponse<String> health = get("/actuator/health");
        HttpResponse<String> info = get("/actuator/info");
        HttpResponse<String> liveness = get("/actuator/health/liveness");
        HttpResponse<String> readiness = get("/actuator/health/readiness");
        HttpResponse<String> metrics = get("/actuator/metrics");
        this.metrics.recordOrderRequest("ACCEPTED");
        HttpResponse<String> prometheus = get("/actuator/prometheus");

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(info.statusCode()).isEqualTo(200);
        assertThat(liveness.statusCode()).isEqualTo(200);
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(metrics.statusCode()).isEqualTo(200);
        assertThat(prometheus.statusCode()).isEqualTo(200);
        assertThat(prometheus.headers().firstValue("Content-Type").orElse(""))
                .contains("text/plain");
        assertThat(prometheus.body())
                .contains("tradeguard_order_request_count_total")
                .contains("application=\"tradeguard\"")
                .doesNotContain("stockCode=")
                .doesNotContain("account=")
                .doesNotContain("token=")
                .doesNotContain("webhook=")
                .doesNotContain("correlationId=");
        assertThat(health.body()).contains("\"status\":\"UP\"");
        assertThat(liveness.body()).contains("\"status\":\"UP\"");
        assertThat(readiness.body()).contains("\"status\":\"UP\"");
        assertThat(String.join(
                health.body(),
                liveness.body(),
                readiness.body()
        ))
                .doesNotContain("sensitive-health-app-key")
                .doesNotContain("sensitive-health-app-secret")
                .doesNotContain("sensitive-health-webhook")
                .doesNotContain("discord.example");
        assertThat(health.headers().firstValue("X-Request-Id")).isPresent();
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path)
                )
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
