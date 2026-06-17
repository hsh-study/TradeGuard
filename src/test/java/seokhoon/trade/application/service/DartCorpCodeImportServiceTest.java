package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Market;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DartCorpCodeImportServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Test
    void parsesCorpCodeXmlAndSkipsUnlistedCorp() {
        List<DartCorpCodeRecord> records = new DartCorpCodeXmlParser().parse(xml("""
                <result>
                  <list><corp_code>00126380</corp_code><corp_name>삼성전자</corp_name><stock_code>005930</stock_code><modify_date>20240601</modify_date></list>
                  <list><corp_code>99999999</corp_code><corp_name>비상장</corp_name><stock_code></stock_code><modify_date>20240601</modify_date></list>
                </result>
                """));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).corpCode()).isEqualTo("00126380");
        assertThat(records.get(1).stockCode()).isBlank();
    }

    @Test
    void importsListedCorpMappingsAndPreservesExistingMarket() {
        InMemoryMappingPort mappings = new InMemoryMappingPort();
        mappings.save(new DartCorpMapping(1L, "005930", "old", "Old",
                Market.KOSPI, NOW, NOW));
        InMemoryHistoryPort histories = new InMemoryHistoryPort();
        DartCorpCodeImportService service = service(mappings, histories, properties(),
                () -> xml("""
                        <result>
                          <list><corp_code>00126380</corp_code><corp_name>삼성전자 변경</corp_name><stock_code>005930</stock_code><modify_date>20240601</modify_date></list>
                          <list><corp_code>00164779</corp_code><corp_name>SK하이닉스</corp_name><stock_code>000660</stock_code><modify_date>20240601</modify_date></list>
                        </result>
                        """));

        DartCorpCodeImportHistory history = service.importCorpCodes();

        assertThat(history.status()).isEqualTo(DartCorpCodeImportStatus.SUCCESS);
        assertThat(history.matchedStockCount()).isEqualTo(2);
        assertThat(mappings.findByStockCode("005930").orElseThrow().market()).isEqualTo(Market.KOSPI);
        assertThat(mappings.findByStockCode("005930").orElseThrow().corpName()).isEqualTo("삼성전자 변경");
        assertThat(mappings.findByStockCode("000660").orElseThrow().market()).isEqualTo(Market.UNKNOWN);
    }

    @Test
    void recordsPartialWhenUnlistedCorpIsSkipped() {
        DartCorpCodeImportHistory history = service(new InMemoryMappingPort(), new InMemoryHistoryPort(),
                properties(), () -> xml("""
                        <result>
                          <list><corp_code>00126380</corp_code><corp_name>삼성전자</corp_name><stock_code>005930</stock_code><modify_date>20240601</modify_date></list>
                          <list><corp_code>99999999</corp_code><corp_name>비상장</corp_name><stock_code></stock_code><modify_date>20240601</modify_date></list>
                        </result>
                        """)).importCorpCodes();

        assertThat(history.status()).isEqualTo(DartCorpCodeImportStatus.PARTIAL);
        assertThat(history.failureReason()).contains("skipped=1");
    }

    @Test
    void recordsFailedAndRedactsUrlAndApiKey() {
        DartProperties properties = properties();
        properties.setApiKey("secret-key");
        properties.setCorpCodeZipUrl("https://example.test/corpCode.xml?crtfc_key=secret-key");
        DartCorpCodeImportHistory history = service(new InMemoryMappingPort(), new InMemoryHistoryPort(),
                properties, () -> {
                    throw new IllegalStateException("failed https://example.test/corpCode.xml?crtfc_key=secret-key secret-key");
                }).importCorpCodes();

        assertThat(history.status()).isEqualTo(DartCorpCodeImportStatus.FAILED);
        assertThat(history.failureReason()).doesNotContain("secret-key");
        assertThat(history.failureReason()).doesNotContain("https://example.test");
    }

    @Test
    void skipsWhenDisabled() {
        DartProperties properties = properties();
        properties.setCorpCodeImportEnabled(false);
        DartCorpCodeImportHistory history = service(new InMemoryMappingPort(), new InMemoryHistoryPort(),
                properties, () -> xml("<result/>")).importCorpCodes();

        assertThat(history.status()).isEqualTo(DartCorpCodeImportStatus.SKIPPED);
    }

    private static DartCorpCodeImportService service(
            InMemoryMappingPort mappings,
            InMemoryHistoryPort histories,
            DartProperties properties,
            DartCorpCodeProviderPort provider
    ) {
        return new DartCorpCodeImportService(provider, mappings, histories, properties,
                OperationalMetricsPort.noop(), new DartCorpCodeXmlParser(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static DartProperties properties() {
        DartProperties properties = new DartProperties();
        properties.setCorpCodeImportEnabled(true);
        properties.setCorpCodeZipUrl("https://example.test/corpCode.zip");
        return properties;
    }

    private static byte[] xml(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static class InMemoryMappingPort implements DartCorpMappingPort {
        private final List<DartCorpMapping> values = new ArrayList<>();

        @Override public DartCorpMapping save(DartCorpMapping value) {
            values.removeIf(existing -> existing.stockCode().equals(value.stockCode()));
            DartCorpMapping saved = new DartCorpMapping(
                    value.id() == null ? (long) values.size() + 1 : value.id(),
                    value.stockCode(), value.corpCode(), value.corpName(),
                    value.market(), value.createdAt(), value.updatedAt());
            values.add(saved);
            return saved;
        }
        @Override public Optional<DartCorpMapping> findByStockCode(String stockCode) {
            return values.stream().filter(value -> value.stockCode().equals(stockCode)).findFirst();
        }
        @Override public List<DartCorpMapping> findAll() { return values; }
    }

    private static class InMemoryHistoryPort implements DartCorpCodeImportHistoryPort {
        private final List<DartCorpCodeImportHistory> values = new ArrayList<>();
        @Override public DartCorpCodeImportHistory save(DartCorpCodeImportHistory value) {
            values.add(value);
            return value;
        }
        @Override public List<DartCorpCodeImportHistory> findAllCorpCodeImports() { return values; }
    }
}
