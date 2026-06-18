package seokhoon.trade.adapter.marketdata.kis;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.config.InvestorFlowProperties;
import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.market.InvestorFlowMarket;
import seokhoon.trade.domain.market.InvestorType;
import seokhoon.trade.domain.market.KisInvestorFlowAmountUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KisInvestorFlowProviderAdapterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T07:40:00Z"), ZoneOffset.UTC);

    @Test
    void mapsOfficialStockFieldsAndConvertsConfiguredThousandKrwUnit() {
        RecordingClient client = new RecordingClient(successResponse());
        KisProperties kis = kisProperties(KisEnvironment.DEMO);
        InvestorFlowProperties flow = flowProperties(KisInvestorFlowAmountUnit.THOUSAND_KRW);
        KisInvestorFlowProviderAdapter adapter = adapter(client, kis, flow);

        var result = adapter.fetchStockInvestorFlows("005930", DATE);

        assertThat(result.rejectedCount()).isZero();
        assertThat(result.flows()).hasSize(3);
        assertThat(result.flows()).filteredOn(value -> value.investorType() == InvestorType.FOREIGN)
                .singleElement().satisfies(value -> {
                    assertThat(value.netBuyAmount()).isEqualByComparingTo("120000");
                    assertThat(value.netBuyQuantity()).isEqualTo(12L);
                    assertThat(value.buyAmount()).isEqualByComparingTo("500000");
                    assertThat(value.sellAmount()).isEqualByComparingTo("380000");
                    assertThat(value.buyQuantity()).isEqualTo(50L);
                    assertThat(value.sellQuantity()).isEqualTo(38L);
                    assertThat(value.rawInvestorType()).isEqualTo("외국인");
                });
        assertThat(client.uri.getPath()).isEqualTo(KisInvestorFlowProviderAdapter.STOCK_PATH);
        assertThat(client.uri.getQuery()).contains("FID_COND_MRKT_DIV_CODE=J", "FID_INPUT_ISCD=005930");
        assertThat(client.headers).containsEntry("tr_id", KisInvestorFlowProviderAdapter.STOCK_TR_ID);
        assertThat(client.headers).doesNotContainKeys("CANO", "ACNT_PRDT_CD");
        assertThat(client.timeout).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void reportsPartialWhenOneAggregateInvestorHasNoNetValue() {
        RecordingClient client = new RecordingClient(json("""
                {"rt_cd":"0","output":[{
                  "stck_bsop_date":"20260615",
                  "frgn_ntby_tr_pbmn":"10","frgn_ntby_qty":"1",
                  "prsn_ntby_tr_pbmn":"-10","prsn_ntby_qty":"-1"
                }]}
                """));

        var result = adapter(client, kisProperties(KisEnvironment.DEMO),
                flowProperties(KisInvestorFlowAmountUnit.KRW))
                .fetchStockInvestorFlows("005930", DATE);

        assertThat(result.flows()).hasSize(2);
        assertThat(result.rejectedCount()).isEqualTo(1);
    }

    @Test
    void mapsOfficialMarketFieldsAndParametersOnlyInRealEnvironment() {
        RecordingClient client = new RecordingClient(successResponse());

        var result = adapter(client, kisProperties(KisEnvironment.REAL),
                flowProperties(KisInvestorFlowAmountUnit.KRW))
                .fetchMarketInvestorFlows(InvestorFlowMarket.KOSPI, DATE);

        assertThat(result.flows()).hasSize(3);
        assertThat(result.flows().getFirst().buyAmount()).isNull();
        assertThat(client.uri.getPath()).isEqualTo(KisInvestorFlowProviderAdapter.MARKET_PATH);
        assertThat(client.uri.getQuery())
                .contains("FID_COND_MRKT_DIV_CODE=U")
                .contains("FID_INPUT_ISCD=0001")
                .contains("FID_INPUT_ISCD_1=KSP")
                .contains("FID_INPUT_DATE_1=20260615")
                .contains("FID_INPUT_DATE_2=20260615")
                .contains("FID_INPUT_ISCD_2=0001");
        assertThat(client.headers).containsEntry("tr_id", KisInvestorFlowProviderAdapter.MARKET_TR_ID);
    }

    @Test
    void blocksUnverifiedAmountUnitBeforeTokenOrHttpCall() {
        RecordingClient client = new RecordingClient(successResponse());
        KisAccessTokenProvider token = mock(KisAccessTokenProvider.class);
        var adapter = adapter(client, token, kisProperties(KisEnvironment.DEMO),
                flowProperties(KisInvestorFlowAmountUnit.UNVERIFIED));

        assertThatThrownBy(() -> adapter.fetchStockInvestorFlows("005930", DATE))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("amount unit");
        verifyNoInteractions(token);
        assertThat(client.uri).isNull();
    }

    @Test
    void blocksMarketTrInDemoEnvironment() {
        RecordingClient client = new RecordingClient(successResponse());
        KisAccessTokenProvider token = mock(KisAccessTokenProvider.class);
        var adapter = adapter(client, token, kisProperties(KisEnvironment.DEMO),
                flowProperties(KisInvestorFlowAmountUnit.KRW));

        assertThatThrownBy(() -> adapter.fetchMarketInvestorFlows(InvestorFlowMarket.KOSPI, DATE))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("REAL");
        verifyNoInteractions(token);
        assertThat(client.uri).isNull();
    }

    @Test
    void doesNotExposeRawProviderMessageInException() {
        RecordingClient client = new RecordingClient(json("""
                {"rt_cd":"1","msg_cd":"AUTH001","msg1":"secret-token account-number"}
                """));

        assertThatThrownBy(() -> adapter(client, kisProperties(KisEnvironment.DEMO),
                flowProperties(KisInvestorFlowAmountUnit.KRW))
                .fetchStockInvestorFlows("005930", DATE))
                .hasMessageContaining("AUTH001")
                .hasMessageNotContaining("secret-token")
                .hasMessageNotContaining("account-number");
    }

    @Test
    void mapsDetailedInstitutionLabelsWithoutLosingRawClassificationPolicy() {
        assertThat(KisInvestorFlowProviderAdapter.mapInvestorType("금융투자"))
                .isEqualTo(InvestorType.FINANCIAL_INVESTMENT);
        assertThat(KisInvestorFlowProviderAdapter.mapInvestorType("투신"))
                .isEqualTo(InvestorType.INVESTMENT_TRUST);
        assertThat(KisInvestorFlowProviderAdapter.mapInvestorType("연기금"))
                .isEqualTo(InvestorType.PENSION_FUND);
        assertThat(KisInvestorFlowProviderAdapter.mapInvestorType("알수없음"))
                .isEqualTo(InvestorType.ETC);
    }

    private static KisInvestorFlowProviderAdapter adapter(RecordingClient client,
            KisProperties kis, InvestorFlowProperties flow) {
        KisAccessTokenProvider token = mock(KisAccessTokenProvider.class);
        when(token.getAccessToken(kis.getEnvironment())).thenReturn("test-token");
        return adapter(client, token, kis, flow);
    }

    private static KisInvestorFlowProviderAdapter adapter(RecordingClient client,
            KisAccessTokenProvider token, KisProperties kis, InvestorFlowProperties flow) {
        return new KisInvestorFlowProviderAdapter(client, token, kis, flow,
                OperationalMetricsPort.noop(), CLOCK);
    }

    private static KisProperties kisProperties(KisEnvironment environment) {
        KisProperties properties = new KisProperties();
        properties.setEnvironment(environment);
        properties.setAppKey("test-app-key");
        properties.setAppSecret("test-app-secret");
        return properties;
    }

    private static InvestorFlowProperties flowProperties(KisInvestorFlowAmountUnit unit) {
        InvestorFlowProperties properties = new InvestorFlowProperties();
        properties.setKisAmountUnit(unit);
        return properties;
    }

    private static JsonNode successResponse() {
        return json("""
                {"rt_cd":"0","output":[{
                  "stck_bsop_date":"20260615",
                  "frgn_ntby_tr_pbmn":"120","frgn_ntby_qty":"12",
                  "frgn_shnu_tr_pbmn":"500","frgn_seln_tr_pbmn":"380",
                  "frgn_shnu_vol":"50","frgn_seln_vol":"38",
                  "orgn_ntby_tr_pbmn":"80","orgn_ntby_qty":"8",
                  "orgn_shnu_tr_pbmn":"300","orgn_seln_tr_pbmn":"220",
                  "orgn_shnu_vol":"30","orgn_seln_vol":"22",
                  "prsn_ntby_tr_pbmn":"-200","prsn_ntby_qty":"-20",
                  "prsn_shnu_tr_pbmn":"400","prsn_seln_tr_pbmn":"600",
                  "prsn_shnu_vol":"40","prsn_seln_vol":"60"
                }]}
                """);
    }

    private static JsonNode json(String value) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static final class RecordingClient implements KisHttpClient {
        private final JsonNode response;
        private URI uri;
        private Map<String, String> headers = Map.of();
        private Duration timeout;

        private RecordingClient(JsonNode response) {
            this.response = response;
        }

        @Override
        public KisHttpResponse postJson(URI uri, Map<String, String> headers, Object body) {
            throw new AssertionError("Investor flow adapter must not issue token or order POST requests");
        }

        @Override
        public KisHttpResponse get(URI uri, Map<String, String> headers) {
            throw new AssertionError("Investor flow adapter must apply its configured timeout");
        }

        @Override
        public KisHttpResponse get(URI uri, Map<String, String> headers, Duration timeout) {
            this.uri = uri;
            this.headers = headers;
            this.timeout = timeout;
            return new KisHttpResponse(200, response);
        }
    }
}
