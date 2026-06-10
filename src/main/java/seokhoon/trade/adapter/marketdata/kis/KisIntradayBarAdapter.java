package seokhoon.trade.adapter.marketdata.kis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.IntradayBarPort;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.IntradayBar;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Component
@ConditionalOnProperty(
        name = "tradeguard.market-data.intraday-provider",
        havingValue = "kis"
)
public class KisIntradayBarAdapter implements IntradayBarPort {
    static final String INTRADAY_BAR_PATH =
            "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice";
    static final String INTRADAY_BAR_TR_ID = "FHKST03010200";

    private static final Logger log =
            LoggerFactory.getLogger(KisIntradayBarAdapter.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    private static final DateTimeFormatter KIS_DATE =
            DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter KIS_TIME =
            DateTimeFormatter.ofPattern("HHmmss");
    private static final int MAX_PAGE_COUNT = 20;
    private static final int VWAP_SCALE = 4;

    private final KisHttpClient httpClient;
    private final KisAccessTokenProvider tokenProvider;
    private final KisProperties properties;
    private final OperationalMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public KisIntradayBarAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            OperationalMetricsPort metricsPort
    ) {
        this(
                httpClient,
                tokenProvider,
                properties,
                metricsPort,
                Clock.system(SEOUL)
        );
    }

    KisIntradayBarAdapter(
            KisHttpClient httpClient,
            KisAccessTokenProvider tokenProvider,
            KisProperties properties,
            OperationalMetricsPort metricsPort,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public List<IntradayBar> findBars(
            String stockCode,
            LocalDate tradeDate,
            LocalTime from,
            LocalTime to,
            BarInterval interval
    ) {
        validate(stockCode, tradeDate, from, to, interval);
        if (!tradeDate.equals(LocalDate.now(clock))) {
            return List.of();
        }
        try {
            List<IntradayBar> oneMinuteBars =
                    fetchOneMinuteBars(stockCode, tradeDate, from, to);
            metricsPort.recordKisReadOnly("intradayBar", "success");
            logResult("success", oneMinuteBars.size());
            return interval == BarInterval.ONE_MINUTE
                    ? oneMinuteBars
                    : aggregateFiveMinuteBars(oneMinuteBars);
        } catch (RuntimeException exception) {
            metricsPort.recordKisReadOnly("intradayBar", "failure");
            logResult("failure", 0);
            throw exception;
        }
    }

    private List<IntradayBar> fetchOneMinuteBars(
            String stockCode,
            LocalDate tradeDate,
            LocalTime from,
            LocalTime to
    ) {
        properties.validateForRequest();
        Map<String, String> headers = Map.of(
                "authorization", "Bearer " + tokenProvider.getAccessToken(),
                "appkey", properties.getAppKey(),
                "appsecret", properties.getAppSecret(),
                "tr_id", INTRADAY_BAR_TR_ID,
                "custtype", "P"
        );
        LocalTime baselineTime = from.isAfter(MARKET_OPEN)
                ? from.minusMinutes(1)
                : MARKET_OPEN;
        Map<LocalTime, RawBar> rowsByTime = new TreeMap<>();
        LocalTime cursor = to;

        for (int page = 0; page < MAX_PAGE_COUNT; page++) {
            KisHttpResponse response = httpClient.get(
                    buildUri(stockCode, cursor),
                    headers
            );
            validateResponse(response);
            List<RawBar> pageRows = mapRows(response.body().path("output2"));
            pageRows.stream()
                    .filter(row -> row.tradeDate().equals(tradeDate))
                    .forEach(row -> rowsByTime.put(row.barTime(), row));
            if (pageRows.isEmpty()) {
                break;
            }
            LocalTime earliest = pageRows.stream()
                    .map(RawBar::barTime)
                    .min(LocalTime::compareTo)
                    .orElseThrow();
            if (!earliest.isAfter(baselineTime)) {
                break;
            }
            LocalTime nextCursor = earliest.minusMinutes(1);
            if (!nextCursor.isBefore(cursor)) {
                throw new KisApiException(
                        "KIS intraday bar pagination did not advance"
                );
            }
            cursor = nextCursor;
        }
        return mapBars(stockCode, tradeDate, from, to, rowsByTime);
    }

    private URI buildUri(String stockCode, LocalTime cursor) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("FID_COND_MRKT_DIV_CODE", "J");
        parameters.put("FID_INPUT_ISCD", stockCode);
        parameters.put("FID_INPUT_HOUR_1", KIS_TIME.format(cursor));
        parameters.put("FID_PW_DATA_INCU_YN", "Y");
        parameters.put("FID_ETC_CLS_CODE", "");
        String query = parameters.entrySet().stream()
                .map(entry -> encode(entry.getKey())
                        + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElseThrow();
        return URI.create(properties.getBaseUrl() + INTRADAY_BAR_PATH + "?" + query);
    }

    private static List<IntradayBar> mapBars(
            String stockCode,
            LocalDate tradeDate,
            LocalTime from,
            LocalTime to,
            Map<LocalTime, RawBar> rowsByTime
    ) {
        List<RawBar> rows = new ArrayList<>(rowsByTime.values());
        BigDecimal previousCumulativeTradingValue = initialCumulativeValue(
                from,
                rowsByTime
        );
        List<IntradayBar> bars = new ArrayList<>();
        for (RawBar row : rows) {
            if (row.barTime().isBefore(from)) {
                previousCumulativeTradingValue = row.cumulativeTradingValue();
                continue;
            }
            if (row.barTime().isAfter(to)) {
                break;
            }
            BigDecimal tradingValue = row.cumulativeTradingValue()
                    .subtract(previousCumulativeTradingValue);
            previousCumulativeTradingValue = row.cumulativeTradingValue();
            if (row.volume() <= 0) {
                continue;
            }
            if (tradingValue.signum() < 0) {
                throw new KisApiException(
                        "KIS intraday cumulative trading value decreased"
                );
            }
            BigDecimal vwap = tradingValue.divide(
                    BigDecimal.valueOf(row.volume()),
                    VWAP_SCALE,
                    RoundingMode.HALF_UP
            );
            if (vwap.signum() <= 0) {
                continue;
            }
            bars.add(new IntradayBar(
                    stockCode,
                    tradeDate,
                    row.barTime(),
                    row.openPrice(),
                    row.highPrice(),
                    row.lowPrice(),
                    row.closePrice(),
                    row.volume(),
                    tradingValue,
                    vwap
            ));
        }
        return List.copyOf(bars);
    }

    private static BigDecimal initialCumulativeValue(
            LocalTime from,
            Map<LocalTime, RawBar> rowsByTime
    ) {
        if (!from.isAfter(MARKET_OPEN)) {
            return BigDecimal.ZERO;
        }
        RawBar baseline = rowsByTime.get(from.minusMinutes(1));
        if (baseline == null) {
            throw new KisApiException(
                    "KIS intraday response did not contain the trading value baseline"
            );
        }
        return baseline.cumulativeTradingValue();
    }

    static List<IntradayBar> aggregateFiveMinuteBars(
            List<IntradayBar> oneMinuteBars
    ) {
        Map<LocalTime, List<IntradayBar>> groups = new TreeMap<>();
        oneMinuteBars.stream()
                .sorted(Comparator.comparing(IntradayBar::barTime))
                .forEach(bar -> groups.computeIfAbsent(
                        fiveMinuteBucket(bar.barTime()),
                        ignored -> new ArrayList<>()
                ).add(bar));
        return groups.entrySet().stream()
                .map(entry -> aggregate(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static IntradayBar aggregate(
            LocalTime barTime,
            List<IntradayBar> bars
    ) {
        IntradayBar first = bars.getFirst();
        IntradayBar last = bars.getLast();
        long volume = bars.stream().mapToLong(IntradayBar::volume).sum();
        BigDecimal tradingValue = bars.stream()
                .map(IntradayBar::tradingValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vwap = tradingValue.divide(
                BigDecimal.valueOf(volume),
                VWAP_SCALE,
                RoundingMode.HALF_UP
        );
        return new IntradayBar(
                first.stockCode(),
                first.tradeDate(),
                barTime,
                first.openPrice(),
                bars.stream()
                        .map(IntradayBar::highPrice)
                        .max(BigDecimal::compareTo)
                        .orElseThrow(),
                bars.stream()
                        .map(IntradayBar::lowPrice)
                        .min(BigDecimal::compareTo)
                        .orElseThrow(),
                last.closePrice(),
                volume,
                tradingValue,
                vwap
        );
    }

    private static LocalTime fiveMinuteBucket(LocalTime time) {
        return time.withMinute(time.getMinute() - time.getMinute() % 5)
                .withSecond(0)
                .withNano(0);
    }

    private static List<RawBar> mapRows(JsonNode output) {
        if (!output.isArray()) {
            throw new KisApiException(
                    "KIS intraday bar response did not contain output2"
            );
        }
        List<RawBar> rows = new ArrayList<>();
        for (JsonNode row : output) {
            rows.add(new RawBar(
                    LocalDate.parse(requiredText(row, "stck_bsop_date"), KIS_DATE),
                    LocalTime.parse(requiredText(row, "stck_cntg_hour"), KIS_TIME),
                    decimal(row, "stck_oprc"),
                    decimal(row, "stck_hgpr"),
                    decimal(row, "stck_lwpr"),
                    decimal(row, "stck_prpr"),
                    longValue(row, "cntg_vol"),
                    decimal(row, "acml_tr_pbmn")
            ));
        }
        return rows;
    }

    private static void validateResponse(KisHttpResponse response) {
        if (response.statusCode() != 200) {
            throw new KisApiException(
                    "KIS intraday bar request failed with HTTP "
                            + response.statusCode()
            );
        }
        if (!"0".equals(response.body().path("rt_cd").asText())) {
            String code = response.body().path("msg_cd").asText("unknown");
            String message = response.body().path("msg1").asText("unknown");
            throw new KisApiException(
                    "KIS intraday bar request failed: " + code + " " + message
            );
        }
    }

    private static void validate(
            String stockCode,
            LocalDate tradeDate,
            LocalTime from,
            LocalTime to,
            BarInterval interval
    ) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        Objects.requireNonNull(tradeDate, "tradeDate");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(interval, "interval");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        if (from.isBefore(MARKET_OPEN)) {
            throw new IllegalArgumentException(
                    "KIS intraday bars are available from 09:00"
            );
        }
    }

    private void logResult(String result, int barCount) {
        log.atInfo()
                .addKeyValue("operation", "intradayBar")
                .addKeyValue("result", result)
                .addKeyValue("barCount", barCount)
                .log("KIS read-only request completed");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        try {
            return new BigDecimal(requiredText(row, field));
        } catch (NumberFormatException exception) {
            throw new KisApiException(
                    "KIS response contained invalid " + field,
                    exception
            );
        }
    }

    private static long longValue(JsonNode row, String field) {
        try {
            return Long.parseLong(requiredText(row, field));
        } catch (NumberFormatException exception) {
            throw new KisApiException(
                    "KIS response contained invalid " + field,
                    exception
            );
        }
    }

    private static String requiredText(JsonNode row, String field) {
        JsonNode value = row.path(field);
        if (!value.isValueNode() || value.asText().isBlank()) {
            throw new KisApiException(
                    "KIS response did not contain " + field
            );
        }
        return value.asText();
    }

    private record RawBar(
            LocalDate tradeDate,
            LocalTime barTime,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            long volume,
            BigDecimal cumulativeTradingValue
    ) {
    }
}
