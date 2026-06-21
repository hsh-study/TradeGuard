package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.TradingAccountManagementUseCase;
import seokhoon.trade.application.port.out.StockOrderBookPort;
import seokhoon.trade.application.port.out.StreamingStockOrderBookPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class KisStreamingStockOrderBookAdapter implements StreamingStockOrderBookPort {
    static final String ORDER_BOOK_TR_ID = "H0STASP0";
    static final String EXECUTION_TR_ID = "H0STCNT0";
    private final KisHttpClient http;
    private final KisProperties kis;
    private final TradingAccountManagementUseCase accounts;
    private final ObjectMapper objectMapper;
    private final HttpClient websocketClient;

    public KisStreamingStockOrderBookAdapter(KisHttpClient http, KisProperties kis,
            TradingAccountManagementUseCase accounts, ObjectMapper objectMapper) {
        this.http = http;
        this.kis = kis;
        this.accounts = accounts;
        this.objectMapper = objectMapper;
        this.websocketClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public Subscription subscribe(String stockCode, long accountId,
            Consumer<StockOrderBookPort.Snapshot> consumer,
            Consumer<RuntimeException> errorConsumer) {
        if (stockCode == null || !stockCode.matches("[0-9A-Za-z]{1,12}")) {
            throw new IllegalArgumentException("invalid stockCode");
        }
        var account = accounts.credentials(accountId)
                .orElseThrow(() -> new IllegalArgumentException("active trading account not found"));
        KisEnvironment environment = account.environment();
        kis.validateForRequest(environment);
        String approvalKey = approvalKey(environment);
        Session listener = new Session(stockCode, approvalKey, consumer, errorConsumer, objectMapper);
        websocketClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(kis.websocketUrl(environment)), listener)
                .exceptionally(error -> {
                    listener.fail(new KisApiException("KIS WebSocket connection failed", error));
                    return null;
                });
        return listener;
    }

    private String approvalKey(KisEnvironment environment) {
        KisHttpResponse response = http.postJson(
                URI.create(kis.baseUrl(environment) + kis.getWebsocketApprovalPath()),
                Map.of(),
                Map.of("grant_type", "client_credentials",
                        "appkey", kis.appKey(environment),
                        "secretkey", kis.appSecret(environment)));
        String key = response.body().path("approval_key").asText("");
        if (response.statusCode() != 200 || key.isBlank()) {
            throw new KisApiException("KIS WebSocket approval failed");
        }
        return key;
    }

    static final class Session implements WebSocket.Listener, Subscription {
        private final String stockCode;
        private final String approvalKey;
        private final Consumer<StockOrderBookPort.Snapshot> consumer;
        private final Consumer<RuntimeException> errorConsumer;
        private final ObjectMapper objectMapper;
        private final AtomicReference<BigDecimal> currentPrice = new AtomicReference<>();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final StringBuilder fragments = new StringBuilder();
        private volatile WebSocket websocket;

        Session(String stockCode, String approvalKey,
                Consumer<StockOrderBookPort.Snapshot> consumer,
                Consumer<RuntimeException> errorConsumer, ObjectMapper objectMapper) {
            this.stockCode = stockCode;
            this.approvalKey = approvalKey;
            this.consumer = consumer;
            this.errorConsumer = errorConsumer;
            this.objectMapper = objectMapper;
        }

        @Override public void onOpen(WebSocket webSocket) {
            websocket = webSocket;
            webSocket.request(1);
            sendSubscription(webSocket, ORDER_BOOK_TR_ID, "1");
            sendSubscription(webSocket, EXECUTION_TR_ID, "1");
        }

        private void sendSubscription(WebSocket webSocket, String trId, String type) {
            try {
                String message = objectMapper.writeValueAsString(Map.of(
                        "header", Map.of("approval_key", approvalKey, "custtype", "P",
                                "tr_type", type, "content-type", "utf-8"),
                        "body", Map.of("input", Map.of("tr_id", trId, "tr_key", stockCode))));
                webSocket.sendText(message, true);
            } catch (JacksonException exception) {
                fail(new KisApiException("KIS WebSocket subscription failed", exception));
            }
        }

        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            fragments.append(data);
            if (last) {
                String message = fragments.toString();
                fragments.setLength(0);
                handle(message);
            }
            webSocket.request(1);
            return null;
        }

        private void handle(String message) {
            if (message.startsWith("0|")) {
                String[] envelope = message.split("\\|", 4);
                if (envelope.length < 4) return;
                if (EXECUTION_TR_ID.equals(envelope[1])) {
                    BigDecimal price = KisWebSocketOrderBookParser.executionPrice(envelope[3]);
                    if (price != null) currentPrice.set(price);
                } else if (ORDER_BOOK_TR_ID.equals(envelope[1])) {
                    StockOrderBookPort.Snapshot snapshot = KisWebSocketOrderBookParser.orderBook(
                            envelope[3], currentPrice.get(), Instant.now());
                    if (snapshot != null) consumer.accept(snapshot);
                }
                return;
            }
            if (message.startsWith("{")) {
                try {
                    JsonNode json = objectMapper.readTree(message);
                    if (json.path("header").path("tr_id").asText("").equals("PINGPONG")) return;
                    String code = json.path("body").path("rt_cd").asText("0");
                    if (!"0".equals(code)) fail(new KisApiException("KIS WebSocket subscription rejected"));
                } catch (JacksonException exception) {
                    fail(new KisApiException("Invalid KIS WebSocket response", exception));
                }
            }
        }

        @Override public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return webSocket.sendPong(message);
        }

        @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (closed.compareAndSet(false, true)) {
                errorConsumer.accept(new KisApiException("KIS WebSocket closed unexpectedly"));
            }
            return null;
        }

        @Override public void onError(WebSocket webSocket, Throwable error) {
            fail(new KisApiException("KIS WebSocket stream failed", error));
        }

        void fail(RuntimeException error) {
            if (closed.compareAndSet(false, true)) errorConsumer.accept(error);
        }

        @Override public void close() {
            if (!closed.compareAndSet(false, true)) return;
            WebSocket socket = websocket;
            if (socket != null) {
                sendSubscription(socket, ORDER_BOOK_TR_ID, "0");
                sendSubscription(socket, EXECUTION_TR_ID, "0");
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closed");
            }
        }
    }
}
