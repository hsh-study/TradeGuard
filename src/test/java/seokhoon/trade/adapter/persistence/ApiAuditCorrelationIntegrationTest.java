package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import seokhoon.trade.application.port.out.SignalStatusHistoryPort;
import seokhoon.trade.application.port.out.OrderStatusHistoryPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.domain.audit.AuditActor;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiAuditCorrelationIntegrationTest {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private TradingSignalPort tradingSignalPort;

    @Autowired
    private SignalStatusHistoryPort signalStatusHistoryPort;

    @Autowired
    private OrderStatusHistoryPort orderStatusHistoryPort;

    @Autowired
    private TradingSignalJpaRepository tradingSignalRepository;

    @Autowired
    private TradingSignalStatusHistoryJpaRepository signalHistoryRepository;

    @Autowired
    private OrderRequestStatusHistoryJpaRepository orderHistoryRepository;

    @Autowired
    private OrderRequestJpaRepository orderRequestRepository;

    @BeforeEach
    void clearData() {
        orderHistoryRepository.deleteAll();
        signalHistoryRepository.deleteAll();
        orderRequestRepository.deleteAll();
        tradingSignalRepository.deleteAll();
    }

    @Test
    void storesProvidedRequestIdInAuditHistories()
            throws IOException, InterruptedException {
        long signalId = saveSignal("005930", LocalDate.of(2026, 6, 9));

        HttpResponse<String> response = requestMockOrder(signalId, "request-123");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Request-Id"))
                .contains("request-123");
        assertThat(signalStatusHistoryPort.findByTradingSignalId(signalId))
                .allSatisfy(history -> {
                    assertThat(history.actor()).isEqualTo(AuditActor.API);
                    assertThat(history.requestCorrelationId()).isEqualTo("request-123");
                });
        long orderId = orderRequestRepository.findAll().getFirst().id();
        assertThat(orderStatusHistoryPort.findByOrderRequestId(orderId))
                .allSatisfy(history -> {
                    assertThat(history.actor()).isEqualTo(AuditActor.API);
                    assertThat(history.requestCorrelationId()).isEqualTo("request-123");
                });
    }

    @Test
    void storesGeneratedRequestIdWhenHeaderIsMissing()
            throws IOException, InterruptedException {
        long signalId = saveSignal("000660", LocalDate.of(2026, 6, 10));

        HttpResponse<String> response = requestMockOrder(signalId, null);

        String generatedRequestId = response.headers()
                .firstValue("X-Request-Id")
                .orElseThrow();
        assertThat(generatedRequestId).isNotBlank();
        assertThat(signalStatusHistoryPort.findByTradingSignalId(signalId))
                .allSatisfy(history -> {
                    assertThat(history.actor()).isEqualTo(AuditActor.API);
                    assertThat(history.requestCorrelationId())
                            .isEqualTo(generatedRequestId);
                });
    }

    private long saveSignal(String stockCode, LocalDate signalDate) {
        TradingSignal signal = new TradingSignal(
                "CLOSING_BET",
                stockCode,
                signalDate,
                SignalType.BUY_CANDIDATE,
                80,
                List.of("TEST")
        );
        tradingSignalPort.save(signal);
        return tradingSignalPort.findId(
                signal.strategyName(),
                signal.stockCode(),
                signal.signalDate(),
                signal.signalType()
        ).orElseThrow();
    }

    private HttpResponse<String> requestMockOrder(long signalId, String requestId)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create(
                                "http://localhost:" + port
                                        + "/api/signals/" + signalId + "/mock-orders"
                        )
                )
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"quantity\":1,\"limitPrice\":50000}"
                ));
        if (requestId != null) {
            request.header("X-Request-Id", requestId);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
