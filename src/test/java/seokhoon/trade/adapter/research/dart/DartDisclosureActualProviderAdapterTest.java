package seokhoon.trade.adapter.research.dart;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.DartCorpMappingPort;
import seokhoon.trade.config.*;
import seokhoon.trade.domain.research.*;
import seokhoon.trade.domain.stock.Market;
import tools.jackson.databind.ObjectMapper;

import java.net.http.*;
import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DartDisclosureActualProviderAdapterTest {
    @SuppressWarnings({"unchecked","rawtypes"})
    @Test void mapsOnlySafeDisclosureMetadata() throws Exception {
        HttpClient client=mock(HttpClient.class); HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {"status":"000","list":[{"rcept_no":"202606150001","report_nm":"단일판매ㆍ공급계약 체결",
                "rcept_dt":"20260615","pblntf_ty":"I","html":"SECRET_RAW_BODY","attachment":"file.zip"}]}
                """);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        DartProperties dart=new DartProperties();dart.setProviderEnabled(true);dart.setApiBaseUrl("https://opendart.fss.or.kr");dart.setApiKey("secret-key");
        DisclosureActualProviderProperties properties=new DisclosureActualProviderProperties();properties.setEnabled(true);
        DartCorpMappingPort mappings=mock(DartCorpMappingPort.class);
        when(mappings.findByStockCode("005930")).thenReturn(Optional.of(new DartCorpMapping(null,"005930","00126380","삼성전자",Market.KOSPI,Instant.EPOCH,Instant.EPOCH)));
        var adapter=new DartDisclosureActualProviderAdapter(client,new ObjectMapper(),dart,properties,mappings,
                new DartDisclosureActualProviderAdapter.RateLimiter(30,Clock.systemUTC()));
        DisclosureActualRecord record=adapter.fetchDisclosures("005930",LocalDate.of(2026,6,1),LocalDate.of(2026,6,15)).getFirst();
        assertThat(record.relatedCatalystType()).isEqualTo(CatalystType.ORDER_CONTRACT);
        assertThat(record.sourceUrl()).contains("202606150001").doesNotContain("secret-key");
        assertThat(record.toString()).doesNotContain("SECRET_RAW_BODY","file.zip","secret-key");
    }
}
