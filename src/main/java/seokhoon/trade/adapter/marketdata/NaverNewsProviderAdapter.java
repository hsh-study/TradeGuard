package seokhoon.trade.adapter.marketdata;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.HtmlUtils;
import seokhoon.trade.application.port.out.NewsProviderPort;
import seokhoon.trade.config.NaverNewsProperties;
import tools.jackson.databind.JsonNode; import tools.jackson.databind.ObjectMapper;
import java.net.*; import java.net.http.*; import java.nio.charset.StandardCharsets;
import java.time.*; import java.time.format.DateTimeFormatter; import java.util.*;

@Component
public class NaverNewsProviderAdapter implements NewsProviderPort {
    private final NaverNewsProperties properties; private final ObjectMapper mapper; private final HttpClient client;
    @Autowired public NaverNewsProviderAdapter(NaverNewsProperties p,ObjectMapper m){this(p,m,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());}
    NaverNewsProviderAdapter(NaverNewsProperties p,ObjectMapper m,HttpClient c){properties=p;mapper=m;client=c;}
    @Override public List<ProviderNews> search(String query,int display){
        properties.validateEnabled();
        try{
            String qs="query="+URLEncoder.encode(query,StandardCharsets.UTF_8)+"&display="+display+"&start=1&sort="+properties.getSort();
            HttpRequest request=HttpRequest.newBuilder(URI.create(properties.getApiBaseUrl()+properties.getSearchPath()+"?"+qs))
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                    .header("X-Naver-Client-Id",properties.getClientId()).header("X-Naver-Client-Secret",properties.getClientSecret()).GET().build();
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new IllegalStateException("Naver News API request failed: HTTP "+response.statusCode());
            JsonNode root=mapper.readTree(response.body());List<ProviderNews> result=new ArrayList<>();
            for(JsonNode item:root.path("items")){String origin=text(item,"originallink"),link=text(item,"link");
                result.add(new ProviderNews(clean(text(item,"title")),clean(text(item,"description")),origin,link,publisher(origin),date(text(item,"pubDate"))));}
            return List.copyOf(result);
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Naver News API request interrupted",e);}
        catch(java.io.IOException|IllegalArgumentException e){throw new IllegalStateException("Naver News API request failed",e);}
    }
    static String clean(String value){if(value==null)return "";String noExecutable=value.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>"," ");String noTags=noExecutable.replaceAll("(?is)<[^>]*>"," ");return HtmlUtils.htmlUnescape(noTags).replaceAll("\\s+"," ").trim();}
    private static String text(JsonNode n,String f){String v=n.path(f).asText("").trim();return v.isEmpty()?null:v;}
    private static Instant date(String v){if(v==null)return null;try{return ZonedDateTime.parse(v,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();}catch(DateTimeException e){return null;}}
    private static String publisher(String link){if(link==null)return null;try{return URI.create(link).getHost();}catch(IllegalArgumentException e){return null;}}
}
