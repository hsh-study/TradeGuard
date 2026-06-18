package seokhoon.trade.adapter.marketdata.kis;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

interface KisHttpClient {
    KisHttpResponse postJson(URI uri, Map<String, String> headers, Object body);

    KisHttpResponse get(URI uri, Map<String, String> headers);

    default KisHttpResponse get(URI uri, Map<String, String> headers, Duration timeout) {
        return get(uri, headers);
    }
}
