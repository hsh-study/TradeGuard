package seokhoon.trade.adapter.research.dart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.DartCorpCodeProviderPort;
import seokhoon.trade.config.DartProperties;
import seokhoon.trade.config.DartProviderException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class DartCorpCodeProviderAdapter implements DartCorpCodeProviderPort {
    private final DartProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public DartCorpCodeProviderAdapter(DartProperties properties) {
        this(properties, HttpClient.newHttpClient());
    }

    DartCorpCodeProviderAdapter(DartProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public byte[] fetchCorpCodeFile() {
        properties.validateCorpCodeImportRequest();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getCorpCodeZipUrl()))
                    .timeout(properties.corpCodeImportTimeout())
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DartProviderException("DART corp code provider returned HTTP " + response.statusCode());
            }
            return extractXml(response.body());
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DartProviderException("DART corp code provider request failed");
        }
    }

    private static byte[] extractXml(byte[] zipBytes) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    zip.transferTo(output);
                    return output.toByteArray();
                }
            }
            throw new DartProviderException("DART corp code zip did not contain XML");
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DartProviderException("DART corp code zip parse failed");
        }
    }
}
