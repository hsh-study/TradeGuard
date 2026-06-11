package seokhoon.trade.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(
                    attribute == null ? List.of() : attribute
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize string list", exception);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(OBJECT_MAPPER.readValue(databaseValue, STRING_LIST));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to deserialize string list", exception);
        }
    }
}
