package seokhoon.trade.adapter.research.dart;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import seokhoon.trade.application.port.out.DartCorpMappingPort;
import seokhoon.trade.application.port.out.DisclosureActualProviderPort;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.config.DartProviderException;
import seokhoon.trade.config.DisclosureActualProviderProperties;
import seokhoon.trade.domain.research.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class DartDisclosureActualProviderAdapter implements DisclosureActualProviderPort {
    private static final String LIST_PATH = "/api/list.json";
    private static final String VIEW_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final DartProperties dart;
    private final DisclosureActualProviderProperties properties;
    private final DartCorpMappingPort mappings;
    private final RateLimiter limiter;

    @Autowired
    public DartDisclosureActualProviderAdapter(ObjectMapper mapper, DartProperties dart,
            DisclosureActualProviderProperties properties, DartCorpMappingPort mappings) {
        this(HttpClient.newBuilder().connectTimeout(properties.timeout()).build(), mapper, dart,
                properties, mappings, new RateLimiter(properties.getRateLimitPerMinute(), Clock.systemUTC()));
    }

    DartDisclosureActualProviderAdapter(HttpClient client, ObjectMapper mapper, DartProperties dart,
            DisclosureActualProviderProperties properties, DartCorpMappingPort mappings, RateLimiter limiter) {
        this.client=client; this.mapper=mapper; this.dart=dart; this.properties=properties;
        this.mappings=mappings; this.limiter=limiter;
    }

    @Override public List<DisclosureActualRecord> fetchDisclosures(String stockCode, LocalDate fromDate, LocalDate toDate) {
        properties.validateRequest();
        dart.validateProviderRequest();
        if (fromDate.isAfter(toDate)) throw new IllegalArgumentException("fromDate must not be after toDate");
        String corpCode=mappings.findByStockCode(stockCode).map(DartCorpMapping::corpCode)
                .orElseThrow(() -> new DartProviderException("DART corporation mapping is missing"));
        limiter.acquire();
        HttpRequest request=HttpRequest.newBuilder(uri(corpCode,fromDate,toDate))
                .timeout(properties.timeout()).GET().build();
        try {
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new DartProviderException("DART disclosure request failed with HTTP "+response.statusCode());
            JsonNode body=mapper.readTree(response.body());
            String status=body.path("status").asText("");
            if("013".equals(status)) return List.of();
            if(!"000".equals(status)) throw new DartProviderException("DART disclosure request failed with status "+status);
            JsonNode list=body.path("list");
            if(!list.isArray()) throw new DartProviderException("DART disclosure response did not contain list");
            List<DisclosureActualRecord> result=new ArrayList<>();
            for(JsonNode row:list) {
                if(result.size()>=properties.getMaxItemsPerStock()) break;
                String title=text(row,"report_nm"); String receipt=text(row,"rcept_no");
                LocalDate date=parseDate(text(row,"rcept_dt")); String category=text(row,"pblntf_detail_ty");
                if(category.isBlank())category=text(row,"pblntf_ty");
                if(title.isBlank()||receipt.isBlank()||date==null) continue;
                result.add(new DisclosureActualRecord(stockCode,date,null,title,
                        DisclosureActualPolicy.disclosureType(title,category),DisclosureProvider.DART,
                        VIEW_URL+encode(receipt),receipt,DisclosureActualPolicy.relatedCatalystType(title),
                        DisclosureActualPolicy.importance(title),category.isBlank()?null:category));
            }
            return List.copyOf(result);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt(); throw new DartProviderException("DART disclosure request was interrupted",e);
        } catch(IOException e) {
            throw new DartProviderException("DART disclosure request failed",e);
        }
    }

    private URI uri(String corpCode,LocalDate from,LocalDate to) {
        Map<String,String> values=new LinkedHashMap<>(); values.put("crtfc_key",dart.getApiKey());
        values.put("corp_code",corpCode); values.put("bgn_de",from.format(DateTimeFormatter.BASIC_ISO_DATE));
        values.put("end_de",to.format(DateTimeFormatter.BASIC_ISO_DATE));
        values.put("page_count",Integer.toString(properties.getMaxItemsPerStock()));
        String query=values.entrySet().stream().map(e->encode(e.getKey())+"="+encode(e.getValue()))
                .reduce((a,b)->a+"&"+b).orElseThrow();
        String base=dart.getApiBaseUrl().endsWith("/")?dart.getApiBaseUrl().substring(0,dart.getApiBaseUrl().length()-1):dart.getApiBaseUrl();
        return URI.create(base+LIST_PATH+"?"+query);
    }
    private static String text(JsonNode node,String field){return node.path(field).asText("").trim();}
    private static LocalDate parseDate(String value){try{return LocalDate.parse(value,DateTimeFormatter.BASIC_ISO_DATE);}catch(Exception e){return null;}}
    private static String encode(String value){return URLEncoder.encode(value, StandardCharsets.UTF_8);}

    static final class RateLimiter {
        private final int limit; private final Clock clock; private Instant window; private int used;
        RateLimiter(int limit,Clock clock){this.limit=Math.max(1,limit);this.clock=clock;this.window=clock.instant();}
        synchronized void acquire(){Instant now=clock.instant();if(Duration.between(window,now).toMinutes()>=1){window=now;used=0;}
            if(used>=limit)throw new DartProviderException("DART disclosure rate limit exceeded");used++;}
    }
}
