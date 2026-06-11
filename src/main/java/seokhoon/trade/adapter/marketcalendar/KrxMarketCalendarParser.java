package seokhoon.trade.adapter.marketcalendar;

import org.springframework.stereotype.Component;
import seokhoon.trade.domain.market.MarketCalendarDay;
import seokhoon.trade.domain.market.MarketCalendarSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
class KrxMarketCalendarParser {
    private static final DateTimeFormatter BASIC_DATE =
            DateTimeFormatter.BASIC_ISO_DATE;
    private final ObjectMapper objectMapper;

    KrxMarketCalendarParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<MarketCalendarDay> parse(String payload, int year) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode rows = findRows(root);
            if (!rows.isArray()) {
                throw new MarketCalendarSyncException(
                        "KRX calendar response does not contain an array"
                );
            }
            List<MarketCalendarDay> result = new ArrayList<>();
            for (JsonNode row : rows) {
                LocalDate date = parseDate(text(row, "tradeDate", "calnd_dd", "date"));
                if (date.getYear() != year) {
                    continue;
                }
                boolean tradingDay = parseTradingDay(row);
                result.add(new MarketCalendarDay(
                        MarketCalendarDay.KRX_STOCK,
                        date,
                        tradingDay,
                        tradingDay
                                ? null
                                : textOrNull(row, "holidayName", "holdy_nm", "name"),
                        MarketCalendarSource.KRX_OFFICIAL
                ));
            }
            if (result.isEmpty()) {
                throw new MarketCalendarSyncException(
                        "KRX calendar response contains no rows for year " + year
                );
            }
            return result;
        } catch (MarketCalendarSyncException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MarketCalendarSyncException(
                    "Failed to parse KRX calendar response",
                    exception
            );
        }
    }

    private static JsonNode findRows(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        for (String field : List.of("OutBlock_1", "output", "data", "calendar")) {
            JsonNode candidate = root.get(field);
            if (candidate != null && candidate.isArray()) {
                return candidate;
            }
        }
        return root;
    }

    private static boolean parseTradingDay(JsonNode row) {
        String value = textOrNull(row, "tradingDay", "opn_yn", "isTradingDay");
        if (value == null) {
            return textOrNull(row, "holidayName", "holdy_nm", "name") == null;
        }
        return switch (value.trim().toUpperCase()) {
            case "TRUE", "Y", "1", "OPEN" -> true;
            case "FALSE", "N", "0", "CLOSED" -> false;
            default -> throw new MarketCalendarSyncException(
                    "Unknown KRX trading day value"
            );
        };
    }

    private static LocalDate parseDate(String value) {
        try {
            String normalized = value.replace("-", "").replace(".", "");
            return LocalDate.parse(normalized, BASIC_DATE);
        } catch (DateTimeParseException exception) {
            throw new MarketCalendarSyncException(
                    "Invalid KRX calendar date",
                    exception
            );
        }
    }

    private static String text(JsonNode row, String... fields) {
        String value = textOrNull(row, fields);
        if (value == null) {
            throw new MarketCalendarSyncException(
                    "KRX calendar row is missing a required field"
            );
        }
        return value;
    }

    private static String textOrNull(JsonNode row, String... fields) {
        for (String field : fields) {
            JsonNode value = row.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }
}
