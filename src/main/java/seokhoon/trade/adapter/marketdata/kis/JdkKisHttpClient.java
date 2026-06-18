package seokhoon.trade.adapter.marketdata.kis;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
class JdkKisHttpClient implements KisHttpClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    JdkKisHttpClient(ObjectMapper objectMapper) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), objectMapper);
    }

    JdkKisHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public KisHttpResponse postJson(URI uri, Map<String, String> headers, Object body) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (JacksonException exception) {
            throw new KisApiException("Failed to create KIS request", exception);
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("content-type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        headers.forEach(request::header);
        return send(request.build());
    }

    @Override
    public KisHttpResponse get(URI uri, Map<String, String> headers) {
        return get(uri, headers, REQUEST_TIMEOUT);
    }

    @Override
    public KisHttpResponse get(URI uri, Map<String, String> headers, Duration timeout) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET();
        headers.forEach(request::header);
        return send(request.build());
    }

    private KisHttpResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            return new KisHttpResponse(response.statusCode(), body);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KisApiException("KIS request was interrupted", exception);
        } catch (IOException exception) {
            throw new KisApiException("KIS request failed", exception);
        }
    }
}
