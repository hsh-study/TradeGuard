package seokhoon.trade.adapter.marketdata.kis;

import java.net.URI;
import java.util.Map;

interface KisHttpClient {
    KisHttpResponse postJson(URI uri, Map<String, String> headers, Object body);

    KisHttpResponse get(URI uri, Map<String, String> headers);
}
