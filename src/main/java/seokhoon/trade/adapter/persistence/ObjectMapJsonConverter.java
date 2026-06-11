package seokhoon.trade.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class ObjectMapJsonConverter
        implements AttributeConverter<Map<String, Object>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final TypeReference<Map<String, Object>> OBJECT_MAP =
            new TypeReference<>() {
            };

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(
                    attribute == null ? Map.of() : attribute
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to serialize parameter snapshot",
                    exception
            );
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return Map.of();
        }
        try {
            return Map.copyOf(OBJECT_MAPPER.readValue(databaseValue, OBJECT_MAP));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize parameter snapshot",
                    exception
            );
        }
    }
}
